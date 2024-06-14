/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ConvertGradleFileToPomFile implements Function<GradleFile, PomFile> {

  private final ScopeUtil scopeUtil = new ScopeUtil();
  private final DepStore depStore;

  public ConvertGradleFileToPomFile(DepStore depStore) {
    this.depStore = depStore;
  }

  static String getA(String filename) {
    int index = filename.indexOf("examples");
    String sub = filename.substring(index + "examples".length());
    int end = sub.lastIndexOf("/");
    String mid = sub.substring(0, end);
    String replace = mid.replace("/", "-");
    return "example" + replace;
  }

  public PomFile apply(GradleFile k) {
    PomFile pomFile = new PomFile();
    String filename = k.getFilename();
    pomFile.setFilename(filename);
    pomFile.setG("com.linecorp.armeria");
    pomFile.setA(getA(filename));
    pomFile.setV("1.0.0-SNAPSHOT");
    List<Dep> deps = getDeps(k);

    // deps corner case
    if (pomFile.getA().contains("dagger")) {
      String version = depStore.getVersion("dagger");
      Dep dep = new Dep();
      dep.setVersion(version);
      dep.setGroup("com.google.dagger");
      dep.setArtifact("dagger-compiler");
      dep.setScope("provided");
      deps.add(dep);
    }
    if (pomFile.getA().contains("spring-boot-jetty")) {
      String version = depStore.getVersion("spring-boot3");
      Dep dep = new Dep();
      dep.setVersion(version);
      dep.setGroup("org.springframework.boot");
      dep.setArtifact("spring-boot-starter-web");
      dep.getExcludes().add("org.springframework.boot:spring-boot-starter-tomcat");
      deps.add(dep);
    }
    if (pomFile.getA().equals("example-dropwizard")) {
      String version = depStore.getVersion("assertj");
      Dep dep = new Dep();
      dep.setVersion(version);
      dep.setGroup("org.assertj");
      dep.setArtifact("assertj-core");
      dep.setScope("test");
      deps.add(dep);
    }

    if (pomFile.getA().equals("example-dropwizard")) {
      String version = depStore.getVersion("junit5");
      Dep dep = new Dep();
      dep.setVersion(version);
      dep.setGroup("org.junit.jupiter");
      dep.setArtifact("junit-jupiter");
      dep.setScope("test");
      deps.add(dep);
    }
    if (pomFile.getA().equals("example-spring-boot-jetty")) {
      String version = depStore.getVersion("jetty12");
      Dep dep = new Dep();
      dep.setVersion("12.0.7");
      dep.setGroup("org.eclipse.jetty");
      dep.setArtifact("jetty-server");
      deps.add(dep);
    }

    if (pomFile.getA().equals("example-dropwizard")) {
      String version = depStore.getVersion("junit5");
      Dep dep = new Dep();
      dep.setVersion(version);
      dep.setGroup("org.junit.jupiter");
      dep.setArtifact("junit-jupiter");
      dep.setScope("test");
      deps.add(dep);
    }
    if (pomFile.getA().equals("example-spring-boot-minimal") ||pomFile.getA().equals("example-spring-boot-tomcat")
        ||  pomFile.getA().equals("example-spring-boot-webflux") ) {
      String version = depStore.getVersion("slf4j2");
      Dep dep = new Dep();
      dep.setVersion(version);
      dep.setGroup("org.slf4j");
      dep.setArtifact("slf4j-api");
      deps.add(dep);
    }

    if (pomFile.getA().equals("example-graphql")) {
      String version = depStore.getVersion("junit5");
      Dep dep = new Dep();
      dep.setVersion(version);
      dep.setGroup("org.junit.jupiter");
      dep.setArtifact("junit-jupiter-params");
      dep.setScope("test");
      deps.add(dep);
    }
    pomFile.setDep(deps);

    // plugin corner case
    if (pomFile.getA().contains("grpc")
        && !pomFile.getA().equals("grpc-scala")
        && !pomFile.getA().equals("grpc-reactor")) {
      pomFile.getPlugin().add("grpc");
      pomFile.getExtension().add("os");
      String grpcJavaVersion = depStore.getVersion("grpc-java");
      pomFile.getProperties().put("dep.grpc-java.version", grpcJavaVersion);

      String protobufVersion = depStore.getVersion("protobuf");
      pomFile.getProperties().put("dep.protobuf.version", protobufVersion);
    }

    if (pomFile.getA().contains("thrift")) {
      pomFile.getPlugin().add("thrift");
    }
    if (pomFile.getA().contains("grpc-scala")) {
      pomFile.getExtension().add("os");
      pomFile.getPlugin().add("grpc-scala");
    } else if (pomFile.getA().contains("grpc-reactor")) {
      pomFile.getExtension().add("os");
      pomFile.getPlugin().add("grpc-reactor");
    }

    pomFile.getPlugin().add("compiler");
    pomFile.getPlugin().add("surefire");

    return pomFile;
  }

  List<Dep> getDeps(GradleFile k) {
    List<Dep> depList = new ArrayList<>();
    Map<String, String> depMap = k.getDep();
    for (Map.Entry<String, String> entry : depMap.entrySet()) {
      String lib = entry.getKey();
      String type = entry.getValue();
      Dep dep = depStore.getDep(lib);
      dep.setScope(scopeUtil.getScope(type));
      if (lib.equals("libs.javax.annotation")) {
        dep.setScope("compile");
      }
      depList.add(dep);
    }

    return depList;
  }
}

class ScopeUtil {

  public Map<String, String> map;

  public ScopeUtil() {
    map = new HashMap<>();
    map.put("testImplementation", "test");
    map.put("runtimeOnly", "runtime");
    map.put("compileOnly", "compile");
    map.put("Implementation", "compile");
  }

  public String getScope(String type) {
    return map.get(type);
  }
}
