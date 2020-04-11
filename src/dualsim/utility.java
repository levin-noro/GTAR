/*
This file contains methods that are used in the src.dualSimulation algorithm.
Originally written by: Peng
Modified for noe4j by: Shayan Monadjemi
June 7th, 2016
 */

package dualsim;

import org.jgrapht.DirectedGraph;
import org.neo4j.graphdb.*;

import base.IMatchNodes;
import edu.stanford.nlp.time.SUTime.Temporal;
import edu.stanford.nlp.util.Index;
import utilities.DefaultLabeledEdge;
import utilities.Dummy;
import utilities.Indexer;
import utilities.PatternNode;
import utilities.TimeIntervalsOperation;

import java.util.*;

/**
 * Simulation.
 */
public class utility {

	public static Map<Integer, HashSet<Integer>> createVerticesMap(GraphDatabaseService Q, GraphDatabaseService G) {
		Map<Integer, HashSet<Integer>> map = new HashMap<>();
		try (Transaction tx1 = Q.beginTx()) {
			for (Node u : Q.getAllNodes()) {
				try (Transaction tx2 = G.beginTx()) {

					for (Node v : G.getAllNodes()) {
						if (v.getLabels().equals(u.getLabels())) {
							map.putIfAbsent((int) u.getId(), new HashSet<Integer>());
							map.get(u.getId()).add((int) v.getId());
						}
					}
					tx2.success();
				} catch (Exception e2) {

				}
			}

			tx1.success();
		} catch (Exception e) {

		}

		return map;
	}

	public static HashMap<PatternNode, HashSet<Integer>> createVerticesMap(
			DirectedGraph<PatternNode, DefaultLabeledEdge> Q, GraphDatabaseService G) {
		HashMap<PatternNode, HashSet<Integer>> map = new HashMap<>();

		for (PatternNode u : Q.vertexSet()) {
			try (Transaction tx2 = G.beginTx()) {

				for (Node v : G.getAllNodes()) {
					Boolean mutualLabelExists = false;
					for (Label l : v.getLabels()) {
						if (l.name().equals(u.getLabel())) {
							mutualLabelExists = true;
							break;
						}
					}
					if (mutualLabelExists) {
						map.putIfAbsent(u, new HashSet<Integer>());
						map.get(u).add((int) v.getId());
					}
				}
				tx2.success();
			} catch (Exception e2) {

			}
		}

		return map;
	}

	// public static HashMap<Integer, HashSet<PatternNode>>
	// createReversedMap(HashMap<PatternNode, HashSet<Integer>> map) {
	// HashMap<Integer, HashSet<PatternNode>> revMap = new HashMap<>();
	// for (PatternNode srcId : map.keySet()) {
	// for (Integer dstId : map.get(srcId)) {
	// revMap.putIfAbsent(dstId, new HashSet<PatternNode>());
	// revMap.get(dstId).add(srcId);
	// }
	// }
	//
	// return revMap;
	// }

	public static boolean isNextSimulated(DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph1,
			Map<PatternNode, HashSet<PatternNode>> sim, PatternNode vId, PatternNode u,
			DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph2) {
		// Get the ids of the children nodes of v
		Set<PatternNode> vNextIds = new HashSet<>();

		// HashSet<PatternNode> nodeIds = new HashSet<PatternNode>();
		for (DefaultLabeledEdge e : patternGraph2.outgoingEdgesOf(vId)) {
			vNextIds.add(patternGraph2.getEdgeTarget(e));
		}

		// Get the children nodes of u
		Set<PatternNode> uNextIds = new HashSet<>();
		for (DefaultLabeledEdge e : patternGraph1.outgoingEdgesOf(u)) {
			// for all outgoing relationships of u
			uNextIds.add(patternGraph1.getEdgeTarget(e));
		}
		for (PatternNode uNext : uNextIds) {
			HashSet<PatternNode> uNextSim = sim.get(uNext);
			if (uNextSim == null || vNextIds == null) {
				System.out.println();
			}
			if (Collections.disjoint(vNextIds, uNextSim)) {
				return false;
			}
		}

		return true;
	}

	public static boolean isNextSimulated(Indexer labelAdjacencyIndexer, IMatchNodes matchNodes,
			// Map<PatternNode, HashSet<Integer>> sim,
			Integer vId, PatternNode u, DirectedGraph<PatternNode, DefaultLabeledEdge> Q) {

		// for each edge in Q : e.g. A*->B
		for (DefaultLabeledEdge e : Q.outgoingEdgesOf(u)) {
			// children of vId with the desired type
			PatternNode uNext = Q.getEdgeTarget(e);

			if (!labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.containsKey(uNext.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					|| !labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
							.get(uNext.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
							.containsKey(Indexer.AFTER)) {
				return false;
			}

			Set<Integer> vNextIds = labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.get(uNext.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					.get(Indexer.AFTER).neighborNodeIds;

			if (vNextIds.isEmpty())
				return false;

			// intersection of I_u with I_e (from temporal index) with I_u'
			HashSet<Integer> uNextSimMatches = matchNodes.getDataGraphMatchNodeOfAbsPNode().get(uNext);

			HashSet<Integer> intersection = new HashSet<Integer>(vNextIds);
			intersection.retainAll(uNextSimMatches);

			if (intersection.isEmpty())
				return false;

			ArrayList<Integer> Iu1 = matchNodes.getTimePointsOfAMatch(u, vId);

			ArrayList<Integer> Ie = labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.get(uNext.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					.get(Indexer.AFTER).Ie;

			Iu1 = TimeIntervalsOperation.intersectionOfTimeIntervals(Iu1, Ie);

			// Iu1 ==[] means -inf/+inf
			if (Iu1 == null)
				return false;

			ArrayList<Integer> Iu2 = new ArrayList<Integer>();
			for (Integer vNextId : intersection) {
				Iu2 = TimeIntervalsOperation.unionOfTimeIntervals(Iu2,
						matchNodes.getTimePointsOfAMatch(uNext, vNextId));
			}

			Iu1 = TimeIntervalsOperation.intersectionOfTimeIntervals(Iu1, Iu2);

			// Iu1 ==[] means -inf/+inf
			if (Iu1 == null)
				return false;

			matchNodes.setTimePointsOfAMatch(u, vId, Iu1);
		}

		return true;
	}

	public static boolean isPrevSimulated(DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph1,
			Map<PatternNode, HashSet<PatternNode>> sim, PatternNode vId, PatternNode u,
			DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph2) {

		// Get the ids of parent nodes v
		Set<PatternNode> vPrevIds = new HashSet<>();

		for (DefaultLabeledEdge e : patternGraph2.incomingEdgesOf(vId)) {
			vPrevIds.add(patternGraph2.getEdgeSource(e));
		}

		// HashSet<Integer> nodeIds =
		// labelAdjacencyIndexer.dataGraphNodeInfos.get(vId).prevNodeIds;
		// if (nodeIds != null)
		// vPrevIds.addAll(nodeIds);

		// Get the ids of parent nodes u
		Set<PatternNode> uPrevIds = new HashSet<>();
		for (DefaultLabeledEdge e : patternGraph1.incomingEdgesOf(u)) {
			// Incoming relationships of u
			uPrevIds.add(patternGraph1.getEdgeSource(e));
		}

		for (PatternNode uPrev : uPrevIds) {
			HashSet<PatternNode> uPrevSim = sim.get(uPrev);
			if (Collections.disjoint(vPrevIds, uPrevSim)) {
				return false;
			}
		}

		return true;
	}

	public static boolean isPrevSimulated(Indexer labelAdjacencyIndexer, IMatchNodes matchNodes, Integer vId,
			PatternNode u, DirectedGraph<PatternNode, DefaultLabeledEdge> Q) {

		for (DefaultLabeledEdge e : Q.incomingEdgesOf(u)) {
			// children of vId with the desired type
			PatternNode uPrev = Q.getEdgeSource(e);

			if (!labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.containsKey(uPrev.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					|| !labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
							.get(uPrev.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
							.containsKey(Indexer.BEFORE)) {
				return false;
			}

			Set<Integer> vPrevIds = labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.get(uPrev.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					.get(Indexer.BEFORE).neighborNodeIds;

			if (vPrevIds.isEmpty())
				return false;

			// intersection of I_u with I_e (from temporal index) with I_u'
			HashSet<Integer> uPrevSimMatches = matchNodes.getDataGraphMatchNodeOfAbsPNode().get(uPrev);

			HashSet<Integer> intersection = new HashSet<Integer>(vPrevIds);
			intersection.retainAll(uPrevSimMatches);

			if (intersection.isEmpty())
				return false;

			ArrayList<Integer> Iu1 = matchNodes.getTimePointsOfAMatch(u, vId);

			if (Iu1 == null) {
				System.out.println("Iu1 is null");
			}

			ArrayList<Integer> Ie = labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.get(uPrev.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					.get(Indexer.BEFORE).Ie;

			Iu1 = TimeIntervalsOperation.intersectionOfTimeIntervals(Iu1, Ie);

			// Iu1 ==[] means -inf/+inf
			if (Iu1 == null)
				return false;

			ArrayList<Integer> Iu2 = new ArrayList<Integer>();
			for (Integer vPrevId : intersection) {
				Iu2 = TimeIntervalsOperation.unionOfTimeIntervals(Iu2,
						matchNodes.getTimePointsOfAMatch(uPrev, vPrevId));
			}

			Iu1 = TimeIntervalsOperation.intersectionOfTimeIntervals(Iu1, Iu2);

			// Iu1 ==[] means -inf/+inf
			if (Iu1 == null)
				return false;

			matchNodes.setTimePointsOfAMatch(u, vId, Iu1);
		}

		return true;
	}
}
