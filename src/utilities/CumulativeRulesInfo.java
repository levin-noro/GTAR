package utilities;

public class CumulativeRulesInfo {

	public double support;
	public double confidence;
	public int numberOfRules;

	public CumulativeRulesInfo(double support, double confidence, int numberOfRules) {
		this.support = support;
		this.confidence = confidence;
		this.numberOfRules = numberOfRules;
	}
}
