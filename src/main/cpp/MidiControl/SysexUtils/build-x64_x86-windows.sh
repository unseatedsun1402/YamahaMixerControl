export JAVA_HOME="/mnt/c/Program Files/Java/jdk-21"

echo "MinGW VERSION ===== $(x86_64-w64-mingw32-g++ --version | head -n 1) ====="

x86_64-w64-mingw32-g++ \
  -I"$JAVA_HOME/include" \
  -I"$JAVA_HOME/include/win32" \
  -shared resolve.cpp \
  -static-libstdc++ -static-libgcc \
  -o native_sysex_debug.dll

x86_64-w64-mingw32-g++ \
  -O2 -g -fPIC \
  -I"$JAVA_HOME/include" \
  -I"$JAVA_HOME/include/win32" \
  -static-libstdc++ -static-libgcc \
  -shared resolve.cpp \
  -o native_sysex.dll 

echo "Dll built: $(objdump -f native_sysex.dll | grep architecture)"
