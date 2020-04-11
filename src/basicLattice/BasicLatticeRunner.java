//package basicLattice;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashSet;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.graphdb.factory.GraphDatabaseSettings;
//
//import base.ILatticeNodeData;
//import utilities.Bitmap;
//import utilities.CorrectnessChecking;
//import utilities.DebugHelper;
//import utilities.Dummy;
//import utilities.Dummy.DummyFunctions;
//import utilities.Dummy.DummyProperties;
//import utilities.InfoHolder;
//import utilities.LatticeNode;
//import utilities.Rule;
//import utilities.TimeLogger;
//import utilities.TopKHandler;
//import utilities.Visualizer;
//
//public class BasicLatticeRunner {
//
//	private String focusSetPath;
//	private int maxAllowedHops;
//	private int maxAllowedEdges;
//	private String dataGraphPath;
//	private boolean debugMode;
//	private int k;
//	private GraphDatabaseService dataGraph;
//	private double threshold = 0.0d;
//	private int numberOfAllFocusNodes = 0;
//	// MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>> topKFrequentPatterns;
//	HashSet<Rule> frequentRules = new HashSet<Rule>();
//	Bitmap bitMap = new Bitmap();
//	private boolean windowMode = false;
//	private int windowSizeL = 2;
//	private int startingWindow = 0;
//	private int endingWindow = 1; // 0, 1, 2
//	public boolean timeout = false;
//	public LatticeAlg1 lattice;
//
//	public BasicLatticeRunner(String[] args) throws Exception {
//
//		for (int i = 0; i < args.length; i++) {
//			if (args[i].equals("-focusSetPath")) {
//				focusSetPath = args[++i];
//			} else if (args[i].equals("-maxAllowedHops")) {
//				maxAllowedHops = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-maxAllowedEdges")) {
//				maxAllowedEdges = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-dataGraphPath")) {
//				dataGraphPath = args[++i];
//			} else if (args[i].equals("-debugMode")) {
//				debugMode = Boolean.parseBoolean(args[++i]);
//			} else if (args[i].equals("-visualize")) {
//				DummyProperties.visualize = Boolean.parseBoolean(args[++i]);
//			} else if (args[i].equals("-k")) {
//				k = Integer.parseInt(args[++i]);
//			} else if (args[i].equals("-threshold")) {
//				threshold = Double.parseDouble(args[++i]);
//			} else if (args[i].equals("-windowMode")) {
//				windowMode = Boolean.parseBoolean(args[++i]);
//				DummyProperties.windowMode = windowMode;
//			} else if (args[i].equals("-windowSize")) {
//				windowSizeL = Integer.parseInt(args[++i]);
//				DummyProperties.WINDOW_SIZE = windowSizeL;
//				endingWindow = windowSizeL - 1;
//			}
//
//		}
//
//		if (focusSetPath == null || dataGraphPath == null || maxAllowedHops == 0 || maxAllowedEdges == 0) {
//			throw new Exception("input parameters: focusSetPath, maxAllowedHops, dataGraphPath, maxAllowedEdges");
//		} else {
//			System.out.println("StaticGraphTopK: -focusSetPath  " + focusSetPath + ", -maxAllowedHops:" + maxAllowedHops
//					+ ", -maxAllowedEdges  " + maxAllowedEdges + ", -dataGraphPath:" + dataGraphPath + ", threshold"
//					+ threshold);
//		}
//
//		if (maxAllowedEdges < maxAllowedHops) {
//			throw new Exception(" maxAllowedEdges < maxAllowedHops : " + maxAllowedEdges + " < " + maxAllowedHops);
//		}
//
//		Dummy.DummyProperties.debugMode = debugMode;
//
//		// findStaticTopK();
//
//	}
//
//	public BasicLatticeRunner(String focusSetPath, int maxHops, int maxEdges, String dataGraphPath, boolean debugMode,
//			int k, double threshold, boolean windowMode, int windowSizeL) {
//
//		this.focusSetPath = focusSetPath;
//		this.maxAllowedHops = maxHops;
//		this.maxAllowedEdges = maxEdges;
//		this.dataGraphPath = dataGraphPath;
//		this.debugMode = debugMode;
//		this.k = k;
//		this.threshold = threshold;
//		this.windowMode = windowMode;
//		DummyProperties.windowMode = windowMode;
//		this.windowSizeL = windowSizeL;
//		DummyProperties.WINDOW_SIZE = windowSizeL;
//		this.endingWindow = windowSizeL - 1;
//	}
//
//	public void findStaticTopK() throws Exception {
//
//		// initialize data graph
//		File storeDir = new File(dataGraphPath);
//		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
//				.setConfig(GraphDatabaseSettings.pagecache_memory, "2g").newGraphDatabase();
//
//		DummyFunctions.registerShutdownHook(dataGraph);
//
//		// topKFrequentPatterns = MinMaxPriorityQueue.orderedBy(new
//		// SupportComparator()).maximumSize(k).create();
//
//		// in order to get the lowerbound:
//		// topKFrequentPatterns.peekLast()
//
//		Transaction tx1 = dataGraph.beginTx();
//
//		// STAT Of the DB START
//		int numberOfAllNodes = Dummy.DummyFunctions.getNumberOfAllNodes(dataGraph);
//		int numberOfAllRelationships = Dummy.DummyFunctions.getNumberOfAllRels(dataGraph);
//		HashSet<String> differentLabels = Dummy.DummyFunctions.getDifferentLabels(dataGraph);
//		HashSet<String> differentRelTypes = Dummy.DummyFunctions.getDifferentRelType(dataGraph);
//
//		double avgDegrees = Dummy.DummyFunctions.getAvgOutDegrees(dataGraph);
//		// STAT Of the DB END
//
//		// init lattice
//		lattice = new LatticeAlg1(focusSetPath, maxAllowedHops, maxAllowedEdges, dataGraph, debugMode, bitMap,
//				frequentRules, threshold);
//
//		HashSet<Integer> allFocusNodes = new HashSet<Integer>();
//		for (String focusLabel : lattice.allNodesOfFocusType.keySet()) {
//			allFocusNodes.addAll(lattice.allNodesOfFocusType.get(focusLabel));
//			// for (Integer nodeId :
//			// lattice.allNodesOfFocusType.get(focusLabel)) {
//			// lattice.labelAdjacencyIndexer.dataGraphNodeInfos.get(nodeId).setFocus();
//			// }
//			numberOfAllFocusNodes += lattice.allNodesOfFocusType.get(focusLabel).size();
//		}
//
//		// initially, threshold is the percentage then we instantiate it with
//		// focus nodes...
//		// lattice.threshold = numberOfAllFocusNodes * threshold;
//
//		// STAT Of the DB START
//		double avgOutDegreeOfFocusNodes = DummyFunctions.getAvgOutDegreeOfFocusNodes(dataGraph, allFocusNodes,
//				numberOfAllFocusNodes);
//		// STAT Of the DB END
//
//		Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;
//		if (DummyProperties.debugMode) {
//			System.out.println("numberOfAllFocusNodes: " + numberOfAllFocusNodes);
//		}
//
//		double startTime = System.nanoTime();
//
//		// lattice.algorithmStartTimeJustForFocusFinding = startTime;
//
//		// generating the prefix tree for G0
//		LatticeNode<ILatticeNodeData> latticeRootNode = lattice.generateLatticeForG0();
//
//		// if (latticeRootNode == null) {
//		// tx1.success();
//		// dataGraph.shutdown();
//		// timeout = true;
//		// return;
//		// }
//
//		double latticeGenerationTimeEnd = System.nanoTime();
//
//		double latticeGenerationDuration = (latticeGenerationTimeEnd - startTime) / 1e6;
//
//		System.out.println("latticeGenerationTime: " + latticeGenerationDuration + " miliseconds.");
//
//		CorrectnessChecking.checkingDownwardProperty(latticeRootNode);
//
//		// double findTopkStart = System.nanoTime();
//		// TopKHandler.findTopK(frequentRules, lattice, k, latticeRootNode,
//		// threshold);
//		// double findTopkEnd = System.nanoTime();
//		// double findTopkDuration = (findTopkEnd - findTopkStart) / 1e6;
//		// System.out.println("findTopkTime: " + findTopkDuration + "
//		// miliseconds.");
//
//		int numberOfAllPatterns = lattice.bfsTraverse(lattice.emptyPTRootNode);
//
////		if (DummyProperties.debugMode)
////			Visualizer.visualizeFrequents(frequentRules);
//
//		//TopKHandler.printTopK(frequentRules);
//
//		if (DummyProperties.debugMode)
//			DebugHelper.printBiSimulatedPatterns(lattice);
//
//		// if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
//		// DebugHelper.printPatternWithDuplicateMatches(lattice);
//
////		if (DummyProperties.debugMode || DummyProperties.bigDataTestMode)
////			Visualizer.visualizePatternWithDuplicateMatches(lattice);
//
//		if (DummyProperties.visualize) {
//			for (int i = 1; i < maxAllowedEdges + 2; i++) {
//				Visualizer.visualizeALevel(lattice, i, i);
//			}
//		}
//
//		if (DummyProperties.debugMode)
//			DebugHelper.printIsomorphicPatterns(lattice);
//
//		// DebugHelper.getAllMatchesOrderedByNodeId(lattice);
//		//
//		// DebugHelper.printAllCandidates(lattice);
//
//		ArrayList<InfoHolder> timeInfos = new ArrayList<InfoHolder>();
//		timeInfos.add(new InfoHolder(0, "Focus", lattice.whatIsFocus));
//		timeInfos.add(new InfoHolder(1, "Nodes", numberOfAllNodes));
//		timeInfos.add(new InfoHolder(2, "Relationship", numberOfAllRelationships));
//		timeInfos.add(new InfoHolder(3, "Distinct Labels", differentLabels.size()));
//		timeInfos.add(new InfoHolder(4, "Distinct RelTypes", differentRelTypes.size()));
//		timeInfos.add(new InfoHolder(5, "Average of Total Degrees", avgDegrees));
//		timeInfos.add(new InfoHolder(6, "Average of Focus Out Degrees", avgOutDegreeOfFocusNodes));
//		timeInfos.add(new InfoHolder(7, "Patterns", numberOfAllPatterns));
//		timeInfos.add(new InfoHolder(8, "Total All Matches", lattice.numberOfTotalAllMatches));
//		timeInfos.add(new InfoHolder(9, "Focus Nodes", numberOfAllFocusNodes));
//		timeInfos.add(new InfoHolder(10, "Max Hops", maxAllowedHops));
//		timeInfos.add(new InfoHolder(11, "Max Edges", maxAllowedEdges));
//		timeInfos.add(new InfoHolder(12, "Lattice Generation Time", latticeGenerationDuration));
//		// timeInfos.add(new InfoHolder(13, "Find Topk Time",
//		// findTopkDuration));
//		timeInfos.add(new InfoHolder(14, "k", k));
//		timeInfos.add(new InfoHolder(15, "Threshold", threshold));
//
//		timeInfos.add(new InfoHolder(16, "Iso Checking Time", lattice.getDurationOfIsoChecking()));
//		timeInfos.add(new InfoHolder(17, "Number Of Iso Checking Request", lattice.getNumberOfIsoCheckingRequest()));
//		timeInfos.add(new InfoHolder(18, "Number Of Real Iso Checking", lattice.getNumberOfRealIsoChecking()));
//		timeInfos.add(new InfoHolder(19, "Creation Of New Lattice Node", lattice.getDurationOfNewLatticeGeneration()));
//		timeInfos.add(new InfoHolder(20, "WindowSize", DummyProperties.WINDOW_SIZE));
//		timeInfos.add(new InfoHolder(21, "WindowMode", DummyProperties.windowMode));
//		timeInfos.add(new InfoHolder(22, "Num. of support computations", lattice.getNumberOfComputeSupport()));
//		timeInfos.add(new InfoHolder(23, "Support computational time", lattice.getDurationOfComputeSupport()));
//
//		TimeLogger.LogTime(
//				"batch_" + (DummyProperties.windowMode ? "Win" : "Inc") + "_" + DummyProperties.WINDOW_SIZE + ".txt",
//				true, timeInfos);
//
//		tx1.success();
//
//		dataGraph.shutdown();
//
//	}
//
//	public static void main(String[] args) throws Exception {
//		BasicLatticeRunner staticGraphTopK = new BasicLatticeRunner(args);
//		staticGraphTopK.findStaticTopK();
//	}
//}
