package transportation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import utilities.Dummy.DummyFunctions;

public class TransportationRead {

	public String transportationPath = "";
	public static String newDataGraphPath = "";

	// autoId;color ;speed ;linklanes;nextLink;lecoord ;timestamp
	// 1;ff000000;26.957581172583236;1-1;10000;-475.165 523.480 0.000;0

	int AUTOIDIndex = 0;
	int COLORIndex = 1;
	int SPEEDIndex = 2;
	int LINKLANESIndex = 3;
	int NEXTLINKIndex = 4;
	int LECOORDIndex = 5;
	int TIMESTAMPIndex = 6;
	private int replicates = 10;
	private static GraphDatabaseService dataGraph;

	private ArrayList<TransportationObject> reader() throws Exception {
		FileInputStream fis = new FileInputStream(transportationPath);

		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		// 4943;0.0;16-4;10063;-487.630 155.929 0.000;1874
		ArrayList<TransportationObject> objects = new ArrayList<TransportationObject>();
		String line = br.readLine();
		int cnt = 1;
		while ((line = br.readLine()) != null) {
			cnt++;
			if (cnt % 1000000 == 0)
				System.out.println(cnt);

			String[] infos = line.split(";");
			String[] linkAndLane = infos[LINKLANESIndex].split("-");
			String[] coord = infos[LECOORDIndex].split(" ");

			objects.add(new TransportationObject(Integer.parseInt(infos[AUTOIDIndex]), infos[COLORIndex],
					Double.parseDouble(infos[SPEEDIndex]), Integer.parseInt(linkAndLane[0]),
					Integer.parseInt(linkAndLane[1]),
					!infos[NEXTLINKIndex].equals("") ? Integer.parseInt(infos[NEXTLINKIndex]) : 0,
					Double.parseDouble(coord[0]), Double.parseDouble(coord[1]), Double.parseDouble(coord[2]),
					Integer.parseInt(infos[TIMESTAMPIndex])));
		}

		br.close();

		return objects;

	}

	public static void main(String[] args) throws Exception {

		TransportationRead tr = new TransportationRead();
		tr.initialize(args);
		ArrayList<TransportationObject> objects = tr.reader();

		File storeDir = new File(newDataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		tr.createDataGraph(objects);
		tr.replicateObjects(objects);

		dataGraph.shutdown();
		System.out.println("shutdown");
		dataGraph = null;
		
		tr.getStatistics();

	}

	private void getStatistics() {

		File storeDir = new File(newDataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int allNodesCnt = 0;
		for (Node node : dataGraph.getAllNodes()) {
			allNodesCnt++;
		}
		System.out.println("number of nodes: " + allNodesCnt);

		int allEdgesCnt = 0;
		for (Relationship edge : dataGraph.getAllRelationships()) {
			allEdgesCnt++;
		}
		System.out.println("number of edges: " + allEdgesCnt);

		int maxDegree = 0;
		for (Node node : dataGraph.getAllNodes()) {
			if (node.getDegree() > maxDegree) {
				maxDegree = node.getDegree();
			}
		}
		System.out.println("max degree: " + maxDegree);

		tx1.success();
		tx1.close();

	}

	private void initialize(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-newDataGraphPath")) {
				newDataGraphPath = args[++i];
			} else if (args[i].equals("-transportationPath")) {
				transportationPath = args[++i];
			} else if (args[i].equals("-replicates")) {
				replicates = Integer.parseInt(args[++i]);
			}
		}

	}

	private void replicateObjects(ArrayList<TransportationObject> objects) {

		int maxAutoId = 0;
		int maxlinkId = 0;
		int maxNextLinkId = 0;

		for (TransportationObject transObj : objects) {
			maxAutoId = Math.max(transObj.autoId, maxAutoId);
			maxlinkId = Math.max(transObj.link, maxlinkId);
			maxNextLinkId = Math.max(transObj.nextLink, maxNextLinkId);
		}
		System.out.println("maxAutoId: " + maxAutoId);
		System.out.println("maxlinkId: " + maxlinkId);
		System.out.println("maxNextLinkId: " + maxNextLinkId);

		for (int i = 0; i < replicates; i++) {
			System.out.println("replicates: " + i);
			ArrayList<TransportationObject> newObjects = new ArrayList<TransportationObject>();
			for (TransportationObject transObj : objects) {
				newObjects.add(new TransportationObject(transObj.autoId + maxAutoId, transObj.color, transObj.speed,
						transObj.link + maxlinkId, transObj.lane, transObj.nextLink + maxNextLinkId, transObj.lecoordx,
						transObj.lecoordy, transObj.lecoordz, transObj.timestamp));
			}

			objects.clear();
			objects.addAll(newObjects);

			for (TransportationObject transObj : objects) {
				maxAutoId = Math.max(transObj.autoId, maxAutoId);
				maxlinkId = Math.max(transObj.link, maxlinkId);
				maxNextLinkId = Math.max(transObj.nextLink, maxNextLinkId);
			}

			System.out.println("maxAutoId: " + maxAutoId);
			System.out.println("maxlinkId: " + maxlinkId);
			System.out.println("maxNextLinkId: " + maxNextLinkId);

			this.createDataGraph(objects);
		}

	}

	private void createDataGraph(ArrayList<TransportationObject> objects) {
		HashMap<Integer, HashSet<Integer>> nextLinkOfLinks = new HashMap<Integer, HashSet<Integer>>();
		for (TransportationObject to : objects) {
			nextLinkOfLinks.putIfAbsent(to.link, new HashSet<Integer>());
			nextLinkOfLinks.get(to.link).add(to.nextLink);
		}

		Collections.sort(objects, new Comparator<TransportationObject>() {
			@Override
			public int compare(TransportationObject o1, TransportationObject o2) {
				Integer compareInt = Integer.compare(o1.autoId, o2.autoId);
				if (compareInt != 0) {
					return compareInt;
				}
				return Integer.compare(o1.timestamp, o2.timestamp);
			}
		});

		HashMap<Integer, Node> autoNodeOfIndex = new HashMap<Integer, Node>();
		HashMap<Integer, Node> linkNodeOfIndex = new HashMap<Integer, Node>();
		HashMap<String, Relationship> relationshipOfPairOfNodes = new HashMap<String, Relationship>();

		Transaction tx1 = dataGraph.beginTx();

		int minTimestamp = Integer.MAX_VALUE;
		int maxTimestamp = Integer.MIN_VALUE;
		for (TransportationObject to : objects) {
			minTimestamp = Math.min(minTimestamp, to.timestamp);
			maxTimestamp = Math.max(maxTimestamp, to.timestamp);
		}

		int cnt2 = 0;
		int d = 0;
		for (TransportationObject to : objects) {
			if (!autoNodeOfIndex.containsKey(to.autoId)) {
				Node autoNode = dataGraph.createNode();
				autoNode.addLabel(Label.label("Auto"));
				autoNode.setProperty("autoId", to.autoId);
				autoNode.setProperty("group", d);

				if ((autoNodeOfIndex.size() / 500) > d) {
					d = (autoNodeOfIndex.size() / 500);
				}

				autoNodeOfIndex.put(to.autoId, autoNode);
				cnt2++;
				if (cnt2 % 100000 == 0) {
					if (cnt2 % 1000000 == 0) {
						System.out.println("cnt2: " + cnt2);
					}
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}
			}

			if (!linkNodeOfIndex.containsKey(to.link)) {
				Node linkNode = dataGraph.createNode();
				linkNode.addLabel(Label.label("Link" + to.link));
				linkNode.setProperty("linkId", to.link);

				linkNodeOfIndex.put(to.link, linkNode);

				cnt2++;
				if (cnt2 % 100000 == 0) {
					if (cnt2 % 1000000 == 0) {
						System.out.println("cnt2: " + cnt2);
					}
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}
			}

			if (!linkNodeOfIndex.containsKey(to.nextLink)) {
				Node linkNode = dataGraph.createNode();
				linkNode.addLabel(Label.label("Link" + to.nextLink));
				linkNode.setProperty("linkId", to.nextLink);

				linkNodeOfIndex.put(to.nextLink, linkNode);

				cnt2++;
				if (cnt2 % 100000 == 0) {
					if (cnt2 % 1000000 == 0) {
						System.out.println("cnt2: " + cnt2);
					}
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}
			}
		}

		for (TransportationObject to : objects) {
			if (!relationshipOfPairOfNodes.containsKey(to.autoId + "_" + to.link)) {
				Relationship rel = autoNodeOfIndex.get(to.autoId).createRelationshipTo(linkNodeOfIndex.get(to.link),
						RelationshipType.withName("at"));
				relationshipOfPairOfNodes.put(to.autoId + "_" + to.link, rel);

				cnt2++;
				if (cnt2 % 100000 == 0) {
					if (cnt2 % 1000000 == 0) {
						System.out.println("cnt2: " + cnt2);
					}
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}
			}

		}

		for (Integer linkIndex : nextLinkOfLinks.keySet()) {
			for (Integer nextLinkId : nextLinkOfLinks.get(linkIndex)) {
				Relationship rel = linkNodeOfIndex.get(linkIndex).createRelationshipTo(linkNodeOfIndex.get(nextLinkId),
						RelationshipType.withName("NeighborOf"));
				rel.setProperty("timepoints", new int[] { minTimestamp, maxTimestamp });

				cnt2++;
				if (cnt2 % 100000 == 0) {
					if (cnt2 % 1000000 == 0) {
						System.out.println("cnt2: " + cnt2);
					}
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}

			}
		}

		int currentAutoId = objects.get(0).autoId;
		int currentLinkId = objects.get(0).link;
		ArrayList<Integer> timepoints = new ArrayList<Integer>();
		for (TransportationObject to : objects) {
			if (to.autoId == currentAutoId && to.link == currentLinkId) {
				if (timepoints.isEmpty()) {
					timepoints.add(to.timestamp);
					timepoints.add(to.timestamp);
				} else {
					timepoints.set(timepoints.size() - 1, to.timestamp);
				}
			} else {
				int[] edgeTimepointsIndexArrInt = new int[timepoints.size()];
				for (int i = 0; i < timepoints.size(); i++) {
					edgeTimepointsIndexArrInt[i] = (int) timepoints.get(i);
				}
				dataGraph
						.getRelationshipById(relationshipOfPairOfNodes.get(currentAutoId + "_" + currentLinkId).getId())
						.setProperty("timepoints", edgeTimepointsIndexArrInt);
				currentAutoId = to.autoId;
				currentLinkId = to.link;
				timepoints = new ArrayList<Integer>();
				timepoints.add(to.timestamp);
				timepoints.add(to.timestamp);

				cnt2++;
				if (cnt2 % 100000 == 0) {
					if (cnt2 % 1000000 == 0) {
						System.out.println("cnt2: " + cnt2);
					}
					tx1.success();
					tx1.close();
					tx1 = dataGraph.beginTx();
				}
			}
		}

		int[] edgeTimepointsIndexArrInt = new int[timepoints.size()];
		for (int i = 0; i < timepoints.size(); i++) {
			edgeTimepointsIndexArrInt[i] = (int) timepoints.get(i);
		}
		dataGraph.getRelationshipById(relationshipOfPairOfNodes.get(currentAutoId + "_" + currentLinkId).getId())
				.setProperty("timepoints", edgeTimepointsIndexArrInt);

		tx1.success();
		tx1.close();
		tx1 = null;

	}
}
