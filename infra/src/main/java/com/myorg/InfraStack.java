package com.myorg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.CfnAccount;
import software.amazon.awscdk.services.apigateway.CfnDeployment;
import software.amazon.awscdk.services.apigateway.CfnMethod;
import software.amazon.awscdk.services.apigateway.CfnResource;
import software.amazon.awscdk.services.apigateway.CfnRestApi;
import software.amazon.awscdk.services.apigateway.CfnStage;
import software.amazon.awscdk.services.apigatewayv2.VpcLink;
import software.amazon.awscdk.services.applicationsignals.CfnDiscovery;
import software.amazon.awscdk.services.cloudwatch.Dashboard;
import software.amazon.awscdk.services.cloudwatch.GraphWidget;
import software.amazon.awscdk.services.cloudwatch.MathExpression;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;
import software.amazon.awscdk.services.cloudwatch.Unit;
import software.amazon.awscdk.services.cloudwatch.YAxisProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.assets.Platform;
import software.amazon.awscdk.services.ecs.AssetImageProps;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinition;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerDependency;
import software.amazon.awscdk.services.ecs.ContainerDependencyCondition;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuArchitecture;
import software.amazon.awscdk.services.ecs.DeploymentCircuitBreaker;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateServiceProps;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LoadBalancerTargetOptions;
import software.amazon.awscdk.services.ecs.LogDrivers;
import software.amazon.awscdk.services.ecs.OperatingSystemFamily;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.RuntimePlatform;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerRule;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerRuleProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroupProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.FixedResponseOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.HttpCodeTarget;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerAction;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCondition;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

public class InfraStack extends Stack {
    private static final int CONTAINER_PORT = 8080;
    private static final String APP_CONTAINER_NAME = "App";

    // Keep tasks small for learning cost, but leave room for Spring Boot plus the OTel javaagent.
    private static final int APP_CPU_UNITS = 256;
    private static final int APP_MEMORY_MIB = 1024;
    private static final int APP_DESIRED_TASKS = 1;
    private static final int HEALTH_CHECK_GRACE_SECONDS = 240;

    private static final String CLOUDWATCH_AGENT_IMAGE =
            "public.ecr.aws/cloudwatch-agent/cloudwatch-agent:latest";
    private static final String REST_API_NAME = "prod-core-rest-api";
    private static final String REST_API_STAGE = "prod";
    private static final String STATE_TABLE_NAME = "prod-core-lab-state";

    public InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Private app subnets keep ECS tasks off the public internet.
        // The NAT gateway lets tasks pull container images and call AWS APIs.
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

        // Enables CloudWatch Application Signals discovery in this account/region.
        CfnDiscovery.Builder.create(this, "ApplicationSignalsDiscovery")
                .build();

        Table stateTable = Table.Builder.create(this, "StateTable")
                .tableName(STATE_TABLE_NAME)
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .timeToLiveAttribute("expiresAtEpochSeconds")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        Queue jobDeadLetterQueue = Queue.Builder.create(this, "JobDeadLetterQueue")
                .retentionPeriod(Duration.days(4))
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        Queue jobQueue = Queue.Builder.create(this, "JobQueue")
                .visibilityTimeout(Duration.seconds(30))
                .receiveMessageWaitTime(Duration.seconds(10))
                .retentionPeriod(Duration.days(4))
                .deadLetterQueue(DeadLetterQueue.builder()
                        .queue(jobDeadLetterQueue)
                        .maxReceiveCount(3)
                        .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        LogGroup authLogGroup = serviceLogGroup("AuthLogGroup",
                "/aws/ecs/prod-core/auth-token-provider");
        LogGroup consumerLogGroup = serviceLogGroup("ConsumerLogGroup",
                "/aws/ecs/prod-core/consumer-service");
        LogGroup workerLogGroup = serviceLogGroup("WorkerLogGroup",
                "/aws/ecs/prod-core/worker-service");

        LogGroup restApiAccessLogGroup = LogGroup.Builder.create(this, "RestApiAccessLogGroup")
                .logGroupName("/aws/apigateway/prod-core-rest-api/access")
                .retention(RetentionDays.ONE_DAY)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        SecurityGroup loadBalancerSecurityGroup = SecurityGroup.Builder.create(this, "InternalAlbSecurityGroup")
                .vpc(vpc)
                .description("Internal ALB security group")
                .allowAllOutbound(true)
                .build();

        SecurityGroup authSecurityGroup = serviceSecurityGroup(vpc, "AuthServiceSecurityGroup",
                "auth-token-provider task security group");
        SecurityGroup consumerSecurityGroup = serviceSecurityGroup(vpc, "ConsumerServiceSecurityGroup",
                "consumer-service task security group");
        SecurityGroup workerSecurityGroup = serviceSecurityGroup(vpc, "WorkerServiceSecurityGroup",
                "worker-service task security group");

        SecurityGroup vpcLinkSecurityGroup = SecurityGroup.Builder.create(this, "VpcLinkSecurityGroup")
                .vpc(vpc)
                .description("API Gateway VPC Link security group")
                .allowAllOutbound(true)
                .build();

        ApplicationLoadBalancer loadBalancer = ApplicationLoadBalancer.Builder.create(this, "InternalAlb")
                .vpc(vpc)
                .internetFacing(false)
                .vpcSubnets(privateSubnets)
                .securityGroup(loadBalancerSecurityGroup)
                .build();

        ApplicationListener listener = loadBalancer.addListener("HttpListener",
                BaseApplicationListenerProps.builder()
                        .port(80)
                        .protocol(ApplicationProtocol.HTTP)
                        .open(false)
                        .defaultAction(ListenerAction.fixedResponse(404, FixedResponseOptions.builder()
                                .contentType("application/json")
                                .messageBody("{\"message\":\"route not found\"}")
                                .build()))
                        .build());

        // API Gateway is the only public ingress. The consumer can also call the ALB internally for auth.
        loadBalancer.getConnections().allowFrom(vpcLinkSecurityGroup, Port.tcp(80),
                "Allow API Gateway VPC Link to reach the internal ALB");
        loadBalancer.getConnections().allowFrom(consumerSecurityGroup, Port.tcp(80),
                "Allow consumer-service to call auth-token-provider through the internal ALB");
        authSecurityGroup.getConnections().allowFrom(loadBalancerSecurityGroup, Port.tcp(CONTAINER_PORT),
                "Allow ALB to reach auth-token-provider");
        consumerSecurityGroup.getConnections().allowFrom(loadBalancerSecurityGroup, Port.tcp(CONTAINER_PORT),
                "Allow ALB to reach consumer-service");

        FargateTaskDefinition authTaskDefinition = createTaskDefinition(
                "AuthTaskDefinition",
                "prod-core-auth-token-provider",
                "../auth-service",
                authLogGroup,
                "auth-token-provider",
                "auth",
                Map.of("STATE_TABLE_NAME", stateTable.getTableName()));

        FargateTaskDefinition consumerTaskDefinition = createTaskDefinition(
                "ConsumerTaskDefinition",
                "prod-core-consumer-service",
                "../service",
                consumerLogGroup,
                "consumer-service",
                "consumer",
                Map.of(
                        "AUTH_BASE_URL", "http://" + loadBalancer.getLoadBalancerDnsName(),
                        "JOB_TABLE_NAME", stateTable.getTableName(),
                        "JOB_QUEUE_URL", jobQueue.getQueueUrl()));

        FargateTaskDefinition workerTaskDefinition = createTaskDefinition(
                "WorkerTaskDefinition",
                "prod-core-worker-service",
                "../worker-service",
                workerLogGroup,
                "worker-service",
                "worker",
                Map.of(
                        "JOB_TABLE_NAME", stateTable.getTableName(),
                        "JOB_QUEUE_URL", jobQueue.getQueueUrl()));

        stateTable.grantReadWriteData(authTaskDefinition.getTaskRole());
        stateTable.grantReadWriteData(consumerTaskDefinition.getTaskRole());
        stateTable.grantReadWriteData(workerTaskDefinition.getTaskRole());
        jobQueue.grantSendMessages(consumerTaskDefinition.getTaskRole());
        jobQueue.grantConsumeMessages(workerTaskDefinition.getTaskRole());

        FargateService authService = createService("AuthService", cluster, authTaskDefinition,
                authSecurityGroup, privateSubnets, true);
        FargateService consumerService = createService("ConsumerService", cluster, consumerTaskDefinition,
                consumerSecurityGroup, privateSubnets, true);
        FargateService workerService = createService("WorkerService", cluster, workerTaskDefinition,
                workerSecurityGroup, privateSubnets, false);

        ApplicationTargetGroup authTargetGroup = createTargetGroup(
                "AuthTargetGroup",
                vpc,
                authService,
                "/actuator/health");
        ApplicationTargetGroup consumerTargetGroup = createTargetGroup(
                "ConsumerTargetGroup",
                vpc,
                consumerService,
                "/actuator/health");

        ApplicationListenerRule authRule = new ApplicationListenerRule(this, "AuthPathRule",
                ApplicationListenerRuleProps.builder()
                        .listener(listener)
                        .priority(10)
                        .conditions(List.of(ListenerCondition.pathPatterns(List.of("/auth/*"))))
                        .targetGroups(List.of(authTargetGroup))
                        .build());

        ApplicationListenerRule consumerRule = new ApplicationListenerRule(this, "ConsumerPathRule",
                ApplicationListenerRuleProps.builder()
                        .listener(listener)
                        .priority(20)
                        .conditions(List.of(ListenerCondition.pathPatterns(List.of("/api/*", "/actuator/*"))))
                        .targetGroups(List.of(consumerTargetGroup))
                        .build());

        // ECS services that use target groups must wait until listener rules attach those groups to the ALB.
        authService.getNode().addDependency(authRule);
        consumerService.getNode().addDependency(consumerRule);

        VpcLink vpcLink = VpcLink.Builder.create(this, "VpcLink")
                .vpc(vpc)
                .subnets(privateSubnets)
                .securityGroups(List.of(vpcLinkSecurityGroup))
                .build();

        Role apiGatewayCloudWatchRole = Role.Builder.create(this, "ApiGatewayCloudWatchRole")
                .assumedBy(new ServicePrincipal("apigateway.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName(
                        "service-role/AmazonAPIGatewayPushToCloudWatchLogs")))
                .description("Allows REST API Gateway stages to write execution logs to CloudWatch")
                .build();

        CfnAccount apiGatewayAccount = CfnAccount.Builder.create(this, "ApiGatewayAccount")
                .cloudWatchRoleArn(apiGatewayCloudWatchRole.getRoleArn())
                .build();

        CfnRestApi restApi = CfnRestApi.Builder.create(this, "RestApi")
                .name(REST_API_NAME)
                .description("REST API entry point for the prod-core microservice lab")
                .endpointConfiguration(CfnRestApi.EndpointConfigurationProperty.builder()
                        .types(List.of("REGIONAL"))
                        .build())
                .build();

        String albDnsName = loadBalancer.getLoadBalancerDnsName();
        String albArn = loadBalancer.getLoadBalancerArn();

        List<CfnMethod> restRootMethods = createRestRootMethods(
                restApi,
                albDnsName,
                vpcLink.getVpcLinkId(),
                albArn);

        CfnResource restProxyResource = CfnResource.Builder.create(this, "RestProxyResource")
                .restApiId(restApi.getRef())
                .parentId(restApi.getAttrRootResourceId())
                .pathPart("{proxy+}")
                .build();

        List<CfnMethod> restProxyMethods = createRestProxyMethods(
                restApi,
                restProxyResource,
                albDnsName,
                vpcLink.getVpcLinkId(),
                albArn);

        CfnDeployment restDeployment = CfnDeployment.Builder.create(this, "RestApiDeploymentConcreteMethods")
                .restApiId(restApi.getRef())
                .description("Deployment for REST API private ALB integration with concrete HTTP methods")
                .build();
        restRootMethods.forEach(method -> restDeployment.getNode().addDependency(method));
        restProxyMethods.forEach(method -> restDeployment.getNode().addDependency(method));

        CfnStage restProdStage = CfnStage.Builder.create(this, "RestApiProdStage")
                .restApiId(restApi.getRef())
                .deploymentId(restDeployment.getRef())
                .stageName(REST_API_STAGE)
                .tracingEnabled(true)
                .accessLogSetting(CfnStage.AccessLogSettingProperty.builder()
                        .destinationArn(restApiAccessLogGroup.getLogGroupArn())
                        .format(restApiAccessLogFormat())
                        .build())
                .methodSettings(List.of(CfnStage.MethodSettingProperty.builder()
                        .resourcePath("/*")
                        .httpMethod("*")
                        .loggingLevel("INFO")
                        .metricsEnabled(true)
                        .dataTraceEnabled(false)
                        .build()))
                .build();
        restProdStage.getNode().addDependency(apiGatewayAccount);

        createDashboard(
                authService,
                consumerService,
                workerService,
                authTargetGroup,
                consumerTargetGroup,
                jobQueue,
                jobDeadLetterQueue,
                stateTable);

        CfnOutput.Builder.create(this, "ApiUrl")
                .description("Primary REST API Gateway URL")
                .value(restApiUrl(restApi))
                .build();

        CfnOutput.Builder.create(this, "AlbDnsName")
                .description("Internal ALB DNS name")
                .value(loadBalancer.getLoadBalancerDnsName())
                .build();

        CfnOutput.Builder.create(this, "StateTableName")
                .description("DynamoDB table for tokens and jobs")
                .value(stateTable.getTableName())
                .build();

        CfnOutput.Builder.create(this, "JobQueueUrl")
                .description("SQS queue used by consumer-service")
                .value(jobQueue.getQueueUrl())
                .build();

        CfnOutput.Builder.create(this, "JobDeadLetterQueueUrl")
                .description("SQS DLQ used for poison job practice")
                .value(jobDeadLetterQueue.getQueueUrl())
                .build();

        CfnOutput.Builder.create(this, "RestApiAccessLogGroupName")
                .description("REST API Gateway access log group")
                .value(restApiAccessLogGroup.getLogGroupName())
                .build();

        CfnOutput.Builder.create(this, "AuthLogGroupName")
                .description("auth-token-provider log group")
                .value(authLogGroup.getLogGroupName())
                .build();

        CfnOutput.Builder.create(this, "ConsumerLogGroupName")
                .description("consumer-service log group")
                .value(consumerLogGroup.getLogGroupName())
                .build();

        CfnOutput.Builder.create(this, "WorkerLogGroupName")
                .description("worker-service log group")
                .value(workerLogGroup.getLogGroupName())
                .build();
    }

    private LogGroup serviceLogGroup(String id, String logGroupName) {
        return LogGroup.Builder.create(this, id)
                .logGroupName(logGroupName)
                .retention(RetentionDays.ONE_DAY)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private SecurityGroup serviceSecurityGroup(Vpc vpc, String id, String description) {
        return SecurityGroup.Builder.create(this, id)
                .vpc(vpc)
                .description(description)
                .allowAllOutbound(true)
                .build();
    }

    private FargateTaskDefinition createTaskDefinition(
            String id,
            String family,
            String assetDirectory,
            LogGroup logGroup,
            String serviceName,
            String streamPrefix,
            Map<String, String> appEnvironment) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, id)
                .family(family)
                .cpu(APP_CPU_UNITS)
                .memoryLimitMiB(APP_MEMORY_MIB)
                .runtimePlatform(RuntimePlatform.builder()
                        .cpuArchitecture(CpuArchitecture.ARM64)
                        .operatingSystemFamily(OperatingSystemFamily.LINUX)
                        .build())
                .build();

        Map<String, String> environment = new HashMap<>(appTracingEnvironment(
                serviceName,
                logGroup.getLogGroupName()));
        environment.putAll(appEnvironment);

        ContainerDefinition appContainer = taskDefinition.addContainer(streamPrefix + "App",
                ContainerDefinitionOptions.builder()
                        .containerName(APP_CONTAINER_NAME)
                        .image(ContainerImage.fromAsset(assetDirectory, AssetImageProps.builder()
                                .platform(Platform.LINUX_ARM64)
                                .build()))
                        .essential(true)
                        .portMappings(List.of(PortMapping.builder()
                                .containerPort(CONTAINER_PORT)
                                .protocol(Protocol.TCP)
                                .build()))
                        .environment(environment)
                        .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(logGroup)
                                .streamPrefix(streamPrefix)
                                .build()))
                        .build());
        taskDefinition.setDefaultContainer(appContainer);

        taskDefinition.getTaskRole().addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentServerPolicy"));
        taskDefinition.addToTaskRolePolicy(xrayPolicy());

        // Sidecar receives telemetry from the Java agent and publishes Application Signals/X-Ray data.
        ContainerDefinition cloudWatchAgent = taskDefinition.addContainer(streamPrefix + "CloudWatchAgent",
                ContainerDefinitionOptions.builder()
                        .containerName("ecs-cwagent")
                        .image(ContainerImage.fromRegistry(CLOUDWATCH_AGENT_IMAGE))
                        .essential(true)
                        .memoryReservationMiB(128)
                        .environment(Map.of("CW_CONFIG_CONTENT", cloudWatchAgentConfig()))
                        .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(logGroup)
                                .streamPrefix("cloudwatch-agent")
                                .build()))
                        .build());

        appContainer.addContainerDependencies(ContainerDependency.builder()
                .container(cloudWatchAgent)
                .condition(ContainerDependencyCondition.START)
                .build());

        return taskDefinition;
    }

    private FargateService createService(
            String id,
            Cluster cluster,
            FargateTaskDefinition taskDefinition,
            SecurityGroup securityGroup,
            SubnetSelection privateSubnets,
            boolean loadBalanced) {

        FargateServiceProps.Builder props = FargateServiceProps.builder()
                .cluster(cluster)
                .taskDefinition(taskDefinition)
                .desiredCount(APP_DESIRED_TASKS)
                .minHealthyPercent(100)
                .maxHealthyPercent(200)
                .assignPublicIp(false)
                .vpcSubnets(privateSubnets)
                .securityGroups(List.of(securityGroup))
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build());

        if (loadBalanced) {
            props.healthCheckGracePeriod(Duration.seconds(HEALTH_CHECK_GRACE_SECONDS));
        }

        return new FargateService(this, id, props.build());
    }

    private ApplicationTargetGroup createTargetGroup(
            String id,
            Vpc vpc,
            FargateService service,
            String healthPath) {

        return ApplicationTargetGroup.Builder.create(this, id)
                .vpc(vpc)
                .port(CONTAINER_PORT)
                .protocol(ApplicationProtocol.HTTP)
                .targetType(TargetType.IP)
                .deregistrationDelay(Duration.seconds(15))
                .healthCheck(HealthCheck.builder()
                        .path(healthPath)
                        .healthyHttpCodes("200")
                        .interval(Duration.seconds(30))
                        .timeout(Duration.seconds(5))
                        .healthyThresholdCount(2)
                        .unhealthyThresholdCount(3)
                        .build())
                .targets(List.of(service.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                        .containerName(APP_CONTAINER_NAME)
                        .containerPort(CONTAINER_PORT)
                        .protocol(Protocol.TCP)
                        .build())))
                .build();
    }

    private void createDashboard(
            FargateService authService,
            FargateService consumerService,
            FargateService workerService,
            ApplicationTargetGroup authTargetGroup,
            ApplicationTargetGroup consumerTargetGroup,
            Queue jobQueue,
            Queue jobDeadLetterQueue,
            Table stateTable) {

        Duration dashboardPeriod = Duration.minutes(1);

        Metric requestCount = restApiMetric("Count", MetricOptions.builder()
                .label("requests")
                .statistic("Sum")
                .period(dashboardPeriod)
                .unit(Unit.COUNT)
                .build());
        Metric clientErrors = restApiMetric("4XXError", countOptions("4xx", dashboardPeriod));
        Metric serverErrors = restApiMetric("5XXError", countOptions("5xx", dashboardPeriod));
        MathExpression successCount = MathExpression.Builder.create()
                .label("success")
                .expression("requests - clientErrors - serverErrors")
                .usingMetrics(Map.of(
                        "requests", requestCount,
                        "clientErrors", clientErrors,
                        "serverErrors", serverErrors))
                .period(dashboardPeriod)
                .build();

        Dashboard.Builder.create(this, "ServiceDashboard")
                .dashboardName("prod-core-microservices")
                .widgets(List.of(
                        List.of(GraphWidget.Builder.create()
                                .title("API Gateway requests")
                                .left(List.of(requestCount, successCount, clientErrors, serverErrors))
                                .leftYAxis(countAxis("count"))
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("API Gateway latency percentiles")
                                .left(List.of(
                                        restApiMetric("Latency", percentileOptions("p50", "p50", dashboardPeriod)),
                                        restApiMetric("Latency", percentileOptions("p90", "p90", dashboardPeriod)),
                                        restApiMetric("Latency", percentileOptions("p99", "p99", dashboardPeriod)),
                                        restApiMetric("Latency", percentileOptions("p999", "p99.9", dashboardPeriod))))
                                .leftYAxis(countAxis("milliseconds"))
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("API Gateway integration latency percentiles")
                                .left(List.of(
                                        restApiMetric("IntegrationLatency", percentileOptions("p50", "p50", dashboardPeriod)),
                                        restApiMetric("IntegrationLatency", percentileOptions("p90", "p90", dashboardPeriod)),
                                        restApiMetric("IntegrationLatency", percentileOptions("p99", "p99", dashboardPeriod)),
                                        restApiMetric("IntegrationLatency", percentileOptions("p999", "p99.9", dashboardPeriod))))
                                .leftYAxis(countAxis("milliseconds"))
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("ALB target health")
                                .left(List.of(
                                        authTargetGroup.getMetrics().healthyHostCount(
                                                countOptions("auth healthy", dashboardPeriod)),
                                        authTargetGroup.getMetrics().unhealthyHostCount(
                                                maxCountOptions("auth unhealthy", dashboardPeriod)),
                                        consumerTargetGroup.getMetrics().healthyHostCount(
                                                countOptions("consumer healthy", dashboardPeriod)),
                                        consumerTargetGroup.getMetrics().unhealthyHostCount(
                                                maxCountOptions("consumer unhealthy", dashboardPeriod))))
                                .leftYAxis(countAxis("tasks"))
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("ALB target responses")
                                .left(List.of(
                                        authTargetGroup.getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_2XX_COUNT,
                                                countOptions("auth 2xx", dashboardPeriod)),
                                        authTargetGroup.getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_4XX_COUNT,
                                                countOptions("auth 4xx", dashboardPeriod)),
                                        authTargetGroup.getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_5XX_COUNT,
                                                countOptions("auth 5xx", dashboardPeriod)),
                                        consumerTargetGroup.getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_2XX_COUNT,
                                                countOptions("consumer 2xx", dashboardPeriod)),
                                        consumerTargetGroup.getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_4XX_COUNT,
                                                countOptions("consumer 4xx", dashboardPeriod)),
                                        consumerTargetGroup.getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_5XX_COUNT,
                                                countOptions("consumer 5xx", dashboardPeriod))))
                                .leftYAxis(countAxis("count"))
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("ECS CPU")
                                .left(List.of(
                                        authService.metricCpuUtilization(percentOptions("auth", dashboardPeriod)),
                                        consumerService.metricCpuUtilization(percentOptions("consumer", dashboardPeriod)),
                                        workerService.metricCpuUtilization(percentOptions("worker", dashboardPeriod))))
                                .leftYAxis(percentAxis())
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("ECS memory")
                                .left(List.of(
                                        authService.metricMemoryUtilization(percentOptions("auth", dashboardPeriod)),
                                        consumerService.metricMemoryUtilization(percentOptions("consumer", dashboardPeriod)),
                                        workerService.metricMemoryUtilization(percentOptions("worker", dashboardPeriod))))
                                .leftYAxis(percentAxis())
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("SQS job queue")
                                .left(List.of(
                                        jobQueue.metricNumberOfMessagesSent(countOptions("sent", dashboardPeriod)),
                                        jobQueue.metricNumberOfMessagesReceived(countOptions("received", dashboardPeriod)),
                                        jobQueue.metricNumberOfMessagesDeleted(countOptions("deleted", dashboardPeriod)),
                                        jobQueue.metricApproximateNumberOfMessagesVisible(
                                                maxCountOptions("visible", dashboardPeriod)),
                                        jobDeadLetterQueue.metricApproximateNumberOfMessagesVisible(
                                                maxCountOptions("dlq visible", dashboardPeriod))))
                                .right(List.of(jobQueue.metricApproximateAgeOfOldestMessage(MetricOptions.builder()
                                        .label("oldest age")
                                        .statistic("Maximum")
                                        .period(dashboardPeriod)
                                        .unit(Unit.SECONDS)
                                        .build())))
                                .leftYAxis(countAxis("messages"))
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("DynamoDB throttles")
                                .left(List.of(
                                        stateTable.metricThrottledRequestsForOperations()))
                                .leftYAxis(countAxis("count"))
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("DynamoDB errors")
                                .left(List.of(
                                        stateTable.metricSystemErrorsForOperations(),
                                        stateTable.metricUserErrors(countOptions("user errors", dashboardPeriod))))
                                .leftYAxis(countAxis("count"))
                                .period(dashboardPeriod)
                                .build())))
                .build();
    }

    private static MetricOptions percentileOptions(String label, String percentile, Duration period) {
        return MetricOptions.builder()
                .label(label)
                .statistic(percentile)
                .period(period)
                .unit(Unit.MILLISECONDS)
                .build();
    }

    private static MetricOptions countOptions(String label, Duration period) {
        return MetricOptions.builder()
                .label(label)
                .statistic("Sum")
                .period(period)
                .unit(Unit.COUNT)
                .build();
    }

    private static MetricOptions maxCountOptions(String label, Duration period) {
        return MetricOptions.builder()
                .label(label)
                .statistic("Maximum")
                .period(period)
                .unit(Unit.COUNT)
                .build();
    }

    private static MetricOptions percentOptions(String label, Duration period) {
        return MetricOptions.builder()
                .label(label)
                .statistic("Average")
                .period(period)
                .unit(Unit.PERCENT)
                .build();
    }

    private static YAxisProps countAxis(String label) {
        return YAxisProps.builder()
                .label(label)
                .min(0)
                .showUnits(false)
                .build();
    }

    private static YAxisProps percentAxis() {
        return YAxisProps.builder()
                .label("percent")
                .min(0)
                .max(100)
                .showUnits(false)
                .build();
    }

    private static PolicyStatement xrayPolicy() {
        return PolicyStatement.Builder.create()
                .actions(List.of(
                        "xray:PutTraceSegments",
                        "xray:PutTelemetryRecords",
                        "xray:GetSamplingRules",
                        "xray:GetSamplingTargets",
                        "xray:GetSamplingStatisticSummaries"))
                .resources(List.of("*"))
                .build();
    }

    private static Map<String, String> appTracingEnvironment(String serviceName, String serviceLogGroupName) {
        return Map.ofEntries(
                Map.entry("JAVA_TOOL_OPTIONS", "-javaagent:/opt/aws-opentelemetry-agent.jar"),
                Map.entry("OTEL_SERVICE_NAME", serviceName),
                Map.entry("OTEL_RESOURCE_ATTRIBUTES", "service.name=" + serviceName + ","
                        + "service.namespace=prod-core,"
                        + "deployment.environment=learning,"
                        + "aws.log.group.names=" + serviceLogGroupName),
                Map.entry("OTEL_AWS_APPLICATION_SIGNALS_ENABLED", "true"),
                Map.entry("OTEL_TRACES_EXPORTER", "otlp"),
                Map.entry("OTEL_METRICS_EXPORTER", "none"),
                Map.entry("OTEL_LOGS_EXPORTER", "none"),
                Map.entry("OTEL_EXPORTER_OTLP_PROTOCOL", "http/protobuf"),
                Map.entry("OTEL_AWS_APPLICATION_SIGNALS_EXPORTER_ENDPOINT", "http://localhost:4316/v1/metrics"),
                Map.entry("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "http://localhost:4316/v1/traces"),
                Map.entry("OTEL_PROPAGATORS", "tracecontext,baggage,b3,xray"),
                Map.entry("OTEL_TRACES_SAMPLER", "xray"));
    }

    private static String cloudWatchAgentConfig() {
        return String.join("\n", List.of(
                "{",
                "  \"traces\": {",
                "    \"traces_collected\": {",
                "      \"application_signals\": {}",
                "    }",
                "  },",
                "  \"logs\": {",
                "    \"metrics_collected\": {",
                "      \"application_signals\": {}",
                "    }",
                "  }",
                "}"));
    }

    private String restApiUrl(CfnRestApi restApi) {
        return "https://" + restApi.getRef()
                + ".execute-api." + getRegion()
                + "." + getUrlSuffix()
                + "/" + REST_API_STAGE + "/";
    }

    private static Metric restApiMetric(String metricName, MetricOptions options) {
        return Metric.Builder.create()
                .namespace("AWS/ApiGateway")
                .metricName(metricName)
                .dimensionsMap(Map.of(
                        "ApiName", REST_API_NAME,
                        "Stage", REST_API_STAGE))
                .label(options.getLabel())
                .statistic(options.getStatistic())
                .period(options.getPeriod())
                .unit(options.getUnit())
                .build();
    }

    private List<CfnMethod> createRestRootMethods(
            CfnRestApi restApi,
            String albDnsName,
            String vpcLinkId,
            String loadBalancerArn) {

        return restHttpMethods().stream()
                .map(httpMethod -> CfnMethod.Builder.create(this, "RestRoot" + methodId(httpMethod) + "Method")
                        .restApiId(restApi.getRef())
                        .resourceId(restApi.getAttrRootResourceId())
                        .httpMethod(httpMethod)
                        .authorizationType("NONE")
                        .integration(restPrivateIntegration(
                                httpMethod,
                                "http://" + albDnsName,
                                vpcLinkId,
                                loadBalancerArn))
                        .build())
                .toList();
    }

    private List<CfnMethod> createRestProxyMethods(
            CfnRestApi restApi,
            CfnResource restProxyResource,
            String albDnsName,
            String vpcLinkId,
            String loadBalancerArn) {

        return restHttpMethods().stream()
                .map(httpMethod -> CfnMethod.Builder.create(this, "RestProxy" + methodId(httpMethod) + "Method")
                        .restApiId(restApi.getRef())
                        .resourceId(restProxyResource.getRef())
                        .httpMethod(httpMethod)
                        .authorizationType("NONE")
                        .requestParameters(Map.of("method.request.path.proxy", true))
                        .integration(restProxyPrivateIntegration(
                                httpMethod,
                                "http://" + albDnsName + "/{proxy}",
                                vpcLinkId,
                                loadBalancerArn))
                        .build())
                .toList();
    }

    private static List<String> restHttpMethods() {
        return List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD");
    }

    private static String methodId(String httpMethod) {
        return httpMethod.charAt(0) + httpMethod.substring(1).toLowerCase();
    }

    private static CfnMethod.IntegrationProperty restPrivateIntegration(
            String httpMethod,
            String uri,
            String vpcLinkId,
            String loadBalancerArn) {
        return CfnMethod.IntegrationProperty.builder()
                .type("HTTP_PROXY")
                .integrationHttpMethod(httpMethod)
                .uri(uri)
                .connectionType("VPC_LINK")
                .connectionId(vpcLinkId)
                .integrationTarget(loadBalancerArn)
                .timeoutInMillis(29_000)
                .build();
    }

    private static CfnMethod.IntegrationProperty restProxyPrivateIntegration(
            String httpMethod,
            String uri,
            String vpcLinkId,
            String loadBalancerArn) {
        return CfnMethod.IntegrationProperty.builder()
                .type("HTTP_PROXY")
                .integrationHttpMethod(httpMethod)
                .uri(uri)
                .connectionType("VPC_LINK")
                .connectionId(vpcLinkId)
                .integrationTarget(loadBalancerArn)
                .requestParameters(Map.of(
                        "integration.request.path.proxy",
                        "method.request.path.proxy"))
                .timeoutInMillis(29_000)
                .build();
    }

    private static String restApiAccessLogFormat() {
        return "{"
                + "\"requestId\":\"$context.requestId\","
                + "\"extendedRequestId\":\"$context.extendedRequestId\","
                + "\"apiId\":\"$context.apiId\","
                + "\"stage\":\"$context.stage\","
                + "\"resourcePath\":\"$context.resourcePath\","
                + "\"httpMethod\":\"$context.httpMethod\","
                + "\"path\":\"$context.path\","
                + "\"status\":\"$context.status\","
                + "\"protocol\":\"$context.protocol\","
                + "\"responseLatency\":\"$context.responseLatency\","
                + "\"responseLength\":\"$context.responseLength\","
                + "\"integrationStatus\":\"$context.integration.status\","
                + "\"integrationLatency\":\"$context.integration.latency\","
                + "\"integrationError\":\"$context.integration.error\","
                + "\"errorMessage\":\"$context.error.message\","
                + "\"sourceIp\":\"$context.identity.sourceIp\","
                + "\"userAgent\":\"$context.identity.userAgent\""
                + "}";
    }
}
