/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class TargetProcessorTest {

  @Test
  void apply() throws IOException {

    Path tmp1 = Files.createTempDirectory("aa");
    Path tmp2 = Files.createTempDirectory("bb");
    String midle = "ccc/ddd/eee";
    Path path1 = Paths.get(tmp1.toString(), midle, "build.gradle.kts");
    TargetProcessor processor = new TargetProcessor(tmp1.toString(), tmp2.toString());
    assertEquals(Paths.get(tmp2.toString(), midle, "pom.xml"), processor.trans(path1));
  }
}
