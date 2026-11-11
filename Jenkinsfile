
pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven-3.9.11'
        nodejs 'NodeJS-22'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        // 1. BUILD BACKEND
        stage('Build Backend') {
            steps {
                echo 'Building Spring Boot backend'
                sh 'mvn clean compile'
            }
        }

        // 2. UNIT TESTING
        stage('Unit Testing') {
            steps {
                echo 'Running JUnit and Mockito tests'
                sh 'mvn test'
            }
        }

        // 3. COVERAGE REPORT
        stage('Generate Coverage Report') {
            steps {
                sh 'mvn jacoco:report'
            }
        }

        // 4. OWASP DEPENDENCY CHECK
        stage('OWASP Dependency Check') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'nvd-api-key',
                        variable: 'NVD_API_KEY'
                    )
                ]) {
                    sh '''
                        mvn org.owasp:dependency-check-maven:13.0.0:check \
                        -DnvdApiKey="$NVD_API_KEY"
                    '''
                }
            }
        }

        // 5. PACKAGE BACKEND
        stage('Package Backend') {
            steps {
                echo 'Creating executable Spring Boot JAR'
                sh 'mvn package -DskipTests'
            }
        }

        // 6. BUILD REACT FRONTEND
        stage('Build Frontend') {
            steps {
                echo 'Building React frontend'

                dir('frontend') {
                    sh '''
                        node --version
                        npm --version

                        npm ci --include=optional

                        npm run build

                        test -s dist/index.html
                    '''
                }
            }
        }
    
        // 9. DEPLOY BACKEND TO UAT
        stage('Deploy Backend to UAT') {

            when {
                expression {
                    return env.CHANGE_ID == null &&
                           (env.BRANCH_NAME == null ||
                            env.BRANCH_NAME == 'main')
                }
            }

            steps {
                sh '''
                    set -eu

                    JAR="target/job-tracker-0.0.1-SNAPSHOT.jar"
                    KEY="/var/lib/jenkins/.ssh/id_ed25519"
                    UAT_USER="deployuser"
                    UAT_HOST="172.31.30.56"
                    UAT_DIR="/opt/jobtracker/uat"
                    JAR_NAME="job-tracker-0.0.1-SNAPSHOT.jar"

                    echo "Checking backend artifact..."
                    test -f "$JAR"

                    echo "Transferring backend JAR..."

                    scp -i "$KEY" \
                        -o BatchMode=yes \
                        -o StrictHostKeyChecking=yes \
                        "$JAR" \
                        "$UAT_USER@$UAT_HOST:$UAT_DIR/$JAR_NAME.new"

                    echo "Deploying backend..."

                    ssh -i "$KEY" \
                        -o BatchMode=yes \
                        -o StrictHostKeyChecking=yes \
                        "$UAT_USER@$UAT_HOST" \
                        "bash -s" <<'REMOTE'

set -eu

cd /opt/jobtracker/uat

JAR="job-tracker-0.0.1-SNAPSHOT.jar"
BACKUP=""

# Backup existing JAR
if [ -f "$JAR" ]; then
    BACKUP="$JAR.backup.$(date +%Y%m%d%H%M%S)"
    cp "$JAR" "$BACKUP"
    echo "Previous JAR backed up: $BACKUP"
fi

# Activate new JAR
mv "$JAR.new" "$JAR"

# Restart application
sudo -n /usr/bin/systemctl restart jobtracker

echo "Waiting for backend startup..."

HEALTHY=false

# Wait up to five minutes
for i in $(seq 1 60); do

    if curl -fsS \
        http://localhost:8081/ \
        > /dev/null; then

        HEALTHY=true
        echo "Backend deployment successful!"
        break
    fi

    echo "Waiting for backend... ($i/60)"
    sleep 5
done

# Rollback if deployment fails
if [ "$HEALTHY" != "true" ]; then

    echo "Backend health check failed!"

    if [ -n "$BACKUP" ]; then
        echo "Rolling back previous JAR..."

        cp "$BACKUP" "$JAR"
        sudo -n /usr/bin/systemctl restart jobtracker

    else
        echo "No previous JAR available for rollback."
    fi

    exit 1
fi

REMOTE
                '''
            }
        }

        // 10. DEPLOY FRONTEND TO UAT
        stage('Deploy Frontend to UAT') {

            when {
                expression {
                    return env.CHANGE_ID == null &&
                           (env.BRANCH_NAME == null ||
                            env.BRANCH_NAME == 'main')
                }
            }

            steps {
                sh '''
                    set -eu

                    KEY="/var/lib/jenkins/.ssh/id_ed25519"
                    UAT_USER="deployuser"
                    UAT_HOST="172.31.30.56"

                    RELEASE_ID="build-${BUILD_NUMBER}"
                    ARCHIVE="frontend-${BUILD_NUMBER}.tar.gz"

                    echo "Checking frontend build..."

                    test -s frontend/dist/index.html

                    echo "Creating frontend deployment archive..."

                    tar -czf "$ARCHIVE" -C frontend/dist .

                    echo "Transferring frontend to EC2..."

                    scp -i "$KEY" \
                        -o BatchMode=yes \
                        -o StrictHostKeyChecking=yes \
                        "$ARCHIVE" \
                        "$UAT_USER@$UAT_HOST:/tmp/$ARCHIVE"

                    echo "Deploying frontend..."

                    ssh -i "$KEY" \
                        -o BatchMode=yes \
                        -o StrictHostKeyChecking=yes \
                        "$UAT_USER@$UAT_HOST" \
                        "bash -s -- '$RELEASE_ID' '$ARCHIVE'" <<'REMOTE'

set -eu

RELEASE_ID="$1"
ARCHIVE="$2"

BASE="/var/www/jobtracker"
RELEASE_DIR="$BASE/releases/$RELEASE_ID"
CURRENT="$BASE/current"
TEMP_LINK="$BASE/current.new"

echo "Creating frontend release..."

mkdir -p "$RELEASE_DIR"

tar -xzf "/tmp/$ARCHIVE" -C "$RELEASE_DIR"

test -s "$RELEASE_DIR/index.html"

OLD_TARGET=""

if [ -L "$CURRENT" ]; then
    OLD_TARGET="$(readlink "$CURRENT")"
fi

echo "Activating frontend release..."

rm -f "$TEMP_LINK"
ln -s "$RELEASE_DIR" "$TEMP_LINK"
mv -Tf "$TEMP_LINK" "$CURRENT"

if [ ! -s "$CURRENT/index.html" ]; then

    echo "Frontend deployment verification failed!"

    if [ -n "$OLD_TARGET" ]; then
        ln -s "$OLD_TARGET" "$TEMP_LINK"
        mv -Tf "$TEMP_LINK" "$CURRENT"
    else
        rm -f "$CURRENT"
    fi

    exit 1
fi

rm -f "/tmp/$ARCHIVE"

echo "Frontend deployment successful!"
echo "Active release: $RELEASE_DIR"

REMOTE

                    rm -f "$ARCHIVE"
                '''
            }
        }
    }

    // POST BUILD ACTIONS
    post {

        always {
            junit(
                testResults: 'target/surefire-reports/*.xml',
                allowEmptyResults: true
            )

            archiveArtifacts(
                artifacts: 'target/*.jar',
                allowEmptyArchive: true
            )

            archiveArtifacts(
                artifacts: 'frontend/dist/**',
                allowEmptyArchive: true
            )
        }

        success {
            echo 'JOB TRACKER CI/CD PIPELINE SUCCESS'

            script {
                if (env.CHANGE_ID != null) {
                    echo 'Pull Request validation completed successfully.'
                } else if (env.BRANCH_NAME == 'main' ||
                           env.BRANCH_NAME == null) {
                    echo 'Backend and frontend deployment completed.'
                } else {
                    echo 'Feature branch CI validation completed.'
                }
            }
        }

        failure {
            echo 'JOB TRACKER CI/CD PIPELINE FAILED.'
        }
    }
}
