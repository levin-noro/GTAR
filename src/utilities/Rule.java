package utilities;

import java.util.ArrayList;

import base.ILatticeNodeData;

public class Rule {
	public LatticeNode<ILatticeNodeData> lhs;
	public LatticeNode<ILatticeNodeData> rhs;
	public Double support;
	public Double confidence;
	public ArrayList<TimepointsPair> minOccurrences;
	public int minOccurrencesSize = 0;
	public double discoveredTime = 0d;

	public Rule(LatticeNode<ILatticeNodeData> lhs, LatticeNode<ILatticeNodeData> rhs, Double support, Double confidence,
			ArrayList<TimepointsPair> minOccurrences, double discoveredTime) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.support = support;
		this.confidence = confidence;
		this.minOccurrences = minOccurrences;
		if (minOccurrences != null) {
			minOccurrencesSize = minOccurrences.size();
		}
		this.discoveredTime = discoveredTime;
	}

	public Rule(LatticeNode<ILatticeNodeData> lhs, LatticeNode<ILatticeNodeData> rhs, double discoveredTime) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.discoveredTime = discoveredTime;
	}

	public Rule(LatticeNode<ILatticeNodeData> lhs, LatticeNode<ILatticeNodeData> rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lhs.getData().getPatternLatticeNodeIndex();
		result = prime * result + rhs.getData().getPatternLatticeNodeIndex();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rule other = (Rule) obj;
		if (lhs != other.lhs)
			return false;
		if (rhs != other.rhs)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return lhs.getData().getPatternLatticeNodeIndex() + "=>" + rhs.getData().getPatternLatticeNodeIndex() + " s:"
				+ this.support + ", c:" + this.confidence;// + ", min occ:" +
															// this.minOccurrences.size();
	}
}
