#!/bin/bash

init() {
  eval $(minikube docker-env) || true
  newgrp docker
}

app-start() {
  echo 'Starting beholder ...'
  lein clean
  lein ring server
}

es-run() {
  echo 'Starting ES ...'
  newgrp docker
  docker start es01
}

docker-build() {
  echo '-----------------------------------------------'
  echo 'BUILDING DOCKER IMAGE'
  lein clean
  lein eftest
  lein ring uberjar
  docker build -t beholder:1.0.0 .
  echo 'DONE'
  echo '-----------------------------------------------'
}

docker-run() {
  docker run --name beholder -p 3000:3000 beholder:1.0.0
}

docker-clean() {
  echo '-----------------------------------------------'
  echo 'CLEANING UP DOCKER'
  docker rm beholder || true
  docker image rm beholder:1.0.0 || true
  echo 'DONE'
  echo '-----------------------------------------------'
}

k8s-run() {
  echo 'Starting minikube ...'
  minikube start --driver=docker
  echo 'Starting proxy ...'
  kubectl proxy --port=8080 &
}

k8s-clean() {
  echo '-----------------------------------------------'
  echo 'CLEANING UP MINIKUBE'
  kubectl delete deployment beholder || true
  echo 'DONE'
  echo '-----------------------------------------------'
}

k8s-deploy() {
  echo '-----------------------------------------------'
  echo 'DEPLOYING TO MINIKUBE'
  k8s-clean
  sleep 3
  docker-clean
  docker-build
  kubectl create deployment beholder --image=beholder:1.0.0
  echo '==============================================='
  echo 'Image build and deployed to minikube. You can access is under'
  minikube service beholder --url
  echo '==============================================='
  echo 'DONE'
  echo '-----------------------------------------------'
}

help() {
  echo '------------------------------------'
  echo 'init = set up local env for development'
  echo 'app-run = start the Beholder'
  echo 'es-run = start ElasticSearch'
  echo '------------------------------------'
  echo 'docker-build = builds docker image'
  echo 'docker-run = runs created Docker image'
  echo 'docker-clean = cleans up Docker image of Beholder'
  echo '------------------------------------'
  echo 'k8s-run = start Minikube with Rest proxy'
  echo 'k8s-deploy = cleans Minikube from Beholder, builds new image and deploys it again'
  echo 'k8s-clean = cleans Minikube from Beholder'
  echo '------------------------------------'
}

"$@"
