package com.google.cloud.bigtable.binguo.example;

import com.google.cloud.bigtable.data.v2.models.BulkMutation;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.bigtable.data.v2.models.Mutation;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.common.utils.AddressUtils;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.protocol.CanalEntry.Column;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.Header;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.CanalEntry.RowChange;
import com.alibaba.otter.canal.protocol.CanalEntry.RowData;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SimpleCanalClient {

    private static final Config config = ConfigFactory.load("simple.conf");
    private static final SimpleConfig simpleConfig = new SimpleConfig(config);

    private static BigtableUtils bigtableUtils = new BigtableUtils(simpleConfig.projectId, simpleConfig.instanceId);
    private static final String tableId = simpleConfig.tableId;

    public SimpleCanalClient() throws IOException {
    }

    public static void main(String args[]) {
        // create the connection
        CanalConnector connector = CanalConnectors
            .newSingleConnector(new InetSocketAddress(AddressUtils.getHostIp(),
                11111), "example", "", "");
        int batchSize = 1000;
        int emptyCount = 0;
        Long batchId = null;
        try {
            connector.connect();
            connector.subscribe(".*\\..*");
            connector.rollback();
            int totalEmptyCount = 1200;
            while (emptyCount < totalEmptyCount) {
                Message message = connector.getWithoutAck(batchSize); // Obtain the events
                batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    emptyCount++;
                    System.out.println("waiting...");
                    System.out.println("empty count : " + emptyCount);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    emptyCount = 0;
                    // System.out.printf("message[batchId=%s,size=%s] \n", batchId, size);
                    manageEntry(message.getEntries());
                }

                connector.ack(batchId); // Confirm commit
            }

            System.out.println("empty too many times, exit");
        } catch (Exception e) {
            if (batchId != null)
                connector.rollback(batchId); // Fail and rollback
            System.out.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            connector.disconnect();
        }
    }

    private static void manageEntry(List<Entry> entries) {
        //Loop
        for (Entry entry : entries) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN
                || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                continue;
            }

            RowChange rowChange = null;
            try {
                rowChange = RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException(
                    "ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),
                    e);
            }
            //Display events
            EventType eventType = rowChange.getEventType();
            Header header = entry.getHeader();
            System.out.println(
                String.format("================;\n binlog[%s:%s] , name[%s,%s] , eventType : %s",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                    eventType));
            //Manage events
            for (RowData rowData : rowChange.getRowDatasList()) {
                if (eventType == EventType.DELETE) {
                    printColumn(rowData.getBeforeColumnsList());
                    deleteEvent(rowData.getBeforeColumnsList());
                } else if (eventType == EventType.INSERT) {
                    printColumn(rowData.getAfterColumnsList());
                    updateEvent(rowData.getAfterColumnsList());
                } else if (eventType == EventType.UPDATE) {
                    System.out.println("-------;\n before");
                    printColumn(rowData.getBeforeColumnsList());
                    System.out.println("-------;\n after");
                    printColumn(rowData.getAfterColumnsList());
                    updateEvent(rowData.getAfterColumnsList());
                }
            }
        }
    }

    private static void printColumn(List<Column> columns) {
        for (Column column : columns) {
            System.out.println(
                column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
        }
    }

    private static void deleteEvent(List<Column> columns) {

        try {
            String rowKey = getKey(columns).toString();
            RowMutation mutation = RowMutation.create(tableId, rowKey).deleteRow();

            bigtableUtils.getClient().mutateRow(mutation);

        } catch (Exception e) {
            System.out.println("Error during Delete: \n" + e.toString());
        }
    }

    private static void updateEvent(List<Column> columns) {
        BulkMutation batch = BulkMutation.create(tableId);
        Long key = getKey(columns);
        try {
            for (Column column : columns) {
                System.out.println(
                    column.getName() + " : " + column.getValue() + "    update=" + column
                        .getUpdated());
                if (!column.getName().equals("id") && column.getUpdated()) {
                    batch.add(key.toString(),
                        Mutation.create().setCell("cf1", column.getName(), column.getValue()));
                }
            }
            bigtableUtils.getClient().bulkMutateRows(batch);
        }catch (Exception e) {
            System.out.println("Error during Insert / Update: \n" + e.toString());
        }
    }

    private static Long getKey(List<Column> columns){
        try{
            for (Column column : columns) {
                if(column.getName().equals("id")){
                    return Long.valueOf(column.getValue());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            throw  new RuntimeException("Not found primary key !");
        }
        throw  new RuntimeException("Not found primary key !");
    }
}
