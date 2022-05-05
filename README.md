# cordapp-shared-library 

This repo is the implementation of the tutorial cordapp. 
https://medium.com/coinmonks/cordapp-example-to-illustrate-privacy-fbc51ce87f78

# Download Zulu JDK 8.
https://www.azul.com/downloads/?version=java-8-lts&package=jdk
For example, this is the tarball for MacOS
https://cdn.azul.com/zulu/bin/zulu8.62.0.19-ca-jdk8.0.332-macosx_x64.tar.gz

# Set JDK path
extract the jdk tarball, and set the JAVA_HOME and PATH.
export JAVA_HOME=zulu8.60.0.21-ca-jdk8.0.322-macosx_x64/zulu-8.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# Build app
run `./gradlew deployNodes`

# Start app
run `cd build/nodes ; ./runnodes`
