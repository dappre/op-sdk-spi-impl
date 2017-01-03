#!/usr/bin/env groovy

def config = updateConfig {
    update = 'micro';             // needs to be set here in the source
}

def nightly = config['nightly'];
def release = config['release'];

node {
    withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
        stage ("Build") {
            echo "Cleaning dir and getting source"
            getCleanGitSource(config);
            echo "Getting latest versions of nl.** projects that we depend upon"
            updateNlMvnDependencies(config);
            echo "Getting latest version from Git and updating the pom accordingly"
            updateMvnVersionFromGitTag(config);
            echo "Building maven project and deploying to Artifactory"
            buildMvnAndDeploy(config);
            echo "Done build stage"
        }
    }
    if (release) {
        stage ("Tag") {
            echo "Starting tagging"
            tagGit(config);
            // TODO [FV 20170103] Deploy to public artifatory
        }
    }
}


