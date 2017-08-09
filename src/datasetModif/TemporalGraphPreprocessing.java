package datasetModif;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import utilities.CopyDirVisitor;
import utilities.Dummy;
import utilities.Dummy.DummyFunctions;

public class TemporalGraphPreprocessing {

	private static GraphDatabaseService dataGraph;

	public static void main(String[] args) throws Exception {

		// source dataset:
		String sourceDataset = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/MovieLens/movielens.db";

		// TODO: print the active timestamps;

		// String[] addTimestampsPropertyKeys = new String[] {
		// "incorporation_date" };
		// String[] deleteTimestampsPropertyKeys = new String[] {
		// "inactivation_date", "dorm_date", "struck_off_date" };
		String[] addTimestampsPropertyKeys = new String[] { "time" };
		String[] deleteTimestampsPropertyKeys = new String[] {};
		// String userSpecifiedDateFormat = "dd-MMM-yyyy";
		// String userSpecifiedDateFormat = "yyyy";
		// String sourceDateFormat = "yyyy-MM-dd HH:mm:ss";
		// String desiredDateFormat = "yyyy-MM-dd HH:mm";
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
		for (Node node : dataGraph.getAllNodes()) {
			// if(node.getId()==0){
			// System.out.println();
			// }
			Map<String, Object> allPropertiesOfNode = node.getAllProperties();
			for (String addTimestampKey : addTimestampsPropertyKeys) {
				if (allPropertiesOfNode.containsKey(addTimestampKey)) {
					String addTimestampValue = allPropertiesOfNode.get(addTimestampKey).toString();
					allActiveDatesOfTheGraphSet.add(sourceFormat.parse(addTimestampValue));
				}
			}

			for (String deleteTimestampKey : deleteTimestampsPropertyKeys) {
				if (allPropertiesOfNode.containsKey(deleteTimestampKey)) {
					String deleteTimestampValue = allPropertiesOfNode.get(deleteTimestampKey).toString();
					allActiveDatesOfTheGraphSet.add(sourceFormat.parse(deleteTimestampValue));
				}
			}
		}

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

		// first find the active time of each node
		for (Node node : dataGraph.getAllNodes()) {
			Map<String, Object> allPropertiesOfNode = node.getAllProperties();
			ArrayList<DateTimeAndType> timepoints = new ArrayList<DateTimeAndType>();
			for (String addTimestampKey : addTimestampsPropertyKeys) {
				if (allPropertiesOfNode.containsKey(addTimestampKey)) {
					String addTimestampValue = allPropertiesOfNode.get(addTimestampKey).toString();
					timepoints.add(new DateTimeAndType(desireFormat.parse(addTimestampValue), true));
				}
			}
			for (String deleteTimestampKey : deleteTimestampsPropertyKeys) {
				if (allPropertiesOfNode.containsKey(deleteTimestampKey)) {
					String deleteTimestampValue = allPropertiesOfNode.get(deleteTimestampKey).toString();
					timepoints.add(new DateTimeAndType(desireFormat.parse(deleteTimestampValue), false));
				}
			}

			// sorting timepoints
			Collections.sort(timepoints, new Comparator<DateTimeAndType>() {
				@Override
				public int compare(DateTimeAndType o1, DateTimeAndType o2) {
					int compare = o1.dateTime.compareTo(o2.dateTime);
					if (compare != 0) {
						return compare;
					}
					return Boolean.compare(o2.isInsert, o1.isInsert);
				}
			});

			// prepare it as time intervals
			ArrayList<Date> finalTimepoints = new ArrayList<Date>();

			// if no timestamp
			if (timepoints.isEmpty()) {
				createInfiniteTimepoints(timepoints, minDateInTheGraph, maxDateInTheGraph);

			} else {
				for (DateTimeAndType dateTimeAndType : timepoints) {
					int index = finalTimepoints.size();

					if (index % 2 == 0) { // if even: we need a start time
											// point
						if (dateTimeAndType.isInsert) {
							finalTimepoints.add(dateTimeAndType.dateTime);
						}
					} else { // if odd: we need an end time point
						if (!dateTimeAndType.isInsert) {
							finalTimepoints.add(dateTimeAndType.dateTime);
						}
					}
				}

				if (finalTimepoints.size() % 2 != 0) {
					finalTimepoints.add(maxDateInTheGraph);
				}
			}

			timeInfosOfNode.put((int) node.getId(), new NodeTimeInfos(finalTimepoints));

			// get specific props:
			// String specPropValue = "";
			// for (String key : addTimestampsPropertyKeys) {
			// if (node.getAllProperties().containsKey(key)) {
			// specPropValue += key + ":" + node.getAllProperties().get(key) +
			// ", ";
			// }
			// }
			// for (String key : deleteTimestampsPropertyKeys) {
			// if (node.getAllProperties().containsKey(key)) {
			// specPropValue += key + ":" + node.getAllProperties().get(key) +
			// ", ";
			// }
			// }

			// readmeWriter.write(node.getId() + " -> date props:" + " -> " +
			// specPropValue);
			// readmeWriter.write("\n");
		}

		readmeWriter.write("end of node traversing");
		readmeWriter.write("\n");

		int relCnt = 1;
		// assign time points to each edge
		for (Relationship rel : dataGraph.getAllRelationships()) {
			Node startNode = rel.getStartNode();
			Node endNode = rel.getEndNode();

			ArrayList<Date> edgeTimepoints = intersectionOfTimeIntervals(
					timeInfosOfNode.get((int) startNode.getId()).timepoints,
					timeInfosOfNode.get((int) endNode.getId()).timepoints);

			if (edgeTimepoints.isEmpty()) {
				edgeTimepoints.add(minDateInTheGraph);
				edgeTimepoints.add(maxDateInTheGraph);
			}

			ArrayList<Integer> edgeTimepointsIndex = new ArrayList<Integer>();
			for (Date dateTimepoint : edgeTimepoints) {
				edgeTimepointsIndex.add(indexOfDate.get(dateTimepoint));
			}
			int[] edgeTimepointsIndexArrInt = new int[edgeTimepointsIndex.size()];
			for (int i = 0; i < edgeTimepointsIndex.size(); i++) {
				edgeTimepointsIndexArrInt[i] = (int) edgeTimepointsIndex.get(i);
			}
			

			rel.setProperty("timepoints", edgeTimepointsIndexArrInt);
			rel.setProperty("realTimepoints", Arrays.toString(edgeTimepoints.toArray()));

			// readmeWriter.write(
			// startNode.getId() + "->" + endNode.getId() + " => " +
			// Arrays.toString(edgeTimepointsIndex.toArray())
			// + " => " + Arrays.toString(edgeTimepoints.toArray()));
			// readmeWriter.write("\n");

			if (relCnt % 10000 == 0) {
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

	public static Path copyG0andGetItsNewPath(String dataGraphPath, String title) throws Exception {

		System.out.println("copying is started..." + new Date());
		String parentDir = dataGraphPath.substring(0, dataGraphPath.lastIndexOf("/"));

		Path sourcePath = Paths.get(dataGraphPath);
		Path destinationPath = Paths.get(parentDir + "/" + title + ".db");

		if (Files.exists(destinationPath)) {
			Dummy.DummyFunctions.deleteCompletely(destinationPath);
		}

		Files.walkFileTree(sourcePath,
				new CopyDirVisitor(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING));
		System.out.println("copying is finished..." + new Date());
		return destinationPath;

	}
	private static void createInfiniteTimepoints(ArrayList<DateTimeAndType> timepoints, Date minDateInTheGraph,
			Date maxDateInTheGraph) {
		timepoints.add(new DateTimeAndType(minDateInTheGraph, true));
		timepoints.add(new DateTimeAndType(maxDateInTheGraph, true));
	}

	private static ArrayList<Date> intersectionOfTimeIntervals(ArrayList<Date> set1, ArrayList<Date> set2) {
		// an empty set means -inf, +inf but a null result means that no
		// intersection.

		if (set1 == null || set2 == null) {
			System.err.println();
		}

		ArrayList<Date> result = new ArrayList<Date>();

		if (set1.size() == 0 && set2.size() == 0) {
			return result;
		}
		if (set1.size() == 0 && set2.size() > 0) {
			result.addAll(set2);
			return result;
		}
		if (set2.size() == 0 && set1.size() > 0) {
			result.addAll(set1);
			return result;
		}

		int s1 = 0, e1 = 1, s2 = 0, e2 = 1;

		while (e1 <= set1.size() && e2 <= set2.size()) {
			if (set1.get(s1).compareTo(set2.get(e2)) <= 0 && set2.get(s2).compareTo(set1.get(e1)) <= 0) {
				Date newStartTime = max(set1.get(s1), set2.get(s2));
				Date newEndTime = min(set1.get(e1), set2.get(e2));
				result.add(newStartTime);
				result.add(newEndTime);
				if (newEndTime.compareTo(set1.get(e1)) >= 0) {
					s1 += 2;
					e1 += 2;
				}
				if (newEndTime.compareTo(set2.get(e2)) >= 0) {
					s2 += 2;
					e2 += 2;
				}

			} else if (set1.get(s1).compareTo(set2.get(s2)) < 0) {
				s1 += 2;
				e1 += 2;
			} else if (set2.get(s2).compareTo(set1.get(s1)) < 0) {
				s2 += 2;
				e2 += 2;
			}
		}
		if (result.size() == 0)
			return null;

		return result;
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
