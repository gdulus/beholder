FROM openjdk:17-alpine
COPY ./target/beholder-0.1.0-SNAPSHOT-standalone.jar /home/beholder-0.1.0-SNAPSHOT-standalone.jar
ENTRYPOINT java \
-Dk8s-apiserver=https://kubernetes.default.svc \
-Dk8s-namespaces=default \
-Dk8s-token=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token) \
-server \
-jar /home/beholder-0.1.0-SNAPSHOT-standalone.jar