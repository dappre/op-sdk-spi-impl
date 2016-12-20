#!/usr/bin/env groovy

def config = updateConfig {
    update = 'micro';             // needs to be set here in the source
}

node {
    withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
        stage ("Build") {
            getCleanSource(config);
            updateNlMvnDependencies(config);
            updateMvnVersionFromGitTag(config);
            buildAndDeploy(config);
            // tagRelease(this, config);
        }
    }
}