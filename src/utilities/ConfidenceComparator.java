package utilities;

import java.util.Comparator;

public class ConfidenceComparator implements Comparator<Rule> {

	@Override
	public int compare(Rule r1, Rule r2) {
		return Double.compare(r1.confidence, r2.confidence);
	}

}
