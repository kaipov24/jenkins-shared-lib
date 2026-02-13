def dockerTagForBranch(String branch) {
  return (branch == 'main') ? 'nodemain:v1.0' : 'nodedev:v1.0'
}

def dockerRemoteForBranch(String branch) {
  return (branch == 'main')
    ? 'kairatkaipov/cicd-pipeline:nodemain-v1.0'
    : 'kairatkaipov/cicd-pipeline:nodedev-v1.0'
}

def containerForBranch(String branch) {
  return (branch == 'main') ? 'app-main' : 'app-dev'
}

def portForBranch(String branch) {
  return (branch == 'main') ? '3000' : '3001'
}

def hadolint() {
  sh 'docker run --rm -i hadolint/hadolint < Dockerfile'
}

def trivyScan(String image) {
  def vulnerabilities = sh(
    script: """
      TRIVY_CACHE_DIR=/var/lib/jenkins/trivy-cache \
      trivy image --exit-code 0 --severity HIGH,MEDIUM,LOW --no-progress ${image}
    """,
    returnStdout: true
  ).trim()

  echo "Vulnerability Report:\n${vulnerabilities}"
}

def dockerLogin(String credId='dockerhub') {
  withCredentials([usernamePassword(credentialsId: credId, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
    sh 'echo $PASS | docker login -u $USER --password-stdin https://index.docker.io/v1/'
  }
}

def dockerLogout() {
  sh 'docker logout https://index.docker.io/v1/ || true'
}

def pushImage(String localImage, String remoteImage, String credId='dockerhub') {
  dockerLogin(credId)
  sh """
    docker tag ${localImage} ${remoteImage}
    docker push ${remoteImage}
  """
  dockerLogout()
}

def deploy(String branch, String image) {
  def name = containerForBranch(branch)
  def port = portForBranch(branch)
  sh """
    docker rm -f ${name} || true
    docker run -d --name ${name} --expose ${port} -p ${port}:3000 ${image}
    docker ps --filter "name=${name}"
  """
}

return this
