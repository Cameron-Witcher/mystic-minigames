pipeline {
    agent { any 'maven:3.8.1-adoptopenjdk-11' } 
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }
        stage("Archive"){
        	steps {
        		script{
        			if(env.JOB_BASE_NAME == "master"){
        				archiveArtifacts artifacts: "target/*.jar", followSymlinks: false 
        				echo "$JOB_BASE_NAME"
        			} else {
        				echo "$JOB_BASE_NAME is not master. Not Arciving.."
        			}
        		}
        	}
        }
        

        stage('Clean') {
            steps {
                sh 'cp target/*.jar /home/Minecraft/Dependencies/'
                sh 'mvn clean'
            }
        }
    }
}