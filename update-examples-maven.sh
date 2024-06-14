#!/bin/bash -e

function generate_maven_pom_xml() {
    local VERSION=$1
    local SRC_DIR=$2
    local CUR_DIR=$3

    echo 'Generate maven pom.xml for examples ...'
    # clone shalk/armeria-tom4j to `.mvn/armeria-tom4` or pull if exists
    local MVN_TOOL_DIR=".mvn/armeria-tom4j"
    pushd $MVN_TOOL_DIR || exit 1
    mvn clean compile exec:java -Dexec.args="$VERSION $SRC_DIR $CUR_DIR"
    popd
}

CUR_DIR=$(pwd)
generate_maven_pom_xml "$VERSION" "$SRC_DIR" "$CUR_DIR"

echo 'Testing examples by ./mvnw clean verify ...'
./mvnw clean verify
