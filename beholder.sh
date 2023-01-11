#!/bin/bash

app-start() {
  echo 'Starting beholder ...'
  lein clean
  lein ring server
}

es-run() {
  echo 'Starting ES ...'
  docker start elasticsearch
}

docker-build() {
  echo '-----------------------------------------------'
  echo 'BUILDING DOCKER IMAGE'
  lein clean
  #lein eftest
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
  kubectl delete service beholder || true
  echo 'DONE'
  echo '-----------------------------------------------'
}

k8s-deploy() {
  echo '-----------------------------------------------'
  echo 'DEPLOYING TO MINIKUBE'
  eval $(minikube docker-env)
  k8s-clean
  sleep 3
  docker-clean
  docker-build
  kubectl create deployment beholder --image=beholder:1.0.0
  kubectl expose deployment beholder --type=LoadBalancer --port=3000
  echo '==============================================='
  echo 'Image build and deployed to minikube. You can access is under'
  sleep 3
  minikube service beholder --url
  echo '==============================================='
  echo 'DONE'
  echo '-----------------------------------------------'
}

init-dev-env() {
  echo '-----------------------------------------------'
  echo 'Starting dev env'
  k8s-run
  es-run
}

help() {
  echo '------------------------------------'
  echo 'init-dev-env = Start all components for dev env'
  echo '------------------------------------'
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
