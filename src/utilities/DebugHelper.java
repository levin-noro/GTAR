package utilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;

import com.google.common.collect.MinMaxPriorityQueue;

import base.ILattice;
import base.ILatticeNodeData;
import dualsim.BatDualSimulation;

public class DebugHelper {
	public static void printParentChildRelationship(ILattice lattice) {
		for (LatticeNode<ILatticeNodeData> parentLatticeNode : lattice.getLabelAdjacencyIndexer().parentChildDifference
				.keySet()) {
			System.out.print(parentLatticeNode.getData().getPatternLatticeNodeIndex() + "=> [");
			for (LatticeNode<ILatticeNodeData> childLatticeNode : lattice
					.getLabelAdjacencyIndexer().parentChildDifference.get(parentLatticeNode).keySet()) {
				System.out.print(childLatticeNode.getData().getPatternLatticeNodeIndex() + ", ");
			}

			System.out.println("]");
		}
	}

	public static void printIsomorphicPatterns(ILattice lattice) {
		System.out.println("isomorphic patterns in tree if any?");

		for (Integer latticeIndex1 : lattice.getLatticeNodeIndex().keySet()) {
			for (Integer latticeIndex2 : lattice.getLatticeNodeIndex().keySet()) {

				if (latticeIndex1 < latticeIndex2) {
					// if (latticeIndex1 == 11 && latticeIndex2 == 16) {
					// System.out.println(latticeIndex1 + " iso " +
					// latticeIndex2);
					// }
					boolean isPreIsoChecking = lattice.preIsoChecking(
							lattice.getLatticeNodeIndex().get(latticeIndex1).getData().getPatternGraph(),
							lattice.getLatticeNodeIndex().get(latticeIndex2).getData().getPatternGraph());

					VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = lattice.getIsomorphism(
							lattice.getLatticeNodeIndex().get(latticeIndex1).getData().getPatternGraph(),
							lattice.getLatticeNodeIndex().get(latticeIndex2).getData().getPatternGraph());

					if (iso != null && iso.isomorphismExists()) {
						System.out.println("g1:" + latticeIndex1 + ", g2:" + latticeIndex2 + " isPreIsoChecking:"
								+ isPreIsoChecking + " graph:"
								+ lattice.getLatticeNodeIndex().get(latticeIndex1).getData());
					}
				}
			}
		}

		System.out.println("end");
	}

	private static boolean isSubgraphIsomorphic(LatticeNode<ILatticeNodeData> bigGraphPTN,
			LatticeNode<ILatticeNodeData> subgraphGraphPTN) {
		VF2SubgraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> iso = new VF2SubgraphIsomorphismInspector<PatternNode, DefaultLabeledEdge>(
				bigGraphPTN.getData().getPatternGraph(), subgraphGraphPTN.getData().getPatternGraph(),
				new Comparator<PatternNode>() {

					@Override
					public int compare(PatternNode v1, PatternNode v2) {
						if (v1.getType().equals(v2.getType()))
							return 0;

						return 1;
					}

				}, new Comparator<DefaultLabeledEdge>() {

					@Override
					public int compare(DefaultLabeledEdge e1, DefaultLabeledEdge e2) {
						if (e1.getType().equals(e2.getType()))
							return 0;

						return 1;
					}
				});

		return iso.isomorphismExists();
	}

	public static void printSubGraphIsomorphicTopkPatterns(
			MinMaxPriorityQueue<LatticeNode<ILatticeNodeData>> topkFrequentPatterns) {

		System.out.println("subgraph iso finder in topk if any?");

		ArrayList<LatticeNode<ILatticeNodeData>> newTopkArr = new ArrayList<LatticeNode<ILatticeNodeData>>(
				topkFrequentPatterns.size());

		newTopkArr.addAll(topkFrequentPatterns);

		for (LatticeNode<ILatticeNodeData> latticeNode1 : newTopkArr) {
			for (LatticeNode<ILatticeNodeData> latticeNode2 : newTopkArr) {
				if (latticeNode1 != latticeNode2) {

					if (isSubgraphIsomorphic(latticeNode1, latticeNode2)) {
						System.err.println("g1:" + latticeNode1.getData().getPatternLatticeNodeIndex() + ", g2:"
								+ latticeNode2.getData().getPatternLatticeNodeIndex());
					}

				}
			}
		}
		System.out.println("end");
	}

	public static void printBiSimulatedPatterns(ILattice lattice) {
		System.out.println("");
		System.out.println("BiSimulated Patterns in tree if any?");
		int cnt = 0;

		for (Integer biggerIndex : lattice.getLatticeNodeIndex().keySet()) {
			for (Integer smallerIndex : lattice.getLatticeNodeIndex().keySet()) {

				if (smallerIndex < biggerIndex
						&& lattice.getLatticeNodeIndex().get(biggerIndex).getData().getPatternGraph().edgeSet()
								.size() >= 1
						&& lattice.getLatticeNodeIndex().get(smallerIndex).getData().getPatternGraph().edgeSet()
								.size() >= 1) {

					if (DualSimulationHandler.isBiDualSimulated(
							lattice.getLatticeNodeIndex().get(biggerIndex).getData().getPatternGraph(),
							lattice.getLatticeNodeIndex().get(smallerIndex), lattice)) {
						cnt++;
						System.out.println("Bi-SiMulaTED");
						System.out.println("bigger: " + lattice.getLatticeNodeIndex().get(biggerIndex).getData());
						System.out.println("smaller: " + lattice.getLatticeNodeIndex().get(smallerIndex).getData());
						System.out.println();
					}

				}
			}
		}
		System.out.println();
		System.out.println("bi simul cnt: " + cnt);
	}

	private static boolean isDualSim(Integer index1, Integer index2, ILattice lattice) {
		HashMap<PatternNode, HashSet<PatternNode>> dsim = new HashMap<PatternNode, HashSet<PatternNode>>();
		for (PatternNode patternNode1 : lattice.getLatticeNodeIndex().get(index1).getData().getPatternGraph()
				.vertexSet()) {
			for (PatternNode patternNode2 : lattice.getLatticeNodeIndex().get(index2).getData().getPatternGraph()
					.vertexSet()) {
				if (patternNode1.equals(patternNode2)) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				} else if (patternNode1.getType().equals(patternNode2.getType())
						// &&
						// lattice.getLatticeNodeIndex().get(biggerIndex).getData()
						// .getIncomingRelTypesOfPatternNodes().get(patternNode1)
						// .equals(lattice.getLatticeNodeIndex().get(latticeIndex2).getData()
						// .getIncomingRelTypesOfPatternNodes().get(patternNode2))
						&& lattice.getLatticeNodeIndex().get(index1).getData().getStepsFromRootOfPatternNodes()
								.get(patternNode1).equals(lattice.getLatticeNodeIndex().get(index2).getData()
										.getStepsFromRootOfPatternNodes().get(patternNode2))) {
					dsim.putIfAbsent(patternNode1, new HashSet<PatternNode>());
					dsim.get(patternNode1).add(patternNode2);
				}
			}
		}

		if (dsim.size() == lattice.getLatticeNodeIndex().get(index1).getData().getPatternGraph().vertexSet().size()) {
			HashMap<PatternNode, HashSet<PatternNode>> result = BatDualSimulation.customizedMatchList(
					lattice.getLatticeNodeIndex().get(index1).getData().getPatternGraph(),
					lattice.getLatticeNodeIndex().get(index2).getData().getPatternGraph(), dsim, lattice);

			if (!result.isEmpty() && result.size() == lattice.getLatticeNodeIndex().get(index1).getData()
					.getPatternGraph().vertexSet().size()) {
				return true;
			} else {
				// System.out.println("NOT simulated");
				// System.out.println("index1: " +
				// lattice.getLatticeNodeIndex().get(index1).getData());
				// System.out.println("index2: " +
				// lattice.getLatticeNodeIndex().get(index2).getData());
				// System.out.println("res: " + dsim);
				// System.out.println();
			}
		}
		return false;
	}

	public static void printGlobalCandidateSet(ILattice lattice) {
		System.out.println();
		for (PatternNode patternNode : lattice.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.keySet()) {
			System.out.print(patternNode + ": ");
			for (Integer nodeId : lattice.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.get(patternNode)) {
				System.out.print(nodeId + ", ");
			}
			System.out.println();
		}
		System.out.println();

	}

	public static void printAllCandidates(ILattice lattice) {
		HashSet<Integer> allCandidatesSet = new HashSet<Integer>();
		System.out.println("allCandidateSet:");
		for (PatternNode patternNode : lattice.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.keySet()) {
			for (Integer nodeId : lattice.getLabelAdjacencyIndexer().candidateSetOfAPatternNode.get(patternNode)) {
				allCandidatesSet.add(nodeId);
			}

		}

		ArrayList<Integer> canIdsOrdered = new ArrayList<Integer>();
		canIdsOrdered.addAll(allCandidatesSet);
		Collections.sort(canIdsOrdered);

		System.out.print("[");
		for (Integer id : canIdsOrdered) {
			System.out.print(id + ", ");
		}
		System.out.print("]");

	}

	public static void printAffectedPatterns(ArrayList<LatticeNode<ILatticeNodeData>> insertAffectedPatternsArr) {
		System.out.print("affected patterns:");
		for (int p = 0; p < insertAffectedPatternsArr.size(); p++) {
			System.out.print(insertAffectedPatternsArr.get(p).getData().getPatternLatticeNodeIndex() + ", ");
		}
		System.out.println();

	}

	public static void logAllMatchesOfAPattern(LatticeNode<ILatticeNodeData> pattern) {
		String str = "";
		for (PatternNode patternNode : pattern.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().keySet()) {
			str += pattern.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode);
		}

		Logger.logAllMatchesOfAPattern(str);
	}
	// public static void testSubGraphIsomorphicParentChild(ILattice
	// lattice) {
	// for (LatticeNode<ILatticeNodeData> parentLatticeNode :
	// lattice
	// .getLabelAdjacencyIndexer().parentChildDifference.keySet()) {
	// for (LatticeNode<ILatticeNodeData> childLatticeNode : lattice
	// .getLabelAdjacencyIndexer().parentChildDifference.get(parentLatticeNode).keySet())
	// {
	//
	// if (parentLatticeNode.getData().getPatternLatticeNodeIndex() == 9
	// && childLatticeNode.getData().getPatternLatticeNodeIndex() == 11) {
	// System.out.println();
	// }
	// if (!isSubgraphIsomorphic(childLatticeNode, parentLatticeNode)) {
	// System.err.println("parent is not subgraph iso of its child");
	// System.err.println("parent: " +
	// parentLatticeNode.getData().getPatternLatticeNodeIndex() + " " +
	// parentLatticeNode.getData().getPatternGraph());
	// System.err.println("parent: " + parentLatticeNode.getData());
	// System.err.println("child: " +
	// childLatticeNode.getData().getPatternLatticeNodeIndex() + " " +
	// childLatticeNode.getData().getPatternGraph());
	// System.err.println("child: " + childLatticeNode.getData());
	// System.err.println();
	// }
	// }
	// }
	// }

	public static void getAllMatchesOrderedByNodeId(ILattice lattice) {

		HashSet<Integer> matchIds = new HashSet<Integer>();
		for (Integer patternId : lattice.getLatticeNodeIndex().keySet()) {
			for (PatternNode patternNode : lattice.getLatticeNodeIndex().get(patternId).getData().getMatchedNodes()
					.getDataGraphMatchNodeOfAbsPNode().keySet()) {
				matchIds.addAll(lattice.getLatticeNodeIndex().get(patternId).getData().getMatchedNodes()
						.getDataGraphMatchNodeOfAbsPNode().get(patternNode));
			}
		}

		ArrayList<Integer> matchIdsOrdered = new ArrayList<Integer>();
		matchIdsOrdered.addAll(matchIds);
		Collections.sort(matchIdsOrdered);

		System.out.print("[");
		for (Integer id : matchIdsOrdered) {
			System.out.print(id + ",");
		}
		System.out.println("]");

	}

	public static void printIfLevelIsNotConsistentWithNumberOfEdges(ILattice lattice) {
		System.out.println("not from same level if any?");
		for (Integer patternId : lattice.getLatticeNodeIndex().keySet()) {
			LatticeNode<ILatticeNodeData> latticeNode = lattice.getLatticeNodeIndex().get(patternId);

			if (latticeNode.getData().getPatternGraph().edgeSet().size() != (latticeNode.getLevel() - 1)) {
				System.out.println(latticeNode.getData() + "parent: " + latticeNode.getParent().getData());
			}

		}
		System.out.println("end");

	}

	public static void printPatternWithDuplicateMatches(ILattice lattice) {
		HashSet<LatticeNode<ILatticeNodeData>> patterns = new HashSet<LatticeNode<ILatticeNodeData>>();

		for (Integer patternId : lattice.getLatticeNodeIndex().keySet()) {
			LatticeNode<ILatticeNodeData> latticeNode = lattice.getLatticeNodeIndex().get(patternId);
			ArrayList<PatternNode> patternNodesArr = new ArrayList<PatternNode>();
			patternNodesArr.addAll(latticeNode.getData().getPatternGraph().vertexSet());
			for (int i = 0; i < patternNodesArr.size(); i++) {
				for (int j = 0; j < patternNodesArr.size(); j++) {
					if (i < j) {
						PatternNode patternNode1 = patternNodesArr.get(i);
						PatternNode patternNode2 = patternNodesArr.get(j);
						if (latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode1)
								.size() != latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
										.get(patternNode2).size())
							continue;

						boolean sameMatches = true;
						for (Integer nodeId1 : latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
								.get(patternNode1)) {
							boolean isFoundEqual = false;
							for (Integer nodeId2 : latticeNode.getData().getMatchedNodes()
									.getDataGraphMatchNodeOfAbsPNode().get(patternNode2)) {
								if (nodeId1 == nodeId2) {
									isFoundEqual = true;
									break;
								}
							}
							if (!isFoundEqual) {
								sameMatches = false;
								break;
							}
						}
						if (sameMatches) {
							patterns.add(latticeNode);
						}
					}
				}
			}
		}

		if (patterns.size() > 0) {
			System.out.println("duplicated matches?");
			for (LatticeNode<ILatticeNodeData> pattern : patterns) {
				System.out.println(pattern.getData());
			}
			System.out.println("end");
			System.out.println();
		}

	}

	public static void printOrderOfTheQueue(PriorityQueue<LatticeNode<ILatticeNodeData>> priorityQueue) {
		System.out.println(" Queue Priorities START");
		ArrayList<LatticeNode<ILatticeNodeData>> tt = new ArrayList<>();
		if (priorityQueue.size() > 1) {
			while (!priorityQueue.isEmpty()) {
				LatticeNode<ILatticeNodeData> tp = priorityQueue.poll();
				System.out.println(tp.getData());
				tt.add(tp);
			}
			System.out.println(" Queue Priorities END");
		}
		for (LatticeNode<ILatticeNodeData> tp : tt) {
			priorityQueue.add(tp);
		}
	}

	public static int printingRules(BufferedWriter bw, ILattice lattice, String settingStr) throws Exception {

		ArrayList<Rule> rules = new ArrayList<>();
		for (int index : lattice.getLatticeNodeIndex().keySet()) {
			if (lattice.getLatticeNodeIndex().get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : lattice.getLatticeNodeIndex().get(index).getData().getRulesOfThis()) {
					rules.add(rule);
				}
			}
		}

		if (rules.isEmpty()) {
			return 0;
		}

		Collections.sort(rules, new Comparator<Rule>() {
			@Override
			public int compare(Rule o1, Rule o2) {
				int compare = Double.compare(o2.confidence, o1.confidence);
				if (compare != 0) {
					return compare;
				}
				compare = Double.compare(o2.support, o1.support);
				if (compare != 0) {
					return compare;
				}
				return Double.compare(o2.minOccurrencesSize, o1.minOccurrencesSize);
			}
		});

		bw.write(settingStr);
		bw.newLine();

		int cnt = 0;
		for (Rule rule : rules) {
			cnt++;
			if (cnt > 300) {
				break;
			}
			bw.write(rule.lhs.getData().getPatternGraph() + "=>" + rule.rhs.getData().getPatternGraph() + ";");
			bw.write(rule.lhs.getData().getTotalSupportFrequency() + ";" + rule.rhs.getData().getTotalSupportFrequency()
					+ ";" + rule.support + ";" + rule.confidence + ";" + rule.minOccurrencesSize + ";"
					+ rule.lhs.getLevel() + ";" + rule.rhs.getLevel() + ";"
					+ rule.lhs.getData().getPatternLatticeNodeIndex() + ";"
					+ rule.rhs.getData().getPatternLatticeNodeIndex());
			bw.newLine();
		}

		bw.newLine();
		bw.newLine();
		bw.flush();

		return rules.size();
	}

	public static void checkIfThereAreNotMaximalRules(ILattice lattice) {
		// grab all the rules
		ArrayList<Rule> rules = new ArrayList<>();
		for (int index : lattice.getLatticeNodeIndex().keySet()) {
			if (lattice.getLatticeNodeIndex().get(index).getData().getRulesOfThis().size() > 0) {
				for (Rule rule : lattice.getLatticeNodeIndex().get(index).getData().getRulesOfThis()) {
					rules.add(rule);
				}
			}
		}

		Collections.sort(rules, new Comparator<Rule>() {
			@Override
			public int compare(Rule o1, Rule o2) {
				int compare = Double.compare(o1.lhs.getLevel() + o1.rhs.getLevel(),
						o2.lhs.getLevel() + o2.rhs.getLevel());
				if (compare != 0) {
					return compare;
				}
				compare = Double.compare(o2.confidence, o1.confidence);
				if (compare != 0) {
					return compare;
				}
				return Double.compare(o2.support, o1.support);
			}
		});

		int cnt = 0;
		for (Rule rule1 : rules) {
			for (Rule rule2 : rules) {
				if (rule1 == rule2)
					continue;
				if (DualSimulationHandler.checkIfSubPattern(lattice,
						lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, rule2.lhs, rule1.lhs)
						&& DualSimulationHandler.checkIfSubPattern(lattice,
								lattice.getLabelAdjacencyIndexer().subPatternsOfAPattern, rule2.rhs, rule1.rhs)) {

					System.out.println("Not maximal rule: " + cnt++);

					System.out.println("Super Rule:");
					System.out.println(rule1.lhs.getData().getPatternLatticeNodeIndex() + ":"
							+ rule1.lhs.getData().getPatternGraph() + "\n=>\n"
							+ rule1.rhs.getData().getPatternLatticeNodeIndex() + ":"
							+ rule1.rhs.getData().getPatternGraph() + ";");
					System.out.println("Sub Rule:");
					System.out.println(rule2.lhs.getData().getPatternLatticeNodeIndex() + ":"
							+ rule2.lhs.getData().getPatternGraph() + "\n=>\n"
							+ rule2.rhs.getData().getPatternLatticeNodeIndex() + ":"
							+ rule2.rhs.getData().getPatternGraph() + ";");

					System.out.println("Super Rule Info:");
					System.out.println("lhs supp:" + rule1.lhs.getData().getTotalSupportFrequency() + "; " + "rhs supp:"
							+ rule1.rhs.getData().getTotalSupportFrequency() + "; ruls supp:" + rule1.support + "; "
							+ "conf:" + rule1.confidence + ";" + " min occ:" + rule1.minOccurrencesSize + ";");

					System.out.println("Sub Rule Info:");
					System.out.println("lhs supp:" + rule2.lhs.getData().getTotalSupportFrequency() + "; " + "rhs supp:"
							+ rule2.rhs.getData().getTotalSupportFrequency() + "; ruls supp:" + rule2.support + "; "
							+ "conf:" + rule2.confidence + ";" + " min occ:" + rule2.minOccurrencesSize + ";");

					System.out.println();
				}
			}
		}
	}

}
