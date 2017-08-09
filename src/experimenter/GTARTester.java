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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;

import com.google.common.collect.Sets.SetView;
import com.google.monitoring.runtime.instrumentation.common.com.google.common.collect.Sets;

import base.ILatticeNodeData;
import reducedLattice.heuristic.GTARFinderHeuristic;
import reducedLattice.twoSteps.GTARFinderTwoSteps;
import scala.collection.concurrent.Debug;
import utilities.DebugHelper;
import utilities.DefaultLabeledEdge;
import utilities.DualSimulationHandler;
import utilities.LatticeNode;
import utilities.PatternNode;
import utilities.Rule;
import utilities.Dummy.DummyProperties;

public class GTARTester {

	private static String allFocusLinesPath;
	private static int[] maxAllowedHops;
	private static int[] maxAllowedEdges;
	private static boolean debugMode;
	private static double[] supportThresholds;
	private static double[] confidenceThresholds;
	private static int numberOfSameExperiments;
	private static String dataGraphPath;
	private static int[] deltaTs;

	private static int[] focusIds = new int[] {};

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

			for (int h : maxAllowedHops) {
				for (int e : maxAllowedEdges) {
					for (double s : supportThresholds) {
						for (double c : confidenceThresholds) {
							for (int t : deltaTs) {
								if (e < h)
									continue;

								GTARFinderTwoSteps gTARFinderTwoSteps = new GTARFinderTwoSteps("focusSet.txt", h, e,
										dataGraphPath, debugMode, s, c, t, 0);
								gTARFinderTwoSteps.findGTARs();

								String settingStr = " focus was " + line + ", h:" + h + " , e:" + e + ", s:" + s
										+ ", c:" + c + ", deltaT:" + t;

								System.out.println("GTARFinderTwoSteps Finder: " + settingStr);

								DebugHelper.printingRules(bwTwoSteps, gTARFinderTwoSteps.lattice, settingStr);

								sleepAndWakeUp();

								GTARFinderHeuristic gTARFinderHeuristic = new GTARFinderHeuristic("focusSet.txt", h, e,
										dataGraphPath, debugMode, s, c, t);
								gTARFinderHeuristic.findGTARs();

								settingStr = " focus was " + line + ", h:" + h + " , e:" + e + ", s:" + s + ", c:" + c
										+ ", deltaT:" + t;
								System.out.println("GTARFinderHeuristic Finder: " + settingStr);

								DebugHelper.printingRules(bwHeuristic, gTARFinderHeuristic.lattice, settingStr);

								// check same patterns to have same support:

								// HashSet<Integer> mustBeIds = new
								// HashSet<Integer>();
								// for (int i = 0; i < focusIds.length; i++) {
								// mustBeIds.add(focusIds[i]);
								// }

								ArrayList<UnMatchedIsomorphicEvent> unmatchedIsomorphicEvents = new ArrayList<UnMatchedIsomorphicEvent>();
								for (Integer heuristicPatternId : gTARFinderHeuristic.lattice.latticeNodeIndex
										.keySet()) {
									for (Integer twoStepsPatternId : gTARFinderTwoSteps.lattice.latticeNodeIndex
											.keySet()) {

										LatticeNode<ILatticeNodeData> heuristicEvent = gTARFinderHeuristic.lattice
												.getLatticeNodeIndex().get(heuristicPatternId);
										LatticeNode<ILatticeNodeData> twoStepEvent = gTARFinderTwoSteps.lattice
												.getLatticeNodeIndex().get(twoStepsPatternId);

										boolean isPreIsoChecking = gTARFinderHeuristic.lattice.preIsoChecking(
												heuristicEvent.getData().getPatternGraph(),
												twoStepEvent.getData().getPatternGraph());

										if (isPreIsoChecking) {
											VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = gTARFinderHeuristic.lattice
													.getIsomorphism(heuristicEvent.getData().getPatternGraph(),
															twoStepEvent.getData().getPatternGraph());

											if (iso != null && iso.isomorphismExists()) {
												// System.out.println("heu Id:"
												// + heuristicPatternId + ", two
												// Steps Id:"
												// + twoStepsPatternId +
												// "isPreIsoChecking:" +
												// isPreIsoChecking
												// + " heu graph:" +
												// heuristicEvent.getData() + "
												// two graph:"
												// + heuristicEvent.getData());

												// two isomorphic patterns
												// should
												// have same support and same
												// focus
												// matches:

												if (heuristicEvent.getData().getTotalSupportFrequency() != twoStepEvent
														.getData().getTotalSupportFrequency()) {

													unmatchedIsomorphicEvents.add(
															new UnMatchedIsomorphicEvent(heuristicEvent, twoStepEvent));
												} else {
													HashSet<Integer> set1 = new HashSet<Integer>();
													HashSet<Integer> set2 = new HashSet<Integer>();
													for (PatternNode pn : heuristicEvent.getData().getMatchedNodes()
															.getDataGraphMatchNodeOfAbsPNode().keySet()) {
														if (pn.isFocus()) {
															set1.addAll(heuristicEvent.getData().getMatchedNodes()
																	.getDataGraphMatchNodeOfAbsPNode().get(pn));
														}
													}
													for (PatternNode pn : twoStepEvent.getData().getMatchedNodes()
															.getDataGraphMatchNodeOfAbsPNode().keySet()) {
														if (pn.isFocus()) {
															set2.addAll(twoStepEvent.getData().getMatchedNodes()
																	.getDataGraphMatchNodeOfAbsPNode().get(pn));
														}
													}

													if (!Sets.symmetricDifference(set1, set2).isEmpty()) {
														unmatchedIsomorphicEvents.add(new UnMatchedIsomorphicEvent(
																heuristicEvent, twoStepEvent));
													}
												}

											}
										}
									}
								}

								Collections.sort(unmatchedIsomorphicEvents, new Comparator<UnMatchedIsomorphicEvent>() {
									@Override
									public int compare(UnMatchedIsomorphicEvent u1, UnMatchedIsomorphicEvent u2) {
										return Integer.compare(
												u1.heuristicEvent.getLevel() + u1.twoStepEvent.getLevel(),
												u2.heuristicEvent.getLevel() + u2.twoStepEvent.getLevel());

									}
								});

								System.out.println("\n\n");

								for (UnMatchedIsomorphicEvent uE : unmatchedIsomorphicEvents) {
									System.out.println("heuristicEvent:");
									System.out.println(uE.heuristicEvent.getData() + "\n Parent:"
											+ uE.heuristicEvent.getParent().getData());

									// HashSet<Integer> setParentHeu = new
									// HashSet<Integer>();
									// for (PatternNode pn :
									// uE.heuristicEvent.getParent().getData().getMatchedNodes()
									// .getDataGraphMatchNodeOfAbsPNode().keySet())
									// {
									// if (pn.isFocus()) {
									// setParentHeu.addAll(uE.heuristicEvent.getParent().getData()
									// .getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(pn));
									// break;
									// }
									// }
									// if (!Sets.difference(mustBeIds,
									// setParentHeu).isEmpty()) {
									// System.out.println();
									// }

									System.out.println("twoStepEvent:");
									System.out.println(uE.twoStepEvent.getData() + "\n Parent:"
											+ uE.twoStepEvent.getParent().getData());

									// HashSet<Integer> setParentTwoSteps = new
									// HashSet<Integer>();
									// for (PatternNode pn :
									// uE.twoStepEvent.getParent().getData().getMatchedNodes()
									// .getDataGraphMatchNodeOfAbsPNode().keySet())
									// {
									// if (pn.isFocus()) {
									// setParentTwoSteps.addAll(uE.twoStepEvent.getParent().getData()
									// .getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(pn));
									// break;
									// }
									// }

									// if (!Sets.difference(mustBeIds,
									// setParentTwoSteps).isEmpty()) {
									//
									// Set<Integer> allOfficers =
									// gTARFinderTwoSteps.lattice.allNodesOfFocusType
									// .get("Officer");
									//
									// if (!Sets.difference(mustBeIds,
									// allOfficers).isEmpty()) {
									// System.out.println("AllOfficers:");
									// System.out.println(Sets.difference(mustBeIds,
									// allOfficers));
									// System.out.println();
									// }
									//
									// LatticeNode<ILatticeNodeData> p =
									// uE.twoStepEvent;
									// while (p.getParent() != null) {
									// p = p.getParent();
									// HashSet<Integer> setInner = new
									// HashSet<Integer>();
									// for (PatternNode pn :
									// p.getData().getMatchedNodes()
									// .getDataGraphMatchNodeOfAbsPNode().keySet())
									// {
									// if (pn.isFocus()) {
									// setInner.addAll(p.getData().getMatchedNodes()
									// .getDataGraphMatchNodeOfAbsPNode().get(pn));
									// break;
									// }
									// }
									// if (!Sets.difference(mustBeIds,
									// setInner).isEmpty()) {
									// System.out.println();
									// System.out.println(p.getData());
									// System.out.println(Sets.difference(mustBeIds,
									// setInner));
									// System.out.println();
									// }
									// }
									// }

									PatternNode pnTwoStep = null;
									PatternNode pnHeuristic = null;

									HashSet<Integer> heuristicFocusMatchId = new HashSet<Integer>();
									HashSet<Integer> twoStepsFocusMatchId = new HashSet<Integer>();
									for (PatternNode pn : uE.heuristicEvent.getData().getMatchedNodes()
											.getDataGraphMatchNodeOfAbsPNode().keySet()) {
										if (pn.isFocus()) {
											heuristicFocusMatchId.addAll(uE.heuristicEvent.getData().getMatchedNodes()
													.getDataGraphMatchNodeOfAbsPNode().get(pn));

											pnHeuristic = pn;
											break;
										}
									}
									for (PatternNode pn : uE.twoStepEvent.getData().getMatchedNodes()
											.getDataGraphMatchNodeOfAbsPNode().keySet()) {
										if (pn.isFocus()) {
											twoStepsFocusMatchId.addAll(uE.twoStepEvent.getData().getMatchedNodes()
													.getDataGraphMatchNodeOfAbsPNode().get(pn));

											pnTwoStep = pn;
											break;
										}
									}
									Set<Integer> insideOfTwoStepsButNotInHeuristic = Sets
											.difference(twoStepsFocusMatchId, heuristicFocusMatchId);

									System.out.println("insideOfTwoStepsButNotInHeuristic");
									for (Integer i : insideOfTwoStepsButNotInHeuristic) {
										System.out.println(i + ":" + uE.twoStepEvent.getData().getMatchedNodes()
												.getTimePointsOfAMatch(pnTwoStep, i) + ",");
									}

									Set<Integer> insideOfHeuristicsButNotInTwoSteps = Sets
											.difference(heuristicFocusMatchId, twoStepsFocusMatchId);

									System.out.println("insideOfHeuristicsButNotInTwoSteps");
									for (Integer i : insideOfHeuristicsButNotInTwoSteps) {
										System.out.println(i + ":" + uE.heuristicEvent.getData().getMatchedNodes()
												.getTimePointsOfAMatch(pnHeuristic, i) + ",");
									}

								}

								// check for rule incosistency:

								checkToHaveIdenticalRules(gTARFinderTwoSteps, gTARFinderHeuristic);

								gTARFinderTwoSteps = null;
								gTARFinderHeuristic = null;
								// sleepAndWakeUp();
								// sleepAndWakeUp();

							}
						}
					}
				}
			}
		}

		bwHeuristic.close();
		bwTwoSteps.close();

	}

	private static void checkToHaveIdenticalRules(GTARFinderTwoSteps gTARFinderTwoSteps,
			GTARFinderHeuristic gTARFinderHeuristic) {

		ArrayList<Rule> heuristicRules = new ArrayList<>();
		for (int index : gTARFinderHeuristic.lattice.latticeNodeIndex.keySet()) {
			if (gTARFinderHeuristic.lattice.latticeNodeIndex.get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : gTARFinderHeuristic.lattice.latticeNodeIndex.get(index).getData().getRulesOfThis()) {
					heuristicRules.add(rule);

				}
			}
		}

		ArrayList<Rule> twoStepsRules = new ArrayList<>();
		for (int index : gTARFinderTwoSteps.lattice.latticeNodeIndex.keySet()) {
			if (gTARFinderTwoSteps.lattice.latticeNodeIndex.get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : gTARFinderTwoSteps.lattice.latticeNodeIndex.get(index).getData().getRulesOfThis()) {
					twoStepsRules.add(rule);
				}
			}
		}

		if (twoStepsRules.size() != heuristicRules.size()) {
			System.err.println(
					"twoStepsRules.size(): " + twoStepsRules.size() + " but heuristicRules:" + heuristicRules.size());
		}

		for (Rule tr : twoStepsRules) {
			boolean foundCorresponding = false;
			for (Rule hr : heuristicRules) {

				if (isoChecking(gTARFinderTwoSteps, tr.lhs.getData().getPatternGraph(),
						hr.lhs.getData().getPatternGraph())
						&& isoChecking(gTARFinderTwoSteps, tr.rhs.getData().getPatternGraph(),
								hr.rhs.getData().getPatternGraph())) {
					heuristicRules.remove(hr);
					foundCorresponding = true;
					break;
				}

			}

			if (!foundCorresponding) {
				System.out.println("corresponding of the following has not been found in hr!");
				System.out.println(tr);
				System.out.println(tr.lhs.getData());
				System.out.println(tr.rhs.getData());
			}
		}

		for (Rule hr : heuristicRules) {
			System.out.println("corresponding of the following has not been found in tr!");
			System.out.println(hr);
			System.out.println(hr.lhs.getData());
			System.out.println(hr.rhs.getData());
		}

	}

	private static boolean isoChecking(GTARFinderTwoSteps gTARFinderTwoSteps,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> pg1,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> pg2) {

		boolean isPreIsoChecking = gTARFinderTwoSteps.lattice.preIsoChecking(pg1, pg2);

		if (isPreIsoChecking) {
			VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = gTARFinderTwoSteps.lattice
					.getIsomorphism(pg1, pg2);

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
		Thread.sleep(4000);
		System.gc();
		System.runFinalization();
		Thread.sleep(4000);
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

class UnMatchedIsomorphicEvent {
	LatticeNode<ILatticeNodeData> heuristicEvent;
	LatticeNode<ILatticeNodeData> twoStepEvent;

	public UnMatchedIsomorphicEvent(LatticeNode<ILatticeNodeData> heuristicEvent,
			LatticeNode<ILatticeNodeData> twoStepEvent) {
		this.heuristicEvent = heuristicEvent;
		this.twoStepEvent = twoStepEvent;
	}
}
