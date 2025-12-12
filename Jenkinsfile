#!groovy

@Library('cib-pipeline-library@master') _

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
	office365WebhookId: Constants.OFFICE_365_CIBSEVEN_WEBHOOK_ID,
    primaryBranch: 'PR-719',
    dependencyTrackSynchronous: true,
    coverageLcovPattern: 'frontend/target/coverage/lcov.info',
    uiParamPresets: [:],
    testMode: false,
    buildPodConfig: [
        (Constants.MAVEN_JDK_17_CONTAINER): [
            resources: [
                cpu: '4',
                memory: '8Gi',
                ephemeralStorage: '8Gi'
            ]
        ]
    ]
]

// Shared function for npm package release
def npmReleasePackage(String packageDir, String npmrcFile) {
    // Read the version from package.json to determine if it's a dev version
    def packageVersion = sh(
        script: "grep '\"version\"' ${packageDir}/package.json | cut -d'\"' -f4",
        returnStdout: true
    ).trim()
    def isDevVersion = packageVersion.contains('-dev')
    def mavenTagArg = isDevVersion ? "-Dnpm.publish.tag.arg=' --tag dev'" : ""

    sh """
        # Copy the .npmrc file to the package directory
        echo "Copying .npmrc file to ${packageDir} directory..."
        cp ${npmrcFile} ./${packageDir}/.npmrc
        
        echo "Current package.json version:"
        grep '"version"' ${packageDir}/package.json
        
        echo "Running Maven to release the npm package..."
        mvn -T4 \\
            -Dbuild.number=${BUILD_NUMBER} \\
            -Drelease-npm-library=${packageDir} \\
            -Dskip.npm.version.update=true \\
            ${mavenTagArg} \\
            clean generate-resources
    """
}

pipeline {
    agent {
        kubernetes {
            yaml BuildPodCreator.cibStandardPod(nodepool: Constants.NODEPOOL_STABLE)
                    .withContainerFromName(pipelineParams.mvnContainerName, pipelineParams.buildPodConfig[pipelineParams.mvnContainerName])
                    .withHelm3Container()
                    .withNode20Container()
                    .asYaml()
            defaultContainer pipelineParams.mvnContainerName
        }
    }

    // Parameter that can be changed in the Jenkins UI
    parameters {
        booleanParam(
            name: 'VERIFY',
            defaultValue: true,
            description: '''Build and verify the project. This includes:
            - Build and test using "mvn verify" (all branches)
            - Run SonarQube Checks on primary branch
            - Run Dependency-Track upload on primary branch
            '''
        )
        booleanParam(
            name: 'RELEASE_BPM_SDK',
            defaultValue: false,
            description: 'Build and deploy bpm-sdk to artifacts.cibseven.org'
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

        stage('Maven verify') {
            when {
                expression { params.VERIFY }
            }
            steps {
                script {
                    withMaven(options: [junitPublisher(disabled: false), jacocoPublisher(disabled: false)]) {
                        sh "mvn -T4 -Dbuild.number=${BUILD_NUMBER} clean verify"
                    }
                    if (!params.DEPLOY_TO_MAVEN_CENTRAL) {
                        junit allowEmptyResults: true, testResults: ConstantsInternal.MAVEN_TEST_RESULTS

                        // Show coverage in Jenkins UI
                        recordCoverage(
                            tools: [[parser: 'COBERTURA', pattern: 'frontend/target/coverage/cobertura-coverage.xml']],
                            sourceCodeRetention: 'LAST_BUILD',
                            sourceDirectories: [[path: 'frontend/src']]
                        )

                        // This archives the whole HTML coverage report so you can download or view it from Jenkins
                        // This archives the Vitest test reports so you can download or view them from Jenkins
                        archiveArtifacts artifacts: 'frontend/target/coverage/lcov-report/**, frontend/target/vitest-reports/**, cibseven-webclient-core/target/failsafe-reports/**', allowEmptyArchive: false, fingerprint: true
                    }
                }
            }
        }

        stage('Run SonarQube Checks') {
            when {
                allOf {
                    branch pipelineParams.primaryBranch
                    expression { params.VERIFY == true }
                }
            }
            steps {
                script {
                    container(Constants.NODE_20_CONTAINER) {
                        withSonarQubeEnv(credentialsId: Constants.SONARQUBE_CREDENTIALS_ID, installationName: 'SonarQube') {
                            script {
                                // Install sonar-scanner
                                sh '''
                                    echo "Installing sonarqube-scanner ..."
                                    cd ./frontend
                                    npm install -g sonarqube-scanner@4.3.2 --ignore-scripts
                                '''

                                // Read version from package.json
                                def packageJson = readJSON file: './frontend/package.json'
                                env.VERSION = packageJson.version
                                env.PACKAGE_NAME = packageJson.name
                                env.DESCRIPTION = packageJson.description

                                if (env.VERSION.isEmpty()) {
                                    error("Could not find version in package.json")
                                }

                                // Sanitize project key: SonarQube only allows alphanumeric, '-', '_', '.' and ':' characters
                                // Replace '@' and '/' with '-' to create a valid project key
                                def sanitizedProjectKey = env.PACKAGE_NAME.replaceAll('@', '').replaceAll('/', '-')

                                // Run SonarQube analysis
                                sh """
                                    cd ./frontend
                                    sonar-scanner \
                                        -Dsonar.projectKey=${sanitizedProjectKey} \
                                        -Dsonar.projectName=${env.PACKAGE_NAME} \
                                        -Dsonar.projectVersion=${env.VERSION} \
                                        -Dsonar.sources=src \
                                        -Dsonar.exclusions='**/node_modules/**,**/dist/**,**/build/**,**/*.min.js' \
                                        -Dsonar.javascript.lcov.reportPaths=${pipelineParams.coverageLcovPattern} \
                                        -Dsonar.coverage.exclusions='**/*.test.js,**/*.spec.js,**/*.test.ts,**/*.spec.ts'
                                """
                            }
                        }
                        script {
                            timeout(time: 5, unit: 'MINUTES') {
                                def qg = waitForQualityGate()
                                if (qg.status != 'OK') {
                                    log.info "Pipeline unstable due to quality gate failure: ${qg.status}"
                                    currentBuild.result = 'UNSTABLE'
                                }
                            }
                        }
                    }
                }
            }
        }
        
        stage('OWASP Dependency-Track') {
            when {
                allOf {
                    branch pipelineParams.primaryBranch
                    expression { params.VERIFY == true }
                }
            }
            steps {
                script {
                    container(Constants.NODE_20_CONTAINER) {
                        script {

                            sh """
                                cd ./frontend
                                npm install --global @cyclonedx/cyclonedx-npm@4.1.0 --ignore-scripts
                                cyclonedx-npm --output-file bom.xml --output-format XML
                            """

                            // Read version from package.json
                            def packageJson = readJSON file: './frontend/package.json'
                            env.VERSION = packageJson.version
                            env.PACKAGE_NAME = packageJson.name
                            env.DESCRIPTION = packageJson.description

                            if (env.VERSION.isEmpty()) {
                                error("Could not find version in package.json")
                            }

                            withCredentials([string(credentialsId: Constants.DEPENDENCY_TRACK_CREDENTIALS_ID, variable: 'API_KEY')]) {
                                dependencyTrackPublisher(
                                    autoCreateProjects: true,
                                    artifact: 'frontend/bom.xml',
                                    projectName: "${env.PACKAGE_NAME}",
                                    projectVersion: "${env.VERSION}",
                                    projectProperties: [
                                        description: "${env.DESCRIPTION}",
                                        tags: ['npm'],
                                        isLatest: true
                                    ],
                                    synchronous: pipelineParams.dependencyTrackSynchronous,
                                    failOnViolationFail: false,
                                    dependencyTrackApiKey: API_KEY
                                )
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy to artifacts.cibseven.org') {
            when {
                anyOf {
                    allOf {
                        // Automatically deploy on main branch if version is SNAPSHOT
                        branch 'main'
                        expression { mavenProjectInformation.version.endsWith("-SNAPSHOT") == true }
                        expression { !params.DEPLOY_TO_MAVEN_CENTRAL }
                    }
                    allOf {
                        expression { params.DEPLOY_TO_ARTIFACTS }
                        expression { !params.DEPLOY_TO_MAVEN_CENTRAL }
                    }
                }
            }
            steps {
                script {
                    String deployment = ""
                    if (isPatchVersion()) {
                        if (isSNAPSHOTVersion()) {
                            deployment = "-Dnexus.snapshot.repository.id=mvn-cibseven-private -Dnexus.snapshot.repository=https://artifacts.cibseven.de/repository/private-snapshots"
                        } else {
                            deployment = "-Dnexus.release.repository.id=mvn-cibseven-private -Dnexus.release.repository=https://artifacts.cibseven.de/repository/private"
                        }
                    }

                    withMaven(options: []) {
                        def skipTestsFlag = params.VERIFY ? "-DskipTests" : ""
                        sh "mvn -T4 -U clean deploy ${skipTestsFlag} ${deployment}"
                    }

                    if (!params.VERIFY) {
                        junit allowEmptyResults: true, testResults: ConstantsInternal.MAVEN_TEST_RESULTS

                        // Show coverage in Jenkins UI
                        recordCoverage(
                            tools: [[parser: 'COBERTURA', pattern: 'frontend/target/coverage/cobertura-coverage.xml']],
                            sourceCodeRetention: 'LAST_BUILD',
                            sourceDirectories: [[path: 'frontend/src']]
                        )

                        // This archives the whole HTML coverage report so you can download or view it from Jenkins
                        archiveArtifacts artifacts: 'frontend/target/coverage/lcov-report/**', allowEmptyArchive: false
                    }
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
                        withCredentials([file(credentialsId: 'credential-cibseven-gpg-private-key', variable: 'GPG_KEY_FILE'), string(credentialsId: 'credential-cibseven-gpg-passphrase', variable: 'GPG_KEY_PASS')]) {
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

        stage('Release bpm-sdk') {
            when {
                allOf {
                    expression { params.RELEASE_BPM_SDK }
                }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'credential-cibseven-artifacts-npmrc', variable: 'NPMRC_FILE')]) {
                        withMaven() {
                            npmReleasePackage('bpm-sdk', env.NPMRC_FILE)
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
                            npmReleasePackage('frontend', env.NPMRC_FILE)
                        }
                    }
                }
            }
        }
        
        stage('Create & Push Docker Image') {
            when {
                anyOf {
                    allOf {
                        branch 'main'
                        expression { mavenProjectInformation.version.endsWith("-SNAPSHOT") == true }
                    }
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
                    allOf {
                        branch 'main'
                        expression { mavenProjectInformation.version.endsWith("-SNAPSHOT") == true }
                    }
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

				if (params.RELEASE_BPM_SDK) {
					notifyResult(
                        office365WebhookId: pipelineParams.office365WebhookId,
                        message: "✅ bpm-sdk was successfully released to artifacts.cibseven.org with version ${mavenProjectInformation.version}"
                    )
				}

				if (params.RELEASE_CIBSEVEN_COMPONENTS) {
					notifyResult(
                        office365WebhookId: pipelineParams.office365WebhookId,
                        message: "✅ cibseven-components was successfully released to artifacts.cibseven.org with version ${mavenProjectInformation.version}"
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
                if (env.BRANCH_NAME == 'main') {
                    notifyResult(
                        office365WebhookId: pipelineParams.office365WebhookId,
                        message: "❌ Build failed on main branch. Access build info at ${env.BUILD_URL}"
                    )
                }
            }
        }

        fixed {
            script {
                log.info '✅ Previous issues fixed'
                if (env.BRANCH_NAME == 'main') {
                    notifyResult(
                        office365WebhookId: pipelineParams.office365WebhookId,
                        message: "✅ Previous issues on main branch fixed. Access build info at ${env.BUILD_URL}"
                    )
                }
            }
        }
    }
}

// - "1.2.0" -> no
// - "1.2.0-SNAPSHOT" -> no
// - "1.2.3" -> yes
// - "1.2.3-SNAPSHOT" -> yes
// - "7.22.0-cibseven" -> no
// - "7.22.1-cibseven" -> yes
def isPatchVersion() {
    List version = mavenProjectInformation.version.tokenize('.')
    if (version.size() < 3) {
        return false
    }
    return version[2].tokenize('-')[0] != "0"
}

def isSNAPSHOTVersion() {
    return mavenProjectInformation.version.endsWith("-SNAPSHOT")
}

