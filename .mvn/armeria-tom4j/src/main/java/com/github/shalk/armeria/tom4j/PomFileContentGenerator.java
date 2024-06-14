/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.io.IOUtils;

public class PomFileContentGenerator implements Function<PomFile, String> {

  public String depToString(Dep dep) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(head("dependency"));
    buffer.append(combine("groupId", dep.getGroup()));
    buffer.append(combine("artifactId", dep.getArtifact()));
    buffer.append(combine("version", dep.getVersion()));
    if (dep.getScope() != null) {
      buffer.append(combine("scope", dep.getScope()));
    }
    if (!dep.getExcludes().isEmpty()) {
      buffer.append("<exclusions>\n");
      for (String exclude : dep.getExcludes()) {
        String[] split = exclude.split(":");
        buffer.append(" <exclusion>\n");
        buffer.append(combine("groupId", split[0]));
        buffer.append(combine("artifactId", split[1]));
        buffer.append(" </exclusion>\n");
      }
      buffer.append("</exclusions>\n");
    }
    buffer.append(tail("dependency"));
    return buffer.toString();
  }

  public String head(String tag) {
    return "<" + tag + ">\n";
  }

  public String tail(String tag) {
    return "</" + tag + ">\n";
  }

  public String combine(String tag, String content) {
    return "<" + tag + ">" + content + "</" + tag + ">\n";
  }

  public String apply(PomFile pomFile) {
    StringBuilder builder = new StringBuilder();
    builder.append(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "\n");
    // gav
    builder.append(combine("groupId", pomFile.getG()));
    builder.append(combine("artifactId", pomFile.getA()));
    builder.append(combine("version", pomFile.getV()));
    // properties
    builder.append("    <properties>\n");
    builder.append(combine("maven.compiler.source", "17"));
    builder.append(combine("maven.compiler.target", "17"));
    builder.append(combine("project.build.sourceEncoding", "UTF-8"));
    Map<String, String> properties = pomFile.getProperties();
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      builder.append(combine(entry.getKey(), entry.getValue()));
    }
    builder.append("    </properties>\n");

    // deps
    builder.append("    <dependencies>\n");
    List<Dep> deps = pomFile.getDep();
    for (Dep dep : deps) {
      builder.append(depToString(dep));
    }

    builder.append("    </dependencies>\n");

    builder.append("<build>\n");
    List<String> plugins = pomFile.getPlugin();
    if (!plugins.isEmpty()) {
      builder.append("<plugins>\n");
      for (String plugin : plugins) {
        List<String> pluginLine = PluginUtil.getPlugin(plugin);
        pluginLine.forEach(line -> builder.append(line).append("\n"));
      }
      builder.append("</plugins>\n");
    }
    List<String> extensions = pomFile.getExtension();
    if (!extensions.isEmpty()) {
      builder.append("<extensions>\n");
      for (String extension : extensions) {
        List<String> lines = ExtUtil.getExt(extension);
        lines.forEach(line -> builder.append(line).append("\n"));
      }
      builder.append("</extensions>\n");
    }

    builder.append("</build>\n");
    builder.append("</project>");
    return builder.toString();
  }
}

class ExtUtil {

  public static List<String> getExt(String name) {
    try (InputStream in =
        ExtUtil.class.getClassLoader().getResourceAsStream("ext-" + name + ".xml")) {
      return IOUtils.readLines(in, StandardCharsets.UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
}

class PluginUtil {

  public static List<String> getPlugin(String name) {
    try (InputStream in =
        ExtUtil.class.getClassLoader().getResourceAsStream("plugin-" + name + ".xml")) {
      return IOUtils.readLines(in, StandardCharsets.UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
}
