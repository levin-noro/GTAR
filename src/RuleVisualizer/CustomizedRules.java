package RuleVisualizer;

import java.util.ArrayList;
import java.util.List;

public class CustomizedRules {
	List<CustomizedRule> customizedRules = new ArrayList<>();
	String settingInfo;

	public CustomizedRules(List<CustomizedRule> customizedRules, String settingInfo) {
		this.customizedRules = customizedRules;
		this.settingInfo = settingInfo;
	}
}
