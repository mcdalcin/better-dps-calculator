#!/bin/bash
# Direct java invocation to bypass WSL2 daemon issues
# Uses Gradle 6.9.4 which works with --no-daemon in WSL2

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon "$@"
