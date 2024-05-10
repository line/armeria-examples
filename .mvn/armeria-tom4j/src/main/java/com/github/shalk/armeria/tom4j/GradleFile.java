/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class GradleFile {

  private String filename;
  private Map<String, String> dep = new HashMap<>();

  private String exec;
}
