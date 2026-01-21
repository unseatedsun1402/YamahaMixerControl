export JAVA_HOME="/mnt/c/Program Files/Java/jdk-21"

echo "MinGW VERSION ===== $(x86_64-w64-mingw32-g++ --version | head -n 1) ====="

x86_64-w64-mingw32-g++ \
  -I"$JAVA_HOME/include" \
  -I"$JAVA_HOME/include/win32" \
  -shared meter_tools.cpp \
  -static-libstdc++ -static-libgcc \
  -o native_meter_tools.dll

echo "Dll built: $(objdump -f native_meter_tools.dll | grep architecture)"
