package utilities;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

import base.ILatticeNodeData;
import utilities.Dummy.DummyProperties;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * This program computes the number of nodes with any label, d hops from every
 * node in the graph
 * 
 * @author Shayan Monadjemi
 * @date June 16, 2016
 */
public class Indexer {
	public static final Integer AFTER = 1;
	public static final Integer BEFORE = -1;

	// The structure V*Sigma*d Path of the neo4j graph database
	GraphDatabaseService dataGraph;

	// nodeId => Adj Label, +-, numbers of that label
	// public HashMap<Integer, HashMap<String, HashMap<Integer, int[]>>>
	// labelAdjMatrix = new HashMap<>();

	// static String path =
	// "/home/shayan/Documents/WSU/Data/offshoreneo4j.data/panama.graphdb";

	// nodeId => label in d-hops => which distinct node ids?
	// public HashMap<Integer, HashMap<String, HashMap<Integer,
	// HashSet<Integer>>>> distinctNodesOfDHopsAway = new HashMap<>();
	public HashMap<Integer, HashMap<String, HashMap<Integer, NeighborhoodInfo>>> temporalNeighborhoodIndex = new HashMap<>();

	public HashMap<Integer, HashMap<Integer, HashSet<Integer>>> timepointsOfNextNodeOfANode = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
	public HashMap<Integer, HashMap<Integer, HashSet<Integer>>> timepointsOfPrevNodeOfANode = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();

	private HashMap<String, HashSet<Integer>> focusNodesOfAllTypes;
	public int maxNodeId = 0;

	public int numberOfNodesInGraph0 = 0;

	public HashMap<Integer, HashSet<Integer>> subPatternsOfAPattern = new HashMap<Integer, HashSet<Integer>>();

	public HashMap<Integer, NodeInfo> dataGraphNodeInfos = new HashMap<Integer, NodeInfo>();

	// public HashMap<Integer, HashMap<Integer, String>> relTypeOfSrcAndDest =
	// new HashMap<Integer, HashMap<Integer, String>>();

	public HashMap<PatternNode, HashSet<Integer>> candidateSetOfAPatternNode = new HashMap<PatternNode, HashSet<Integer>>();

	public HashMap<Integer, HashSet<LatticeNode<ILatticeNodeData>>> latticeNodesOfALevel = new HashMap<Integer, HashSet<LatticeNode<ILatticeNodeData>>>();

	public HashMap<SourceRelDestTypeTriple, CorrespondsOfSrcRelDest> correspondsOfSrcRelDestType = new HashMap<SourceRelDestTypeTriple, CorrespondsOfSrcRelDest>();

	public HashMap<LatticeNode<ILatticeNodeData>, HashMap<LatticeNode<ILatticeNodeData>, SourceRelDestTypeTriple>> parentChildDifference = new HashMap<LatticeNode<ILatticeNodeData>, HashMap<LatticeNode<ILatticeNodeData>, SourceRelDestTypeTriple>>();

	public static HashSet<Integer> usefulStaticNodes = new HashSet<Integer>();

	private Integer vf2NodeIndex = 0;
	// public SimpleDirectedGraph<SimpleCustomNode, DefaultEdge>
	// inducedGraphForVF2 = new SimpleDirectedGraph<SimpleCustomNode,
	// DefaultEdge>(
	// DefaultEdge.class);

	// public HashMap<Integer, SimpleCustomNode> simpleNodeOfNeo4jNodeId = new
	// HashMap<Integer, SimpleCustomNode>();

	// public static HashSet<Integer> weirdNodesSet = new HashSet<Integer>();

	// in the bitmap each bit represents both pattern id and pattern node id
	// here we'll decode it.
	public static HashMap<Integer, Integer> patternIdOfPatternNodeId = new HashMap<Integer, Integer>();
	public static Integer patternNodeIdCounter = 0;

	public Indexer(GraphDatabaseService dataGraph, HashMap<String, HashSet<Integer>> focusNodesOfAllTypes) {
		this.dataGraph = dataGraph;
		this.focusNodesOfAllTypes = focusNodesOfAllTypes;
	}

	// public Indexer(String[] args) throws Exception {
	// for (int i = 0; i < args.length; i++) {
	// if (args[i].equals("-dataGraphPath")) {
	// path = args[++i];
	// } else if (args[i].equals("-dHops")) {
	// dHops = Integer.parseInt(args[++i]);
	// }
	// }
	//
	// if (path.equals("")) {
	// throw new Exception("-dataGraphPath should be filled with a neo4j graph
	// db path");
	// }
	// File storeDir = new File(path);
	// dataGraph = new
	// GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
	// .setConfig(GraphDatabaseSettings.pagecache_memory,
	// "6g").newGraphDatabase();
	//
	// }

	// public static void main(String[] args) throws Exception {
	// Indexer lblAdjIndexer = new Indexer(args);
	//

	//
	// // lblAdjIndexer.generateLabelAdjMatrix();
	// // lblAdjIndexer.printLabelAdjMatrix();
	// //
	// // lblAdjIndexer.generateDistinctNodesAdjMatrix();
	// // lblAdjIndexer.printDistinctNodesAdjMatrix(lblAdjIndexer.dHops);
	//
	// }

	// private void printDistinctNodesAdjMatrix(Integer maxDHops) {
	// System.out.println();
	// for (Integer sourceNodeId : distinctNodesOfDHopsAway.keySet()) {
	// System.out.print(sourceNodeId + ": " + "{");
	// for (String l : distinctNodesOfDHopsAway.get(sourceNodeId).keySet()) {
	// System.out.print(l + "=[");
	// for (int hop = -maxDHops; hop <= maxDHops; hop++) {
	// if (distinctNodesOfDHopsAway.get(sourceNodeId).get(l).containsKey(hop)) {
	// System.out.print(hop + "=>(");
	// for (Integer adjNodeId :
	// distinctNodesOfDHopsAway.get(sourceNodeId).get(l).get(hop)) {
	// System.out.print(adjNodeId + ", ");
	// }
	// System.out.print("), ");
	// }
	// }
	// System.out.print("], ");
	// }
	// System.out.println("}");
	//
	// }
	// }
	//
	public void generateTemporalNeighborhoodIndex(int realHops, String focus) throws Exception {
		// try (Transaction tx1 = dataGraph.beginTx()) {

		HashSet<Integer> allFocusNodes = new HashSet<Integer>();
		for (String label : focusNodesOfAllTypes.keySet()) {
			allFocusNodes.addAll(focusNodesOfAllTypes.get(label));
		}

		findUsefulNodesSet(realHops, dataGraph, allFocusNodes, focus);

		// generateRelTypeMap();
		generateDegreeMap();

		// First, load all node ids in the label adjacency matrix
		numberOfNodesInGraph0 = 0;
		for (Node n : dataGraph.getAllNodes()) {
			numberOfNodesInGraph0++;

			int id = (int) n.getId();

			maxNodeId = Math.max(maxNodeId, id);

//			if (!usefulStaticNodes.contains(id)) {
//				continue;
//			}

			temporalNeighborhoodIndex.put(id, new HashMap<>());

		}

		for (Integer nodeId : temporalNeighborhoodIndex.keySet()) {

			Node n = dataGraph.getNodeById(nodeId);

			for (Relationship outgoingRel : n.getRelationships(Direction.OUTGOING)) {

//				String temp = (String) outgoingRel.getProperty("timepoints");
//				String[] tempList = temp.split(";");
//				int[] tempTimePoints = new int[tempList.length]; int i = 0;
//				for (String item:tempList) {
//					tempTimePoints[i] = Integer.parseInt(tempList[i]);
//					i++;
//				}
//					
//				ArrayList<Integer> timePoints = TimeIntervalsOperation
//						.getArrayListOfArray(tempTimePoints);
				ArrayList<Integer> timePoints = new ArrayList<Integer>();
				timePoints.add(0);
				timePoints.add(1);
				
				Node otherNode = outgoingRel.getOtherNode(n);
				String nextType = otherNode.getLabels().iterator().next().name().toString()
						+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + outgoingRel.getType().toString();

				nextType = nextType.intern();
				temporalNeighborhoodIndex.get((int) n.getId()).putIfAbsent(nextType, new HashMap<>());
				temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).putIfAbsent(AFTER, new NeighborhoodInfo());

				temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).get(AFTER).neighborNodeIds
						.add((int) otherNode.getId());

				temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).get(AFTER).Ie = TimeIntervalsOperation
						.unionOfTimeIntervals(
								temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).get(AFTER).Ie, timePoints);
			}

			for (Relationship incomingRel : n.getRelationships(Direction.INCOMING)) {

//				String temp = (String) incomingRel.getProperty("timepoints");
//				String[] tempList = temp.split(";");
//				int[] tempTimePoints = new int[tempList.length]; int i = 0;
//				for (String item:tempList) {
//					tempTimePoints[i] = Integer.parseInt(tempList[i]);
//					i++;
//				}
//					
//				ArrayList<Integer> timePoints = TimeIntervalsOperation
//						.getArrayListOfArray(tempTimePoints);
				
				ArrayList<Integer> timePoints = new ArrayList<Integer>();
				timePoints.add(0);
				timePoints.add(1);

				Node otherNode = incomingRel.getOtherNode(n);
				String prevType = otherNode.getLabels().iterator().next().name().toString()
						+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + incomingRel.getType().toString();

				prevType = prevType.intern();
				temporalNeighborhoodIndex.get((int) n.getId()).putIfAbsent(prevType, new HashMap<>());
				temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).putIfAbsent(BEFORE,
						new NeighborhoodInfo());

				temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).get(BEFORE).neighborNodeIds
						.add((int) otherNode.getId());

				temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).get(BEFORE).Ie = TimeIntervalsOperation
						.unionOfTimeIntervals(
								temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).get(BEFORE).Ie,
								timePoints);
			}
		}

		printTemporalNeighborhoodIndex();

		// tx1.success();
		// tx1.close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

	}

	public void generateTemporalNeighborhoodIndexForStat(int realHops, String focus, int minTimestamp, int maxTimestamp)
			throws Exception {
		// try (Transaction tx1 = dataGraph.beginTx()) {

		HashSet<Integer> allFocusNodes = new HashSet<Integer>();
		for (String label : focusNodesOfAllTypes.keySet()) {
			allFocusNodes.addAll(focusNodesOfAllTypes.get(label));
		}

		findUsefulNodesSet(realHops, dataGraph, allFocusNodes, focus);

		// generateRelTypeMap();
		generateDegreeMap();

		// First, load all node ids in the label adjacency matrix
		numberOfNodesInGraph0 = 0;
		for (Node n : dataGraph.getAllNodes()) {
			numberOfNodesInGraph0++;

			int id = (int) n.getId();

			maxNodeId = Math.max(maxNodeId, id);

			if (!usefulStaticNodes.contains(id)) {
				continue;
			}

			temporalNeighborhoodIndex.put(id, new HashMap<>());
			timepointsOfNextNodeOfANode.put(id, new HashMap<>());
			timepointsOfPrevNodeOfANode.put(id, new HashMap<>());

		}

		for (Integer nodeId : temporalNeighborhoodIndex.keySet()) {

			Node n = dataGraph.getNodeById(nodeId);

			for (Relationship outgoingRel : n.getRelationships(Direction.OUTGOING)) {

				ArrayList<Integer> timePoints = TimeIntervalsOperation
						.getArrayListOfArray((int[]) outgoingRel.getProperty("timepoints"));

				Node otherNode = outgoingRel.getOtherNode(n);
				String nextType = otherNode.getLabels().iterator().next().name().toString()
						+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + outgoingRel.getType().toString();

				nextType = nextType.intern();
				temporalNeighborhoodIndex.get((int) n.getId()).putIfAbsent(nextType, new HashMap<>());
				temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).putIfAbsent(AFTER, new NeighborhoodInfo());

				temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).get(AFTER).neighborNodeIds
						.add((int) otherNode.getId());

				temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).get(AFTER).Ie = TimeIntervalsOperation
						.unionOfTimeIntervals(
								temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).get(AFTER).Ie, timePoints);

				// just for STAT:
				timepointsOfNextNodeOfANode.get((int) n.getId()).putIfAbsent((int) otherNode.getId(), new HashSet<>());
				if (timePoints.size() != 2 || timePoints.get(0) != minTimestamp || timePoints.get(1) != maxTimestamp) {
					for (int i = 0; i < (timePoints.size() - 1); i += 2) {
						for (int j = timePoints.get(i); j <= timePoints.get(i + 1); j++) {
							timepointsOfNextNodeOfANode.get((int) n.getId()).get((int) otherNode.getId()).add(j);
						}
					}
				}

			}

			for (Relationship incomingRel : n.getRelationships(Direction.INCOMING)) {

				ArrayList<Integer> timePoints = TimeIntervalsOperation
						.getArrayListOfArray((int[]) incomingRel.getProperty("timepoints"));

				Node otherNode = incomingRel.getOtherNode(n);
				String prevType = otherNode.getLabels().iterator().next().name().toString()
						+ DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + incomingRel.getType().toString();

				prevType = prevType.intern();
				temporalNeighborhoodIndex.get((int) n.getId()).putIfAbsent(prevType, new HashMap<>());
				temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).putIfAbsent(BEFORE,
						new NeighborhoodInfo());

				temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).get(BEFORE).neighborNodeIds
						.add((int) otherNode.getId());

				temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).get(BEFORE).Ie = TimeIntervalsOperation
						.unionOfTimeIntervals(
								temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).get(BEFORE).Ie,
								timePoints);

				// just for STAT:
				timepointsOfPrevNodeOfANode.get((int) n.getId()).putIfAbsent((int) otherNode.getId(), new HashSet<>());
				if (timePoints.size() != 2 || timePoints.get(0) != minTimestamp || timePoints.get(1) != maxTimestamp) {
					for (int i = 0; i < (timePoints.size() - 1); i += 2) {
						for (int j = timePoints.get(i); j <= timePoints.get(i + 1); j++) {
							timepointsOfPrevNodeOfANode.get((int) n.getId()).get((int) otherNode.getId()).add(j);
						}
					}
				}
			}
		}

	}

	// public void generateDistinctNodesAdjMatrix(int realHops, String focus) {
	//
	// try (Transaction tx1 = dataGraph.beginTx()) {
	//
	// HashSet<Integer> allFocusNodes = new HashSet<Integer>();
	// for (String label : focusNodesOfAllTypes.keySet()) {
	// allFocusNodes.addAll(focusNodesOfAllTypes.get(label));
	// }
	//
	// findUsefulNodesSet(realHops, dataGraph, allFocusNodes, focus);
	//
	// generateRelTypeMap();
	// generateDegreeMap();
	//
	// // distinctNodesOfDHopsAway.clear();
	// // distinctNodesOfDHopsAway = new HashMap<>();
	//
	// // First, load all node ids in the label adjacency matrix
	// numberOfNodesInGraph0 = 0;
	// for (Node n : dataGraph.getAllNodes()) {
	// numberOfNodesInGraph0++;
	//
	// int id = (int) n.getId();
	//
	// maxNodeId = Math.max(maxNodeId, id);
	//
	// // For Performance and memory
	// if (!usefulStaticNodes.contains(id))
	// continue;
	//
	// // distinctNodesOfDHopsAway.put(id, new HashMap<>());
	// temporalNeighborhoodIndex.put(id, new HashMap<>());
	//
	// }
	//
	// // Use the recursive helper function to compute the neighborhood
	// // nodes up to d hops away
	// // for (Integer nodeId : distinctNodesOfDHopsAway.keySet()) {
	// // addNextNodesToMatrix(nodeId, nodeId, dHops, 0, dataGraph);
	// // addPreviousNodesToMatrix(nodeId, nodeId, -dHops, 0, dataGraph);
	// // }
	//
	// for (Node n : dataGraph.getAllNodes()) {
	// for (Relationship outgoingRel : n.getRelationships(Direction.OUTGOING)) {
	//
	// ArrayList<Integer> timePoints = TimeIntervalsOperation
	// .getArrayListOfArray((int[]) outgoingRel.getProperty("timepoints"));
	//
	// Node otherNode = outgoingRel.getOtherNode(n);
	// String nextType =
	// otherNode.getLabels().iterator().next().name().toString()
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE +
	// outgoingRel.getType().toString();
	// System.out.println(nextType);
	// temporalNeighborhoodIndex.get((int) n.getId()).putIfAbsent(nextType, new
	// HashMap<>());
	// temporalNeighborhoodIndex.get((int)
	// n.getId()).get(nextType).putIfAbsent(AFTER, new ArrayList<>());
	//
	// temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).put(AFTER,
	// TimeIntervalsOperation.unionOfTimeIntervals(
	// temporalNeighborhoodIndex.get((int) n.getId()).get(nextType).get(AFTER),
	// timePoints));
	// }
	//
	// for (Relationship incomingRel : n.getRelationships(Direction.INCOMING)) {
	// ArrayList<Integer> timePoints = TimeIntervalsOperation
	// .getArrayListOfArray((int[]) incomingRel.getProperty("timepoints"));
	//
	// Node otherNode = incomingRel.getOtherNode(n);
	// String prevType =
	// otherNode.getLabels().iterator().next().name().toString()
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE +
	// incomingRel.getType().toString();
	// temporalNeighborhoodIndex.get((int) n.getId()).putIfAbsent(prevType, new
	// HashMap<>());
	// temporalNeighborhoodIndex.get((int)
	// n.getId()).get(prevType).putIfAbsent(BEFORE, new ArrayList<>());
	//
	// temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).put(BEFORE,
	// TimeIntervalsOperation.unionOfTimeIntervals(
	// temporalNeighborhoodIndex.get((int) n.getId()).get(prevType).get(BEFORE),
	// timePoints));
	// }
	// }
	//
	// // printDistinctNodesAdjMatrix(realHops);
	// // System.out.println();
	// tx1.success();
	// tx1.close();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }

	private void printTemporalNeighborhoodIndex() {
		for (Integer nodeId : temporalNeighborhoodIndex.keySet()) {
			System.out.println("nodeId: " + nodeId);
			for (String type : temporalNeighborhoodIndex.get(nodeId).keySet()) {
				System.out.print("type: " + type + " =>");
				if (temporalNeighborhoodIndex.get(nodeId).get(type).containsKey(BEFORE)) {
					System.out.println("Before:");
					HashSet<Integer> neighborNodeIds = temporalNeighborhoodIndex.get(nodeId).get(type)
							.get(BEFORE).neighborNodeIds;
					System.out.print("prev neighbors: " + neighborNodeIds);
					ArrayList<Integer> Ie = temporalNeighborhoodIndex.get(nodeId).get(type).get(BEFORE).Ie;
					System.out.println(" [" + Arrays.toString(Ie.toArray()) + "]");
				}
				if (temporalNeighborhoodIndex.get(nodeId).get(type).containsKey(AFTER)) {
					System.out.println("After:");
					HashSet<Integer> neighborNodeIds = temporalNeighborhoodIndex.get(nodeId).get(type)
							.get(AFTER).neighborNodeIds;
					System.out.print("next neighbors: " + neighborNodeIds);
					ArrayList<Integer> Ie = temporalNeighborhoodIndex.get(nodeId).get(type).get(AFTER).Ie;
					System.out.println(" [" + Arrays.toString(Ie.toArray()) + "]");
				}

			}
			System.out.println();
		}
	}

	// private void generateRelTypeMap() {
	//
	// for (Relationship rel : dataGraph.getAllRelationships()) {
	// int sourceId = (int) rel.getStartNode().getId();
	// int destId = (int) rel.getEndNode().getId();
	//
	// // For Performance and memory
	// if (!usefulStaticNodes.contains(sourceId) &&
	// !usefulStaticNodes.contains(destId))
	// continue;
	//
	// this.relTypeOfSrcAndDest.putIfAbsent(sourceId, new HashMap<Integer,
	// String>());
	// this.relTypeOfSrcAndDest.get(sourceId).put(destId,
	// rel.getType().name().intern());
	//
	// }
	//
	// }

	private void generateDegreeMap() {
		dataGraphNodeInfos.clear();

		dataGraphNodeInfos = new HashMap<Integer, NodeInfo>();

		for (Node node : dataGraph.getAllNodes()) {

			// For Performance and memory
			if (!usefulStaticNodes.contains((int) node.getId())) {
				
				
				continue;
			}

			// HashSet<Integer> nextNodeIds = new HashSet<Integer>();
			// for (Relationship r : node.getRelationships(Direction.OUTGOING))
			// {
			// nextNodeIds.add((int) r.getEndNode().getId());
			// }
			//
			// HashSet<Integer> prevNodeIds = new HashSet<Integer>();
			// for (Relationship r : node.getRelationships(Direction.INCOMING))
			// {
			// prevNodeIds.add((int) r.getStartNode().getId());
			// }

			dataGraphNodeInfos.put((int) node.getId(), new NodeInfo(node.getLabels().iterator().next().name().intern(),
					node.getDegree(Direction.INCOMING), node.getDegree(
							Direction.OUTGOING)/* , prevNodeIds, nextNodeIds */));
		}
		 System.out.println("inside generateDegreeMap");
	}

	// private void addPreviousNodesToMatrix(Integer parentId, Integer thisId,
	// int maxDHops, int currentHop,
	// GraphDatabaseService dataGraph2) {
	// if (currentHop > maxDHops) {
	//
	// for (Integer cId : this.dataGraphNodeInfos.get(thisId).prevNodeIds) {
	//
	// if (Math.abs(maxDHops) > 1)
	// addPreviousNodesToMatrix(parentId, cId, maxDHops, currentHop - 1,
	// dataGraph);
	//
	// if (this.dataGraphNodeInfos.containsKey(cId)) {
	// String label_RelType = this.dataGraphNodeInfos.get(cId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE
	// + this.relTypeOfSrcAndDest.get(cId).get(thisId);
	// if (!distinctNodesOfDHopsAway.get(parentId).containsKey(label_RelType)) {
	// distinctNodesOfDHopsAway.get(parentId).put(label_RelType,
	// new HashMap<Integer, HashSet<Integer>>());
	// }
	// if
	// (!distinctNodesOfDHopsAway.get(parentId).get(label_RelType).containsKey(currentHop
	// - 1)) {
	// distinctNodesOfDHopsAway.get(parentId).get(label_RelType).put(currentHop
	// - 1,
	// new HashSet<Integer>());
	// }
	// distinctNodesOfDHopsAway.get(parentId).get(label_RelType).get(currentHop
	// - 1).add(cId);
	// }
	// // else {
	// // System.out.println();
	// // }
	//
	// }
	// } else
	// return;
	//
	// }
	//
	// private void addNextNodesToMatrix(Integer parentId, Integer thisId, int
	// maxDHops, int currentHop,
	// GraphDatabaseService dataGraph) {
	//
	// // recursion base case: as Integer as we haven't
	// // exceeded the max number of hops, keep going
	// if (currentHop < maxDHops) {
	// // Node c;
	//
	// for (Integer cId : this.dataGraphNodeInfos.get(thisId).nextNodeIds) {
	// // for (Relationship r :
	// // dataGraph.getNodeById(thisId).getRelationships(Direction.OUTGOING))
	// // {
	// // c = r.getEndNode();
	//
	// if (Math.abs(maxDHops) > 1)
	// addNextNodesToMatrix(parentId, cId, maxDHops, currentHop + 1, dataGraph);
	//
	// if (this.dataGraphNodeInfos.containsKey(cId)) {
	// // for (Label l : c.getLabels()) {
	// String label_RelType = this.dataGraphNodeInfos.get(cId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE +
	// relTypeOfSrcAndDest.get(thisId).get(cId);
	// if (!distinctNodesOfDHopsAway.get(parentId).containsKey(label_RelType)) {
	// distinctNodesOfDHopsAway.get(parentId).put(label_RelType,
	// new HashMap<Integer, HashSet<Integer>>());
	// }
	// if
	// (!distinctNodesOfDHopsAway.get(parentId).get(label_RelType).containsKey(currentHop
	// + 1)) {
	// distinctNodesOfDHopsAway.get(parentId).get(label_RelType).put(currentHop
	// + 1,
	// new HashSet<Integer>());
	// }
	// distinctNodesOfDHopsAway.get(parentId).get(label_RelType).get(currentHop
	// + 1).add(cId);
	// // }
	// }
	// // else {
	// // System.out.println();
	// // }
	// }
	// } else
	// return;
	//
	// }
	//
	/**
	 * This method populated the three dimensional structure with every neighbor
	 * label for every node, d hops away from the node
	 * 
	 * @param d
	 *            the number of MAXIMUM hops (edges) from the parent node to go.
	 *            (Example: d=3 will calculate 1 hop,2 hops, and 3 hops away.
	 */

	// public void generateLabelAdjMatrix() {
	//
	// try (Transaction tx1 = dataGraph.beginTx()) {
	// // First, load all node ids in the label adjacency matrix
	// for (Node n : dataGraph.getAllNodes()) {
	// labelAdjMatrix.put((int) n.getId(), new HashMap<>());
	// }
	// numberOfNodesInGraph0 = labelAdjMatrix.size();
	//
	// // Use the recursive helper function to compute the neighborhood
	// // labels
	// // up to d hops away
	// for (Integer nodeId : labelAdjMatrix.keySet()) {
	// addNextLabelsToMatrix(nodeId, nodeId, dHops, 0, dataGraph);
	// addPreviousLabelsToMatrix(nodeId, nodeId, -dHops, 0, dataGraph);
	// }
	// tx1.success();
	// } catch (Exception e) {
	//
	// }
	//
	// }

	// private void addPreviousLabelsToMatrix(Integer parentid, Integer thisid,
	// int maxdHops, int currentHop,
	// GraphDatabaseService gdb) {
	// // recursion base case: as Integer as we
	// // haven't
	// if (currentHop > maxdHops) // recursion base case: as Integer as we
	// // haven't
	// // exceeded
	// // the
	// // max number of hops, keep going
	// {
	// Node c;
	// for (Relationship r :
	// gdb.getNodeById(thisid).getRelationships(Direction.INCOMING)) {
	// c = r.getStartNode();
	//
	// addPreviousLabelsToMatrix(parentid, (int) c.getId(), maxdHops, currentHop
	// - 1, gdb);
	// for (Label l : c.getLabels()) {
	// if (labelAdjMatrix.get(parentid).containsKey(l.name())) {
	// if (!labelAdjMatrix.get(parentid).get(l.name()).containsKey(-1)) {
	// labelAdjMatrix.get(parentid).get(l.name()).put(-1, new int[-maxdHops +
	// 1]);
	// }
	// } else {
	// labelAdjMatrix.get(parentid).put(l.name(), new HashMap<Integer,
	// int[]>());
	// labelAdjMatrix.get(parentid).get(l.name()).put(-1, new int[-maxdHops +
	// 1]);
	// }
	//
	// labelAdjMatrix.get(parentid).get(l.name()).get(-1)[-currentHop + 1]++;
	// }
	// }
	// } else
	// return;
	// }
	//
	// /**
	// * This recursive method computes the number of neighbor nodes with every
	// * label
	// *
	// * @param parentid
	// * the starting node's id from which we are counting hops (where
	// * hop = 0)
	// * @param thisid
	// * the current node's id, whose children we'll be processing
	// * @param dhop
	// * the number of maximum hops
	// * @param h
	// * the number of hops away from parent (starting) node
	// * @param gdb
	// * the neo4j graph database
	// */
	// public void addNextLabelsToMatrix(Integer parentid, Integer thisid, int
	// maxdHops, int currentHop,
	// GraphDatabaseService gdb) {
	// if (currentHop < maxdHops) // recursion base case: as Integer as we
	// // haven't
	// // exceeded
	// // the
	// // max number of hops, keep going
	// {
	// Node c;
	// try (Transaction tx1 = gdb.beginTx()) {
	//
	// for (Relationship r :
	// gdb.getNodeById(thisid).getRelationships(Direction.OUTGOING)) {
	// c = r.getEndNode();
	//
	// addNextLabelsToMatrix(parentid, (int) c.getId(), maxdHops, currentHop +
	// 1, gdb);
	// for (Label l : c.getLabels()) {
	// if (labelAdjMatrix.get(parentid).containsKey(l.name())) {
	// if (!labelAdjMatrix.get(parentid).get(l.name()).containsKey(+1)) {
	// labelAdjMatrix.get(parentid).get(l.name()).put(+1, new int[maxdHops +
	// 1]);
	// }
	// } else {
	// labelAdjMatrix.get(parentid).put(l.name(), new HashMap<Integer,
	// int[]>());
	// labelAdjMatrix.get(parentid).get(l.name()).put(+1, new int[maxdHops +
	// 1]);
	// }
	//
	// labelAdjMatrix.get(parentid).get(l.name()).get(+1)[currentHop + 1]++;
	// }
	//
	// }
	// } catch (Exception e) {
	//
	// }
	// } else
	// return;
	// }
	//
	// /**
	// * This method prints our neighborhood label adjacency matrix
	// */
	// public void printLabelAdjMatrix() {
	// for (Integer id : labelAdjMatrix.keySet()) {
	// System.out.print(id + ": " + "{");
	// for (String l : labelAdjMatrix.get(id).keySet()) {
	// System.out.print(l + "= [");
	// if (labelAdjMatrix.get(id).get(l) != null) {
	// if (labelAdjMatrix.get(id).get(l).get(-1) != null) {
	// for (int i = 1; i < labelAdjMatrix.get(id).get(l).get(-1).length; i++) {
	// System.out.print(-i + ":" + labelAdjMatrix.get(id).get(l).get(-1)[i] + "
	// ");
	// }
	// }
	// if (labelAdjMatrix.get(id).get(l).get(+1) != null) {
	// for (int i = 1; i < labelAdjMatrix.get(id).get(l).get(+1).length; i++) {
	// System.out.print(i + ":" + labelAdjMatrix.get(id).get(l).get(+1)[i] + "
	// ");
	// }
	// }
	// }
	// System.out.print("], ");
	// }
	// System.out.println("}");
	//
	// }
	// }

	// public HashSet<Integer> getNextIds(Integer srcId) {
	// // HashSet<Integer> nextIdsSet = new HashSet<Integer>();
	// // for (String lbl : distinctNodesOfDHopsAway.get(srcId).keySet()) {
	// //
	// nextIdsSet.addAll(distinctNodesOfDHopsAway.get(srcId).get(lbl).get(AFTER));
	// // }
	// return this.dataGraphNodeInfos.get(srcId).nextNodeIds;
	// }
	//
	// public HashSet<Integer> getPrevIds(Integer srcId) {
	// return this.dataGraphNodeInfos.get(srcId).prevNodeIds;
	// }

	// public void addNewNode(Node node) {
	// distinctNodesOfDHopsAway.put((int) node.getId(), new HashMap<String,
	// HashMap<Integer, HashSet<Integer>>>());
	// }

	// public void updateNeighborhood(GraphDatabaseService dataGraph, Integer
	// srcNodeId, Integer destNodeId,
	// int maxAllowedHops, boolean isAdded) {
	//

	//
	// if (!isAdded) {
	// this.distinctNodesOfDHopsAway.get(srcNodeId).clear();
	// this.distinctNodesOfDHopsAway.get(destNodeId).clear();
	// }
	//
	// // just next of src and its previous nodes would be changed.
	// addNextNodesToMatrix(srcNodeId, srcNodeId, 1, 0, dataGraph);
	//
	//
	// // just previous of dest and its next nodes would be changed.
	// addPreviousNodesToMatrix(destNodeId, destNodeId, -1, 0, dataGraph);
	//
	// }

	// public void updateNeighborhood(GraphDatabaseService dataGraph, Integer
	// srcNodeId, Integer destNodeId,
	// String relationshipType, int maxAllowedHops, boolean isAdded) {
	//
	// if (isAdded) {
	//
	// // generating the neighborhood index by demand:
	// if (!distinctNodesOfDHopsAway.containsKey(srcNodeId)) {
	// distinctNodesOfDHopsAway.put(srcNodeId, new HashMap<String,
	// HashMap<Integer, HashSet<Integer>>>());
	// }
	//
	// if (!distinctNodesOfDHopsAway.containsKey(destNodeId)) {
	// distinctNodesOfDHopsAway.put(destNodeId, new HashMap<String,
	// HashMap<Integer, HashSet<Integer>>>());
	// }
	//
	// // for (Integer cId :
	// // this.dataGraphNodeInfos.get(srcNodeId).nextNodeIds) {
	//
	// // NEXT
	// String label_RelType = this.dataGraphNodeInfos.get(destNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType;
	// if (!distinctNodesOfDHopsAway.get(srcNodeId).containsKey(label_RelType))
	// {
	// distinctNodesOfDHopsAway.get(srcNodeId).put(label_RelType, new
	// HashMap<Integer, HashSet<Integer>>());
	// }
	// if
	// (!distinctNodesOfDHopsAway.get(srcNodeId).get(label_RelType).containsKey(AFTER))
	// {
	// distinctNodesOfDHopsAway.get(srcNodeId).get(label_RelType).put(AFTER, new
	// HashSet<Integer>());
	// }
	// distinctNodesOfDHopsAway.get(srcNodeId).get(label_RelType).get(AFTER).add(destNodeId);
	//
	// // Previous
	// label_RelType = this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType;
	// if (!distinctNodesOfDHopsAway.get(destNodeId).containsKey(label_RelType))
	// {
	// distinctNodesOfDHopsAway.get(destNodeId).put(label_RelType, new
	// HashMap<Integer, HashSet<Integer>>());
	// }
	// if
	// (!distinctNodesOfDHopsAway.get(destNodeId).get(label_RelType).containsKey(BEFORE))
	// {
	// distinctNodesOfDHopsAway.get(destNodeId).get(label_RelType).put(BEFORE,
	// new HashSet<Integer>());
	// }
	// distinctNodesOfDHopsAway.get(destNodeId).get(label_RelType).get(BEFORE).add(srcNodeId);
	//
	// } else {
	//
	// // NEXT
	//
	// if (distinctNodesOfDHopsAway.containsKey(srcNodeId)
	// && distinctNodesOfDHopsAway.get(srcNodeId)
	// .get(this.dataGraphNodeInfos.get(destNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType) != null
	// && distinctNodesOfDHopsAway.get(srcNodeId)
	// .get(this.dataGraphNodeInfos.get(destNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
	// .get(AFTER) != null) {
	// distinctNodesOfDHopsAway.get(srcNodeId)
	// .get(this.dataGraphNodeInfos.get(destNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
	// .get(AFTER).remove(destNodeId);
	// }
	//
	// // Previous
	// if (distinctNodesOfDHopsAway.containsKey(destNodeId)
	// && distinctNodesOfDHopsAway.get(destNodeId)
	// .get(this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType) != null
	// && distinctNodesOfDHopsAway.get(destNodeId)
	// .get(this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
	// .get(BEFORE) != null) {
	// distinctNodesOfDHopsAway.get(destNodeId)
	// .get(this.dataGraphNodeInfos.get(srcNodeId).nodeLabel
	// + DummyProperties.SEPARATOR_LABEL_AND_RELTYPE + relationshipType)
	// .get(BEFORE).remove(srcNodeId);
	// }
	//
	// }
	// }

	public void findUsefulNodesSet(int dHops, GraphDatabaseService temporalGraph, HashSet<Integer> allFocusNodes,
			String focus) throws Exception {
		String replacedFocus = focus.replace("|", "").replace(":", "").replace(",", "");
		String filePath = "/Users/Levin/Downloads/" + replacedFocus + "_" + dHops + ".txt";
		File indexOfUsefulsNodeFile = new File(filePath);

		if (indexOfUsefulsNodeFile.exists()) {
			Scanner linReader = new Scanner(indexOfUsefulsNodeFile);

			while (linReader.hasNext()) {
				usefulStaticNodes.add(Integer.parseInt(linReader.nextLine()));
			}
			linReader.close();

		} else {
			for (Integer nodeId : allFocusNodes) {
				usefulStaticNodes.add(nodeId);
			}
			HashSet<Integer> allNodesInTwoHopsSet = new HashSet<>();
			int allNodesInTwoHops = 0;

			for (Integer nodeId : allFocusNodes) {
				TraversalDescription twoHopsTraversal = temporalGraph.traversalDescription().breadthFirst()
						.uniqueness(Uniqueness.NODE_GLOBAL).evaluator(Evaluators.toDepth(dHops));

				for (Node currentNode : twoHopsTraversal.traverse(temporalGraph.getNodeById(nodeId)).nodes()) {
					allNodesInTwoHopsSet.add((int) currentNode.getId());
					allNodesInTwoHops++;
				}
			}

			usefulStaticNodes.addAll(allNodesInTwoHopsSet);

			if (true) {
				System.out.println("allFocusNodes: " + allFocusNodes.size());
				System.out.println("allNodesInTwoHopsSet: " + allNodesInTwoHopsSet.size());
				System.out.println("allNodesInTwoHops: " + allNodesInTwoHops);
				// System.out.println("affectedRels: " + affectedRels);
				// System.out.println("relsSet: " + relsSet.size());

			}
			// relsSet.clear();
			// relsSet = null;
			allNodesInTwoHopsSet.clear();
			allNodesInTwoHopsSet = null;

			System.gc();
			System.runFinalization();

//			BufferedWriter out = new BufferedWriter(new FileWriter(filePath), 32768);
//			for (Integer usefulNodeId : usefulStaticNodes) {
//				out.write(usefulNodeId.toString() + "\n");
//			}
//			out.close();

		}

	}

	// private void addPreviousNodesSet(Integer parentId, Integer thisId, int
	// maxDHops, int currentHop,
	// GraphDatabaseService dataGraph) {
	// if (currentHop > maxDHops) {
	// Node c;
	// for (Relationship r :
	// dataGraph.getNodeById(thisId).getRelationships(Direction.INCOMING)) {
	// c = r.getStartNode();
	// int cId = (int) c.getId();
	//
	// if (Math.abs(maxDHops) > 1)
	// addPreviousNodesSet(parentId, cId, maxDHops, currentHop - 1, dataGraph);
	//
	// // usefulStaticNodes.add(cId);
	//
	// }
	//
	// } else
	// return;
	//
	// }
	//
	// private void addNextNodesSet(Integer parentId, Integer thisId, int
	// maxDHops, int currentHop,
	// GraphDatabaseService dataGraph) {
	// // recursion base case: as Integer as we haven't
	// // exceeded the max number of hops, keep going
	// if (currentHop < maxDHops) {
	// Node c;
	// for (Relationship r :
	// dataGraph.getNodeById(thisId).getRelationships(Direction.OUTGOING)) {
	// c = r.getEndNode();
	// int cId = (int) c.getId();
	// if (Math.abs(maxDHops) > 1)
	// addNextNodesSet(parentId, cId, maxDHops, currentHop + 1, dataGraph);
	//
	// // usefulStaticNodes.add(cId);
	// }
	// } else
	// return;
	//
	// }

	// public void checkForExistenceInNeighborhoodIndex(ILattice lattice, int
	// srcNodeId, int destNodeId, Node srcNode,
	// Node destNode) {
	// // neighborhood indexing by demand for memory usage
	// if
	// (!lattice.getLabelAdjacencyIndexer().dataGraphNodeInfos.containsKey(srcNodeId))
	// {
	// HashSet<Integer> nextNodeIds = new HashSet<Integer>();
	// for (Relationship r : srcNode.getRelationships(Direction.OUTGOING)) {
	// nextNodeIds.add((int) r.getEndNode().getId());
	// }
	//
	// HashSet<Integer> prevNodeIds = new HashSet<Integer>();
	// for (Relationship r : srcNode.getRelationships(Direction.INCOMING)) {
	// prevNodeIds.add((int) r.getStartNode().getId());
	// }
	//
	// dataGraphNodeInfos.put((int) srcNode.getId(),
	// new NodeInfo(srcNode.getLabels().iterator().next().name(),
	// srcNode.getDegree(Direction.INCOMING),
	// srcNode.getDegree(Direction.OUTGOING), prevNodeIds, nextNodeIds));
	// }
	//
	// if
	// (!lattice.getLabelAdjacencyIndexer().dataGraphNodeInfos.containsKey(destNodeId))
	// {
	// HashSet<Integer> nextNodeIds = new HashSet<Integer>();
	// for (Relationship r : destNode.getRelationships(Direction.OUTGOING)) {
	// nextNodeIds.add((int) r.getEndNode().getId());
	// }
	//
	// HashSet<Integer> prevNodeIds = new HashSet<Integer>();
	// for (Relationship r : destNode.getRelationships(Direction.INCOMING)) {
	// prevNodeIds.add((int) r.getStartNode().getId());
	// }
	//
	// dataGraphNodeInfos.put((int) destNode.getId(),
	// new NodeInfo(destNode.getLabels().iterator().next().name(),
	// destNode.getDegree(Direction.INCOMING),
	// destNode.getDegree(Direction.OUTGOING), prevNodeIds, nextNodeIds));
	// }
	// }

}
