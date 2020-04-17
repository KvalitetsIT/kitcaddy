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

                                        junit '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'

				}
			}
		}
		stage('Tag Docker Images And Push') {
		    steps {
		        script {
				image = docker.image("kvalitetsit/kitcaddy:dev")
				image.push("latest")
				if(env.TAG_NAME != null && env.TAG_NAME.matches("^v[0-9]*\\.[0-9]*\\.[0-9]*")) {
					echo "Tagging version"
					image.push(env.TAG_NAME.substring(1))
				}

				timage = docker.image("kvalitetsit/kitcaddy-templates:dev")
				timage.push("latest")
				if(env.TAG_NAME != null && env.TAG_NAME.matches("^v[0-9]*\\.[0-9]*\\.[0-9]*")) {
					echo "Tagging version"
					timage.push(env.TAG_NAME.substring(1))
				}
			}
		    }
		}

	}
}

