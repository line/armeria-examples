/* Licensed under Apache-2.0 2024. */
package com.github.shalk.armeria.tom4j;

public interface DepStore {

  Dep getDep(String name);

  String getVersion(String name);
}
