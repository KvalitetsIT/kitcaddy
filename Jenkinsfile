pipeline {
	agent any
        options {
                disableConcurrentBuilds()
        }
	stages {

		stage('Clone repository') {
			steps {
				checkout scm
			}
		}

                stage('Build Docker image (KitCaddy)') {
                        steps {
                                script {
                                        docker.build("kvalitetsit/kitcaddy", ".")
                                }
                        }
                }
                stage('Build Docker image (KitCaddy templates)') {
                        steps {
                                script {
                                        docker.build("kvalitetsit/kitcaddy-templates", "-f Dockerfile-caddytemplates .")
                                }
                        }
                }
	        stage('Build And Test') {
			steps {
				script {
					def maven = docker.image('maven:3-jdk-11')
					maven.pull()
					maven.inside() {
						sh 'cd integrationtest; mvn clean install'
					}
				}
			}
		}
	}
}

