/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

@AllArgsConstructor
public class TargetProcessor implements UnaryOperator<Path> {
  String source;
  String target;

  @SneakyThrows
  @Override
  public Path apply(Path gradleFIle) {
    Path targetPom = trans(gradleFIle);
    FileUtils.forceMkdir(
        new File(gradleFIle.getParent().toString().replaceAll(this.source, this.target)));
    return targetPom;
  }

  Path trans(Path gradleFIle) {
    Path targetPom =
        Paths.get(
            gradleFIle.getParent().toString().replaceAll(this.source, this.target), "pom.xml");
    return targetPom;
  }
}
