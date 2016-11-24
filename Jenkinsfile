#!/usr/bin/env groovy

def depVersion='0.0.16-SNAPSHOT'       // version of the sdk-lib, on which this project depends
def update='micro'            // needs to be set here in the source
def project='op-sdk-spi-impl' // needs to be set here in the source
def credid='ab8fd421-14d3-49a0-a429-809039ef0e1b' // jenkins id for deployer key for this project
def branch='master'           // can we get this as a parameter?
def release=false             // by default false; true if parameter

def giturl="git@github.com:digital-me/${project}.git"  // NB: this is the format ssh-agent understands
def tagPrefix="${branch}-"    // maybe: branch name?

node {
    def newVersion=null

    withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
        stage('Get clean source') {
            deleteDir()
            git url: giturl
            sh "mvn clean"
        }
        
        stage ('Set new version') {
            // ask Git for the tags that start with the tagPrefix, 
            // keep everything after the first dash
            // sort it as version numbers, reversed
            // take the first entry
            // or 0.0.12 if nothing was found
            def currVersion=sh (script: "tmp=\$(git tag -l  '${tagPrefix}*' | cut -d'-' -f2- | sort -r -V | head -n1);echo \${tmp:-'0.0.12'}", returnStdout: true).trim()
            newVersion = nextVersion(update, currVersion, release);
            echo "current version is ${currVersion}, new version will be ${newVersion}"
            sh "mvn versions:set -DnewVersion=$newVersion"
            currentBuild.displayName="#${env.BUILD_NUMBER}: ${newVersion}"
            
            // extra step 
            sh "sed -i -e 's|<version>0.0.0</version>|<version>${depVersion}</version>|' pom.xml"
            currentBuild.description="depends on : ${depVersion}"
            
        }
        
        stage('Build & Deploy') {
        	def goals = 'install';
            def buildInfo = Artifactory.newBuildInfo()
            def server = Artifactory.server('private-repo')
            def artifactoryMaven = Artifactory.newMavenBuild()
            artifactoryMaven.tool = 'maven' // Tool name from Jenkins configuration
            artifactoryMaven.opts = '-Djava.io.tmpdir="/opt/tmp"'
            artifactoryMaven.deployer releaseRepo:'Qiy', snapshotRepo:'Qiy', server: server
            artifactoryMaven.resolver releaseRepo:'libs-releases', snapshotRepo:'libs-snapshots', server: server
            artifactoryMaven.run pom: 'pom.xml', goals: goals, buildInfo: buildInfo
            junit testResults: '**/target/surefire-reports/*.xml'
            step ([$class: 'DependencyCheckPublisher'])
        }

        stage('Tag release') {   
            if (release) {
                sh "git tag -a '${tagPrefix}${newVersion}' -m 'Release tag by Jenkins'"
                sshagent([credid]) {
                    sh "git -c core.askpass=true push origin '${tagPrefix}${newVersion}'"
                }
            }
        }
    }
}

@NonCPS
def nextVersion(update, currVersion, release) {
//    println "${update} - ${currVersion}"
    if (currVersion.length() < 5)  {
        throw new IllegalArgumentException("${currVersion} is too short")
    }
    def parts = currVersion.split('\\.')
    def major = parts[0].toInteger()
    def minor = parts[1].toInteger()
    def micro = parts[2].toInteger()
    
    switch (update) {
        case 'major':
            major = 1+major;
            minor = 0;
            micro = 0;
            break;
        case 'minor':
            minor = 1+minor;
            micro = 0;
            break;
        case 'micro':
            micro = 1+micro;
            break;
        default:
            throw new IllegalArgumentException(update + " is not a valid value for update")
    }
    String result = "${major}.${minor}.${micro}";
//    println result
    return release ? result : "${result}-SNAPSHOT"
}

