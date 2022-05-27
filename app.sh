#!/bin/bash

app() {
  echo 'Starting beholder ...'
  lein ring server
}

k8s() {
  echo 'Starting minikube ...'
  minikube start --driver=docker --container-runtime=cri-o
}

es() {
  echo 'Starting ES ...'
  docker start elastic
}

"$@"