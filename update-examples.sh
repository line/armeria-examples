#!/bin/bash -e
ASSERTJ_VERSION='3.12.1'
DEPENDENCY_MANAGEMENT_PLUGIN_VERSION='1.0.7.RELEASE'
IO_PROJECTREACTOR_VERSION='3.2.6.RELEASE'
JSON_UNIT_VERSION='2.4.0'
JUNIT_VERSION='4.12'
SLF4J_VERSION='1.7.26'
SPRING_BOOT_VERSION='2.1.3.RELEASE'

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <Armeria version> <Armeria working copy path>"
  exit 1
fi

READLINK_PATH="$(which greadlink 2>/dev/null || true)"
if [[ -z "$READLINK_PATH" ]]; then
  READLINK_PATH="$(which readlink 2>/dev/null || true)"
fi
if [[ -z "$READLINK_PATH" ]]; then
  echo "Cannot find 'readlink'"
  exit 1
fi

VERSION="$1"
SRC_DIR="$($READLINK_PATH -f "$2")"

if [[ ! -d "$SRC_DIR/.git" ]]; then
  echo "Not a git repository: $SRC_DIR"
  exit 1
fi
cd "$(dirname "$0")"

echo 'Copying README.md ..'
cp -f "$SRC_DIR/examples/README.md" .

function find_examples() {
  find "$SRC_DIR/examples" -mindepth 1 -maxdepth 1 -type d -print | while read -r D; do
    if [[ -f "$D/build.gradle" ]]; then
      basename "$D"
    fi
  done
}

echo 'Copying examples ..'
for E in $(find_examples); do
  rsync --archive --delete "$SRC_DIR/examples/$E" .
done

echo 'Copying Gradle wrapper ..'
for E in $(find_examples); do
  cp "$SRC_DIR/gradlew" "$SRC_DIR/gradlew.bat" "$E"
  mkdir "$E/gradle"
  cp -r "$SRC_DIR/gradle/wrapper" "$E/gradle"
done

for E in $(find_examples); do
  echo "Patching $E/build.gradle .."
  TMPF="$(mktemp)"

  # Start with removing the 'buildscript' section.
  perl -e '
    undef $/; $_=<>;
    s/(^|\n|\s)*buildscript \{(\n|.)*?\n}//;
    print
  ' < "$E/build.gradle" > "$TMPF"

  # Remove the 'apply plugin' statements.
  perl -i -pe 's/^apply plugin:.*$//g' "$TMPF"

  # Replace the 'project(...)' dependencies.
  perl -i \
    -pe "s/project\\(':core'\\)/'com.linecorp.armeria:armeria'/g;" \
    -pe "s/project\\(':logback'\\)/'com.linecorp.armeria:armeria-logback'/g;" \
    -pe "s/project\\(':saml'\\)/'com.linecorp.armeria:armeria-saml'/g;" \
    -pe "s/project\\(':spring:boot-actuator-starter'\\)/'com.linecorp.armeria:armeria-spring-boot-actuator-starter'/g;" \
    -pe "s/project\\(':spring:boot-autoconfigure'\\)/'com.linecorp.armeria:armeria-spring-boot-autoconfigure'/g;" \
    -pe "s/project\\(':spring:boot-starter'\\)/'com.linecorp.armeria:armeria-spring-boot-starter'/g;" \
    -pe "s/project\\(':spring:boot-webflux-autoconfigure'\\)/'com.linecorp.armeria:armeria-spring-boot-webflux-autoconfigure'/g;" \
    -pe "s/project\\(':spring:boot-webflux-starter'\\)/'com.linecorp.armeria:armeria-spring-boot-webflux-starter'/g;" \
    -pe "s/project\\(':tomcat'\\)/'com.linecorp.armeria:armeria-tomcat'/g;" \
    "$TMPF"

  # Append version numbers to the 3rd party dependencies.
  perl -i \
    -pe "s/'junit:junit'/'junit:junit:$JUNIT_VERSION'/g;" \
    -pe "s/'net.javacrumbs.json-unit:json-unit-fluent'/'net.javacrumbs.json-unit:json-unit-fluent:$JSON_UNIT_VERSION'/g;" \
    -pe "s/'org.assertj:assertj-core'/'org.assertj:assertj-core:$ASSERTJ_VERSION'/g;" \
    -pe "s/'org.slf4j:slf4j-simple'/'org.slf4j:slf4j-simple:$SLF4J_VERSION'/g;" \
    -pe "s/'io.projectreactor:reactor-core'/'io.projectreactor:reactor-core:$IO_PROJECTREACTOR_VERSION'/g;" \
    -pe "s/'io.projectreactor:reactor-test'/'io.projectreactor:reactor-test:$IO_PROJECTREACTOR_VERSION'/g;" \
    "$TMPF"

  {
    # Add the 'plugins' section.
    PLUGINS=('io.spring.dependency-management')
    PLUGIN_VERSIONS=("$DEPENDENCY_MANAGEMENT_PLUGIN_VERSION")
    if grep -qF springBoot "$TMPF"; then
      PLUGINS+=('org.springframework.boot')
      PLUGIN_VERSIONS+=("$SPRING_BOOT_VERSION")
    fi
    echo 'plugins {'
    for ((I=0; I<${#PLUGINS[@]}; I++)); do
      echo "    id \"${PLUGINS[$I]}\" version \"${PLUGIN_VERSIONS[$I]}\""
    done
    echo '}'
    echo

    # Apply the common plugins.
    echo "apply plugin: 'java'"
    echo "apply plugin: 'eclipse'"
    echo "apply plugin: 'idea'"
    echo

    # Define the repositories.
    echo 'repositories {'
    echo '    mavenCentral()'
    echo '}'
    echo

    # Import the BOM.
    echo 'dependencyManagement {'
    echo '    imports {'
    echo "        mavenBom 'com.linecorp.armeria:armeria-bom:$VERSION'"
    echo '    }'
    echo '}'
    echo

    # Paste the patched file while removing the redundant empty lines.
    perl -e '
      undef $/; $_=<>;
      s/^(\r?\n)*//;
      s/(\r?\n)(\r?\n){2,}/\1\1/g;
      print
    ' < "$TMPF"

    # Configure the Java compiler.
    echo
    echo 'tasks.withType(JavaCompile) {'
    echo "    sourceCompatibility = '1.8'"
    echo "    targetCompatibility = '1.8'"
    echo "    options.encoding = 'UTF-8'"
    echo '    options.debug = true'
    echo "    options.compilerArgs += '-parameters'"
    echo '}'
  } > "$E/build.gradle"
done

# Test all examples
for E in $(find_examples); do
  echo "Testing $E .."
  pushd "$E" >/dev/null
  ./gradlew -q clean check
  popd >/dev/null
done

echo "Successfully updated the examples for Armeria $VERSION"
