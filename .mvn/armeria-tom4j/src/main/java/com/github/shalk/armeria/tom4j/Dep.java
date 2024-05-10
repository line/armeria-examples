/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Dep {
  private String name;
  private String group;
  private String artifact;
  private String version;
  private String scope;
  private List<String> excludes = new ArrayList<>();
}
