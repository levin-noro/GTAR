package reducedLattice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import base.IMatchNodes;
import utilities.PatternNode;
import utilities.PatternNodeIdPair;

public class MatchNodesReduced implements IMatchNodes {
	// public DirectedGraph<DataGraphMatchNode, DefaultEdge> concreteMatchGraph;
	public HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode;
	public HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode;
	public HashMap<PatternNodeIdPair, ArrayList<Integer>> timepointsOfAMatchNodeMap;

	public HashMap<PatternNodeIdPair, ArrayList<Integer>> getTimePointsOfAMatchNodeMap() {
		return timepointsOfAMatchNodeMap;
	}

	public ArrayList<Integer> getTimePointsOfAMatch(PatternNode patternNode, Integer dataNodeId) {
		return timepointsOfAMatchNodeMap.get(new PatternNodeIdPair(patternNode, dataNodeId));
	}

	public void setTimePointsOfAMatch(PatternNode patternNode, Integer dataNodeId, ArrayList<Integer> timeIntervals) {
		this.timepointsOfAMatchNodeMap.put(new PatternNodeIdPair(patternNode, dataNodeId), timeIntervals);
	}

	// public Long dgPatternRootNodeId;
	// private boolean isDone;

	// public HashMap<PatternNode, HashSet<Integer>>
	// dataGraphCandidateNodeOfAbsPNode;
	// public HashMap<Integer, HashSet<PatternNode>>
	// patternNodeOfCandidateNeo4jNode;

	public HashMap<PatternNode, HashSet<Integer>> getDataGraphMatchNodeOfAbsPNode() {
		return dataGraphMatchNodeOfAbsPNode;
	}

	public HashMap<Integer, HashSet<PatternNode>> getPatternNodeOfNeo4jNode() {
		return patternNodeOfNeo4jNode;
	}

	// public HashMap<PatternNode, HashSet<Integer>>
	// getDataGraphCandidateNodeOfAbsPNode() {
	// return dataGraphCandidateNodeOfAbsPNode;
	// }

	// public HashMap<Integer, HashSet<PatternNode>>
	// getPatternNodeOfCandidateNeo4jNode() {
	// return patternNodeOfCandidateNeo4jNode;
	// }

	public MatchNodesReduced(HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode,
			HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode,
			HashMap<PatternNodeIdPair, ArrayList<Integer>> timepointsOfAMatchNodeMap) {
		this.dataGraphMatchNodeOfAbsPNode = dataGraphMatchNodeOfAbsPNode;
		this.patternNodeOfNeo4jNode = patternNodeOfNeo4jNode;
		this.timepointsOfAMatchNodeMap = timepointsOfAMatchNodeMap;

	}

	public String getDataGraphMatchOfAbsPNode() {
		String returnValue = "";
		for (PatternNode pNode : dataGraphMatchNodeOfAbsPNode.keySet()) {
			returnValue += pNode + "--> " + dataGraphMatchNodeOfAbsPNode.get(pNode) + "\n";
		}
		return returnValue;
	}

	public void setDataGraphMatchNodeOfAbsPNode(HashMap<PatternNode, HashSet<Integer>> result) {
		this.dataGraphMatchNodeOfAbsPNode = result;
		// this.patternNodeOfNeo4jNode = new HashMap<>();

	}

	@Override
	public void removeTimePointsOfMatches(PatternNode ptNode, HashSet<Integer> nodeIdsToBeRemoved) {
		for (Integer nodeId : nodeIdsToBeRemoved) {
			this.timepointsOfAMatchNodeMap.remove(new PatternNodeIdPair(ptNode, nodeId));
		}
	}

	// public void printPatternNodeOfNeo4jNode() {
	// DummyFunctions.printIfDebug("printPatternNodeOfNeo4jNode");
	// for (Long nodeId : patternNodeOfNeo4jNode.keySet()) {
	// DummyFunctions.printIfDebug(nodeId + " --> " +
	// patternNodeOfNeo4jNode.get(nodeId).getType() + "_"
	// + patternNodeOfNeo4jNode.get(nodeId).isFocus());
	// }
	// }

}
