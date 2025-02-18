#!groovy

@Library('cib-pipeline-library') _

import de.cib.pipeline.library.Constants

standardMavenPipeline(
    mvnParams: '-U',
    uiParamPresets: [
        // The Docker image is created in the custom stage with its own Maven profile
        'CREATE_DOCKER_IMAGE': true,
        'DEPLOY_HELM_CHARTS_TO_HARBOR': true
    ],
    helmChartPaths: ['helm/seven-webclient'],
    office365WebhookId: Constants.OFFICE_365_FLOW_WEBHOOK_ID,
    mvnContainerName: Constants.MAVEN_JDK_21_CONTAINER
)
