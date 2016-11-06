#!/usr/bin/env groovy

def update='micro'
def branch='master'
def release=false
def project='op-sdk-spi-impl'
def tagPrefix='rel-'

node {
    def artifactoryMaven=null
    def newVersion=null

    withEnv(["PATH+MAVEN=${tool 'maven'}/bin", "JAVA_HOME=${tool 'jdk1.8.0_latest'}"]) {
        stage('Get clean source') {
            deleteDir()
            git credentialsId: '200c2dab-036b-48a3-a824-1f4257be94ff', url: 'https://github.com/digital-me/op-sdk-spi-impl.git'
            sh "mvn clean"
        }
        
        stage ('Set new version') {
            def currVersion=sh (script: 'tmp=\$(git tag -l  "rel-*" | cut -d\'-\' -f2- | sort -r -V | head -n1);echo \${tmp:-\'rel-0.0.12\'}', returnStdout: true).trim()
            newVersion = nextVersion(tagPrefix, update, currVersion);
            echo "current version is ${currVersion}, new version will be ${newVersion}"
            sh "mvn -DnewVersion=$newVersion versions:set"
        }
        
        stage('Prepare Artifactory') {
            def server = Artifactory.server('qiy-artifactory@boxtel')
            artifactoryMaven = Artifactory.newMavenBuild()
            artifactoryMaven.tool = 'maven' // Tool name from Jenkins configuration
            artifactoryMaven.deployer releaseRepo:'Qiy', snapshotRepo:'Qiy', server: server
            artifactoryMaven.resolver releaseRepo:'libs-releases', snapshotRepo:'libs-snapshot', server: server
        }

        stage('Build & Deploy') {
            try {
                def buildInfo = Artifactory.newBuildInfo()
                artifactoryMaven.run pom: 'pom.xml', goals: 'install', buildInfo: buildInfo
             } catch (e) {
                 // TODO [FV 20161104] Send XMPP message here
                 throw e;
             } finally {
                 step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
             }
        }

        stage('Tag release') {   
            sh "git tag -a 'rel-${newVersion}' -m 'Release tag by Jenkins'"
            sshagent(['200c2dab-036b-48a3-a824-1f4257be94ff']) {
                sh "git remote set-url origin 'git@github.com:digital-me/op-sdk-spi-impl.git'"
                sh "git -c core.askpass=true push origin 'rel-${newVersion}'"
            }
        }
    }
}

@NonCPS
def nextVersion(tagPrefix, update, currVersion) {
//    println "${tagPrefix} - ${update} - ${currVersion}"
    if (currVersion.length() < tagPrefix.length() + 5)  {
        throw new IllegalArgumentException("${currVersion} is too short for prefix ${tagPrefix}")
    }
    def parts = currVersion.substring(tagPrefix.length()).split('\\.')
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
    return result
}

