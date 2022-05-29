#!/bin/bash

app() {
  echo 'Starting beholder ...'
  lein clean
  lein ring server
}

k8s() {
  echo 'Starting minikube ...'
  minikube start --driver=docker
}

es() {
  echo 'Starting ES ...'
  docker start es01
}

"$@"
