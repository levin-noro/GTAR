/*
This is a version of Peng's src.dualSimulation code which supports Neo4j graphs.
Modified by: Shayan Monadjemi
June 7th, 2016
 */

package statDualSim;

import org.jgrapht.DirectedGraph;

import org.neo4j.graphdb.GraphDatabaseService;

import base.ILattice;
import base.IMatchNodes;
import dualsim.utility;
import utilities.DefaultLabeledEdge;
import utilities.PatternNode;

import java.util.*;

/**
 * Batch Dual Simulation.
 */
public class StatDualSimulation {

	public static HashMap<PatternNode, HashSet<Integer>> customizedMatchListStat(GraphDatabaseService dataGraph,
			DirectedGraph<PatternNode, DefaultLabeledEdge> queryGraph, HashMap<PatternNode, HashSet<Integer>> dsim, ILattice lattice,
			int timestamp, IMatchNodes matchNodes) {

		boolean noStop = true;
		// try (Transaction tx1 = dataGraph.beginTx()) {
		while (noStop) {
			noStop = false;
			for (PatternNode uId : dsim.keySet()) {
				for (Iterator<Integer> it = dsim.get(uId).iterator(); it
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

					if ((!StatDSUtility.isNextSimulated(lattice.getLabelAdjacencyIndexer(), dsim, vId, u,
							queryGraph, timestamp))
							|| (!StatDSUtility.isPrevSimulated(lattice.getLabelAdjacencyIndexer(), dsim, vId, u,
									queryGraph, timestamp))) {
						it.remove();
						// matchNodes.getTimePointsOfAMatchNodeMap().remove(new
						// PatternNodeIdPair(u, vId));
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
		for (PatternNode uId : dsim.keySet()) {
			simRes.putIfAbsent(uId, new HashSet<Integer>());
			if ((dsim.get(uId) != null) && (!dsim.get((uId)).isEmpty())) {
				simRes.put(uId, dsim.get(uId));
			}
		}

		return simRes;

	}

}
