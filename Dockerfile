FROM azul/zulu-openjdk:17.0.2-17.32.13-arm64

WORKDIR /opt/java
COPY zulu8.60.0.21-ca-jdk8.0.322-linux_aarch64.tar .
RUN tar -xvf zulu8.60.0.21-ca-jdk8.0.322-linux_aarch64.tar
ENV JAVA_HOME=/opt/java/zulu8.60.0.21-ca-jdk8.0.322-linux_aarch64
ENV PATH=$JAVA_HOME/bin:$PATH
COPY startnode.sh  /opt/java/startnode.sh
RUN chmod u+x /opt/java/startnode.sh
CMD /opt/java/startnode.sh
