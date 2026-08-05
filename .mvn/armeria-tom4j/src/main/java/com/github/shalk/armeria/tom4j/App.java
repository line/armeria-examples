/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.logging.XMLFormatter;

public class App {
  public static void main(String[] args) throws IOException, URISyntaxException {
    if (args.length < 1) {
      System.out.println("Usage: armeriaPath");
      System.out.println("mvn exec:java -Dexec.args=\"1.27.2 /home/user/armeria /target \"");
      System.exit(1);
    }
    String armeriaCodeDir = args[1];
    String armeriaVersion = args[0];
    String targetDir = args[2];
    System.out.println("reading " + armeriaCodeDir);
    Path depTomlPath = Paths.get(armeriaCodeDir, "dependencies.toml");

    String armeriaCodeExampleDir = armeriaCodeDir + "/examples";
    // find all file
    FindFinder findFinder = new FindFinder();
    List<Path> gradleList = findFinder.getGradleList(armeriaCodeExampleDir);

    // load dependencies.toml
    DepStore depstore = new DepStoreImpl(armeriaVersion, depTomlPath);

    for (Path gradleFilePath : gradleList) {
      // read file to as Pojo GradleFile
      GradleFileReader gradleFileReader = new GradleFileReader();
      // convert  GradleFile to PomFile
      ConvertGradleFileToPomFile convertGradleFileToPomFile =
          new ConvertGradleFileToPomFile(depstore);
      // generate pom.xml from pomFile
      PomFileContentGenerator pomFileContentGenerator = new PomFileContentGenerator();
      XmlFormat xmlFormat = new XmlFormat();
      String pomFileContent =
          gradleFileReader
              .andThen(convertGradleFileToPomFile)
              .andThen(pomFileContentGenerator)
              .andThen(xmlFormat)
              .apply(gradleFilePath);

      // write to  pom.xml
      Path pomFilePath =
          new TargetProcessor(armeriaCodeExampleDir, targetDir).apply(gradleFilePath);
      System.out.println("generating " + pomFilePath);
      Files.write(pomFilePath, pomFileContent.getBytes(StandardCharsets.UTF_8));
    }
  }
}
