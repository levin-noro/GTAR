package transportation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class SetPropsForFocus {

	private static String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/MovieLens/movielens.db";

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

		System.out.println("dataset: " + dataGraphPath);

		Transaction tx1 = dataGraph.beginTx();

		FileInputStream fis = new FileInputStream("setgroup.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		int cnt = 1;
		while ((line = br.readLine()) != null) {
			String[] split = line.split(";");
			int gId = Integer.parseInt(split[0]);
			long autoId = Long.parseLong(split[1]);

			dataGraph.getNodeById(autoId).setProperty("newGroup", gId);

			if (cnt % 1000000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
				System.out.println("cnt: " + cnt);
			}

		}

		br.close();

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

}
