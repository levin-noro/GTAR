package base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import utilities.PatternNode;
import utilities.PatternNodeIdPair;

public interface IMatchNodes {
//	public HashMap<Integer, HashSet<PatternNode>> getPatternNodeOfNeo4jNode();

	public HashMap<PatternNode, HashSet<Integer>> getDataGraphMatchNodeOfAbsPNode();

	// public HashMap<Integer, HashSet<PatternNode>>
	// getPatternNodeOfCandidateNeo4jNode();
	// public HashMap<PatternNode, HashSet<Integer>>
	// getDataGraphCandidateNodeOfAbsPNode();
	public void setDataGraphMatchNodeOfAbsPNode(HashMap<PatternNode, HashSet<Integer>> result);

	public HashMap<PatternNodeIdPair, ArrayList<Integer>> getTimePointsOfAMatchNodeMap();

	public ArrayList<Integer> getTimePointsOfAMatch(PatternNode patternNode, Integer dataNodeId);

	public void setTimePointsOfAMatch(PatternNode patternNode, Integer dataNodeId, ArrayList<Integer> iu1);

	public void removeTimePointsOfMatches(PatternNode ptNode, HashSet<Integer> hashSet);
}
