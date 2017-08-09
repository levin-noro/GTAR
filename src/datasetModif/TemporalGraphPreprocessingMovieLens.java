package datasetModif;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.cypher.internal.compiler.v3_0.helpers.RuntimeJavaValueConverter.feedIteratorToVisitable;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import utilities.Dummy.DummyFunctions;

public class TemporalGraphPreprocessingMovieLens {

	private static GraphDatabaseService dataGraph;

	public static void main(String[] args) throws Exception {

		// source dataset:
		String sourceDataset = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/MovieLens/movielens.db";

		// TODO: print the active timestamps;
		String sourceDateFormat = "HH:mm";
		String desiredDateFormat = "HH:mm";

		SimpleDateFormat sourceFormat = new SimpleDateFormat(sourceDateFormat);
		SimpleDateFormat desireFormat = new SimpleDateFormat(desiredDateFormat);

		HashMap<Integer, NodeTimeInfos> timeInfosOfNode = new HashMap<Integer, NodeTimeInfos>();

		Path newDGPath = DummyFunctions.copyG0andGetItsNewPath(sourceDataset);

		// initialize data graph
		File storeDir = new File(newDGPath.toString());
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "2g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		File readme = new File(newDGPath.toString() + "/Timepoints.txt");
		FileWriter readmeWriter = new FileWriter(readme);

		Transaction tx1 = dataGraph.beginTx();

		HashSet<Date> allActiveDatesOfTheGraphSet = new HashSet<Date>();

		// finding maximum, minimum, and active date of the graph.

		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (rel.hasProperty("time")) {
				allActiveDatesOfTheGraphSet.add(sourceFormat.parse(rel.getProperty("time").toString()));
			}
		}
		System.out.println("after reading all active dates");

		// desired format:
		HashSet<Date> desiredActiveDatesOfTheGraphSet = new HashSet<Date>();
		for (Date date : allActiveDatesOfTheGraphSet) {
			// if(date.toString().contains("16 23:58")){
			// System.out.println();
			// }
			desiredActiveDatesOfTheGraphSet.add(desireFormat.parse(desireFormat.format(date)));
		}

		System.out.println("after updating dates to desired format");

		ArrayList<Date> allActiveDatesOfTheGraphArr = new ArrayList<Date>();
		allActiveDatesOfTheGraphArr.addAll(desiredActiveDatesOfTheGraphSet);

		Collections.sort(allActiveDatesOfTheGraphArr, new Comparator<Date>() {
			@Override
			public int compare(Date o1, Date o2) {
				return o1.compareTo(o2);
			}
		});

		System.out.println("after sorting dates");

		Date minDateInTheGraph = allActiveDatesOfTheGraphArr.get(0);
		readmeWriter.write("minDateInTheGraph: " + minDateInTheGraph);
		readmeWriter.write("\n");
		Date maxDateInTheGraph = allActiveDatesOfTheGraphArr.get(allActiveDatesOfTheGraphArr.size() - 1);
		readmeWriter.write("maxDateInTheGraph: " + maxDateInTheGraph);
		readmeWriter.write("\n");

		HashMap<Integer, Date> dateOfIndex = new HashMap<Integer, Date>();
		HashMap<Date, Integer> indexOfDate = new HashMap<Date, Integer>();
		for (int i = 0; i < allActiveDatesOfTheGraphArr.size(); i++) {
			dateOfIndex.put(i, allActiveDatesOfTheGraphArr.get(i));
			indexOfDate.put(allActiveDatesOfTheGraphArr.get(i), i);
			readmeWriter.write(i + " -> " + allActiveDatesOfTheGraphArr.get(i) + "\n");
		}
		readmeWriter.write("\n");
		readmeWriter.write("date indices: " + dateOfIndex.size());
		readmeWriter.write("\n");
		readmeWriter.write("\n");

		System.out.println("date indices: " + dateOfIndex.size());

		int relCnt = 1;
		// assign time points to each edge
		for (Relationship rel : dataGraph.getAllRelationships()) {

			ArrayList<Integer> edgeTimepoints = new ArrayList<>();
			int[] edgeTimepointsIndexArrInt = new int[2];
			if (!rel.hasProperty("time")) {
				edgeTimepoints.add(indexOfDate.get(minDateInTheGraph));
				edgeTimepoints.add(indexOfDate.get(maxDateInTheGraph));
			} else {
				String val = rel.getProperty("time").toString();
				edgeTimepoints.add(indexOfDate.get(desireFormat.parse(val)));
				edgeTimepoints.add(indexOfDate.get(desireFormat.parse(val)));
			}
			for (int i = 0; i < edgeTimepoints.size(); i++) {
				edgeTimepointsIndexArrInt[i] = edgeTimepoints.get(i);
			}
			rel.setProperty("timepoints", edgeTimepointsIndexArrInt);

			if (relCnt % 500000 == 0) {
				// readmeWriter.flush();

				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
			}

			relCnt++;
		}

		readmeWriter.close();
		tx1.success();
		tx1.close();
		dataGraph.shutdown();
	}

	public static Date max(Date c1, Date c2) {
		if (c1.compareTo(c2) > 0)
			return c1;
		else
			return c2;
	}

	public static Date min(Date c1, Date c2) {
		if (c1.compareTo(c2) < 0)
			return c1;
		else
			return c2;
	}

	class DateTimeAndType {
		public Date dateTime;
		public boolean isInsert;

		public DateTimeAndType(Date dateTime, boolean isInsert) {
			this.dateTime = dateTime;
			this.isInsert = isInsert;
		}
	}

	class NodeTimeInfos {
		public ArrayList<Date> timepoints;

		public NodeTimeInfos(ArrayList<Date> timepoints) {
			this.timepoints = timepoints;
		}

		@Override
		public String toString() {
			return Arrays.toString(this.timepoints.toArray());
		}
	}
}
