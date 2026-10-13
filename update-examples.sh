#!/bin/bash -e
ASSERTJ_VERSION='3.27.7'
ATHENZ_VERSION='1.12.42'
AWAITILITY_VERSION='4.3.0'
DAGGER_VERSION='2.59.2'
DEPENDENCY_MANAGEMENT_PLUGIN_VERSION='1.1.7'
DROPWIZARD_VERSION='2.1.12'
GRADLE_VERSION='8.14.5'
IO_PROJECTREACTOR_VERSION='3.8.6'
JSON_UNIT_VERSION='2.38.0'
JSR305_VERSION='3.0.2'
JUNIT_VERSION='4.13.2'
JUNIT5_VERSION='5.14.2'
JUNIT6_VERSION='6.0.3'
LOGBACK14='1.4.14'
MICROMETER_VERSION='1.17.0'
NETTY_VERSION='4.2.15.Final'
THRIFT_GRADLE_PLUGIN="0.6.1"
PROTOC_VERSION='3.25.8'
PROTOC_GEN_GRPC_VERSION='1.81.0'
PROTOBUF_GRADLE_PLUGIN_VERSION='0.9.5'
REACTIVE_GRPC_VERSION='1.2.4'
RESILIENCE4J2_VERSION='2.4.0'
SLF4J_VERSION='1.7.36'
SLF4J2_VERSION='2.0.18'
SPRING_BOOT3_VERSION='3.5.10'
SPRING_BOOT4_VERSION='4.0.6'
SPOTIFY_COMPLETABLE_FUTURES_VERSION='0.3.6'
SPOTIFY_FUTURES_EXTRA_VERSION='4.3.3'
TESTCONTAINERS_VERSION='2.0.5'
JAVAX_ANNOTATION_VERSION='1.3.2'
HIBERNATE_VERSION='8.0.3.Final'
JACKSON_VERSION='2.22.1'
SPRING_CLOUD_CONFIG_SERVER_VERSION='5.0.4'

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
  find "$SRC_DIR/examples" -mindepth 1 -maxdepth 2 -type d -print | while read -r D; do
    if [[ -f "$D/build.gradle" && "$D" != *-scala && "$D" != *-sangria && "$D" != */athenz ]]; then
      echo "${D##*/examples/}"
    fi
  done
}

echo 'Copying examples ..'
for E in $(find_examples); do
  rsync --archive --delete "$SRC_DIR/examples/$E/" "$E"
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

  # Remove the 'alias ..' statements.
  # perl -i -pe 's/^alias libs:.*$//g' "$TMPF"

  # Replace the 'project(...)' dependencies.
  perl -i \
    -pe "s/project\\(':core'\\)/'com.linecorp.armeria:armeria'/g;" \
    -pe "s/project\\(':dropwizard2'\\)/'com.linecorp.armeria:armeria-dropwizard2'/g;" \
    -pe "s/project\\(':graphql'\\)/'com.linecorp.armeria:armeria-graphql'/g;" \
    -pe "s/project\\(':graphql-protocol'\\)/'com.linecorp.armeria:armeria-graphql-protocol'/g;" \
    -pe "s/project\\(':grpc'\\)/'com.linecorp.armeria:armeria-grpc'/g;" \
    -pe "s/project\\(':junit5'\\)/'com.linecorp.armeria:armeria-junit5'/g;" \
    -pe "s/project\\(':jetty12'\\)/'com.linecorp.armeria:armeria-jetty12'/g;" \
    -pe "s/project\\(':logback'\\)/'com.linecorp.armeria:armeria-logback'/g;" \
    -pe "s/project\\(':prometheus1'\\)/'com.linecorp.armeria:armeria-prometheus1'/g;" \
    -pe "s/project\\(':reactor3'\\)/'com.linecorp.armeria:armeria-reactor3'/g;" \
    -pe "s/project\\(':resilience4j2'\\)/'com.linecorp.armeria:armeria-resilience4j2'/g;" \
    -pe "s/project\\(':rxjava3'\\)/'com.linecorp.armeria:armeria-rxjava3'/g;" \
    -pe "s/project\\(':saml'\\)/'com.linecorp.armeria:armeria-saml'/g;" \
    -pe "s/project\\(':athenz'\\)/'com.linecorp.armeria:armeria-athenz'/g;" \
    -pe "s/project\\(':spring:boot2-actuator-starter'\\)/'com.linecorp.armeria:armeria-spring-boot2-actuator-starter'/g;" \
    -pe "s/project\\(':spring:boot2-starter'\\)/'com.linecorp.armeria:armeria-spring-boot2-starter'/g;" \
    -pe "s/project\\(':spring:boot3-actuator-starter'\\)/'com.linecorp.armeria:armeria-spring-boot3-actuator-starter'/g;" \
    -pe "s/project\\(':spring:boot3-autoconfigure'\\)/'com.linecorp.armeria:armeria-spring-boot3-autoconfigure'/g;" \
    -pe "s/project\\(':spring:boot3-starter'\\)/'com.linecorp.armeria:armeria-spring-boot3-starter'/g;" \
    -pe "s/project\\(':spring:boot3-webflux-autoconfigure'\\)/'com.linecorp.armeria:armeria-spring-boot3-webflux-autoconfigure'/g;" \
    -pe "s/project\\(':spring:boot3-webflux-starter'\\)/'com.linecorp.armeria:armeria-spring-boot3-webflux-starter'/g;" \
    -pe "s/project\\(':spring:boot4-actuator-starter'\\)/'com.linecorp.armeria:armeria-spring-boot4-actuator-starter'/g;" \
    -pe "s/project\\(':spring:boot4-starter'\\)/'com.linecorp.armeria:armeria-spring-boot4-starter'/g;" \
    -pe "s/project\\(':spring:boot4-webflux-starter'\\)/'com.linecorp.armeria:armeria-spring-boot4-webflux-starter'/g;" \
    -pe "s/project\\(':tomcat10'\\)/'com.linecorp.armeria:armeria-tomcat10'/g;" \
    -pe "s/project\\(':xds'\\)/'com.linecorp.armeria:armeria-xds'/g;" \
    -pe "s/project\\(':spring:boot4-xds'\\)/'com.linecorp.armeria:armeria-spring-boot4-xds'/g;" \
    -pe "s/project\\(':thrift0.18'\\)/'com.linecorp.armeria:armeria-thrift0.18'/g;" \
    "$TMPF"

  # Remove the line that refers to `project(':annotation-processor')`.
  perl -i -pe 's/^.*:annotation-processor.*$//g' "$TMPF"

  # Append version numbers to the 3rd party dependencies.
  perl -i \
    -pe "s/libs.protobuf.java.util/'com.google.protobuf:protobuf-java-util:$PROTOC_VERSION'/g;" \
    -pe "s/libs.dagger.producers/'com.google.dagger:dagger-producers:$DAGGER_VERSION'/g;" \
    -pe "s/libs.dagger.compiler/'com.google.dagger:dagger-compiler:$DAGGER_VERSION'/g;" \
    -pe "s/libs.dagger/'com.google.dagger:dagger:$DAGGER_VERSION'/g;" \
    -pe "s/libs.reactor.grpc.stub/'com.salesforce.servicelibs:reactor-grpc-stub:$REACTIVE_GRPC_VERSION'/g;" \
    -pe "s/libs.futures.completable/'com.spotify:completable-futures:$SPOTIFY_COMPLETABLE_FUTURES_VERSION'/g;" \
    -pe "s/libs.futures.extra/'com.spotify:futures-extra:$SPOTIFY_FUTURES_EXTRA_VERSION'/g;" \
    -pe "s/libs.dropwizard2.testing/'io.dropwizard:dropwizard-testing:$DROPWIZARD_VERSION'/g;" \
    -pe "s/libs.reactor.core/'io.projectreactor:reactor-core:$IO_PROJECTREACTOR_VERSION'/g;" \
    -pe "s/libs.reactor.test/'io.projectreactor:reactor-test:$IO_PROJECTREACTOR_VERSION'/g;" \
    -pe "s/libs.logback14/'ch.qos.logback:logback-classic'/g;" \
    -pe "s/libs.micrometer.prometheus/'io.micrometer:micrometer-registry-prometheus'/g;" \
    -pe "s/libs.resilience4j.springboot4/'io.github.resilience4j:resilience4j-spring-boot4:$RESILIENCE4J2_VERSION'/g;" \
    -pe "s/libs.resilience4j.springboot2/'io.github.resilience4j:resilience4j-spring-boot2'/g;" \
    -pe "s/libs.resilience4j.micrometer/'io.github.resilience4j:resilience4j-micrometer'/g;" \
    -pe "s/libs.javax.annotation/'javax.annotation:javax.annotation-api:$JAVAX_ANNOTATION_VERSION'/g;" \
    -pe "s/libs.junit4/'junit:junit:$JUNIT_VERSION'/g;" \
    -pe "s/libs.junit5.jupiter.api/'org.junit.jupiter:junit-jupiter-api'/g;" \
    -pe "s/libs.json.unit.fluent/'net.javacrumbs.json-unit:json-unit-fluent:$JSON_UNIT_VERSION'/g;" \
    -pe "s/libs.assertj/'org.assertj:assertj-core:$ASSERTJ_VERSION'/g;" \
    -pe "s/libs.awaitility/'org.awaitility:awaitility:$AWAITILITY_VERSION'/g;" \
    -pe "s/libs.slf4j.simple/'org.slf4j:slf4j-simple:$SLF4J_VERSION'/g;" \
    -pe "s/libs.slf4j2.api/'org.slf4j:slf4j-api:$SLF4J2_VERSION'/g;" \
    -pe "s/libs.spring.boot2.starter.test/'org.springframework.boot:spring-boot-starter-test'/g;" \
    -pe "s/libs.spring.boot2.starter.web/'org.springframework.boot:spring-boot-starter-web'/g;" \
    -pe "s/libs.spring.boot3.configuration.processor/'org.springframework.boot:spring-boot-configuration-processor:$SPRING_BOOT3_VERSION'/g;" \
    -pe "s/libs.spring.boot3.starter.jetty/'org.springframework.boot:spring-boot-starter-jetty:$SPRING_BOOT3_VERSION'/g;" \
    -pe "s/libs.spring.boot3.starter.test/'org.springframework.boot:spring-boot-starter-test:$SPRING_BOOT3_VERSION'/g;" \
    -pe "s/libs.spring.boot3.starter.web/'org.springframework.boot:spring-boot-starter-web:$SPRING_BOOT3_VERSION'/g;" \
    -pe "s/libs.spring.boot4.configuration.processor/'org.springframework.boot:spring-boot-configuration-processor:$SPRING_BOOT4_VERSION'/g;" \
    -pe "s/libs.spring.boot4.starter.webmvc/'org.springframework.boot:spring-boot-starter-webmvc:$SPRING_BOOT4_VERSION'/g;" \
    -pe "s/libs.spring.boot4.starter.test/'org.springframework.boot:spring-boot-starter-test:$SPRING_BOOT4_VERSION'/g;" \
    -pe "s/libs.spring.boot4.validation/'org.springframework.boot:spring-boot-validation:$SPRING_BOOT4_VERSION'/g;" \
    -pe "s/libs.spring.boot4.webflux.test/'org.springframework.boot:spring-boot-webflux-test:$SPRING_BOOT4_VERSION'/g;" \
    -pe "s/libs.spring.boot4.webmvc.test/'org.springframework.boot:spring-boot-webmvc-test:$SPRING_BOOT4_VERSION'/g;" \
    -pe "s/libs.athenz.zms.client/'com.yahoo.athenz:athenz-zms-java-client:$ATHENZ_VERSION'/g;" \
    -pe "s/libs.athenz.zts.client/'com.yahoo.athenz:athenz-zts-java-client:$ATHENZ_VERSION'/g;" \
    -pe "s/libs.hibernate.validator8/'org.hibernate.validator:hibernate-validator:$HIBERNATE_VERSION'/g;" \
    -pe "s/libs.jackson.databind/'com.fasterxml.jackson.core:jackson-databind:$JACKSON_VERSION'/g;" \
    -pe "s/libs.jackson.dataformat.yaml/'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$JACKSON_VERSION'/g;" \
    -pe "s/libs.spring.cloud.config.server/'org.springframework.cloud:spring-cloud-config-server:$SPRING_CLOUD_CONFIG_SERVER_VERSION'/g;" \
    -pe "s/libs.testcontainers.core/'org.testcontainers:testcontainers:$TESTCONTAINERS_VERSION'/g;" \
    -pe "s/libs.testcontainers.junit.jupiter/'org.testcontainers:testcontainers-junit-jupiter:$TESTCONTAINERS_VERSION'/g;" \
    "$TMPF"

  {
    if [[ "$E" == *grpc* ]]; then
      echo 'buildscript {'
      echo '    dependencies {'
      echo "        classpath 'com.google.protobuf:protobuf-gradle-plugin:$PROTOBUF_GRADLE_PLUGIN_VERSION'"
      echo '    }'
      echo '}'
    fi
    # Add the 'plugins' section.
    PLUGINS=('io.spring.dependency-management')
    PLUGIN_VERSIONS=("$DEPENDENCY_MANAGEMENT_PLUGIN_VERSION")
    SPRING_BOOT_MAJOR=""
    if grep -qF spring-boot4 "$TMPF"; then
      SPRING_BOOT_MAJOR="4"
      PLUGINS+=('org.springframework.boot')
      PLUGIN_VERSIONS+=("$SPRING_BOOT4_VERSION")
    elif grep -qF spring-boot3 "$TMPF"; then
      SPRING_BOOT_MAJOR="3"
      PLUGINS+=('org.springframework.boot')
      PLUGIN_VERSIONS+=("$SPRING_BOOT3_VERSION")
    fi
    echo 'plugins {'
    if grep -qF "id 'application'" "$TMPF"; then
      echo "    id 'application'"
    fi

    if [[ "$E" == *thrift* ]]; then
      echo "    id \"com.linecorp.thrift-gradle-plugin\" version \"${THRIFT_GRADLE_PLUGIN}\""
    fi

    for ((I=0; I<${#PLUGINS[@]}; I++)); do
      echo "    id \"${PLUGINS[$I]}\" version \"${PLUGIN_VERSIONS[$I]}\""
    done
    echo '}'
    echo

    # Remove the application plugin because we added it.
    perl -i -pe 'BEGIN{undef $/;} s/plugins.*?}//smg' "$TMPF"

    # Apply the common plugins.
    echo "apply plugin: 'java'"
    echo "apply plugin: 'eclipse'"
    echo "apply plugin: 'idea'"
    if [[ "$E" == *grpc* ]]; then
      echo "apply plugin: 'com.google.protobuf'"
    fi
    echo

    # Define the repositories.
    echo 'repositories {'
    echo '    mavenCentral()'
    echo '}'
    echo

    # Import the BOM.
    if [[ "$SPRING_BOOT_MAJOR" == "4" ]]; then
      JUNIT_BOM_VERSION="$JUNIT6_VERSION"
    else
      JUNIT_BOM_VERSION="$JUNIT5_VERSION"
    fi
    echo 'dependencyManagement {'
    echo '    imports {'
    echo "        mavenBom 'io.micrometer:micrometer-bom:$MICROMETER_VERSION'"
    echo "        mavenBom 'io.netty:netty-bom:$NETTY_VERSION'"
    echo "        mavenBom 'com.linecorp.armeria:armeria-bom:$VERSION'"
    echo "        mavenBom 'org.junit:junit-bom:$JUNIT_BOM_VERSION'"
    echo "        mavenBom 'io.github.resilience4j:resilience4j-bom:$RESILIENCE4J2_VERSION'"
    echo '    }'
    echo '}'
    echo

    if [[ "$E" == *grpc* ]]; then
      echo 'protobuf {'
      echo '    // Configure the protoc executable.'
      echo '    protoc {'
      echo '        // Download from the repository.'
      echo "        artifact = 'com.google.protobuf:protoc:$PROTOC_VERSION'"
      echo '    }'
      echo
      echo '    // Locate the codegen plugins.'
      echo '    plugins {'
      echo "        // Locate a plugin with name 'grpc'."
      echo '        grpc {'
      echo '            // Download from the repository.'
      echo "            artifact = 'io.grpc:protoc-gen-grpc-java:$PROTOC_GEN_GRPC_VERSION'"
      echo '        }'
      if [[ "$E" == "grpc-reactor" ]]; then
        echo "        // Locate a plugin with name 'reactorGrpc'."
        echo '        reactorGrpc {'
        echo '            // Download from the repository.'
        echo "            artifact = 'com.salesforce.servicelibs:reactor-grpc:$REACTIVE_GRPC_VERSION'"
        echo '        }'
      fi
      echo '    }'
      echo '    generateProtoTasks {'
      echo "        ofSourceSet('main')*.plugins {"
      echo '            grpc {}'
      if [[ "$E" == "grpc-reactor" ]]; then
        echo '            reactorGrpc {}'
      fi
      echo '        }'
      echo '    }'
      echo '}'
      echo
    fi

    # Add common dependencies
    echo 'dependencies {'
    echo "  implementation 'com.google.code.findbugs:jsr305:$JSR305_VERSION'"
    if [[ "$SPRING_BOOT_MAJOR" != "4" ]]; then
      echo "  testImplementation 'junit:junit:$JUNIT_VERSION'"
    fi
    echo "  testImplementation 'org.assertj:assertj-core:$ASSERTJ_VERSION'"
    echo "  testImplementation 'org.junit.jupiter:junit-jupiter-api'"
    echo "  testImplementation 'org.junit.jupiter:junit-jupiter-params'"
    echo "  testRuntimeOnly 'org.junit.platform:junit-platform-commons'"
    echo "  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'"
    echo "  testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine'"
    if [[ "$SPRING_BOOT_MAJOR" != "4" ]]; then
      echo "  testRuntimeOnly 'org.junit.vintage:junit-vintage-engine'"
    fi
    echo '}'
    echo

    # Paste the patched file while removing the redundant empty lines.
    perl -e '
      undef $/; $_=<>;
      s/^(\r?\n)*//;
      s/(\r?\n)(\r?\n){2,}/\1\1/g;
      print
    ' < "$TMPF"
    echo

    # Configure the Java compiler.
    echo 'tasks.withType(JavaCompile) {'
    echo "    sourceCompatibility = '17'"
    echo "    targetCompatibility = '17'"
    echo "    options.encoding = 'UTF-8'"
    echo '    options.debug = true'
    echo "    options.compilerArgs += '-parameters'"
    echo '}'
    echo

    # Configure JUnit.
    echo 'tasks.withType(Test) {'
    echo '    useJUnitPlatform()'
    echo '}'
    echo
  } > "$E/build.gradle"
done

# Update Gradle version
for E in $(find_examples); do
  perl -i -pe "s/distributionUrl=.*$/distributionUrl=https\:\/\/services.gradle.org\/distributions\/gradle-${GRADLE_VERSION}-all.zip/g" \
    "$E/gradle/wrapper/gradle-wrapper.properties"
done

# Test all examples
for E in $(find_examples); do
  echo "Testing $E .."
  pushd "$E" >/dev/null
  ./gradlew -q clean check
  popd >/dev/null
done

echo "Successfully updated the examples for Armeria $VERSION"
