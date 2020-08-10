import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2020.1"

project {

    vcsRoot(WpCalypso)

    buildType(prepare_calypso_live)
    buildType(run_woo_e2e_tests)
    buildType(build_o2_blocks)
    buildType(run_client_tests)
    buildType(run_packages_tests)
    buildType(aggregate_results)
    buildType(run_server_tests)
    buildType(build_wpcom_block_editor)
    buildType(_run_fse_tests)
    buildType(build_notifications)
    buildType(run_ie11_e2e_tests)
    buildType(run_safari_tests)
    buildType(typecheck)

    template(run_e2e)
    template(build_app)
    template(run_unit_tests)

    params {
        param("env.NODE_OPTIONS", "--max-old-space-size=12288")
        text("env.E2E_WORKERS", "7", label = "Magellan parallel workers", description = "Number of parallel workers in Magellan (e2e tests)", allowEmpty = true)
        text("env.JEST_MAX_WORKERS", "6", label = "Jest max workers", description = "How many tests run in parallel", allowEmpty = true)
        password("env.CONFIG_KEY", "credentialsJSON:16d15e36-f0f2-4182-8477-8d8072d0b5ec", label = "Config key", description = "Key used to decrypt config")
        text("env.CHILD_CONCURRENCY", "5", label = "Yarn child concurrency", description = "How many packages yarn builds in parallel", allowEmpty = true)
        text("docker_image", "automattic/wp-calypso-ci:1.0.9", label = "Docker image", description = "Docker image to use for the run", allowEmpty = true)
    }

    features {
        dockerRegistry {
            id = "PROJECT_EXT_6"
            name = "Docker Registry"
            url = "https://registry.a8c.com"
        }
    }
}

object _run_fse_tests : BuildType({
    templates(run_unit_tests)
    name = "Run FSE tests"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_13"
            scriptContent = """
                set -e
                export JEST_JUNIT_OUTPUT_DIR="../../test_results"
                export JEST_JUNIT_OUTPUT_NAME="results.xml"
                export HOME="/calypso"
                export NODE_ENV="test"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export npm_config_cache=${'$'}(yarn cache dir)
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                cd apps/full-site-editing
                yarn test:js --reporters=default --reporters=jest-junit  --maxWorkers=${'$'}JEST_MAX_WORKERS
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }

    features {
        feature {
            id = "BUILD_EXT_364"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "test_results/**/*.xml")
        }
    }
})

object aggregate_results : BuildType({
    name = "Aggregate results"

    type = BuildTypeSettings.Type.COMPOSITE

    vcs {
        showDependenciesChanges = true
    }

    dependencies {
        snapshot(_run_fse_tests) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(build_notifications) {
        }
        snapshot(build_o2_blocks) {
        }
        snapshot(build_wpcom_block_editor) {
        }
        snapshot(prepare_calypso_live) {
        }
        snapshot(run_client_tests) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(run_ie11_e2e_tests) {
        }
        snapshot(run_packages_tests) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(run_safari_tests) {
        }
        snapshot(run_server_tests) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        snapshot(run_woo_e2e_tests) {
        }
        snapshot(typecheck) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }
})

object build_notifications : BuildType({
    templates(build_app)
    name = "Build notifications"

    enablePersonalBuilds = false
    artifactRules = "artifacts/notifications"
    maxRunningBuilds = 1
    publishArtifacts = PublishMode.SUCCESSFUL

    steps {
        script {
            name = "Build notifications"
            id = "RUNNER_360"
            scriptContent = """
                set -e
                export JEST_JUNIT_OUTPUT_DIR="./test_results/client"
                export JEST_JUNIT_OUTPUT_NAME="results.xml"
                export HOME="/calypso"
                export NODE_ENV="production"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export npm_config_cache=${'$'}(yarn cache dir)
                export ARTIFACTS="${'$'}{PWD}/artifacts/notifications"
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Build notifications
                cd apps/notifications/
                yarn build --output-path=${'$'}ARTIFACTS
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }
})

object build_o2_blocks : BuildType({
    templates(build_app)
    name = "Build o2-blocks"

    enablePersonalBuilds = false
    artifactRules = "artifacts/o2-blocks"
    maxRunningBuilds = 1
    publishArtifacts = PublishMode.SUCCESSFUL

    steps {
        script {
            name = "Build o2-blocks"
            id = "RUNNER_360"
            scriptContent = """
                set -e
                export JEST_JUNIT_OUTPUT_DIR="./test_results/client"
                export JEST_JUNIT_OUTPUT_NAME="results.xml"
                export HOME="/calypso"
                export NODE_ENV="production"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export npm_config_cache=${'$'}(yarn cache dir)
                export ARTIFACTS="${'$'}{PWD}/artifacts/o2-blocks"
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Build wpcom-block-editor
                cd apps/o2-blocks/
                yarn build --output-path=${'$'}ARTIFACTS
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }
})

object build_wpcom_block_editor : BuildType({
    templates(build_app)
    name = "Build wpcom-block-editor"

    enablePersonalBuilds = false
    artifactRules = "artifacts/wpcom-block-editor"
    maxRunningBuilds = 1
    publishArtifacts = PublishMode.SUCCESSFUL

    steps {
        script {
            name = "Build wpcom-block-editor"
            id = "RUNNER_360"
            scriptContent = """
                set -e
                export JEST_JUNIT_OUTPUT_DIR="./test_results/client"
                export JEST_JUNIT_OUTPUT_NAME="results.xml"
                export HOME="/calypso"
                export NODE_ENV="production"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export npm_config_cache=${'$'}(yarn cache dir)
                export ARTIFACTS="${'$'}{PWD}/artifacts/wpcom-block-editor"
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Build wpcom-block-editor
                cd apps/wpcom-block-editor/
                yarn build --output-path=${'$'}ARTIFACTS
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }
})

object prepare_calypso_live : BuildType({
    name = "Prepare calypso live"

    vcs {
        root(WpCalypso)
    }

    steps {
        script {
            name = "Prime calypso live"
            scriptContent = """
                set -x
                set -e
                export sha=${'$'}(git rev-parse HEAD)
                curl "https://calypso.live/?hash=${'$'}{sha}"
                ./test/e2e/scripts/wait-for-running-branch.sh
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }
})

object run_client_tests : BuildType({
    templates(run_unit_tests)
    name = "Run client tests"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_13"
            scriptContent = """
                set -e
                export JEST_JUNIT_OUTPUT_DIR="./test_results/client"
                export JEST_JUNIT_OUTPUT_NAME="results.xml"
                export HOME="/calypso"
                export NODE_ENV="test"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export npm_config_cache=${'$'}(yarn cache dir)
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                yarn test-client --maxWorkers=${'$'}JEST_MAX_WORKERS --ci --reporters=default --reporters=jest-junit --silent
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "automattic/wp-calypso-ci:1.0.5"
            dockerRunParameters = "-u %env.UID%"
        }
    }

    features {
        feature {
            id = "BUILD_EXT_362"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "test_results/**/*.xml")
        }
    }
})

object run_ie11_e2e_tests : BuildType({
    templates(run_e2e)
    name = "Run e2e tests - Canary IE11"

    artifactRules = "artifacts"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_363"
            scriptContent = """
                set -e
                
                export NODE_CONFIG_ENV=test
                export BRANCHNAME=master
                export HIGHLIGHT_ELEMENT=true
                export sha=${'$'}(git rev-parse HEAD)
                export HOME="/calypso"
                export npm_config_cache=${'$'}(yarn cache dir)
                export TARGET="IE11"
                export JETPACKHOST=''
                export ARTIFACTS="${'$'}{PWD}/artifacts"
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                cd  ./test/e2e
                yarn run decryptconfig          
                ./run.sh -a ${'$'}E2E_WORKERS -R -z -S ${'$'}sha
                
                # Save artifacts
                mkdir -p "${'$'}ARTIFACTS"
                cp -r ./reports "${'$'}{ARTIFACTS}/e2ereports"
                cp -r ./screenshots "${'$'}{ARTIFACTS}/screenshots"
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }

    features {
        feature {
            id = "BUILD_EXT_362"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "artifacts/**/*.xml")
            param("xmlReportParsing.verboseOutput", "true")
        }
    }

    dependencies {
        snapshot(prepare_calypso_live) {
        }
    }
})

object run_packages_tests : BuildType({
    templates(run_unit_tests)
    name = "Run packages tests"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_323"
            scriptContent = """
                set -e
                export JEST_JUNIT_OUTPUT_DIR="./test_results"
                export JEST_JUNIT_OUTPUT_NAME="results.xml"
                export HOME="/calypso"
                export NODE_ENV="test"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export npm_config_cache=${'$'}(yarn cache dir)
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                yarn test-packages --maxWorkers=${'$'}JEST_MAX_WORKERS --ci --reporters=default --reporters=jest-junit --silent
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }

    features {
        feature {
            id = "BUILD_EXT_370"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "test_results/**/*.xml")
        }
    }
})

object run_safari_tests : BuildType({
    templates(run_e2e)
    name = "Run e2e tests - Canary Safari"

    artifactRules = "artifacts"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_363"
            scriptContent = """
                set -e
                
                export NODE_CONFIG_ENV=test
                export BRANCHNAME=master
                export HIGHLIGHT_ELEMENT=true
                export sha=${'$'}(git rev-parse HEAD)
                export HOME="/calypso"
                export npm_config_cache=${'$'}(yarn cache dir)
                export TARGET=''
                export JETPACKHOST=''
                export ARTIFACTS="${'$'}{PWD}/artifacts"
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                cd  ./test/e2e
                yarn run decryptconfig
                ./run.sh -a ${'$'}E2E_WORKERS -R -y -S ${'$'}sha
                
                # Save artifacts
                mkdir -p "${'$'}ARTIFACTS"
                cp -r ./reports "${'$'}{ARTIFACTS}/e2ereports"
                cp -r ./screenshots "${'$'}{ARTIFACTS}/screenshots"
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }

    features {
        feature {
            id = "BUILD_EXT_362"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "artifacts/**/*.xml")
            param("xmlReportParsing.verboseOutput", "true")
        }
    }

    dependencies {
        snapshot(prepare_calypso_live) {
        }
    }
})

object run_server_tests : BuildType({
    templates(run_unit_tests)
    name = "Run server tests"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_13"
            scriptContent = """
                set -e
                export JEST_JUNIT_OUTPUT_DIR="./test_results"
                export JEST_JUNIT_OUTPUT_NAME="results.xml"
                export HOME="/calypso"
                export NODE_ENV="test"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export npm_config_cache=${'$'}(yarn cache dir)
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                yarn test-server --maxWorkers=${'$'}JEST_MAX_WORKERS --ci --reporters=default --reporters=jest-junit --silent
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }

    features {
        feature {
            id = "BUILD_EXT_374"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "test_results/**/*.xml")
        }
    }
})

object run_woo_e2e_tests : BuildType({
    templates(run_e2e)
    name = "Run e2e tests - Canary Woo"

    artifactRules = "artifacts"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_363"
            scriptContent = """
                set -e
                
                export NODE_CONFIG_ENV=test
                export BRANCHNAME=master
                export HIGHLIGHT_ELEMENT=true
                export sha=${'$'}(git rev-parse HEAD)
                export npm_config_cache=${'$'}(yarn cache dir)
                export ARTIFACTS="${'$'}{PWD}/artifacts"
                export TARGET="WOO"
                export JETPACKHOST=''
                export TEST_VIDEO=true
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                cd  ./test/e2e
                yarn run decryptconfig
                ./run.sh -a ${'$'}E2E_WORKERS -R -W -S ${'$'}sha
                
                # Save artifacts
                mkdir -p "${'$'}ARTIFACTS"
                cp -r ./reports "${'$'}{ARTIFACTS}/e2ereports"
                cp -r ./screenshots "${'$'}{ARTIFACTS}/screenshots"
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }

    features {
        feature {
            id = "BUILD_EXT_362"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "artifacts/**/*.xml")
            param("xmlReportParsing.verboseOutput", "true")
        }
    }

    dependencies {
        snapshot(prepare_calypso_live) {
        }
    }
})

object typecheck : BuildType({
    templates(run_unit_tests)
    name = "Typecheck"

    steps {
        script {
            name = "Run tests"
            id = "RUNNER_13"
            scriptContent = """
                set -e
                export HOME="/calypso"
                export NODE_ENV="test"
                export CHROMEDRIVER_SKIP_DOWNLOAD=true
                export PUPPETEER_SKIP_DOWNLOAD=true
                export NODE_OPTIONS=--max-old-space-size=8192
                export npm_config_cache=${'$'}(yarn cache dir)
                
                # Update node
                . "${'$'}NVM_DIR/nvm.sh" --install
                nvm use
                
                # Install modules
                yarn install
                
                # Run tests
                yarn run tsc --project client/landing/gutenboarding
            """.trimIndent()
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerImage = "%docker_image%"
            dockerRunParameters = "-u %env.UID%"
        }
    }
})

object build_app : Template({
    name = "Build app"

    params {
        param("env.JEST_MAX_WORKERS", "6")
        param("env.NODE_OPTIONS", "--max-old-space-size=12288")
    }

    vcs {
        root(WpCalypso)

        cleanCheckout = true
    }

    features {
        perfmon {
            id = "perfmon"
        }
    }
})

object run_e2e : Template({
    name = "Run e2e tests"

    vcs {
        root(WpCalypso)

        cleanCheckout = true
    }

    features {
        feature {
            id = "BUILD_EXT_362"
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "test_results/**/*.xml")
        }
        perfmon {
            id = "perfmon"
        }
    }
})

object run_unit_tests : Template({
    name = "Run unit tests"

    vcs {
        root(WpCalypso)

        cleanCheckout = true
    }

    features {
        perfmon {
            id = "perfmon"
        }
    }
})

object WpCalypso : GitVcsRoot({
    name = "wp-calypso"
    url = "git@github.com:Automattic/wp-calypso.git"
    pushUrl = "git@github.com:Automattic/wp-calypso.git"
    branch = "refs/heads/try/teamcity"
    authMethod = uploadedKey {
        uploadedKey = "Sergio TeamCity"
    }
})
