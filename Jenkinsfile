pipeline {
    agent any

    environment {
        APP_NAME = 'ifs25042-p6-pbo2026-springboot-cashflow'
        IMAGE_NAME = 'ifs25042-p6-pbo2026-springboot-cashflow'
    }

    stages {
        stage('Build & Test') {
            steps {
                sh 'chmod +x mvnw && ./mvnw clean test'
            }
        }

        stage('Jacoco 100%') {
            steps {
                sh './mvnw jacoco:check'
            }
        }

        stage('Trivy Scan') {
            steps {
                sh 'trivy fs --exit-code 0 --severity HIGH,CRITICAL .'
            }
        }

        stage('Package') {
            steps {
                sh './mvnw package -Pproduction -DskipTests'
            }
        }

        stage('Publish') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying ${APP_NAME} using configured environment variables..."
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}
