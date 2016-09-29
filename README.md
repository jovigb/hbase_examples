# hbase examples

---
client mapreduce coprocessor examples

## Previous

create table by hbase shell 
```
create 'users', 'personalDet', 'salaryDet'
put 'users','admin','personalDet:name','Admin'
put 'users','admin','personalDet:lastname','Admin'
put 'users','cdickens','personalDet:name','Charles'
put 'users','cdickens','personalDet:lastname','Dickens'
put 'users','cdickens','personalDet:dob','02/07/1812'
put 'users','cdickens','salaryDet:gross','10000'
put 'users','cdickens','salaryDet:net','8000'
put 'users','cdickens','salaryDet:allowances','2000'
put 'users','jverne','personalDet:name','Jules'
put 'users','jverne','personalDet:lastname','Verne'
put 'users','jverne','personalDet:dob','02/08/1828'
put 'users','jverne','salaryDet:gross','12000'
put 'users','jverne','salaryDet:net','9000'
put 'users','jverne','salaryDet:allowances','3000'
```

put local coprocessor jar to hdfs
```
hdfs dfs -put -f service_rowcnt.jar /hbase
```

## Usage

```
HADOOP_CLASSPATH=`hbase classpath`:`hadoop classpath`:~/hbase_examples.jar hadoop jar hbase_examples.jar examples.mapred.SampleUploader hdfs://cdh180:8020/user/hive/warehouse/apachelog/000001_0 mytest
```

## hbase shell 

split region by create table
```
create 'mytest',{NAME=>'d',VERSIONS=>1,BLOCKCACHE=>true,BLOOMFILTER=>'ROW',COMPRESSION=>'SNAPPY'},{SPLITS => ['1','2','3','4','5','6','7','8','9','a','b','c','d','e']}
```

alter coprocesser on table
```
disable 'users'
alter 'users', METHOD => 'table_att', 'coprocessor'=>'hdfs://cdh180:8020/hbase/service_rowcnt.jar|examples.coprocessor.SumEndPoint||'
enable 'users'
```

clear coprocesser
```
disable 'users'
alter 'users', METHOD => 'table_att_unset', NAME => 'coprocessor$1'
enable 'users'
```

## Notice

```
Must restart hbase if you update service_rowcnt.jar. 
coprocessor jar has been cached. I dont know how to clear the cache.
```

## Reference
```
http://www.3pillarglobal.com/insights/hbase-coprocessors
https://github.com/apache/hbase/tree/rel/1.2.0/hbase-examples/src/main/java/org/apache/hadoop/hbase
http://hbase.apache.org/book.html#cp_example
```