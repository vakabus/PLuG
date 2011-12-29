#!/bin/bash


OS=`uname`
ARCH_RAW=`uname -m`
ARCH="unspec"

if [ "${ARCH_RAW}" = "i686" -o "${ARCH_RAW}" = "x86_32" -o "${ARCH_RAW}" = "i386" ]; then
  ARCH="x32"
elif [ "${ARCH_RAW}" = "x86_64" ]; then
  ARCH="x64"
else
  echo "Unknow architecture...."
  exit -1
fi

if [ "${OS}" = "Darwin" ]; then
  JBORAT_AGENT="macosx/${ARCH}/"libjboratagent.jnilib
elif  [ "${OS}" = "Linux" ]; then
  JBORAT_AGENT="linux/${ARCH}/"libjboratagent.so
else
  echo "Unknow platform..."
  exit -1
fi

# shared memory is the default communication
# to enable socket, add ",ipc.socket" to the options
JBORAT_AGENT_OPTS="1234,localhost,ipc.socket"

java -noverify \
    -XX:MaxPermSize=128m \
    -Xmx2G \
    -javaagent:../../test/lib/agent.jar \
    -Xbootclasspath/p:../../test/lib/remote-runtime.jar:../../lib/jborat-runtime.jar:build/fia-runtime.jar \
    -agentpath:../../test/lib/${JBORAT_AGENT}=${JBORAT_AGENT_OPTS} \
    -cp ./bin \
    $*
#  -jar /home/sarimbea/workspace/lib/dacapo-9.12-bach.jar  luindex lusearch pmd h2 tradebeans tradesoap sunflow tomcat   