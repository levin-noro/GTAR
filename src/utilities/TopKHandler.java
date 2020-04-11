package utilities;

import java.util.HashSet;

public class TopKHandler {
	/**
	 * This function will guarantee that we generate our top-k in maximal
	 * frequent ones so top-k don't include each other.
	 * 
	 * @throws Exception
	 **/
	// public static void findTopK(HashSet<Rule> frequentRules,
	// ILattice lattice, int k, LatticeNode<ILatticeNodeData> latticeNode,
	// double threshold)
	// throws Exception {
	//
	// HashSet<Integer> visitedLatticeNodes = new HashSet<Integer>();
	// findTopK(topKFrequentPatterns, lattice, k, latticeNode, threshold,
	// visitedLatticeNodes);
	//
	// }

	// private static void
	// findTopK(MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>>
	// topKFrequentPatterns,
	// ILattice lattice, int k, LatticeNode<ILatticeNodeData> latticeNode,
	// double threshold,
	// HashSet<Integer> visitedLatticeNodes) throws Exception {
	//
	// // if (visitedLatticeNodes.isEmpty()) {
	// // for (Integer patternId :
	// // lattice.getLatticeNodeIndex().keySet()) {
	// //
	// lattice.getLatticeNodeIndex().get(patternId).getData().setCanBeMaximalFrequent(true);
	// // }
	// // }
	//
	// visitedLatticeNodes.add(latticeNode.getData().getPatternLatticeNodeIndex());
	//
	// ArrayList<LatticeNode<ILatticeNodeData>> childrenLatticeNodes = new
	// ArrayList<LatticeNode<ILatticeNodeData>>();
	// childrenLatticeNodes.addAll(latticeNode.getChildren());
	// if (latticeNode.getLinkedNodes() != null) {
	// childrenLatticeNodes.addAll(latticeNode.getLinkedNodes());
	// }
	//
	// for (LatticeNode<ILatticeNodeData> child : childrenLatticeNodes) {
	// if
	// (visitedLatticeNodes.contains(child.getData().getPatternLatticeNodeIndex()))
	// {
	// continue;
	// }
	// if (child.getData().getTotalSupportFrequency() < threshold &&
	// child.getData().getFoundAllFocuses()) {
	// // because of downward property
	// child.getData().setFrequent(false);
	// child.getData().setCanBeMaximalFrequent(false);
	// child.getData().setMaximalFrequent(false, latticeNode, lattice);
	//
	// child.getData().setMinimalInFrequent(true, latticeNode, lattice);
	//
	// } else {
	// findTopK(topKFrequentPatterns, lattice, k, child, threshold,
	// visitedLatticeNodes);
	// }
	// }
	//
	// // if it's not a frequent pattern
	// if (latticeNode.getData().getTotalSupportFrequency() < threshold ||
	// !latticeNode.getData().isCorrect()
	// || !latticeNode.getData().isValid()) {
	// return;
	// }
	// // if it's frequent but it cannot be maximal
	// else if (!latticeNode.getData().canBeMaximalFrequent()) {
	// latticeNode.getData().setFrequent(true);
	// latticeNode.getData().setMaximalFrequent(false, latticeNode, lattice);
	//
	// if (latticeNode.getParent() != null)
	// latticeNode.getParent().getData().setCanBeMaximalFrequent(false);
	//
	// if (latticeNode.getSuperNodeLinks() != null) {
	// for (LatticeNode<ILatticeNodeData> superLink :
	// latticeNode.getSuperNodeLinks()) {
	// superLink.getData().setCanBeMaximalFrequent(false);
	// }
	// }
	//
	// }
	// // it's frequent and it can be maximal
	// else if (latticeNode.getData().canBeMaximalFrequent()) {
	// latticeNode.getData().setFrequent(true);
	// latticeNode.getData().setCanBeMaximalFrequent(true);
	// latticeNode.getData().setMaximalFrequent(true, latticeNode, lattice);
	//
	// if (latticeNode.getParent() != null)
	// latticeNode.getParent().getData().setCanBeMaximalFrequent(false);
	//
	// if (latticeNode.getSuperNodeLinks() != null) {
	// for (LatticeNode<ILatticeNodeData> superLink :
	// latticeNode.getSuperNodeLinks()) {
	// superLink.getData().setCanBeMaximalFrequent(false);
	// }
	// }
	//
	// // // TODO: thinking more about it
	//
	// // latticeNode.getData().addToTopK(lattice, latticeNode);
	// }
	//
	// }

	public static void printTopK(HashSet<Rule> frequentRules) {
		System.out.println();
		System.out.println("print top-k list:");
		System.out.println("size: " + frequentRules.size());
		for (Rule rule : frequentRules) {
			System.out.println(
					rule.lhs.getData().getMappedGraphString() + " =>" + rule.rhs.getData().getMappedGraphString()
							+ " support: " + rule.support + " , confidence:" + rule.confidence);
		}

	}

}
