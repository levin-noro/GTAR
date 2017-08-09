package graphMakers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import utilities.Dummy.DummyFunctions;

public class HospitalWardMaker {

	public static void main(String[] args) throws Exception {
		HospitalWardMaker h = new HospitalWardMaker();
		h.createNeo4j();

	}

	private void createNeo4j() throws Exception {
		FileInputStream fis = new FileInputStream(
				"/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/HealthNetwork/detailed_list_of_contacts_Hospital.txt");

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		ArrayList<HWarden> healthData = new ArrayList<HWarden>();
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\t");
			healthData.add(new HWarden(Integer.parseInt(items[0]) / 20, Integer.parseInt(items[1]),
					Integer.parseInt(items[2]), items[3], items[4]));
		}

		br.close();

		String newDataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/HealthNetwork/graph";
		File storeDir = new File(newDataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig("cache_type", "none").setConfig(GraphDatabaseSettings.pagecache_memory, "245760")
				.newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		HashMap<Long, Node> nodeOfIndex = new HashMap<Long, Node>();

		Transaction tx1 = dataGraph.beginTx();

		for (HWarden hWarden : healthData) {
			Node fromIdNode;
			if (!nodeOfIndex.containsKey(hWarden.fromId)) {
				fromIdNode = dataGraph.createNode();
				fromIdNode.addLabel(Label.label(hWarden.fromType));
				fromIdNode.setProperty("ID", hWarden.fromId);
				nodeOfIndex.put(fromIdNode.getId(), fromIdNode);
			} else {
				fromIdNode = nodeOfIndex.get(hWarden.fromId);
			}

			Node toIdNode;
			if (!nodeOfIndex.containsKey(hWarden.toId)) {
				toIdNode = dataGraph.createNode();
				toIdNode.addLabel(Label.label(hWarden.toType));
				toIdNode.setProperty("ID", hWarden.toId);
				nodeOfIndex.put(toIdNode.getId(), toIdNode);
			} else {
				toIdNode = nodeOfIndex.get(hWarden.toId);
			}

			boolean handled = false;
			for (Relationship rel : fromIdNode.getRelationships(Direction.OUTGOING)) {
				if (rel.getEndNode().getId() == toIdNode.getId()) {
					int[] timepoints = (int[]) rel.getProperty("timepoints");

					if (timepoints[timepoints.length - 1] == (hWarden.endTime - 1)) {
						timepoints[timepoints.length - 1] = hWarden.endTime;
						rel.setProperty("timepoints", timepoints);
					} else {

						int[] newTimepoints = new int[timepoints.length + 2];
						for (int l = 0; l < timepoints.length; l++) {
							newTimepoints[l] = timepoints[l];
						}

						newTimepoints[timepoints.length] = hWarden.endTime - 1;
						newTimepoints[timepoints.length + 1] = hWarden.endTime;

						rel.setProperty("timepoints", newTimepoints);

						System.out
								.println(hWarden.fromId + "->" + hWarden.toId + " t:" + Arrays.toString(newTimepoints));
					}

					handled = true;
				}
			}

			if (!handled) {

				Relationship rel = fromIdNode.createRelationshipTo(toIdNode, RelationshipType.withName("contact"));
				int[] newTimepoints = new int[2];
				newTimepoints[0] = hWarden.endTime - 1;
				newTimepoints[1] = hWarden.endTime;
				rel.setProperty("timepoints", newTimepoints);
			}
		}

		tx1.success();
		tx1.close();

		dataGraph.shutdown();
		System.out.println("done!");
	}

}

class HWarden {
	int endTime;
	long fromId;
	long toId;
	String fromType;
	String toType;

	public HWarden(int endTime, long fromId, long toId, String fromType, String toType) {
		this.endTime = endTime;
		this.fromId = fromId;
		this.toId = toId;
		this.fromType = fromType;
		this.toType = toType;

	}
}
