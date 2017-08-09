package experimenter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;

import reducedLattice.heuristic.GTARFinderHeuristic;
import reducedLattice.twoSteps.GTARFinderTwoSteps;
import utilities.DebugHelper;
import utilities.DefaultLabeledEdge;
import utilities.DualSimulationHandler;
import utilities.PatternNode;
import utilities.Rule;
import utilities.Dummy.DummyProperties;

public class GTARPredictionExperimenter {

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

		String trainGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/Train.db";
		String testGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/specificPanama/Test.db";

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

								DummyProperties.hasOptimization = true;

								//
								GTARFinderHeuristic gTARFinderHeuristicTrain = new GTARFinderHeuristic("focusSet.txt",
										h, e, trainGraphPath, debugMode, s, c, t);
								gTARFinderHeuristicTrain.findGTARs();
								String settingStrTrain = " focus was " + line + ", h:" + h + " , e:" + e + ", s:" + s
										+ ", c:" + c + ", deltaT:" + t;
								DebugHelper.printingRules(bwHeuristic, gTARFinderHeuristicTrain.lattice,
										settingStrTrain);
								// gTARFinderHeuristicTrain = null;
								sleepAndWakeUp();

								// ArrayList<Rule> gTARFinderHeuristicTrainRules
								// = new ArrayList<>();
								// for (int index :
								// gTARFinderHeuristicTrain.lattice.latticeNodeIndex.keySet())
								// {
								// if
								// (gTARFinderHeuristicTrain.lattice.latticeNodeIndex.get(index).getData()
								// .getRulesOfThis().size() > 0) {
								// for (Rule rule :
								// gTARFinderHeuristicTrain.lattice.latticeNodeIndex.get(index)
								// .getData().getRulesOfThis()) {
								// gTARFinderHeuristicTrainRules.add(rule);
								//
								// }
								// }
								// }

								// for (Rule rule :
								// gTARFinderHeuristicTrain.lattice.latticeNodeIndex.get(index).getData()
								// .getRulesOfThis()) {
								//
								// DualSimulationHandler.gtarVerification2(rule,
								// 0, 0, 5);
								//
								// }

								//
								GTARFinderHeuristic gTARFinderHeuristicTest = new GTARFinderHeuristic("focusSet.txt", h,
										e, testGraphPath, debugMode, 0.01, 0.01, t);
								gTARFinderHeuristicTest.findGTARs();
								String settingStrTest = " focus was " + line + ", h:" + h + " , e:" + e + ", s:" + s
										+ ", c:" + c + ", deltaT:" + t;
								DebugHelper.printingRules(bwHeuristic, gTARFinderHeuristicTest.lattice, settingStrTest);
								// gTARFinderHeuristicTest = null;
								sleepAndWakeUp();

								checkIdenticalRules(gTARFinderHeuristicTrain, gTARFinderHeuristicTest);

							}
						}
					}
				}
			}
		}

		br.close();
		bwHeuristic.close();
		bwTwoSteps.close();

	}

	private static void checkIdenticalRules(GTARFinderHeuristic gTARFinderHeuristicTrain,
			GTARFinderHeuristic gTARFinderHeuristicTest) {

		ArrayList<Rule> gTARFinderHeuristicTrainRules = new ArrayList<>();
		for (int index : gTARFinderHeuristicTrain.lattice.latticeNodeIndex.keySet()) {
			if (gTARFinderHeuristicTrain.lattice.latticeNodeIndex.get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : gTARFinderHeuristicTrain.lattice.latticeNodeIndex.get(index).getData()
						.getRulesOfThis()) {
					gTARFinderHeuristicTrainRules.add(rule);

				}
			}
		}

		ArrayList<Rule> gTARFinderHeuristicTestRules = new ArrayList<>();
		for (int index : gTARFinderHeuristicTest.lattice.latticeNodeIndex.keySet()) {
			if (gTARFinderHeuristicTest.lattice.latticeNodeIndex.get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : gTARFinderHeuristicTest.lattice.latticeNodeIndex.get(index).getData()
						.getRulesOfThis()) {
					gTARFinderHeuristicTestRules.add(rule);
				}
			}
		}

		for (Rule tr : gTARFinderHeuristicTrainRules) {
			for (Rule hr : gTARFinderHeuristicTestRules) {
				if (isoChecking(gTARFinderHeuristicTrain, tr.lhs.getData().getPatternGraph(),
						hr.lhs.getData().getPatternGraph())
						&& isoChecking(gTARFinderHeuristicTest, tr.rhs.getData().getPatternGraph(),
								hr.rhs.getData().getPatternGraph())) {
					System.out.println("train: " + tr.confidence + " test: " + hr.confidence);
				}
			}
		}
	}

	private static boolean isoChecking(GTARFinderHeuristic gTARFinder,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> pg1,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> pg2) {

		boolean isPreIsoChecking = gTARFinder.lattice.preIsoChecking(pg1, pg2);

		if (isPreIsoChecking) {
			VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = gTARFinder.lattice.getIsomorphism(pg1,
					pg2);

			if (iso != null && iso.isomorphismExists()) {
				return true;
			}
		}
		return false;
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
