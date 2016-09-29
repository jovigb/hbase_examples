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

import service.coprocessor.Sum.SumRequest;
import service.coprocessor.Sum.SumResponse;
import service.coprocessor.Sum.SumService;
import service.coprocessor.SumEndPoint;

public class TestSumCoprocessor {

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
		hTableDescriptor.addCoprocessor(SumEndPoint.class.getCanonicalName(), path, Coprocessor.PRIORITY_USER, null);
		admin.modifyTable(tableName, hTableDescriptor);
		admin.enableTable(tableName);

		Table table = connection.getTable(tableName);

		final SumRequest request = SumRequest.newBuilder().setFamily("salaryDet").setColumn("gross").build();
		try {
			Map<byte[], Long> results = table.coprocessorService(SumService.class, null, null,
					new Batch.Call<SumService, Long>() {
						@Override
						public Long call(SumService aggregate) throws IOException {
							BlockingRpcCallback<SumResponse> rpcCallback = new BlockingRpcCallback<SumResponse>();
							aggregate.getSum(null, request, rpcCallback);
							SumResponse response = rpcCallback.get();
							return response.hasSum() ? response.getSum() : 0L;
						}
					});
			for (Long sum : results.values()) {
				System.out.println("Sum = " + sum);
			}
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
