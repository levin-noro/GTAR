//package datasetModif;
//
//import java.io.File;
//
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.graphdb.factory.GraphDatabaseSettings;
//
//public class FastModif {
//
//	private static String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/idsJun17_V3.db";
//
//	public static void main(String[] args) {
//
//		File storeDir = new File(dataGraphPath);
//		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
//				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
//				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();
//
//		System.out.println("dataset: " + dataGraphPath);
//
//		Transaction tx1 = dataGraph.beginTx();
//
//		int cnt = 0;
//		for (Node node : dataGraph.getAllNodes()) {
//			if (node.hasProperty("startTime ")) {
//				node.setProperty("startTime", node.getProperty("startTime "));
//				node.removeProperty("startTime ");
//				cnt++;
//				if (cnt % 100000 == 0) {
//					tx1.success();
//					tx1.close();
//					tx1 = dataGraph.beginTx();
//					System.out.println("cnt:" + cnt);
//				}
//			}
//		}
//
//		tx1.success();
//		tx1.close();
//		dataGraph.shutdown();
//
//	}
//
//}
