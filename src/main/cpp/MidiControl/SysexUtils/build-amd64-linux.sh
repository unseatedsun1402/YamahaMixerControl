export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"

g++ -I"$JAVA_HOME/include" \
    -I"$JAVA_HOME/include/linux" \
    -fPIC -shared resolve.cpp \
    -o libnative_sysex.so