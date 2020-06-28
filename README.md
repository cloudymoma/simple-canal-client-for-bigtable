## Install and start canal

Please refer to the [doc](https://github.com/alibaba/canal/wiki/QuickStart)

+ Configure source MySQL instance: IP, user, password, etc.
+ Create source table
```
Create table test(id number, col1 char(30), col2 char(30));
```
+ Start the canal

## Start the simple canal client for bigtable

+ Configure target Cloud Bigtable: projectId, instanceId, tableId, etc in the simple.conf file.
+ create cloud bigtable table by using [cbt](https://cloud.google.com/bigtable/docs/cbt-reference)
```
cbt createtble test
cbt createfamily test cf1
```
+ start the simple canal client
```
mvn package assembly:single
java -jar xxx.jar
```

## Generate workload and observe the result

```
cbt -project binguo-learning-centre -instance binguo-test read test-bin
```

## To Do list

Please help to ignore the operations from maintenance in Cloud SQL for MySQL