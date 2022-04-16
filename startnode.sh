export JAVA_HOME=/opt/corda/zulu8.60.0.21-ca-jdk8.0.322-linux_aarch64
export PATH=$JAVA_HOME/bin:$PATH
cd /opt/corda
pwd
ls
java -version
touch nohup.out
nohup java  -jar corda.jar -n &
tail -f nohup.out
