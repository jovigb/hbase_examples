package examples.coprocessor;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;

import com.google.protobuf.ServiceException;

import service.coprocessor.ExampleProtos.CountRequest;
import service.coprocessor.ExampleProtos.CountResponse;
import service.coprocessor.ExampleProtos.RowCountService;
import service.coprocessor.RowCountEndpoint;

public class TestRowCountCoprocessor {

	public static void main(String[] args) throws Exception {
		System.setProperty("HADOOP_USER_NAME", "hdfs");

		String tbl = "users";
		if (args.length > 0) {
			tbl = args[0];
		}

		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.property.clientPort", "2181");
		conf.set("hbase.zookeeper.quorum", "192.168.2.180,192.168.2.179,192.168.2.178");
		conf.set("hbase.master", "192.168.2.179:60000");
		TableName tableName = TableName.valueOf(tbl);
		Path path = new Path("hdfs://192.168.2.180:8020/hbase/service_rowcnt.jar");
		Connection connection = ConnectionFactory.createConnection(conf);

		Admin admin = connection.getAdmin();
		admin.disableTable(tableName);
		HTableDescriptor hTableDescriptor = new HTableDescriptor(tableName);
		if (!tbl.equals("users")) {
			HColumnDescriptor columnFamily1 = new HColumnDescriptor("d");
			columnFamily1.setMaxVersions(3);
			hTableDescriptor.addFamily(columnFamily1);
		} else {
			HColumnDescriptor columnFamily1 = new HColumnDescriptor("personalDet");
			columnFamily1.setMaxVersions(3);
			hTableDescriptor.addFamily(columnFamily1);
			HColumnDescriptor columnFamily2 = new HColumnDescriptor("salaryDet");
			columnFamily2.setMaxVersions(3);
			hTableDescriptor.addFamily(columnFamily2);
		}
		hTableDescriptor.addCoprocessor(RowCountEndpoint.class.getCanonicalName(), path, Coprocessor.PRIORITY_USER,
				null);
		admin.modifyTable(tableName, hTableDescriptor);
		admin.enableTable(tableName);

		Table table = connection.getTable(tableName);

		final CountRequest request = CountRequest.newBuilder().build();
		try {
			Long totalSum = 0L;
			Map<byte[], Long> results = table.coprocessorService(RowCountService.class, null, null,
					new Batch.Call<RowCountService, Long>() {
						@Override
						public Long call(RowCountService aggregate) throws IOException {
							BlockingRpcCallback<CountResponse> rpcCallback = new BlockingRpcCallback<CountResponse>();
							aggregate.getRowCount(null, request, rpcCallback);
							CountResponse response = rpcCallback.get();
							return response.hasCount() ? response.getCount() : 0L;
						}
					});
			for (Long sum : results.values()) {
				System.out.println("Sum = " + sum);
				totalSum += sum;
			}
			System.out.println("TotalSum = " + totalSum);
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
