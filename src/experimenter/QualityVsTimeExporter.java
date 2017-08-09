package experimenter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class QualityVsTimeExporter {

	public static void main(String[] args) throws Exception {
		QualityVsTimeExporter qs = new QualityVsTimeExporter();
		qs.run();

	}

	private String base = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/panamaQVsT/QVsT/";

	private void run() throws Exception {
		String[] heuPaths = new String[] { "QualityVsTime_Heuristic1.csv", "QualityVsTime_Heuristic2.csv"};
		String[] twoStepsPaths = new String[] { "QualityVsTime_TwoSteps2.csv", "QualityVsTime_TwoSteps3.csv",
				"QualityVsTime_TwoSteps4.csv","QualityVsTime_TwoSteps5.csv" };

		File fout = new File(base + "QvsTChartData.csv");
		FileOutputStream fos = new FileOutputStream(fout);

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

		ArrayList<ConfidenceElements> heuConfElements = getConfidenceElements(heuPaths);
		ArrayList<ConfidenceElements> twoStepsConfElements = getConfidenceElements(twoStepsPaths);

		for (int i = 0; i < 1500; i++) {
			double accumulatedHeu = 0d;
			double accumulatedTwo = 0d;

			for (ConfidenceElements ce : heuConfElements) {
				accumulatedHeu += ce.confidence[i];
			}
			for (ConfidenceElements ce : twoStepsConfElements) {
				accumulatedTwo += ce.confidence[i];
			}

			if (i % 25 == 0) {
				bw.write(i + "," + (accumulatedHeu / 100.0) + "," + (accumulatedTwo / 100.0));
				bw.newLine();
			}
		}
		bw.close();
	}

	private ArrayList<ConfidenceElements> getConfidenceElements(String[] heuPaths) throws Exception {
		ArrayList<ConfidenceElements> confElementsArr = new ArrayList<ConfidenceElements>();

		for (String heuPath : heuPaths) {
			FileInputStream fis = new FileInputStream(base + heuPath);

			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			String line = br.readLine();
			while ((line = br.readLine()) != null) {
				String[] splittedLine = line.split(",");
				Double maxConf = 1d;
				ConfidenceElements confElements = new ConfidenceElements();
				for (int i = 8; i < 1508; i++) {
					confElements.confidence[i - 8] = Double.parseDouble(splittedLine[i]);
					maxConf = Math.max(maxConf, Double.parseDouble(splittedLine[i]));
				}

				for (int i = 0; i < 1500; i++) {
					confElements.confidence[i] = confElements.confidence[i] / maxConf;
				}
				confElementsArr.add(confElements);
			}

			br.close();
		}

		return confElementsArr;

	}

}

class ConfidenceElements {
	double[] confidence = new double[1500];
}
