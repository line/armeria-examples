/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class DepStoreImplTest {
  @Test
  public void test() throws IOException, URISyntaxException {
    Path path = Paths.get(ClassLoader.getSystemResource("dependencies.toml").toURI());
    DepStoreImpl depStore = new DepStoreImpl("1.27.2", path);
    Dep dep = depStore.getDep("libs.assertj");
    String name = new PomFileContentGenerator().depToString(dep);
    System.out.println("name = " + name);
  }
}
