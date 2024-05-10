/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindFinder {

  List<Path> getGradleList(String path) throws IOException {
    Stream<Path> pathStream =
        Files.find(
            Paths.get(path),
            10,
            new BiPredicate<Path, BasicFileAttributes>() {
              @Override
              public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                return path.endsWith("build.gradle") && !path.getParent().toString().endsWith("-scala") && !path.getParent().toString().endsWith("-sangria");
              }
            });
    return pathStream.collect(Collectors.toList());
  }
}
