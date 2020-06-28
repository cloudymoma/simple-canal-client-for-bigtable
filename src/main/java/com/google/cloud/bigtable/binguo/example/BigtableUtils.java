package com.google.cloud.bigtable.binguo.example;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import java.io.IOException;

public class BigtableUtils {

  public BigtableDataClient dataClient;

  public BigtableUtils(String projectId,String instanceId) {
    try {
      BigtableDataSettings settings =
          BigtableDataSettings.newBuilder().setProjectId(projectId).setInstanceId(instanceId)
              .build();

      dataClient = BigtableDataClient.create(settings);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  public BigtableDataClient getClient() {
    return dataClient;
  }
}

