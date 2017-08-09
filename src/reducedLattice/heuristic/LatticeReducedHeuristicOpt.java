package reducedLattice.heuristic;

import java.io.*;
import java.util.*;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import base.ILattice;
import base.ILatticeNodeData;
import utilities.CumulativeRulesInfo;
import utilities.DebugHelper;
import utilities.DefaultLabeledEdge;
import utilities.DualSimulationHandler;
import utilities.Dummy;
import utilities.Dummy.DummyFunctions;
import utilities.Dummy.DummyProperties;
import utilities.Dummy.DummyProperties.Direction;
import utilities.Dummy.DummyProperties.ExpansionSide;
import utilities.GoBackToPrevHolder;
import utilities.Indexer;
import utilities.LHSQueueComparator;
import utilities.LatticeNode;
import utilities.PatternNode;
import utilities.Rule;

public class LatticeReducedHeuristicOpt implements ILattice {
	public int fixedLHSCnt = 0;
	public int rhsTrials = 0;
	public int intersectionPruning = 0;
	public int totalStopDueToSupportOrConfidenceThreshold = 0;
	public int numberOfGtarVerification = 0;

	public double supportThreshold;
	public double confidenceThreshold;
	int cntVisualization = 0;
	private HashMap<String, ArrayList<PairStrings>> focusLabelPropValSet = new HashMap<String, ArrayList<PairStrings>>();
	private int maxAllowedHops;
	private int maxAllowedEdges;
	private String dataGraphPath;
	GraphDatabaseService dataGraph;
	public static final boolean The_Focus_Node = true;
	public static final boolean FRESH_SOURCE = true;
	public String whatIsFocus = "";

	// when we initialize a new child, we should add it here also
	public HashMap<Integer, LatticeNode<ILatticeNodeData>> latticeNodeIndex = new HashMap<Integer, LatticeNode<ILatticeNodeData>>();
	// public HashMap<Integer, BiSimGroup> biSimGroupIndex = new
	// HashMap<Integer, BiSimGroup>();

	public int numberOfPatternsInLattice = 0;
	public int numberOfBiSimGroupsInLattice = 0;

	// assumption: user give different types as focus.
	// assumption: user can give property key values to just select some of the
	// node with same type
	// String: should be nodeType and then all focus node candidates.
	public HashMap<String, HashSet<Integer>> allNodesOfFocusType = new HashMap<String, HashSet<Integer>>();
	private HashSet<String> focusLabelSet = new HashSet<String>();
	private boolean debugMode = false;
	String focusSetPath = null;
	public Indexer labelAdjacencyIndexer;

	// a queue for processing all the waiting new PT nodes.
	PriorityQueue<LatticeNode<ILatticeNodeData>> lhsQueue;
	HashSet<LatticeNode<ILatticeNodeData>> lhsQueueSet = new HashSet<LatticeNode<ILatticeNodeData>>();

	// maintain the same node level for graph isomorphism checking
	// ArrayList<LatticeNode<ILatticeNodeData>> sameLevelLatticeNodes;

	public LatticeNode<ILatticeNodeData> emptyPTRootNode = null;
	boolean goBackToPrev = false;

	public int numberOfAllFocusNodes = 0;
	public int numberOfTotalAllMatches = 0;

	public int numberOfRealIsoChecking = 0;
	public int numberOfIsoCheckingRequest = 0;

	private double isoTimeStart = 0d;
	private double isoTimeDuration = 0d;

	private long numberOfBiSimCheckingRequest = 0;
	private long numberOfRealBiSimChecking = 0;

	private double biSimTimeDuration = 0d;

	private double creationOfNewLatticeNodeStart = 0d;
	private double creationOfNewLatticeNodeDuration = 0d;

	private double danglingCreationStartTime = 0d;
	public double danglingCreationDuration = 0d;
	public int numberOfDangling = 0;

	private double checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime = 0d;
	public double checkSameTypeSameStepsFromRootHasEnoughMatchesDuration = 0d;

	private double checkValidityAtLeastOneMatchForEachPatternNodeStartTime = 0d;
	public double checkValidityAtLeastOneMatchForEachPatternNodeDuration = 0d;

	private double processQueueNodeStartTime = 0d;
	public double processQueueNodeDuration = 0d;

	private double generateLatticeFromHereStartTime = 0d;
	public double generateLatticeFromHereDuration = 0d;

	private int numberOfComputeTemporalMatchSet = 0;
	private double computeTemporalMatchSetDuration = 0d;

	private double lhsExpansionStartTime = 0d;
	public double lhsExpansionDuration = 0d;

	private double rhsExpansionStartTime = 0d;
	public double rhsExpansionDuration = 0d;

	public int numberOfTotalRulesGenerated = 0;
	public int numberOfMaximalRulesGenerated = 0;

	private double gtarVerificationStartTime = 0d;
	public double gtarVerificationDuration = 0d;

	public int lastQualitySavedInterval = 0;
	public TreeMap<Integer, CumulativeRulesInfo> qualityOfTime = new TreeMap<Integer, CumulativeRulesInfo>();

	int minTimestamp;
	int maxTimestamp;
	int deltaT;
	public long checkIfSubPatternStartTime;
	public double checkIfSubPatternDuration = 0d;
	public double startTime;

	/**
	 * for static top-k finder
	 * 
	 * @param focusSetPath
	 * @param maxAllowedHops
	 * @param maxAllowedEdges
	 * @param dataGraph
	 * @param debugMode
	 * @param bitMap
	 * @throws Exception
	 */
	public LatticeReducedHeuristicOpt(String focusSetPath, int maxAllowedHops, int maxAllowedEdges,
			GraphDatabaseService dataGraph, boolean debugMode, double supportThreshold, double confidenceThreshold,
			int minTimestamp, int maxTimestamp, int deltaT) throws Exception {

		this.focusSetPath = focusSetPath;
		this.maxAllowedHops = maxAllowedHops;
		this.maxAllowedEdges = maxAllowedEdges;
		this.dataGraph = dataGraph;
		this.debugMode = debugMode;
		this.supportThreshold = supportThreshold;
		this.confidenceThreshold = confidenceThreshold;
		this.minTimestamp = minTimestamp;
		this.maxTimestamp = maxTimestamp;
		this.deltaT = deltaT;

		emptyPTRootNode = initializeLattice();

	}

	private void fillSetFromFile(String focusSetPath) throws Exception {
		// the format should be like:
		// NodeType | key1:value1, key2:value2
		FileInputStream fis = new FileInputStream(focusSetPath);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] labelAndPropKeyVals = line.trim().split("\\|");
			ArrayList<PairStrings> propKeyValues = new ArrayList<PairStrings>();
			if (labelAndPropKeyVals.length == 1) {
				focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
			} else if (labelAndPropKeyVals.length > 1) {
				String[] keyValuePairs = labelAndPropKeyVals[1].split(",");
				for (int i = 0; i < keyValuePairs.length; i++) {
					String[] separatedKeyValue = keyValuePairs[i].split(":");
					propKeyValues.add(new PairStrings(separatedKeyValue[0], separatedKeyValue[1].replace("\"", "")));
				}
			}
			// Assumption: distinct labels
			focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
			whatIsFocus += line;
		}
		br.close();
	}

	private LatticeNode<ILatticeNodeData> initializeLattice() throws Exception {

		// filling focusLabelPropValSet
		fillSetFromFile(focusSetPath);

		emptyPTRootNode = null;

		// generating the root of the prefix tree
		ILatticeNodeData emptyPTRootData = new LatticeNodeDataReducedHeuristic(focusLabelSet);
		emptyPTRootNode = new LatticeNode<ILatticeNodeData>(emptyPTRootData);

		emptyPTRootNode.getData().setPatternLatticeNodeIndex(-1);
		// the first level index should be set, otherwise all the levels
		// will be null!

		emptyPTRootNode.setRootLevel();

		// getting all focus nodes of the prefix-tree
		fillFocusNodesOfRequestedTypes(dataGraph);

		// a queue for searching in LHS space
		lhsQueue = new PriorityQueue<LatticeNode<ILatticeNodeData>>(new LHSQueueComparator());

		labelAdjacencyIndexer = new Indexer(dataGraph, allNodesOfFocusType);

		int minValue = Integer.MAX_VALUE;
		String selectedFocus = "";
		for (String focusLabel : allNodesOfFocusType.keySet()) {
			if (minValue > allNodesOfFocusType.get(focusLabel).size()) {
				minValue = allNodesOfFocusType.get(focusLabel).size();
				selectedFocus = focusLabel;
			}
		}

		PatternNode focusNode = new PatternNode(selectedFocus, The_Focus_Node);

		HashSet<Integer> dgGraphMatchNodes = new HashSet<Integer>();
		for (Integer nodeId : allNodesOfFocusType.get(selectedFocus)) {
			dgGraphMatchNodes.add(nodeId);
		}
		ILatticeNodeData firstLevelChildData = new LatticeNodeDataReducedHeuristic(focusNode, dgGraphMatchNodes,
				focusLabelSet, numberOfPatternsInLattice, this.labelAdjacencyIndexer);

		LatticeNode<ILatticeNodeData> firstLevelChildPTNode = new LatticeNode<ILatticeNodeData>(firstLevelChildData);

		emptyPTRootNode.addChild(firstLevelChildPTNode);

		lhsQueue.add(firstLevelChildPTNode);
		lhsQueueSet.add(firstLevelChildPTNode);

		latticeNodeIndex.put(numberOfPatternsInLattice++, firstLevelChildPTNode);

		if (DummyProperties.debugMode)
			System.out.println(
					"Before: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1e9);
		labelAdjacencyIndexer.generateTemporalNeighborhoodIndex(maxAllowedHops, whatIsFocus);
		if (DummyProperties.debugMode)
			System.out.println(
					"After: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1e9);

		return emptyPTRootNode;
	}

	private void fillSetFromFocuses(String focusesItSelf) {

		// the format should be like:
		// NodeType | key1:value1, key2:value2

		String[] focuses = focusesItSelf.split(",");
		for (String focus : focuses) {
			String[] labelAndPropKeyVals = focus.trim().split("\\|");
			ArrayList<PairStrings> propKeyValues = new ArrayList<PairStrings>();
			if (labelAndPropKeyVals.length == 1) {
				focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
			} else if (labelAndPropKeyVals.length > 1) {
				String[] keyValuePairs = labelAndPropKeyVals[1].split(",");
				for (int i = 0; i < keyValuePairs.length; i++) {
					String[] separatedKeyValue = keyValuePairs[i].split(":");
					propKeyValues.add(new PairStrings(separatedKeyValue[0], separatedKeyValue[1].replace("\"", "")));
				}
			}
			// Assumption: distinct labels
			focusLabelPropValSet.put(labelAndPropKeyVals[0], propKeyValues);
			whatIsFocus += focus;
		}

	}

	//
	private boolean createDanglingLatticeNodesIfNeeded(LatticeNode<ILatticeNodeData> tempProcessingNode,
			HashSet<Integer> newCreatedOrTouchedPTNodes, int snapshot, ExpansionSide expansionSide) throws Exception {

		if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
			return false;
		}

		danglingCreationStartTime = System.nanoTime();

		boolean isCreated = false;
		if (DummyProperties.debugMode) {
			System.out.println("");
			System.out.println("createDanglingLatticeNodesIfNeeded START");
		}

		// checking for neighbors if source node is the neighbor of focus
		// Set<DefaultLabeledEdge> edges =
		// tempProcessingNode.getData().getPatternGraph().getAllEdges(patternRootNode,
		// tempProcessingNode.getData().getSourcePatternNode());

		// boolean sourcePatternNodeIsACandidateForDangling = false;
		// if
		// (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
		// .get(tempProcessingNode.getData().getSourcePatternNode())
		// .size() != this.labelAdjacencyIndexer.candidateSetOfAPatternNode
		// .get(tempProcessingNode.getData().getSourcePatternNode()).size())
		// {
		// minCorrespondingPatternNode.add(e);
		// }

		// ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>
		// currentPatternGraph = tempProcessingNode.getData()
		// .getPatternGraph();
		// PatternNode sourceNode =
		// tempProcessingNode.getData().getSourcePatternNode();
		// PatternNode targetNode =
		// tempProcessingNode.getData().getTargetPatternNode();

		// PatternNode rootNode1 =
		// tempProcessingNode.getData().getPatternRootNodes().iterator().next();
		// if
		// (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(rootNode1)
		// .size() !=
		// labelAdjacencyIndexer.candidateSetOfAPatternNode.get(rootNode1).size())
		// {
		// return false;
		// }

		MyGraphPaths myGraphPaths = new MyGraphPaths(tempProcessingNode);

		HashMap<PatternNode, SelectedMinPatternNodeWithItsPath> minCorrespondingPatternNode = new HashMap<PatternNode, SelectedMinPatternNodeWithItsPath>();

		for (PatternNode rootNode : myGraphPaths.confirmedPaths.keySet()) {
			for (MyGraphPath myGraphPath : myGraphPaths.confirmedPaths.get(rootNode)) {
				if (myGraphPath.path.size() < 3)
					continue;
				for (int i = 1; i < myGraphPath.path.size(); i++) {

					// if
					// (tempProcessingNode.getParent().getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					// .get(myGraphPath.path.get(i)) == null) {
					// System.out.println();
					// }

					if (tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
							.get(myGraphPath.path.get(i)).size() != labelAdjacencyIndexer.candidateSetOfAPatternNode
									.get(myGraphPath.path.get(i)).size()) {
						minCorrespondingPatternNode.put(myGraphPath.path.get(i),
								new SelectedMinPatternNodeWithItsPath(myGraphPath, i));
						break;
					}
				}
			}
		}

		for (PatternNode minPatternNode : minCorrespondingPatternNode.keySet()) {

			Set<Integer> remainingNodeIdsView = Sets.symmetricDifference(
					this.labelAdjacencyIndexer.candidateSetOfAPatternNode.get(minPatternNode), tempProcessingNode
							.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(minPatternNode));

			HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
			remainingNodeIds.addAll(remainingNodeIdsView);

			SelectedMinPatternNodeWithItsPath smpnwip = minCorrespondingPatternNode.get(minPatternNode);

			PatternNode prevMinPatternNode = smpnwip.myGraphPath.path.get(smpnwip.selectedMinPatternNode - 1);
			Direction prevDirection = smpnwip.myGraphPath.directions.get(smpnwip.selectedMinPatternNode);

			Set<DefaultLabeledEdge> incomingOrOutgoingEdges;
			if (prevDirection == Direction.OUTGOING) {
				incomingOrOutgoingEdges = tempProcessingNode.getData().getPatternGraph()
						.incomingEdgesOf(minPatternNode);
			} else {
				incomingOrOutgoingEdges = tempProcessingNode.getData().getPatternGraph()
						.outgoingEdgesOf(minPatternNode);
			}

			for (DefaultLabeledEdge incomingOrOutgoinE : incomingOrOutgoingEdges) {

				PatternNode oldDanglingPatternNode = null;
				if (prevDirection == Direction.OUTGOING) {

					if (tempProcessingNode.getData().getPatternGraph()
							.getEdgeSource(incomingOrOutgoinE) != prevMinPatternNode)
						continue;

					for (DefaultLabeledEdge eee : tempProcessingNode.getData().getPatternGraph()
							.outgoingEdgesOf(prevMinPatternNode)) {

						PatternNode dest = tempProcessingNode.getData().getPatternGraph().getEdgeTarget(eee);
						if (dest != minPatternNode) {
							if (dest.getType().equals(minPatternNode.getType())
									&& tempProcessingNode.getData().getStepsFromRootOfPatternNodes().get(dest)
											.equals(tempProcessingNode.getData().getStepsFromRootOfPatternNodes()
													.get(minPatternNode))
									&& tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
											.get(minPatternNode).equals(tempProcessingNode.getData()
													.getIncomingRelTypesOfPatternNodes().get(dest))) {
								remainingNodeIds.removeAll(tempProcessingNode.getData().getMatchedNodes()
										.getDataGraphMatchNodeOfAbsPNode().get(dest));

								if (tempProcessingNode.getData().getPatternGraph().outDegreeOf(dest) == 0) {
									oldDanglingPatternNode = dest;
								}
							}
						}
					}
				} else {

					if (tempProcessingNode.getData().getPatternGraph()
							.getEdgeTarget(incomingOrOutgoinE) != prevMinPatternNode)
						continue;

					for (DefaultLabeledEdge eee : tempProcessingNode.getData().getPatternGraph()
							.incomingEdgesOf(prevMinPatternNode)) {

						PatternNode dest = tempProcessingNode.getData().getPatternGraph().getEdgeSource(eee);
						if (dest != minPatternNode) {
							if (!tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
									.containsKey(minPatternNode)
									&& tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
											.containsKey(dest)) {

							} else if (dest.getType().equals(minPatternNode.getType())
									&& tempProcessingNode.getData().getStepsFromRootOfPatternNodes().get(dest)
											.equals(tempProcessingNode.getData().getStepsFromRootOfPatternNodes()
													.get(minPatternNode))
									&& ((tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
											.get(minPatternNode) == null
											&& tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
													.get(dest) == null)
											|| (tempProcessingNode.getData().getIncomingRelTypesOfPatternNodes()
													.get(minPatternNode).equals(tempProcessingNode.getData()
															.getIncomingRelTypesOfPatternNodes().get(dest))))) {
								remainingNodeIds.removeAll(tempProcessingNode.getData().getMatchedNodes()
										.getDataGraphMatchNodeOfAbsPNode().get(dest));

								if (tempProcessingNode.getData().getPatternGraph().inDegreeOf(dest) == 0) {
									oldDanglingPatternNode = dest;
								}
							}
						}
					}
				}

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					if (patternNode != minPatternNode && patternNode.getLabel().equals(minPatternNode.getLabel())
							&& tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
									.get(patternNode).size() == 1) {
						remainingNodeIds.removeAll(tempProcessingNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
					}
				}

				if (remainingNodeIds.size() > 0) {

					String newNodeType = minPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
							+ incomingOrOutgoinE.getType();

					int max = 0;

					if (prevDirection == Direction.OUTGOING) {
						for (Integer nodeId : tempProcessingNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(prevMinPatternNode)) {

							if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(nodeId).containsKey(newNodeType)
									&& labelAdjacencyIndexer.temporalNeighborhoodIndex.get(nodeId).get(newNodeType)
											.containsKey(Indexer.AFTER))
								max = Math.max(labelAdjacencyIndexer.temporalNeighborhoodIndex.get(nodeId)
										.get(newNodeType).get(Indexer.AFTER).neighborNodeIds.size(), max);
						}

						if (max > tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
								.get(prevMinPatternNode).get(newNodeType)) {

							isCreated = innerDanglingCreation(oldDanglingPatternNode, remainingNodeIds,
									tempProcessingNode, snapshot, incomingOrOutgoinE, minPatternNode,
									prevMinPatternNode, newCreatedOrTouchedPTNodes, prevDirection, expansionSide);
						}
					} else {
						for (Integer nodeId : tempProcessingNode.getData().getMatchedNodes()
								.getDataGraphMatchNodeOfAbsPNode().get(prevMinPatternNode)) {

							if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(nodeId).containsKey(newNodeType)
									&& labelAdjacencyIndexer.temporalNeighborhoodIndex.get(nodeId).get(newNodeType)
											.containsKey(Indexer.BEFORE))
								max = Math.max(labelAdjacencyIndexer.temporalNeighborhoodIndex.get(nodeId)
										.get(newNodeType).get(Indexer.BEFORE).neighborNodeIds.size(), max);
						}

						if (max > tempProcessingNode.getData().getFrequencyOfPrevNeighborOfSameType()
								.get(prevMinPatternNode).get(newNodeType)) {

							isCreated = innerDanglingCreation(oldDanglingPatternNode, remainingNodeIds,
									tempProcessingNode, snapshot, incomingOrOutgoinE, minPatternNode,
									prevMinPatternNode, newCreatedOrTouchedPTNodes, prevDirection, expansionSide);
						}
					}

				}
			}
		}
		if (DummyProperties.debugMode) {
			// DebugHelper.printGlobalCandidateSet(this);
			System.out.println("createDanglingLatticeNodesIfNeeded END");
			System.out.println("");
		}

		danglingCreationDuration += ((System.nanoTime() - danglingCreationStartTime) / 1e6);
		return isCreated;

	}

	private void createFromOldDangling(PatternNode oldDanglingPatternNode, HashSet<Integer> remainingNodeIds,
			LatticeNode<ILatticeNodeData> tempProcessingNode, int snapshot) throws Exception {
		this.labelAdjacencyIndexer.candidateSetOfAPatternNode.get(oldDanglingPatternNode).addAll(remainingNodeIds);

		// for (Integer nodeId : remainingNodeIds) {
		// tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().putIfAbsent(nodeId,
		// new HashSet<PatternNode>());
		// tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(nodeId)
		// .add(oldDanglingPatternNode);
		// }

		DualSimulationHandler.computeMatchSetOfAPattern(dataGraph, tempProcessingNode, this);

		if (DummyProperties.debugMode) {
			System.out.println("adding some new matches in dangling proc: " + tempProcessingNode.getData());

		}
	}

	private boolean innerDanglingCreation(PatternNode oldDanglingPatternNode, HashSet<Integer> remainingNodeIds,
			LatticeNode<ILatticeNodeData> tempProcessingNode, int snapshot, DefaultLabeledEdge incomingOrOutgoinE,
			PatternNode minPatternNode, PatternNode prevMinPatternNode, HashSet<Integer> newCreatedOrTouchedPTNodes,
			Direction prevDirection, ExpansionSide expansionSide) throws Exception {
		boolean isCreated = false;

		if (oldDanglingPatternNode != null) {
			createFromOldDangling(oldDanglingPatternNode, remainingNodeIds, tempProcessingNode, snapshot);
		} else {

			ArrayList<PatternGraphAndPreMatches> newerPatternGraphs = new ArrayList<PatternGraphAndPreMatches>();
			// DefaultLabeledEdge.class

			SetView<Integer> intersectionOfTargetNodeAndRemainingIds = Sets.intersection(remainingNodeIds,
					tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
							.get(tempProcessingNode.getData().getTargetPatternNode()));

			SetView<Integer> disjointOfTargetNodeAndRemainingIds = Sets.difference(remainingNodeIds,
					tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
							.get(tempProcessingNode.getData().getTargetPatternNode()));

			// if there is any intersection we should create
			// a
			// pattern for that
			if (intersectionOfTargetNodeAndRemainingIds.size() > 0) {

				ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newerPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
						DefaultLabeledEdge.class);

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					newerPatternGraph.addVertex(patternNode);
				}

				for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
					newerPatternGraph.addEdge(newerPatternGraph.getEdgeSource(e), newerPatternGraph.getEdgeTarget(e),
							e);
				}

				PatternNode danglingPatternNode = tempProcessingNode.getData().getTargetPatternNode();

				newerPatternGraph.addVertex(danglingPatternNode);

				if (prevDirection == Direction.OUTGOING) {
					newerPatternGraph.addEdge(
							tempProcessingNode.getData().getPatternGraph().getEdgeSource(incomingOrOutgoinE),
							danglingPatternNode, new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				} else {
					newerPatternGraph.addEdge(
							tempProcessingNode.getData().getPatternGraph().getEdgeTarget(incomingOrOutgoinE),
							danglingPatternNode, new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				}

				newerPatternGraphs.add(new PatternGraphAndPreMatches(newerPatternGraph,
						intersectionOfTargetNodeAndRemainingIds, danglingPatternNode));
			}

			// if there is no intersection => 0 and
			// remainingNodeIds >0
			// or some intersections
			if (intersectionOfTargetNodeAndRemainingIds.size() != remainingNodeIds.size()) {

				ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newerPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
						DefaultLabeledEdge.class);

				for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
					newerPatternGraph.addVertex(patternNode);
				}

				for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
					newerPatternGraph.addEdge(newerPatternGraph.getEdgeSource(e), newerPatternGraph.getEdgeTarget(e),
							e);
				}

				PatternNode danglingPatternNode = new PatternNode(minPatternNode.getLabel(), false);
				newerPatternGraph.addVertex(danglingPatternNode);

				if (prevDirection == Direction.OUTGOING) {
					newerPatternGraph.addEdge(
							tempProcessingNode.getData().getPatternGraph().getEdgeSource(incomingOrOutgoinE),
							danglingPatternNode, new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				} else {// ?? make sure that it's correct
					newerPatternGraph.addEdge(danglingPatternNode,
							tempProcessingNode.getData().getPatternGraph().getEdgeTarget(incomingOrOutgoinE),
							new DefaultLabeledEdge(incomingOrOutgoinE.getType()));
				}

				newerPatternGraphs.add(new PatternGraphAndPreMatches(newerPatternGraph,
						disjointOfTargetNodeAndRemainingIds, danglingPatternNode));
			}

			for (PatternGraphAndPreMatches newPatternGraphAndPreMatches : newerPatternGraphs) {
				boolean hadBiSimulInTheLevel = false;

				if (this.labelAdjacencyIndexer.latticeNodesOfALevel.get(tempProcessingNode.getLevel() + 1) != null) {
					for (LatticeNode<ILatticeNodeData> LatticeSibling : this.labelAdjacencyIndexer.latticeNodesOfALevel
							.get(tempProcessingNode.getLevel() + 1)) {

						if (DualSimulationHandler.isBiDualSimulated(newPatternGraphAndPreMatches.patternGraph,
								LatticeSibling, this)) {

							if (DummyProperties.debugMode) {
								System.out.println("DANGLING: simultanous siblings: add node link from "
										+ tempProcessingNode.getData().getPatternLatticeNodeIndex() + " to "
										+ LatticeSibling.getData().getPatternLatticeNodeIndex());
							}
							addIncNodeLink(tempProcessingNode, LatticeSibling, this);
							hadBiSimulInTheLevel = true;
							isCreated = true;
						}
					}
				}

				if (!hadBiSimulInTheLevel) {

					if (DummyProperties.debugMode) {
						System.out.println("new dangling child: "
								+ tempProcessingNode.getData().getPatternGraph().getEdgeSource(incomingOrOutgoinE)
								+ " -> " + newPatternGraphAndPreMatches.danglingPatternNode + " rem. nodeIds:"
								+ newPatternGraphAndPreMatches.preMatches);
						System.out.println(" from " + tempProcessingNode.getData());
					}

					numberOfDangling++;

					createNewLatticeNode(tempProcessingNode, newPatternGraphAndPreMatches.patternGraph,
							prevMinPatternNode, newPatternGraphAndPreMatches.danglingPatternNode,
							newPatternGraphAndPreMatches.preMatches, newCreatedOrTouchedPTNodes,
							incomingOrOutgoinE.getType(),
							tempProcessingNode.getData().getStepsFromRootOfPatternNodes().get(minPatternNode), snapshot,
							null, true, prevDirection, expansionSide);

					isCreated = true;

				}
			}
		}
		return isCreated;
	}

	private boolean checkSameTypeSameStepsFromRootHasEnoughMatches(LatticeNode<ILatticeNodeData> tempProcessingNode) {

		checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime = System.nanoTime();

		for (PatternNode srcPatternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
			for (String nexType : tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
					.get(srcPatternNode).keySet()) {

				int howManyOfSameType = tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
						.get(srcPatternNode).get(nexType);
				if (howManyOfSameType > 1) {

					HashSet<Integer> allMatchNodesSet = new HashSet<Integer>();
					for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph()
							.outgoingEdgesOf(srcPatternNode)) {

						String tempNexType = tempProcessingNode.getData().getPatternGraph().getEdgeTarget(e).getLabel()
								+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType();

						if (nexType.equals(tempNexType)) {
							allMatchNodesSet.addAll(
									tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
											.get(tempProcessingNode.getData().getPatternGraph().getEdgeTarget(e)));
						}
					}
					if (allMatchNodesSet.size() < howManyOfSameType) {
						checkSameTypeSameStepsFromRootHasEnoughMatchesDuration += ((System.nanoTime()
								- checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime) / 1e6);
						return false;
					}
				}

			}
		}
		checkSameTypeSameStepsFromRootHasEnoughMatchesDuration += ((System.nanoTime()
				- checkSameTypeSameStepsFromRootHasEnoughMatchesStartTime) / 1e6);
		return true;
	}

	public LatticeNode<ILatticeNodeData> generateLatticeForTemporalGraph(double startTime) throws Exception {

		this.startTime = startTime;

		int latticeProcessingLevel = lhsQueue.peek().getLevel();

		while (!lhsQueue.isEmpty()) {

			lhsExpansionStartTime = System.nanoTime();

			if (DummyProperties.debugMode) {
				System.out.println();
				System.out.println("lhsQueue.size: " + lhsQueue.size());
			}

			LatticeNode<ILatticeNodeData> lhsProcessingNode = lhsQueue.poll();
			lhsQueueSet.remove(lhsProcessingNode);

			try {

				// if (lhsProcessingNode.getData().getPatternLatticeNodeIndex()
				// == 60) {
				// System.out.println();
				// }
				// matchset should have computed before

				if (maxAllowedHops > 1 && !lhsProcessingNode.getData().isDanglingPattern()
						&& lhsProcessingNode.getData().getPatternGraph().edgeSet().size() >= 2
						&& lhsProcessingNode.getData().getPatternGraph().edgeSet().size() < maxAllowedEdges) {
					createDanglingLatticeNodesIfNeeded(lhsProcessingNode, null, 0, ExpansionSide.LHS);
				}

			} catch (Exception exc) {
				System.out.println(lhsProcessingNode.getData().getMappedGraphString());
				throw exc;
			}

			// hasSameNeighborsWithLessMatch(tempProcessingNode);

			lhsProcessingNode.getData().setVisited(true);

			if (latticeProcessingLevel < lhsProcessingNode.getLevel()) {

				// new sibling will be created soon.
				// old ones should be cleared
				// sameLevelLatticeNodes.clear();
				// going to the next level
				latticeProcessingLevel = lhsProcessingNode.getLevel();
				if (DummyProperties.debugMode)
					System.out.println("latticeProcessingLevel G0: " + latticeProcessingLevel);
			}

			if (DummyProperties.debugMode) {
				System.out.println("generator processing node:");
				System.out.println(lhsProcessingNode.getData().getMappedGraphString());
			}

			processQueueNode(dataGraph, lhsProcessingNode.getLevel() + 1, lhsProcessingNode, null, 0,
					ExpansionSide.LHS);

			queueNodePostProcessing(lhsProcessingNode, null, ExpansionSide.LHS);
			removeUselessMatchSet(lhsProcessingNode);

			// if (DummyProperties.qualityVsTime) {
			// checkTimeAndSaveQualityIfNeeded();
			// }

			// System.out.println("LHS:1111111");
			// DebugHelper.printOrderOfTheQueue(lhsQueue);

			if (DummyProperties.hasOptimization
					&& lhsProcessingNode.getData().getTotalSupportFrequency() < supportThreshold)
				continue;

			// if it can be a maximal frequent pattern
			// fix it as LHS
			boolean hasANonCheckedChildren = false;
			if (lhsProcessingNode.getChildrenLinksSet() != null) {
				for (LatticeNode<ILatticeNodeData> child : lhsProcessingNode.getChildrenLinksSet()) {
					if (child.getData().canBeFrequent() && !child.getData().hasLHSProcessed()) {
						hasANonCheckedChildren = true;
						break;
					}
				}
			}

			lhsExpansionDuration += (System.nanoTime() - lhsExpansionStartTime) / 1e6;
			if (!hasANonCheckedChildren) {

				// if (lhsProcessingNode.getData().getPatternLatticeNodeIndex()
				// == 65) {
				// System.out.println();
				// }

				fixedLHSCnt++;
				if (DummyProperties.debugMode)
					System.out.println(
							fixedLHSCnt + ": FIXED AS LHS:" + lhsProcessingNode.getData().getPatternLatticeNodeIndex()
									+ " #patterns:" + latticeNodeIndex.size());

				lhsProcessingNode.getData().setLHSProcessed();

				rhsExpansionStartTime = System.nanoTime();

				lhsProcessingNode.getData().initRHSQueueFromChildrenMaxRules(this, lhsProcessingNode, maxAllowedEdges,
						supportThreshold);

				// we assumed that the heuristic function of rhsQueue sorted
				// available rhs's intelligently
				while (!lhsProcessingNode.getData().getRHSQueue().isEmpty()) {

					LatticeNode<ILatticeNodeData> rhsProcessingNode = lhsProcessingNode.getData().getRHSQueue().poll();

					if (DummyProperties.hasOptimization
							&& rhsProcessingNode.getData().getTotalSupportFrequency() < supportThreshold) {
						continue;
					}

					// if LHS and RHS do not have enough identical focus matches
					// they cannot create a rule:
					if (DummyProperties.hasOptimization && DualSimulationHandler
							.getAllIntersectionOfTwoPatterns(lhsProcessingNode, rhsProcessingNode) < supportThreshold) {
						intersectionPruning++;
						continue;
					}

					lhsProcessingNode.getData().addToVisitedRHS(rhsProcessingNode);
					rhsTrials++;
					// DebugHelper.printOrderOfTheQueue(lhsProcessingNode.getData().getRHSQueue());
					if (DummyProperties.debugMode) {
						System.out.println("rhs processing node:");
						System.out.println(rhsProcessingNode.getData().getMappedGraphString());
					}

					boolean createdInfrequentRule = false;
					// if RHS is not a subpattern of LHS, we create a gtar
					checkIfSubPatternStartTime = System.nanoTime();
					boolean isSubPattern = DualSimulationHandler.checkIfSubPattern(this,
							this.labelAdjacencyIndexer.subPatternsOfAPattern, rhsProcessingNode, lhsProcessingNode);

					// it might be a child of this (as a linked node) considered
					// before as a maximal
					// rule (rhs), so we just ignore this and go to its
					// children.
					if (!isSubPattern)
						isSubPattern = DualSimulationHandler.checkIfSubPatternOfACurrentRule(this,
								this.labelAdjacencyIndexer.subPatternsOfAPattern, rhsProcessingNode,
								lhsProcessingNode.getData().getRulesOfThis());
					checkIfSubPatternDuration += (System.nanoTime() - checkIfSubPatternStartTime) / 1e6;

					// if
					// (lhsProcessingNode.getData().getPatternLatticeNodeIndex()
					// == 60
					// // &&
					// //
					// rhsProcessingNode.getData().getPatternLatticeNodeIndex()
					// // == 39
					// ) {
					// System.out.println();
					// }

					if (!isSubPattern) {
						// generate a rule
						Rule currentRule = lhsProcessingNode.getData().generateARule(lhsProcessingNode,
								rhsProcessingNode, (System.nanoTime() - startTime));

						numberOfTotalRulesGenerated++;

						gtarVerificationStartTime = System.nanoTime();
						numberOfGtarVerification++;
						// verify it
						DualSimulationHandler.gtarVerification2(currentRule, minTimestamp, maxTimestamp, deltaT);
						gtarVerificationDuration += (System.nanoTime() - gtarVerificationStartTime) / 1e6;

						DualSimulationHandler.computeConfidence(currentRule);

						if (currentRule.support < supportThreshold || currentRule.confidence < confidenceThreshold) {
							lhsProcessingNode.getData().removeTheRule(currentRule);
							totalStopDueToSupportOrConfidenceThreshold++;
							// if (DummyProperties.hasOptimization)
							continue;
						}

						if (currentRule.support >= supportThreshold && currentRule.confidence >= confidenceThreshold) {
							// remove a rule with parent of this as RHS if any:
							// it's not a maximal rule
							lhsProcessingNode.getData().removeRule(lhsProcessingNode, rhsProcessingNode.getParent());
							if (rhsProcessingNode.getSuperNodeLinks() != null) {
								for (LatticeNode<ILatticeNodeData> superLatticeNode : rhsProcessingNode
										.getSuperNodeLinks()) {
									lhsProcessingNode.getData().removeRule(lhsProcessingNode, superLatticeNode);
								}
							}
						} else {
							createdInfrequentRule = true;
						}
					}

					// if the created rule was infrequent it would be same
					if (!createdInfrequentRule) {
						// expanding RHS by generation if it's not visited
						if (!rhsProcessingNode.getData().isVisited()) {

							rhsProcessingNode.getData().setVisited(true);

							if (maxAllowedHops > 1 && !rhsProcessingNode.getData().isDanglingPattern()
									&& rhsProcessingNode.getData().getPatternGraph().edgeSet().size() >= 2
									&& rhsProcessingNode.getData().getPatternGraph().edgeSet()
											.size() < maxAllowedEdges) {
								createDanglingLatticeNodesIfNeeded(rhsProcessingNode, null, 0, ExpansionSide.RHS);
							}

							processQueueNode(dataGraph, rhsProcessingNode.getLevel() + 1, rhsProcessingNode, null, 0,
									ExpansionSide.RHS);

							queueNodePostProcessing(lhsProcessingNode, rhsProcessingNode, ExpansionSide.RHS);
							removeUselessMatchSet(rhsProcessingNode);
							// System.out.println("RHS: 111111");
							// DebugHelper.printOrderOfTheQueue(lhsProcessingNode.getData().getRHSQueue());
						}
						// expanding RHS by traversal if it's already
						// visited
						else {
							// it has both children and linkedNodes
							if (rhsProcessingNode.getChildrenLinksSet() != null) {
								for (LatticeNode<ILatticeNodeData> rhs : rhsProcessingNode.getChildrenLinksSet()) {
									if (rhs.getData().getNumberOfDistinctFocusesOverAllTimestamps() > 0
											&& rhs.getData().getTotalSupportFrequency() >= supportThreshold) {
										lhsProcessingNode.getData().addToRHSQueue(rhs);
									}
								}
								// System.out.println("RHS: 22222222");
								// DebugHelper.printOrderOfTheQueue(lhsProcessingNode.getData().getRHSQueue());
							}
						}
					}
					// continue to find all maximal rules with the fixed
					// LHS
				}

				rhsExpansionDuration += (System.nanoTime() - rhsExpansionStartTime) / 1e6;

				// TODO: free RHS related memory for this node
				// lhsProcessingNode.getData().getRHSQueue().clear();
				lhsProcessingNode.getData().nullRHSQueue();

				// if(lhsProcessingNode.getData().getPatternLatticeNodeIndex()==29
				// ||
				// lhsProcessingNode.getData().getPatternLatticeNodeIndex()==21){
				// System.out.println();
				// }
				//

			} else {
				// if it's not fixed as LHS we should come back to it later
				if (!lhsProcessingNode.getData().hasLHSProcessed() && !lhsQueueSet.contains(lhsProcessingNode)) {
					// if
					// (lhsProcessingNode.getData().getPatternLatticeNodeIndex()
					// == 60) {
					// System.out.println();
					// }
					lhsQueue.add(lhsProcessingNode);
					lhsQueueSet.add(lhsProcessingNode);
				}
			}
			// DebugHelper.checkIfThereAreNotMaximalRules(this);

			// if (DummyProperties.qualityVsTime) {
			// checkTimeAndSaveQualityIfNeeded();
			// }

			// System.out.println("LHS:3333333");
			// DebugHelper.printOrderOfTheQueue(lhsQueue);
			// System.out.println();
		}

		if (DummyProperties.debugMode)
			System.out.println("finishing queue!");
		return emptyPTRootNode;

	}

	// private void checkTimeAndSaveQualityIfNeeded() {
	// // check the current duration, then based on the interval if we are in
	// // the next interval we should save the quality value in somewhere
	// double checkingTimeStart = System.nanoTime();
	// int d = (int) (((System.nanoTime() - startTime) / 1e6) /
	// DummyProperties.qualitySaveIntervalInMilliSeconds);
	// CumulativeRulesInfo cri = null;
	// if (d > lastQualitySavedInterval) {
	// cri = getTotalQualityOfMaximalRules();
	// qualityOfTime.put(d, cri);
	// lastQualitySavedInterval = d;
	// }
	//
	// if (Dummy.DummyProperties.debugMode)
	// System.out.println("checkTimeAndSaveQualityIfNeeded: " + d + ",
	// lastQualitySavedInterval:"
	// + lastQualitySavedInterval + ", rules: " + (cri != null ?
	// cri.numberOfRules : "null"));
	//
	// startTime += (System.nanoTime() - checkingTimeStart);
	//
	// }
	//
	// private CumulativeRulesInfo getTotalQualityOfMaximalRules() {
	//
	// double support = 0d;
	// double confidence = 0d;
	// int rulesNum = 0;
	//
	// for (int index : this.latticeNodeIndex.keySet()) {
	// if (this.latticeNodeIndex.get(index).getData().getRulesOfThis().size() >
	// 0) {
	// for (Rule rule :
	// this.latticeNodeIndex.get(index).getData().getRulesOfThis()) {
	// support += rule.support;
	// confidence += rule.confidence;
	// rulesNum++;
	// }
	// }
	// }
	//
	// return new CumulativeRulesInfo(support, confidence, rulesNum);
	// }

	private void removeUselessMatchSet(LatticeNode<ILatticeNodeData> processingNode) {

		for (PatternNode ptNode : processingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			if (!ptNode.isFocus()) {
				processingNode.getData().getMatchedNodes().removeTimePointsOfMatches(ptNode,
						processingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(ptNode));
				processingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().put(ptNode,
						new HashSet<Integer>());
			}
		}
	}

	private void queueNodePostProcessing(LatticeNode<ILatticeNodeData> lhsProcessingNode,
			LatticeNode<ILatticeNodeData> rhsProcessingNode, ExpansionSide expansionSide) throws Exception {

		LatticeNode<ILatticeNodeData> processingNode = (expansionSide == ExpansionSide.LHS ? lhsProcessingNode
				: rhsProcessingNode);

		if (processingNode.getChildren() != null) {
			for (LatticeNode<ILatticeNodeData> newChild : processingNode.getChildren()) {
				innerQueueNodePostProcessing(lhsProcessingNode, rhsProcessingNode, newChild, expansionSide);
			}
		}
	}

	private void innerQueueNodePostProcessing(LatticeNode<ILatticeNodeData> lhsProcessingNode,
			LatticeNode<ILatticeNodeData> rhsProcessingNode, LatticeNode<ILatticeNodeData> newChild,
			ExpansionSide expansionSide) throws Exception {

		LatticeNode<ILatticeNodeData> processingNode = (expansionSide == ExpansionSide.LHS ? lhsProcessingNode
				: rhsProcessingNode);

		if (!newChild.getData().isVerified())
			// compute matchset and associated time intervals:
			DualSimulationHandler.computeMatchSetOfAPattern(dataGraph, newChild, this);

		// we should only add the new pattern to the queue if
		// it has
		// enough distinct focus node
		// TODO: can we make it tighter?
		if (newChild.getData().getNumberOfDistinctFocusesOverAllTimestamps() > 0 && (!DummyProperties.hasOptimization
				|| newChild.getData().getTotalSupportFrequency() >= supportThreshold)) {

			newChild.getData().setCanBeFrequent(true);

			if (!newChild.getData().hasLHSProcessed() && !lhsQueueSet.contains(newChild)) {
				lhsQueue.add(newChild);
				lhsQueueSet.add(newChild);
			}

			if (expansionSide == ExpansionSide.RHS && !lhsProcessingNode.getData().isVisitedRHS(newChild)) {
				// if (newChild.getData().getTotalSupportFrequency() <
				// supportThreshold) {
				// System.out.println();
				// }
				lhsProcessingNode.getData().addToRHSQueue(newChild);
			}
		}

	}

	private void fillFocusNodesOfRequestedTypes(GraphDatabaseService dataGraph2) throws Exception {

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			allNodesOfFocusType.put(focusLabel, new HashSet<Integer>());
			focusLabelSet.add(focusLabel);
		}

		for (String focusLabel : focusLabelPropValSet.keySet()) {
			ArrayList<PairStrings> propVals = focusLabelPropValSet.get(focusLabel);
			for (Node node : dataGraph.getAllNodes()) {
				// boolean isCandidate = true;
				if (!node.hasLabel(Label.label(focusLabel))) {
					continue;
				}
				if (propVals.size() > 0) {
					for (PairStrings pairString : propVals) {

						if (node.hasProperty(pairString.key)) {
							if (node.getProperty(pairString.key).toString().toLowerCase()
									.equals(pairString.value.toLowerCase())
									|| DummyFunctions.isContain(
											node.getProperty(pairString.key).toString().toLowerCase(),
											pairString.value.toLowerCase())) {
								allNodesOfFocusType.get(focusLabel).add((int) node.getId());
								break;
							}
						}

					}
				} else {
					allNodesOfFocusType.get(focusLabel).add((int) node.getId());
				}

			}
		}

		numberOfAllFocusNodes = 0;
		for (String key : allNodesOfFocusType.keySet()) {
			if (allNodesOfFocusType.get(key).size() == 0) {
				throw new Exception("no items for \"" + key + "\"");
			}

			numberOfAllFocusNodes += allNodesOfFocusType.get(key).size();
		}

		Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES = numberOfAllFocusNodes;
		if (DummyProperties.debugMode) {
			System.out.println("focusNodesOfSpecificType size: " + allNodesOfFocusType.size());
		}
	}

	//
	private void outgoingExpansion(LatticeNode<ILatticeNodeData> tempProcessingNode,
			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode,
			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcOutgoing,
			HashSet<Integer> newCreatedOrTouchedPTNodes, HashMap<String, HashSet<Integer>> seenRelTypeOutgoingSet,
			int snapshot, int latticeProcessingLevel,
			HashMap<PatternNode, HashSet<LatticeNode<ILatticeNodeData>>> newChildrenOfTheSrc, boolean unSeenFocusType,
			ExpansionSide expansionSide) throws Exception {

		HashSet<Integer> sameLabelNeighborNodes = new HashSet<Integer>();

		for (String otherNodeLabelRelType : seenRelTypeOutgoingSet.keySet()) {
			HashSet<Integer> srcNodeIds = seenRelTypeOutgoingSet.get(otherNodeLabelRelType);
			for (Integer srcDataGpNodeId : srcNodeIds) {
				if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId).get(otherNodeLabelRelType)
						.containsKey(Indexer.AFTER)) {
					sameLabelNeighborNodes.addAll(labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId)
							.get(otherNodeLabelRelType).get(Indexer.AFTER).neighborNodeIds);
				}
			}

			if (unSeenFocusType) {
				sameLabelNeighborNodes.retainAll(allNodesOfFocusType
						.get(otherNodeLabelRelType.split(DummyProperties.SEPARATOR_LABEL_AND_RELTYPE)[0]));
			}

			if (sameLabelNeighborNodes == null || sameLabelNeighborNodes.size() == 0) {
				return;
			}

			int separatorIndex = otherNodeLabelRelType.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
			String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
			String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);
			Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;

			ArrayList<GoBackToPrevHolder> destPatternNodes = new ArrayList<GoBackToPrevHolder>();

			getDestPatternNodeAndCheckForGoBackToPrev(destPatternNodes, tempProcessingNodeData, srcPatternNode,
					srcNodeIds, sameLabelNeighborNodes, destLabel, relationshipType, destStepsFromRoot);

			for (GoBackToPrevHolder destPatternNode : destPatternNodes) {

				destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
				int destInDegree = 1;
				int incomingFromSameType = 1;

				if (destPatternNode.goBackToPrev) {
					// b1 or b3 a->b->d->b and a->b
					destStepsFromRoot = Math.min(destStepsFromRoot, tempProcessingNodeData
							.getStepsFromRootOfPatternNodes().get(destPatternNode.destPatternNode));

					destInDegree += tempProcessingNodeData.getPatternGraph()
							.inDegreeOf(destPatternNode.destPatternNode);

					for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
							.incomingEdgesOf(destPatternNode.destPatternNode)) {
						if (tempProcessingNodeData.getPatternGraph().getEdgeSource(e).getLabel()
								.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
							incomingFromSameType++;
						}
					}
				}

				HashSet<Integer> newNodeIdsMustBeRemoved = new HashSet<Integer>();
				for (Integer newNodeId : destPatternNode.newNodeIds) {
					if (destInDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).inDegree
					// dataGraph.getNodeById(newNodeId)
					// .getDegree(org.neo4j.graphdb.Direction.INCOMING)
					) {

						newNodeIdsMustBeRemoved.add(newNodeId);
					}
				}
				destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
				newNodeIdsMustBeRemoved.clear();

				for (Integer newNodeId : destPatternNode.newNodeIds) {

					if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(newNodeId)
							.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
									+ relationshipType) != null
							&& labelAdjacencyIndexer.temporalNeighborhoodIndex.get(newNodeId)
									.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
											+ relationshipType)
									.containsKey(Indexer.BEFORE)
							&& (incomingFromSameType > labelAdjacencyIndexer.temporalNeighborhoodIndex
									.get(newNodeId).get(srcPatternNode.getLabel()
											+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
									.get(Indexer.BEFORE).neighborNodeIds.size())) {
						if (DummyProperties.debugMode) {
							System.out.println("cont. incomingFromSameType: " + incomingFromSameType
									+ " prev index type in data graph:"
									+ labelAdjacencyIndexer.temporalNeighborhoodIndex
											.get(newNodeId).get(srcPatternNode.getLabel()
													+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
											.get(Indexer.BEFORE).neighborNodeIds.size());
						}
						newNodeIdsMustBeRemoved.add(newNodeId);
					}
				}
				destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);

				if (destPatternNode.newNodeIds.size() == 0)
					continue;

				// finding the new node type;
				String newNodeType = null;

				// if we've seen it in this pattern
				// before...

				newNodeType = destPatternNode.destPatternNode.getType();

				LatticeNode<ILatticeNodeData> seenLatticeNode = null;
				if (destPatternNode.goBackToPrev) {
					if (seenPatternNodeFromPreviousNodesForThisSrcOutgoing.containsKey(destPatternNode)) {
						seenLatticeNode = seenPatternNodeFromPreviousNodesForThisSrcOutgoing.get(destPatternNode);
					}
				} else {
					if (newlySeenPatternNodeForThisSrcOutgoing.containsKey(
							newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
						// if this expansion has
						// seen
						// before
						// add it to the group of
						// that
						// prefix-tree node
						seenLatticeNode = newlySeenPatternNodeForThisSrcOutgoing
								.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
					}
				}

				if (seenLatticeNode != null) {
					// double start =
					// System.nanoTime();

					ILatticeNodeData LatticeNodeData = seenLatticeNode.getData();

					PatternNode tempDestPatternNode = seenLatticeNode.getData().getTargetPatternNode();

					LatticeNodeData.addNewMatchSet(tempDestPatternNode, destPatternNode.newNodeIds,
							this.labelAdjacencyIndexer);

					if (DummyProperties.debugMode) {
						System.out.println("prev pattern seen:");
						System.out.println(LatticeNodeData.getMappedGraphString());
					}

					seenPatternNodeFromPreviousNodesForThisSrcOutgoing.put(tempDestPatternNode, seenLatticeNode);

					if (newCreatedOrTouchedPTNodes != null)
						newCreatedOrTouchedPTNodes.add(seenLatticeNode.getData().getPatternLatticeNodeIndex());
				} else {

					// make a new pattern for SGI
					// checking
					// and add it as
					// a new child if possible
					ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
							DefaultLabeledEdge.class);

					for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
						newPatternGraph.addVertex(patternNode);
					}

					for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
						newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
					}

					if (!newPatternGraph.vertexSet().contains(destPatternNode.destPatternNode)) {
						newPatternGraph.addVertex(destPatternNode.destPatternNode);
					}

					newPatternGraph.addEdge(srcPatternNode, destPatternNode.destPatternNode,
							new DefaultLabeledEdge(relationshipType));

					addNewChildrenOrMatches(tempProcessingNode, tempProcessingNodeData, newPatternGraph, srcPatternNode,
							destPatternNode, destPatternNode.newNodeIds, newlySeenPatternNodeForThisSrcOutgoing,
							newCreatedOrTouchedPTNodes, destStepsFromRoot, relationshipType,
							seenPatternNodeFromPreviousNodesForThisSrcOutgoing, snapshot,
							Dummy.DummyProperties.Direction.OUTGOING, latticeProcessingLevel, newChildrenOfTheSrc,
							expansionSide);

				}
			}
			// } for all same nodes
		}

	}

	private void addNewChildrenOrMatches(LatticeNode<ILatticeNodeData> tempProcessingNode,
			ILatticeNodeData tempProcessingNodeData,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph, PatternNode srcPatternNode,
			GoBackToPrevHolder destPatternNode, HashSet<Integer> newNodeIds,
			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrc,
			HashSet<Integer> newCreatedOrTouchedPTNodes, Integer destStepsFromRoot, String relationshipType,
			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrc,
			int snapshot, Direction direction, int latticeProcessingLevel,
			HashMap<PatternNode, HashSet<LatticeNode<ILatticeNodeData>>> newChildrenOfTheSrc,
			ExpansionSide expansionSide) throws Exception {

		// for all other same-level
		// children
		// of the prefix-tree:
		boolean itWasBisimulated = false;

		if (this.labelAdjacencyIndexer.latticeNodesOfALevel.get(latticeProcessingLevel) != null) {
			for (LatticeNode<ILatticeNodeData> LatticeSibling : this.labelAdjacencyIndexer.latticeNodesOfALevel
					.get(latticeProcessingLevel)) {
				// if
				// (LatticeSibling.getData().getPatternLatticeNodeIndex()
				// == 38) {
				// System.out.println();
				// }

				if (DualSimulationHandler.isBiDualSimulated(newPatternGraph, LatticeSibling, this)) {

					// if yes:
					// if (check sgi ==
					// true)
					// link the
					// processing
					// node
					// to the SGIed
					// pattern
					// each node can
					// have
					// multiple node
					// links
					itWasBisimulated = true;

					if (DummyProperties.debugMode) {
						System.out.println("BATCH: simultanous siblings: add node link from "
								+ tempProcessingNode.getData().getPatternLatticeNodeIndex() + " to "
								+ LatticeSibling.getData().getPatternLatticeNodeIndex());
					}
					addIncNodeLink(tempProcessingNode, LatticeSibling, this);

					// }

					break;
					// so, this child
					// doesn't
					// need any
					// from here.
				}
			}

			if (!itWasBisimulated
					&& this.labelAdjacencyIndexer.latticeNodesOfALevel.containsKey(tempProcessingNode.getLevel() + 1)) {

				for (LatticeNode<ILatticeNodeData> latticeNode : this.labelAdjacencyIndexer.latticeNodesOfALevel
						.get(tempProcessingNode.getLevel() + 1)) {

					if (DualSimulationHandler.isBiDualSimulated(newPatternGraph, latticeNode, this)) {

						if (DummyProperties.debugMode) {
							System.out.println("BATCH: sameLevels: add node link from "
									+ tempProcessingNode.getData().getPatternLatticeNodeIndex() + " to "
									+ latticeNode.getData().getPatternLatticeNodeIndex());
						}

						itWasBisimulated = true;
						addIncNodeLink(tempProcessingNode, latticeNode, this);

						// }

						break;
					}
				}
			}
		}

		if (!itWasBisimulated)

		{
			// HashSet<Integer> newNodeIds = new HashSet<Integer>();
			// newNodeIds.add(newNodeId);
			LatticeNode<ILatticeNodeData> newChild = createNewLatticeNode(tempProcessingNode, newPatternGraph,
					srcPatternNode, destPatternNode.destPatternNode, newNodeIds, newCreatedOrTouchedPTNodes,
					relationshipType, destStepsFromRoot, snapshot, newChildrenOfTheSrc, false, direction,
					expansionSide);

			if (!destPatternNode.goBackToPrev)
				newlySeenPatternNodeForThisSrc.put(destPatternNode.destPatternNode.getType()
						+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType, newChild);
			else
				seenPatternNodeFromPreviousNodesForThisSrc.put(destPatternNode.destPatternNode, newChild);
		}

	}

	private boolean thisTypeExistedBeforeInThePattern(LatticeNode<ILatticeNodeData> tempProcessingNode,
			String destLabel) {
		for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
			if (patternNode.getLabel().equals(destLabel)) {
				return true;
			}
		}
		return false;

	}

	private void processQueueNode(GraphDatabaseService dataGraph, int latticeProcessingLevel,
			LatticeNode<ILatticeNodeData> tempProcessingNode, HashSet<Integer> newCreatedOrTouchedPTNodes, int snapshot,
			ExpansionSide expansionSide) throws Exception {

		if (maxAllowedEdges <= (tempProcessingNode.getLevel() - 1)) {
			return;
		}

		// while we are inside of this method we expand the same pattern to
		// generate all the possible children

		// get the pattern
		// for all nodes in the pattern
		ILatticeNodeData tempProcessingNodeData = tempProcessingNode.getData();

		HashMap<PatternNode, HashSet<LatticeNode<ILatticeNodeData>>> newChildrenOfTheSrc = new HashMap<PatternNode, HashSet<LatticeNode<ILatticeNodeData>>>();
		for (PatternNode srcPatternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			newChildrenOfTheSrc.put(srcPatternNode, new HashSet<LatticeNode<ILatticeNodeData>>());

			// if it needs any new expansion based on its hops from the root
			if (tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) >= maxAllowedHops) {
				if (DummyProperties.debugMode)
					System.out.println("maxAllowedHops for srcAbstractPatternNode:" + srcPatternNode.getType() + ""
							+ srcPatternNode.isFocus());
				continue;
			}

			if (DummyProperties.debugMode) {
				System.out.println("srcAbstractPatternNode:" + srcPatternNode.getType() + "" + srcPatternNode.isFocus()
						+ " hashCode:" + srcPatternNode.hashCode() + " stepsFromRoot: "
						+ tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode));
			}

			// String: the destination because source are same
			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcOutgoing = new HashMap<String, LatticeNode<ILatticeNodeData>>();
			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing = new HashMap<PatternNode, LatticeNode<ILatticeNodeData>>();

			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcIncoming = new HashMap<String, LatticeNode<ILatticeNodeData>>();
			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming = new HashMap<PatternNode, LatticeNode<ILatticeNodeData>>();

			// int matchGraphIndex = -1;
			// for all match nodes in this prefix-tree node and for this src
			// pattern node
			HashSet<Integer> srcNodeIds = tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					.get(srcPatternNode);

			if (!tempProcessingNode.getData().getFoundAllFocuses()) {

				// first expandToFocuses;
				expandToFocusesFirst(tempProcessingNode, tempProcessingNodeData,
						seenPatternNodeFromPreviousNodesForThisSrcOutgoing, newlySeenPatternNodeForThisSrcOutgoing,
						newCreatedOrTouchedPTNodes, srcNodeIds, snapshot, newChildrenOfTheSrc, latticeProcessingLevel,
						srcPatternNode, seenPatternNodeFromPreviousNodesForThisSrcIncoming,
						newlySeenPatternNodeForThisSrcIncoming, expansionSide);
			}

			else {

				HashMap<String, HashSet<Integer>> seenRelTypeOutgoingSet = new HashMap<String, HashSet<Integer>>();
				for (Integer srcDataGpNodeId : srcNodeIds) {
					for (String otherNodeLabelRelType : labelAdjacencyIndexer.temporalNeighborhoodIndex
							.get(srcDataGpNodeId).keySet()) {

						// if this node type already went to the
						// otherNodeLabelRelType we are not allowed to go from
						// here:
						if (tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
								.containsKey(srcPatternNode)
								&& tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
										.get(srcPatternNode).get(otherNodeLabelRelType) != null) {
							continue;
						}

						if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId)
								.get(otherNodeLabelRelType).containsKey(Indexer.AFTER)) {
							seenRelTypeOutgoingSet.putIfAbsent(otherNodeLabelRelType, new HashSet<>());
							seenRelTypeOutgoingSet.get(otherNodeLabelRelType).add(srcDataGpNodeId);
						}
					}
				}

				// of all possible labels_reltype

				if (Dummy.DummyProperties.debugMode)
					System.out.println("outgoing expansion");

				outgoingExpansion(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
						seenPatternNodeFromPreviousNodesForThisSrcOutgoing, newlySeenPatternNodeForThisSrcOutgoing,
						newCreatedOrTouchedPTNodes, seenRelTypeOutgoingSet, snapshot, latticeProcessingLevel,
						newChildrenOfTheSrc, false, expansionSide);

				HashMap<String, HashSet<Integer>> seenRelTypeIncomingSet = new HashMap<String, HashSet<Integer>>();
				for (Integer srcDataGpNodeId : srcNodeIds) {
					for (String otherNodeLabelRelType : labelAdjacencyIndexer.temporalNeighborhoodIndex
							.get(srcDataGpNodeId).keySet()) {

						// if this node type already went to the
						// otherNodeLabelRelType we are not allowed to go from
						// here:
						if (tempProcessingNode.getData().getFrequencyOfPrevNeighborOfSameType()
								.containsKey(srcPatternNode)
								&& tempProcessingNode.getData().getFrequencyOfPrevNeighborOfSameType()
										.get(srcPatternNode).get(otherNodeLabelRelType) != null) {
							continue;
						}

						if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId)
								.get(otherNodeLabelRelType).containsKey(Indexer.BEFORE)) {
							seenRelTypeIncomingSet.putIfAbsent(otherNodeLabelRelType, new HashSet<>());
							seenRelTypeIncomingSet.get(otherNodeLabelRelType).add(srcDataGpNodeId);
						}
					}
				}

				if (Dummy.DummyProperties.debugMode)
					System.out.println("incoming expansion");

				incomingExpansion(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
						seenPatternNodeFromPreviousNodesForThisSrcIncoming, newlySeenPatternNodeForThisSrcIncoming,
						newCreatedOrTouchedPTNodes, seenRelTypeIncomingSet, snapshot, latticeProcessingLevel,
						newChildrenOfTheSrc, false, expansionSide);

			}
		}

	}

	private void expandToFocusesFirst(LatticeNode<ILatticeNodeData> tempProcessingNode,
			ILatticeNodeData tempProcessingNodeData,
			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcOutgoing,
			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcOutgoing,
			HashSet<Integer> newCreatedOrTouchedPTNodes, HashSet<Integer> srcNodeIds, int snapshot,
			HashMap<PatternNode, HashSet<LatticeNode<ILatticeNodeData>>> newChildrenOfTheSrc,
			int latticeProcessingLevel, PatternNode srcPatternNode,
			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming,
			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcIncoming,
			ExpansionSide expansionSide) throws Exception {

		HashMap<String, HashSet<Integer>> seenRelTypeOutgoingSet = new HashMap<String, HashSet<Integer>>();
		for (Integer srcDataGpNodeId : srcNodeIds) {
			for (String otherNodeLabelRelType : labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId)
					.keySet()) {

				for (String unSeenFocusType : tempProcessingNode.getData().getTypeOfUnSeenFocusNodes()) {
					if (otherNodeLabelRelType
							.startsWith(unSeenFocusType + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE))

						// if this node type already went to the
						// otherNodeLabelRelType we are not allowed to go
						// from
						// here:
						if (tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
								.containsKey(srcPatternNode)
								&& tempProcessingNode.getData().getFrequencyOfNextNeighborOfSameType()
										.get(srcPatternNode).get(otherNodeLabelRelType) != null) {
						continue;
						}

					if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId).get(otherNodeLabelRelType)
							.containsKey(Indexer.AFTER)) {
						seenRelTypeOutgoingSet.putIfAbsent(otherNodeLabelRelType, new HashSet<>());
						seenRelTypeOutgoingSet.get(otherNodeLabelRelType).add(srcDataGpNodeId);
					}
				}
			}
		}

		outgoingExpansion(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
				seenPatternNodeFromPreviousNodesForThisSrcOutgoing, newlySeenPatternNodeForThisSrcOutgoing,
				newCreatedOrTouchedPTNodes, seenRelTypeOutgoingSet, snapshot, latticeProcessingLevel,
				newChildrenOfTheSrc, true, expansionSide);

		HashMap<String, HashSet<Integer>> seenRelTypeIncomingSet = new HashMap<String, HashSet<Integer>>();
		for (Integer srcDataGpNodeId : srcNodeIds) {
			for (String otherNodeLabelRelType : labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId)
					.keySet()) {

				for (String unSeenFocusType : tempProcessingNode.getData().getTypeOfUnSeenFocusNodes()) {
					if (otherNodeLabelRelType
							.startsWith(unSeenFocusType + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE)) {

						// if this node type already went to the
						// otherNodeLabelRelType we are not allowed to go from
						// here:
						if (tempProcessingNode.getData().getFrequencyOfPrevNeighborOfSameType()
								.containsKey(srcPatternNode)
								&& tempProcessingNode.getData().getFrequencyOfPrevNeighborOfSameType()
										.get(srcPatternNode).get(otherNodeLabelRelType) != null) {
							continue;
						}

						if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId)
								.get(otherNodeLabelRelType).containsKey(Indexer.BEFORE)) {
							seenRelTypeIncomingSet.putIfAbsent(otherNodeLabelRelType, new HashSet<>());
							seenRelTypeIncomingSet.get(otherNodeLabelRelType).add(srcDataGpNodeId);
						}
					}
				}
			}
		}
		incomingExpansion(tempProcessingNode, tempProcessingNodeData, srcPatternNode,
				seenPatternNodeFromPreviousNodesForThisSrcIncoming, newlySeenPatternNodeForThisSrcIncoming,
				newCreatedOrTouchedPTNodes, seenRelTypeIncomingSet, snapshot, latticeProcessingLevel,
				newChildrenOfTheSrc, true, expansionSide);

	}

	private void incomingExpansion(LatticeNode<ILatticeNodeData> tempProcessingNode,
			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode,
			HashMap<PatternNode, LatticeNode<ILatticeNodeData>> seenPatternNodeFromPreviousNodesForThisSrcIncoming,
			HashMap<String, LatticeNode<ILatticeNodeData>> newlySeenPatternNodeForThisSrcIncoming,
			HashSet<Integer> newCreatedOrTouchedPTNodes, HashMap<String, HashSet<Integer>> seenRelTypeIncomingSet,
			int snapshot, int latticeProcessingLevel,
			HashMap<PatternNode, HashSet<LatticeNode<ILatticeNodeData>>> newChildrenOfTheSrc, boolean unSeenFocusType,
			ExpansionSide expansionSide) throws Exception {

		HashSet<Integer> sameLabelNeighborNodes = new HashSet<Integer>();

		for (String otherNodeLabelRelType : seenRelTypeIncomingSet.keySet()) {
			HashSet<Integer> srcNodeIds = seenRelTypeIncomingSet.get(otherNodeLabelRelType);
			for (Integer srcDataGpNodeId : srcNodeIds) {
				if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId).get(otherNodeLabelRelType)
						.containsKey(Indexer.BEFORE)) {
					sameLabelNeighborNodes.addAll(labelAdjacencyIndexer.temporalNeighborhoodIndex.get(srcDataGpNodeId)
							.get(otherNodeLabelRelType).get(Indexer.BEFORE).neighborNodeIds);
				}
			}

			if (unSeenFocusType) {
				sameLabelNeighborNodes.retainAll(allNodesOfFocusType
						.get(otherNodeLabelRelType.split(DummyProperties.SEPARATOR_LABEL_AND_RELTYPE)[0]));
			}

			if (sameLabelNeighborNodes == null || sameLabelNeighborNodes.size() == 0) {
				return;
			}

			int separatorIndex = otherNodeLabelRelType.lastIndexOf(Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE);
			String destLabel = otherNodeLabelRelType.substring(0, separatorIndex);
			String relationshipType = otherNodeLabelRelType.substring(separatorIndex + 1);
			Integer destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;

			ArrayList<GoBackToPrevHolder> destPatternNodes = new ArrayList<GoBackToPrevHolder>();
			getDestPatternNodeAndCheckForGoBackToPrevIncoming(destPatternNodes, tempProcessingNodeData, srcPatternNode,
					srcNodeIds, sameLabelNeighborNodes, destLabel, relationshipType, destStepsFromRoot);

			for (GoBackToPrevHolder destPatternNode : destPatternNodes) {

				destStepsFromRoot = tempProcessingNodeData.getStepsFromRootOfPatternNodes().get(srcPatternNode) + 1;
				int destOutDegree = 1;
				int outgoingToSameType = 1;

				if (destPatternNode.goBackToPrev) {
					// b1 or b3 a->b->d->b and a->b
					destStepsFromRoot = Math.min(destStepsFromRoot, tempProcessingNodeData
							.getStepsFromRootOfPatternNodes().get(destPatternNode.destPatternNode));

					destOutDegree += tempProcessingNodeData.getPatternGraph()
							.outDegreeOf(destPatternNode.destPatternNode);

					for (DefaultLabeledEdge e : tempProcessingNodeData.getPatternGraph()
							.outgoingEdgesOf(destPatternNode.destPatternNode)) {
						if (tempProcessingNodeData.getPatternGraph().getEdgeTarget(e).getLabel()
								.equals(srcPatternNode.getLabel()) && e.getType().equals(relationshipType)) {
							outgoingToSameType++;
						}
					}

				}

				HashSet<Integer> newNodeIdsMustBeRemoved = new HashSet<Integer>();
				for (Integer newNodeId : destPatternNode.newNodeIds) {
					if (destOutDegree > labelAdjacencyIndexer.dataGraphNodeInfos.get(newNodeId).outDegree
					// dataGraph.getNodeById(newNodeId)
					// .getDegree(org.neo4j.graphdb.Direction.OUTGOING)
					) {

						newNodeIdsMustBeRemoved.add(newNodeId);
					}
				}
				destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);
				newNodeIdsMustBeRemoved.clear();

				for (Integer newNodeId : destPatternNode.newNodeIds) {
					// if

					if (labelAdjacencyIndexer.temporalNeighborhoodIndex.get(newNodeId)
							.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
									+ relationshipType) != null
							&& labelAdjacencyIndexer.temporalNeighborhoodIndex.get(newNodeId)
									.get(srcPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
											+ relationshipType)
									.containsKey(Indexer.AFTER)
							&& (outgoingToSameType > labelAdjacencyIndexer.temporalNeighborhoodIndex
									.get(newNodeId).get(srcPatternNode.getLabel()
											+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
									.get(Indexer.AFTER).neighborNodeIds.size())) {
						if (DummyProperties.debugMode) {
							System.out.println("cont. outgoingToSameType: " + outgoingToSameType
									+ " prev index type in data graph:"
									+ labelAdjacencyIndexer.temporalNeighborhoodIndex
											.get(newNodeId).get(srcPatternNode.getLabel()
													+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
											.get(Indexer.AFTER).neighborNodeIds.size());
						}
						newNodeIdsMustBeRemoved.add(newNodeId);
					}
				}
				destPatternNode.newNodeIds.removeAll(newNodeIdsMustBeRemoved);

				if (destPatternNode.newNodeIds.size() == 0)
					continue;

				// finding the new node type;
				String newNodeType = null;

				// if we've seen it in this pattern
				// before...

				newNodeType = destPatternNode.destPatternNode.getType();

				LatticeNode<ILatticeNodeData> seenLatticeNode = null;
				if (goBackToPrev) {
					if (seenPatternNodeFromPreviousNodesForThisSrcIncoming.containsKey(destPatternNode)) {
						seenLatticeNode = seenPatternNodeFromPreviousNodesForThisSrcIncoming.get(destPatternNode);
					}
				} else {
					if (newlySeenPatternNodeForThisSrcIncoming.containsKey(
							newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)) {
						// if this expansion has
						// seen
						// before
						// add it to the group of
						// that
						// prefix-tree node
						seenLatticeNode = newlySeenPatternNodeForThisSrcIncoming
								.get(newNodeType + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType);
					}
				}

				if (seenLatticeNode != null) {
					// double start =
					// System.nanoTime();

					ILatticeNodeData latticeNodeData = seenLatticeNode.getData();

					PatternNode tempDestPatternNode = seenLatticeNode.getData().getTargetPatternNode();

					latticeNodeData.addNewMatchSet(tempDestPatternNode, destPatternNode.newNodeIds,
							this.labelAdjacencyIndexer);

					if (DummyProperties.debugMode) {
						System.out.println("prev pattern seen:");
						System.out.println(latticeNodeData.getMappedGraphString());
					}

					seenPatternNodeFromPreviousNodesForThisSrcIncoming.put(tempDestPatternNode, seenLatticeNode);

					if (newCreatedOrTouchedPTNodes != null)
						newCreatedOrTouchedPTNodes.add(seenLatticeNode.getData().getPatternLatticeNodeIndex());
				} else {

					// make a new pattern for SGI
					// checking
					// and add it as
					// a new child if possible
					ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
							DefaultLabeledEdge.class);

					for (PatternNode patternNode : tempProcessingNode.getData().getPatternGraph().vertexSet()) {
						newPatternGraph.addVertex(patternNode);
					}

					for (DefaultLabeledEdge e : tempProcessingNode.getData().getPatternGraph().edgeSet()) {
						newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
					}

					if (!newPatternGraph.vertexSet().contains(destPatternNode.destPatternNode)) {
						newPatternGraph.addVertex(destPatternNode.destPatternNode);
					}

					newPatternGraph.addEdge(destPatternNode.destPatternNode, srcPatternNode,
							new DefaultLabeledEdge(relationshipType));

					addNewChildrenOrMatches(tempProcessingNode, tempProcessingNodeData, newPatternGraph, srcPatternNode,
							destPatternNode, destPatternNode.newNodeIds, newlySeenPatternNodeForThisSrcIncoming,
							newCreatedOrTouchedPTNodes, destStepsFromRoot, relationshipType,
							seenPatternNodeFromPreviousNodesForThisSrcIncoming, snapshot,
							Dummy.DummyProperties.Direction.INCOMING, latticeProcessingLevel, newChildrenOfTheSrc,
							expansionSide);

				}
			}
		}
	}

	private LatticeNode<ILatticeNodeData> createNewLatticeNode(LatticeNode<ILatticeNodeData> tempProcessingNode,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph, PatternNode srcPatternNode,
			PatternNode destPatternNode, Set<Integer> newNodeIds, HashSet<Integer> newCreatedOrTouchedPTNodes,
			String relationshipType, Integer destStepsFromRoot, int snapshot,
			HashMap<PatternNode, HashSet<LatticeNode<ILatticeNodeData>>> newChildrenOfTheSrc, boolean isDanglingPattern,
			Direction direction, ExpansionSide expansionSide) throws Exception {

		creationOfNewLatticeNodeStart = System.nanoTime();

		if (destPatternNode.isFocus()) {
			destStepsFromRoot = 0;
			tempProcessingNode.getData().setPatternRootNode(destPatternNode);
		}

		ILatticeNodeData LatticeNodeData = new LatticeNodeDataReducedHeuristic(newPatternGraph,
				tempProcessingNode.getData().getPatternRootNodes(), tempProcessingNode.getData(),
				tempProcessingNode.getData().getMatchedNodes(), srcPatternNode, destPatternNode, newNodeIds,
				numberOfPatternsInLattice, relationshipType, destStepsFromRoot, snapshot, this.labelAdjacencyIndexer,
				isDanglingPattern, direction);

		LatticeNode<ILatticeNodeData> newChild = new LatticeNode<ILatticeNodeData>(LatticeNodeData);

		latticeNodeIndex.put(numberOfPatternsInLattice++, newChild);

		tempProcessingNode.addChild(newChild);

		// subpattern indexing
		this.labelAdjacencyIndexer.subPatternsOfAPattern.putIfAbsent(newChild.getData().getPatternLatticeNodeIndex(),
				new HashSet<>());
		this.labelAdjacencyIndexer.subPatternsOfAPattern.get(newChild.getData().getPatternLatticeNodeIndex())
				.add(tempProcessingNode.getData().getPatternLatticeNodeIndex());

		if (this.labelAdjacencyIndexer.subPatternsOfAPattern
				.get(tempProcessingNode.getData().getPatternLatticeNodeIndex()) != null) {
			this.labelAdjacencyIndexer.subPatternsOfAPattern.get(newChild.getData().getPatternLatticeNodeIndex())
					.addAll(this.labelAdjacencyIndexer.subPatternsOfAPattern
							.get(tempProcessingNode.getData().getPatternLatticeNodeIndex()));
		}

		if (newCreatedOrTouchedPTNodes != null)
			newCreatedOrTouchedPTNodes.add(newChild.getData().getPatternLatticeNodeIndex());

		this.labelAdjacencyIndexer.latticeNodesOfALevel.putIfAbsent(newChild.getLevel(),
				new HashSet<LatticeNode<ILatticeNodeData>>());
		this.labelAdjacencyIndexer.latticeNodesOfALevel.get(newChild.getLevel()).add(newChild);

		if (newChildrenOfTheSrc != null) {
			newChildrenOfTheSrc.putIfAbsent(srcPatternNode, new HashSet<LatticeNode<ILatticeNodeData>>());
			newChildrenOfTheSrc.get(srcPatternNode).add(newChild);
		}

		creationOfNewLatticeNodeDuration += ((System.nanoTime() - creationOfNewLatticeNodeStart) / 1e6);

		// if (numberOfPatternsInLattice == 60) {
		// System.out.println();
		// }

		if (DummyProperties.debugMode) {

			System.out.println("newChild:" + newChild.getLevel() + " edgeSet size: "
					+ newChild.getData().getPatternGraph().edgeSet().size());
			System.out.println(newChild.getData().getMappedGraphString());

			// DebugHelper.printGlobalCandidateSet(this);
		}

		// DebugHelper.printIsomorphicPatterns(this);
		return newChild;

	}

	private void getDestPatternNodeAndCheckForGoBackToPrev(ArrayList<GoBackToPrevHolder> destPatternNodes,
			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode, HashSet<Integer> srcNodeIds,
			HashSet<Integer> newNodeIds, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {

		// PatternNode destPatternNode = null;
		// goBackToPrev = false;
		HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
		remainingNodeIds.addAll(newNodeIds);
		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			if ((patternNode != srcPatternNode && patternNode.getLabel().equals(otherNodeLabel))) {

				if (tempProcessingNodeData.getPatternGraph().containsEdge(srcPatternNode, patternNode)) {
					remainingNodeIds.removeAll(tempProcessingNodeData.getMatchedNodes()
							.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
				} else {

					HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
					for (Integer newNodeId : remainingNodeIds) {
						if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
								.contains(newNodeId)) {
							goBackToPrevNodeIds.add(newNodeId);
						}
					}

					if (goBackToPrevNodeIds.size() > 0) {
						destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, patternNode));
					}

					remainingNodeIds.removeAll(goBackToPrevNodeIds);
				}

			}
		}
		// if (remainingNodeIds.size() > 0 &&
		// !Collections.disjoint(remainingNodeIds, srcNodeIds)) {
		// // handling self-loop
		// HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
		// SetView<Integer> loopNodeIds = Sets.intersection(remainingNodeIds,
		// srcNodeIds);
		// goBackToPrevNodeIds.addAll(loopNodeIds);
		// Iterator<Integer> delIdItr = remainingNodeIds.iterator();
		// while (delIdItr.hasNext()) {
		// if (loopNodeIds.contains(delIdItr.next())) {
		// delIdItr.remove();
		// }
		// }
		//
		// destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds,
		// true, srcPatternNode));
		// // goBackToPrev = true;
		// }

		if (remainingNodeIds.size() > 0 && tempProcessingNodeData.getTypeOfUnSeenFocusNodes() != null) {
			// if we can find another focus
			// node, if
			// anything
			// remaining to find.

			for (String type : tempProcessingNodeData.getTypeOfUnSeenFocusNodes()) {
				HashSet<Integer> newNodeIdForTheType = new HashSet<Integer>();
				SetView<Integer> interesected = Sets.intersection(allNodesOfFocusType.get(type), newNodeIds);
				newNodeIdForTheType.addAll(interesected);
				remainingNodeIds.removeAll(interesected);
				if (newNodeIdForTheType.size() > 0) {
					destPatternNodes.add(new GoBackToPrevHolder(newNodeIdForTheType, false,
							new PatternNode(otherNodeLabel, The_Focus_Node)));
				}
			}
		}

		// if we already found all the focus
		// nodes, all this
		// labels is not in our focus list
		if (remainingNodeIds.size() > 0) {
			destPatternNodes.add(new GoBackToPrevHolder(newNodeIds, false, new PatternNode(otherNodeLabel)));
		}

		// return destPatternNodes;
	}

	private void getDestPatternNodeAndCheckForGoBackToPrevIncoming(ArrayList<GoBackToPrevHolder> destPatternNodes,
			ILatticeNodeData tempProcessingNodeData, PatternNode srcPatternNode, HashSet<Integer> srcNodeIds,
			HashSet<Integer> newNodeIds, String otherNodeLabel, String relationshipType, Integer destStepsFromRoot) {

		// PatternNode destPatternNode = null;
		// goBackToPrev = false;
		HashSet<Integer> remainingNodeIds = new HashSet<Integer>();
		remainingNodeIds.addAll(newNodeIds);
		for (PatternNode patternNode : tempProcessingNodeData.getPatternGraph().vertexSet()) {
			if ((patternNode != srcPatternNode && patternNode.getLabel().equals(otherNodeLabel))) {

				if (tempProcessingNodeData.getPatternGraph().containsEdge(patternNode, srcPatternNode)) {
					remainingNodeIds.removeAll(tempProcessingNodeData.getMatchedNodes()
							.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
				} else {
					HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
					for (Integer newNodeId : remainingNodeIds) {
						if (tempProcessingNodeData.getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
								.contains(newNodeId)) {
							goBackToPrevNodeIds.add(newNodeId);
						}
					}
					if (goBackToPrevNodeIds.size() > 0) {
						destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds, true, patternNode));
					}

					remainingNodeIds.removeAll(goBackToPrevNodeIds);
				}
			}
		}

		// TODO: why Officer*->Officer?
		// if (remainingNodeIds.size() > 0 &&
		// !Collections.disjoint(remainingNodeIds, srcNodeIds)) {
		// // handling self-loop
		// HashSet<Integer> goBackToPrevNodeIds = new HashSet<Integer>();
		// SetView<Integer> loopNodeIds = Sets.intersection(remainingNodeIds,
		// srcNodeIds);
		// goBackToPrevNodeIds.addAll(loopNodeIds);
		// Iterator<Integer> delIdItr = remainingNodeIds.iterator();
		// while (delIdItr.hasNext()) {
		// if (loopNodeIds.contains(delIdItr.next())) {
		// delIdItr.remove();
		// }
		// }
		// destPatternNodes.add(new GoBackToPrevHolder(goBackToPrevNodeIds,
		// true, srcPatternNode));
		// }

		if (remainingNodeIds.size() > 0 && tempProcessingNodeData.getTypeOfUnSeenFocusNodes() != null) {
			// if we can find another focus
			// node, if
			// anything
			// remaining to find.
			for (String type : tempProcessingNodeData.getTypeOfUnSeenFocusNodes()) {
				HashSet<Integer> newNodeIdForTheType = new HashSet<Integer>();
				SetView<Integer> interesected = Sets.intersection(allNodesOfFocusType.get(type), newNodeIds);
				newNodeIdForTheType.addAll(interesected);
				remainingNodeIds.removeAll(interesected);
				if (newNodeIdForTheType.size() > 0) {
					destPatternNodes.add(new GoBackToPrevHolder(newNodeIdForTheType, false,
							new PatternNode(otherNodeLabel, The_Focus_Node)));
				}
			}
		}
		// if we already found all the focus
		// nodes, all this
		// labels is not in our focus list
		if (remainingNodeIds.size() > 0) {
			destPatternNodes.add(new GoBackToPrevHolder(newNodeIds, false, new PatternNode(otherNodeLabel)));
		}

	}

	private void print(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {
		ArrayList<String> absGraphEdges = new ArrayList<String>();
		String returnValue = "";

		for (DefaultLabeledEdge e : newAbsPattern.edgeSet()) {
			absGraphEdges.add((newAbsPattern.getEdgeSource(e).getType())
					// + (newAbsPattern.getEdgeSource(e).isFocus() ? "*" : "")
					+ "->" + (newAbsPattern.getEdgeTarget(e).getType())
					// + (newAbsPattern.getEdgeTarget(e).isFocus() ? "*" : "")
					+ ", ");
		}
		Collections.sort(absGraphEdges);

		for (String v : absGraphEdges) {
			returnValue += v;
		}

		if (DummyProperties.debugMode)
			System.out.println(returnValue);

	}

	public boolean preIsoChecking(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {

		// SHAYAN
		// TODO: finalize it
		// if two patterns don't have same number of nodes?
		if (abstractPatternGraph.vertexSet().size() != newAbsPattern.vertexSet().size())
			return false;

		// if two patterns don't have same number of edges?
		if (abstractPatternGraph.edgeSet().size() != newAbsPattern.edgeSet().size())
			return false;

		// TODO: degree-distribution & label distribution checking
		// before isomorphism checking

		// if they don't have same label distribution?

		// .....

		return true;

	}

	private VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsoChecker(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {

		isoTimeStart = System.nanoTime();

		VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = new VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge>(
				abstractPatternGraph, newAbsPattern, new Comparator<PatternNode>() {

					@Override
					public int compare(PatternNode v1, PatternNode v2) {
						if (v1.getType().equals(v2.getType()))
							return 0;

						return 1;
					}

				}, new Comparator<DefaultLabeledEdge>() {

					@Override
					public int compare(DefaultLabeledEdge e1, DefaultLabeledEdge e2) {
						if (e1.getType().equals(e2.getType()))
							return 0;

						return 1;
					}
				});

		isoTimeDuration += ((System.nanoTime() - isoTimeStart) / 1e6);
		numberOfRealIsoChecking++;

		return iso;

	}

	private void addIncNodeLink(LatticeNode<ILatticeNodeData> latticeNode, LatticeNode<ILatticeNodeData> temp,
			ILattice lattice) throws Exception {

		if (!latticeNode.getChildrenLinksSet().contains(temp)) {
			if (DummyProperties.debugMode) {
				System.out.println("INC: add node link from " + latticeNode.getData().getPatternLatticeNodeIndex()
						+ " to " + temp.getData().getPatternLatticeNodeIndex());
			}
			latticeNode.addNodeLink(temp);

			// subpattern indexing
			this.labelAdjacencyIndexer.subPatternsOfAPattern.putIfAbsent(temp.getData().getPatternLatticeNodeIndex(),
					new HashSet<>());
			this.labelAdjacencyIndexer.subPatternsOfAPattern.get(temp.getData().getPatternLatticeNodeIndex())
					.add(latticeNode.getData().getPatternLatticeNodeIndex());

			this.labelAdjacencyIndexer.subPatternsOfAPattern.get(temp.getData().getPatternLatticeNodeIndex()).addAll(
					this.labelAdjacencyIndexer.subPatternsOfAPattern.get(temp.getData().getPatternLatticeNodeIndex()));

			if (temp.getData().getNumberOfDistinctFocusesOverAllTimestamps() > 0
					&& temp.getData().getTotalSupportFrequency() >= supportThreshold) {
				temp.getData().setCanBeFrequent(true);
			}
		}
	}

	private ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> createNewPatternFromParentPattern(
			LatticeNode<ILatticeNodeData> LatticeNode, PatternNode srcPatternNode, PatternNode destPatternNode,
			String relationshipType) {
		ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newPatternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(
				DefaultLabeledEdge.class);

		// TODO: it seems that we don't need to add vertexes first then edges
		// it seems that the second for can handle both jobs
		for (PatternNode patternNode : LatticeNode.getData().getPatternGraph().vertexSet()) {
			newPatternGraph.addVertex(patternNode);
		}

		for (DefaultLabeledEdge e : LatticeNode.getData().getPatternGraph().edgeSet()) {
			newPatternGraph.addEdge(newPatternGraph.getEdgeSource(e), newPatternGraph.getEdgeTarget(e), e);
		}

		if (!newPatternGraph.vertexSet().contains(destPatternNode)) {
			newPatternGraph.addVertex(destPatternNode);
		}

		newPatternGraph.addEdge(srcPatternNode, destPatternNode, new DefaultLabeledEdge(relationshipType));

		return newPatternGraph;
	}

	public int bfsTraverse(LatticeNode<ILatticeNodeData> rootNode) {
		if (DummyProperties.incMode)
			System.out.println("starting Lattice BFS Traversal");
		Queue<LatticeNode<ILatticeNodeData>> bfsQueue = new LinkedList<LatticeNode<ILatticeNodeData>>();
		bfsQueue.add(rootNode);
		int cnt = 0;
		while (!bfsQueue.isEmpty()) {
			LatticeNode<ILatticeNodeData> queueNode = bfsQueue.poll();
			cnt++; // root count included
			for (int i = 0; i < queueNode.getChildren().size(); i++) {
				bfsQueue.add(queueNode.getChildren().get(i));
			}

			numberOfTotalAllMatches += queueNode.getData().getNumerOfAllMatches();
			if (DummyProperties.debugMode) {
				System.out.println(queueNode.getData().getMappedGraphString());
				// System.out.print("isMaximalFrequent: " +
				// queueNode.getData().isMaximalFrequent() + ", ");
				System.out.print("isFrequent: " + queueNode.getData().isFrequent() + ", ");
				// System.out.print("isValid: " + queueNode.getData().isValid()
				// + ", ");
				System.out.println("isVisited: " + queueNode.getData().isVisited());
				// System.out.println("totalSup: " +
				// queueNode.getData().getTotalSupportFrequency());
				System.out.println();

			}
		}
		if (DummyProperties.debugMode) {
			System.out.println("number of traversed nodes: " + cnt);
			System.out.println();
		}

		return cnt;

	}

	// private void printTreeDualSim(LatticeNode<ILatticeNodeData> rootNode,
	// GraphDatabaseService graphdb,
	// LatticeReduced Lattice) {
	// Queue<LatticeNode<ILatticeNodeData>> bfsQueue = new
	// LinkedList<LatticeNode<ILatticeNodeData>>();
	// bfsQueue.add(rootNode);
	// Map<PatternNode, HashSet<Integer>> dsim;
	// while (!bfsQueue.isEmpty()) {
	// LatticeNode<ILatticeNodeData> queueNode = bfsQueue.poll();
	//
	// for (int i = 0; i < queueNode.getChildren().size(); i++) {
	// bfsQueue.add(queueNode.getChildren().get(i));
	// }
	//
	// if (queueNode.getData().getPatternGraph() != null) {
	// dsim = BatDualSimulation.run(graphdb,
	// queueNode.getData().getPatternGraph(), Lattice);
	// print(queueNode.getData().getPatternGraph());
	// System.out.print("{");
	// for (PatternNode patternNode : dsim.keySet()) {
	// System.out.print(patternNode.getType() + "=[");
	// for (Integer nodeId : dsim.get(patternNode)) {
	// System.out.print(nodeId + ",");
	// }
	// System.out.print("]");
	// }
	// System.out.println("}");
	// System.out.println();
	// }
	// // System.out.println(queueNode.getData().getMappedGraphString());
	//
	// }
	//
	// }

	public HashMap<Integer, LatticeNode<ILatticeNodeData>> getLatticeNodeIndex() {
		return this.latticeNodeIndex;
	}

	@Override
	public Indexer getLabelAdjacencyIndexer() {
		return this.labelAdjacencyIndexer;
	}

	@Override
	public GraphDatabaseService getDataGraph() {
		return this.dataGraph;
	}

	@Override
	public double getDurationOfIsoChecking() {
		return isoTimeDuration;
	}

	@Override
	public double getDurationOfNewLatticeGeneration() {
		return creationOfNewLatticeNodeDuration;
	}

	@Override
	public int getNumberOfComputeTemporalMatchSetDuration() {
		return numberOfComputeTemporalMatchSet;
	}

	@Override
	public double getDurationOfComputeTemporalMatchSet() {
		return computeTemporalMatchSetDuration;
	}

	@Override
	public void incNumberOfComputeTemporalMatchSet() {
		numberOfComputeTemporalMatchSet++;
	}

	@Override
	public void updateDurationOfComputeTemporalMatchSet(double newDuration) {
		computeTemporalMatchSetDuration += newDuration;
	}

	@Override
	public void resetNumberOfIsoChecking() {
		numberOfIsoCheckingRequest = 0;
		numberOfRealIsoChecking = 0;
	}

	@Override
	public void resetDurationOfIsoChecking() {
		isoTimeDuration = 0;

	}

	@Override
	public void resetDurationOfBiSimChecking() {
		biSimTimeDuration = 0;

	}

	@Override
	public void resetNumberOfComputeTemporalMatchSet() {
		numberOfComputeTemporalMatchSet = 0;

	}

	@Override
	public void resetDurationOfComputeTemporalMatchSet() {
		computeTemporalMatchSetDuration = 0;

	}

	@Override
	public void resetDurationOfNewLatticeGeneration() {
		creationOfNewLatticeNodeDuration = 0;

	}

	@Override
	public long getNumberOfIsoCheckingRequest() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public long getNumberOfRealIsoChecking() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsomorphism(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern) {

		return getIsoChecker(abstractPatternGraph, newAbsPattern);
	}

	@Override
	public void incrementBiSimCheckingRequest() {
		numberOfBiSimCheckingRequest++;
	}

	@Override
	public void incrementRealBiSimChecking() {
		numberOfRealBiSimChecking++;
	}

	@Override
	public void updateDurationOfBiSimChecking(double newDuration) {
		biSimTimeDuration += newDuration;
	}

	@Override
	public double getDurationOfBiSimChecking() {
		return biSimTimeDuration;
	}

	@Override
	public long getNumberOfBiSimCheckingRequest() {

		return numberOfBiSimCheckingRequest;
	}

	@Override
	public long getNumberOfRealBiSimChecking() {

		return numberOfRealBiSimChecking;
	}

	public void resetNumberIfBiSimChecking() {
		this.numberOfBiSimCheckingRequest = 0;
		this.numberOfRealBiSimChecking = 0;

	}

	class PairStrings {
		public String key;
		public String value;

		public PairStrings(String key, String value) {
			this.key = key;
			this.value = value;

		}

	}

	class PatternGraphAndPreMatches {
		public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph;
		public SetView<Integer> preMatches;
		public PatternNode danglingPatternNode;

		public PatternGraphAndPreMatches(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph,
				SetView<Integer> preMatches, PatternNode danglingPatternNode) {
			this.patternGraph = patternGraph;
			this.preMatches = preMatches;
			this.danglingPatternNode = danglingPatternNode;
		}

	}

	class MyGraphPaths {

		HashMap<PatternNode, ArrayList<MyGraphPath>> confirmedPaths = new HashMap<PatternNode, ArrayList<MyGraphPath>>();

		public HashMap<PatternNode, ArrayList<MyGraphPath>> getPaths() {
			return confirmedPaths;
		}

		public MyGraphPaths(LatticeNode<ILatticeNodeData> tempProcessingNode) {

			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> graphPattern = tempProcessingNode.getData()
					.getPatternGraph();
			HashSet<PatternNode> rootNodes = tempProcessingNode.getData().getPatternRootNodes();
			// PatternNode destNode = null;
			// if (tempProcessingNode.getData().getGrowthDirection() ==
			// Direction.OUTGOING) {
			// destNode = tempProcessingNode.getData().getTargetPatternNode();
			// } else {
			// destNode = tempProcessingNode.getData().getSourcePatternNode();
			// }

			for (PatternNode rootNode : rootNodes) {
				HashSet<PatternNode> visitedPatternNodes = new HashSet<PatternNode>();
				confirmedPaths.put(rootNode, new ArrayList<MyGraphPath>());
				LinkedList<MyGraphPath> queue = new LinkedList<MyGraphPath>();
				queue.add(new MyGraphPath(rootNode, null, new ArrayList<PatternNode>(), new ArrayList<Direction>()));
				visitedPatternNodes.add(rootNode);

				while (!queue.isEmpty()) {
					MyGraphPath node = queue.poll();
					int lastElement = node.path.size() - 1;
					boolean notHavingAnyOutgoing = true;
					boolean notHavingAnyIncoming = true;

					for (DefaultLabeledEdge e : graphPattern.outgoingEdgesOf(node.path.get(lastElement))) {
						if (!visitedPatternNodes.contains(graphPattern.getEdgeTarget(e))) {
							queue.add(new MyGraphPath(graphPattern.getEdgeTarget(e), Direction.OUTGOING, node.path,
									node.directions));
							visitedPatternNodes.add(graphPattern.getEdgeTarget(e));
							notHavingAnyOutgoing = false;
						}
					}
					for (DefaultLabeledEdge e : graphPattern.incomingEdgesOf(node.path.get(lastElement))) {
						if (!visitedPatternNodes.contains(graphPattern.getEdgeSource(e))) {
							queue.add(new MyGraphPath(graphPattern.getEdgeSource(e), Direction.INCOMING, node.path,
									node.directions));
							visitedPatternNodes.add(graphPattern.getEdgeSource(e));
							notHavingAnyIncoming = false;
						}
					}

					if (notHavingAnyIncoming && notHavingAnyOutgoing)
						confirmedPaths.get(rootNode).add(node);
				}
			}
		}
	}

	class MyGraphPath {
		ArrayList<PatternNode> path = new ArrayList<PatternNode>();
		ArrayList<Direction> directions = new ArrayList<Direction>();

		public MyGraphPath(PatternNode patternNode, Direction direction, ArrayList<PatternNode> parentPath,
				ArrayList<Direction> parentDirections) {
			this.path.addAll(parentPath);
			this.path.add(patternNode);
			this.directions.addAll(parentDirections);
			this.directions.add(direction);
		}
	}

	class SelectedMinPatternNodeWithItsPath {
		MyGraphPath myGraphPath;
		int selectedMinPatternNode;

		public SelectedMinPatternNodeWithItsPath(MyGraphPath myGraphPath, int selectedMinPatternNode) {
			this.myGraphPath = myGraphPath;
			this.selectedMinPatternNode = selectedMinPatternNode;
		}
	}
}
