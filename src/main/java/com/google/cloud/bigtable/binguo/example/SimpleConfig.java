package com.google.cloud.bigtable.binguo.example;

import com.typesafe.config.Config;

public class SimpleConfig {
  public final String projectId;
  public final String instanceId;
  public final String tableId;

  public SimpleConfig(Config config) {
    this.projectId = config.getString("simple.projectId");
    this.instanceId = config.getString("simple.instanceId");
    this.tableId = config.getString("simple.tableId");
  }
}

