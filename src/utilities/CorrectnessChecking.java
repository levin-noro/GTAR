package utilities;

import java.util.Iterator;

import base.ILatticeNodeData;

public class CorrectnessChecking {
	public static void checkingDownwardProperty(LatticeNode<ILatticeNodeData> prefixTreeNode) {
		Iterator<LatticeNode<ILatticeNodeData>> prefixTreeNodeItr = prefixTreeNode.getChildren().iterator();
		while (prefixTreeNodeItr.hasNext()) {
			LatticeNode<ILatticeNodeData> child = prefixTreeNodeItr.next();
			if (prefixTreeNode.getLevel() > 1 && child.getData().getTotalSupportFrequency() > prefixTreeNode.getData()
					.getTotalSupportFrequency()) {
				System.err.println("parent: " + prefixTreeNode.getData().getMappedGraphString() + " -> child: "
						+ child.getData().getMappedGraphString() + " Support: "
						+ prefixTreeNode.getData().getTotalSupportFrequency() + " -> "
						+ child.getData().getTotalSupportFrequency());
			}
		}

	}
}
