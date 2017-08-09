//package utilities;
//
//import java.io.File;
//import java.util.HashMap;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.graphdb.factory.GraphDatabaseSettings;
//import org.roaringbitmap.IntConsumer;
//import org.roaringbitmap.RoaringBitmap;
//
//import base.ILattice;
//import base.ILatticeNodeData;
//
//public class Bitmap {
//
//	private static String dataGraphPath;
//	private static GraphDatabaseService dataGraph;
//	public static RoaringBitmap[] rBitmapsOfNodes;
//	public boolean isInitialized = false;
//
//	public Bitmap() {
//
//	}
//
//	public void store(HashMap<Integer, LatticeNode<ILatticeNodeData>> latticeNodeIndex,
//			GraphDatabaseService dataGraph) {
//
//		// for each pattern, we want to save the node ids which is there.
//		long maxNodeId = 0;
//		int nodeCnt = 0;
//		Transaction tx1 = dataGraph.beginTx();
//		for (Node node : dataGraph.getAllNodes()) {
//			maxNodeId = Math.max(maxNodeId, node.getId());
//			nodeCnt++;
//		}
//		tx1.success();
//		tx1.close();
//
//		System.out.println("maxNodeId: " + maxNodeId);
//		System.out.println("nodeCnt: " + nodeCnt);
//
//		// RoaringBitmap[] rBitmapsOfPatterns = new
//		// RoaringBitmap[latticeNodeIndex.size()];
//		// for (Integer patternId : latticeNodeIndex.keySet()) {
//		// rBitmapsOfPatterns[patternId] = new RoaringBitmap();
//		// for (ConcreteMatch concreteMatch :
//		// latticeNodeIndex.get(patternId).getData().matchGraphs) {
//		// for (Long nodeId : concreteMatch.patternNodeOfNeo4jNode.keySet()) {
//		// rBitmapsOfPatterns[patternId].add(nodeId, nodeId + 1);
//		// }
//		// }
//		// }
//
//	}
//
//	public RoaringBitmap[] storeAllPatternIndexByNodeId(ILattice lattice) {
//
//		rBitmapsOfNodes = new RoaringBitmap[(int) Math.max(lattice.getLabelAdjacencyIndexer().maxNodeId + 1,
//				lattice.getLabelAdjacencyIndexer().numberOfNodesInGraph0)];
//
//		for (int i = 0; i < rBitmapsOfNodes.length; i++) {
//			rBitmapsOfNodes[i] = new RoaringBitmap();
//		}
//
//		for (Integer patternId : lattice.getLatticeNodeIndex().keySet()) {
//			for (Integer nodeId : lattice.getLatticeNodeIndex().get(patternId).getData().getMatchedNodes()
//					.getPatternNodeOfNeo4jNode().keySet()) {
//				rBitmapsOfNodes[nodeId.intValue()].add(patternId);
//			}
//
//		}
//
//		for (int i = 0; i < rBitmapsOfNodes.length; i++) {
//			rBitmapsOfNodes[i].runOptimize();
//		}
//
//		isInitialized = true;
//		return rBitmapsOfNodes;
//	}
//
//	// TODO: may be if we give "this" to other consturctor then we don't need to
//	// define it as a static
//	public RoaringBitmap[] storeOnePatternIndexByNodeId(ILattice lattice, Integer patternId) {
//
//		if (lattice.getLatticeNodeIndex().get(patternId) != null) {
//			for (Integer nodeId : lattice.getLatticeNodeIndex().get(patternId).getData().getMatchedNodes()
//					.getPatternNodeOfNeo4jNode().keySet()) {
//				rBitmapsOfNodes[nodeId.intValue()].add(patternId);
//			}
//		}
//		return rBitmapsOfNodes;
//	}
//
//	public void updateBitmapByPairOfPatternAndNodeId(Integer patternId, Integer nodeId) {
//		rBitmapsOfNodes[nodeId].add(patternId);
//	}
//
//	public void printBitmap() {
//
//	}
//
//	public static void main(String[] args) {
//
//		for (int i = 0; i < args.length; i++) {
//			if (args[i].equals("-dataGraphPath")) {
//				dataGraphPath = args[++i];
//			}
//		}
//
//		// initialize data graph
//		File storeDir = new File(dataGraphPath);
//		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
//				.setConfig(GraphDatabaseSettings.pagecache_memory, "6g").newGraphDatabase();
//
//		RoaringBitmap[] rr2 = new RoaringBitmap[5];
//		for (int i = 0; i < 5; i++) {
//			rr2[i] = new RoaringBitmap();
//		}
//		try (Transaction tx1 = dataGraph.beginTx()) {
//
//			for (Node node : dataGraph.getAllNodes()) {
//				int div = ((int) node.getId() % 5);
//				rr2[div].add(((int) node.getId()));
//			}
//
//			System.out.println();
//
//			rr2[0].forEach(new IntConsumer() {
//				@Override
//				public void accept(int value) {
//					System.out.println(value);
//				}
//			});
//
//		} catch (Exception exc) {
//			exc.printStackTrace();
//		}
//
//	}
//
//	public void removeOnePatternIndexForAllNodesHavingIt(Integer patternLatticeNodeIndex) {
//		for (int i = 0; i < rBitmapsOfNodes.length; i++) {
//			rBitmapsOfNodes[i].remove(patternLatticeNodeIndex);
//		}
//	}
//
//	public void removeNodeIdFromPatternId(Integer destNodeId, Integer patternLatticeNodeIndex) {
//		// System.out.println("bitmap.removeNodeIdFromPatternId(" + destNodeId +
//		// ", " + patternLatticeNodeIndex + ")");
//		rBitmapsOfNodes[destNodeId].remove(patternLatticeNodeIndex);
//
//	}
//}
