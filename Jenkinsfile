#!/usr/bin/env groovy
def libLegacy = [
    remote:           'ssh://git@code.in.digital-me.nl:2222/DEVops/Jenkins.git',
    branch:           'master',
    credentialsId:    'bot-ci-dgm-rsa',
]

library(
    identifier: "libLegacy@${libLegacy.branch}",
    retriever: modernSCM([
        $class: 'GitSCMSource',
        remote: libLegacy.remote,
        credentialsId: libLegacy.credentialsId
    ])
)

def config = updateConfig {
    update = 'micro';             // needs to be set here in the source
}

def nightly = config['nightly'];
def release = config['release'];

node {
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
    
    if (release) {
        stage ("Tag") {
            echo "Starting tagging"
            tagGit(config);
            // TODO [FV 20170103] Deploy to public artifatory
        }
    }
}


