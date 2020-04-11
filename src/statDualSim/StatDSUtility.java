/*
This file contains methods that are used in the src.dualSimulation algorithm.
Originally written by: Peng
Modified for noe4j by: Shayan Monadjemi
June 7th, 2016
 */

package statDualSim;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.neo4j.graphdb.*;

import base.IMatchNodes;
import utilities.DefaultLabeledEdge;
import utilities.Dummy;
import utilities.Indexer;
import utilities.PatternNode;
import utilities.TimeIntervalsOperation;

import java.util.*;

/**
 * Simulation.
 */
public class StatDSUtility {
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

	public static HashMap<Integer, HashSet<PatternNode>> createReversedMap(HashMap<PatternNode, HashSet<Integer>> map) {
		HashMap<Integer, HashSet<PatternNode>> revMap = new HashMap<>();
		for (PatternNode srcId : map.keySet()) {
			for (Integer dstId : map.get(srcId)) {
				revMap.putIfAbsent(dstId, new HashSet<PatternNode>());
				revMap.get(dstId).add(srcId);
			}
		}

		return revMap;
	}

	public static boolean isNextSimulated(Map<Integer, HashSet<Integer>> sim, Node v, Node u) {
		// Get the ids of the children nodes of v
		Set<Integer> vNextIds = new HashSet<>();
		for (Relationship r : v.getRelationships(Direction.OUTGOING)) {
			vNextIds.add((int) r.getEndNode().getId());
		}
		// Get the ids of the children nodes of u
		Set<Integer> uNextIds = new HashSet<>();
		for (Relationship r : u.getRelationships(Direction.OUTGOING)) {
			uNextIds.add((int) r.getEndNode().getId());
		}
		for (Integer uNext : uNextIds) {
			HashSet<Integer> uNextSim = sim.get(uNext);
			if (Collections.disjoint(vNextIds, uNextSim)) {
				return false;
			}
		}

		return true;
	}

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

	public static boolean isNextSimulated(Indexer labelAdjacencyIndexer, Map<PatternNode, HashSet<Integer>> sim,
			Integer vId, PatternNode u, DirectedGraph<PatternNode, DefaultLabeledEdge> Q, int timestamp) {
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

			Set<Integer> vNextIds = new  HashSet<Integer>(labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.get(uNext.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					.get(Indexer.AFTER).neighborNodeIds);
			
			
			if (vNextIds.isEmpty())
				return false;

			// just maintain nextNodeIds at this timestamp:
			Iterator<Integer> nexItr = vNextIds.iterator();
			while (nexItr.hasNext()) {
				Integer nexId = nexItr.next();
				if (!labelAdjacencyIndexer.timepointsOfNextNodeOfANode.get(vId).get(nexId).isEmpty()
						&& !labelAdjacencyIndexer.timepointsOfNextNodeOfANode.get(vId).get(nexId).contains(timestamp)) {
					nexItr.remove();
				}
			}

			HashSet<Integer> uNextSimMatches = sim.get(uNext);

			HashSet<Integer> intersection = new HashSet<Integer>(vNextIds);
			intersection.retainAll(uNextSimMatches);

			if (intersection.isEmpty())
				return false;
		}

		return true;
	}

	public static boolean isPrevSimulated(Map<Integer, HashSet<Integer>> sim, Node v, Node u) {
		// Get the ids of parent nodes v
		Set<Integer> vPrevIds = new HashSet<>();
		for (Relationship r : v.getRelationships(Direction.INCOMING)) {
			vPrevIds.add((int) r.getStartNode().getId());
		}
		// Get the ids of parent nodes u
		Set<Integer> uPrevIds = new HashSet<>();
		for (Relationship r : u.getRelationships(Direction.INCOMING)) {
			uPrevIds.add((int) r.getStartNode().getId());
		}

		for (Integer uPrev : uPrevIds) {
			HashSet<Integer> uPrevSim = sim.get(uPrev);
			if (Collections.disjoint(vPrevIds, uPrevSim)) {
				return false;
			}
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

	public static boolean isPrevSimulated(Indexer labelAdjacencyIndexer, Map<PatternNode, HashSet<Integer>> sim,
			Integer vId, PatternNode u, DirectedGraph<PatternNode, DefaultLabeledEdge> Q, int timestamp) {

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

			Set<Integer> vPrevIds = new HashSet<Integer>();
			
			vPrevIds.addAll(labelAdjacencyIndexer.temporalNeighborhoodIndex.get(vId)
					.get(uPrev.getLabel() + Dummy.DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + e.getType())
					.get(Indexer.BEFORE).neighborNodeIds);

			if (vPrevIds.isEmpty())
				return false;

			// just maintain nextNodeIds at this timestamp:
			Iterator<Integer> prevItr = vPrevIds.iterator();
			while (prevItr.hasNext()) {
				Integer prevId = prevItr.next();
				if (!labelAdjacencyIndexer.timepointsOfPrevNodeOfANode.get(vId).get(prevId).isEmpty()
						&& !labelAdjacencyIndexer.timepointsOfPrevNodeOfANode.get(vId).get(prevId)
								.contains(timestamp)) {
					prevItr.remove();
				}
			}

			HashSet<Integer> uPrevSimMatches = sim.get(uPrev);

			HashSet<Integer> intersection = new HashSet<Integer>(vPrevIds);
			intersection.retainAll(uPrevSimMatches);

			if (intersection.isEmpty())
				return false;
		}
		return true;
	}
}
