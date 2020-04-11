package reducedLattice.statDisGTAR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import org.jgrapht.graph.ListenableDirectedGraph;

import base.ILattice;
import base.ILatticeNodeData;
import base.IMatchNodes;
import reducedLattice.MatchNodesReduced;
import utilities.CorrespondsOfSrcRelDest;
import utilities.DefaultLabeledEdge;
import utilities.DualSimulationHandler;
import utilities.Dummy.DummyProperties;
import utilities.Dummy.DummyProperties.Direction;
import utilities.Indexer;
import utilities.LatticeNode;
import utilities.PatternNode;
import utilities.PatternNodeIdPair;
import utilities.RHSQueueComparator;
import utilities.Rule;
import utilities.SourceRelDestTypeTriple;;

//TODO: make sure that "numberOfFrequentChildrenAndLinked" is maintained

//prefix-tree node data
public class LatticeNodeDataReducedStat implements ILatticeNodeData {

	int allMatchesForThisPattern = 0;
	private HashSet<PatternNode> patternRootNodes = new HashSet<PatternNode>();

	public PatternNode sourcePatternNode;
	public PatternNode targetPatternNode;
	public HashMap<PatternNode, HashMap<String, Integer>> incomingRelTypesOfPatternNodes = new HashMap<PatternNode, HashMap<String, Integer>>();
	public HashMap<PatternNode, Integer> stepsFromRootOfPatternNodes = new HashMap<PatternNode, Integer>();
	// public HashMap<Integer, HashSet<PatternNode>> patternNodesOfStepsFromRoot
	// = new HashMap<Integer, HashSet<PatternNode>>();
	public HashMap<PatternNode, HashMap<String, Integer>> frequencyOfNextNeighborOfSameType = new HashMap<PatternNode, HashMap<String, Integer>>();
	public HashMap<PatternNode, HashMap<String, Integer>> frequencyOfPrevNeighborOfSameType = new HashMap<PatternNode, HashMap<String, Integer>>();

	// from source to target we have connected through a unique relationshipType
	public String relationshipType;

	public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph = null;

	// collection of all concrete patterns:
	public MatchNodesReduced matchNodes = null;

	// public int maxHopsFromRoot = 0;

	// if it has any children with distinct focus nodes more than a value
	public boolean canBeMaximalFrequent = true;

	// if no child of this is frequent
	// public boolean isMaximalFrequent = false;

	// the first infrequent node in a branch to be MFP
	// public boolean isMinimalInFrequent = false;

	// just if satisfied the threshold condition
	public boolean isFrequent = false;

	// if we ran dual-simulation for this
	public boolean isVerified = false;

	// if it has at least one possible match set for each pattern node in it
	// public boolean isValid = true;

	// if the # of matches for same type/sibling are less than the number of
	// them
	// public boolean isCorrect = true;

	public boolean isVisited = false;

	public boolean hasLHSProcessed = false;

	public boolean isDanglingPattern = false;

	// public HashSet<LatticeNode<ILatticeNodeData>>
	// doNotUseTheseAndThierSubpatterns = new
	// HashSet<LatticeNode<ILatticeNodeData>>();

	// public int numberOfFrequentChildrenAndLinked = 0;

	// we have to maintain which focus nodes we've seen so far to not select
	// another node of same type for another focus.
	public HashSet<String> typeOfUnSeenFocusNodes;

	// public Double[] supportFrequency = new
	// Double[DummyProperties.NUMBER_OF_SNAPSHOTS];
	HashMap<Integer, HashSet<Integer>> focusNodesOfTimePoint;
	public Integer numberOfDistinctFocusesOverAllTimestamps = 0;
	HashMap<Integer, Integer> numberOfFocusNodesAtTimePoint = new HashMap<Integer, Integer>();
	// HashMap<Integer, Double> supportAtTimePoint = new HashMap<Integer,
	// Double>();
	// public LinkedList<Double> supportFrequencyWindowing = new
	// LinkedList<Double>();

	public double totalSupportFrequency = 0.0d;
	public boolean foundAllFocuses = false;

	public Integer patternLatticeNodeIndex;

	public Direction growthDirection = Direction.OUTGOING;

	/// GTAR
	public PriorityQueue<LatticeNode<ILatticeNodeData>> rhsQueue;
	public HashSet<LatticeNode<ILatticeNodeData>> visitedRHSs;
	public HashSet<Rule> rulesOfThisLHS = new HashSet<Rule>();
	HashMap<Integer, Integer> approxIntersectionValueOfRHS;

	private boolean canBeFrequent = false;

	@Override
	public double getTotalSupportFrequency() {
		return this.totalSupportFrequency;
	}

	public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> getPatternGraph() {
		return this.patternGraph;
	}

	public void setPatternGraph(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph) {
		this.patternGraph = patternGraph;
	}

	public int getPatternLatticeNodeIndex() {
		return this.patternLatticeNodeIndex;
	}

	public void setPatternLatticeNodeIndex(int patternLatticeNodeIndex) {
		this.patternLatticeNodeIndex = patternLatticeNodeIndex;
	}

	public IMatchNodes getMatchedNodes() {
		return this.matchNodes;
	}

	public HashMap<PatternNode, HashMap<String, Integer>> getIncomingRelTypesOfPatternNodes() {
		return this.incomingRelTypesOfPatternNodes;
	}

	public HashMap<PatternNode, Integer> getStepsFromRootOfPatternNodes() {
		return this.stepsFromRootOfPatternNodes;
	}

	// public boolean isMaximalFrequent() {
	// return this.isMaximalFrequent;
	// }

	// public boolean isValid() {
	// return this.isValid;
	// }

	public boolean isVerified() {
		return this.isVerified;
	}

	public boolean isVisited() {
		return this.isVisited;
	}

	public PatternNode getSourcePatternNode() {
		return this.sourcePatternNode;
	}

	public PatternNode getTargetPatternNode() {
		return this.targetPatternNode;
	}

	public boolean isFrequent() {
		return this.isFrequent;
	}

	// usage: for root node;
	public LatticeNodeDataReducedStat(Set<String> set) {
		typeOfUnSeenFocusNodes = new HashSet<String>();
		typeOfUnSeenFocusNodes = (HashSet<String>) ((HashSet<String>) set).clone();
		this.isVisited = true;
	}

	/**
	 * usage: for first born focus labels;
	 * 
	 * @param sourceAbstractPatternNode:
	 *            first born focus pattern node
	 * @param srcDataGraphPatternNodes:
	 *            focus candidates
	 * @param focusLabelSet
	 *            ??
	 * @param patternLatticeNodeIndex:
	 *            the index of this pattern in the Lattice
	 */
	public LatticeNodeDataReducedStat(PatternNode sourceAbstractPatternNode,
			HashSet<Integer> srcDataGraphPatternNodes, HashSet<String> focusLabelSet, Integer patternLatticeNodeIndex,
			Indexer labelAdjacencyIndexer) {

		labelAdjacencyIndexer.candidateSetOfAPatternNode.putIfAbsent(sourceAbstractPatternNode, new HashSet<Integer>());
		labelAdjacencyIndexer.candidateSetOfAPatternNode.get(sourceAbstractPatternNode)
				.addAll(srcDataGraphPatternNodes);

		this.patternLatticeNodeIndex = patternLatticeNodeIndex;
		this.sourcePatternNode = sourceAbstractPatternNode;
		this.patternGraph = new ListenableDirectedGraph<PatternNode, DefaultLabeledEdge>(DefaultLabeledEdge.class);
		patternGraph.addVertex(sourceAbstractPatternNode);
		stepsFromRootOfPatternNodes.put(sourceAbstractPatternNode, 0);

		// each focus label can translate to multiple data nodes.
		HashMap<PatternNode, HashSet<Integer>> dataGraphNodeOfAbsPttnNode = new HashMap<PatternNode, HashSet<Integer>>();
		HashMap<Integer, HashSet<PatternNode>> abstractPatternNodeOfNeo4jNode = new HashMap<Integer, HashSet<PatternNode>>();
		HashMap<PatternNodeIdPair, ArrayList<Integer>> timepointsOfAMatchNodeMap = new HashMap<PatternNodeIdPair, ArrayList<Integer>>();

		dataGraphNodeOfAbsPttnNode.put(sourceAbstractPatternNode, new HashSet<Integer>());

		for (Integer srcDataGraphPatternNodeId : srcDataGraphPatternNodes) {

			dataGraphNodeOfAbsPttnNode.get(sourceAbstractPatternNode).add(srcDataGraphPatternNodeId);

			abstractPatternNodeOfNeo4jNode.put(srcDataGraphPatternNodeId, new HashSet<PatternNode>());
			abstractPatternNodeOfNeo4jNode.get(srcDataGraphPatternNodeId).add(sourceAbstractPatternNode);

			timepointsOfAMatchNodeMap.put(new PatternNodeIdPair(sourceAbstractPatternNode, srcDataGraphPatternNodeId),
					new ArrayList<>());

		}

		matchNodes = new MatchNodesReduced(dataGraphNodeOfAbsPttnNode, abstractPatternNodeOfNeo4jNode,
				timepointsOfAMatchNodeMap);

		// pattern root node:
		patternRootNodes.add(sourceAbstractPatternNode);

		typeOfUnSeenFocusNodes = (HashSet<String>) focusLabelSet.clone();
		if (sourceAbstractPatternNode.isFocus()) {
			typeOfUnSeenFocusNodes.remove(sourceAbstractPatternNode.getLabel());
		}
		if (typeOfUnSeenFocusNodes.size() == 0) {
			foundAllFocuses = true;
		}

		// TODO: support?

		this.frequencyOfNextNeighborOfSameType.put(sourceAbstractPatternNode, new HashMap<String, Integer>());
		this.frequencyOfPrevNeighborOfSameType.put(sourceAbstractPatternNode, new HashMap<String, Integer>());

		getNumerOfAllMatches();
	}

	/**
	 * adding a regular prefix-tree-node
	 * 
	 * @param newAbsPattern
	 * @param patternRootNode
	 * @param parentPTNodeData
	 * @param parentMatchedNodes
	 * @param srcAbstractPatternNode
	 * @param destAbstractPatternNode
	 * @param srcDataGraphPatternNodeId
	 * @param newNodeIds
	 * @param patternLatticeNodeIndex
	 * @param freshSource
	 * @param labelAdjacencyIndexer
	 * @param direction
	 */
	public LatticeNodeDataReducedStat(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern,
			HashSet<PatternNode> patternRootNodes, ILatticeNodeData parentPTNodeData, IMatchNodes parentMatchedNodes,
			PatternNode srcAbstractPatternNode, PatternNode destAbstractPatternNode, Set<Integer> newNodeIds,
			Integer patternLatticeNodeIndex, String relationshipType, Integer destStepsFromRoot, int snapshot,
			Indexer labelAdjacencyIndexer, boolean isDanglingPattern, Direction direction) {

		relationshipType = relationshipType.intern();

		growthDirection = direction;

		this.isDanglingPattern = isDanglingPattern;
		labelAdjacencyIndexer.candidateSetOfAPatternNode.putIfAbsent(destAbstractPatternNode, new HashSet<Integer>());
		labelAdjacencyIndexer.candidateSetOfAPatternNode.get(destAbstractPatternNode).addAll(newNodeIds);

		this.patternLatticeNodeIndex = patternLatticeNodeIndex;
		basicOperation(newAbsPattern, patternRootNodes, srcAbstractPatternNode, destAbstractPatternNode,
				relationshipType);

		if (matchNodes == null) {
			HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode = new HashMap<PatternNode, HashSet<Integer>>();
			HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode = new HashMap<Integer, HashSet<PatternNode>>();
			HashMap<PatternNodeIdPair, ArrayList<Integer>> timepointsOfAMatchNodeMap = new HashMap<PatternNodeIdPair, ArrayList<Integer>>();

			this.matchNodes = new MatchNodesReduced(dataGraphMatchNodeOfAbsPNode, patternNodeOfNeo4jNode,
					timepointsOfAMatchNodeMap);
		}

		// warm-up child from parent:
		for (PatternNodeIdPair patternNodeIdPair : parentMatchedNodes.getTimePointsOfAMatchNodeMap().keySet()) {
			this.matchNodes.timepointsOfAMatchNodeMap.put(patternNodeIdPair, new ArrayList<Integer>());
			this.matchNodes.timepointsOfAMatchNodeMap.get(patternNodeIdPair)
					.addAll(parentMatchedNodes.getTimePointsOfAMatchNodeMap().get(patternNodeIdPair));
		}

		for (Integer newNodeId : newNodeIds) {
			this.matchNodes.timepointsOfAMatchNodeMap
					.putIfAbsent(new PatternNodeIdPair(destAbstractPatternNode, newNodeId), new ArrayList<Integer>());
		}

		// indexing over next type nodes
		for (PatternNode patternNode : parentPTNodeData.getFrequencyOfNextNeighborOfSameType().keySet()) {
			this.frequencyOfNextNeighborOfSameType.put(patternNode, new HashMap<String, Integer>());
			for (String nextType : parentPTNodeData.getFrequencyOfNextNeighborOfSameType().get(patternNode).keySet()) {
				this.frequencyOfNextNeighborOfSameType.get(patternNode).put(nextType,
						parentPTNodeData.getFrequencyOfNextNeighborOfSameType().get(patternNode).get(nextType));
			}
		}

		// indexing over prev type nodes
		for (PatternNode patternNode : parentPTNodeData.getFrequencyOfPrevNeighborOfSameType().keySet()) {
			this.frequencyOfPrevNeighborOfSameType.put(patternNode, new HashMap<String, Integer>());
			for (String prevType : parentPTNodeData.getFrequencyOfPrevNeighborOfSameType().get(patternNode).keySet()) {
				this.frequencyOfPrevNeighborOfSameType.get(patternNode).put(prevType,
						parentPTNodeData.getFrequencyOfPrevNeighborOfSameType().get(patternNode).get(prevType));
			}
		}

		if (direction == Direction.OUTGOING) {
			String nextType = destAbstractPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
					+ relationshipType;

			nextType = nextType.intern();

			String prevType = srcAbstractPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
					+ relationshipType;
			prevType = prevType.intern();

			this.frequencyOfPrevNeighborOfSameType.putIfAbsent(destAbstractPatternNode, new HashMap<String, Integer>());
			this.frequencyOfNextNeighborOfSameType.putIfAbsent(srcAbstractPatternNode, new HashMap<String, Integer>());

			this.frequencyOfNextNeighborOfSameType.get(srcAbstractPatternNode).putIfAbsent(nextType, 0);
			this.frequencyOfNextNeighborOfSameType.get(srcAbstractPatternNode).put(nextType,
					this.frequencyOfNextNeighborOfSameType.get(srcAbstractPatternNode).get(nextType) + 1);

			this.frequencyOfPrevNeighborOfSameType.get(destAbstractPatternNode).putIfAbsent(prevType, 0);
			this.frequencyOfPrevNeighborOfSameType.get(destAbstractPatternNode).put(prevType,
					this.frequencyOfPrevNeighborOfSameType.get(destAbstractPatternNode).get(prevType) + 1);

		} else {
			String prevType = destAbstractPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
					+ relationshipType;
			prevType = prevType.intern();

			String nextType = srcAbstractPatternNode.getLabel() + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
					+ relationshipType;
			nextType = nextType.intern();

			this.frequencyOfNextNeighborOfSameType.putIfAbsent(destAbstractPatternNode, new HashMap<String, Integer>());
			this.frequencyOfPrevNeighborOfSameType.putIfAbsent(srcAbstractPatternNode, new HashMap<String, Integer>());

			this.frequencyOfNextNeighborOfSameType.get(destAbstractPatternNode).putIfAbsent(nextType, 0);
			this.frequencyOfNextNeighborOfSameType.get(destAbstractPatternNode).put(nextType,
					this.frequencyOfNextNeighborOfSameType.get(destAbstractPatternNode).get(nextType) + 1);
			this.frequencyOfNextNeighborOfSameType.putIfAbsent(srcAbstractPatternNode, new HashMap<String, Integer>());

			this.frequencyOfPrevNeighborOfSameType.get(srcAbstractPatternNode).putIfAbsent(prevType, 0);
			this.frequencyOfPrevNeighborOfSameType.get(srcAbstractPatternNode).put(prevType,
					this.frequencyOfPrevNeighborOfSameType.get(srcAbstractPatternNode).get(prevType) + 1);

		}

		for (PatternNode patternNode : parentMatchedNodes.getDataGraphMatchNodeOfAbsPNode().keySet()) {

			this.matchNodes.dataGraphMatchNodeOfAbsPNode.put(patternNode, new HashSet<Integer>());

			for (Integer nodeId : parentMatchedNodes.getDataGraphMatchNodeOfAbsPNode().get(patternNode)) {
				this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).add(nodeId);
			}
		}
		this.matchNodes.dataGraphMatchNodeOfAbsPNode.putIfAbsent(destAbstractPatternNode, new HashSet<Integer>());

		// for (Integer nodeId :
		// parentMatchedNodes.getPatternNodeOfNeo4jNode().keySet()) {
		// this.matchNodes.patternNodeOfNeo4jNode.put(nodeId, new
		// HashSet<PatternNode>());
		//
		// for (PatternNode patternNode :
		// parentMatchedNodes.getPatternNodeOfNeo4jNode().get(nodeId)) {
		// this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(patternNode);
		// }
		//
		// if (this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).size() == 0) {
		// this.matchNodes.patternNodeOfNeo4jNode.remove(nodeId);
		// }
		//
		// }

		for (Integer newNodeId : newNodeIds) {
			this.matchNodes.patternNodeOfNeo4jNode.putIfAbsent(newNodeId, new HashSet<PatternNode>());
		}

		// for reltype parent-child data warm-up
		for (

		PatternNode patternNode : parentPTNodeData.getIncomingRelTypesOfPatternNodes().keySet()) {
			this.incomingRelTypesOfPatternNodes.put(patternNode, new HashMap<String, Integer>());
		}
		for (PatternNode patternNode : parentPTNodeData.getIncomingRelTypesOfPatternNodes().keySet()) {
			for (String relType : parentPTNodeData.getIncomingRelTypesOfPatternNodes().get(patternNode).keySet()) {
				this.incomingRelTypesOfPatternNodes.get(patternNode).put(relType,
						parentPTNodeData.getIncomingRelTypesOfPatternNodes().get(patternNode).get(relType));
			}
		}

		if (direction == Direction.OUTGOING) {
			if (!this.incomingRelTypesOfPatternNodes.containsKey(destAbstractPatternNode)) {
				this.incomingRelTypesOfPatternNodes.put(destAbstractPatternNode, new HashMap<String, Integer>());
			}

			if (!this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).containsKey(relationshipType)) {
				this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).put(relationshipType, 1);
			} else {
				this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).put(relationshipType,
						this.incomingRelTypesOfPatternNodes.get(destAbstractPatternNode).get(relationshipType) + 1);
			}
		} else {
			if (!this.incomingRelTypesOfPatternNodes.containsKey(srcAbstractPatternNode)) {
				this.incomingRelTypesOfPatternNodes.put(srcAbstractPatternNode, new HashMap<String, Integer>());
			}

			if (!this.incomingRelTypesOfPatternNodes.get(srcAbstractPatternNode).containsKey(relationshipType)) {
				this.incomingRelTypesOfPatternNodes.get(srcAbstractPatternNode).put(relationshipType, 1);
			} else {
				this.incomingRelTypesOfPatternNodes.get(srcAbstractPatternNode).put(relationshipType,
						this.incomingRelTypesOfPatternNodes.get(srcAbstractPatternNode).get(relationshipType) + 1);
			}
		}

		// for steps from root from parent
		for (PatternNode patternNode : parentPTNodeData.getStepsFromRootOfPatternNodes().keySet()) {
			this.stepsFromRootOfPatternNodes.put(patternNode,
					parentPTNodeData.getStepsFromRootOfPatternNodes().get(patternNode));
		}
		this.stepsFromRootOfPatternNodes.put(destAbstractPatternNode, destStepsFromRoot);

		for (Integer newNodeId : newNodeIds) {
			addNewMatch(destAbstractPatternNode, newNodeId, labelAdjacencyIndexer);
		}

		this.foundAllFocuses = parentPTNodeData.getFoundAllFocuses();
		if (parentPTNodeData.getTypeOfUnSeenFocusNodes() != null
				&& parentPTNodeData.getTypeOfUnSeenFocusNodes().size() > 0) {
			this.typeOfUnSeenFocusNodes = (HashSet<String>) parentPTNodeData.getTypeOfUnSeenFocusNodes().clone();
			// removing an unseen focus node if we've seen it right now:
			if (destAbstractPatternNode.isFocus()) {
				this.typeOfUnSeenFocusNodes.remove(destAbstractPatternNode.getLabel());
				if (this.typeOfUnSeenFocusNodes.size() == 0) {
					foundAllFocuses = true;
				}
			}
		}

		SourceRelDestTypeTriple key;

		if (direction == Direction.OUTGOING) {
			key = new SourceRelDestTypeTriple(srcAbstractPatternNode.getType().intern(),
					destAbstractPatternNode.getType().intern(), relationshipType.intern());
		} else {
			key = new SourceRelDestTypeTriple(destAbstractPatternNode.getType().intern(),
					srcAbstractPatternNode.getType().intern(), relationshipType.intern());
		}

		labelAdjacencyIndexer.correspondsOfSrcRelDestType.putIfAbsent(key, new CorrespondsOfSrcRelDest());

		if (direction == Direction.OUTGOING) {
			labelAdjacencyIndexer.correspondsOfSrcRelDestType.get(key).addCorresponding(this.patternLatticeNodeIndex,
					srcAbstractPatternNode, destAbstractPatternNode);
		} else {
			labelAdjacencyIndexer.correspondsOfSrcRelDestType.get(key).addCorresponding(this.patternLatticeNodeIndex,
					destAbstractPatternNode, srcAbstractPatternNode);
		}

		for (DefaultLabeledEdge e : parentPTNodeData.getPatternGraph().edgeSet()) {

			SourceRelDestTypeTriple keyInParent = new SourceRelDestTypeTriple(
					parentPTNodeData.getPatternGraph().getEdgeSource(e).getType().intern(),
					parentPTNodeData.getPatternGraph().getEdgeTarget(e).getType().intern(), e.getType().intern());

			labelAdjacencyIndexer.correspondsOfSrcRelDestType.get(keyInParent).addCorresponding(
					this.patternLatticeNodeIndex, parentPTNodeData.getPatternGraph().getEdgeSource(e),
					parentPTNodeData.getPatternGraph().getEdgeTarget(e));
		}

		getNumerOfAllMatches();
	}

	private void basicOperation(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern,
			HashSet<PatternNode> patternRootNodes, PatternNode srcAbstractPatternNode,
			PatternNode destAbstractPatternNode, String relationshipType) {

		// this first node in this pattern
		this.patternRootNodes = patternRootNodes;

		this.sourcePatternNode = srcAbstractPatternNode;
		this.targetPatternNode = destAbstractPatternNode;
		this.relationshipType = relationshipType;

		// abstract pattern
		// we have the new abs pattern here because we had to do a SGI checking
		this.patternGraph = newAbsPattern;

	}

	private void addNewMatch(PatternNode destAbstractPatternNode, Integer destDataGraphPatternNodeId,
			Indexer labelAdjacencyIndexer) {

		if (DummyProperties.debugMode
				&& this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(destAbstractPatternNode) == null) {
			System.out.println(this);
			System.out.println(this.matchNodes.dataGraphMatchNodeOfAbsPNode);
			System.out.println(this.matchNodes.patternNodeOfNeo4jNode);
			System.out.println("destOrSrcAbstractPatternNode: " + destAbstractPatternNode
					+ ", destOrSrcDataGraphPatternNodeId: " + destDataGraphPatternNodeId
					+ ", destOrSrcPatternNode hashCode: " + destAbstractPatternNode.hashCode());
		}

		this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(destAbstractPatternNode).add(destDataGraphPatternNodeId);

		if (!this.matchNodes.patternNodeOfNeo4jNode.containsKey(destDataGraphPatternNodeId)) {
			this.matchNodes.patternNodeOfNeo4jNode.put(destDataGraphPatternNodeId, new HashSet<PatternNode>());
		}

		if (!this.matchNodes.timepointsOfAMatchNodeMap
				.containsKey(new PatternNodeIdPair(destAbstractPatternNode, destDataGraphPatternNodeId))) {
			this.matchNodes.timepointsOfAMatchNodeMap
					.put(new PatternNodeIdPair(destAbstractPatternNode, destDataGraphPatternNodeId), new ArrayList<>());
		}

		this.matchNodes.patternNodeOfNeo4jNode.get(destDataGraphPatternNodeId).add(destAbstractPatternNode);

		labelAdjacencyIndexer.candidateSetOfAPatternNode.get(destAbstractPatternNode).add(destDataGraphPatternNodeId);
	}

	//
	public void addNewMatchSet(PatternNode destAbstractPatternNode, HashSet<Integer> destDataGraphPatternNodeIds,
			Indexer labelAdjacencyIndexer) {

		if (DummyProperties.debugMode
				&& this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(destAbstractPatternNode) == null) {
			System.out.println(this);
			System.out.println(this.matchNodes.dataGraphMatchNodeOfAbsPNode);
			System.out.println(this.matchNodes.patternNodeOfNeo4jNode);
			System.out.println("destOrSrcAbstractPatternNode: " + destAbstractPatternNode
					+ ", destOrSrcDataGraphPatternNodeIds: " + destDataGraphPatternNodeIds
					+ ", destOrSrcPatternNode hashCode: " + destAbstractPatternNode.hashCode());
		}

		this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(destAbstractPatternNode).addAll(destDataGraphPatternNodeIds);

		for (Integer destDataGraphPatternNodeId : destDataGraphPatternNodeIds) {
			if (!this.matchNodes.patternNodeOfNeo4jNode.containsKey(destDataGraphPatternNodeId)) {
				this.matchNodes.patternNodeOfNeo4jNode.put(destDataGraphPatternNodeId, new HashSet<PatternNode>());
			}

			this.matchNodes.patternNodeOfNeo4jNode.get(destDataGraphPatternNodeId).add(destAbstractPatternNode);

			if (!this.matchNodes.timepointsOfAMatchNodeMap
					.containsKey(new PatternNodeIdPair(destAbstractPatternNode, destDataGraphPatternNodeId))) {
				this.matchNodes.timepointsOfAMatchNodeMap.put(
						new PatternNodeIdPair(destAbstractPatternNode, destDataGraphPatternNodeId), new ArrayList<>());
			}

		}

		labelAdjacencyIndexer.candidateSetOfAPatternNode.get(destAbstractPatternNode)
				.addAll(destDataGraphPatternNodeIds);

		if (this.isVerified) {
			System.err.println("ERROR: new matchset for a verified pattern! why?");
		}

	}

	private boolean twoPatternNodesAreSame(PatternNode unexpandedPatternNode,
			LatticeNode<ILatticeNodeData> parentLatticeNode, PatternNode patternNode,
			LatticeNodeDataReducedStat LatticeNodeData) {

		if (!LatticeNodeData.incomingRelTypesOfPatternNodes.containsKey(patternNode)
				&& parentLatticeNode.getData().getIncomingRelTypesOfPatternNodes().containsKey(unexpandedPatternNode)) {
			return false;
		}

		if (patternNode.getType().equals(unexpandedPatternNode.getType())
				&& LatticeNodeData.stepsFromRootOfPatternNodes.get(patternNode)
						.equals(parentLatticeNode.getData().getStepsFromRootOfPatternNodes().get(unexpandedPatternNode))
				&& ((LatticeNodeData.incomingRelTypesOfPatternNodes.get(patternNode) == null && parentLatticeNode
						.getData().getIncomingRelTypesOfPatternNodes().get(unexpandedPatternNode) == null)
						|| LatticeNodeData.incomingRelTypesOfPatternNodes.get(patternNode).equals(parentLatticeNode
								.getData().getIncomingRelTypesOfPatternNodes().get(unexpandedPatternNode)))) {
			return true;
		}

		return false;
	}

	public HashSet<PatternNode> getPatternRootNodes() {
		return patternRootNodes;
	}

	public PatternNode getPatternRootNode() {
		return null;
	}

	public String getMappedGraphString() {

		String returnValue = " << pattern index: " + this.patternLatticeNodeIndex + " > ";

		// allMatchesForThisPattern = 0;
		// // int allCandidatesForThisPattern = 0;
		// if (this.matchNodes != null) {
		// allMatchesForThisPattern = getNumerOfAllMatches();
		// }
		// if (this.matchNodes != null) {
		// allCandidatesForThisPattern = getNumerOfAllCandidates();
		// }

		// returnValue += " allCandidatesForThisPattern: " +
		// allCandidatesForThisPattern + " , ";
		returnValue += " allMatchesForThisPattern: " + allMatchesForThisPattern + "  " + ", focuses over all:"
				+ this.getNumberOfDistinctFocusesOverAllTimestamps() + ", totalSupportFrequency:"
				+ this.totalSupportFrequency + " > \n";

		if (this.patternGraph != null) {
			ArrayList<String> absGraphEdges = new ArrayList<String>();
			for (DefaultLabeledEdge e : this.patternGraph.edgeSet()) {
				absGraphEdges.add((this.patternGraph.getEdgeSource(e).getType()) + "_"
						+ this.patternGraph.getEdgeSource(e).hashCode() + "_" + e.getType() + "->"
						+ (this.patternGraph.getEdgeTarget(e).getType()) + "_"
						+ this.patternGraph.getEdgeTarget(e).hashCode() + ", ");
			}
			Collections.sort(absGraphEdges);

			for (String e : absGraphEdges) {
				returnValue += e;
			}
			returnValue += " >> ";

			// for (PatternNode ptNode :
			// this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
			// if (ptNode.isFocus()) {
			// for (Integer focusNodeId :
			// this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(ptNode)) {
			//
			// }
			// }
			// }

			if (this.matchNodes.dataGraphMatchNodeOfAbsPNode != null) {
				returnValue += "\n matches: [ ";
				for (PatternNode patternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {

					returnValue += " " + patternNode.getType() + "_" + patternNode.hashCode() + "=>";
					int c = 0;
					returnValue += " ( ";
					for (Integer nodeId : this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode)) {
						c++;
						if (c > 20) {
							returnValue += "...";
							break;
						}
						if (this.matchNodes.timepointsOfAMatchNodeMap
								.get(new PatternNodeIdPair(patternNode, nodeId)) == null) {
							System.out.println(patternNode + " -> " + nodeId + " not found \n");
						} else {

							returnValue += (nodeId + ":" + Arrays.toString(this.matchNodes.timepointsOfAMatchNodeMap
									.get(new PatternNodeIdPair(patternNode, nodeId)).toArray())) + " , ";
						}

					}
					returnValue += ")";

				}
				returnValue += " ] ";
			}

			returnValue += "\n srcPN? " + this.sourcePatternNode;
			returnValue += " tgtPN? " + this.targetPatternNode;
			returnValue += " isVisited? " + isVisited;
			returnValue += " isVerified? " + this.isVerified;
			returnValue += " isDangling? " + this.isDanglingPattern;
			returnValue += " edgeSet size: " + this.patternGraph.edgeSet().size();
			returnValue += " vertexSet size: " + this.patternGraph.vertexSet().size();
			returnValue += " has all focuses? " + this.foundAllFocuses;
			returnValue += " direction? " + this.growthDirection;
			returnValue += " roots? " + this.patternRootNodes;

			returnValue += "\n";

		}
		return returnValue;
	}

	@Override
	public String toString() {
		return this.getMappedGraphString();
	}

	// public void setPatternAsInvalid(LatticeNode<ILatticeNodeData> thisNode,
	// ILattice Lattice, int snapshot)
	// throws Exception {
	//
	// // set pattern as an invalid pattern
	//
	// this.isValid = false;
	// this.isVerified = false;
	// this.isFrequent = false;
	// this.isMaximalFrequent = false;
	// // if it was in the border list it should be removed
	// Lattice.getMfpLatticeNodes().remove(thisNode);
	// this.canBeMaximalFrequent = false;
	//
	// // if it was in the topk list it should be removed and replaced
	// if (Lattice.getTopKFrequentPatterns() != null)
	// thisNode.getData().removeFromTopK(Lattice, thisNode);
	//
	// }

	public void setPatternAsUnEvaluated() {
		this.isVerified = false;

	}

	// public void setSupportFrequency(int snapshot, double supp, boolean
	// fromCarryOver) {
	// if (!DummyProperties.windowMode) {
	//
	// // because this the most updated version of support,
	// // otherwise we've carried over to this snapshot
	// if (fromCarryOver) {
	// this.supportFrequency[snapshot] = supp;
	// this.totalSupportFrequency = alpha * this.totalSupportFrequency + supp;
	// } else {
	// this.totalSupportFrequency -= this.supportFrequency[snapshot];
	// this.supportFrequency[snapshot] = supp;
	// this.totalSupportFrequency += supp;
	// }
	//
	// } else {
	// if (snapshot < DummyProperties.WINDOW_SIZE) {
	//// if (this.patternLatticeNodeIndex == 0) {
	//// System.out.println(this.supportFrequencyWindowing);
	//// }
	// if (fromCarryOver) {
	// this.supportFrequencyWindowing.set(snapshot, supp);
	// this.totalSupportFrequency = alpha * this.totalSupportFrequency + supp;
	// } else {
	// this.totalSupportFrequency -=
	// this.supportFrequencyWindowing.get(snapshot);
	// this.supportFrequencyWindowing.set(snapshot, supp);
	// this.totalSupportFrequency += supp;
	// }
	//
	// } else {
	// // because we are updating the last window always
	// this.totalSupportFrequency += (supp -
	// this.supportFrequencyWindowing.getLast());
	// }
	// }
	//
	// }

	/**
	 * public void setTotalUpperbound(int snapshot) {
	 * 
	 * this.totalUpperboundEstimation = 0.0d;
	 * 
	 * // until here we have a valid upperbound for each snapshot for (int i =
	 * 0; i < snapshot; i++) { this.totalUpperboundEstimation +=
	 * this.supportFrequency[i]; this.snapshotUB[i] = this.supportFrequency[i];
	 * }
	 * 
	 * this.totalUpperboundEstimation += this.snapshotUB[snapshot];
	 * 
	 * // from here we consider max possible upperbound for each snapshot for
	 * (int i = snapshot + 1; i < this.snapshotUB.length; i++) {
	 * this.snapshotUB[i] = 0; // this.totalUpperboundEstimation +=
	 * this.snapshotUB[i]; }
	 * 
	 * this.lastSeenSnapshot = snapshot;
	 * 
	 * }
	 * 
	 * 
	 * 
	 * /*
	 * 
	 * 
	 */

	// public void freqToNonFreqHandling(LatticeNode<ILatticeNodeData> thisNode)
	// {
	// this.isFrequent = false;
	//
	// }

	public int getNumerOfAllMatches() {
		allMatchesForThisPattern = 0;
		if (this.matchNodes != null && this.matchNodes.dataGraphMatchNodeOfAbsPNode != null) {
			for (PatternNode patternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
				allMatchesForThisPattern += this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).size();
			}
		}
		return allMatchesForThisPattern;
	}

	public int getNumerOfAllCandidates() {
		int allCandidatesForThisPattern = 0;
		if (this.matchNodes != null && this.matchNodes.dataGraphMatchNodeOfAbsPNode != null) {
			for (PatternNode patternNode : this.matchNodes.dataGraphMatchNodeOfAbsPNode.keySet()) {
				allCandidatesForThisPattern += this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(patternNode).size();
			}
		}
		return allCandidatesForThisPattern;
	}

	public void setVerified(boolean isVerified) {
		this.isVerified = isVerified;

	}

	public void setVisited(boolean isVisited) {
		this.isVisited = isVisited;
	}

	public boolean getFoundAllFocuses() {
		return this.foundAllFocuses;
	}

	// public void setValid(boolean isValid) {
	// this.isValid = isValid;
	//
	// }

	public String getRelationshipType() {
		return this.relationshipType;
	}

	// public void updateNumberOfFrequentChildrenAndLinked(int updateValue) {
	// this.numberOfFrequentChildrenAndLinked += updateValue;
	// }

	public void setCanBeMaximalFrequent(boolean canBeMFP) {
		this.canBeMaximalFrequent = canBeMFP;
	}

	public HashSet<String> getTypeOfUnSeenFocusNodes() {
		return this.typeOfUnSeenFocusNodes;
	}

	public void setFrequent(boolean isFrequent) {
		this.isFrequent = isFrequent;
	}

	public boolean canBeMaximalFrequent() {
		return canBeMaximalFrequent;
	}

	@Override
	public void addImmediateMatches(PatternNode possiblePatternNode, int nodeId) {

		if (!this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(possiblePatternNode).contains(nodeId)) {
			this.matchNodes.dataGraphMatchNodeOfAbsPNode.get(possiblePatternNode).add(nodeId);
			this.matchNodes.patternNodeOfNeo4jNode.putIfAbsent(nodeId, new HashSet<PatternNode>());
			this.matchNodes.patternNodeOfNeo4jNode.get(nodeId).add(possiblePatternNode);
		}

		// this.newUnexpandedNodesOfPatternNodes.putIfAbsent(nodeId, new
		// HashSet<PatternNode>());
		// this.newUnexpandedNodesOfPatternNodes.get(nodeId).add(possiblePatternNode);
		//
		// this.newUnexpandedPatternsNodesOfNeo4jNodes.putIfAbsent(possiblePatternNode,
		// new HashSet<Integer>());
		// this.newUnexpandedPatternsNodesOfNeo4jNodes.get(possiblePatternNode).add(nodeId);

	}

	// @Override
	// public boolean isMinimalInFrequent() {
	// return isMinimalInFrequent;
	// }

	@Override
	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfNextNeighborOfSameType() {
		return frequencyOfNextNeighborOfSameType;
	}

	@Override
	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfPrevNeighborOfSameType() {
		return frequencyOfPrevNeighborOfSameType;
	}

	// @Override
	// public boolean isCorrect() {
	// return this.isCorrect;
	// }

	public HashMap<Integer, HashSet<PatternNode>> getPatternNodesOfStepsFromRoot() {
		return null;
	}
	// public void addRelType(String relationshipType) {
	//
	// if (relationshipType != null) {
	// if (incomingRelTypes.containsKey(relationshipType)) {
	// incomingRelTypes.put(relationshipType,
	// incomingRelTypes.get(relationshipType) + 1);
	// } else {
	// incomingRelTypes.put(relationshipType, 1);
	// }
	// }
	//
	// }
	//
	// public void removeRelType(String relationshipType) {
	// incomingRelTypes.put(relationshipType,
	// incomingRelTypes.get(relationshipType) - 1);
	// }

	@Override
	public boolean isDanglingPattern() {
		return isDanglingPattern;
	}

	// @Override
	// public void shiftSupportsValues() {
	// this.totalSupportFrequency -= this.supportFrequencyWindowing.getFirst();
	//
	// this.supportFrequencyWindowing.removeFirst();
	// this.supportFrequencyWindowing.addLast(0d);
	// }

	@Override
	public Direction getGrowthDirection() {
		return growthDirection;
	}

	@Override
	public void setPatternRootNode(PatternNode newRootNode) {
		this.patternRootNodes.add(newRootNode);
	}

	@Override
	public void removePatternRootNode(PatternNode oldRootNode) {
		this.patternRootNodes.remove(oldRootNode);
	}

	@Override
	public void setFocusNodesOfTimePoint(HashMap<Integer, HashSet<Integer>> focusNodesOfTimePoint) {
		this.focusNodesOfTimePoint = focusNodesOfTimePoint;
	}

	@Override
	public HashMap<Integer, HashSet<Integer>> getFocusNodesOfTimePoint() {
		return this.focusNodesOfTimePoint;
	}

	@Override
	public Integer getNumberOfFocusNodesAtTimePoint(int t) {
		if (!numberOfFocusNodesAtTimePoint.containsKey(t)) {
			return null;
		} else {
			return numberOfFocusNodesAtTimePoint.get(t);
		}
	}

	@Override
	public void setNumberOfFocusNodesAtTimePoint(int t, int sizeOfFocusesAtT) {
		numberOfFocusNodesAtTimePoint.put(t, sizeOfFocusesAtT);
	}

	@Override
	public Integer getNumberOfDistinctFocusesOverAllTimestamps() {
		return numberOfDistinctFocusesOverAllTimestamps;
	}

	@Override
	public void setNumberOfDistinctFocusesOverAllTimestamps(int n) {
		numberOfDistinctFocusesOverAllTimestamps = n;
	}

	@Override
	public void initRHSQueueFromChildrenMaxRules(ILattice lattice, LatticeNode<ILatticeNodeData> thisNode,
			int maxAllowedEdges, double supportThreshold) {

		// TODO: optimize this section
		// if (thisNode.getData().getPatternLatticeNodeIndex() == 21
		// || thisNode.getData().getPatternLatticeNodeIndex() == 29) {
		// System.out.println();
		// }
		// the assumption here is that we always fixed children of a LHS as a
		// LHS.
		rhsQueue = new PriorityQueue<>(new RHSQueueComparator());
		visitedRHSs = new HashSet<>();
		approxIntersectionValueOfRHS = new HashMap<>();

		HashSet<LatticeNode<ILatticeNodeData>> suitableRHSs = new HashSet<LatticeNode<ILatticeNodeData>>();
		if (thisNode.getChildrenLinksSet() == null || !havingPotentialChildren(thisNode)) {
			rhsQueue.addAll(lattice.getLatticeNodeIndex().get(0).getChildren());
			return;
		} else {
			// Finding descendant RHS's
			HashSet<LatticeNode<ILatticeNodeData>> allDesendantRHS = new HashSet<LatticeNode<ILatticeNodeData>>();
			Queue<LatticeNode<ILatticeNodeData>> queue = new LinkedList<>();
			HashSet<LatticeNode<ILatticeNodeData>> queueSet = new HashSet<LatticeNode<ILatticeNodeData>>();
			queue.add(thisNode);
			while (!queue.isEmpty()) {
				LatticeNode<ILatticeNodeData> currentNode = queue.poll();
				queueSet.add(currentNode);

				if (!currentNode.getData().getRulesOfThis().isEmpty()) {
					for (Rule rule : currentNode.getData().getRulesOfThis()) {
						allDesendantRHS.add(rule.rhs);
					}
				}

				if (currentNode.getChildrenLinksSet() != null) {
					for (LatticeNode<ILatticeNodeData> child : currentNode.getChildrenLinksSet()) {
						if (!queueSet.contains(child))
							queue.add(child);
					}
				}
			}

			if (allDesendantRHS.isEmpty()) {
				rhsQueue.addAll(lattice.getLatticeNodeIndex().get(0).getChildren());
				return;
			}
			
			// Adding RHS's children as RHS seed
			for (LatticeNode<ILatticeNodeData> desendantRhs : allDesendantRHS) {
				if (desendantRhs.getChildrenLinksSet() != null && !desendantRhs.getChildrenLinksSet().isEmpty()) {
					for (LatticeNode<ILatticeNodeData> childRHS : desendantRhs.getChildrenLinksSet()) {
						if (childRHS.getData().getTotalSupportFrequency() > supportThreshold)
							removeSubPatternKeepSuperPattern(lattice, suitableRHSs, childRHS);
					}

				}
			}

			

			// Finding other patterns that are not supper/sub pattern of RHS's
			// Add them as RHS seed

			int level = 2;
			while (level <= (maxAllowedEdges + 1)) {

				for (Integer latticeNodeId : lattice.getLatticeNodeIndex().keySet()) {
					if (lattice.getLatticeNodeIndex().get(latticeNodeId).getLevel() == level) {
						boolean itsOkToBeThere = true;
						if (!lattice.getLatticeNodeIndex().get(latticeNodeId).getData().canBeFrequent()) {
							continue;
						}

						for (LatticeNode<ILatticeNodeData> fixedRhs : allDesendantRHS) {
							if (DualSimulationHandler.checkIfSubPattern(lattice,
									lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern,
									lattice.getLatticeNodeIndex().get(latticeNodeId), fixedRhs)
									|| DualSimulationHandler.checkIfSubPattern(lattice,
											lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, fixedRhs,
											lattice.getLatticeNodeIndex().get(latticeNodeId))) {
								itsOkToBeThere = false;
								break;
							}
						}

						if (itsOkToBeThere) {
							suitableRHSs.add(lattice.getLatticeNodeIndex().get(latticeNodeId));
						}
					}
				}
				level++;
			}

			if (suitableRHSs.size() > 0) {
				rhsQueue.addAll(suitableRHSs);
			}
			// HashSet<LatticeNode<ILatticeNodeData>> fixedRHSs = new
			// HashSet<LatticeNode<ILatticeNodeData>>();
			// suitableRHSs.clear();

		}

		// TODO: we can index subpattern superpattern checkes to avoid repeated
		// processing
		// HashSet<LatticeNode<ILatticeNodeData>>
		// shouldBeRemovedIncludingTheirAncestors = new
		// HashSet<LatticeNode<ILatticeNodeData>>();
		// shouldBeRemovedIncludingTheirAncestors.add(thisNode);
		// while (shouldBeRemovedIncludingTheirAncestors != null) {
		// LatticeNode<ILatticeNodeData> parentNode = thisNode.getParent();
		// List<LatticeNode<ILatticeNodeData>> superNodes =
		// thisNode.getSuperNodeLinks();
		//
		// if (parentNode != null) {
		// rhsPatternsToBeChecked.remove(parentNode.getData().getPatternLatticeNodeIndex());
		// shouldBeRemovedIncludingTheirAncestors.add(parentNode);
		// }
		//
		// if (superNodes != null) {
		// for (LatticeNode<ILatticeNodeData> superNode : superNodes) {
		// rhsPatternsToBeChecked.remove(superNode.getData().getPatternLatticeNodeIndex());
		// shouldBeRemovedIncludingTheirAncestors.add(superNode);
		// }
		// }
		// }

	}

	private boolean havingPotentialChildren(LatticeNode<ILatticeNodeData> thisNode) {

		for (LatticeNode<ILatticeNodeData> child : thisNode.getChildrenLinksSet()) {
			if (child.getData().canBeFrequent()) {
				return true;
			}
		}
		return false;
	}

	private void removeSubPatternKeepSuperPattern(ILattice lattice, HashSet<LatticeNode<ILatticeNodeData>> suitableRHSs,
			LatticeNode<ILatticeNodeData> rhs) {
		// rhs is a pattern that already created a maximal rule with the child
		// of current processing LHS.

		// add all rhs children due to maximal rule:
		// when we consider sth as a maximal rule we generate its children if
		// possible (size bound) and then
		// select it as a maximal rule;
		// so the children always generated before if any.
		boolean authorizedToBeThere = true;
		Iterator<LatticeNode<ILatticeNodeData>> itr = suitableRHSs.iterator();
		while (itr.hasNext()) {
			LatticeNode<ILatticeNodeData> oldRhs = itr.next();
			// if an old rhs is superpattern keep it and do not add this one
			if ((DualSimulationHandler.checkIfSubPattern(lattice,
					lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, rhs, oldRhs))) {
				authorizedToBeThere = false;
				break;
			}
			// if an new rhs is superpattern of an old one remove the old
			// one
			if (DualSimulationHandler.checkIfSubPattern(lattice,
					lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, oldRhs, rhs)) {
				itr.remove();
			}
		}

		if (authorizedToBeThere) {
			suitableRHSs.add(rhs);
		}

	}

	@Override
	public PriorityQueue<LatticeNode<ILatticeNodeData>> getRHSQueue() {
		return rhsQueue;
	}

	@Override
	public Rule generateARule(LatticeNode<ILatticeNodeData> lhsProcessingNode,
			LatticeNode<ILatticeNodeData> rhsProcessingNode, double discoveredTime) {

		Rule rule = new Rule(lhsProcessingNode, rhsProcessingNode, discoveredTime);

		rulesOfThisLHS.add(rule);
		return rule;

	}

	@Override
	public HashSet<Rule> getRulesOfThis() {
		return rulesOfThisLHS;
	}

	@Override
	public void removeRule(LatticeNode<ILatticeNodeData> lhs, LatticeNode<ILatticeNodeData> rhs) {

//		if (lhs.getData().getPatternLatticeNodeIndex() == 60 && rhs.getData().getPatternLatticeNodeIndex() == 39) {
//			System.out.println("remove1");
//		}
		Rule rule = new Rule(lhs, rhs);
		rulesOfThisLHS.remove(rule);
	}

	@Override
	public HashMap<Integer, Integer> getApproxIntersectionValueOfRHS() {
		return approxIntersectionValueOfRHS;
	}

	@Override
	public void setTotalSupportFrequency(double totalSupport) {
		this.totalSupportFrequency = totalSupport;
	}

	@Override
	public void addToVisitedRHS(LatticeNode<ILatticeNodeData> rhsProcessingNode) {
		this.visitedRHSs.add(rhsProcessingNode);
	}

	@Override
	public boolean isVisitedRHS(LatticeNode<ILatticeNodeData> rhsProcessingNode) {
		if (this.visitedRHSs.contains(rhsProcessingNode)) {
			return true;
		}
		return false;
	}

	@Override
	public void addToRHSQueue(LatticeNode<ILatticeNodeData> rhs) {
		if (!visitedRHSs.contains(rhs)) {
			this.rhsQueue.add(rhs);
		} else if (DummyProperties.debugMode) {
			System.out.println("WARNING: rhs already visited for this lhs!");
		}

	}

	@Override
	public void removeTheRule(Rule currentRule) {
//		if (currentRule.lhs.getData().getPatternLatticeNodeIndex() == 60
//				&& currentRule.rhs.getData().getPatternLatticeNodeIndex() == 39) {
//			System.out.println("remove2");
//		}
		this.rulesOfThisLHS.remove(currentRule);
	}

	@Override
	public boolean hasLHSProcessed() {
		return hasLHSProcessed;
	}

	@Override
	public void setLHSProcessed() {
		hasLHSProcessed = true;
	}

	@Override
	public boolean canBeFrequent() {
		return canBeFrequent;
	}

	@Override
	public void setCanBeFrequent(boolean canBeFrequent) {
		this.canBeFrequent = canBeFrequent;
	}

	// @Override
	// public HashSet<LatticeNode<ILatticeNodeData>>
	// getDoNotUseTheseOrTheirSubpatterns() {
	// return doNotUseTheseAndThierSubpatterns;
	// }

	@Override
	public void nullRHSQueue() {
		rhsQueue = null;
	}

	// @Override
	// public void nullDoNotUse() {
	// doNotUseTheseAndThierSubpatterns = null;
	// }

	// @Override
	// public void addToDoNotUseTheseOrTheirSubpatterns(ILattice lattice,
	// LatticeNode<ILatticeNodeData> rhs) {
	// boolean addable = true;
	// Iterator<LatticeNode<ILatticeNodeData>> oldSetItr =
	// doNotUseTheseAndThierSubpatterns.iterator();
	// while (oldSetItr.hasNext()) {
	// LatticeNode<ILatticeNodeData> oldForbiddenRHS = oldSetItr.next();
	// if (DualSimulationHandler.checkIfSubPattern(lattice,
	// lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, rhs,
	// oldForbiddenRHS)) {
	// addable = false;
	// break;
	// }
	// if (DualSimulationHandler.checkIfSubPattern(lattice,
	// lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern,
	// oldForbiddenRHS, rhs)) {
	// oldSetItr.remove();
	// }
	//
	// }
	// if (addable)
	// doNotUseTheseAndThierSubpatterns.add(rhs);
	//
	// }

	// @Override
	// public Double getSupportAtTimePoint(int t) {
	// if (!supportAtTimePoint.containsKey(t)) {
	// return null;
	// } else {
	// return supportAtTimePoint.get(t);
	// }
	// }
	//
	// @Override
	// public void setSupportAtTimePoint(int t, double total) {
	// supportAtTimePoint.put(t, total);
	// }

}

/// **
// * usage: previous seen pattern so it's without any concrete match
// *
// * @param newAbsPattern
// * @param patternRootNode
// * @param srcAbstractPatternNode
// * @param destAbstractPatternNode
// * @param relationshipType
// */
// @Deprecated
// public LatticeNodeData(ListenableDirectedGraph<PatternNode,
/// DefaultLabeledEdge> newAbsPattern,
// PatternNode patternRootNode, PatternNode srcAbstractPatternNode, PatternNode
/// destAbstractPatternNode,
// String relationshipType) {
//
// basicOperation(newAbsPattern, patternRootNode, srcAbstractPatternNode,
/// destAbstractPatternNode,
// relationshipType);
// }