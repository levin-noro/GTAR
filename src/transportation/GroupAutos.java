package transportation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

public class GroupAutos {

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

		int cnt = 0;

		ArrayList<Long> autoNodesArr = new ArrayList<Long>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.getLabels().iterator().next().name().toString().equals("User")) {
				autoNodesArr.add(node.getId());
			}
		}

		System.out.println("autoNodesArr size: " + autoNodesArr.size());

		Random random = new Random();

		File fout = new File("movieLensUsersGroup.txt");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		int groupId = 1;
		HashMap<Integer, ArrayList<Long>> autoIdsOfAGroup = new HashMap<Integer, ArrayList<Long>>();
		Collections.shuffle(autoNodesArr);

		int i = 0;
		while (i < autoNodesArr.size()) {
			int groupSize = random.nextInt(100)+10;

			autoIdsOfAGroup.put(groupId, new ArrayList<Long>());
			autoIdsOfAGroup.get(groupId).addAll(autoNodesArr.subList(i, Math.min(i + groupSize, autoNodesArr.size())));
			i += autoIdsOfAGroup.get(groupId).size();
			System.out.println("groupId: " + groupId + " selected group size: " + autoIdsOfAGroup.get(groupId).size());
			groupId++;

		}

		for (Integer gId : autoIdsOfAGroup.keySet()) {
			for (Long nodeId : autoIdsOfAGroup.get(gId)) {
				bw.write(gId + ";" + nodeId);
				bw.newLine();
			}
		}

		bw.close();
		// cnt++;
		// if (cnt % 100000 == 0) {
		// tx1.success();
		// tx1.close();
		// tx1 = dataGraph.beginTx();
		// System.out.println("cnt:" + cnt);
		// }

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

}
