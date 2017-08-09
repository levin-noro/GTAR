package utilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import base.ILattice;
import base.ILatticeNodeData;
import dualsim.BatDualSimulation;
import utilities.Dummy.DummyProperties;

public class DualSimulationHandler {

	public static boolean isBiDualSimulated(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1,
			LatticeNode<ILatticeNodeData> latticeNode2, ILattice lattice) {

		double startTime = System.nanoTime();

		if (!preBiSimChecking(p1, latticeNode2.getData().getPatternGraph(), lattice)) {
			lattice.updateDurationOfBiSimChecking((System.nanoTime() - startTime) / 1e6);
			return false;
		}

		lattice.incrementRealBiSimChecking();
		if (isDualSim(p1, latticeNode2, lattice) && isDualSim(latticeNode2, p1, lattice)) {
			lattice.updateDurationOfBiSimChecking((System.nanoTime() - startTime) / 1e6);
			return true;
		}

		lattice.updateDurationOfBiSimChecking((System.nanoTime() - startTime) / 1e6);
		return false;
	}

	public static boolean isDualSim(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1,
			LatticeNode<ILatticeNodeData> latticeNode2, ILattice lattice) {

		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : p1.vertexSet()) {
			for (PatternNode patternNode2 : latticeNode2.getData().getPatternGraph().vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())) {
					HashSet<String> incomingRelTypes = new HashSet<String>();
					for (DefaultLabeledEdge e : p1.incomingEdgesOf(patternNode1)) {
						incomingRelTypes.add(e.getType());
					}

					if (((!latticeNode2.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode2)
							|| latticeNode2.getData().getIncomingRelTypesOfPatternNodes().get(patternNode2).size() == 0)
							&& incomingRelTypes.size() == 0)
							|| (latticeNode2.getData().getIncomingRelTypesOfPatternNodes().get(patternNode2) != null
									&& incomingRelTypes.equals(latticeNode2.getData()
											.getIncomingRelTypesOfPatternNodes().get(patternNode2).keySet()))) {
						dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
						dsim.get(patternNode1).add(patternNode2);

					}
				}
			}
		}

		if (dsim.size() == p1.vertexSet().size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation.customizedMatchList(p1,
					latticeNode2.getData().getPatternGraph(), dsim, lattice);

			if (!result.isEmpty() && result.size() == p1.vertexSet().size()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isDualSim(LatticeNode<ILatticeNodeData> latticeNode1,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p2, ILattice lattice) {

		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : latticeNode1.getData().getPatternGraph().vertexSet()) {
			for (PatternNode patternNode2 : p2.vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())) {
					HashSet<String> incomingRelTypes = new HashSet<String>();
					for (DefaultLabeledEdge e : p2.incomingEdgesOf(patternNode2)) {
						incomingRelTypes.add(e.getType());
					}

					if (((!latticeNode1.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode1)
							|| latticeNode1.getData().getIncomingRelTypesOfPatternNodes().get(patternNode1).size() == 0)
							&& incomingRelTypes.size() == 0)
							|| (latticeNode1.getData().getIncomingRelTypesOfPatternNodes().get(patternNode1) != null
									&& incomingRelTypes.equals(latticeNode1.getData()
											.getIncomingRelTypesOfPatternNodes().get(patternNode1).keySet()))) {
						dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
						dsim.get(patternNode1).add(patternNode2);

					}
				}
			}
		}

		if (dsim.size() == latticeNode1.getData().getPatternGraph().vertexSet().size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation
					.customizedMatchList(latticeNode1.getData().getPatternGraph(), p2, dsim, lattice);

			if (!result.isEmpty() && result.size() == latticeNode1.getData().getPatternGraph().vertexSet().size()) {
				return true;
			}
		}
		return false;
	}

	public static HashMap<PatternNode, HashSet<PatternNode>> getDualSimIfAny(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1, LatticeNode<ILatticeNodeData> latticeNode2,
			ILattice lattice) {
		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : p1.vertexSet()) {
			for (PatternNode patternNode2 : latticeNode2.getData().getPatternGraph().vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())) {
					HashSet<String> incomingRelTypes = new HashSet<String>();
					for (DefaultLabeledEdge e : p1.incomingEdgesOf(patternNode1)) {
						incomingRelTypes.add(e.getType());
					}

					if (!latticeNode2.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode2)
							&& incomingRelTypes.size() > 0) {
						// not a match
					} else if ((!latticeNode2.getData().getIncomingRelTypesOfPatternNodes().containsKey(patternNode2)
							&& incomingRelTypes.size() == 0)
							|| incomingRelTypes.equals(latticeNode2.getData().getIncomingRelTypesOfPatternNodes()
									.get(patternNode2).keySet())) {
						dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
						dsim.get(patternNode1).add(patternNode2);

					}
				}
			}
		}

		if (dsim.size() == p1.vertexSet().size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation.customizedMatchList(p1,
					latticeNode2.getData().getPatternGraph(), dsim, lattice);

			if (!result.isEmpty() && result.size() == p1.vertexSet().size()) {
				return result;
			}
		}
		return null;
	}

	public static void computeMatchSetOfAPattern(GraphDatabaseService dataGraph,
			LatticeNode<ILatticeNodeData> tempProcessingNode, ILattice lattice) throws Exception {

//		if (tempProcessingNode.getData().getPatternLatticeNodeIndex()==1790){
//			System.out.println();
//		}
		
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

		Map<PatternNode, HashSet<Integer>> dsim = cutomizedMatchListDualSim(tempProcessingNode, dataGraph, lattice);

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

	public static HashMap<PatternNode, HashSet<Integer>> cutomizedMatchListDualSim(
			LatticeNode<ILatticeNodeData> tempProcessingNode, GraphDatabaseService dataGraph, ILattice lattice) {

		// if (tempProcessingNode.getData().getPatternLatticeNodeIndex() == 9) {
		// System.out.println();
		// }

		if (tempProcessingNode.getData().getPatternGraph().vertexSet().size() <= 1) {
			return tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();
		}

		tempProcessingNode.getData().getMatchedNodes().setDataGraphMatchNodeOfAbsPNode(
				BatDualSimulation.customizedMatchList(dataGraph, tempProcessingNode.getData().getPatternGraph(),
						tempProcessingNode.getData().getMatchedNodes(), lattice));

		return tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();

	}

	// public static double computeSupportBiSimOpt(GraphDatabaseService
	// dataGraph,
	// LatticeNode<ILatticeNodeData> tempProcessingNode, int snapshot,
	// ILattice lattice)
	// throws Exception {
	//
	// lattice.incNumberOfComputeSupport();
	//
	// double startTime = System.nanoTime();
	//
	// if (!tempProcessingNode.getData().isValid()) {
	// throw new Exception(
	// "non-valid patterns shouldn't be check for dual-sim, because they lose
	// other match nodes!");
	// }
	//
	// tempProcessingNode.getData().setVerified(true);
	//
	// Map<PatternNode, HashSet<Integer>> dsim =
	// cutomizedMatchListDualSimBiSimOpt(tempProcessingNode, dataGraph,
	// lattice);
	//
	//
	//
	// // compute support - should be in method
	// double count = 0;
	// for (PatternNode ptNode : dsim.keySet()) {
	// if (ptNode.isFocus()) {
	// count += dsim.get(ptNode).size();
	// }
	// }
	//
	// double snapshotSupp = count /
	// Dummy.DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
	// tempProcessingNode.getData().setSupportFrequency(snapshot, snapshotSupp);
	//
	// // tempProcessingNode.getData().snapshotUB[snapshot] = snapshotSupp;
	// // tempProcessingNode.getData().setTotalUpperbound(snapshot);
	//
	// if (count == 0) {
	// tempProcessingNode.getData().setPatternAsInvalid(tempProcessingNode,
	// lattice, snapshot);
	// }
	//
	// lattice.updateDurationOfComputeSupport((System.nanoTime() - startTime)
	// / 1e6);
	//
	// return tempProcessingNode.getData().getSupportFrequency(snapshot);
	// }

	// public static HashMap<PatternNode, HashSet<Integer>>
	// cutomizedMatchListDualSimBiSimOpt(
	// LatticeNode<ILatticeNodeData> tempProcessingNode,
	// GraphDatabaseService dataGraph,
	// ILattice lattice) {
	//
	// HashMap<PatternNode, HashSet<Integer>> newMatchNodeOfAbsPNode = new
	// HashMap<PatternNode, HashSet<Integer>>();
	// for (PatternNode patternNode :
	// tempProcessingNode.getData().getMatchedNodes()
	// .getDataGraphCandidateNodeOfAbsPNode().keySet()) {
	// newMatchNodeOfAbsPNode.put(patternNode, new HashSet<Integer>());
	// for (Integer nodeId :
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphCandidateNodeOfAbsPNode()
	// .get(patternNode)) {
	// newMatchNodeOfAbsPNode.get(patternNode).add(nodeId);
	// }
	// }
	//
	// if (tempProcessingNode.getData().getPatternGraph().vertexSet().size() <=
	// 1) {
	// tempProcessingNode.getData().getMatchedNodes().setDataGraphMatchNodeOfAbsPNode(newMatchNodeOfAbsPNode);
	// return
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphCandidateNodeOfAbsPNode();
	// }
	//
	// tempProcessingNode.getData().getMatchedNodes()
	// .setDataGraphMatchNodeOfAbsPNode(BatDualSimulation.customizedMatchList(dataGraph,
	// tempProcessingNode.getData().getPatternGraph(), newMatchNodeOfAbsPNode,
	// lattice));
	//
	// for (PatternNode patternNode :
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
	// .keySet()) {
	// for (Integer nodeId :
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
	// .get(patternNode)) {
	// tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().putIfAbsent(nodeId,
	// new HashSet<PatternNode>());
	// tempProcessingNode.getData().getMatchedNodes().getPatternNodeOfNeo4jNode().get(nodeId).add(patternNode);
	// }
	// }
	//
	// return
	// tempProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode();
	//
	// }

	public static boolean preBiSimChecking(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p1,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> p2, ILattice lattice) {

		lattice.incrementBiSimCheckingRequest();

		HashSet<String> allTypesOf1V = new HashSet<String>();

		for (PatternNode patternNode1 : p1.vertexSet()) {
			allTypesOf1V.add(patternNode1.getType());
		}

		HashSet<String> allTypesOf2V = new HashSet<String>();
		for (PatternNode patternNode2 : p2.vertexSet()) {
			allTypesOf2V.add(patternNode2.getType());
		}

		if (allTypesOf1V.size() != allTypesOf2V.size() || !allTypesOf1V.containsAll(allTypesOf2V)) {
			return false;
		}

		HashSet<String> allTypesOf1E = new HashSet<String>();
		for (DefaultLabeledEdge e : p1.edgeSet()) {
			allTypesOf1E.add(e.getType());
		}

		HashSet<String> allTypesOf2E = new HashSet<String>();
		for (DefaultLabeledEdge e : p2.edgeSet()) {
			allTypesOf2E.add(e.getType());
		}

		if (allTypesOf1E.size() != allTypesOf2E.size() || !allTypesOf1E.containsAll(allTypesOf2E)) {
			return false;
		}

		return true;
	}

	public static void gtarVerification2(Rule rule, int startTime, int endTime, int deltaT) {

		// TODO: optimization for upperbound estimation if the support is not
		// good after checking some common focus matches we can return

		rule.support = 0d;
		for (PatternNode focusPatternNode : rule.lhs.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			if (focusPatternNode.isFocus()) {
				HashSet<Integer> intersectedFocusMatches = new HashSet<>(rule.lhs.getData().getMatchedNodes()
						.getDataGraphMatchNodeOfAbsPNode().get(focusPatternNode).size());

				intersectedFocusMatches.addAll(
						rule.lhs.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(focusPatternNode));

				intersectedFocusMatches.retainAll(
						rule.rhs.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(focusPatternNode));

				for (Integer focusMatchId : intersectedFocusMatches) {
					if (DummyProperties.considerCoOcc) {
						rule.support += TimeIntervalsOperation.getMinOccurrencesOfTwoFocusMatches(
								rule.lhs.getData().getMatchedNodes().getTimePointsOfAMatch(focusPatternNode,
										focusMatchId),
								rule.rhs.getData().getMatchedNodes().getTimePointsOfAMatch(focusPatternNode,
										focusMatchId),
								deltaT, startTime, endTime);
					} else {
						rule.support += TimeIntervalsOperation.getMinOccurrencesOfTwoFocusMatchesExcludingCoOcc(
								rule.lhs.getData().getMatchedNodes().getTimePointsOfAMatch(focusPatternNode,
										focusMatchId),
								rule.rhs.getData().getMatchedNodes().getTimePointsOfAMatch(focusPatternNode,
										focusMatchId),
								deltaT, startTime, endTime);
					}

				}
				intersectedFocusMatches = null;
			}
		}

		rule.minOccurrencesSize = (int) Math.floor(rule.support);
		rule.support = rule.support
				/ (double) (DummyProperties.NUMBER_OF_ALL_FOCUS_NODES * DummyProperties.NUMBER_OF_SNAPSHOTS);

	}

	public static void gtarVerification(Rule rule, int startTime, int endTime, int deltaT) {
		// if (rule.lhs.getData().getPatternLatticeNodeIndex() == 174
		// && (rule.rhs.getData().getPatternLatticeNodeIndex() == 69
		// || rule.rhs.getData().getPatternLatticeNodeIndex() == 72)) {
		// System.out.println();
		// }

		int t_l = nextTimePatternGreaterThanOrEqual(rule.lhs, 0);
		int t_r = nextTimePatternGreaterThanOrEqual(rule.rhs, t_l);

		// if (rule.minOccurrences == null) {
		// rule.minOccurrences = new ArrayList<>();
		// }

		rule.support = 0d;
		while (t_l < Integer.MAX_VALUE && t_r < Integer.MAX_VALUE) {
			// create a min occ candidate
			TimepointsPair minOccCandidate = new TimepointsPair(t_l, t_r);
			minOccCandidate.t_r = t_r;

			while (t_l < minOccCandidate.t_r) {
				t_l++;
				// if P_L occured at t_l
				if (patternOccurredAtTimepoint(rule.lhs, t_l)) {
					// tighten min occ
					minOccCandidate.t_l = t_l;
				}

			}

			if ((minOccCandidate.t_r - minOccCandidate.t_l) <= deltaT) {
				// compute supp of gtar:
				int intersection = computeRuleIntersection(rule.lhs, minOccCandidate.t_l, rule.rhs,
						minOccCandidate.t_r);
				minOccCandidate.intersection = intersection;
				rule.support += (double) intersection
						/ (double) (DummyProperties.NUMBER_OF_ALL_FOCUS_NODES * DummyProperties.NUMBER_OF_SNAPSHOTS);

				// rule.minOccurrences.add(minOccCandidate);
			}

			t_l = nextTimePatternGreaterThanOrEqual(rule.lhs, minOccCandidate.t_r + 1); // >
			t_r = nextTimePatternGreaterThanOrEqual(rule.rhs, t_l);// >=
		}

	}

	private static boolean patternOccurredAtTimepoint(LatticeNode<ILatticeNodeData> latticeNode, int t) {
		if (numberOfOccOfPatternAtTimepoint(latticeNode, t) > 0) {
			return true;
		}
		return false;
	}

	private static int computeRuleIntersection(LatticeNode<ILatticeNodeData> lhsNode, int t_l,
			LatticeNode<ILatticeNodeData> rhsNode, int t_r) {
		int tempSupport = 0;

		HashSet<Integer> lhsFocuses = new HashSet<Integer>();
		HashSet<Integer> rhsFocuses = new HashSet<Integer>();

		for (PatternNode focusPatternNode : lhsNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			if (focusPatternNode.isFocus()) {
				lhsFocuses.addAll(occOfPatternAtTimepoint(lhsNode, t_l));
				rhsFocuses.addAll(occOfPatternAtTimepoint(rhsNode, t_r));

				lhsFocuses.retainAll(rhsFocuses);

				tempSupport += lhsFocuses.size();
			}
		}

		return tempSupport;
	}

	private static int numberOfOccOfPatternAtTimepoint(LatticeNode<ILatticeNodeData> latticeNode, Integer t) {

		if (latticeNode.getData().getNumberOfFocusNodesAtTimePoint(t) == null) {
			latticeNode.getData().setNumberOfFocusNodesAtTimePoint(t, occOfPatternAtTimepoint(latticeNode, t).size());
		}

		return latticeNode.getData().getNumberOfFocusNodesAtTimePoint(t);

	}

	private static HashSet<Integer> occOfPatternAtTimepoint(LatticeNode<ILatticeNodeData> latticeNode, Integer t) {
		// by search if we didn't maintain them.
		HashSet<Integer> occOfPatternAtT = new HashSet<Integer>();
		for (PatternNode focusPatternNode : latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			if (focusPatternNode.isFocus()) {
				for (Integer focusNodeId : latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
						.get(focusPatternNode)) {

					// findRelevantTimeInterval in logN:
					int endIndex = TimeIntervalsOperation.checkOccurrenceOfNodeIdAtT(latticeNode.getData()
							.getMatchedNodes().getTimePointsOfAMatch(focusPatternNode, focusNodeId), t);

					if (endIndex > -1)
						occOfPatternAtT.add(focusNodeId);
				}
			}
		}

		latticeNode.getData().setNumberOfFocusNodesAtTimePoint(t, occOfPatternAtT.size());
		return occOfPatternAtT;
	}

	// private static double
	// supportOfPatternAtTimepoint(LatticeNode<ILatticeNodeData> latticeNode,
	// Integer t) {
	//
	// double total = 0d;
	//
	// // [0 - t-1]
	// if (latticeNode.getData().getSupportAtTimePoint(t - 1) != null) {
	// total = latticeNode.getData().getSupportAtTimePoint(t - 1);
	// } else {
	// for (int i = 0; i < t; i++) {
	// int atI = occOfPatternAtTimepoint(latticeNode, i).size();
	// total += atI / DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
	// latticeNode.getData().setSupportAtTimePoint(i, total);
	// }
	// }
	//
	// // for "t"
	// if (latticeNode.getData().getSupportAtTimePoint(t) != null) {
	// total += latticeNode.getData().getSupportAtTimePoint(t);
	// } else {
	// int atI = occOfPatternAtTimepoint(latticeNode, t).size();
	// total += atI / DummyProperties.NUMBER_OF_ALL_FOCUS_NODES;
	// latticeNode.getData().setSupportAtTimePoint(t, total);
	// }
	//
	// return total;
	// }

	private static int nextTimePatternGreaterThanOrEqual(LatticeNode<ILatticeNodeData> latticeNode, int t) {

		for (int i = t; i < Dummy.DummyProperties.NUMBER_OF_SNAPSHOTS; i++) {
			if (numberOfOccOfPatternAtTimepoint(latticeNode, i) > 0) {
				return i;
			}
		}
		return Integer.MAX_VALUE;

		// if we maintained them --precomputed:
		// if (((double)
		// latticeNode.getData().getFocusNodesOfTimePoint().get(i).size()
		// / (double) DummyProperties.NUMBER_OF_ALL_FOCUS_NODES) > treshold) {
		// return i;
		// }

	}

	public static boolean checkIfSubPattern(ILattice lattice, HashMap<Integer, HashSet<Integer>> subPatternsOfAPattern,
			LatticeNode<ILatticeNodeData> rhsProcessingNode, LatticeNode<ILatticeNodeData> lhsProcessingNode) {

		if (rhsProcessingNode == lhsProcessingNode) {
			return true;
		}

		if (rhsProcessingNode.getLevel() >= lhsProcessingNode.getLevel()) {
			return false;
		}

		if (subPatternsOfAPattern.containsKey(lhsProcessingNode.getData().getPatternLatticeNodeIndex())
				&& subPatternsOfAPattern.get(lhsProcessingNode.getData().getPatternLatticeNodeIndex())
						.contains(rhsProcessingNode.getData().getPatternLatticeNodeIndex())) {
			return true;
		} else if (!subPatternTypeChecking(rhsProcessingNode, lhsProcessingNode)) {
			// if it doesn't have subset of node/edge types
			return false;
		} else {
			VF2SubgraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> sgi = new VF2SubgraphIsomorphismInspector<PatternNode, DefaultLabeledEdge>(
					lhsProcessingNode.getData().getPatternGraph(), rhsProcessingNode.getData().getPatternGraph(),
					new Comparator<PatternNode>() {

						@Override
						public int compare(PatternNode v1, PatternNode v2) {
							if (v1.getType().equals(v2.getType()) && v1.isFocus() == v2.isFocus())
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

			if (sgi.isomorphismExists()) {
				subPatternsOfAPattern.putIfAbsent(lhsProcessingNode.getData().getPatternLatticeNodeIndex(),
						new HashSet<>());
				subPatternsOfAPattern.get(lhsProcessingNode.getData().getPatternLatticeNodeIndex())
						.add(rhsProcessingNode.getData().getPatternLatticeNodeIndex());
				subPatternsOfAPattern.get(lhsProcessingNode.getData().getPatternLatticeNodeIndex())
						.addAll(subPatternsOfAPattern.get(rhsProcessingNode.getData().getPatternLatticeNodeIndex()));

				return true;
			}

		}

		return false;
	}

	private static boolean subPatternTypeChecking(LatticeNode<ILatticeNodeData> rhsProcessingNode,
			LatticeNode<ILatticeNodeData> lhsProcessingNode) {

		HashSet<String> allVTypesOfSubPattern = new HashSet<String>();

		for (PatternNode patternNode1 : rhsProcessingNode.getData().getPatternGraph().vertexSet()) {
			allVTypesOfSubPattern.add(patternNode1.getType());
		}

		HashSet<String> allVTypesOfSuperPattern = new HashSet<String>();
		for (PatternNode patternNode2 : lhsProcessingNode.getData().getPatternGraph().vertexSet()) {
			allVTypesOfSuperPattern.add(patternNode2.getType());
		}

		if (allVTypesOfSubPattern.size() > allVTypesOfSuperPattern.size()
				|| !allVTypesOfSuperPattern.containsAll(allVTypesOfSubPattern)) {
			return false;
		}

		HashSet<String> allETypesOfSubPattern = new HashSet<String>();
		for (DefaultLabeledEdge e : rhsProcessingNode.getData().getPatternGraph().edgeSet()) {
			allETypesOfSubPattern.add(e.getType());
		}

		HashSet<String> allETypesOfSuperPattern = new HashSet<String>();
		for (DefaultLabeledEdge e : lhsProcessingNode.getData().getPatternGraph().edgeSet()) {
			allETypesOfSuperPattern.add(e.getType());
		}

		if (allETypesOfSubPattern.size() > allETypesOfSuperPattern.size()
				|| !allETypesOfSuperPattern.containsAll(allETypesOfSubPattern)) {
			return false;
		}

		return true;
	}

	public static void computeConfidence(Rule rule) {
		rule.confidence = Math.min(1.0, (rule.support / rule.lhs.getData().getTotalSupportFrequency()));
	}

	public static boolean checkIfSubPatternOfACurrentRule(ILattice lattice,
			HashMap<Integer, HashSet<Integer>> subPatternsOfAPattern, LatticeNode<ILatticeNodeData> rhsProcessingNode,
			HashSet<Rule> rulesOfThis) {

		for (Rule rule : rulesOfThis) {
			if (DualSimulationHandler.checkIfSubPattern(lattice, subPatternsOfAPattern, rhsProcessingNode, rule.rhs)) {
				return true;
			}
		}
		return false;

	}

	private static HashSet<Integer> getSetOfAllIntersectionOfTwoPatterns(
			LatticeNode<ILatticeNodeData> lhsProcessingNode, LatticeNode<ILatticeNodeData> rhsProcessingNode) {

		HashSet<Integer> allFocusMatches1 = new HashSet<Integer>();
		HashSet<Integer> allFocusMatches2 = new HashSet<Integer>();
		for (PatternNode pNode : lhsProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			if (pNode.isFocus()) {
				allFocusMatches1.addAll(
						lhsProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(pNode));
			}
		}
		for (PatternNode pNode : rhsProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			if (pNode.isFocus()) {
				allFocusMatches2.addAll(
						rhsProcessingNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(pNode));
			}
		}
		allFocusMatches1.retainAll(allFocusMatches2);
		return allFocusMatches1;

	}

	public static int getAllIntersectionOfTwoPatterns(LatticeNode<ILatticeNodeData> lhsProcessingNode,
			LatticeNode<ILatticeNodeData> rhsProcessingNode) {
		return DualSimulationHandler.getSetOfAllIntersectionOfTwoPatterns(lhsProcessingNode, rhsProcessingNode).size();

	}
}
