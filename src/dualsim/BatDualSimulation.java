/*
This is a version of Peng's src.dualSimulation code which supports Neo4j graphs.
Modified by: Shayan Monadjemi
June 7th, 2016
 */

package dualsim;

import org.jgrapht.DirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import base.ILattice;
import base.IMatchNodes;
import utilities.DefaultLabeledEdge;
import utilities.PatternNode;
import utilities.PatternNodeIdPair;

import java.io.File;
import java.util.*;

/**
 * Batch Dual Simulation.
 */
public class BatDualSimulation {

	public static HashMap<PatternNode, HashSet<PatternNode>> customizedMatchList(
			DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph1,
			DirectedGraph<PatternNode, DefaultLabeledEdge> patternGraph2,
			HashMap<PatternNode, HashSet<PatternNode>> dsim, ILattice lattice) {

		boolean noStop = true;
		// try (Transaction tx1 = dataGraph.beginTx()) {
		while (noStop) {
			noStop = false;
			for (PatternNode uId : dsim.keySet()) {
				for (Iterator<PatternNode> it = dsim.get(uId).iterator(); it.hasNext();) {
					PatternNode vId = it.next();
					// PatternNode u = null;
					// for (PatternNode n : patternGraph2.vertexSet()) {
					// if (n.equals(uId)) {
					// u = n;
					// break;
					// }
					// }
					// Node v = dataGraph.getNodeById(vId);

					if ((!utility.isNextSimulated(patternGraph1, dsim, vId, uId, patternGraph2))
							|| (!utility.isPrevSimulated(patternGraph1, dsim, vId, uId, patternGraph2))) {
						it.remove();
						noStop = true;
					}
				}
			}
		}

		HashMap<PatternNode, HashSet<PatternNode>> simRes = new HashMap<>();
		for (PatternNode uId : dsim.keySet()) {
			// simRes.putIfAbsent(uId, new HashSet<PatternNode>());
			if ((dsim.get(uId) != null) && (!dsim.get((uId)).isEmpty())) {
				simRes.put(uId, dsim.get(uId));
			}
		}

		return simRes;
	}

	// I think
	public static HashMap<PatternNode, HashSet<Integer>> customizedMatchList(GraphDatabaseService dataGraph,
			DirectedGraph<PatternNode, DefaultLabeledEdge> queryGraph, IMatchNodes matchNodes, ILattice lattice) {

		boolean noStop = true;
		// try (Transaction tx1 = dataGraph.beginTx()) {
		while (noStop) {
			noStop = false;
			for (PatternNode uId : matchNodes.getDataGraphMatchNodeOfAbsPNode().keySet()) {
				for (Iterator<Integer> it = matchNodes.getDataGraphMatchNodeOfAbsPNode().get(uId).iterator(); it
						.hasNext();) {
					Integer vId = it.next();
					PatternNode u = null;
					for (PatternNode n : queryGraph.vertexSet()) {
						if (n.equals(uId)) {
							u = n;
							break;
						}
					}
					// Node v = dataGraph.getNodeById(vId);

					if ((!utility.isNextSimulated(lattice.getLabelAdjacencyIndexer(), matchNodes, vId, u, queryGraph))
							|| (!utility.isPrevSimulated(lattice.getLabelAdjacencyIndexer(), matchNodes, vId, u,
									queryGraph))) {
						it.remove();
						matchNodes.getTimePointsOfAMatchNodeMap().remove(new PatternNodeIdPair(u, vId));
						noStop = true;
					}
				}
			}
		}
		//
		// tx1.success();
		// } catch (Exception e) {
		//
		// }

		HashMap<PatternNode, HashSet<Integer>> simRes = new HashMap<>();
		for (PatternNode uId : matchNodes.getDataGraphMatchNodeOfAbsPNode().keySet()) {
			simRes.putIfAbsent(uId, new HashSet<Integer>());
			if ((matchNodes.getDataGraphMatchNodeOfAbsPNode().get(uId) != null)
					&& (!matchNodes.getDataGraphMatchNodeOfAbsPNode().get((uId)).isEmpty())) {
				simRes.put(uId, matchNodes.getDataGraphMatchNodeOfAbsPNode().get(uId));
			}
		}

//		for (PatternNode u : simRes.keySet()) {
//			for (Integer nodeId : simRes.get(u)) {
//				if (matchNodes.getTimePointsOfAMatchNodeMap().containsKey(new PatternNodeIdPair(u, nodeId))
//						&& matchNodes.getTimePointsOfAMatch(u, nodeId) == null) {
//					System.err.println("null in batch dual sim");
//				}
//			}
//
//		}

		return simRes;

	}

}
