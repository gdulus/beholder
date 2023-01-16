# beholder
Beholder is a microservice documentation search engine with K8S first approach.

### Motivation
Common approach with delivery for service documentation is to use services like https://swagger.io or https://apiary.io to read documentation from project repo and make it accessible over service page.

Beholder take different approach. It uses services deployed to K8S cluster as carriers of the documentation files.

There is no need for external service serving the doc. There is no ambiguity which version of documentation is deployed to the specific environment. There is just BEHOLDER!

### Requirements

* Elastics Search (tested with version 7.17.6)
* Kubernetes (tested with version 1.23.3)

### Usage

First of all, you need to deploy Beholder to K8S cluster. Beholder will need to have access to two things:

* ES instance 
* K8S API 




### License

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