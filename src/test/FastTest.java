package test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class FastTest {

	private static String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2016/FreqPatternResearch/Data/panama_v4.0_continent.graphdb";

	public static void main(String[] args) throws Exception {
		File storeDir = new File(dataGraphPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

		System.out.println("dataGraphPath: " + dataGraphPath);
		// SimpleDateFormat dateFormatComplex = new SimpleDateFormat("E MMM dd
		// HH:mm:ss Z yyyy");
		// SimpleDateFormat dateFormatYear = new SimpleDateFormat("yyyy");
		// SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");

		Transaction tx1 = dataGraph.beginTx();
		int cnt = 0;
		// HashSet<Date> datesSet = new HashSet<Date>();
		// for(Node node : dataGraph.getAllNodes()){
		// if(node.hasProperty("Year")){
		// String dateStr = node.getProperty("Year").toString();
		// datesSet.add(dateFormatYear.parse(dateStr));
		// }
		// }
		// HashSet<String> datesSet = new HashSet<String>();
		// for (Node node : dataGraph.getAllNodes()) {
		// if (node.hasProperty("jurisdiction")) {
		// String jur = node.getProperty("jurisdiction").toString();
		// datesSet.add(jur);
		// }
		// }

		HashMap<Integer, Integer> freq = new HashMap<Integer, Integer>();
		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (rel.hasProperty("timepoints")) {
				int[] timepoints = (int[]) rel.getProperty("timepoints");
				for (int t = 0; t < timepoints.length; t++) {
					freq.putIfAbsent(timepoints[t], 0);
					freq.put(timepoints[t], freq.get(timepoints[t]) + 1);
				}
			}
		}

		// for (Relationship rel : dataGraph.getAllRelationships()) {
		// if (rel.hasProperty("realTimepoints")) {
		// String[] timepoints =
		// rel.getProperty("realTimepoints").toString().split(",");
		// for (String t : timepoints) {
		// datesSet.add(dateFormat.parse(t.replace("[", "").replace("]",
		// "").trim()));
		// }
		// }
		// }
		// ArrayList<Date> datesArr = new ArrayList<Date>();
		// datesArr.addAll(datesSet);
		//
		// Collections.sort(datesArr, new Comparator<Date>() {
		// @Override
		// public int compare(Date o1, Date o2) {
		// return o1.compareTo(o2);
		// }
		// });

//		for (String d : datesSet) {
//			System.out.println(d);
//		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

}
