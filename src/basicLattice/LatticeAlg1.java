//package basicLattice;
//
//import java.io.*;
//import java.util.*;
//
//import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
//import org.jgrapht.graph.ListenableDirectedGraph;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Label;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.graphdb.factory.GraphDatabaseSettings;
//
//import com.google.common.collect.MinMaxPriorityQueue;
//import com.google.common.collect.Sets;
//import com.google.common.collect.Sets.SetView;
//
//import base.ILattice;
//import base.ILatticeNodeData;
//import dualsim.BatDualSimulation;
//import utilities.Bitmap;
//import utilities.DefaultLabeledEdge;
//import utilities.DualSimulationHandler;
//import utilities.Dummy;
//import utilities.Dummy.DummyFunctions;
//import utilities.Dummy.DummyProperties;
//import utilities.Dummy.DummyProperties.LatticeMode;
//import utilities.GoBackToPrevHolder;
//import utilities.Indexer;
//import utilities.LatticeNode;
//import utilities.PatternNode;
//import utilities.Rule;
//import utilities.SupportComparator;
//
//import org.jgrapht.GraphMapping;
//import org.jgrapht.alg.DijkstraShortestPath;
//
////TODO: upperbound estimation should be filled before access.
////TODO: found all focus nodes should be filled by true whenever it found all focus because from there we have downward property
//
////TODO: when we want to create a new pattern we don't need to copy node matches from parent for source pattern node because source and target will be checked in the expansion time
////TODO: we can decrease memory usage if we use the number of some labels in d-hops instead of using the exact node ids.
//
////TODO: make sure that isMaximalFrequent, isFrequent, isValid, isVerfied, isVisited, MFP queue, top-k queue are up to date at each state of the program.
////TODO: make sure that top-k list/MFP list don't have duplicated items in them 
//
//public class LatticeAlg1 implements ILattice {
//	// SnapshotTopkMonitoring snapshotTopkMonitoring;
//	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
//	private int maxAllowedHops;
//	private int maxAllowedEdges;
//	public double threshold;
//	private String dataGraphPath;
//	GraphDatabaseService dataGraph;
//	public static final boolean The_Focus_Node = true;
//	public static final boolean FRESH_SOURCE = true;
//
//	// when we initialize a new child, we should add it here also
//	public HashMap<Integer, LatticeNode<ILatticeNodeData>> latticeNodeIndex = new HashMap<Integer, LatticeNode<ILatticeNodeData>>();
//	public int numberOfPatternsInLattice = 0;
//
//	// assumption: user give different types as focus.
//	// assumption: user can give property key values to just select some of the
//	// node with same type
//	// String: should be nodeType and then all focus node candidates.
//	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
//	private HashSet<String> focusLabelSet = new HashSet<String>();
//	private boolean debugMode = false;
//	String focusSetPath = null;
//	public Indexer labelAdjacencyIndexer;
//	public String whatIsFocus = "";
//	// a queue for processing all the waiting new PT nodes.
//	Queue<LatticeNode<ILatticeNodeData>> traversalQueue;
//
//	// maintain the same node level for graph isomorphism checking
//	ArrayList<LatticeNode<ILatticeNodeData>> sameLevelLatticeNodes;
//
//	public LatticeNode<ILatticeNodeData> emptyPTRootNode = null;
//	boolean goBackToPrev = false;
//	public Bitmap bitmap;
//	public int numberOfAllFocusNodes = 0;
//	public int numberOfTotalAllMatches = 0;
//
//	private double isoTimeStart = 0d;
//	private double isoTimeDuration = 0d;
//	private long numberOfIsoCheckingRequest = 0;
//	private long numberOfRealIsoChecking = 0;
//
//	private double creationOfNewLatticeNodeStart = 0d;
//	private double creationOfNewLatticeNodeDuration = 0d;
//
//	private int numberOfComputeSupport = 0;
//	private double computeSupportDuration = 0d;
//
//	private double checkValidityAtLeastOneMatchForEachPatternNodeStartTime = 0d;
//	public double checkValidityAtLeastOneMatchForEachPatternNodeDuration = 0d;
//
//	private double processQueueNodeStartTime = 0d;
//	public double processQueueNodeDuration = 0d;
//
//	public double algorithmStartTimeJustForFocusFinding = 0d;
//
//	public PriorityQueue<LatticeNode<ILatticeNodeData>> mfpLatticeNodes = new PriorityQueue<LatticeNode<ILatticeNodeData>>(
//			100, new SupportComparator());
//
//	public PriorityQueue<LatticeNode<ILatticeNodeData>> mipLatticeNodes = new PriorityQueue<LatticeNode<ILatticeNodeData>>(
//			100, new SupportComparator());
//
//	MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>> topKFrequentPatterns;
//	private HashSet<Rule> frequentRules;
//
//	public LatticeAlg1(String[] args) throws Exception {
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
//			}
//		}
//
//		if (focusSetPath == null || dataGraphPath == null || maxAllowedHops == 0) {
//			throw new Exception("input parameters: focusSetPath, maxAllowedHops, dataGraphPath");
//		} else {
//			if (DummyProperties.debugMode)
//				System.out.println("-focusSetPath  " + focusSetPath + ", -maxAllowedHops:" + maxAllowedHops);
//		}
//
//		DummyProperties.debugMode = debugMode;
//
//		// initialize data graph
//		File storeDir = new File(dataGraphPath);
//		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
//				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
//
//		try (Transaction tx1 = dataGraph.beginTx()) {
//
//			emptyPTRootNode = initializeLattice();
//			tx1.success();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		if (DummyProperties.debugMode)
//			System.out.println("focusSet size: " + focusLabelPropValSet.size());
//
//	}
//
//	/**
//	 * for static top-k finder
//	 * 
//	 * @param focusSetPath
//	 * @param maxAllowedHops
//	 * @param maxAllowedEdges
//	 * @param dataGraph
//	 * @param debugMode
//	 * @param bitMap
//	 * @param threshold
//	 * @throws Exception
//	 */
//	public LatticeAlg1(String focusSetPath, int maxAllowedHops, int maxAllowedEdges, GraphDatabaseService dataGraph,
//			boolean debugMode, Bitmap bitMap, HashSet<Rule> frequentRules, double threshold) throws Exception {
//
//		this.focusSetPath = focusSetPath;
//		this.maxAllowedHops = maxAllowedHops;
//		this.maxAllowedEdges = maxAllowedEdges;
//		this.dataGraph = dataGraph;
//		this.debugMode = debugMode;
//		this.bitmap = bitMap;
//		this.frequentRules = frequentRules;
//		this.threshold = threshold;
//		emptyPTRootNode = initializeLattice();
//		// this.topKFrequentPatterns = topKFrequentPatterns;
//	}
//
//	private void fillSetFromFile(String focusSetPath) throws Exception {
//		// the format should be like:
//		// NodeType | key1:value1, key2:value2
//		FileInputStream fis = new FileInputStream(focusSetPath);
//		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
//		String line = null;
//		while ((line = br.readLine()) != null) {
//			String[] labelAndPropKeyVals = line.trim().split("\\|");
//			ArrayList<PairStrings> propKeyValues = new ArrayList<PairStrings>();
//			if (labelAndPropKeyVals.length == 1) {
//				focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
//			} else if (labelAndPropKeyVals.length > 1) {
//				String[] keyValuePairs = labelAndPropKeyVals[1].split(",");
//				for (int i = 0; i < keyValuePairs.length; i++) {
//					String[] separatedKeyValue = keyValuePairs[i].split(":");
//					propKeyValues.add(new PairStrings(separatedKeyValue[0], separatedKeyValue[1].replace("\"", "")));
//				}
//
//			}
//			// Assumption: distinct labels
//			focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
//			whatIsFocus += line;
//		}
//		br.close();
//	}
//
//	public static void main(String[] args) throws Exception {
//
//		LatticeAlg1 lattice = new LatticeAlg1(args);
//		try (Transaction tx1 = lattice.dataGraph.beginTx()) {
//			LatticeNode<ILatticeNodeData> rootNode = lattice.generateLatticeForG0();
//			lattice.bfsTraverse(rootNode);
//			tx1.success();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		// lattice.printTreeDualSim(rootNode, dataGraph);
//
//		// Bitmap bitmap = new Bitmap();
//		// bitmap.store(latticeNodeIndex, dataGraph);
//
//		// TODO: traversing prefix-tree to check dual-simulation of each pattern
//		// over neo4j data graph
//
//	}
//
//	private LatticeNode<ILatticeNodeData> initializeLattice() throws Exception {
//
//		// filling focusLabelPropValSet
//		fillSetFromFile(focusSetPath);
//
//		emptyPTRootNode = null;
//
//		// Transaction tx1 = dataGraph.beginTx();
//
//		// generating the root of the prefix tree
//		ILatticeNodeData emptyPTRootData = new LatticeNodeDataAlg1(focusLabelSet);
//		emptyPTRootNode = new LatticeNode<ILatticeNodeData>(emptyPTRootData);
//
//		emptyPTRootNode.getData().setPatternLatticeNodeIndex(-1);
//		// this.latticeNodeIndex.put(emptyPTRootNode.getData().getPatternLatticeNodeIndex(),
//		// emptyPTRootNode);
//
//		// the first level index should be set, otherwise all the levels
//		// will be null!
//		emptyPTRootNode.setRootLevel();
//
//		// getting all focus nodes of the prefix-tree
//		fillFocusNodesOfRequestedTypes(dataGraph);
//
//		// a queue for processing all the waiting new PT nodes.
//		traversalQueue = new LinkedList<LatticeNode<ILatticeNodeData>>();
//
//		// maintain the same node level for graph isomorphism checking
//		sameLevelLatticeNodes = new ArrayList<LatticeNode<ILatticeNodeData>>();
//
//		labelAdjacencyIndexer = new Indexer(dataGraph, 1, allNodesOfFocusType);
//
//		// we don't need to worry about their existence after that.
//		for (String focusLabel : allNodesOfFocusType.keySet()) {
//
//			PatternNode focusNode = new PatternNode(focusLabel.intern(), The_Focus_Node);
//
//			HashSet<Integer> dgGraphMatchNodes = new HashSet<Integer>();
//			for (Integer nodeId : allNodesOfFocusType.get(focusLabel)) {
//				dgGraphMatchNodes.add(nodeId);
//			}
//			ILatticeNodeData firstLevelChildData = new LatticeNodeDataAlg1(focusNode, dgGraphMatchNodes, focusLabelSet,
//					numberOfPatternsInLattice, labelAdjacencyIndexer);
//
//			LatticeNode<ILatticeNodeData> firstLevelChildPTNode = new LatticeNode<ILatticeNodeData>(
//					firstLevelChildData);
//
//			emptyPTRootNode.addChild(firstLevelChildPTNode);
//
//			traversalQueue.add(firstLevelChildPTNode);
//
//			latticeNodeIndex.put(numberOfPatternsInLattice++, firstLevelChildPTNode);
//
//		} // all the labels added as the children of the PT root
//
//		if (DummyProperties.debugMode)
//			System.out.println(
//					"Before: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1e9);
//		labelAdjacencyIndexer.generateDistinctNodesAdjMatrix(maxAllowedHops);
//		if (DummyProperties.debugMode)
//			System.out.println(
//					"After: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1e9);
//
//		return emptyPTRootNode;
//	}
//
//	/**
//	 * private void estimateUpperbound(LatticeNode <LatticeNodeData> parentNode,
//	 * LatticeNode <LatticeNodeData> tempProcessingNode, HashMap<String, HashSet
//	 * <Integer>> allNodesOfFocusType, PatternNode lastPatternNode, String
//	 * relationshipType, int snapshot) throws Exception {
//	 * 
//	 * // it should be minimum of parent support and ....
//	 * 
//	 * double parentSnapshotSupport = Double.MAX_VALUE;
//	 * 
//	 * if (tempProcessingNode.getParent().getData().lastSeenSnapshot < snapshot)
//	 * { if (tempProcessingNode.getParent().getData().snapshotUB[snapshot] == 0)
//	 * { tempProcessingNode.getData().snapshotUB[snapshot] = 1; }
//	 * tempProcessingNode.getData().setTotalUpperbound(snapshot); }
//	 * 
//	 * if (parentNode.getData().isVerified) parentSnapshotSupport =
//	 * parentNode.getData().supportFrequency[snapshot];
//	 * 
//	 * // PatternNode lastPatternNode = //
//	 * tempProcessingNode.getData().targetPatternNode; // // if (destPatternNode
//	 * != null) { // lastPatternNode = destPatternNode; // }
//	 * 
//	 * if (lastPatternNode == null) { throw new Exception(
//	 * "lastPatternNode is null for finding shortest path!"); }
//	 * 
//	 * // the number of focus nodes which has the possibility of being neighbor
//	 * // of this new node double cnt = 0;
//	 * 
//	 * for (PatternNode patternNode :
//	 * tempProcessingNode.getData().patternGraph.vertexSet()) { if
//	 * (patternNode.isFocus()) { // TODO: may be we can save this information
//	 * also in the pattern // node like steps from root!
//	 * DijkstraShortestPath<PatternNode, DefaultLabeledEdge> djShortestPath =
//	 * new DijkstraShortestPath<PatternNode, DefaultLabeledEdge>(
//	 * tempProcessingNode.getData().patternGraph, patternNode, lastPatternNode);
//	 * 
//	 * double length = djShortestPath.getPathLength();
//	 * 
//	 * if (length == Double.POSITIVE_INFINITY) { cnt +=
//	 * tempProcessingNode.getData().getMatchedNodes().
//	 * getDataGraphMatchNodeOfAbsPNode().get( patternNode).size(); } else { for
//	 * (Integer nodeId : tempProcessingNode.getData().getMatchedNodes().
//	 * getDataGraphMatchNodeOfAbsPNode() .get(patternNode)) { if
//	 * (labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId)
//	 * .get(lastPatternNode.getLabel() +
//	 * DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType) != null
//	 * && labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(nodeId)
//	 * .get(lastPatternNode.getLabel() +
//	 * DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
//	 * .get((int) length) != null &&
//	 * labelAdjacencyIndexer.distinctNodesOfDHopsAway
//	 * .get(nodeId).get(lastPatternNode.getLabel() +
//	 * DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
//	 * .get((int) length).size() > 0) cnt++; } }
//	 * 
//	 * } } double possibleSnapshotUB = cnt /
//	 * DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
//	 * 
//	 * tempProcessingNode.getData().snapshotUB[snapshot] =
//	 * Math.min(parentSnapshotSupport, possibleSnapshotUB);
//	 * 
//	 * tempProcessingNode.getData().setTotalUpperbound(snapshot);
//	 * 
//	 * }
//	 **/
//
//	public LatticeNode<ILatticeNodeData> generateLatticeFromHere(
//			MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>> topKFrequentPatterns,
//			HashSet<Integer> newCreatedOrTouchedPTNodes, LatticeMode latticeMode, int snapshot, double threshold)
//			throws Exception {
//
//		// FOR-DEBUG START
//		// if (traversalQueue.isEmpty())
//		// return emptyPTRootNode;
//
//		// removeNullObjects();
//
//		// FOR-DEBUG END
//
//		int latticeProcessingLevel = traversalQueue.peek().getLevel();
//
//		// if(latticeProcessingLevel==3){
//		// System.out.println();
//		// }
//
//		while (!traversalQueue.isEmpty()) {
//
//			if (DummyProperties.debugMode) {
//				System.out.println();
//				System.out.println("traversalQueue.size: " + traversalQueue.size());
//			}
//
//			LatticeNode<ILatticeNodeData> tempProcessingNode = traversalQueue.poll();
//
//			tempProcessingNode.getData().setVisited(true);
//
//			// if (tempProcessingNode.getData().getPatternLatticeNodeIndex()
//			// == 4)
//			// {
//			// System.out.println();
//			// }
//
//			// FOR-DEBUG START
//			// if (tempProcessingNode == null || tempProcessingNode.getData() ==
//			// null) {
//			// tempProcessingNode = null;
//			// continue;
//			// }
//			// FOR-DEBUG END
//
//			if (!tempProcessingNode.getData().isValid())
//				// TODO: if i can i should remove checkValidty instead whenever
//				// i remove a match
//				// i check if it's valid yet or not
//				tempProcessingNode.getData()
//						.setValid(checkValidityAtLeastOneMatchForEachPatternNode(tempProcessingNode));
//
//			if (!tempProcessingNode.getData().isValid())
//				// TODO:make sure that "tempProcessingNode" doesn't take memory
//				continue;
//
//			try {
//				// double lowerbound = 0;
//				boolean wasFrequent = tempProcessingNode.getData().isFrequent();
//				boolean wasMFP = tempProcessingNode.getData().isMaximalFrequent();
//
//				// TODO: may be if we consider super linked nodes too we can
//				// find a tighter upperbound
//				// estimateUpperbound(tempProcessingNode.getParent(),
//				// tempProcessingNode, allNodesOfFocusType,
//				// tempProcessingNode.getData().targetPatternNode,
//				// tempProcessingNode.getData().relationshipType,
//				// snapshot);
//
//				// if UB>=LB then compute support and try to add in topk
//
//				// if (topKFrequentPatterns.size() > 0) {
//				// FOR-DEBUG START
//				// if (topKFrequentPatterns.peekLast().getData() == null) {
//				// System.err.println("topKFrequentPatterns.peekLast().getData()
//				// == null");
//				// topKFrequentPatterns.removeLast();
//				// } else {
//				// lowerbound =
//				// topKFrequentPatterns.peekLast().getData().getTotalSupportFrequency();
//				// FOR-DEBUG END}
//				// }
//
//				// check for dual-simulation in an incremental way
//				DualSimulationHandler.computeSupport(dataGraph, tempProcessingNode, snapshot, this);
//
//				if (!tempProcessingNode.getData().isValid())
//					continue;
//
//				if (!checkSameTypeSameStepsFromRootHasEnoughMatches(tempProcessingNode)) {
//					tempProcessingNode.getData().setCorrectness(false, tempProcessingNode, this, 0);
//					continue;
//				}
//
//				if (tempProcessingNode.getData().getTotalSupportFrequency() < threshold) {
//					continue;
//				}
//
//				// if (tempProcessingNode.getData().getTotalSupportFrequency()
//				// >=
//				// lowerbound) {
//
//				// A*->A {0} , A*->A {0} is wrong!
//				// if (hasSameNeighborsWithLessMatch(tempProcessingNode)) {
//				// tempProcessingNode.getData().isValid() = false;
//				// tempProcessingNode.getData()..isFrequent() = false;
//				// tempProcessingNode.getData().isMaximalFrequent() = false;
//				// mfpLatticeNodes.remove(tempProcessingNode);
//				// tempProcessingNode.getData().isVerified = true;
//				// tempProcessingNode.getData().removeFromTopK(this,
//				// tempProcessingNode);
//				// removeLatticeNode(tempProcessingNode,
//				// newCreatedOrTouchedPTNodes, sameLevelLatticeNodes);
//				//
//				// if (DummyProperties.debugMode)
//				// System.out.println("end hasSameNeighborsWithLessMatch:
//				// true");
//				//
//				// continue;
//				// }
//
//				// if it's frequent right now
//				if (tempProcessingNode.getData().getTotalSupportFrequency() >= threshold) {
//					if (
//					// new frequent child
//					!tempProcessingNode.getData().isVisited()
//							// nonFreq -> Freq
//							|| !wasFrequent) {
//
//						tempProcessingNode.getData().setAsMFP(tempProcessingNode, tempProcessingNode.getParent(),
//								tempProcessingNode.getSuperNodeLinks(), this, snapshot);
//
//						// updateNumberOfFrequentChildrenOrLinked(tempProcessingNode.getParent(),
//						// tempProcessingNode.getSuperNodeLinks(), +1);
//					}
//				} else {
//					// if it's not frequent right now
//					if (wasFrequent) {
//
//						// updateNumberOfFrequentChildrenOrLinked(tempProcessingNode.getParent(),
//						// tempProcessingNode.getSuperNodeLinks(), -1);
//
//						if (wasMFP) {
//							// non-MFP's shouldn't be inside of the topK
//							// list also
//							tempProcessingNode.getData().maxFreqToNonFreqHandling(tempProcessingNode, this, snapshot);
//						} else {
//							tempProcessingNode.getData().freqToNonFreqHandling(tempProcessingNode);
//						}
//					}
//				}
//
//				// we just offer new MFP to the top-k list
//				if (tempProcessingNode.getData().isMaximalFrequent()
//						&& tempProcessingNode.getData().getFoundAllFocuses()) {
//					tempProcessingNode.getData().addToTopK(this, tempProcessingNode);
//				}
//
//				if (DummyProperties.debugMode)
//					System.out.println(tempProcessingNode.getData().getMappedGraphString() + " -> supp:"
//							+ tempProcessingNode.getData().getSupportFrequency(snapshot));
//				// }
//			} catch (Exception exc) {
//
//				System.out.println(tempProcessingNode.getData().getMappedGraphString());
//				throw exc;
//			}
//
//			// if (tempProcessingNode.getData().getSupportFrequency(snapshot) ==
//			// 0.0d) {
//			// tempProcessingNode.getData().setPatternAsInvalid();
//			// TODO: I've commented this after wrting the until edge
//			// deletion
//			// because we can maintain all the patterns and just make them
//			// invalid
//			// so we should make sure that we don't expand invalid patterns
//
//			// bitmap.removeOnePatternIndexForAllNodesHavingIt(
//			// tempProcessingNode.getData().getPatternLatticeNodeIndex());
//
//			// TODO: i've commented this part after writing delete unit edge
//			// because i want to maintain all patterns and just make them
//			// invalid
//			// latticeNodeIndex.remove(tempProcessingNode.getData().getPatternLatticeNodeIndex());
//
//			// if (newCreatedOrTouchedPTNodes != null) {
//			// newCreatedOrTouchedPTNodes.remove(tempProcessingNode.getData().getPatternLatticeNodeIndex());
//			// }
//			// tempProcessingNode.removeAllReferences();
//			// tempProcessingNode = null;
//			// continue;
//			// }
//
//			if (!tempProcessingNode.getData().isValid()) {
//				continue;
//			}
//
//			if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
//				continue;
//			}
//
//			if (latticeProcessingLevel < tempProcessingNode.getLevel()) {
//				// new sibling will be created soon.
//				// old ones should be cleared
//				sameLevelLatticeNodes.clear();
//				// going to the next level
//				latticeProcessingLevel = tempProcessingNode.getLevel();
//				if (DummyProperties.debugMode)
//					System.out.println("latticeProcessingLevel1: " + latticeProcessingLevel);
//			}
//
//			// if (tempProcessingNode.getData().getPatternLatticeNodeIndex()
//			// == 6)
//			// {
//			// System.out.println();
//			// }
//
//			// TODO: where this isValid will be true?!
//
//			// FOR-DEBUG STARTif (tempProcessingNode != null &&
//			// tempProcessingNode.getData().isValid()) {
//			if (DummyProperties.debugMode) {
//				System.out.println("generator processing node:");
//				System.out.println(tempProcessingNode.getData().getMappedGraphString());
//			}
//
//			processQueueNodeStartTime = System.nanoTime();
//			processQueueNode(dataGraph, traversalQueue, sameLevelLatticeNodes, tempProcessingNode,
//					newCreatedOrTouchedPTNodes, latticeMode, snapshot);
//			processQueueNodeDuration += ((System.nanoTime() - processQueueNodeStartTime) / 1e6);
//			//// FOR-DEBUG END}
//
//		}
//
//		if (DummyProperties.debugMode)
//			System.out.println("finishing queue!");
//
//		return emptyPTRootNode;
//
//	}
//
//	private boolean checkSameTypeSameStepsFromRootHasEnoughMatches(LatticeNode<ILatticeNodeData> tempProcessingNode) {
//
//		for (PatternNode srcPatternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
//			for (String nexType : tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
//					.get(srcPatternNode).keySet()) {
//
//				int howManyOfSameType = tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
//						.get(srcPatternNode).get(nexType);
//				if (howManyOfSameType > 1) {
//
//					HashSet<Integer> allMatchNodesSet = new HashSet<Integer>();
//					for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph()
//							.outgoingEdgesOf(srcPatternNode)) {
//
//						String tempNexType = tempProcessingNode.getData().getPatternGraph().getEdgeTarget(e).getLabel()
//								+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType();
//
//						if (nexType.equals(tempNexType)) {
//							allMatchNodesSet.addAll(
//									tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
//											.get(tempProcessingNode.getData().getPatternGraph().getEdgeTarget(e)));
//						}
//					}
//					if (allMatchNodesSet.size() < howManyOfSameType) {
//						return false;
//					}
//				}
//
//			}
//		}
//		return true;
//	}
//
//	public LatticeNode<ILatticeNodeData> generateLatticeForG0() throws Exception {
//
//		// try (Transaction tx1 = dataGraph.beginTx()) {
//		int latticeProcessingLevel = traversalQueue.peek().getLevel();
//
//		while (!traversalQueue.isEmpty()) {
//
//			// if (algorithmStartTimeJustForFocusFinding > 0) {
//			// double duration = ((System.nanoTime() -
//			// algorithmStartTimeJustForFocusFinding) / 1e6);
//			// if (duration > 120000) {
//			// return null;
//			// }
//			// }
//
//			if (DummyProperties.debugMode) {
//				System.out.println();
//				System.out.println("traversalQueue.size: " + traversalQueue.size());
//			}
//
//			LatticeNode<ILatticeNodeData> tempProcessingNode = traversalQueue.poll();
//
//			// if (tempProcessingNode.getData().getPatternLatticeNodeIndex()
//			// == 5) {
//			// System.out.println();
//			// }
//
//			if (!tempProcessingNode.getData().isValid())
//				tempProcessingNode.getData()
//						.setValid(checkValidityAtLeastOneMatchForEachPatternNode(tempProcessingNode));
//
//			if (!tempProcessingNode.getData().isValid())
//				// TODO:make sure that "tempProcessingNode" doesn't take memory
//				continue;
//
//			try {
//				if(tempProcessingNode.getData().getPatternLatticeNodeIndex() == 8){
//					System.out.println();
//				}
//				// check for dual-simulation in an incremental way
//				DualSimulationHandler.computeSupport(dataGraph, tempProcessingNode, 0, this);
//				// tempProcessingNode.getData().getTotalSupportFrequency() =
//				// tempProcessingNode.getData().supportFrequency[0];
//
//				if (!checkSameTypeSameStepsFromRootHasEnoughMatches(tempProcessingNode)) {
//					tempProcessingNode.getData().setCorrectness(false, tempProcessingNode, this, 0);
//					continue;
//				}
//
//				if (tempProcessingNode.getData().getTotalSupportFrequency() < threshold) {
//					continue;
//				}
//
//				if (DummyProperties.debugMode)
//					System.out.println(tempProcessingNode.getData().getMappedGraphString() + " -> supp:"
//							+ tempProcessingNode.getData().getSupportFrequency(0));
//			} catch (Exception exc) {
//				System.out.println(tempProcessingNode.getData().getMappedGraphString());
//				throw exc;
//			}
//
//			// if (hasSameNeighborsWithLessMatch(tempProcessingNode)) {
//			// tempProcessingNode.getData().isValid() = false;
//			// tempProcessingNode.getData().isFrequent() = false;
//			// tempProcessingNode.getData().isMaximalFrequent() = false;
//			// tempProcessingNode.getData().isVerified = true;
//			// removeLatticeNode(tempProcessingNode, null,
//			// sameLevelLatticeNodes);
//			//
//			// if (DummyProperties.debugMode)
//			// System.out.println("end hasSameNeighborsWithLessMatch: true");
//			//
//			// continue;
//			// }
//
//			tempProcessingNode.getData().setVisited(true);
//
//			if (!tempProcessingNode.getData().isValid()) {
//				continue;
//			}
//
//			if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
//				continue;
//			}
//
//			if (latticeProcessingLevel < tempProcessingNode.getLevel()) {
//				// new sibling will be created soon.
//				// old ones should be cleared
//				sameLevelLatticeNodes.clear();
//				// going to the next level
//				latticeProcessingLevel = tempProcessingNode.getLevel();
//				if (DummyProperties.debugMode)
//					System.out.println("latticeProcessingLevel G0: " + latticeProcessingLevel);
//				// if (latticeProcessingLevel == 5) {
//				// // System.out.println();
//				// }
//			}
//
//			if (tempProcessingNode != null && tempProcessingNode.getData().isValid()) {
//				if (DummyProperties.debugMode) {
//					System.out.println("generator processing node:");
//					System.out.println(tempProcessingNode.getData().getMappedGraphString());
//				}
//
//				// if
//				// (tempProcessingNode.getData().getPatternLatticeNodeIndex()
//				// == 7) {
//				// System.out.println();
//				// }
//
//				processQueueNode(dataGraph, traversalQueue, sameLevelLatticeNodes, tempProcessingNode, null,
//						LatticeMode.BATCH, 0);
//			}
//		}
//
//		if (DummyProperties.debugMode)
//			System.out.println("finishing queue!");
//		return emptyPTRootNode;
//	}
//
//	private void fillFocusNodesOfRequestedTypes(GraphDatabaseService dataGraph2) throws Exception {
//
//		for (String focusLabel : focusLabelPropValSet.keySet()) {
//			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
//			focusLabelSet.add(focusLabel);
//		}
//
//		for (String focusLabel : focusLabelPropValSet.keySet()) {
//			ArrayList<PairStrings> propVals = focusLabelPropValSet.get(focusLabel);
//			for (Node node : dataGraph.getAllNodes()) {
//				// boolean isCandidate = true;
//				if (!node.hasLabel(Label.label(focusLabel))) {
//					continue;
//				}
//				if (propVals.size() > 0) {
//					for (PairStrings pairString : propVals) {
//
//						if (node.hasProperty(pairString.key)) {
//							if (DummyFunctions.isContain(node.getProperty(pairString.key).toString().toLowerCase(),
//									pairString.value.toLowerCase())
//									|| node.getProperty(pairString.key).toString().toLowerCase()
//											.equals(pairString.value.toLowerCase())) {
//								allNodesOfFocusType.get(focusLabel).add((int) node.getId());
//								break;
//							}
//						}
//
//					}
//				} else {
//					allNodesOfFocusType.get(focusLabel).add((int) node.getId());
//				}
//
//			}
//		}
//
//		numberOfAllFocusNodes = 0;
//		for (String key : allNodesOfFocusType.keySet()) {
//			if (allNodesOfFocusType.get(key).size() == 0) {
//				throw new Exception("no items for \"" + key + "\"");
//			}
//
//			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();
//		}
//
//		Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;
//		if (DummyProperties.debugMode) {
//			System.out.println("focusNodesOfSpecificType size: " + allNodesOfFocusType.size());
//		}
//	}
//
//	private void processQueueNode(GraphDatabaseService dataGraph, Queue<LatticeNode<ILatticeNodeData>> traversalQueue,
//			ArrayList<LatticeNode<ILatticeNodeData>> sameLevelLatticeNodes,
//			LatticeNode<ILatticeNodeData> tempProcessingNode, HashSet<Integer> newCreatedOrTouchedPTNodes,
//			LatticeMode latticeMode, int snapshot) throws Exception {
//
//		// while we are inside of this method we expand the same pattern to
//		// generate all the possible children
//
//		// get the pattern
//		// for all nodes in the pattern
//		ILatticeNodeData tempProcessingNodeData = tempProcessingNode.getData();
//
//		for (PatternNode srcPatternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
//
//			// if it's in the update mode, we should just expand new nodes
//			// then for better performance we don't need to go further
//			if (tempProcessingNodeData.getLatticeMode() == LatticeMode.UPDATE) {
//				if (DummyProperties.debugMode) {
//					System.out.println("newUnexpandedPatternsNodesOfNeo4jNodes: "
//							+ tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes());
//				}
//
//				// TODO: patch:
//				// tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes()
//				// == null why it's null?!
//				if (tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes() == null
//						|| !tempProcessingNodeData.getNewUnexpandedPatternsNodesOfNeo4jNodes().keySet()
//								.contains(srcPatternNode))
//					continue;
//			}
//
//			// if it needs any new expansion based on its hops from the root
//			if (tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) < maxAllowedHops) {
//				if (DummyProperties.debugMode) {
//					System.out.println(
//							"srcAbstractPatternNode:" + srcPatternNode.getType() + "" + srcPatternNode.isFocus());
//				}
//
//				// String: the destination because source are same
//				HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcOutgoing = new HashMap<String, LatticeNode<ILatticeNodeData>>();
//				HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing = new HashMap<PatternNode, LatticeNode<ILatticeNodeData>>();
//
//				HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcIncoming = new HashMap<String, LatticeNode<ILatticeNodeData>>();
//				HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming = new HashMap<PatternNode, LatticeNode<ILatticeNodeData>>();
//
//				// TODO: if we create this index inside of pattern creation then
//				// it will be cheaper in terms of time
//				// but more expensive in terms of memory
//
//				int matchGraphIndex = -1;
//				// for all match nodes in this prefix-tree node and for this src
//				// pattern node
//				for (Integer srcDataGpNodeId : tempProcessingNodeData.getMatchedNodes()
//						.getDataGraphMatchNodeOfAbsPNode().get(srcPatternNode)) {
//
//					if (tempProcessingNodeData.getLatticeMode() == LatticeMode.UPDATE) {
//						if (!tempProcessingNodeData.getNewUnexpandedNodesOfPatternNodes().keySet()
//								.contains(srcDataGpNodeId)) {
//							continue;
//							// TODO: maintaining update/batch mode of the PT
//							// nodes
//						}
//
//						// maybe A*={a0,a1} and A={a0,a1}
//						// ???
//						if (!tempProcessingNodeData.getNewUnexpandedNodesOfPatternNodes().get(srcDataGpNodeId)
//								.contains(srcPatternNode)) {
//							continue;
//						}
//					}
//
//					// if same sibling type of srcPatternNode previously
//					// expanded from this srcDataNodeId and right now
//					// they are alone (just one match node for the pattern node)
//					// or has some outdegree
//					// we shouldn't expand from this src data node id at all
//					boolean expandedSrcBefore = false;
//					for (DefaultLabeledEdge e1 : tempProcessingNodeData.getPatternGraph()
//							.incomingEdgesOf(srcPatternNode)) {
//						PatternNode parentOfSrcPattern = tempProcessingNodeData.getPatternGraph().getEdgeSource(e1);
//						for (DefaultLabeledEdge e2 : tempProcessingNodeData.getPatternGraph()
//								.outgoingEdgesOf(parentOfSrcPattern)) {
//							if (e2.getType().equals(e1.getType())
//									&& tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2) != srcPatternNode
//									&& tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2).getType()
//											.equals(srcPatternNode.getType())) {
//
//								if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
//										.get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e2))
//										.contains(srcDataGpNodeId)
//										&& (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
//												.size() == 1
//										/*
//										 * || tempProcessingNodeData.
//										 * getPatternGraph(). outDegreeOf(
//										 * tempProcessingNodeData.
//										 * getPatternGraph(). getEdgeTarget(e2))
//										 * > 0
//										 */)
//
//								) {
//									expandedSrcBefore = true;
//									break;
//								}
//
//							}
//
//						}
//						if (expandedSrcBefore) {
//							break;
//						}
//
//					}
//					if (expandedSrcBefore)
//						continue;
//
//					matchGraphIndex++;
//
//					// System.out.println("srcNodeId:" +
//					// srcDataGpNode.patternGNodeId);
//
//					if (!tempProcessingNode.getData().getFoundAllFocuses()) {
//
//						// first expandToFocuses;
//						expandToFocusesFirst(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
//								matchGraphIndex, seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
//								newlySeenPatternNodeForThisSrcOutgoing, newCreatedOrTouchedPTNodes, srcDataGpNodeId,
//								snapshot, seenPatternNodeFromPreviousNodesForThisSrcIncoming,
//								newlySeenPatternNodeForThisSrcIncoming);
//					}
//
//					else {
//						// TODO: out degree indexing is needed here
//						if ((labelAdjacencyIndexer.dataGraphNodeInfos
//								.get(srcDataGpNodeId).outDegree > tempProcessingNodeData.getPatternGraph()
//										.outDegreeOf(srcPatternNode))) {
//
//							// of all possible labels_reltype
//							for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway
//									.get(srcDataGpNodeId).keySet()) {
//
//								// outgoing expansion
//								outogingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
//										matchGraphIndex, seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
//										newlySeenPatternNodeForThisSrcOutgoing, newCreatedOrTouchedPTNodes,
//										srcDataGpNodeId, otherNodeLabelRelType, snapshot);
//
//								// if (latticeNodeIndex.get(7) != null) {
//								// System.out.println("repeating\n" +
//								// latticeNodeIndex.get(7).getData().getMappedGraphString());
//								// }
//							}
//						}
//
//						if ((labelAdjacencyIndexer.dataGraphNodeInfos
//								.get(srcDataGpNodeId).inDegree > tempProcessingNodeData.getPatternGraph()
//										.inDegreeOf(srcPatternNode))) {
//							for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway
//									.get(srcDataGpNodeId).keySet()) {
//
//								// incoming expansion
//								incomingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
//										matchGraphIndex, seenPatternNodeFromPreviousNodesForThisSrcIncoming,
//										newlySeenPatternNodeForThisSrcIncoming, newCreatedOrTouchedPTNodes,
//										srcDataGpNodeId, otherNodeLabelRelType, snapshot);
//
//								// if (latticeNodeIndex.get(7) != null) {
//								// System.out.println("repeating\n"
//								// +latticeNodeIndex.get(7).getData().getMappedGraphString());
//								// }
//							}
//						}
//					}
//
//				}
//			}
//		}
//
//		tempProcessingNodeData.renewNewUnexpandedNodesOfPatternNodes();
//		tempProcessingNodeData.renewNewUnexpandedPatternsNodesOfNeo4jNodes();
//
//	}
//
//	private void expandToFocusesFirst(LatticeNode<ILatticeNodeData> tempProcessingNode,
//			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode, int matchGraphIndex,
//			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
//			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcOutgoing,
//			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, int snapshot,
//			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming,
//			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcIncoming) throws Exception {
//
//		// of all possible labels_reltype
//		if ((labelAdjacencyIndexer.dataGraphNodeInfos.get(srcDataGpNodeId).outDegree > tempProcessingNodeData
//				.getPatternGraph().outDegreeOf(srcPatternNode))) {
//			for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
//					.keySet()) {
//
//				// outgoing expansion
//				outogingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode, matchGraphIndex,
//						seenPatternNodeFromPreviousNodesForThisSrcOutgoing, newlySeenPatternNodeForThisSrcOutgoing,
//						newCreatedOrTouchedPTNodes, srcDataGpNodeId, otherNodeLabelRelType, snapshot);
//			}
//		}
//
//		if ((labelAdjacencyIndexer.dataGraphNodeInfos.get(srcDataGpNodeId).inDegree > tempProcessingNodeData
//				.getPatternGraph().inDegreeOf(srcPatternNode))) {
//			for (String otherNodeLabelRelType : labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
//					.keySet()) {
//
//				// incoming expansion
//				incomingExpansion2(tempProcessingNode, tempProcessingNodeData, srcPatternNode, matchGraphIndex,
//						seenPatternNodeFromPreviousNodesForThisSrcIncoming, newlySeenPatternNodeForThisSrcIncoming,
//						newCreatedOrTouchedPTNodes, srcDataGpNodeId, otherNodeLabelRelType, snapshot);
//			}
//		}
//
//	}
//
//	private void incomingExpansion2(LatticeNode<ILatticeNodeData> tempProcessingNode,
//			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode, int matchGraphIndex,
//			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrc,
//			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrc,
//			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, String otherNodeLabelRelType,
//			int snapshot) throws Exception {
//
//		HashMap<String, Integer> frequencyOfSameInNeighborType = getFrequencyOfSameInNeighborType(
//				tempProcessingNodeData, srcPatternNode);
//
//		HashSet<Integer> sameLabelNeighborNodes = labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
//				.get(otherNodeLabelRelType).get(Indexer.BEFORE);
//
//		if (sameLabelNeighborNodes != null && sameLabelNeighborNodes.size() > 0) {
//			if (!frequencyOfSameInNeighborType.containsKey(otherNodeLabelRelType)
//					|| frequencyOfSameInNeighborType.get(otherNodeLabelRelType) < sameLabelNeighborNodes.size()) {
//
//				int separatorIndex = otherNodeLabelRelType
//						.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
//				String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
//				String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);
//
//				Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode)
//						+ 1;
//
//				// goBackToPrev = false;
//				ArrayList<GoBackToPrevHolder> destPatternNodes = new ArrayList<GoBackToPrevHolder>();
//				getDestPatternNodeAndCheckForGoBackToPrevIncoming2(destPatternNodes, tempProcessingNodeData,
//						srcPatternNode, srcDataGpNodeId, sameLabelNeighborNodes, destLabel, relationshipType,
//						destStepsFromRoot);
//
//				// for (Integer newNodeId : sameLabelNeighborNodes) {
//
//				// if (DummyProperties.debugMode) {
//				// System.out.println("matchGraphIndex: " + matchGraphIndex + ",
//				// srcNodeId:" + srcDataGpNodeId
//				// + ", newNodeId:" + newNodeId);
//				// }
//
//				// boolean expandedBefore = false;
//
//				for (int d = 0; d < destPatternNodes.size(); d++) {
//					GoBackToPrevHolder destPatternNode = destPatternNodes.get(d);
//					destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
//
//					HashSet<Integer> newNodeIdsMustBeRemoved = new HashSet<Integer>();
//
//					for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
//							.incomingEdgesOf(srcPatternNode)) {
//
//						// if from this src I went to the
//						// target with the newNodeId
//						// and out degree of that is more
//						// than 0 which means that it
//						// verified before.
//						// or it has just that node we
//						// shouldn't expand to that node
//						// again
//						for (Integer newNodeId : destPatternNode.newNodeIds) {
//							if (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
//									.get(tempProcessingNodeData.getPatternGraph().getEdgeSource(e)).contains(newNodeId)
//									&& (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
//											.get(tempProcessingNodeData.getPatternGraph().getEdgeSource(e))
//											.size() == 1)) {
//
//								newNodeIdsMustBeRemoved.add(newNodeId);
//							}
//						}
//
//					}
//					destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
//					newNodeIdsMustBeRemoved.clear();
//
//					//
//					if (destPatternNode.newNodeIds.size() == 0) {
//						// if (DummyProperties.debugMode)
//						// System.out.println("expanded before from " +
//						// srcDataGpNodeId + " to " + newNodeId);
//						continue;
//					}
//
//					int destOutDegree = 1;
//					int outgoingToSameType = 1;
//
//					if (destPatternNode.goBackToPrev) {
//						// b1 or b3 a->b->d->b and a->b
//						destStepsFromRoot = Math.min(destStepsFromRoot, tempProcessingNodeData
//								.getStepsFromRootOfPatternNodes().get(destPatternNode.destPatternNode));
//
//						destOutDegree += tempProcessingNodeData.getPatternGraph()
//								.outDegreeOf(destPatternNode.destPatternNode);
//
//						for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
//								.outgoingEdgesOf(destPatternNode.destPatternNode)) {
//							if (tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
//									.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
//								outgoingToSameType++;
//							}
//						}
//					}
//
//					for (Integer newNodeId : destPatternNode.newNodeIds) {
//						if (destOutDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).outDegree) {
//							if (DummyProperties.debugMode) {
//								System.out
//										.println("cont: destInDegree:" + destOutDegree + " >  out degree in data graph:"
//												+ labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).outDegree);
//							}
//							newNodeIdsMustBeRemoved.add(newNodeId);
//						}
//					}
//
//					for (Integer newNodeId : destPatternNode.newNodeIds) {
//						if (outgoingToSameType > labelAdjacencyIndexer.distinctNodesOfDHopsAway
//								.get(newNodeId).get(srcPatternNode.getLabel()
//										+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
//								.get(Indexer.AFTER).size()) {
//							if (DummyProperties.debugMode) {
//								System.out.println("cont. outgoingToSameType: " + outgoingToSameType
//										+ " prev index type in data graph:"
//										+ labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
//												.get(srcPatternNode.getLabel()
//														+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
//														+ relationshipType)
//												.get(Indexer.AFTER).size());
//							}
//							newNodeIdsMustBeRemoved.add(newNodeId);
//						}
//					}
//
//					destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
//
//					if (destPatternNode.newNodeIds.size() == 0)
//						continue;
//
//					// Integer
//					// destDataGraphPatternNodeId =
//					// newNodeId;
//
//					// finding the new node type;
//					String newNodeType = null;
//
//					// if we've seen it in this pattern
//					// before...
//
//					newNodeType = destPatternNode.destPatternNode.getType();
//
//					LatticeNode<ILatticeNodeData> seenLatticeNode = null;
//					if (goBackToPrev) {
//						if (seenPatternNodeFromPreviousNodesForThisSrc.containsKey(destPatternNode)) {
//							seenLatticeNode = seenPatternNodeFromPreviousNodesForThisSrc.get(destPatternNode);
//						}
//					} else {
//						if (newlySeenPatternNodeForThisSrc.containsKey(
//								newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
//							// if this expansion has
//							// seen
//							// before
//							// add it to the group of
//							// that
//							// prefix-tree node
//							seenLatticeNode = newlySeenPatternNodeForThisSrc
//									.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
//						}
//					}
//
//					if (seenLatticeNode != null) {
//						// double start =
//						// System.nanoTime();
//
//						ILatticeNodeData latticeNodeData = seenLatticeNode.getData();
//
//						PatternNode tempDestPatternNode = seenLatticeNode.getData().getTargetPatternNode();
//
//						if (DummyProperties.debugMode) {
//							System.out.println("prev pattern seen:");
//							System.out.println(latticeNodeData.getMappedGraphString());
//						}
//
//						latticeNodeData.addNewMatchSet(tempDestPatternNode, destPatternNode.newNodeIds,
//								this.labelAdjacencyIndexer);
//
//						seenPatternNodeFromPreviousNodesForThisSrc.put(tempDestPatternNode, seenLatticeNode);
//
//						if (newCreatedOrTouchedPTNodes != null)
//							newCreatedOrTouchedPTNodes.add(seenLatticeNode.getData().getPatternLatticeNodeIndex());
//					} else {
//
//						// make a new pattern for SGI
//						// checking
//						// and add it as
//						// a new child if possible
//						ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
//								DefaultLabeledEdge.class);
//
//						for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
//							newPatternGraph.addVertex(patternNode);
//						}
//
//						for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
//							newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e),
//									e);
//						}
//
//						if (!newPatternGraph.vertexSet().contains(destPatternNode.destPatternNode)) {
//							newPatternGraph.addVertex(destPatternNode.destPatternNode);
//						}
//
//						newPatternGraph.addEdge(destPatternNode.destPatternNode, srcPatternNode,
//								new DefaultLabeledEdge(relationshipType));
//
//						// if
//						// (srcPatternNode.getLabel().equals("b")
//						// &&
//						// destPatternNode.getLabel().equals("d"))
//						// {
//						// System.out.println();
//						// }
//						for (Integer newNodeId : destPatternNode.newNodeIds) {
//							HashSet<Integer> nextNodesOfSrcType = labelAdjacencyIndexer.distinctNodesOfDHopsAway
//									.get(newNodeId).get(srcPatternNode.getLabel()
//											+ Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
//									.get(Indexer.AFTER);
//
//							int sameTypeOutgoingEdgeCnt = 0;
//							for (DefaultLabeledEdge e : newPatternGraph
//									.outgoingEdgesOf(destPatternNode.destPatternNode)) {
//								if (newPatternGraph.getEdgeTarget(e).getType().equals(srcPatternNode.getType())
//										&& e.getType().equals(relationshipType)) {
//									sameTypeOutgoingEdgeCnt++;
//								}
//							}
//							if (sameTypeOutgoingEdgeCnt > nextNodesOfSrcType.size()) {
//								if (DummyProperties.debugMode)
//									System.out.println(
//											"newPatternGraph.outDegreeOf(destPatternNode) > nextNodesOfSrcType.size()");
//								newNodeIdsMustBeRemoved.add(newNodeId);
//							}
//						}
//
//						destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
//
//						if (destPatternNode.newNodeIds.size() == 0)
//							continue;
//
//						// for all other same-level
//						// children
//						// of the prefix-tree:
//
//						addNewChildrenOrMatches2(tempProcessingNode, tempProcessingNodeData, newPatternGraph,
//								srcPatternNode, destPatternNode, srcDataGpNodeId, destPatternNode.newNodeIds,
//								newlySeenPatternNodeForThisSrc, newCreatedOrTouchedPTNodes, destStepsFromRoot,
//								relationshipType, seenPatternNodeFromPreviousNodesForThisSrc, snapshot, "incoming");
//					}
//				}
//				// }
//			}
//		}
//
//	}
//
//	private void outogingExpansion2(LatticeNode<ILatticeNodeData> tempProcessingNode,
//			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode, int matchGraphIndex,
//			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrc,
//			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrc,
//			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer srcDataGpNodeId, String otherNodeLabelRelType,
//			int snapshot) throws Exception {
//
//		HashMap<String, Integer> frequencyOfSameOutNeighborType = getFrequencyOfSameOutNeighborType(
//				tempProcessingNodeData, srcPatternNode);
//
//		HashSet<Integer> sameLabelNeighborNodes = labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(srcDataGpNodeId)
//				.get(otherNodeLabelRelType).get(Indexer.AFTER);
//
//		if (sameLabelNeighborNodes != null && sameLabelNeighborNodes.size() > 0) {
//			if (!frequencyOfSameOutNeighborType.containsKey(otherNodeLabelRelType)
//					|| frequencyOfSameOutNeighborType.get(otherNodeLabelRelType) < sameLabelNeighborNodes.size()) {
//				// we should add one same node label to this
//				// TODO: difference between focus nodes and
//				// non-focus nodes
//				int separatorIndex = otherNodeLabelRelType
//						.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
//				String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
//				String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);
//				Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode)
//						+ 1;
//				// for (Integer newNodeId : sameLabelNeighborNodes) {
//
//				ArrayList<GoBackToPrevHolder> destPatternNodes = new ArrayList<GoBackToPrevHolder>();
//				getDestPatternNodeAndCheckForGoBackToPrev2(destPatternNodes, tempProcessingNodeData, srcPatternNode,
//						srcDataGpNodeId, sameLabelNeighborNodes, destLabel, relationshipType, destStepsFromRoot);
//
//				if (DummyProperties.debugMode) {
//					System.out.println("matchGraphIndex: " + matchGraphIndex + ", srcNodeId:" + srcDataGpNodeId
//							+ ", newNodeId:" + sameLabelNeighborNodes);
//				}
//
//				for (int d = 0; d < destPatternNodes.size(); d++) {
//					GoBackToPrevHolder destPatternNode = destPatternNodes.get(d);
//					destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
//
//					HashSet<Integer> newNodeIdsMustBeRemoved = new HashSet<Integer>();
//					for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
//							.outgoingEdgesOf(srcPatternNode)) {
//
//						// if from this src I went to the
//						// target with the newNodeId
//						// and out degree of that is more
//						// than 0 which means that it
//						// verified before.
//						// or it has just that node we
//						// shouldn't expand to that node
//						// again
//						for (Integer newNodeId : destPatternNode.newNodeIds) {
//							if (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
//									.get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e)).contains(newNodeId)
//									&& (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
//											.get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e))
//											.size() == 1)) {
//								newNodeIdsMustBeRemoved.add(newNodeId);
//							}
//						}
//					}
//
//					destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
//					newNodeIdsMustBeRemoved.clear();
//
//					if (destPatternNode.newNodeIds.size() == 0) {
//						// if (DummyProperties.debugMode)
//						// System.out.println("expanded before from " +
//						// srcDataGpNodeId + " to " + newNodeIdsMustBeRemoved);
//						continue;
//					}
//
//					int destInDegree = 1;
//					int incomingFromSameType = 1;
//
//					if (destPatternNode.goBackToPrev) {
//						// b1 or b3 a->b->d->b and a->b
//						destStepsFromRoot = Math.min(destStepsFromRoot, tempProcessingNodeData
//								.getStepsFromRootOfPatternNodes().get(destPatternNode.destPatternNode));
//
//						destInDegree += tempProcessingNodeData.getPatternGraph()
//								.inDegreeOf(destPatternNode.destPatternNode);
//
//						for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
//								.incomingEdgesOf(destPatternNode.destPatternNode)) {
//							if (tempProcessingNodeData.getPatternGraph().getEdgeSource(e).getLabel()
//									.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
//								incomingFromSameType++;
//							}
//						}
//					}
//
//					for (Integer newNodeId : destPatternNode.newNodeIds) {
//						if (destInDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree) {
//							if (DummyProperties.debugMode) {
//								System.out.println("cont: destInDegree:" + destInDegree + " >  in degree in data graph:"
//										+ labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree);
//							}
//							newNodeIdsMustBeRemoved.add(newNodeId);
//						}
//					}
//					destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
//					newNodeIdsMustBeRemoved.clear();
//
//					for (Integer newNodeId : destPatternNode.newNodeIds) {
//						if (incomingFromSameType > labelAdjacencyIndexer.distinctNodesOfDHopsAway
//								.get(newNodeId).get(srcPatternNode.getLabel()
//										+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
//								.get(Indexer.BEFORE).size()) {
//							if (DummyProperties.debugMode) {
//								System.out.println("cont. incomingFromSameType: " + incomingFromSameType
//										+ " prev index type in data graph:"
//										+ labelAdjacencyIndexer.distinctNodesOfDHopsAway.get(newNodeId)
//												.get(srcPatternNode.getLabel()
//														+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
//														+ relationshipType)
//												.get(Indexer.BEFORE).size());
//							}
//							newNodeIdsMustBeRemoved.add(newNodeId);
//						}
//					}
//
//					destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
//					newNodeIdsMustBeRemoved.clear();
//
//					if (destPatternNode.newNodeIds.size() == 0)
//						continue;
//
//					// finding the new node type;
//					String newNodeType = null;
//
//					// if we've seen it in this pattern
//					// before...
//
//					newNodeType = destPatternNode.destPatternNode.getType();
//
//					LatticeNode<ILatticeNodeData> seenLatticeNode = null;
//					if (destPatternNode.goBackToPrev) {
//						if (seenPatternNodeFromPreviousNodesForThisSrc.containsKey(destPatternNode.destPatternNode)) {
//							seenLatticeNode = seenPatternNodeFromPreviousNodesForThisSrc
//									.get(destPatternNode.destPatternNode);
//						}
//					} else {
//						if (newlySeenPatternNodeForThisSrc.containsKey(
//								newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
//							// if this expansion has
//							// seen
//							// before
//							// add it to the group of
//							// that
//							// prefix-tree node
//							seenLatticeNode = newlySeenPatternNodeForThisSrc
//									.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
//						}
//					}
//
//					if (seenLatticeNode != null) {
//						// double start =
//						// System.nanoTime();
//
//						ILatticeNodeData latticeNodeData = seenLatticeNode.getData();
//
//						PatternNode tempDestPatternNode = seenLatticeNode.getData().getTargetPatternNode();
//
//						if (DummyProperties.debugMode) {
//							System.out.println("prev pattern seen:");
//							System.out.println(latticeNodeData.getMappedGraphString());
//						}
//
//						latticeNodeData.addNewMatchSet(tempDestPatternNode, destPatternNode.newNodeIds,
//								this.labelAdjacencyIndexer);
//
//						seenPatternNodeFromPreviousNodesForThisSrc.put(tempDestPatternNode, seenLatticeNode);
//
//						if (newCreatedOrTouchedPTNodes != null)
//							newCreatedOrTouchedPTNodes.add(seenLatticeNode.getData().getPatternLatticeNodeIndex());
//					} else {
//
//						// make a new pattern for SGI
//						// checking
//						// and add it as
//						// a new child if possible
//						ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
//								DefaultLabeledEdge.class);
//
//						for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
//							newPatternGraph.addVertex(patternNode);
//						}
//
//						for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
//							newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e),
//									e);
//						}
//
//						if (!newPatternGraph.vertexSet().contains(destPatternNode.destPatternNode)) {
//							newPatternGraph.addVertex(destPatternNode.destPatternNode);
//						}
//
//						newPatternGraph.addEdge(srcPatternNode, destPatternNode.destPatternNode,
//								new DefaultLabeledEdge(relationshipType));
//
//						for (Integer newNodeId : destPatternNode.newNodeIds) {
//							HashSet<Integer> prevNodesOfSrcType = labelAdjacencyIndexer.distinctNodesOfDHopsAway
//									.get(newNodeId).get(srcPatternNode.getLabel()
//											+ Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
//									.get(Indexer.BEFORE);
//
//							int sameTypeIncomingEdgeCnt = 0;
//							for (DefaultLabeledEdge e : newPatternGraph
//									.incomingEdgesOf(destPatternNode.destPatternNode)) {
//								if (newPatternGraph.getEdgeSource(e).getType().equals(srcPatternNode.getType())
//										&& e.getType().equals(relationshipType)) {
//									sameTypeIncomingEdgeCnt++;
//								}
//							}
//							if (sameTypeIncomingEdgeCnt > prevNodesOfSrcType.size()) {
//								if (DummyProperties.debugMode)
//									System.out.println(
//											"newPatternGraph.inDegreeOf(destPatternNode) > prevNodesOfSrcType.size()");
//								newNodeIdsMustBeRemoved.add(newNodeId);
//							}
//						}
//
//						if (destPatternNode.newNodeIds.size() == 0) {
//							continue;
//						}
//						// for all other same-level
//						// children
//						// of the prefix-tree:
//
//						addNewChildrenOrMatches2(tempProcessingNode, tempProcessingNodeData, newPatternGraph,
//								srcPatternNode, destPatternNode, srcDataGpNodeId, destPatternNode.newNodeIds,
//								newlySeenPatternNodeForThisSrc, newCreatedOrTouchedPTNodes, destStepsFromRoot,
//								relationshipType, seenPatternNodeFromPreviousNodesForThisSrc, snapshot, "outgoing");
//					}
//				}
//				// }
//			}
//		}
//
//	}
//
//	private void addNewChildrenOrMatches2(LatticeNode<ILatticeNodeData> tempProcessingNode,
//			ILatticeNodeData tempProcessingNodeData,
//			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph, PatternNode srcPatternNode,
//			GoBackToPrevHolder destPatternNode, Integer srcDataGpNodeId, HashSet<Integer> newNodeIds,
//			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrc,
//			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer destStepsFromRoot, String relationshipType,
//			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrc,
//			int snapshot, String origin) throws Exception {
//		boolean itWasIsomorphic = false;
//		if (tempProcessingNodeData.getLatticeMode() == LatticeMode.BATCH) {
//			for (LatticeNode<ILatticeNodeData> latticeSibling : sameLevelLatticeNodes) {
//				// if
//				// (latticeSibling.getData().getPatternLatticeNodeIndex()
//				// == 38) {
//				// System.out.println();
//				// }
//				if (preIsoChecking(latticeSibling.getData().getPatternGraph(), newPatternGraph)
//						&& getIsomorphism(newPatternGraph, latticeSibling.getData().getPatternGraph())
//								.isomorphismExists()) {
//
//					itWasIsomorphic = true;
//
//					if (DummyProperties.debugMode) {
//						System.out.println(
//								"BATCH: add node link from " + tempProcessingNode.getData().getPatternLatticeNodeIndex()
//										+ " to " + latticeSibling.getData().getPatternLatticeNodeIndex());
//					}
//					addIncNodeLink(tempProcessingNode, latticeSibling);
//
//					// }
//
//					break;
//					// so, this child
//					// doesn't
//					// need any
//					// from here.
//				}
//			}
//
//			if (!itWasIsomorphic && this.getLabelAdjacencyIndexer().latticeNodesOfALevel
//					.containsKey(tempProcessingNode.getLevel() + 1)) {
//
//				for (LatticeNode<ILatticeNodeData> latticeNode : this.getLabelAdjacencyIndexer().latticeNodesOfALevel
//						.get(tempProcessingNode.getLevel() + 1)) {
//
//					if (preIsoChecking(latticeNode.getData().getPatternGraph(), newPatternGraph)
//							&& getIsomorphism(newPatternGraph, latticeNode.getData().getPatternGraph())
//									.isomorphismExists()) {
//
//						if (DummyProperties.debugMode) {
//							System.out.println("BATCH: sameLevels: add node link from "
//									+ tempProcessingNode.getData().getPatternLatticeNodeIndex() + " to "
//									+ latticeNode.getData().getPatternLatticeNodeIndex());
//						}
//						itWasIsomorphic = true;
//						addIncNodeLink(tempProcessingNode, latticeNode);
//
//						// }
//
//						break;
//
//					}
//
//				}
//			}
//		} else {
//			// TODO:may be we can handle
//			// it
//			// without SGI
//			for (LatticeNode<ILatticeNodeData> child : tempProcessingNode.getChildren()) {
//				VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso;
//				if (preIsoChecking(child.getData().getPatternGraph(), newPatternGraph)
//						&& (iso = getIsomorphism(newPatternGraph, child.getData().getPatternGraph()))
//								.isomorphismExists()) {
//
//					ArrayList<PatternNode> destPatternNodesFromIso = new ArrayList<PatternNode>();
//
//					Iterator<GraphMapping<PatternNode, DefaultLabeledEdge>> mappingItr = iso.getMappings();
//
//					PatternNode tempSrcPatternNode = null;
//					while (mappingItr.hasNext()) {
//						GraphMapping<PatternNode, DefaultLabeledEdge> map = mappingItr.next();
//
//						tempSrcPatternNode = map.getVertexCorrespondence(srcPatternNode, true);
//						destPatternNodesFromIso.add(map.getVertexCorrespondence(destPatternNode.destPatternNode, true));
//
//						break;
//					}
//
//					if (destPatternNodesFromIso.size() == 0) {
//						if (Dummy.DummyProperties.debugMode) {
//							System.out.println("no dest pattern is found after a successful SGI for children nodes!!");
//						}
//						continue;
//					}
//
//					itWasIsomorphic = true;
//
//					for (PatternNode destPtn : destPatternNodesFromIso) {
//						// if
//						// (child.getData().getPatternLatticeNodeIndex()
//						// == 199) {
//						// System.out.println();
//						// }
//						child.getData().addNewMatchSetForUpdate(tempProcessingNode, tempSrcPatternNode, srcDataGpNodeId,
//								destPtn, newNodeIds, this.labelAdjacencyIndexer);
//					}
//
//					if (DummyProperties.debugMode) {
//						System.out.println(child.getData().getMappedGraphString());
//						System.out.println("new match for a child node!");
//					}
//					traversalQueue.add(child);
//					newCreatedOrTouchedPTNodes.add(child.getData().getPatternLatticeNodeIndex());
//					break;
//				}
//			}
//			if (!itWasIsomorphic && tempProcessingNode.getLinkedNodes() != null) {
//
//				for (LatticeNode<ILatticeNodeData> child : tempProcessingNode.getLinkedNodes()) {
//					VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso;
//					if (preIsoChecking(child.getData().getPatternGraph(), newPatternGraph)
//							&& (iso = getIsomorphism(newPatternGraph, child.getData().getPatternGraph()))
//									.isomorphismExists()) {
//
//						ArrayList<PatternNode> destPatternNodesFromIso = new ArrayList<PatternNode>();
//
//						Iterator<GraphMapping<PatternNode, DefaultLabeledEdge>> mappingItr = iso.getMappings();
//
//						PatternNode tempSrcPatternNode = null;
//						while (mappingItr.hasNext()) {
//							GraphMapping<PatternNode, DefaultLabeledEdge> map = mappingItr.next();
//
//							tempSrcPatternNode = map.getVertexCorrespondence(srcPatternNode, true);
//							destPatternNodesFromIso
//									.add(map.getVertexCorrespondence(destPatternNode.destPatternNode, true));
//
//							break;
//						}
//
//						// if
//						// (child.getData().getPatternLatticeNodeIndex()
//						// == 13
//						// &&
//						// destPatternNode.getLabel().equals("b"))
//						// {
//						// System.out.println();
//						// }
//
//						if (destPatternNodesFromIso.size() == 0) {
//							if (Dummy.DummyProperties.debugMode) {
//								System.out.println("no dest pattern is found after a successful SGI for linked nodes!");
//							}
//							continue;
//						}
//
//						itWasIsomorphic = true;
//
//						// if
//						// (tempProcessingNode
//						// .getData().getPatternLatticeNodeIndex()
//						// == 3) {
//						// System.out.println();
//						// }
//
//						for (PatternNode destPtn : destPatternNodesFromIso) {
//							child.getData().addNewMatchSetForUpdate(tempProcessingNode, tempSrcPatternNode,
//									srcDataGpNodeId, destPtn, newNodeIds, this.labelAdjacencyIndexer);
//						}
//
//						if (DummyProperties.debugMode)
//							System.out.println("new match for a linked node using SGI!");
//
//						traversalQueue.add(child);
//						newCreatedOrTouchedPTNodes.add(child.getData().getPatternLatticeNodeIndex());
//						break;
//					}
//				}
//			}
//
//			if (!itWasIsomorphic) {
//				// if
//				// (numberOfPatternsInLattice
//				// == 11) {
//				// System.out.println();
//				// }
//				int childLevel = tempProcessingNode.getLevel() + 1;
//				for (Integer ptIndex : latticeNodeIndex.keySet()) {
//					if (latticeNodeIndex.get(ptIndex).getLevel() == childLevel) {
//
//						if (tempProcessingNode.getChildren().contains(latticeNodeIndex.get(ptIndex)))
//							// it's
//							// checked
//							// before
//							continue;
//
//						if (tempProcessingNode.getLinkedNodes() != null
//								&& tempProcessingNode.getLinkedNodes().contains(latticeNodeIndex.get(ptIndex))) {
//							// it's
//							// checked
//							// before
//							continue;
//						}
//						VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso;
//						if (preIsoChecking(latticeNodeIndex.get(ptIndex).getData().getPatternGraph(), newPatternGraph)
//								&& (iso = getIsomorphism(newPatternGraph,
//										latticeNodeIndex.get(ptIndex).getData().getPatternGraph()))
//												.isomorphismExists()) {
//							itWasIsomorphic = true;
//
//							ArrayList<PatternNode> destPatternNodesFromIso = new ArrayList<PatternNode>();
//
//							// int steps
//							// =
//							// destStepsFromRoot;
//
//							Iterator<GraphMapping<PatternNode, DefaultLabeledEdge>> mappingItr = iso.getMappings();
//
//							PatternNode tempSrcPatternNode = null;
//							while (mappingItr.hasNext()) {
//								GraphMapping<PatternNode, DefaultLabeledEdge> map = mappingItr.next();
//
//								tempSrcPatternNode = map.getVertexCorrespondence(srcPatternNode, true);
//								destPatternNodesFromIso
//										.add(map.getVertexCorrespondence(destPatternNode.destPatternNode, true));
//
//								break;
//							}
//
//							for (PatternNode destPtn : destPatternNodesFromIso) {
//								latticeNodeIndex.get(ptIndex).getData().addNewMatchSetForUpdate(tempProcessingNode,
//										tempSrcPatternNode, srcDataGpNodeId, destPtn, newNodeIds,
//										this.labelAdjacencyIndexer);
//							}
//
//							addIncNodeLink(tempProcessingNode, latticeNodeIndex.get(ptIndex));
//
//							if (DummyProperties.debugMode)
//								System.out.println("new match for a SGI node in INC mode!");
//							traversalQueue.add(latticeNodeIndex.get(ptIndex));
//							newCreatedOrTouchedPTNodes
//									.add(latticeNodeIndex.get(ptIndex).getData().getPatternLatticeNodeIndex());
//
//							break;
//						}
//
//					}
//				}
//
//			}
//		}
//
//		if (!itWasIsomorphic) {
//			// System.out.println("origin:" + origin);
//			LatticeNode<ILatticeNodeData> newChild = createNewLatticeNode(tempProcessingNode, newPatternGraph,
//					srcPatternNode, destPatternNode.destPatternNode, srcDataGpNodeId, newNodeIds,
//					newCreatedOrTouchedPTNodes, relationshipType, destStepsFromRoot,
//					tempProcessingNode.getData().getLatticeMode() == LatticeMode.UPDATE ? !FRESH_SOURCE : FRESH_SOURCE,
//					snapshot);
//
//			if (!destPatternNode.goBackToPrev)
//				newlySeenPatternNodeForThisSrc.put(destPatternNode.destPatternNode.getType()
//						+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType, newChild);
//			else
//				seenPatternNodeFromPreviousNodesForThisSrc.put(destPatternNode.destPatternNode, newChild);
//
//			sameLevelLatticeNodes.add(newChild);
//
//		}
//
//	}
//
//	private HashMap<String, Integer> getFrequencyOfSameOutNeighborType(ILatticeNodeData tempProcessingNodeData,
//			PatternNode srcPatternNode) {
//		HashMap<String, Integer> frequencyOfSameOutNeighborType = new HashMap<String, Integer>();
//		for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph().outgoingEdgesOf(srcPatternNode)) {
//
//			frequencyOfSameOutNeighborType
//					.putIfAbsent(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
//							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType(), 0);
//
//			frequencyOfSameOutNeighborType.put(
//					tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
//							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType(),
//					frequencyOfSameOutNeighborType
//							.get(tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
//									+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
//							+ 1);
//		}
//		return frequencyOfSameOutNeighborType;
//	}
//
//	private HashMap<String, Integer> getFrequencyOfSameInNeighborType(ILatticeNodeData tempProcessingNodeData,
//			PatternNode srcPatternNode) {
//		HashMap<String, Integer> frequencyOfSameInNeighborType = new HashMap<String, Integer>();
//		for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph().incomingEdgesOf(srcPatternNode)) {
//
//			frequencyOfSameInNeighborType
//					.putIfAbsent(tempProcessingNodeData.getPatternGraph().getEdgeSource(e).getLabel()
//							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType(), 0);
//
//			frequencyOfSameInNeighborType.put(
//					tempProcessingNodeData.getPatternGraph().getEdgeSource(e).getLabel()
//							+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType(),
//					frequencyOfSameInNeighborType
//							.get(tempProcessingNodeData.getPatternGraph().getEdgeSource(e).getLabel()
//									+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
//							+ 1);
//		}
//		return frequencyOfSameInNeighborType;
//	}
//
//	private LatticeNode<ILatticeNodeData> createNewLatticeNode(LatticeNode<ILatticeNodeData> tempProcessingNode,
//			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph, PatternNode srcPatternNode,
//			PatternNode destPatternNode, Integer srcDataGpNodeId, HashSet<Integer> newNodeIds,
//			HashSet<Integer> newCreatedOrTouchedPTNodes, String relationshipType, Integer destStepsFromRoot,
//			boolean freshSource, int snapshot) {
//
//		creationOfNewLatticeNodeStart = System.nanoTime();
//
//		if (destPatternNode.isFocus()) {
//			destStepsFromRoot = 0;
//		}
//
//		ILatticeNodeData latticeNodeData = new LatticeNodeDataAlg1(newPatternGraph,
//				tempProcessingNode.getData().getPatternRootNode(), tempProcessingNode.getData(),
//				tempProcessingNode.getData().getMatchedNodes(), srcPatternNode, destPatternNode, srcDataGpNodeId,
//				newNodeIds, numberOfPatternsInLattice, relationshipType, destStepsFromRoot, freshSource, snapshot,
//				this.labelAdjacencyIndexer);
//
//		LatticeNode<ILatticeNodeData> newChild = new LatticeNode<ILatticeNodeData>(latticeNodeData);
//		// if (numberOfPatternsInLattice == 40) {
//		// System.out.println();
//		// }
//		latticeNodeIndex.put(numberOfPatternsInLattice++, newChild);
//		tempProcessingNode.addChild(newChild);
//		traversalQueue.add(newChild);
//
//		if (newCreatedOrTouchedPTNodes != null)
//			newCreatedOrTouchedPTNodes.add(newChild.getData().getPatternLatticeNodeIndex());
//
//		if (DummyProperties.debugMode) {
//			// if (newChild.getData().getPatternLatticeNodeIndex() == 7) {
//			// System.out.println("");
//			// }
//
//			System.out.println("newChild:" + newChild.getLevel() + " edgeSet size: "
//					+ newChild.getData().getPatternGraph().edgeSet().size());
//			System.out.println(newChild.getData().getMappedGraphString());
//		}
//
//		this.getLabelAdjacencyIndexer().latticeNodesOfALevel.putIfAbsent(newChild.getLevel(),
//				new HashSet<LatticeNode<ILatticeNodeData>>());
//		this.getLabelAdjacencyIndexer().latticeNodesOfALevel.get(newChild.getLevel()).add(newChild);
//
//		creationOfNewLatticeNodeDuration += ((System.nanoTime() - creationOfNewLatticeNodeStart) / 1e6);
//
//		return newChild;
//
//	}
//
//	private void getDestPatternNodeAndCheckForGoBackToPrevIncoming2(ArrayList<GoBackToPrevHolder> destPatternNodes,
//			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode, Integer srcDataGpNodeId,
//			HashSet<Integer> newNodeIds, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {
//
//		HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
//		remainingNodeIds.addAll(newNodeIds);
//		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
//			if ((patternNode != srcPatternNode)
//					&& (!tempProcessingNodeData.getPatternGraph().containsEdge(patternNode, srcPatternNode))) {
//				HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
//				for (Integer newNodeId : newNodeIds) {
//					if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
//							.contains(newNodeId)) {
//						goBackToPrevNodeIds.add(newNodeId);
//					}
//				}
//				if (goBackToPrevNodeIds.size() > 0) {
//					destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, patternNode));
//				}
//
//				remainingNodeIds.removeAll(goBackToPrevNodeIds);
//			}
//		}
//		if (remainingNodeIds.size() > 0 && newNodeIds.contains(srcDataGpNodeId)) {
//			// handling self-loop
//			HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
//			goBackToPrevNodeIds.add(srcDataGpNodeId);
//			remainingNodeIds.remove(srcDataGpNodeId);
//			destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, srcPatternNode));
//		}
//
//		if (remainingNodeIds.size() > 0 && tempProcessingNodeData.getTypeOfUnSeenFocusNodes() != null) {
//			// if we can find another focus
//			// node, if
//			// anything
//			// remaining to find.
//			for (String type : tempProcessingNodeData.getTypeOfUnSeenFocusNodes()) {
//				HashSet<Integer> newNodeIdForTheType = new HashSet<Integer>();
//				SetView<Integer> interesected = Sets.intersection(allNodesOfFocusType.get(type), newNodeIds);
//				newNodeIdForTheType.addAll(interesected);
//				remainingNodeIds.removeAll(interesected);
//				if (newNodeIdForTheType.size() > 0) {
//					destPatternNodes.add(new GoBackToPrevHolder(newNodeIdForTheType, false,
//							new PatternNode(otherNodeLabel, The_Focus_Node)));
//				}
//			}
//		}
//		// if we already found all the focus
//		// nodes, all this
//		// labels is not in our focus list
//		if (remainingNodeIds.size() > 0) {
//			destPatternNodes.add(new GoBackToPrevHolder(remainingNodeIds, false, new PatternNode(otherNodeLabel)));
//		}
//
//	}
//
//	private void getDestPatternNodeAndCheckForGoBackToPrev2(ArrayList<GoBackToPrevHolder> destPatternNodes,
//			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode, Integer srcDataGpNodeId,
//			HashSet<Integer> newNodeIds, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {
//
//		// PatternNode destPatternNode = null;
//		// goBackToPrev = false;
//		HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
//		remainingNodeIds.addAll(newNodeIds);
//		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
//			if ((patternNode != srcPatternNode)
//					&& (!tempProcessingNodeData.getPatternGraph().containsEdge(srcPatternNode, patternNode))) {
//
//				HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
//				for (Integer newNodeId : newNodeIds) {
//					if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
//							.contains(newNodeId)) {
//						goBackToPrevNodeIds.add(newNodeId);
//					}
//				}
//
//				if (goBackToPrevNodeIds.size() > 0) {
//					destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, patternNode));
//				}
//
//				remainingNodeIds.removeAll(goBackToPrevNodeIds);
//			}
//		}
//		if (remainingNodeIds.size() > 0 && newNodeIds.contains(srcDataGpNodeId)) {
//			// handling self-loop
//			HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
//			goBackToPrevNodeIds.add(srcDataGpNodeId);
//			remainingNodeIds.remove(srcDataGpNodeId);
//			destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, srcPatternNode));
//
//		}
//
//		if (remainingNodeIds.size() > 0 && tempProcessingNodeData.getTypeOfUnSeenFocusNodes() != null) {
//			// if we can find another focus
//			// node, if
//			// anything
//			// remaining to find.
//
//			for (String type : tempProcessingNodeData.getTypeOfUnSeenFocusNodes()) {
//				HashSet<Integer> newNodeIdForTheType = new HashSet<Integer>();
//				SetView<Integer> interesected = Sets.intersection(allNodesOfFocusType.get(type), newNodeIds);
//				newNodeIdForTheType.addAll(interesected);
//				remainingNodeIds.removeAll(interesected);
//				if (newNodeIdForTheType.size() > 0) {
//					destPatternNodes.add(new GoBackToPrevHolder(newNodeIdForTheType, false,
//							new PatternNode(otherNodeLabel, The_Focus_Node)));
//				}
//			}
//		}
//
//		// if we already found all the focus
//		// nodes, all this
//		// labels is not in our focus list
//		if (remainingNodeIds.size() > 0) {
//			destPatternNodes.add(new GoBackToPrevHolder(remainingNodeIds, false, new PatternNode(otherNodeLabel)));
//		}
//
//	}
//
//	private void print(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {
//		ArrayList<String> absGraphEdges = new ArrayList<String>();
//		String returnValue = "";
//
//		for (DefaultLabeledEdge e : newAbsPattern.edgeSet()) {
//			absGraphEdges.add((newAbsPattern.getEdgeSource(e).getType())
//					// + (newAbsPattern.getEdgeSource(e).isFocus() ? "*" : "")
//					+ "->" + (newAbsPattern.getEdgeTarget(e).getType())
//					// + (newAbsPattern.getEdgeTarget(e).isFocus() ? "*" : "")
//					+ ", ");
//		}
//		Collections.sort(absGraphEdges);
//
//		for (String v : absGraphEdges) {
//			returnValue += v;
//		}
//
//		if (DummyProperties.debugMode)
//			System.out.println(returnValue);
//
//	}
//
//	public boolean preIsoChecking(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
//			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {
//		numberOfIsoCheckingRequest++;
//		isoTimeStart = System.nanoTime();
//		// SHAYAN
//		// TODO: finalize it
//		// if two patterns don't have same number of nodes?
//		if (abstractPatternGraph.vertexSet().size() != newAbsPattern.vertexSet().size()) {
//			isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
//			return false;
//		}
//
//		// if two patterns don't have same number of edges?
//		if (abstractPatternGraph.edgeSet().size() != newAbsPattern.edgeSet().size()) {
//			isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
//			return false;
//		}
//
//		// TODO: degree-distribution & label distribution checking
//		// before isomorphism checking
//
//		// if they don't have same label distribution?
//
//		// .....
//		isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
//		return true;
//
//	}
//
//	public VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsomorphism(
//			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
//			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {
//
//		return getIsoChecker(abstractPatternGraph, newAbsPattern);
//
//	}
//
//	private VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsoChecker(
//			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
//			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {
//
//		isoTimeStart = System.nanoTime();
//
//		VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = new VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge>(
//				abstractPatternGraph, newAbsPattern, new Comparator<PatternNode>() {
//
//					@Override
//					public int compare(PatternNode v1, PatternNode v2) {
//						// ??
//						if (v1.getType().equals(v2.getType()) && v1.isFocus() == v2.isFocus())
//							return 0;
//
//						return 1;
//					}
//
//				}, new Comparator<DefaultLabeledEdge>() {
//
//					@Override
//					public int compare(DefaultLabeledEdge e1, DefaultLabeledEdge e2) {
//						if (e1.getType().equals(e2.getType()))
//							return 0;
//
//						return 1;
//					}
//				});
//
//		isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
//		numberOfRealIsoChecking++;
//
//		return iso;
//
//	}
//
//	//
//	private void addIncNodeLink(LatticeNode<ILatticeNodeData> latticeNode, LatticeNode<ILatticeNodeData> temp)
//			throws Exception {
//
//		if (!latticeNode.getChildren().contains(temp)
//				&& (latticeNode.getLinkedNodes() == null || !latticeNode.getLinkedNodes().contains(temp))) {
//			if (DummyProperties.debugMode) {
//				System.out.println("INC: add node link from " + latticeNode.getData().getPatternLatticeNodeIndex()
//						+ " to " + temp.getData().getPatternLatticeNodeIndex());
//			}
//			latticeNode.addNodeLink(temp);
//		}
//
//		// because latticeNode right now has at least one
//		// child
//		if (latticeNode.getData().isMaximalFrequent() && temp.getData().isFrequent()) {
//			latticeNode.getData().setCanBeMaximalFrequent(false);
//			latticeNode.getData().setMaximalFrequent(false, latticeNode, this);
//			mfpLatticeNodes.remove(latticeNode);
//			latticeNode.getData().removeFromTopK(this, latticeNode);
//		}
//
//	}
//
//	// TODO: may be better to find invalid prefix nodes at the time that we
//	// shrink them or add new matches to them
//	// not at the time that we want to expand them
//	private boolean checkValidityAtLeastOneMatchForEachPatternNode(
//			LatticeNode<ILatticeNodeData> thePTNodeBaseOnTheNewEdge) {
//
//		checkValidityAtLeastOneMatchForEachPatternNodeStartTime = System.nanoTime();
//
//		boolean isValid = true;
//		for (PatternNode patternNode : thePTNodeBaseOnTheNewEdge.getData().getMatchedNodes()
//				.getDataGraphMatchNodeOfAbsPNode().keySet()) {
//			if (thePTNodeBaseOnTheNewEdge.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
//					.size() < 1) {
//				isValid = false;
//				break;
//			}
//		}
//		checkValidityAtLeastOneMatchForEachPatternNodeDuration += ((System.nanoTime()
//				- checkValidityAtLeastOneMatchForEachPatternNodeStartTime) / 1e6);
//		return isValid;
//	}
//
//	// TODO: may be we have to remove some from top-k even if they are valid
//	// yet.
//	public void shrinkForNewDeletedEdge(Node srcNode, Node destNode, boolean srcIsAFocus, boolean destIsAFocus,
//			LatticeNode<ILatticeNodeData> latticeNode, HashSet<Integer> newCreatedOrTouchedPTNodes, int snapshot,
//			double threshold, MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>> topKFrequentPatterns,
//			String relationshipType) throws Exception {
//
//		// queue for processing this node and all of it's children
//		Queue<LatticeNode<ILatticeNodeData>> shrinkageQueue = new LinkedList<LatticeNode<ILatticeNodeData>>();
//		shrinkageQueue.add(latticeNode);
//
//		Integer srcNodeId = (int) srcNode.getId();
//		Integer destNodeId = (int) destNode.getId();
//		String destLabel = destNode.getLabels().iterator().next().name();
//		String sourceLabel = srcNode.getLabels().iterator().next().name();
//
//		if (DummyProperties.debugMode) {
//			System.out.println("srcNodeId: " + srcNodeId + ", destNodeId: " + destNodeId + ", destLabel:" + destLabel);
//		}
//
//		while (!shrinkageQueue.isEmpty()) {
//
//			// get any affected prefixtree node (desendants of the main pt node)
//			LatticeNode<ILatticeNodeData> thisNode = shrinkageQueue.poll();
//
//			// if we checked it before we just move on
//			if (newCreatedOrTouchedPTNodes.contains(thisNode.getData().getPatternLatticeNodeIndex())) {
//				continue;
//			}
//
//			// we should add it as a seen case for this edge deletion
//			newCreatedOrTouchedPTNodes.add(thisNode.getData().getPatternLatticeNodeIndex());
//			if (DummyProperties.debugMode) {
//				System.out.println("DEL-Affected: Before remove: " + thisNode.getData());
//			}
//
//			// commented becasue if I make it invalid then I don't know if it
//			// was mfp or not
//			// downward property
//			// if (!thisNode.getParent().getData().isValid()) {
//			// thisNode.getData().setPatternAsInvalid(thisNode, this, snapshot);
//			// }
//
//			// we should remove the corresponding destNodeId in the
//			// destPatternNode
//			if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(srcNodeId) == null) {
//				if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) != null) {
//					Iterator<PatternNode> destIterator = thisNode.getData().getMatchedNodes()
//							.getPatternNodeOfNeo4jNode().get(destNodeId).iterator();
//
//					while (destIterator.hasNext()) {
//						PatternNode destPatternNode = destIterator.next();
//						// if
//						// (!thisNode.getData().getPatternGraph().vertexSet().contains(destPatternNode))
//						// {
//						// System.out.println();
//						// }
//						for (DefaultLabeledEdge e : thisNode.getData().getPatternGraph()
//								.incomingEdgesOf(destPatternNode)) {
//							if (thisNode.getData().getPatternGraph().getEdgeSource(e).getLabel().equals(sourceLabel)
//									&& e.getType().equals(relationshipType)) {
//								removeDestNodeForDelete(destIterator, thisNode, destNodeId, destPatternNode,
//										topKFrequentPatterns, snapshot, threshold, relationshipType);
//								break;
//							}
//						}
//					}
//				}
//			} else {
//				HashSet<PatternNode> srcPatternNodes = new HashSet<PatternNode>(
//						thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(srcNodeId));
//
//				for (PatternNode srcPatternNode : srcPatternNodes) {
//
//					if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) != null) {
//						Iterator<PatternNode> destIterator = thisNode.getData().getMatchedNodes()
//								.getPatternNodeOfNeo4jNode().get(destNodeId).iterator();
//
//						// TODO:it's a patch
//						HashSet<PatternNode> patternNodesToIterate = new HashSet<PatternNode>();
//						patternNodesToIterate.addAll(
//								thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId));
//
//						for (PatternNode destPatternNode : patternNodesToIterate) {
//
//							// A->B->C->B
//							if (!thisNode.getData().getPatternGraph().containsEdge(srcPatternNode, destPatternNode)) {
//								if (DummyProperties.debugMode)
//									System.out.println("this case is happened!");
//								continue;
//							}
//
//							removeDestNodeForDelete(destIterator, thisNode, destNodeId, destPatternNode,
//									topKFrequentPatterns, snapshot, threshold, relationshipType);
//
//						}
//					}
//				}
//			}
//
//			for (LatticeNode<ILatticeNodeData> childNode : thisNode.getChildren()) {
//				// if (childNode.getData().isValid())
//				shrinkageQueue.add(childNode);
//
//			}
//
//			if (thisNode.getLinkedNodes() != null)
//				for (LatticeNode<ILatticeNodeData> linkedNode : thisNode.getLinkedNodes()) {
//					// if (linkedNode.getData().isValid())
//					shrinkageQueue.add(linkedNode);
//				}
//			if (DummyProperties.debugMode) {
//				System.out.println("DEL-Affected: After remove: " + thisNode.getData());
//			}
//		}
//	}
//
//	private void removeDestNodeForDelete(Iterator<PatternNode> destIterator, LatticeNode<ILatticeNodeData> thisNode,
//			Integer destNodeId, PatternNode destPatternNode,
//			MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>> topKFrequentPatterns, int snapshot, double threshold,
//			String relationshipType) throws Exception {
//
//		try {// TODO: it's a patch
//			destIterator.remove();
//		} catch (Exception exc) {
//			return;
//		}
//
//		thisNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(destPatternNode).remove(destNodeId);
//
//		// System.out.println("remove destNodeId: " + destNodeId + ",
//		// destPatternNode: " + destPatternNode
//		// + " patternLatticeNodeIndex:" +
//		// thisNode.getData().getPatternLatticeNodeIndex());
//
//		if (thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId) == null
//				|| thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(destNodeId).size() == 0) {
//			thisNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().remove(destNodeId);
//			bitmap.removeNodeIdFromPatternId(destNodeId, thisNode.getData().getPatternLatticeNodeIndex());
//		}
//
//		// we don't remove "destPatternNode",
//		// we just remove its corresponding node ids
//
//		boolean wasMFP = thisNode.getData().isMaximalFrequent();
//		boolean wasFrequent = thisNode.getData().isFrequent();
//		double prevTotalSupp = thisNode.getData().getTotalSupportFrequency();
//
//		// if it's not valid we don't need to do anything further because we
//		// already did!
//		if (!thisNode.getData().isValid()) {
//			return;
//		}
//
//		else if (thisNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(destPatternNode)
//				.size() == 0) {
//			// if it's invalid all it's children cannot have
//			// support greater than zero
//			thisNode.getData().setPatternAsInvalid(thisNode, this, snapshot);
//
//			if (wasMFP) {
//				thisNode.getData().maxFreqToNonFreqHandling(thisNode, this, snapshot);
//			} else if (wasFrequent) {
//				thisNode.getData().freqToNonFreqHandling(thisNode);
//			}
//
//		} else {
//
//			// thisNode.getData().setPatternAsUnEvaluated();
//
//			double lowerbound = 0;
//			if (topKFrequentPatterns.size() > 0) {
//				if (topKFrequentPatterns.peekLast().getData() == null) {
//					System.err.println("in shrink: topKFrequentPatterns.peekLast().getData() == null");
//					topKFrequentPatterns.removeLast();
//				} else {
//					lowerbound = topKFrequentPatterns.peekLast().getData().getTotalSupportFrequency();
//				}
//			}
//
//			DualSimulationHandler.computeSupport(dataGraph, thisNode, snapshot, this);
//
//			// if support didn't change, we can easily return
//			if (prevTotalSupp == thisNode.getData().getTotalSupportFrequency()) {
//				return;
//			}
//
//			if (wasFrequent) {
//
//				// estimateUpperbound(thisNode.getParent(), thisNode,
//				// allNodesOfFocusType, destPatternNode,
//				// relationshipType, snapshot);
//
//				// if it has the potential to be a mfp pattern
//
//				// if it cannot be a frequent pattern
//				if (thisNode.getData().getTotalSupportFrequency() < threshold) {
//
//					if (wasMFP)
//						thisNode.getData().maxFreqToNonFreqHandling(thisNode, this, snapshot);
//					else
//						thisNode.getData().freqToNonFreqHandling(thisNode);
//
//				}
//
//				// if it had the potential to be inside of the topk
//				// and it's frequent yet
//				else if (wasMFP && thisNode.getData().getTotalSupportFrequency() < lowerbound) {
//
//					// so, as it's a MFP and it's freq yet,
//					// nothing will be changed about it.
//					// just it should be removed from top-k
//					thisNode.getData().removeFromTopK(this, thisNode);
//
//				} else if (wasMFP) {
//
//					if (!thisNode.getData().isValid() || thisNode.getData().getTotalSupportFrequency() < threshold) {
//						// it cannot be maximal frequent
//						// and we should find one mfp in its ancestors
//						thisNode.getData().maxFreqToNonFreqHandling(thisNode, this, snapshot);
//					}
//					// it can be mfp but it cannot be inside of topk
//					else if (thisNode.getData().getTotalSupportFrequency() < lowerbound) {
//						thisNode.getData().removeFromTopK(this, thisNode);
//					}
//
//				}
//			}
//
//		}
//	}
//
//	public int bfsTraverse(LatticeNode<ILatticeNodeData> rootNode) {
//		if (DummyProperties.debugMode)
//			System.out.println("starting lattice BFS Traversal");
//		Queue<LatticeNode<ILatticeNodeData>> bfsQueue = new LinkedList<LatticeNode<ILatticeNodeData>>();
//		bfsQueue.add(rootNode);
//		int cnt = 0;
//		while (!bfsQueue.isEmpty()) {
//			LatticeNode<ILatticeNodeData> queueNode = bfsQueue.poll();
//			cnt++; // root count included
//			for (int i = 0; i < queueNode.getChildren().size(); i++) {
//				bfsQueue.add(queueNode.getChildren().get(i));
//			}
//
//			numberOfTotalAllMatches += queueNode.getData().getNumerOfAllMatches();
//			if (DummyProperties.debugMode) {
//				System.out.println(queueNode.getData().getMappedGraphString());
//				System.out.print("isMaximalFrequent: " + queueNode.getData().isMaximalFrequent() + ", ");
//				System.out.print("isFrequent: " + queueNode.getData().isFrequent() + ", ");
//				System.out.print("isValid: " + queueNode.getData().isValid() + ", ");
//				System.out.print("isCorrect: " + queueNode.getData().isCorrect() + ", ");
//				System.out.println("isVisited: " + queueNode.getData().isVisited());
//				System.out.println("totalSup: " + queueNode.getData().getTotalSupportFrequency());
//				System.out.println();
//
//			}
//		}
//		if (DummyProperties.debugMode) {
//			System.out.println("number of traversed nodes: " + cnt);
//			System.out.println();
//		}
//
//		return cnt;
//
//	}
//
////	private void printTreeDualSim(LatticeNode<ILatticeNodeData> rootNode, GraphDatabaseService graphdb,
////			LatticeAlg1 lattice) {
////		Queue<LatticeNode<ILatticeNodeData>> bfsQueue = new LinkedList<LatticeNode<ILatticeNodeData>>();
////		bfsQueue.add(rootNode);
////		Map<PatternNode, HashSet<Integer>> dsim;
////		while (!bfsQueue.isEmpty()) {
////			LatticeNode<ILatticeNodeData> queueNode = bfsQueue.poll();
////
////			for (int i = 0; i < queueNode.getChildren().size(); i++) {
////				bfsQueue.add(queueNode.getChildren().get(i));
////			}
////
////			if (queueNode.getData().getPatternGraph() != null) {
////				dsim = BatDualSimulation.run(graphdb, queueNode.getData().getPatternGraph(), lattice);
////				print(queueNode.getData().getPatternGraph());
////				System.out.print("{");
////				for (PatternNode patternNode : dsim.keySet()) {
////					System.out.print(patternNode.getType() + "=[");
////					for (Integer nodeId : dsim.get(patternNode)) {
////						System.out.print(nodeId + ",");
////					}
////					System.out.print("]");
////				}
////				System.out.println("}");
////				System.out.println();
////			}
////			// System.out.println(queueNode.getData().getMappedGraphString());
////
////		}
////
////	}
//
//	@Override
//	public MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>> getTopKFrequentPatterns() {
//		return this.topKFrequentPatterns;
//	}
//
//	public PriorityQueue<LatticeNode<ILatticeNodeData>> getMfpLatticeNodes() {
//		return this.mfpLatticeNodes;
//	}
//
//	public HashMap<Integer, LatticeNode<ILatticeNodeData>> getLatticeNodeIndex() {
//		return this.latticeNodeIndex;
//	}
//
//	@Override
//	public Indexer getLabelAdjacencyIndexer() {
//		return this.labelAdjacencyIndexer;
//	}
//
//	@Override
//	public GraphDatabaseService getDataGraph() {
//		return this.dataGraph;
//	}
//
//	@Override
//	public double getThreshold() {
//		return -1d;
//	}
//
//	@Override
//	public Bitmap getBitmap() {
//		return this.bitmap;
//	}
//
//	@Override
//	public PriorityQueue<LatticeNode<ILatticeNodeData>> getMipLatticeNodes() {
//		return mipLatticeNodes;
//	}
//
//	@Override
//	public double getDurationOfIsoChecking() {
//		return isoTimeDuration;
//	}
//
//	@Override
//	public double getDurationOfNewLatticeGeneration() {
//		return creationOfNewLatticeNodeDuration;
//	}
//
//	@Override
//	public int getNumberOfComputeSupport() {
//		return numberOfComputeSupport;
//	}
//
//	@Override
//	public double getDurationOfComputeSupport() {
//		return computeSupportDuration;
//	}
//
//	@Override
//	public void incNumberOfComputeSupport() {
//		numberOfComputeSupport++;
//	}
//
//	@Override
//	public void updateDurationOfComputeSupport(double newDuration) {
//		computeSupportDuration += newDuration;
//	}
//
//	@Override
//	public void resetNumberOfIsoChecking() {
//		numberOfIsoCheckingRequest = 0;
//		numberOfRealIsoChecking = 0;
//
//	}
//
//	@Override
//	public void resetDurationOfIsoChecking() {
//		isoTimeDuration = 0;
//
//	}
//
//	public void resetNumberOfComputeSupport() {
//		numberOfComputeSupport = 0;
//
//	}
//
//	public void resetDurationOfComputeSupport() {
//		computeSupportDuration = 0;
//
//	}
//
//	public void resetDurationOfNewLatticeGeneration() {
//		creationOfNewLatticeNodeDuration = 0;
//	}
//
//	@Override
//	public double getDurationOfBiSimChecking() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public long getNumberOfIsoCheckingRequest() {
//		return numberOfIsoCheckingRequest;
//	}
//
//	@Override
//	public long getNumberOfRealIsoChecking() {
//		return numberOfRealIsoChecking;
//	}
//
//	@Override
//	public long getNumberOfBiSimCheckingRequest() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public long getNumberOfRealBiSimChecking() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public void incrementBiSimCheckingRequest() {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void incrementRealBiSimChecking() {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void updateDurationOfBiSimChecking(double newDuration) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void resetDurationOfBiSimChecking() {
//
//	}
//}
//
//class PairStrings {
//	public String key;
//	public String value;
//
//	public PairStrings(String key, String value) {
//		this.key = key;
//		this.value = value;
//
//	}
//
//}