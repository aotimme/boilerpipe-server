mkdir -p classes
javac -d classes \
  -classpath "lib/*" \
  -Xlint \
  ParseServer.java
