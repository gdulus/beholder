# beholder

API documentation search engine with microservice and K8S first approach. 

## Usage

TBD

## Installation

1. Install FireFox Geckodriver: ```sudo apt-get install firefox-geckodriver```    
2. Install ElasticSearch

## Tasks

```
# DEPLOY IMAGE TO MINIKUBE AND EXPOSE IT
eval $(minikube docker-env)  
docker build -t openapi-app:1.0.0 .
kubectl create deployment openapi-app --image=openapi-app:1.0.0
kubectl expose deployment openapi-app --type=LoadBalancer --port=3000

# CLEANUP
docker rm openapi-app
docker image rm openapi-app:1.0.0 
```

## Setting up minikube
```
kubectl apply -f ./infra/k8s/ClusterRole.yml
kubectl create clusterrolebinding service-reader-pod \
--clusterrole=service-reader  \
--serviceaccount=default:default
```

## License

MIT License

Copyright (c) 2022 Beholder

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.