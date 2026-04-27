package com.myorg;

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
import software.amazon.awscdk.services.cloudwatch.Dashboard;
import software.amazon.awscdk.services.cloudwatch.GraphWidget;
import software.amazon.awscdk.services.cloudwatch.MathExpression;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;
import software.amazon.awscdk.services.cloudwatch.Unit;
import software.amazon.awscdk.services.cloudwatch.YAxisProps;
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
import software.amazon.awscdk.services.ecs.LogDrivers;
import software.amazon.awscdk.services.ecs.OperatingSystemFamily;
import software.amazon.awscdk.services.ecs.RuntimePlatform;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.HttpCodeTarget;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
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

    // Spring Boot plus the tracing javaagent can take longer to become ALB-healthy on a tiny task.
    private static final int HEALTH_CHECK_GRACE_SECONDS = 240;

    private static final String ADOT_COLLECTOR_IMAGE =
            "public.ecr.aws/aws-observability/aws-otel-collector:v0.47.0";
    private static final String REST_API_NAME = "prod-core-rest-api";
    private static final String REST_API_STAGE = "prod";

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

        LogGroup restApiAccessLogGroup = LogGroup.Builder.create(this, "RestApiAccessLogGroup")
                .logGroupName("/aws/apigateway/prod-core-rest-api/access")
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
                .healthCheckGracePeriod(Duration.seconds(HEALTH_CHECK_GRACE_SECONDS))
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
                        .environment(appTracingEnvironment())
                        .logDriver(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(serviceLogGroup)
                                .streamPrefix("service")
                                .build()))
                        .build())
                .build();

        // ADOT collector sidecar receives OTLP traces from the Java agent and exports them to X-Ray.
        ContainerDefinition adotCollector = service.getTaskDefinition().addContainer("AdotCollector",
                ContainerDefinitionOptions.builder()
                        .containerName("adot-collector")
                        .image(ContainerImage.fromRegistry(ADOT_COLLECTOR_IMAGE))
                        .essential(false)
                        .memoryReservationMiB(64)
                        .environment(Map.of("AOT_CONFIG_CONTENT", adotCollectorConfig()))
                        .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(serviceLogGroup)
                                .streamPrefix("adot")
                                .build()))
                        .build());

        service.getTaskDefinition().getDefaultContainer().addContainerDependencies(ContainerDependency.builder()
                .container(adotCollector)
                .condition(ContainerDependencyCondition.START)
                .build());

        service.getTaskDefinition().addToTaskRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of(
                        "xray:PutTraceSegments",
                        "xray:PutTelemetryRecords",
                        "xray:GetSamplingRules",
                        "xray:GetSamplingTargets",
                        "xray:GetSamplingStatisticSummaries"))
                .resources(List.of("*"))
                .build());

        // ALB health checks must hit an endpoint that Spring Boot can answer quickly.
        service.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .path("/actuator/health")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(30))
                .timeout(Duration.seconds(5))
                .healthyThresholdCount(2)
                .unhealthyThresholdCount(3)
                .build());

        // API Gateway reaches private VPC resources through this VPC Link.
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
                .description("REST API migration target for prod-core-service")
                .endpointConfiguration(CfnRestApi.EndpointConfigurationProperty.builder()
                        .types(List.of("REGIONAL"))
                        .build())
                .build();

        String albDnsName = service.getLoadBalancer().getLoadBalancerDnsName();
        String albArn = service.getLoadBalancer().getLoadBalancerArn();

        CfnMethod restRootAnyMethod = CfnMethod.Builder.create(this, "RestRootAnyMethod")
                .restApiId(restApi.getRef())
                .resourceId(restApi.getAttrRootResourceId())
                .httpMethod("ANY")
                .authorizationType("NONE")
                .integration(restPrivateIntegration(
                        "http://" + albDnsName,
                        vpcLink.getVpcLinkId(),
                        albArn))
                .build();

        CfnResource restProxyResource = CfnResource.Builder.create(this, "RestProxyResource")
                .restApiId(restApi.getRef())
                .parentId(restApi.getAttrRootResourceId())
                .pathPart("{proxy+}")
                .build();

        CfnMethod restProxyAnyMethod = CfnMethod.Builder.create(this, "RestProxyAnyMethod")
                .restApiId(restApi.getRef())
                .resourceId(restProxyResource.getRef())
                .httpMethod("ANY")
                .authorizationType("NONE")
                .requestParameters(Map.of("method.request.path.proxy", true))
                .integration(restProxyPrivateIntegration(
                        "http://" + albDnsName + "/{proxy}",
                        vpcLink.getVpcLinkId(),
                        albArn))
                .build();

        CfnDeployment restDeployment = CfnDeployment.Builder.create(this, "RestApiDeployment")
                .restApiId(restApi.getRef())
                .description("Deployment for REST API private ALB integration")
                .build();
        restDeployment.getNode().addDependency(restRootAnyMethod);
        restDeployment.getNode().addDependency(restProxyAnyMethod);

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

        Duration dashboardPeriod = Duration.minutes(1);

        Metric requestCount = restApiMetric("Count", MetricOptions.builder()
                .label("requests")
                .statistic("Sum")
                .period(dashboardPeriod)
                .unit(Unit.COUNT)
                .build());
        Metric clientErrors = restApiMetric("4XXError", MetricOptions.builder()
                .label("4xx")
                .statistic("Sum")
                .period(dashboardPeriod)
                .unit(Unit.COUNT)
                .build());
        Metric serverErrors = restApiMetric("5XXError", MetricOptions.builder()
                .label("5xx")
                .statistic("Sum")
                .period(dashboardPeriod)
                .unit(Unit.COUNT)
                .build());
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
                .dashboardName("prod-core-service")
                .widgets(List.of(
                        List.of(GraphWidget.Builder.create()
                                .title("API Gateway requests")
                                .left(List.of(requestCount, successCount, clientErrors, serverErrors))
                                .leftYAxis(YAxisProps.builder()
                                        .label("count")
                                        .min(0)
                                        .showUnits(false)
                                        .build())
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("API Gateway latency percentiles")
                                .left(List.of(
                                        restApiMetric("Latency", percentileOptions("p50", dashboardPeriod)),
                                        restApiMetric("Latency", percentileOptions("p90", dashboardPeriod)),
                                        restApiMetric("Latency", percentileOptions("p99", dashboardPeriod)),
                                        restApiMetric("Latency", percentileOptions("p99.9", dashboardPeriod))))
                                .leftYAxis(YAxisProps.builder()
                                        .label("milliseconds")
                                        .min(0)
                                        .showUnits(false)
                                        .build())
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("API Gateway integration latency percentiles")
                                .left(List.of(
                                        restApiMetric("IntegrationLatency", percentileOptions("p50", dashboardPeriod)),
                                        restApiMetric("IntegrationLatency", percentileOptions("p90", dashboardPeriod)),
                                        restApiMetric("IntegrationLatency", percentileOptions("p99", dashboardPeriod)),
                                        restApiMetric("IntegrationLatency", percentileOptions("p99.9", dashboardPeriod))))
                                .leftYAxis(YAxisProps.builder()
                                        .label("milliseconds")
                                        .min(0)
                                        .showUnits(false)
                                        .build())
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("ALB target health")
                                .left(List.of(
                                        service.getTargetGroup().getMetrics().healthyHostCount(MetricOptions.builder()
                                                .label("healthy tasks")
                                                .statistic("Minimum")
                                                .period(dashboardPeriod)
                                                .unit(Unit.COUNT)
                                                .build()),
                                        service.getTargetGroup().getMetrics().unhealthyHostCount(MetricOptions.builder()
                                                .label("unhealthy tasks")
                                                .statistic("Maximum")
                                                .period(dashboardPeriod)
                                                .unit(Unit.COUNT)
                                                .build())))
                                .leftYAxis(YAxisProps.builder()
                                        .label("tasks")
                                        .min(0)
                                        .showUnits(false)
                                        .build())
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("ALB target responses")
                                .left(List.of(
                                        service.getTargetGroup().getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_2XX_COUNT,
                                                countOptions("target 2xx", dashboardPeriod)),
                                        service.getTargetGroup().getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_4XX_COUNT,
                                                countOptions("target 4xx", dashboardPeriod)),
                                        service.getTargetGroup().getMetrics().httpCodeTarget(HttpCodeTarget.TARGET_5XX_COUNT,
                                                countOptions("target 5xx", dashboardPeriod))))
                                .leftYAxis(YAxisProps.builder()
                                        .label("count")
                                        .min(0)
                                        .showUnits(false)
                                        .build())
                                .period(dashboardPeriod)
                                .build()),
                        List.of(GraphWidget.Builder.create()
                                .title("ECS service resource usage")
                                .left(List.of(
                                        service.getService().metricCpuUtilization(MetricOptions.builder()
                                                .label("cpu")
                                                .statistic("Average")
                                                .period(dashboardPeriod)
                                                .unit(Unit.PERCENT)
                                                .build()),
                                        service.getService().metricMemoryUtilization(MetricOptions.builder()
                                                .label("memory")
                                                .statistic("Average")
                                                .period(dashboardPeriod)
                                                .unit(Unit.PERCENT)
                                                .build())))
                                .leftYAxis(YAxisProps.builder()
                                        .label("percent")
                                        .min(0)
                                        .max(100)
                                        .showUnits(false)
                                        .build())
                                .period(dashboardPeriod)
                                .build())))
                .build();

        CfnOutput.Builder.create(this, "ApiUrl")
                .description("Primary REST API Gateway URL")
                .value(restApiUrl(restApi))
                .build();

        CfnOutput.Builder.create(this, "RestApiAccessLogGroupName")
                .description("REST API Gateway access log group")
                .value(restApiAccessLogGroup.getLogGroupName())
                .build();

        CfnOutput.Builder.create(this, "AlbDnsName")
                .description("Internal ALB DNS name")
                .value(service.getLoadBalancer().getLoadBalancerDnsName())
                .build();
    }

    private static MetricOptions percentileOptions(String percentile, Duration period) {
        return MetricOptions.builder()
                .label(percentile)
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

    private static Map<String, String> appTracingEnvironment() {
        return Map.ofEntries(
                Map.entry("JAVA_TOOL_OPTIONS", "-javaagent:/opt/aws-opentelemetry-agent.jar"),
                Map.entry("OTEL_SERVICE_NAME", "prod-core-service"),
                Map.entry("OTEL_RESOURCE_ATTRIBUTES", "service.namespace=prod-core,deployment.environment.name=learning"),
                Map.entry("OTEL_TRACES_EXPORTER", "otlp"),
                Map.entry("OTEL_METRICS_EXPORTER", "none"),
                Map.entry("OTEL_LOGS_EXPORTER", "none"),
                Map.entry("OTEL_EXPORTER_OTLP_ENDPOINT", "http://127.0.0.1:4317"),
                Map.entry("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc"),
                Map.entry("OTEL_PROPAGATORS", "xray,tracecontext,baggage"),
                Map.entry("OTEL_TRACES_SAMPLER", "parentbased_traceidratio"),
                Map.entry("OTEL_TRACES_SAMPLER_ARG", "1.0"));
    }

    private static String adotCollectorConfig() {
        return String.join("\n", List.of(
                "receivers:",
                "  otlp:",
                "    protocols:",
                "      grpc:",
                "        endpoint: 0.0.0.0:4317",
                "      http:",
                "        endpoint: 0.0.0.0:4318",
                "processors:",
                "  batch/traces:",
                "exporters:",
                "  awsxray:",
                "service:",
                "  pipelines:",
                "    traces:",
                "      receivers: [otlp]",
                "      processors: [batch/traces]",
                "      exporters: [awsxray]"));
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

    private static CfnMethod.IntegrationProperty restPrivateIntegration(
            String uri,
            String vpcLinkId,
            String loadBalancerArn) {
        return CfnMethod.IntegrationProperty.builder()
                .type("HTTP_PROXY")
                .integrationHttpMethod("ANY")
                .uri(uri)
                .connectionType("VPC_LINK")
                .connectionId(vpcLinkId)
                .integrationTarget(loadBalancerArn)
                .timeoutInMillis(29_000)
                .build();
    }

    private static CfnMethod.IntegrationProperty restProxyPrivateIntegration(
            String uri,
            String vpcLinkId,
            String loadBalancerArn) {
        return CfnMethod.IntegrationProperty.builder()
                .type("HTTP_PROXY")
                .integrationHttpMethod("ANY")
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
