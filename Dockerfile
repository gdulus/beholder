FROM openjdk:17-alpine
COPY ./target/beholder-0.1.0-SNAPSHOT-standalone.jar /home/beholder-0.1.0-SNAPSHOT-standalone.jar
ENTRYPOINT java \
-Dk8s-apiservier=https://kubernetes.default.svc \
-Dk8s-namespace=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace) \
-Dk8s-token=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token) \
-Dk8s-cacert=$(cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt) \
-server \
-jar /home/beholder-0.1.0-SNAPSHOT-standalone.jar