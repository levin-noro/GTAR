package base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.jgrapht.graph.ListenableDirectedGraph;

import utilities.DefaultLabeledEdge;
import utilities.Dummy.DummyProperties.Direction;
import utilities.Indexer;
import utilities.LatticeNode;
import utilities.PatternNode;
import utilities.Rule;

public interface ILatticeNodeData {
	// public double getTotalSupportFrequency();

	// public void setTotalSupportFrequency(double totalSupportFrequency);

	public ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> getPatternGraph();

	public void setPatternGraph(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph);

	public int getPatternLatticeNodeIndex();

	public void setPatternLatticeNodeIndex(int patternLatticeNodeIndex);

	public IMatchNodes getMatchedNodes();

	public HashMap<PatternNode, HashMap<String, Integer>> getIncomingRelTypesOfPatternNodes();

	public HashMap<PatternNode, Integer> getStepsFromRootOfPatternNodes();

	public HashMap<Integer, HashSet<PatternNode>> getPatternNodesOfStepsFromRoot();

	// public boolean isMaximalFrequent();

	// public boolean isValid();

	public boolean isVerified();

	public boolean isVisited();

	public boolean hasLHSProcessed();

	public boolean canBeFrequent();

	public PatternNode getSourcePatternNode();

	public PatternNode getTargetPatternNode();

	public String getMappedGraphString();

	public HashSet<PatternNode> getPatternRootNodes();

	public PatternNode getPatternRootNode();

	public void setPatternAsUnEvaluated();

	// public void freqToNonFreqHandling(LatticeNode<ILatticeNodeData>
	// thisNode);

	public int getNumerOfAllMatches();

	public boolean isFrequent();

	public void setVerified(boolean isVerified);

	// public Double getSupportFrequency(int snapshot);

	public void setVisited(boolean isVisited);

	public boolean getFoundAllFocuses();

	// public void setValid(boolean isValid);

	public String getRelationshipType();

	// public void updateNumberOfFrequentChildrenAndLinked(int updateValue);

	// public void addNewMatch(PatternNode destPatternNode, Integer newNodeId,
	// Indexer indexer);

	// public void setCanBeMaximalFrequent(boolean b);

	public HashSet<String> getTypeOfUnSeenFocusNodes();

	public void setFrequent(boolean isFrequent);

	public boolean canBeMaximalFrequent();

	// public Double[] getSupportFrequencies();

	public void addImmediateMatches(PatternNode possiblePatternNode, int nodeId) throws Exception;

	// public boolean isMinimalInFrequent() throws Exception;

	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfNextNeighborOfSameType();

	public HashMap<PatternNode, HashMap<String, Integer>> getFrequencyOfPrevNeighborOfSameType();

	// public boolean isCorrect();
	// public HashMap<Integer, HashSet<PatternNode>>
	// getNewUnexpandedNodesOfPatternNodes();
	// public HashMap<PatternNode, HashSet<Integer>>
	// getNewUnexpandedPatternsNodesOfNeo4jNodes();

	public boolean isDanglingPattern();

	// public void shiftSupportsValues();

	public Direction getGrowthDirection();

	public void setPatternRootNode(PatternNode targetPatternNode);

	void removePatternRootNode(PatternNode oldRootNode);

	public void addNewMatchSet(PatternNode tempDestPatternNode, HashSet<Integer> newNodeIds,
			Indexer labelAdjacencyIndexer);

	public void setFocusNodesOfTimePoint(HashMap<Integer, HashSet<Integer>> focusNodesOfTimePoint);

	HashMap<Integer, HashSet<Integer>> getFocusNodesOfTimePoint();

	public Integer getNumberOfFocusNodesAtTimePoint(int t);

	public void setNumberOfFocusNodesAtTimePoint(int t, int sizeOfFocusesAtT);

	public Integer getNumberOfDistinctFocusesOverAllTimestamps();

	public void setNumberOfDistinctFocusesOverAllTimestamps(int n);

	public PriorityQueue<LatticeNode<ILatticeNodeData>> getRHSQueue();

	public Rule generateARule(LatticeNode<ILatticeNodeData> lhsProcessingNode,
			LatticeNode<ILatticeNodeData> rhsProcessingNode, double discoveredTime);

	HashSet<Rule> getRulesOfThis();

	public void removeRule(LatticeNode<ILatticeNodeData> lhsProcessingNode, LatticeNode<ILatticeNodeData> parent);

	public HashMap<Integer, Integer> getApproxIntersectionValueOfRHS();

	public void setTotalSupportFrequency(double totalSupport);

	double getTotalSupportFrequency();

	public void addToVisitedRHS(LatticeNode<ILatticeNodeData> rhsProcessingNode);

	boolean isVisitedRHS(LatticeNode<ILatticeNodeData> rhsProcessingNode);

	public void addToRHSQueue(LatticeNode<ILatticeNodeData> rhs);

	public void removeTheRule(Rule currentRule);

	void setLHSProcessed();

	void setCanBeFrequent(boolean canBeFrequent);

	void initRHSQueueFromChildrenMaxRules(ILattice lattice, LatticeNode<ILatticeNodeData> thisNode, int maxEdges, double supportThreshold);

	//HashSet<LatticeNode<ILatticeNodeData>> getDoNotUseTheseOrTheirSubpatterns();

	public void nullRHSQueue();

	//public void nullDoNotUse();

	//public void addToDoNotUseTheseOrTheirSubpatterns(ILattice lattice, LatticeNode<ILatticeNodeData> rhs);

	// public Double getSupportAtTimePoint(int i);
	//
	// public void setSupportAtTimePoint(int i, double total);

}
