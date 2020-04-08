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
                                        docker.build("kvalitetsit/kitcaddy:dev", ".")
                                }
                        }
                }
                stage('Build Docker image (KitCaddy templates)') {
                        steps {
                                script {
                                        docker.build("kvalitetsit/kitcaddy-templates:dev", "caddytemplates")
                                }
                        }
                }
	        stage('Run integration tests') {
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

