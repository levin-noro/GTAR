package utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import utilities.Dummy.DummyProperties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;

public class Dummy {
	public static class DummyProperties {
		public static boolean softwareMode = false;
		public static boolean visualize = false;
		public static boolean debugMode = false;
		public static boolean incMode = false;

		public static int NUMBER_OF_SNAPSHOTS = 1;
		public static int NUMBER_OF_ALL_FOCUS_NODES = 0;
		public static String SEPARATOR_LABEL_AND_RELTYPE = "#";
		public static boolean bigDataTestMode = false;

		public static double supportThreshold = 0.0d;
		public static double confidenceThreshold = 0.0d;
		public static boolean considerCoOcc = true;

		public static boolean hasOptimization = true;

		// WHEN THIS IS TRUE, other detailed time are not correct, only the
		// final time is correct
		// public static boolean qualityVsTime = false;
		public static int qualitySaveIntervalInMilliSeconds = 1000;

		public static enum Direction {
			INCOMING, OUTGOING
		}

		public static enum ExpansionSide {
			LHS, RHS
		}

		public static enum RuleSide {
			LEFT, RIGHT
		};
	}

	public static class DummyFunctions {

		public static Path copyG0andGetItsNewPath(String dataGraphPath) throws Exception {

			System.out.println("copying is started..." + new Date());
			String parentDir = dataGraphPath.substring(0, dataGraphPath.lastIndexOf("/"));

			Path sourcePath = Paths.get(dataGraphPath);
			Path destinationPath = Paths.get(parentDir + "/target.db");

			if (Files.exists(destinationPath)) {
				Dummy.DummyFunctions.deleteCompletely(destinationPath);
			}

			Files.walkFileTree(sourcePath,
					new CopyDirVisitor(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING));
			System.out.println("copying is finished..." + new Date());
			return destinationPath;

		}

		public static ArrayList<Integer> getSortedUniqueRandomNumbers(int maximumNumber, int size) {
			ArrayList<Integer> wholeList = new ArrayList<Integer>();
			ArrayList<Integer> output = new ArrayList<Integer>();
			for (int i = 0; i < maximumNumber; i++) {
				wholeList.add(new Integer(i));
			}

			Collections.shuffle(wholeList);
			for (int i = 0; i < size; i++) {
				output.add(wholeList.get(i));
			}

			Collections.sort(output);
			return output;
		}

		public static void sleepAndWakeUp(int milisecondsOfSleep) throws Exception {
			System.out.println("sleeping..." + new Date());
			System.gc();
			System.runFinalization();
			Thread.sleep(milisecondsOfSleep);
			System.gc();
			System.runFinalization();
			Thread.sleep(milisecondsOfSleep);
			System.out.println("waking up..." + new Date());
		}

		public static boolean deleteCompletely(Path rootPath) {
			try {
				Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						// System.out.println("delete file: " +
						// file.toString());
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						// System.out.println("delete dir: " + dir.toString());
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}

		public static int getNumberOfAllNodes(GraphDatabaseService dataGraph) {
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {
				numberOfAllNodes++;
			}
			return numberOfAllNodes;
		}

		public static int getNumberOfAllRels(GraphDatabaseService dataGraph) {
			int numberOfAllRelationships = 0;
			for (Relationship rel : dataGraph.getAllRelationships()) {
				numberOfAllRelationships++;
			}
			return numberOfAllRelationships;
		}

		public static HashSet<String> getDifferentLabels(GraphDatabaseService dataGraph) {
			HashSet<String> differentLabels = new HashSet<String>();
			for (Label label : dataGraph.getAllLabels()) {
				differentLabels.add(label.name());
			}
			return differentLabels;
		}

		public static double getAvgOutDegrees(GraphDatabaseService dataGraph) {
			double allDegrees = 0.0d;
			double avgDegrees = 0.0d;
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {

				// if(numberOfAllNodes==7){
				// System.out.println(numberOfAllNodes);
				allDegrees += node.getDegree(Direction.OUTGOING);
				numberOfAllNodes++;
			}
			avgDegrees = allDegrees / numberOfAllNodes;
			return avgDegrees;
		}

		public static double getAvgInDegrees(GraphDatabaseService dataGraph) {
			double allDegrees = 0.0d;
			double avgDegrees = 0.0d;
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {

				// if(numberOfAllNodes==7){
				// System.out.println(numberOfAllNodes);
				allDegrees += node.getDegree(Direction.INCOMING);
				numberOfAllNodes++;
			}
			avgDegrees = allDegrees / numberOfAllNodes;
			return avgDegrees;
		}

		public static double getAvgDegrees(GraphDatabaseService dataGraph) {
			double allDegrees = 0.0d;
			double avgDegrees = 0.0d;
			int numberOfAllNodes = 0;
			for (Node node : dataGraph.getAllNodes()) {

				// if(numberOfAllNodes==7){
				// System.out.println(numberOfAllNodes);
				allDegrees += node.getDegree(Direction.BOTH);
				numberOfAllNodes++;
			}
			avgDegrees = allDegrees / numberOfAllNodes;
			return avgDegrees;
		}

		public static double getAvgDegreeOfFocusNodes(GraphDatabaseService dataGraph, HashSet<Integer> allFocusNodes,
				int numberOfAllFocusNodes) throws Exception {
			double avgOutDegreeOfFocusNodes = 0;
			for (Integer nodeId : allFocusNodes) {
				avgOutDegreeOfFocusNodes += dataGraph.getNodeById(nodeId).getDegree();
			}

			if (avgOutDegreeOfFocusNodes == 0) {
				System.err.println("avgDegreeOfFocusNodes is zero!");
				return 0;
			}
			
			
			System.out.println(avgOutDegreeOfFocusNodes / numberOfAllFocusNodes);
			return avgOutDegreeOfFocusNodes / numberOfAllFocusNodes;
		}

		public static double getAverageOfDoubleArray(ArrayList<Double> arrayOfDoubles) {
			if (arrayOfDoubles.size() == 0) {
				return 0;
			}

			double sum = 0;
			for (double val : arrayOfDoubles) {
				sum += val;
			}
			return sum / (arrayOfDoubles.size());
		}

		public static double getTotalSumOfDoubleArray(ArrayList<Double> arrayOfDoubles) {
			double sum = 0;
			for (double val : arrayOfDoubles) {
				sum += val;
			}
			return sum;
		}

		public static void registerShutdownHook(final GraphDatabaseService dataGraph) {
			// Registers a shutdown hook for the Neo4j instance so that it
			// shuts down nicely when the VM exits (even if you "Ctrl-C" the
			// running application).
			// Runtime.getRuntime().addShutdownHook(new Thread() {
			// @Override
			// public void run() {
			// dataGraph.shutdown();
			// }
			// });

		}

		public static HashSet<String> getDifferentRelType(GraphDatabaseService dataGraph) {

			HashSet<String> relTypes = new HashSet<String>();
			for (RelationshipType relType : dataGraph.getAllRelationshipTypes()) {
				relTypes.add(relType.name());
			}
			return relTypes;
		}

		public static File[] getFilesInTheDirfinder(String dirName) {
			File dir = new File(dirName);

			File[] files = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return filename.endsWith(".txt") || filename.startsWith("Delta");
				}
			});

			if (files != null && files.length > 1)
				Arrays.sort(files);

			for (int i = 0; i < files.length; i++) {
				System.out.println("catched file " + i + "; " + files[i].getName());
			}
			return files;

		}

		public static boolean isContain(String source, String subItem) {
			// String pattern = "\\b" + subItem + "\\b";
			// Pattern p = Pattern.compile(pattern);
			// Matcher m = p.matcher(source);
			// return m.find();
			StringTokenizer st = new StringTokenizer(source);
			while (st.hasMoreTokens()) {
				if (st.nextToken().equals(subItem)) {
					return true;
				}
			}
			return false;
		}

		public static void printQualityVsTime(String algorithm, String whatIsFocus, int numberOfAllPatterns,
				int maxAllowedHops, int maxAllowedEdges, double supportThreshold, double confidenceThreshold,
				double latticeGenerationDuration, int qualitySaveIntervalInMilliSeconds,
				TreeMap<Integer, CumulativeRulesInfo> qualityOfTime) throws Exception {

			ArrayList<InfoHolder> logInfos = new ArrayList<InfoHolder>();
			logInfos.add(new InfoHolder(1, "Focus", whatIsFocus));
			logInfos.add(new InfoHolder(2, "numberOfAllPatterns", numberOfAllPatterns));
			logInfos.add(new InfoHolder(3, "maxAllowedHops", maxAllowedHops));
			logInfos.add(new InfoHolder(4, "maxAllowedEdges", maxAllowedEdges));
			logInfos.add(new InfoHolder(5, "supportThreshold", supportThreshold));
			logInfos.add(new InfoHolder(6, "confidenceThreshold", confidenceThreshold));
			logInfos.add(new InfoHolder(7, "totalTime", latticeGenerationDuration));
			logInfos.add(new InfoHolder(8, "qualitySaveIntervalInMilliSeconds", qualitySaveIntervalInMilliSeconds));

			int maxNumberOfQualityHops = 3000;
			double prevConfidence = 0;
			for (int i = 0; i < maxNumberOfQualityHops; i++) {

				if (qualityOfTime.containsKey(i)) {
					prevConfidence = qualityOfTime.get(i).confidence;

				}
				logInfos.add(new InfoHolder(i + 9, "c_" + String.valueOf(i), prevConfidence));
			}

			double prevSupport = 0;
			for (int i = maxNumberOfQualityHops; i < (2 * maxNumberOfQualityHops); i++) {
				if (qualityOfTime.containsKey(i - maxNumberOfQualityHops)) {
					prevSupport = qualityOfTime.get(i - maxNumberOfQualityHops).support;

				}
				logInfos.add(new InfoHolder(i + 9, "s_" + String.valueOf(i - maxNumberOfQualityHops), prevSupport));
			}

			double prevNumOfRules = 0;
			for (int i = (2 * maxNumberOfQualityHops); i < (3 * maxNumberOfQualityHops); i++) {
				if (qualityOfTime.containsKey(i - (2 * maxNumberOfQualityHops))) {
					prevNumOfRules = qualityOfTime.get(i - (2 * maxNumberOfQualityHops)).numberOfRules;

				}
				logInfos.add(
						new InfoHolder(i + 9, "r_" + String.valueOf(i - (2 * maxNumberOfQualityHops)), prevNumOfRules));
			}

			// NavigableSet<Integer> nav = qualityOfTime.navigableKeySet();
			// Iterator<Integer> navItrConfidence = nav.iterator();
			// while (navItrConfidence.hasNext()) {
			// Integer interval = navItrConfidence.next();
			//
			// logInfos.add(new InfoHolder(cnt++, interval.toString(),
			// qualityOfTime.get(interval).confidence));
			// }

			TimeLogger.LogTime("QualityVsTime_" + algorithm + ".csv", true, logInfos);

		}

	}

}
