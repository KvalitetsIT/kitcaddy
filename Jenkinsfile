podTemplate(
        containers: [containerTemplate(image: 'docker', name: 'docker', command: 'cat', ttyEnabled: true)],
        volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')],
) {
    node(POD_LABEL) {
        properties([disableConcurrentBuilds()])
        try {

            stage('Clone repository') {
                checkout scm
            }

            stage('Build Docker image (KitCaddy)') {
                container('docker') {
                    docker.build("kvalitetsit/kitcaddy:dev", ".")
                }
            }
            stage('Build Docker image (KitCaddy templates)') {
                container('docker') {
                    docker.build("kvalitetsit/kitcaddy-templates:dev", "caddytemplates")
                }
            }
            //stage('Run integration tests') {
            //	container('docker') {
            //			def maven = docker.image('maven:3-jdk-11')
            //			maven.pull()
            //			maven.inside() {
            //				sh 'cd integrationtest; mvn clean install'
            //			}
//
            //                                      junit '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'
//
//			}
//		}*/
            stage('Tag Docker Images And Push') {
                container('docker') {
                    docker.withRegistry('', 'dockerhub') {
                        image = docker.image("kvalitetsit/kitcaddy:dev")
                        image.push("latest")
                        if (env.TAG_NAME != null && env.TAG_NAME.matches("^v[0-9]*\\.[0-9]*\\.[0-9]*")) {
                            echo "Tagging version"
                            image.push(env.TAG_NAME.substring(1))
                        }

                        timage = docker.image("kvalitetsit/kitcaddy-templates:dev")
                        timage.push("latest")
                        if (env.TAG_NAME != null && env.TAG_NAME.matches("^v[0-9]*\\.[0-9]*\\.[0-9]*")) {
                            echo "Tagging version"
                            //timage.push(env.TAG_NAME.substring(1))
                        }
                    }
                }
            }

        } finally {
            container('docker') {
                stage('Clean up') {
                    sh 'echo nothing to do'
                }
            }
        }
    }
}

