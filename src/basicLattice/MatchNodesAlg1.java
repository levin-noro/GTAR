//package basicLattice;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import base.IMatchNodes;
//import utilities.PatternNode;
//
//public class MatchNodesAlg1 implements IMatchNodes {
//	// public DirectedGraph<DataGraphMatchNode, DefaultEdge> concreteMatchGraph;
//	public HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode;
//	public HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode;
//	public HashMap<Integer, ArrayList<Integer>> timepointsOfAMatchNodeMap;
//
//	// public Long dgPatternRootNodeId;
//	// private boolean isDone;
//
//	public HashMap<PatternNode, HashSet<Integer>> getDataGraphMatchNodeOfAbsPNode() {
//		return dataGraphMatchNodeOfAbsPNode;
//	}
//
//	public HashMap<Integer, ArrayList<Integer>> getTimePointsOfAMatchNodeMap() {
//		return timepointsOfAMatchNodeMap;
//	}
//
//	public ArrayList<Integer> getTimePointsOfAMatch(Integer dataNodeId) {
//		return timepointsOfAMatchNodeMap.get(dataNodeId);
//	}
//
//	public void setTimePointsOfAMatch(Integer dataNodeId, ArrayList<Integer> timeIntervals) {
//		this.timepointsOfAMatchNodeMap.put(dataNodeId, timeIntervals);
//	}
//
//	public HashMap<Integer, HashSet<PatternNode>> getPatternNodeOfNeo4jNode() {
//		return patternNodeOfNeo4jNode;
//	}
//
//	public MatchNodesAlg1(HashMap<PatternNode, HashSet<Integer>> dataGraphMatchNodeOfAbsPNode,
//			HashMap<Integer, HashSet<PatternNode>> patternNodeOfNeo4jNode,
//			HashMap<Integer, ArrayList<Integer>> timepointsOfAMatchNodeMap) {
//		this.dataGraphMatchNodeOfAbsPNode = dataGraphMatchNodeOfAbsPNode;
//		this.patternNodeOfNeo4jNode = patternNodeOfNeo4jNode;
//		this.timepointsOfAMatchNodeMap = timepointsOfAMatchNodeMap;
//		// this.dgPatternRootNodeId = dgPatternRootNodeId;
//	}
//
//	// public void setDone() {
//	// isDone = true;
//	// }
//	//
//	// public boolean isDone() {
//	// return isDone;
//	// }
//
//	public String getDataGraphMatchOfAbsPNode() {
//		String returnValue = "";
//		for (PatternNode pNode : dataGraphMatchNodeOfAbsPNode.keySet()) {
//			returnValue += pNode + "--> " + dataGraphMatchNodeOfAbsPNode.get(pNode) + "\n";
//		}
//		return returnValue;
//	}
//
//	public void setDataGraphMatchNodeOfAbsPNode(HashMap<PatternNode, HashSet<Integer>> result) {
//		this.dataGraphMatchNodeOfAbsPNode = result;
//
//	}
//
//	// public void printPatternNodeOfNeo4jNode() {
//	// DummyFunctions.printIfDebug("printPatternNodeOfNeo4jNode");
//	// for (Long nodeId : patternNodeOfNeo4jNode.keySet()) {
//	// DummyFunctions.printIfDebug(nodeId + " --> " +
//	// patternNodeOfNeo4jNode.get(nodeId).getType() + "_"
//	// + patternNodeOfNeo4jNode.get(nodeId).isFocus());
//	// }
//	// }
//
//}
