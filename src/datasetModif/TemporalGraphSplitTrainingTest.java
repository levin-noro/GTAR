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

import utilities.Dummy.DummyFunctions;
import utilities.CopyDirVisitor;
import utilities.Dummy;
import utilities.TimeIntervalsOperation;

public class TemporalGraphSplitTrainingTest {

	private static GraphDatabaseService dataGraph;

	private static int maxTimestamp = 11963;

	public static void main(String[] args) throws Exception {

		// source dataset:
		String sourceDataset = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/panama_GTAR_V3.0.db";

		Path trainingPath = copyG0andGetItsNewPath(sourceDataset, "Train");
		Path testingPath = copyG0andGetItsNewPath(sourceDataset, "Test");

		ArrayList<Integer> desiredTimes = new ArrayList<Integer>();
		desiredTimes.add(0);
		desiredTimes.add((75 * maxTimestamp) / 100);
		refineDataSet(trainingPath, desiredTimes);

		desiredTimes = new ArrayList<Integer>();
		desiredTimes.add(((75 * maxTimestamp) / 100) + 1);
		desiredTimes.add(maxTimestamp);
		refineDataSet(testingPath, desiredTimes);

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

	private static void refineDataSet(Path dataGraphPath, ArrayList<Integer> desiredTimes) {

		// initialize data graph
		File storeDir = new File(dataGraphPath.toString());
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.setConfig(GraphDatabaseSettings.pagecache_memory, "2g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int relCnt = 1;
		// assign time points to each edge
		for (Relationship rel : dataGraph.getAllRelationships()) {
			if (rel.hasProperty("timepoints")) {
				ArrayList<Integer> timepointsOfEdge = new ArrayList<Integer>();
				int[] timepoints = (int[]) rel.getProperty("timepoints");
				for (int t = 0; t < timepoints.length; t++) {
					timepointsOfEdge.add(timepoints[t]);
				}

				ArrayList<Integer> refinedTimepoints = TimeIntervalsOperation.intersectionOfTimeIntervals(desiredTimes,
						timepointsOfEdge);

				if (refinedTimepoints == null || refinedTimepoints.isEmpty()) {
					rel.delete();
				} else {
					int[] edgeTimepointsIndexArrInt = new int[refinedTimepoints.size()];
					for (int i = 0; i < refinedTimepoints.size(); i++) {
						edgeTimepointsIndexArrInt[i] = (int) refinedTimepoints.get(i);
					}
					rel.setProperty("timepoints", edgeTimepointsIndexArrInt);
				}
			}

			if (relCnt % 10000 == 0) {
				// readmeWriter.flush();

				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
			}

			relCnt++;
		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private static void createInfiniteTimepoints(ArrayList<DateTimeAndType> timepoints, Date minDateInTheGraph,
			Date maxDateInTheGraph) {
		timepoints.add(new DateTimeAndType(minDateInTheGraph, true));
		timepoints.add(new DateTimeAndType(maxDateInTheGraph, true));
	}

}
