#!groovy

@Library('cib-pipeline-library') _

import de.cib.pipeline.library.Constants
import de.cib.pipeline.library.kubernetes.BuildPodCreator
import de.cib.pipeline.library.logging.Logger
import de.cib.pipeline.library.ConstantsInternal
import de.cib.pipeline.library.MavenProjectInformation
import de.cib.pipeline.library.helm.HelmChartInformation
import groovy.transform.Field

@Field Logger log = new Logger(this)
@Field MavenProjectInformation mavenProjectInformation = null
@Field Map pipelineParams = [
    pom: ConstantsInternal.DEFAULT_MAVEN_POM_PATH,
    mvnContainerName: Constants.MAVEN_JDK_17_CONTAINER,
    uiParamPresets: [:],
    testMode: false
]

pipeline {
    agent {
        kubernetes {
            yaml BuildPodCreator.cibStandardPod()
                    .withContainerFromName(pipelineParams.mvnContainerName)
                    .withHelm3Container()
                    .asYaml()
            defaultContainer pipelineParams.mvnContainerName
        }
    }

    // Parameter that can be changed in the Jenkins UI
    parameters {
        booleanParam(
            name: 'INSTALL',
            defaultValue: true,
            description: 'Build and test'
        )
        booleanParam(
            name: 'RELEASE_COMMON_COMPONENTS',
            defaultValue: false,
            description: 'Build and deploy cib-common-components to artifacts.cibseven.org'
        )
        booleanParam(
            name: 'RELEASE_CIBSEVEN_COMPONENTS',
            defaultValue: false,
            description: 'Build and deploy cibseven-components to artifacts.cibseven.org'
        )
        booleanParam(
            name: 'DEPLOY_TO_ARTIFACTS',
            defaultValue: false,
            description: 'Deploy artifacts to artifacts.cibseven.org'
        )
        booleanParam(
            name: 'DEPLOY_TO_MAVEN_CENTRAL',
            defaultValue: false,
            description: 'Deploy artifacts to Maven Central'
        )
        booleanParam(
			name: 'DEPLOY_ANY_BRANCH_TO_HARBOR',
			defaultValue: false,
			description: 'Deploy any branch to harbor'
		)
    }

    options {
        buildDiscarder(
            logRotator(
                // number of build logs to keep
                numToKeepStr:'5',
                // history to keep in days
                daysToKeepStr: '15',
                // artifacts are kept for days
                artifactDaysToKeepStr: '15',
                // number of builds have their artifacts kept
                artifactNumToKeepStr: '5'
            )
        )
        // Stop build after 240 minutes
        timeout(time: 240, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {
        stage('Print Settings & Checkout') {
            steps {
                script {
                    printSettings()

                    def pom = readMavenPom file: pipelineParams.pom

                    // for overlays often no groupId is set as the parent groupId is used
                    def groupId = pom.groupId
                    if (groupId == null) {
                        groupId = pom.parent.groupId
                        log.info "parent groupId is used"
                    }

                    mavenProjectInformation = new MavenProjectInformation(groupId, pom.artifactId, pom.version, pom.name, pom.description)

                    log.info "Build Project: ${mavenProjectInformation.groupId}:${mavenProjectInformation.artifactId}, ${mavenProjectInformation.name} with version ${mavenProjectInformation.version}"

                    // Avoid Git "dubious ownership" error in checked out repository. Needed in
                    // build containers with newer Git versions. Originates from Jenkins running
                    // pipeline as root but repository being owned by user 1000. For more, see
                    // https://stackoverflow.com/questions/72978485/git-submodule-update-failed-with-fatal-detected-dubious-ownership-in-repositor
                    sh "git config --global --add safe.directory \$(pwd)"
                }
            }
        }

        stage('Maven install') {
            when {
                expression { params.INSTALL }
            }
            steps {
                script {
                    withMaven(options: [junitPublisher(disabled: false), jacocoPublisher(disabled: false)]) {
                        sh "mvn -T4 -Dbuild.number=${BUILD_NUMBER} install"
                    }
                    if (!params.DEPLOY_TO_ARTIFACTS && !params.DEPLOY_TO_MAVEN_CENTRAL) {
                        junit allowEmptyResults: true, testResults: ConstantsInternal.MAVEN_TEST_RESULTS

                        // Show coverage in Jenkins UI
                        recordCoverage(
                            tools: [[parser: 'COBERTURA', pattern: 'frontend/coverage/cobertura-coverage.xml']],
                            sourceCodeRetention: 'LAST_BUILD',
                            sourceDirectories: [[path: 'frontend/src']]
                        )

                        // This archives the whole HTML coverage report so you can download or view it from Jenkins
                        archiveArtifacts artifacts: 'frontend/coverage/lcov-report/**', allowEmptyArchive: false
                    }
                }
            }
        }

        stage('Deploy to artifacts.cibseven.org') {
            when {
                allOf {
                    expression { params.DEPLOY_TO_ARTIFACTS }
                    expression { !params.DEPLOY_TO_MAVEN_CENTRAL }
                }
            }
            steps {
                script {
                    withMaven(options: []) {
                        sh "mvn -T4 -U clean deploy"
                    }
                  
                    junit allowEmptyResults: true, testResults: ConstantsInternal.MAVEN_TEST_RESULTS
                }
            }
        }
        
        stage('Deploy to Maven Central') {
            when {
                allOf {
                    expression { params.DEPLOY_TO_MAVEN_CENTRAL }
                    expression { mavenProjectInformation.version.endsWith("-SNAPSHOT") == false }
                }
            }
            steps {
                script {
                    withMaven(options: []) {
                        withCredentials([file(credentialsId: 'credential-cibseven-community-gpg-private-key', variable: 'GPG_KEY_FILE'), string(credentialsId: 'credential-cibseven-community-gpg-passphrase', variable: 'GPG_KEY_PASS')]) {
                            sh "gpg --batch --import ${GPG_KEY_FILE}"
    
                            def GPG_KEYNAME = sh(script: "gpg --list-keys --with-colons | grep pub | cut -d: -f5", returnStdout: true).trim()

                            sh """
                                mvn -T4 -U \
                                    -Dgpg.keyname="${GPG_KEYNAME}" \
                                    -Dgpg.passphrase="${GPG_KEY_PASS}" \
                                    clean deploy \
                                    -Psonatype-oss-release \
                                    -Dskip.cibseven.release="${!params.DEPLOY_TO_ARTIFACTS}"
                            """
                        }
                    }

                    junit allowEmptyResults: true, testResults: ConstantsInternal.MAVEN_TEST_RESULTS
                }
            }
        }

        stage('Release cib-common-components') {
            when {
                allOf {
                    expression { params.RELEASE_COMMON_COMPONENTS }
                }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'credential-cibseven-artifacts-npmrc', variable: 'NPMRC_FILE')]) {
                        withMaven() {
                            sh """
                                # Copy the .npmrc file to the frontend directory
                                cp ${NPMRC_FILE} ./cib-common-components/.npmrc
                                # Run Maven with the required profile
                                mvn -T4 -Dbuild.number=${BUILD_NUMBER} clean generate-resources -Drelease-npm-library=cib-common-components -Dskip.npm.version.update=true
                            """
                        }
                    }
                }
            }
        }

        stage('Release cibseven-components') {
            when {
                allOf {
                    expression { params.RELEASE_CIBSEVEN_COMPONENTS }
                }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'credential-cibseven-artifacts-npmrc', variable: 'NPMRC_FILE')]) {
                        withMaven() {
                            def pom = readMavenPom file: 'pom.xml'
                            def baseVersion = pom.version.replace("-SNAPSHOT", "")
                            def dynamicVersion = "${baseVersion}-${BUILD_NUMBER}-SNAPSHOT"

                            sh """
                                echo "Copy the .npmrc file to the frontend directory..."
                                cp ${NPMRC_FILE} ./frontend/.npmrc

                                echo "Setting dynamic version to ${dynamicVersion}..."
                                sed -i 's/__CI_VERSION__/${dynamicVersion}/' frontend/package.json

                                echo "Final package.json version:"
                                grep '"version"' frontend/package.json

                                echo "Running Maven to release the npm package..."
                                mvn -T4 \
                                    -Dbuild.number=${BUILD_NUMBER} \
                                    -Drelease-npm-library=frontend \
                                    -Dskip.npm.version.update=true \
                                    clean generate-resources
                            """
                        }
                    }
                }
            }
        }
        
        stage('Create & Push Docker Image') {
            when {
                anyOf {
                    branch 'main'
                    expression { params.DEPLOY_ANY_BRANCH_TO_HARBOR == true }
                }
            }
            steps {
                script {
                    withMaven() {
                        // "package" before jib:build is needed to support maven multi module projects
                        // see https://github.com/GoogleContainerTools/jib/tree/master/examples/multi-module
                        sh """
                            mvn -f ./pom.xml \
                                package \
                                jib:build \
                                -Dmaven.test.skip \
                                -DskipTests \
                                -Dlicense.skipDownloadLicenses=true \
                                -T4 \
                                -Dbuild.number=${BUILD_NUMBER}
                        """
                    }
                    //TODO SBOM needed?
//                    if (params.RELEASE_BUILD) {
//                        log.info 'Generating and uploading SBOM for image due to release build'
//                        container(Constants.SYFT_CONTAINER) {
//                            withCredentials([string(credentialsId: Constants.DEPENDENCY_TRACK_CREDENTIALS_ID, variable: 'API_KEY')]) {
//                                def files = findFiles(glob: '**/target/jib-image.json')
//                                files.each { file ->
//                                    String image = readJSON(file: file.path).image
//                                    sh "syft ${image} -o cyclonedx-xml=syft-bom.xml -v"
//                                    String[] imageSplit = image.split(':')
//                                    String imageName = imageSplit[0].split('/')[-1]
//                                    String imageVersion = imageSplit[-1]
//                                    dependencyTrackPublisher(
//                                        artifact: 'syft-bom.xml',
//                                        // Add suffix '-image' so previously uploaded bom for java artifact is not
//                                        // overwritten in dependency-track
//                                        projectName: imageName + '-image',
//                                        projectVersion: imageVersion,
//                                        projectProperties: [
//                                            description: mavenProjectInformation.description,
//                                            group: mavenProjectInformation.groupId,
//                                            tags: ['jib']
//                                        ],
//                                        synchronous: false,
//                                        dependencyTrackApiKey: API_KEY
//                                    )
//                                    sh 'rm -f syft-bom.xml'
//                                }
//                            }
//                        }
//                    } else {
//                        log.info 'Skipping SBOM generation and upload for image'
//                    }
                }
            }
        }
        
        stage('Deploy Helm Charts to Harbor') {
	        when {
                anyOf {
                    branch 'main'
                    expression { params.DEPLOY_ANY_BRANCH_TO_HARBOR }
                }
	        }
	        steps {
	            script {
                    HelmChartInformation helmChartInformation = readHelmChart(path: 'helm/cibseven-webclient')
                    helmChartInformation.setUploadVersion(mavenProjectInformation.version)
                    helmChartInformation.setUploadAppVersion(mavenProjectInformation.version)
                    deployHelmChart(
                        helmChartInformation: helmChartInformation,
                        updateDependencies: true,
                        runChecks: true,
                        dryRun: false
                    )
	            }
	        }
	    }
    }

    post {
        always {
            script {
                log.info 'End of the build'
            }
        }

        success {
            script {
                log.info '✅ Build successful'
                if (params.RELEASE_BUILD == true) {
                    notifyResult(
                        office365WebhookId: pipelineParams.office365WebhookId,
                        message: "Application was successfully released with version ${mavenProjectInformation.version}"
                    )
                }
            }
        }

        unstable {
            script {
                log.warning '⚠️ Build unstable'
            }
        }

        failure {
            script {
                log.warning '❌ Build failed'
                if (env.BRANCH_NAME == 'master') {
                    notifyResult(
                        office365WebhookId: pipelineParams.office365WebhookId,
                        message: "Access build info at ${env.BUILD_URL}"
                    )
                }
            }
        }

        fixed {
            script {
                log.info '✅ Previous issues fixed'
                if (env.BRANCH_NAME == 'master') {
                    notifyResult(
                        office365WebhookId: pipelineParams.office365WebhookId,
                        message: "Access build info at ${env.BUILD_URL}"
                    )
                }
            }
        }
    }
}
