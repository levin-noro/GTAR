package reducedLattice.twoSteps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

import base.ILatticeNodeData;
import reducedLattice.heuristic.LatticeReducedHeuristicOpt;
import utilities.CumulativeRulesInfo;
import utilities.DualSimulationHandler;
import utilities.Dummy;
import utilities.LatticeNode;
import utilities.Rule;
import utilities.Dummy.DummyProperties;

public class RuleGenerator {

	private LatticeReducedHeuristicOpt lattice;
	private int deltaT;
	private double supportThreshold;
	private double confidenceThreshold;
	private int minTimestamp;
	private int maxTimestamp;
	public double ruleGeneratorStartTime;

	public int lastQualitySavedInterval = 0;
	public TreeMap<Integer, CumulativeRulesInfo> qualityOfTime = new TreeMap<Integer, CumulativeRulesInfo>();
	private double startTime;
	boolean timeout = false;
	public double timebound = 0d;

//	public RuleGenerator(LatticeReducedTwoStepsOpt lattice, int maxAllowedEdges, int deltaT, double supportThreshold,
//			double confidenceThreshold, int minTimestamp, int maxTimestamp) {
//
//		this.lattice = lattice;
//		this.deltaT = deltaT;
//		this.supportThreshold = supportThreshold;
//		this.confidenceThreshold = confidenceThreshold;
//		this.minTimestamp = minTimestamp;
//		this.maxTimestamp = maxTimestamp;
//
//	}

	public RuleGenerator(LatticeReducedHeuristicOpt lattice, int maxAllowedEdges, int deltaT,
			double supportThreshold, double confidenceThreshold, int minTimestamp, int maxTimestamp) {
		this.lattice = lattice;
		this.deltaT = deltaT;
		this.supportThreshold = supportThreshold;
		this.confidenceThreshold = confidenceThreshold;
		this.minTimestamp = minTimestamp;
		this.maxTimestamp = maxTimestamp;	
		
	}

	public RuleGenerator(LatticeReducedTwoStepsOpt lattice2, int maxAllowedEdges, int deltaT2, double supportThreshold2,
			double confidenceThreshold2, int minTimestamp2, int maxTimestamp2) {
		// TODO Auto-generated constructor stub
	}

	public void generateRules(double startTime, double ruleGeneratorStartTime) {

		this.startTime = startTime;
		this.ruleGeneratorStartTime = ruleGeneratorStartTime;

		int maxNumberOfPatterns = lattice.getLatticeNodeIndex().size();

		HashMap<Integer, ArrayList<LatticeNode<ILatticeNodeData>>> latticeNodesOfLevel = new HashMap<Integer, ArrayList<LatticeNode<ILatticeNodeData>>>();

		int maxLevel = 0;
		// pattern level indexing
		for (Integer latticeNodeIndex : lattice.getLatticeNodeIndex().keySet()) {
			LatticeNode<ILatticeNodeData> pattern = lattice.getLatticeNodeIndex().get(latticeNodeIndex);
			latticeNodesOfLevel.putIfAbsent(pattern.getLevel(), new ArrayList<>());
			latticeNodesOfLevel.get(pattern.getLevel()).add(pattern);
			maxLevel = Math.max(maxLevel, pattern.getLevel());
		}

		if (DummyProperties.debugMode)
			System.out.println("after level indexing");

		for (int patternIndex = 1; patternIndex < maxNumberOfPatterns; patternIndex++) {

			LatticeNode<ILatticeNodeData> fixedLHS = lattice.getLatticeNodeIndex().get(patternIndex);

			// if (fixedLHS == null || fixedLHS.getData() == null) {
			// System.out.println();
			// }

			if (DummyProperties.hasOptimization && fixedLHS.getData().getTotalSupportFrequency() < supportThreshold)
				continue;

			lattice.fixedLHSCnt++;
			if (DummyProperties.debugMode) {
				System.out.println(lattice.fixedLHSCnt + ": FIXED AS LHS:"
						+ fixedLHS.getData().getPatternLatticeNodeIndex() + " #patterns:" + maxNumberOfPatterns);
				System.out.println(fixedLHS.getData());
			}

			HashSet<LatticeNode<ILatticeNodeData>> shouldVisitRHS = new HashSet<LatticeNode<ILatticeNodeData>>();
			for (int level = maxLevel; level >= 1; level--) {
				shouldVisitRHS.addAll(latticeNodesOfLevel.get(level));
			}

			for (int level = maxLevel; level >= 1; level--) {

				if (DummyProperties.debugMode)
					System.out.println("level: " + level);

				if (shouldVisitRHS.isEmpty())
					break;

				for (LatticeNode<ILatticeNodeData> rhsCandidate : latticeNodesOfLevel.get(level)) {
					if (/*DummyProperties.hasOptimization &&*/
							!shouldVisitRHS.contains(rhsCandidate)) {
						continue;
					}

					// if it is not a potential rhs
					if (DummyProperties.hasOptimization
							&& rhsCandidate.getData().getTotalSupportFrequency() < supportThreshold) {
						System.out.println();
						continue;
					}

					lattice.checkIfSubPatternStartTime = System.nanoTime();
					boolean isSubPattern = DualSimulationHandler.checkIfSubPattern(lattice,
							lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, rhsCandidate, fixedLHS);

					lattice.checkIfSubPatternDuration += (System.nanoTime() - lattice.checkIfSubPatternStartTime) / 1e6;

					if (isSubPattern) {
						flagParentsAsNonCandidate(shouldVisitRHS, rhsCandidate);
						continue;
					}

					// 19=>55
					// if (fixedLHS.getData().getPatternLatticeNodeIndex() == 19
					// && rhsCandidate.getData().getPatternLatticeNodeIndex() ==
					// 55) {
					// System.out.println();
					// }

					if (timebound > 0 && ((System.nanoTime() - startTime) / 1e6) > timebound) {
						timeout = true;
						return;
					}
					
					// not a subpattern:
					Rule currentRule = fixedLHS.getData().generateARule(fixedLHS, rhsCandidate,
							(System.nanoTime() - startTime));

					lattice.numberOfTotalRulesGenerated++;

					lattice.gtarVerificationStartTime = System.nanoTime();
					lattice.numberOfGtarVerification++;
					lattice.rhsTrials++;
					// verify it
					DualSimulationHandler.gtarVerification2(currentRule, minTimestamp, maxTimestamp, deltaT);
					lattice.gtarVerificationDuration += (System.nanoTime() - lattice.gtarVerificationStartTime) / 1e6;

					DualSimulationHandler.computeConfidence(currentRule);

					// if (currentRule.support > 0)
					// System.out.println("Valid Rule: " + currentRule);

					if (currentRule.support >= supportThreshold && currentRule.confidence >= confidenceThreshold) {
						if (DummyProperties.debugMode)
							System.out.println(currentRule);
						flagParentsAsNonCandidate(shouldVisitRHS, rhsCandidate);
						continue;
					} else {
						fixedLHS.getData().removeTheRule(currentRule);
					}
				}
			}
			// checkTimeAndSaveQualityIfNeeded(lattice);
		}
		
		

		
		HashSet<Rule> rules = new HashSet<>();
		for (int index : lattice.getLatticeNodeIndex().keySet()) {
			if (lattice.getLatticeNodeIndex().get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : lattice.getLatticeNodeIndex().get(index).getData().getRulesOfThis()) {
					rules.add(rule);
				}
			}
		}

		int cnt = 0;
		for (int index1 : lattice.latticeNodeIndex.keySet()) {
			if (lattice.latticeNodeIndex.get(index1).getData().getRulesOfThis().size() > 0) {

				HashSet<Rule> rules1 = new HashSet<>();
				rules1.addAll(lattice.latticeNodeIndex.get(index1).getData().getRulesOfThis());
				Iterator<Rule> ruleItr1 = rules1.iterator();

				while (ruleItr1.hasNext()) {

					Rule rule1 = ruleItr1.next();
					// for (Rule rule1 :
					// lattice.latticeNodeIndex.get(index1).getData().getRulesOfThis())
					// {
					for (int index2 : lattice.latticeNodeIndex.keySet()) {
						if (lattice.latticeNodeIndex.get(index2).getData().getRulesOfThis().size() > 0) {
							HashSet<Rule> rules2 = lattice.latticeNodeIndex.get(index2).getData().getRulesOfThis();
							Iterator<Rule> ruleItr2 = rules2.iterator();
							while (ruleItr2.hasNext()) {
								Rule rule2 = ruleItr2.next();
								if (rule1 == rule2)
									continue;

								if (DualSimulationHandler.checkIfSubPattern(lattice,
										lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, rule2.lhs, rule1.lhs)
										&& DualSimulationHandler.checkIfSubPattern(lattice,
												lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, rule2.rhs,
												rule1.rhs)) {
									ruleItr2.remove();
									cnt++;
								}
							}
						}
					}
				}
			}
		}

		if (DummyProperties.debugMode)
			System.out.println("# of pruned non-maximal rules: " + cnt);

	}

	// private void checkTimeAndSaveQualityIfNeeded(LatticeReducedTwoStepsOpt
	// lattice) {
	// // check the current duration, then based on the interval if we are in
	// // the next interval we should save the quality value in somewhere
	// double checkingTimeStart = System.nanoTime();
	// int d = (int) (((System.nanoTime() - startTime) / 1e6) /
	// DummyProperties.qualitySaveIntervalInMilliSeconds);
	// if (d > lastQualitySavedInterval) {
	// qualityOfTime.put(d, getTotalQualityOfMaximalRules(lattice));
	// lastQualitySavedInterval = d;
	// }
	//
	// ruleGeneratorStartTime += (System.nanoTime() - checkingTimeStart);
	//
	// }
	//
	// private CumulativeRulesInfo
	// getTotalQualityOfMaximalRules(LatticeReducedTwoStepsOpt lattice) {
	//
	// double support = 0d;
	// double confidence = 0d;
	// int rulesNum = 0;
	//
	// for (int index : lattice.latticeNodeIndex.keySet()) {
	// if (lattice.latticeNodeIndex.get(index).getData().getRulesOfThis().size()
	// > 0) {
	// for (Rule rule :
	// lattice.latticeNodeIndex.get(index).getData().getRulesOfThis()) {
	// support += rule.support;
	// confidence += rule.confidence;
	// rulesNum++;
	// }
	// }
	// }
	//
	// return new CumulativeRulesInfo(support, confidence, rulesNum);
	// }

	private void flagParentsAsNonCandidate(HashSet<LatticeNode<ILatticeNodeData>> shouldVisitRHS,
			LatticeNode<ILatticeNodeData> rhsCandidate) {

//		if (!DummyProperties.hasOptimization) {
//			return;
//		}

		Queue<LatticeNode<ILatticeNodeData>> parents = new LinkedList<>();

		parents.add(rhsCandidate);

		while (!parents.isEmpty()) {
			LatticeNode<ILatticeNodeData> node = parents.poll();
			shouldVisitRHS.remove(node);
			if (node.getParent() != null) {
				if (shouldVisitRHS.contains(node.getParent())) {
					parents.add(node.getParent());
					shouldVisitRHS.remove(node.getParent());
				}
			}

			if (node.getSuperNodeLinks() != null) {
				for (LatticeNode<ILatticeNodeData> superNode : node.getSuperNodeLinks()) {
					if (shouldVisitRHS.contains(superNode)) {
						parents.add(superNode);
						shouldVisitRHS.remove(superNode);
					}
				}
			}
		}

	}

}
