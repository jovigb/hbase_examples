package examples.coprocessor;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import service.coprocessor.RegionObserverExample;

public class TestObserverCoprocessor {

	public static void main(String[] args) throws Exception {
		System.setProperty("HADOOP_USER_NAME", "hdfs");

		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.property.clientPort", "2181");
		conf.set("hbase.zookeeper.quorum", "192.168.2.180,192.168.2.179,192.168.2.178");
		conf.set("hbase.master", "192.168.2.179:60000");
		TableName tableName = TableName.valueOf("users");
		Path path = new Path("hdfs://192.168.2.180:8020/hbase/service_rowcnt.jar");
		Connection connection = ConnectionFactory.createConnection(conf);

		Admin admin = connection.getAdmin();
		admin.disableTable(tableName);
		HTableDescriptor hTableDescriptor = new HTableDescriptor(tableName);
		HColumnDescriptor columnFamily1 = new HColumnDescriptor("personalDet");
		columnFamily1.setMaxVersions(3);
		hTableDescriptor.addFamily(columnFamily1);
		HColumnDescriptor columnFamily2 = new HColumnDescriptor("salaryDet");
		columnFamily2.setMaxVersions(3);
		hTableDescriptor.addFamily(columnFamily2);
		hTableDescriptor.addCoprocessor(RegionObserverExample.class.getCanonicalName(), path, Coprocessor.PRIORITY_USER,
				null);
		admin.modifyTable(tableName, hTableDescriptor);
		admin.enableTable(tableName);

		Table table = connection.getTable(tableName);
		Get get = new Get(Bytes.toBytes("admin"));
		Result result = table.get(get);
		for (Cell c : result.rawCells()) {
			System.out.println(Bytes.toString(CellUtil.cloneRow(c)) + "==> " + Bytes.toString(CellUtil.cloneFamily(c))
					+ "{" + Bytes.toString(CellUtil.cloneQualifier(c)) + ":" + Bytes.toString(CellUtil.cloneValue(c))
					+ "}");
		}
		System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		Scan scan = new Scan();
		ResultScanner scanner = table.getScanner(scan);
		for (Result res : scanner) {
			for (Cell c : res.rawCells()) {
				System.out.println(Bytes.toString(CellUtil.cloneRow(c)) + " ==> "
						+ Bytes.toString(CellUtil.cloneFamily(c)) + " {" + Bytes.toString(CellUtil.cloneQualifier(c))
						+ ":" + Bytes.toString(CellUtil.cloneValue(c)) + "}");
			}
		}
	}
}
