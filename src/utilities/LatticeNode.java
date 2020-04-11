package utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import base.ILatticeNodeData;

public class LatticeNode<T> {

	private List<LatticeNode<T>> children = new ArrayList<LatticeNode<T>>();

	// linkedNode will link this PTnode to another PTNode with the same pattern.
	private List<LatticeNode<T>> linkedNodes;

	private HashSet<LatticeNode<T>> childrenLinksSet = new HashSet<LatticeNode<T>>();

	// each node has just one parent. However, some upper-level nodes can reach
	// to this node using linkedNode.
	private LatticeNode<T> parent = null;
	private List<LatticeNode<T>> superNodeLink = null;

	private T data = null;

	private Integer nodeLevel = null;

	public LatticeNode(T data) {
		this.data = data;
	}

	public LatticeNode(T data, LatticeNode<T> parent) {
		this.data = data;
		this.parent = parent;
	}

	public List<LatticeNode<T>> getChildren() {
		return children;
	}

	public HashSet<LatticeNode<T>> getChildrenLinksSet() {
		return childrenLinksSet;
	}

	public List<LatticeNode<T>> getLinkedNodes() {
		return linkedNodes;
	}

	public void setParent(LatticeNode<T> parent) {
		this.parent = parent;
	}

	public void setSuperNodeLink(LatticeNode<T> superLinkNode) {

		if (this.superNodeLink == null) {
			this.superNodeLink = new ArrayList<LatticeNode<T>>();
		}
		this.superNodeLink.add(superLinkNode);
	}

	public void addChild(T data) {
		LatticeNode<T> child = new LatticeNode<T>(data);
		child.setParent(this);
		this.children.add(child);
		this.childrenLinksSet.add(child);
		child.nodeLevel = this.nodeLevel + 1;
	}

	public int getLevel() {
		return this.nodeLevel;
	}

	public void addChild(LatticeNode<T> child) {
		child.setParent(this);
		this.children.add(child);
		this.childrenLinksSet.add(child);
		child.nodeLevel = this.nodeLevel + 1;
	}

	public void addNodeLink(LatticeNode<T> nodeLink) {
		if (this.linkedNodes == null) {
			this.linkedNodes = new ArrayList<LatticeNode<T>>();
		}
		this.linkedNodes.add(nodeLink);
		this.childrenLinksSet.add(nodeLink);
		nodeLink.setSuperNodeLink(this);
	}

	public T getData() {
		return this.data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public boolean isRoot() {
		return (this.parent == null);
	}

	public boolean isLeaf() {
		if (this.children.size() == 0)
			return true;
		else
			return false;
	}

	public void removeParent() {
		this.parent = null;
	}

	public void setRootLevel() {
		this.nodeLevel = 0;
	}

	public LatticeNode<T> getParent() {
		return this.parent;
	}

	public List<LatticeNode<T>> getSuperNodeLinks() {
		return this.superNodeLink;
	}

	public void removeChildren(LatticeNode<T> tempProcessingNode) {
		if (this.children != null)
			this.children.remove(tempProcessingNode);

		this.childrenLinksSet.remove(tempProcessingNode);
	}

	public void removeLinkedNode(LatticeNode<T> tempProcessingNode) {
		if (this.linkedNodes != null)
			this.linkedNodes.remove(tempProcessingNode);

		this.childrenLinksSet.remove(tempProcessingNode);
	}

	public void removeAllChildren(LatticeNode<ILatticeNodeData> tempProcessingNode) {
		this.children = null;
		this.childrenLinksSet.clear();
	}

	public void removeAllReferences() {

		this.getParent().removeChildren(this);
		if (this.getLinkedNodes() != null) {
			for (LatticeNode<T> superLinkNode : this.getLinkedNodes()) {
				superLinkNode.removeLinkedNode(this);
			}
		}
		this.setParent(null);

		this.nodeLevel = null;
		children = null;
		linkedNodes = null;
		parent = null;
		superNodeLink = null;
		data = null;

	}
}
