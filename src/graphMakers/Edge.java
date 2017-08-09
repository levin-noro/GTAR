package graphMakers;

import java.util.ArrayList;

/**
 * Used for SyntheticFreeScaleGraphGenerator Created by shayan on 7/12/16.
 */
public class Edge {
	// int sourceId;
	ArrayList<Integer> timepoints;
	String relationshipType;

	public Edge() {

	}

	public void setTimeStamps(int startTime, int endTime) {
		if (timepoints == null) {
			timepoints = new ArrayList<Integer>();
		}
		timepoints.add(startTime);
		timepoints.add(endTime);

	}

	public void setTimeStamps(ArrayList<Integer> timepoints) {
		this.timepoints = timepoints;
	}

	public String getTimePointsString() {
		String output = "";
		for (int i = 0; i < this.timepoints.size(); i++) {
			output += this.timepoints.get(i);
			if (i < (this.timepoints.size() - 1))
				output += ";";
		}
		return output;
	}

}
