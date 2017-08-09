package reducedLattice.twoSteps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import base.ILatticeNodeData;
import utilities.CumulativeRulesInfo;
import utilities.DebugHelper;
import utilities.Dummy;
import utilities.Dummy.DummyFunctions;
import utilities.Dummy.DummyProperties;
import utilities.InfoHolder;
import utilities.LatticeNode;
import utilities.Rule;
import utilities.TimeIntervalsOperation;
import utilities.TimeLogger;
import utilities.Visualizer;

public class GTARFinderTwoSteps {

	private String focusSetPath;
	private int maxAllowedHops;
	private int maxAllowedEdges;
	private String dataGraphPath;
	private boolean debugMode;
	private GraphDatabaseService dataGraph;
	private double supportThreshold = 0.0d;
	private double confidenceThreshold = 0.0d;

	private int numberOfAllFocusNodes = 0;

	private int deltaT = -1;

	public LatticeReducedTwoStepsOpt lattice;

	int numberOfAllNodes;
	int numberOfAllRelationships;
	HashSet<String> differentLabels;
	HashSet<String> differentRelTypes;

	double avgDegrees;

	int minTimestamp = Integer.MAX_VALUE;
	int maxTimestamp = 0;
	double avgDegreeOfFocusNodes;
	private double timebound;
	public boolean timeout = false;

	public GTARFinderTwoSteps(String[] args) throws Exception {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-focusSetPath")) {
				focusSetPath = args[++i];
			} else if (args[i].equals("-maxAllowedHops")) {
				maxAllowedHops = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-maxAllowedEdges")) {
				maxAllowedEdges = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-debugMode")) {
				debugMode = Boolean.parseBoolean(args[++i]);
			} else if (args[i].equals("-visualize")) {
				DummyProperties.visualize = Boolean.parseBoolean(args[++i]);
			} else if (args[i].equals("-supportThreshold")) {
				DummyProperties.supportThreshold = Double.parseDouble(args[++i]);
				supportThreshold = DummyProperties.supportThreshold;
			} else if (args[i].equals("-confidenceThreshold")) {
				DummyProperties.confidenceThreshold = Double.parseDouble(args[++i]);
				confidenceThreshold = DummyProperties.confidenceThreshold;
			} else if (args[i].equals("-deltaT")) {
				deltaT = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-considerCoOcc")) {
				DummyProperties.considerCoOcc = Boolean.parseBoolean(args[++i]);
			}
			// else if (args[i].equals("-qualityVsTime")) {
			// DummyProperties.qualityVsTime = Boolean.parseBoolean(args[++i]);
			// }
			else if (args[i].equals("-qualitySaveIntervals")) {
				DummyProperties.qualitySaveIntervalInMilliSeconds = Integer.parseInt(args[++i]);
			}
		}

		if (focusSetPath == null || dataGraphPath == null || maxAllowedHops == 0 || maxAllowedEdges == 0
				|| deltaT == -1) {
			throw new Exception(
					"input parameters: focusSetPath, maxAllowedHops, dataGraphPath, maxAllowedEdges, deltaT");
		} else {
			System.out.println("GTAR Finder: -focusSetPath  " + focusSetPath + ", -maxAllowedHops:" + maxAllowedHops
					+ ", -maxAllowedEdges  " + maxAllowedEdges + ", -dataGraphPath:" + dataGraphPath + ", -deltaT: "
					+ deltaT + ", -supportThreshold " + supportThreshold + ", -confidenceThreshold "
					+ confidenceThreshold);
		}

		if (maxAllowedEdges < maxAllowedHops) {
			throw new Exception(" maxAllowedEdges < maxAllowedHops : " + maxAllowedEdges + " < " + maxAllowedHops);
		}

		Dummy.DummyProperties.debugMode = debugMode;

	}

	public GTARFinderTwoSteps(String focusSetPath, int maxHops, int maxEdges, String dataGraphPath, boolean debugMode,
			double supportThreshold, double confidenceThreshold, int deltaT, double timebound) {

		this.focusSetPath = focusSetPath;
		this.maxAllowedHops = maxHops;
		this.maxAllowedEdges = maxEdges;
		this.dataGraphPath = dataGraphPath;
		this.debugMode = debugMode;
		this.supportThreshold = supportThreshold;
		this.confidenceThreshold = confidenceThreshold;
		this.deltaT = deltaT;
		this.timebound = timebound;
	}

	public void findGTARs() throws Exception {

		loadDataGraph();

		System.out.println("after indexing");

		double startTime = System.nanoTime();

		// generating the prefix tree for G0
		lattice.timebound = timebound;
		LatticeNode<ILatticeNodeData> latticeRootNode = lattice.generateLatticeForTemporalGraph(startTime);

		if (lattice.timeout) {
			timeout = true;
			return;
		}

		// CorrectnessChecking.checkingDownwardProperty(latticeRootNode);
		double latticeGenerationDuration = (System.nanoTime() - startTime) / 1e6;

		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		// generate rules

		double ruleGeneratorStartTime = System.nanoTime();
		RuleGenerator rg = new RuleGenerator(lattice, maxAllowedEdges, deltaT, supportThreshold, confidenceThreshold,
				minTimestamp, maxTimestamp);
		rg.timebound = this.timebound;
		rg.generateRules(startTime, ruleGeneratorStartTime);
		double ruleGeneratorDuration = (System.nanoTime() - rg.ruleGeneratorStartTime) / 1e6;

		if (rg.timeout) {
			timeout = true;
			return;
		}

		double totalTime = ruleGeneratorDuration + latticeGenerationDuration;

		System.out.println("latticeGenerationTime: " + latticeGenerationDuration + " miliseconds.");
		System.out.println("ruleGeneratorDuration: " + ruleGeneratorDuration + " miliseconds.");
		System.out.println("total time: " + totalTime + " miliseconds.");

		int numberOfAllPatterns = lattice.bfsTraverse(lattice.emptyPTRootNode);

		if (DummyProperties.visualize) {
			for (int i = 1; i < maxAllowedEdges + 2; i++) {
				Visualizer.visualizeALevel(lattice, i, i);
			}
		}

		// Extracting saved rules
		double cumulativeSupport = 0d;
		double cumulativeConfidence = 0d;

		// Extracting saved rules
		ArrayList<Rule> rules = new ArrayList<>();
		for (int index : lattice.latticeNodeIndex.keySet()) {
			if (lattice.latticeNodeIndex.get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : lattice.latticeNodeIndex.get(index).getData().getRulesOfThis()) {
					// if (rule.lhs.getData().getPatternLatticeNodeIndex() == 84
					// && rule.rhs.getData().getPatternLatticeNodeIndex() ==
					// 142) {
					// System.out.println("where?");
					// }
					rules.add(rule);
					cumulativeSupport += rule.support;
					cumulativeConfidence += rule.confidence;
				}
			}
		}

		Collections.sort(rules, new Comparator<Rule>() {
			@Override
			public int compare(Rule o1, Rule o2) {
				int compare = Double.compare(o2.confidence, o1.confidence);
				if (compare != 0) {
					return compare;
				}
				compare = Double.compare(o2.support, o1.support);
				if (compare != 0) {
					return compare;
				}
				compare = Double.compare(o2.minOccurrencesSize, o1.minOccurrencesSize);
				if (compare != 0) {
					return compare;
				}
				compare = Integer.compare(o2.lhs.getLevel(), o1.lhs.getLevel());
				if (compare != 0) {
					return compare;
				}
				compare = Integer.compare(o2.rhs.getLevel(), o1.rhs.getLevel());
				if (compare != 0) {
					return compare;
				}
				compare = Integer.compare(o2.lhs.getData().getPatternGraph().vertexSet().size(),
						o1.lhs.getData().getPatternGraph().vertexSet().size());
				if (compare != 0) {
					return compare;
				}

				return Integer.compare(o2.rhs.getData().getPatternGraph().vertexSet().size(),
						o1.rhs.getData().getPatternGraph().vertexSet().size());
			}
		});

		int maxChangedTime = 0;
		for (Rule rule : rules) {
			int d = (int) ((rule.discoveredTime / 1e6) / DummyProperties.qualitySaveIntervalInMilliSeconds);
			maxChangedTime = Math.max(maxChangedTime, d);
			rg.qualityOfTime.putIfAbsent(d, new CumulativeRulesInfo(0, 0, 0));
			rg.qualityOfTime.get(d).support += rule.support;
			rg.qualityOfTime.get(d).confidence += rule.confidence;
			rg.qualityOfTime.get(d).numberOfRules++;
		}

		ArrayList<Integer> times = new ArrayList<Integer>(rg.qualityOfTime.size());
		times.addAll(rg.qualityOfTime.keySet());
		Collections.sort(times);

		for (int i = 0; i < times.size(); i++) {
			for (int j = 0; j < times.size(); j++) {
				if (times.get(i) > times.get(j)) {
					rg.qualityOfTime.get(times.get(i)).support += rg.qualityOfTime.get(times.get(j)).support;
					rg.qualityOfTime.get(times.get(i)).confidence += rg.qualityOfTime.get(times.get(j)).confidence;
					rg.qualityOfTime.get(times.get(i)).numberOfRules += rg.qualityOfTime
							.get(times.get(j)).numberOfRules;
				}
			}
		}

		lattice.numberOfMaximalRulesGenerated = rules.size();

		// Visualize Rules
		if (DummyProperties.visualize)

		{
			Visualizer.visualizeRules(rules);
		}

		// Visualize the lattice
		if (DummyProperties.visualize) {
			Visualizer.visualizeTheLattice(lattice);
		}

		// if (DummyProperties.visualize)
		// Visualizer.visualizeTopK(topKFrequentPatterns);
		//
		// if (!DummyProperties.bigDataTestMode)
		// TopKHandler.printTopK(topKFrequentPatterns);

		if (DummyProperties.debugMode)
			DebugHelper.printBiSimulatedPatterns(lattice);

		if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
			DebugHelper.printIsomorphicPatterns(lattice);

		if (DummyProperties.debugMode)
			DebugHelper.printGlobalCandidateSet(lattice);

		if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
			DebugHelper.printIfLevelIsNotConsistentWithNumberOfEdges(lattice);

		if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
			DebugHelper.printPatternWithDuplicateMatches(lattice);

		if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
			Visualizer.visualizePatternWithDuplicateMatches(lattice);

		if (DummyProperties.debugMode)
			DebugHelper.checkIfThereAreNotMaximalRules(lattice);

		String settingStr = " focus was " + lattice.whatIsFocus + ", h:" + maxAllowedHops + " , e:" + maxAllowedEdges
				+ ", s:" + supportThreshold + ", c:" + confidenceThreshold + ", deltaT:" + deltaT;
		System.out.println("GTARFinderTwoSteps Finder: " + settingStr);

		File foutTwoSteps = new File("twoStepsRules.txt");
		FileOutputStream fosTwoSteps = new FileOutputStream(foutTwoSteps);
		BufferedWriter bwTwoSteps = new BufferedWriter(new OutputStreamWriter(fosTwoSteps));
		DebugHelper.printingRules(bwTwoSteps, lattice, settingStr);

		ArrayList<InfoHolder> timeInfos = new ArrayList<InfoHolder>();
		timeInfos.add(new InfoHolder(0, "Focus", lattice.whatIsFocus));
		timeInfos.add(new InfoHolder(1, "Nodes", numberOfAllNodes));
		timeInfos.add(new InfoHolder(2, "Relationship", numberOfAllRelationships));
		timeInfos.add(new InfoHolder(3, "Distinct Labels", differentLabels.size()));
		timeInfos.add(new InfoHolder(4, "Distinct RelTypes", differentRelTypes.size()));
		timeInfos.add(new InfoHolder(5, "Average of Total Degrees", avgDegrees));
		timeInfos.add(new InfoHolder(6, "Average of Focus Degrees", avgDegreeOfFocusNodes));
		timeInfos.add(new InfoHolder(7, "Patterns", numberOfAllPatterns));
		timeInfos.add(new InfoHolder(8, "Total All Matches", lattice.numberOfTotalAllMatches));
		timeInfos.add(new InfoHolder(9, "Focus Nodes", numberOfAllFocusNodes));
		timeInfos.add(new InfoHolder(10, "Max Hops", maxAllowedHops));
		timeInfos.add(new InfoHolder(11, "Max Edges", maxAllowedEdges));
		timeInfos.add(new InfoHolder(12, "Total Time", totalTime));
		timeInfos.add(new InfoHolder(13, "sup threshold", supportThreshold));
		timeInfos.add(new InfoHolder(15, "conf threshold", confidenceThreshold));
		timeInfos.add(new InfoHolder(16, "BiSim Checking Time", lattice.getDurationOfBiSimChecking()));
		timeInfos
				.add(new InfoHolder(17, "Number Of BiSim Checking Request", lattice.getNumberOfBiSimCheckingRequest()));
		timeInfos.add(new InfoHolder(18, "Number Of Real BiSim Checking", lattice.getNumberOfRealBiSimChecking()));
		timeInfos.add(new InfoHolder(19, "Creation Of New Lattice Node", lattice.getDurationOfNewLatticeGeneration()));
		timeInfos.add(new InfoHolder(20, "Creation/Checking Of Dangling Nodes", lattice.danglingCreationDuration));
		timeInfos.add(new InfoHolder(21, "Number of Dangling Nodes", lattice.numberOfDangling));
		timeInfos.add(new InfoHolder(22, "deltaT", deltaT));
		timeInfos.add(new InfoHolder(24, "Num. of support computations",
				lattice.getNumberOfComputeTemporalMatchSetDuration()));
		timeInfos.add(new InfoHolder(25, "TemporalMatchSet computational time",
				lattice.getDurationOfComputeTemporalMatchSet()));

		// new
		timeInfos.add(new InfoHolder(27, "fixed LHS", numberOfAllPatterns));
		timeInfos.add(new InfoHolder(28, "RHS trials", lattice.rhsTrials));
		timeInfos.add(new InfoHolder(29, "totalStopDueToSupportOrConfidenceThreshold",
				lattice.totalStopDueToSupportOrConfidenceThreshold));

		timeInfos.add(new InfoHolder(32, "num Of TotalRules Generated", lattice.numberOfTotalRulesGenerated));
		timeInfos.add(new InfoHolder(33, "num Of Maximal Rules Generated", lattice.numberOfMaximalRulesGenerated));
		timeInfos.add(new InfoHolder(34, "num of gtarVerification", lattice.numberOfGtarVerification));
		timeInfos.add(new InfoHolder(35, "gtarVerificationDuration", lattice.gtarVerificationDuration));
		timeInfos.add(new InfoHolder(36, "checkIfSubPatternDuration", lattice.checkIfSubPatternDuration));
		timeInfos.add(new InfoHolder(37, "minTimestamp", lattice.minTimestamp));
		timeInfos.add(new InfoHolder(38, "maxTimestamp", lattice.maxTimestamp));

		if (!rules.isEmpty()) {
			TimeLogger.LogTime("GTAR_Reduced_TwoSteps_" + DummyProperties.hasOptimization + ".csv", true, timeInfos);
		}

		System.out.println("maxChangedTime: " + maxChangedTime);
		System.out.println("Final Rules: cumSup: " + cumulativeSupport + ", cumConf: " + cumulativeConfidence
				+ ", numRules:" + rules.size());
		// print quality vs time:

		if (!rules.isEmpty()) {
			DummyFunctions.printQualityVsTime("TwoSteps", lattice.whatIsFocus, numberOfAllPatterns, maxAllowedHops,
					maxAllowedEdges, supportThreshold, confidenceThreshold, latticeGenerationDuration,
					DummyProperties.qualitySaveIntervalInMilliSeconds, rg.qualityOfTime);
		}

	}

	private void loadDataGraph() throws Exception {

		// initialize data graph
		File storeDir = new File(dataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)//.setConfig("cache_type", "none")
				.setConfig(GraphDatabaseSettings.pagecache_memory, "8g").newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		// STAT Of the DB START
		numberOfAllNodes = Dummy.DummyFunctions.getNumberOfAllNodes(dataGraph);
		numberOfAllRelationships = Dummy.DummyFunctions.getNumberOfAllRels(dataGraph);
		differentLabels = Dummy.DummyFunctions.getDifferentLabels(dataGraph);
		differentRelTypes = Dummy.DummyFunctions.getDifferentRelType(dataGraph);

		avgDegrees = Dummy.DummyFunctions.getAvgOutDegrees(dataGraph);
		// STAT Of the DB END

		// find min timestamp and max timestamp:
		minTimestamp = Integer.MAX_VALUE;
		maxTimestamp = 0;

		for (Relationship rel : dataGraph.getAllRelationships()) {
			ArrayList<Integer> timePoints = TimeIntervalsOperation
					.getArrayListOfArray((int[]) rel.getProperty("timepoints"));

			minTimestamp = Math.min(minTimestamp, timePoints.get(0));
			maxTimestamp = Math.max(maxTimestamp, timePoints.get(timePoints.size() - 1));
		}

		System.out.println("minTimestamp:" + minTimestamp);
		System.out.println("maxTimestamp:" + maxTimestamp);
		DummyProperties.NUMBER_OF_SNAPSHOTS = maxTimestamp + 1; // including
																// zero!

		// init lattice
		// lattice = new LatticeReducedTwoSteps(focusSetPath,
		// maxAllowedHops, maxAllowedEdges, dataGraph, debugMode,
		// supportThreshold, confidenceThreshold, minTimestamp,
		// maxTimestamp, deltaT);
		lattice = new LatticeReducedTwoStepsOpt(focusSetPath, maxAllowedHops, maxAllowedEdges, dataGraph, debugMode,
				supportThreshold, confidenceThreshold, minTimestamp, maxTimestamp, deltaT);

		HashSet<Integer> allFocusNodes = new HashSet<Integer>();
		for (String focusLabel : lattice.allNodesOfFocusType.keySet()) {
			allFocusNodes.addAll(lattice.allNodesOfFocusType.get(focusLabel));
			// for (Integer nodeId :
			// lattice.allNodesOfFocusType.get(focusLabel)) {
			// lattice.labelAdjacencyIndexer.dataGraphNodeInfos.get(nodeId).setFocus();
			// }
			numberOfAllFocusNodes += lattice.allNodesOfFocusType.get(focusLabel).size();
		}

		// STAT Of the DB START
		avgDegreeOfFocusNodes = DummyFunctions.getAvgDegreeOfFocusNodes(dataGraph, allFocusNodes,
				numberOfAllFocusNodes);
		// STAT Of the DB END

		Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;
		if (DummyProperties.debugMode) {
			System.out.println("numberOfAllFocusNodes: " + numberOfAllFocusNodes);
		}

		tx1.success();
		tx1 = null;
		dataGraph.shutdown();
		dataGraph = null;
		System.gc();
		System.runFinalization();

		Thread.sleep(5000);

		return;

	}

	public static void main(String[] args) throws Exception {
		GTARFinderTwoSteps temporalGraphAssociationRuleFinder = new GTARFinderTwoSteps(args);
		temporalGraphAssociationRuleFinder.findGTARs();
	}
}
