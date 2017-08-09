package statDualSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;

import base.ILattice;
import base.ILatticeNodeData;
import utilities.Dummy.DummyProperties;
import utilities.LatticeNode;
import utilities.PatternNode;
import utilities.PatternNodeIdPair;

public class StatDualSimulationHandler {

	public static void computeMatchSetOfAPatternStat(GraphDatabaseService dataGraph,
			LatticeNode<ILatticeNodeData> tempProcessingNode, ILattice lattice, int minTimestamp, int maxTimestamp)
			throws Exception {

		if (tempProcessingNode.getData().isVerified()) {
			// System.err.println("it's verified already but called for compute
			// match set of a pattern!");
			return;
		}

		lattice.incNumberOfComputeTemporalMatchSet();

		double startTime = System.nanoTime();

		// if (!tempProcessingNode.getData().isValid()) {
		// throw new Exception(
		// "non-valid patterns shouldn't be check for dual-sim, because they
		// lose other match nodes!");
		// }

		tempProcessingNode.getData().setVerified(true);

		Map<PatternNode, HashSet<Integer>> dsim = cutomizedMatchListDualSimStat(tempProcessingNode, dataGraph, lattice,
				minTimestamp, maxTimestamp);

		// compute support - should be in method
		// double count = 0;
		// HashMap<Integer, HashSet<Integer>> focusNodesOfTimePoint = new
		// HashMap<Integer, HashSet<Integer>>();
		HashSet<Integer> distinctFocusMatches = new HashSet<Integer>();

		// if (tempProcessingNode.getData().getPatternLatticeNodeIndex() == 3) {
		// System.out.println();
		// }
		double totalOccurrences = 0d;
		for (PatternNode ptNode : dsim.keySet()) {
			if (ptNode.isFocus()) {
				distinctFocusMatches.addAll(dsim.get(ptNode));

				for (Integer dataNodeId : dsim.get(ptNode)) {
					ArrayList<Integer> availableTimepoints = tempProcessingNode.getData().getMatchedNodes()
							.getTimePointsOfAMatch(ptNode, dataNodeId);
					for (int i = 0; i < (availableTimepoints.size() - 1); i++) {
						totalOccurrences += (availableTimepoints.get(i + 1) - availableTimepoints.get(i)) + 1;
					}
				}

			}
		}

		tempProcessingNode.getData().setTotalSupportFrequency((double) totalOccurrences
				/ (double) (DummyProperties.NUMBER_OF_ALL_FOCUS_NODES * DummyProperties.NUMBER_OF_SNAPSHOTS));

		tempProcessingNode.getData().setNumberOfDistinctFocusesOverAllTimestamps(distinctFocusMatches.size());

		// tempProcessingNode.getData().setFocusNodesOfTimePoint(focusNodesOfTimePoint);

		lattice.updateDurationOfComputeTemporalMatchSet((System.nanoTime() - startTime) / 1e6);

		// return tempProcessingNode.getData().getSupportFrequency(snapshot);
	}

	public static HashMap<PatternNode, HashSet<Integer>> cutomizedMatchListDualSimStat(
			LatticeNode<ILatticeNodeData> tempProcessingNode, GraphDatabaseService dataGraph, ILattice lattice,
			int minTimestamp, int maxTimestamp) {

		// if (tempProcessingNode.getData().getPatternLatticeNodeIndex() == 9) {
		// System.out.println();
		// }

		if (tempProcessingNode.getData().getPatternGraph().vertexSet().size() <= 1) {
			return tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();
		}

		HashMap<PatternNode, HashSet<Integer>> soFarMatchSet = new HashMap<PatternNode, HashSet<Integer>>();

		for (int t = minTimestamp; t <= maxTimestamp; t++) {

			if (DummyProperties.debugMode && (t % 1000) == 0)
				System.out.println("i: " + tempProcessingNode.getData().getPatternLatticeNodeIndex() + ", t: " + t);

			HashMap<PatternNode, HashSet<Integer>> dsim = new HashMap<PatternNode, HashSet<Integer>>();

			for (PatternNode patternNode : tempProcessingNode.getData().getMatchedNodes()
					.getDataGraphMatchNodeOfAbsPNode().keySet()) {
				dsim.putIfAbsent(patternNode, new HashSet<>());
				dsim.get(patternNode).addAll(tempProcessingNode.getData().getMatchedNodes()
						.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
			}

			dsim = StatDualSimulation.customizedMatchListStat(dataGraph, tempProcessingNode.getData().getPatternGraph(),
					dsim, lattice, t, tempProcessingNode.getData().getMatchedNodes());

			for (PatternNode patternNode : dsim.keySet()) {
				soFarMatchSet.putIfAbsent(patternNode, new HashSet<>());
				soFarMatchSet.get(patternNode).addAll(dsim.get(patternNode));

				if (patternNode.isFocus()) {
					for (Integer nodeId : dsim.get(patternNode)) {
						tempProcessingNode.getData().getMatchedNodes().getTimePointsOfAMatchNodeMap()
								.putIfAbsent(new PatternNodeIdPair(patternNode, nodeId), new ArrayList<Integer>());

						ArrayList<Integer> soFarTimes = tempProcessingNode.getData().getMatchedNodes()
								.getTimePointsOfAMatchNodeMap().get(new PatternNodeIdPair(patternNode, nodeId));

						if (soFarTimes.isEmpty()) {
							soFarTimes.add(t);
							soFarTimes.add(t);
						} else if (soFarTimes.get(soFarTimes.size() - 1) + 1 == t) {
							soFarTimes.set(soFarTimes.size() - 1, t);
						} else {
							soFarTimes.add(t);
							soFarTimes.add(t);
						}
					}
				}
			}
			// for (PatternNodeIdPair pnp :
			// tempProcessingNode.getData().getMatchedNodes().getTimePointsOfAMatchNodeMap()
			// .keySet()) {
			// if (pnp.dataNodeId == 709020) {
			// System.out.println(pnp + " -> "
			// +
			// tempProcessingNode.getData().getMatchedNodes().getTimePointsOfAMatchNodeMap().get(pnp));
			// }
			// }
		}

		tempProcessingNode.getData().getMatchedNodes().setDataGraphMatchNodeOfAbsPNode(soFarMatchSet);

		return tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();

	}

}
