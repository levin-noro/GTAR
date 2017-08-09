package transportation;

import java.io.File;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class FindActiveTimestamps {

	private static String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/Synthetics/TransVaryingT/trans_3.db";

	public static void main(String[] args) throws Exception {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			}
		}

		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		System.out.println("newPath: " + dataGraphPath);
		Transaction tx1 = dataGraph.beginTx();

		HashSet<Integer> activeTimestamps = new HashSet<Integer>();
		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (rel.hasProperty("timepoints")) {
				int[] timepoints = (int[]) rel.getProperty("timepoints");
				for (int t = 0; t < timepoints.length; t++) {
					activeTimestamps.add(timepoints[t]);
				}
			}
		}

		for (Integer activeTimestamp : activeTimestamps) {
			System.out.println(activeTimestamp);
		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();
	}

}
