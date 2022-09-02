#!/bin/bash

app() {
  echo 'Starting beholder ...'
  lein clean
  lein ring server
}

k8s() {
  echo 'Starting minikube ...'
  minikube start --driver=docker
  echo 'Starting proxy ...'
  kubectl proxy --port=8080 &
}

es() {
  echo 'Starting ES ...'
  newgrp docker
  docker start es01
}

build-docker() {
  lein clean
  lein ring uberjar
  docker build -t beholder:1.0.0 .
}

run-docker() {
  docker run --name beholder -p 3000:3000 beholder:1.0.0
}

clean-up() {
  kubectl delete deployment beholder || true
  echo 'waiting to finish'
  sleep 3
  echo 'cleaning up docker'
  docker rm beholder || true
  docker image rm beholder:1.0.0 || true
  echo 'done'
}

deploy-mini() {
  clean-up
  echo 'building docker image'
  build-docker
  echo 'deploy to minikube'
  kubectl create deployment beholder --image=beholder:1.0.0
}

help() {
  echo '------------------------------------'
  echo 'app = start the Beholder'
  echo 'k8s = start the Minikube'
  echo 'es = start ElasticSearch'
  echo 'build-docker = builds docker image'
  echo 'run-docker = runs created Docker image'
  echo 'deploy-mini = deploye beholder image to minikube'
  echo '------------------------------------'
}

"$@"
