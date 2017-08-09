package utilities;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PatternNodeIdPair {
	public PatternNode patternNode;
	public Integer dataNodeId;

	public PatternNodeIdPair(PatternNode patternNode, Integer dataNodeId) {
		this.dataNodeId = dataNodeId;
		this.patternNode = patternNode;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
				append(patternNode).append(dataNodeId).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PatternNodeIdPair))
			return false;
		if (obj == this)
			return true;

		PatternNodeIdPair rhs = (PatternNodeIdPair) obj;
		return new EqualsBuilder().append(patternNode, rhs.patternNode).append(dataNodeId, rhs.dataNodeId).isEquals();
	}

	@Override
	public String toString() {
		return this.patternNode + "___" + this.dataNodeId;
	}
}
