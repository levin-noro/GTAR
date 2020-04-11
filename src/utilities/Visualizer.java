package utilities;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;

import com.google.common.collect.MinMaxPriorityQueue;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;

import base.ILattice;
import base.ILatticeNodeData;
import utilities.Dummy.DummyProperties;
import utilities.Dummy.DummyProperties.RuleSide;

//import src.lattice.DefaultLabeledEdge;
//import src.lattice.PatternNode;
//import src.lattice.LatticeNode;
//import src.lattice.LatticeNodeData;

public class Visualizer {

	public static void visualizeFrequents(HashSet<Rule> frequentRules) {
		if (!Dummy.DummyProperties.visualize)
			return;

		PriorityQueue<Rule> newTopkQ = new PriorityQueue<Rule>(frequentRules.size(), new ConfidenceComparator());

		newTopkQ.addAll(frequentRules);

		createAndShowGui(newTopkQ);

	}

	public static void visualizeALevel(ILattice lattice, int level, int cntVisualization) {
		if (!Dummy.DummyProperties.visualize)
			return;

		System.out.println("level:" + level);
		ArrayList<LatticeNode<ILatticeNodeData>> patternsInALevel = new ArrayList<LatticeNode<ILatticeNodeData>>();
		for (Integer patternId : lattice.getLatticeNodeIndex().keySet()) {
			LatticeNode<ILatticeNodeData> latticeNode = lattice.getLatticeNodeIndex().get(patternId);

			if (latticeNode.getLevel() == level) {
				patternsInALevel.add(latticeNode);
			}

		}
		createAndShowGui2(patternsInALevel, level, cntVisualization);

	}

	public static void visualizeXnotInY(String title, ArrayList<LatticeNode<ILatticeNodeData>> patterns) {
		if (!Dummy.DummyProperties.visualize)
			return;

		HashSet<LatticeNode<ILatticeNodeData>> patterns2 = new HashSet<LatticeNode<ILatticeNodeData>>();
		if (patterns.size() > 0) {
			patterns2.addAll(patterns);
			createAndShowGui3(title, patterns2);
		}
	}

	public static void visualizePatternWithDuplicateMatches(ILattice lattice) {
		if (!Dummy.DummyProperties.visualize)
			return;

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
		createAndShowGui3("same matches (duplicated)", patterns);
	}

	private static void createAndShowGui3(String title, HashSet<LatticeNode<ILatticeNodeData>> patterns) {
		JFrame frame = new JFrame(title + " size:" + patterns.size());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// latticeNode.getData().patternLatticeNodeIndex.toString()
		Iterator<LatticeNode<ILatticeNodeData>> itr = patterns.iterator();

		while (itr.hasNext()) {
			LatticeNode<ILatticeNodeData> latticeNode = itr.next();

			JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
					latticeNode.getData().getPatternGraph());

			mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
			layout1.execute(graphAdapter1.getDefaultParent());

			JPanel prefixNodePanel = new JPanel();
			prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

			JPanel titlePanel = new JPanel();
			JLabel titleLabel = new JLabel(Integer.toString(latticeNode.getData().getPatternLatticeNodeIndex()));
			titlePanel.add(titleLabel);

			JPanel gPanel = new JPanel();
			gPanel.add(new mxGraphComponent(graphAdapter1));
			// gPanel.setLayout(new BorderLayout(10,10));

			gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width+20,
			// gPanel.getSize().height+20));

			JTextArea textArea = new JTextArea(10, 20);
			JScrollPane scrollInfoPane = new JScrollPane(textArea);
			textArea.setEditable(false);

			String text = latticeNode.getData().getPatternLatticeNodeIndex() + "\n";

			for (PatternNode patternNode : latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					.keySet()) {
				text += patternNode + ": "
						+ latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						+ "\n";
			}

			text += "TotalSupp: " + latticeNode.getData().getTotalSupportFrequency() + "\n";
			text += "\n dangling:" + latticeNode.getData().isDanglingPattern() + " direction:"
					+ latticeNode.getData().getGrowthDirection();
			text += "\n ancestors:";

			text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
					+ latticeNode.getData().getSourcePatternNode() + " -> "
					+ latticeNode.getData().getTargetPatternNode() + " dangling:"
					+ latticeNode.getData().isDanglingPattern() + " direction:"
					+ latticeNode.getData().getGrowthDirection();
			while (latticeNode.getParent() != null) {
				latticeNode = latticeNode.getParent();
				text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
						+ latticeNode.getData().getSourcePatternNode() + " -> "
						+ latticeNode.getData().getTargetPatternNode() + " dangling:"
						+ latticeNode.getData().isDanglingPattern() + " direction:"
						+ latticeNode.getData().getGrowthDirection();

			}

			textArea.setText(text);

			prefixNodePanel.add(titlePanel);
			prefixNodePanel.add(gPanel);
			prefixNodePanel.add(scrollInfoPane);

			// prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20,
			// prefixNodePanel.getSize().height + 20);

			mainPanel.add(prefixNodePanel);

		}

		// frame.add(new mxGraphComponent(graphAdapter1));

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 500);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);

	}

	private static void createAndShowGui2(ArrayList<LatticeNode<ILatticeNodeData>> patternsInALevel, int level,
			int cntVisualization) {
		JFrame frame = new JFrame("Level: " + level + " index:" + cntVisualization + " #: " + patternsInALevel.size());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// latticeNode.getData().patternLatticeNodeIndex.toString()
		Collections.sort(patternsInALevel, new Comparator<LatticeNode<ILatticeNodeData>>() {
			@Override
			public int compare(LatticeNode<ILatticeNodeData> o1, LatticeNode<ILatticeNodeData> o2) {
				int compare = Double.compare(o2.getData().getTotalSupportFrequency(),
						o1.getData().getTotalSupportFrequency());
				if (compare != 0) {
					return compare;
				}
				return Integer.compare(o2.getData().getPatternGraph().vertexSet().size(),
						o1.getData().getPatternGraph().vertexSet().size());
				// return
				// Integer.compare(o1.getData().getPatternLatticeNodeIndex(),
				// o2.getData().getPatternLatticeNodeIndex());
			}
		});
		while (!patternsInALevel.isEmpty()) {
			LatticeNode<ILatticeNodeData> latticeNode = patternsInALevel.remove(0);

			JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
					latticeNode.getData().getPatternGraph());

			mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
			layout1.execute(graphAdapter1.getDefaultParent());

			JPanel prefixNodePanel = new JPanel();
			prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

			JPanel titlePanel = new JPanel();
			JLabel titleLabel = new JLabel(Integer.toString(latticeNode.getData().getPatternLatticeNodeIndex()));
			titlePanel.add(titleLabel);

			JPanel gPanel = new JPanel();
			gPanel.add(new mxGraphComponent(graphAdapter1));
			// gPanel.setLayout(new BorderLayout(10,10));

			gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width+20,
			// gPanel.getSize().height+20));

			JTextArea textArea = new JTextArea(10, 20);
			JScrollPane scrollInfoPane = new JScrollPane(textArea);
			textArea.setEditable(false);

			String text = latticeNode.getData().getPatternLatticeNodeIndex() + "\n";

			for (PatternNode patternNode : latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
					.keySet()) {
				text += patternNode + ": "
						+ latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode)
						+ "\n";
			}

			text += "TotalSupp: " + latticeNode.getData().getTotalSupportFrequency() + "\n";

			// if (latticeNode.getData().getMatchedNodes() != null) {
			// text += "\n";
			// text +=
			// latticeNode.getData().getMatchedNodes().getTimePointsOfAMatchNodeMap();
			// text += "\n";
			// }

			text += "\n dangling:" + latticeNode.getData().isDanglingPattern() + " direction:"
					+ latticeNode.getData().getGrowthDirection();
			text += "\n ancestors:";

			text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
					+ latticeNode.getData().getSourcePatternNode() + " -> "
					+ latticeNode.getData().getTargetPatternNode() + " dangling:"
					+ latticeNode.getData().isDanglingPattern() + " direction:"
					+ latticeNode.getData().getGrowthDirection();
			while (latticeNode.getParent() != null) {
				latticeNode = latticeNode.getParent();
				text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
						+ latticeNode.getData().getSourcePatternNode() + " -> "
						+ latticeNode.getData().getTargetPatternNode() + " dangling:"
						+ latticeNode.getData().isDanglingPattern() + " direction:"
						+ latticeNode.getData().getGrowthDirection();

			}

			textArea.setText(text);

			prefixNodePanel.add(titlePanel);
			prefixNodePanel.add(gPanel);
			prefixNodePanel.add(scrollInfoPane);

			// prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20,
			// prefixNodePanel.getSize().height + 20);

			mainPanel.add(prefixNodePanel);

		}

		// frame.add(new mxGraphComponent(graphAdapter1));

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 500);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);

	}

	private static void createAndShowGui(PriorityQueue<Rule> frequentRules) {

		JFrame frame = new JFrame("TOP-K FINAL RESULTS #:" + frequentRules.size());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// latticeNode.getData().patternLatticeNodeIndex.toString()
		// while (!topKFrequentPatterns.isEmpty()) {
		// LatticeNode<ILatticeNodeData> latticeNode =
		// topKFrequentPatterns.poll();
		for (Rule rule : frequentRules) {

			// lhs print:
			show(mainPanel, rule.lhs, DummyProperties.RuleSide.LEFT);
			// rhs print:
			show(mainPanel, rule.rhs, DummyProperties.RuleSide.RIGHT);

		}

		// frame.add(new mxGraphComponent(graphAdapter1));

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 500);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

	private static void show(JPanel mainPanel, LatticeNode<ILatticeNodeData> latticeNode, RuleSide ruleSide) {
		JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
				latticeNode.getData().getPatternGraph());

		mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
		layout1.execute(graphAdapter1.getDefaultParent());

		JPanel prefixNodePanel = new JPanel();
		prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

		JPanel titlePanel = new JPanel();
		JLabel titleLabel = new JLabel(Integer.toString(latticeNode.getData().getPatternLatticeNodeIndex()));
		titlePanel.add(titleLabel);

		JPanel gPanel = new JPanel();
		gPanel.add(new mxGraphComponent(graphAdapter1));
		// gPanel.setLayout(new BorderLayout(10,10));

		gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width+20,
		// gPanel.getSize().height+20));

		JTextArea textArea = new JTextArea(10, 20);
		JScrollPane scrollInfoPane = new JScrollPane(textArea);
		textArea.setEditable(false);

		String text = latticeNode.getData().getPatternLatticeNodeIndex() + "\n";

		text += "RuleSide: " + ruleSide + "\n";

		for (PatternNode patternNode : latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			text += patternNode + ": "
					+ latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode) + "\n";
		}

		text += "TotalSupp: " + latticeNode.getData().getTotalSupportFrequency() + "\n";
		text += "\n dangling:" + latticeNode.getData().isDanglingPattern();
		text += "\n ancestors:";

		text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
				+ latticeNode.getData().getSourcePatternNode() + " -> " + latticeNode.getData().getTargetPatternNode()
				+ " dangling:" + latticeNode.getData().isDanglingPattern() + " direction:"
				+ latticeNode.getData().getGrowthDirection();
		while (latticeNode.getParent() != null) {
			latticeNode = latticeNode.getParent();
			text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
					+ latticeNode.getData().getSourcePatternNode() + " -> "
					+ latticeNode.getData().getTargetPatternNode() + " dangling:"
					+ latticeNode.getData().isDanglingPattern() + " direction:"
					+ latticeNode.getData().getGrowthDirection();

		}

		textArea.setText(text);

		prefixNodePanel.add(titlePanel);
		prefixNodePanel.add(gPanel);
		prefixNodePanel.add(scrollInfoPane);

		// prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20,
		// prefixNodePanel.getSize().height + 20);

		mainPanel.add(prefixNodePanel);

	}

	public static void visualizeRules(ArrayList<Rule> rules) {
		JFrame frame = new JFrame("Rules: " + rules.size());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// latticeNode.getData().patternLatticeNodeIndex.toString()
		for (Rule rule : rules) {

			JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
					rule.lhs.getData().getPatternGraph());
			JGraphXAdapter<PatternNode, DefaultLabeledEdge> graphAdapter2 = new JGraphXAdapter<PatternNode, DefaultLabeledEdge>(
					rule.rhs.getData().getPatternGraph());

			mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
			layout1.execute(graphAdapter1.getDefaultParent());

			mxIGraphLayout layout2 = new mxHierarchicalLayout(graphAdapter2);
			layout2.execute(graphAdapter2.getDefaultParent());

			JPanel prefixNodePanel = new JPanel();
			prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

			JPanel titlePanel = new JPanel();
			JLabel titleLabel = new JLabel(rule.lhs.getData().getPatternLatticeNodeIndex() + "=>"
					+ rule.rhs.getData().getPatternLatticeNodeIndex());
			titlePanel.add(titleLabel);

			JPanel gPanel = new JPanel();
			gPanel.add(new mxGraphComponent(graphAdapter1));
			gPanel.add(new mxGraphComponent(graphAdapter2));

			gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width+20,
			// gPanel.getSize().height+20));

			JTextArea textArea = new JTextArea(10, 20);
			JScrollPane scrollInfoPane = new JScrollPane(textArea);
			textArea.setEditable(false);

			String text = rule.lhs.getData().getPatternLatticeNodeIndex() + "=>"
					+ rule.rhs.getData().getPatternLatticeNodeIndex() + "\n";

			text += "support: " + rule.support + "\n";
			text += "confidence: " + rule.confidence + "\n";
			// text += "min occ: " +
			// Arrays.toString(rule.minOccurrences.toArray()) + "\n";
			// text += "min occ size: " + rule.minOccurrences.size() + "\n";

			text += "LHS:\n";

			text += getLatticeNodeInfo(rule.lhs);

			text += "RHS:\n";
			text += getLatticeNodeInfo(rule.rhs);

			textArea.setText(text);

			prefixNodePanel.add(titlePanel);
			prefixNodePanel.add(gPanel);
			prefixNodePanel.add(scrollInfoPane);

			// prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20,
			// prefixNodePanel.getSize().height + 20);

			mainPanel.add(prefixNodePanel);

		}

		// frame.add(new mxGraphComponent(graphAdapter1));

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 500);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);

	}

	private static String getLatticeNodeInfo(LatticeNode<ILatticeNodeData> latticeNode) {
		String text = "";
		for (PatternNode patternNode : latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode()
				.keySet()) {
			text += patternNode + ": "
					+ latticeNode.getData().getMatchedNodes().getDataGraphMatchNodeOfAbsPNode().get(patternNode) + "\n";
		}

		text += "TotalSupp: " + latticeNode.getData().getTotalSupportFrequency() + "\n";

		if (latticeNode.getData().getMatchedNodes() != null) {
			text += "\n";
			text += latticeNode.getData().getMatchedNodes().getTimePointsOfAMatchNodeMap();
			text += "\n";
		}

		text += "\n dangling:" + latticeNode.getData().isDanglingPattern() + " direction:"
				+ latticeNode.getData().getGrowthDirection();
		text += "\n ancestors:";

		text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
				+ latticeNode.getData().getSourcePatternNode() + " -> " + latticeNode.getData().getTargetPatternNode()
				+ " dangling:" + latticeNode.getData().isDanglingPattern() + " direction:"
				+ latticeNode.getData().getGrowthDirection();
		while (latticeNode.getParent() != null) {
			latticeNode = latticeNode.getParent();
			text += "\n " + latticeNode.getLevel() + ":" + latticeNode.getData().getPatternLatticeNodeIndex()
					+ latticeNode.getData().getSourcePatternNode() + " -> "
					+ latticeNode.getData().getTargetPatternNode() + " dangling:"
					+ latticeNode.getData().isDanglingPattern() + " direction:"
					+ latticeNode.getData().getGrowthDirection();

		}
		return text;
	}

	public static void visualizeTheLattice(ILattice lattice) {

		ListenableDirectedGraph<Integer, DefaultEdge> latticeAsAGraph = new ListenableDirectedGraph<Integer, DefaultEdge>(
				DefaultEdge.class);

		for (Integer index : lattice.getLatticeNodeIndex().keySet()) {
			latticeAsAGraph.addVertex(index);
		}

		for (Integer index : lattice.getLatticeNodeIndex().keySet()) {
			for (LatticeNode<ILatticeNodeData> childLatticeNode : lattice.getLatticeNodeIndex().get(index)
					.getChildrenLinksSet()) {
				latticeAsAGraph.addEdge(index, childLatticeNode.getData().getPatternLatticeNodeIndex());
			}
		}

		JFrame frame = new JFrame("The lattice");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// latticeNode.getData().patternLatticeNodeIndex.toString()

		JGraphXAdapter<Integer, DefaultEdge> graphAdapter1 = new JGraphXAdapter<Integer, DefaultEdge>(latticeAsAGraph);

		mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
		layout1.execute(graphAdapter1.getDefaultParent());

		JPanel prefixNodePanel = new JPanel();
		prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

		JPanel titlePanel = new JPanel();

		JPanel gPanel = new JPanel();
		gPanel.add(new mxGraphComponent(graphAdapter1));

		gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		prefixNodePanel.add(titlePanel);
		prefixNodePanel.add(gPanel);

		mainPanel.add(prefixNodePanel);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.add(mainPanel);
		scrollPane.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 800);

		frame.getContentPane().add(scrollPane);

		frame.pack();

		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

}
