#!groovy

@Library('cib-pipeline-library') _

import de.cib.pipeline.library.Constants

/*
 * Custom stage which will build only the Docker image with a different maven profile 'deploy-k8s'.
 * The rest of the pipeline (see below) will use the 'deploy' profile so the produced artifacts in
 * Nexus can be used with the old VM-environments for dev, test, etc., where some jackson libraries
 * are already provided and thus not packaged
 */
def customCreateDockerImage = {
    if (env.BRANCH_NAME == 'master' || params.DEPLOY_ANY_BRANCH_TO_REPOSITORY) {
        withMaven() {
            // Note that we *must* use goal 'package' here - 'compile' is not enough as the jib
            // plugin will then simply take the artifacts built beforehand with profile 'deploy'
            sh '''
                mvn clean package jib:build \
                    -Pdeploy-k8s \
                    -Dmaven.test.skip \
                    -DskipTests \
                    -Dlicense.skipDownloadLicenses=true \
                    -U
            '''
        }
    } else {
        echo '[INFO] Skipping custom stage for building docker image...'
    }
}

standardMavenPipeline(
    mvnParams: '-Pdeploy -U',
    uiParamPresets: [
        // The Docker image is created in the custom stage with its own Maven profile
        'CREATE_DOCKER_IMAGE': false,
        'DEPLOY_HELM_CHARTS_TO_HARBOR': false
    ],
    helmChartPaths: ['helm/seven-webclient'],
    office365WebhookId: Constants.OFFICE_365_FLOW_WEBHOOK_ID,
    mvnContainerName: Constants.MAVEN_JDK_21_CONTAINER
)
