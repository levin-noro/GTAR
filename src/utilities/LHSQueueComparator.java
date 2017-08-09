package utilities;

import java.util.Comparator;

import base.ILatticeNodeData;

public class LHSQueueComparator implements Comparator<LatticeNode<ILatticeNodeData>> {

	// LHS Heuristic
	// It should cover all the search space
	// the level with higher value (more depth) should select first.
	// breaking tie with distinct number of focuses over all the timstamps

	// Maybe in future we can ignore considering top of the tree for LHS, but
	// let's first see the results.
	@Override
	public int compare(LatticeNode<ILatticeNodeData> n1, LatticeNode<ILatticeNodeData> n2) {
		// TODO: to be checked.
		int compare = Integer.compare(n2.getLevel(), n1.getLevel());
		if (compare != 0) {
			return compare;
		}

		compare = Double.compare(n1.getData().getTotalSupportFrequency(), n2.getData().getTotalSupportFrequency());

		if (compare != 0) {
			return compare;
		}

		return Integer.compare(n1.getData().getPatternLatticeNodeIndex(), n2.getData().getPatternLatticeNodeIndex());

	}

}
