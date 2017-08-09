package experimenter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Date;

import reducedLattice.heuristic.GTARFinderHeuristic;
import reducedLattice.twoSteps.GTARFinderTwoSteps;
import utilities.DebugHelper;
import utilities.Dummy.DummyProperties;

public class GTARExperimenterNonOpt {

	private static String allFocusLinesPath;
	private static int[] maxAllowedHops;
	private static int[] maxAllowedEdges;
	private static boolean debugMode;
	private static double[] supportThresholds;
	private static double[] confidenceThresholds;
	private static int numberOfSameExperiments;
	private static String dataGraphPath;
	private static int[] deltaTs;

	// TODO: one possible bottleneck might be the # of timestamps specially in
	// terms of memory, so we may consider a part of the graph timestamps

	// TODO:

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-allFocusLinesPath")) {
				allFocusLinesPath = args[++i];
			} else if (args[i].equals("-maxAllowedHops")) {
				maxAllowedHops = getArrOutOfCSV(maxAllowedHops, args[++i]);
			} else if (args[i].equals("-maxAllowedEdges")) {
				maxAllowedEdges = getArrOutOfCSV(maxAllowedEdges, args[++i]);
			} else if (args[i].equals("-debugMode")) {
				debugMode = Boolean.parseBoolean(args[++i]);
			} else if (args[i].equals("-supportThresholds")) {
				supportThresholds = getArrOutOfCSV(supportThresholds, args[++i]);
			} else if (args[i].equals("-confidenceThresholds")) {
				confidenceThresholds = getArrOutOfCSV(confidenceThresholds, args[++i]);
			} else if (args[i].equals("-numberOfSameExperiments")) {
				numberOfSameExperiments = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-deltaTs")) {
				deltaTs = getArrOutOfCSV(deltaTs, args[++i]);
			}
			// else if (args[i].equals("-qualityVsTime")) {
			// DummyProperties.qualityVsTime = Boolean.parseBoolean(args[++i]);
			// }
			else if (args[i].equals("-qualitySaveIntervals")) {
				DummyProperties.qualitySaveIntervalInMilliSeconds = Integer.parseInt(args[++i]);
			}
		}

		if (allFocusLinesPath == null || dataGraphPath == null || supportThresholds == null
				|| supportThresholds.length == 0 || maxAllowedEdges == null || maxAllowedEdges.length == 0
				|| maxAllowedHops == null || maxAllowedHops.length == 0 || confidenceThresholds == null
				|| confidenceThresholds.length == 0 || deltaTs == null || deltaTs.length == 0) {
			throw new Exception(
					"input parameters: allFocusLinesPath, dataGraphPath, supportThresholds,  maxAllowedEdges, maxAllowedEdges, maxAllowedHops, confidenceThresholds, deltaTs");
		} else {
			System.out.println("-allFocusLinesPath  " + allFocusLinesPath + "\n -dataGraphPath:" + dataGraphPath
					+ "\n -maxAllowedHops: " + Arrays.toString(maxAllowedHops) + "\n -maxAllowedEdges:"
					+ Arrays.toString(maxAllowedEdges) + "\n -supportThresholds: " + Arrays.toString(supportThresholds)
					+ "\n -confidenceThresholds: " + Arrays.toString(confidenceThresholds) + "\n -deltaTs: "
					+ Arrays.toString(deltaTs) + "\n -numberOfSameExperiments:  " + numberOfSameExperiments

			);
		}

		DummyProperties.debugMode = debugMode;

		// read from each line of all focus lines path and create a
		// focusSetFile....
		FileInputStream fis = new FileInputStream(allFocusLinesPath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		// printingRules writers
		File foutheuristic = new File("heuristicRules.txt");
		FileOutputStream fosheuristic = new FileOutputStream(foutheuristic);
		BufferedWriter bwHeuristic = new BufferedWriter(new OutputStreamWriter(fosheuristic));

		File foutTwoSteps = new File("twoStepsRules.txt");
		FileOutputStream fosTwoSteps = new FileOutputStream(foutTwoSteps);
		BufferedWriter bwTwoSteps = new BufferedWriter(new OutputStreamWriter(fosTwoSteps));

		String line = null;
		// a focus set line
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				continue;

			File fout = new File("focusSet.txt");
			FileOutputStream fos = new FileOutputStream(fout);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(line.split(" = ")[0]);
			bw.close();

			boolean goToNextFocus = false;
			for (int h : maxAllowedHops) {
				if (goToNextFocus)
					break;
				for (int e : maxAllowedEdges) {
					if (goToNextFocus)
						break;
					for (double s : supportThresholds) {
						if (goToNextFocus)
							break;
						for (double c : confidenceThresholds) {
							if (goToNextFocus)
								break;
							for (int t : deltaTs) {
								if (goToNextFocus)
									break;

								if (e < h)
									continue;

								DummyProperties.hasOptimization = false;
								for (int exp = 0; exp < numberOfSameExperiments; exp++) {
									GTARFinderTwoSteps gTARFinderTwoSteps = new GTARFinderTwoSteps("focusSet.txt", h, e,
											dataGraphPath, debugMode, s, c, t, 0);
									gTARFinderTwoSteps.findGTARs();

									String settingStr = " focus was " + line + ", h:" + h + " , e:" + e + ", s:" + s
											+ ", c:" + c + ", deltaT:" + t;

									System.out.println("NON-OPT GTARFinderTwoSteps Finder: exp " + exp + settingStr);

									gTARFinderTwoSteps = null;
									sleepAndWakeUp();
								}

								DummyProperties.hasOptimization = false;
								for (int exp = 0; exp < numberOfSameExperiments; exp++) {
									GTARFinderHeuristic gTARFinderHeuristic = new GTARFinderHeuristic("focusSet.txt", h,
											e, dataGraphPath, debugMode, s, c, t);
									gTARFinderHeuristic.findGTARs();

									String settingStr = " focus was " + line + ", h:" + h + " , e:" + e + ", s:" + s
											+ ", c:" + c + ", deltaT:" + t;
									System.out.println("NON-OPT  GTARFinderHeuristic Finder: exp " + exp + settingStr);

									// DebugHelper.printingRules(bwHeuristic,
									// gTARFinderHeuristic.lattice, settingStr);

									gTARFinderHeuristic = null;
									sleepAndWakeUp();
								}

							}
						}
					}
				}
			}
		}

		bwHeuristic.close();
		bwTwoSteps.close();
	}

	private static void sleepAndWakeUp() throws Exception {
		System.out.println("sleeping..." + new Date());
		System.gc();
		System.runFinalization();
		Thread.sleep(5000);
		System.gc();
		System.runFinalization();
		Thread.sleep(5000);
		System.out.println("waking up..." + new Date());
	}

	private static double[] getArrOutOfCSV(double[] doubleArr, String string) {
		String[] strArray = string.split(",");
		doubleArr = new double[strArray.length];
		for (int i = 0; i < strArray.length; i++) {
			doubleArr[i] = Double.parseDouble(strArray[i]);
		}
		return doubleArr;
	}

	private static int[] getArrOutOfCSV(int[] intArr, String string) {
		String[] strArray = string.split(",");
		intArr = new int[strArray.length];
		for (int i = 0; i < strArray.length; i++) {
			intArr[i] = Integer.parseInt(strArray[i]);
		}
		return intArr;
	}
}
