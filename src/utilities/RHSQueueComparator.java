package utilities;

import java.util.Comparator;

import base.ILatticeNodeData;

public class RHSQueueComparator implements Comparator<LatticeNode<ILatticeNodeData>> {

	// RHS Heuristic
	// the level with lower value (less depth) should be selected first.
	// we select the RHS having more number of intersection with LHS

	@Override
	public int compare(LatticeNode<ILatticeNodeData> n1, LatticeNode<ILatticeNodeData> n2) {
		// TODO: to be checked.
		int compare = Integer.compare(n2.getLevel(), n1.getLevel());
		if (compare != 0) {
			return compare;
		}

		compare = Double.compare(n2.getData().getTotalSupportFrequency(), n1.getData().getTotalSupportFrequency());

		if (compare != 0) {
			return compare;
		}

		return Integer.compare(n1.getData().getPatternLatticeNodeIndex(), n2.getData().getPatternLatticeNodeIndex());

	}

}
