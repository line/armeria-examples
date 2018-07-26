#!/bin/bash -e
if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <Armeria version> <Armeria working copy>"
  exit 1
fi

VERSION="$1"
SRC_DIR="$(readlink -f "$2")"

if [[ ! -d "$SRC_DIR/.git" ]]; then
  echo "Not a git repository: $SRC_DIR"
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

echo 'Patching build.gradle ..'
for E in $(find_examples); do
  TMPF="$(mktemp)"
  cp "$E/build.gradle" "$TMPF"

  # Remove the 'buildscript' section.
  perl -i -e '
    undef $/; $_=<>;
    s/(^|\n|\s)*buildscript \{(\n|.)*?\n}//;
    print
  ' "$TMPF"

  # Remove the 'apply plugin' statements.
  perl -i -pe 's/^apply plugin:.*$//g' "$TMPF"

  # Replace the 'project(...)' dependencies.
  perl -i \
    -pe "s/project\\(':core'\\)/'com.linecorp.armeria:armeria'/g;" \
    -pe "s/project\\(':spring:boot-autoconfigure'\\)/'com.linecorp.armeria:armeria-spring-boot-autoconfigure'/g;" \
    -pe "s/project\\(':tomcat'\\)/'com.linecorp.armeria:armeria-tomcat'/g;" \
    "$TMPF"

  # Remove the redundant empty lines.
  perl -i -e '
    undef $/; $_=<>;
    s/^(\r?\n)*//;
    s/(\r?\n)(\r?\n){2,}/\1\1/g;
    print
  ' "$TMPF"

  {
    # Add the 'plugins' section.
    PLUGINS=('io.spring.dependency-management')
    PLUGIN_VERSIONS=('1.0.6.RELEASE')
    if grep -qF springBoot "$TMPF"; then
      PLUGINS+=('org.springframework.boot')
      PLUGIN_VERSIONS+=('2.0.3.RELEASE')
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

    cat "$TMPF"

    # Configure the Java compiler.
    echo 'tasks.withType(JavaCompile) {'
    echo "    sourceCompatibility = '1.8'"
    echo "    targetCompatibility = '1.8'"
    echo "    options.encoding = 'UTF-8'"
    echo '    options.debug = true'
    echo "    options.compilerArgs += '-parameters'"
    echo '}'
    echo
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
