#!/data/data/com.termux/files/usr/bin/sh

DIRNAME=$(dirname "$0")

exec java -Xmx1536m \
  -classpath "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
