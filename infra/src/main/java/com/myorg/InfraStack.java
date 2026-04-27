package com.myorg;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpAlbIntegration;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.VpcLink;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.assets.Platform;
import software.amazon.awscdk.services.ecs.AssetImageProps;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuArchitecture;
import software.amazon.awscdk.services.ecs.DeploymentCircuitBreaker;
import software.amazon.awscdk.services.ecs.LogDrivers;
import software.amazon.awscdk.services.ecs.OperatingSystemFamily;
import software.amazon.awscdk.services.ecs.RuntimePlatform;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class InfraStack extends Stack {
    private static final int CONTAINER_PORT = 8080;

    // Smallest valid Fargate task size. AWS does not allow lower than 256 CPU / 512 MiB.
    private static final int APP_CPU_UNITS = 256;
    private static final int APP_MEMORY_MIB = 512;

    // Single running task is enough for the current one-controller service.
    // During deployments, ECS may briefly run a replacement task to avoid downtime.
    private static final int APP_DESIRED_TASKS = 1;

    public InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Production baseline: two AZs and private application subnets.
        // The NAT gateway lets private ECS tasks pull images and reach AWS APIs without public IPs.
        Vpc vpc = Vpc.Builder.create(this, "Vpc")
                .maxAzs(2)
                .natGateways(1)
                .build();

        SubnetSelection privateSubnets = SubnetSelection.builder()
                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                .build();

        Cluster cluster = Cluster.Builder.create(this, "Cluster")
                .vpc(vpc)
                .build();

        // Keep logs cheap for this learning stack, and delete them on cdk destroy.
        LogGroup serviceLogGroup = LogGroup.Builder.create(this, "ServiceLogGroup")
                .retention(RetentionDays.ONE_DAY)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Creates ECS service, task definition, target group, listener, and ALB.
        // The ALB stays internal; API Gateway is the only public entry point.
        ApplicationLoadBalancedFargateService service = ApplicationLoadBalancedFargateService.Builder
                .create(this, "Service")
                .cluster(cluster)
                .cpu(APP_CPU_UNITS)
                .memoryLimitMiB(APP_MEMORY_MIB)
                .desiredCount(APP_DESIRED_TASKS)
                .minHealthyPercent(100)
                // Match the Docker asset build architecture to Fargate, avoiding exec format errors.
                .runtimePlatform(RuntimePlatform.builder()
                        .cpuArchitecture(CpuArchitecture.ARM64)
                        .operatingSystemFamily(OperatingSystemFamily.LINUX)
                        .build())
                .assignPublicIp(false)
                .publicLoadBalancer(false)
                .openListener(false)
                .taskSubnets(privateSubnets)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .image(ContainerImage.fromAsset("../service", AssetImageProps.builder()
                                .platform(Platform.LINUX_ARM64)
                                .build()))
                        .containerPort(CONTAINER_PORT)
                        .logDriver(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(serviceLogGroup)
                                .streamPrefix("service")
                                .build()))
                        .build())
                .build();

        // ALB health checks must hit an endpoint that Spring Boot can answer quickly.
        service.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .path("/actuator/health")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(30))
                .timeout(Duration.seconds(5))
                .build());

        // API Gateway HTTP API reaches private VPC resources through this VPC Link.
        SecurityGroup vpcLinkSecurityGroup = SecurityGroup.Builder.create(this, "VpcLinkSecurityGroup")
                .vpc(vpc)
                .description("API Gateway VPC Link security group")
                .allowAllOutbound(true)
                .build();

        // Only the VPC Link security group can call the internal ALB listener.
        service.getLoadBalancer().getConnections().allowFrom(
                vpcLinkSecurityGroup,
                Port.tcp(80),
                "Allow API Gateway VPC Link to reach the internal ALB");

        VpcLink vpcLink = VpcLink.Builder.create(this, "VpcLink")
                .vpc(vpc)
                .subnets(privateSubnets)
                .securityGroups(List.of(vpcLinkSecurityGroup))
                .build();

        // Default proxy route: API Gateway -> VPC Link -> ALB listener -> ECS task.
        HttpAlbIntegration albIntegration = HttpAlbIntegration.Builder
                .create("AlbIntegration", service.getListener())
                .vpcLink(vpcLink)
                .method(HttpMethod.ANY)
                .timeout(Duration.seconds(29))
                .build();

        HttpApi api = HttpApi.Builder.create(this, "Api")
                .apiName("prod-core-api")
                .defaultIntegration(albIntegration)
                .build();

        CfnOutput.Builder.create(this, "ApiUrl")
                .description("Public API Gateway URL")
                .value(api.getUrl())
                .build();

        CfnOutput.Builder.create(this, "AlbDnsName")
                .description("Internal ALB DNS name")
                .value(service.getLoadBalancer().getLoadBalancerDnsName())
                .build();
    }
}
