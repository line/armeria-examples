/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleFileReader implements Function<Path, GradleFile> {
  static String rex = "^\\s*(implementation|runtimeOnly|testImplementation|compileOnly|annotationProcessor)\\s+(\\S+)";
  static String rex2 =
      "^\\s*(implementation|runtimeOnly|testImplementation|compileOnly|annotationProcessor)\\((\\S+)\\)";

  static Pattern p = Pattern.compile(rex);
  static Pattern p2 = Pattern.compile(rex2);

  public GradleFile apply(Path path) {
    GradleFile file = new GradleFile();
    file.setFilename(path.toString());
    List<String> lines = null;
    try {
      lines = Files.readAllLines(path);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    for (String line : lines) {
      Matcher matcher = p.matcher(line);
      if (matcher.matches()) {
        String group1 = matcher.group(1);
        String group2 = matcher.group(2);
        if (!file.getDep().containsKey(group2)) {
          file.getDep().put(group2, group1);
        } else if (file.getDep().get(group2).equals("testImplementation")) {
          file.getDep().put(group2, group1);
        } else {
          new RuntimeException("unkown " + group1 + group2);
        }
      }
      Matcher matcher2 = p2.matcher(line);
      if (matcher2.matches()) {
        String group1 = matcher2.group(1);
        String group2 = matcher2.group(2);
        if (!file.getDep().containsKey(group2)) {
          file.getDep().put(group2, group1);
        } else if (group1.equals("testImplementation")) {
          file.getDep().put(group2, group1);
        } else {
          new RuntimeException("unkown " + group1 + group2);
        }
      }
    }
    return file;
  }
}
