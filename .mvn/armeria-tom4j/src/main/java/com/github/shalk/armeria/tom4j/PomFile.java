/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class PomFile {

  private String filename;
  private String g;
  private String a;
  private String v;

  private List<Dep> dep;

  private List<String> plugin = new ArrayList<>();
  private List<String> extension = new ArrayList<>();

  private Map<String, String> properties = new HashMap<>();

  private String execMain;
}
