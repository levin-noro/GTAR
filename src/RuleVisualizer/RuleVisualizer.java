package RuleVisualizer;

import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.jgrapht.ext.JGraphXAdapter;

import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;

import utilities.DefaultLabeledEdge;

public class RuleVisualizer {
	static String rulesFilePath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/may8/citation/2/twoStepsRules.txt";

	public static void main(String[] args) throws Exception {
		FileInputStream fis = new FileInputStream(rulesFilePath);

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		RuleReader rr = new RuleReader();
		CustomizedRules customizedRules = null;
		while ((customizedRules = rr.getNextRules(br, 1000)) != null) {
			visualizeRules(customizedRules);
		}

		br.close();
	}

	public static void visualizeRules(CustomizedRules customizedRules) {
		JFrame frame = new JFrame("Rules: " + customizedRules.customizedRules.size());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		// latticeNode.getData().patternLatticeNodeIndex.toString()
		for (CustomizedRule rule : customizedRules.customizedRules) {

			JGraphXAdapter<String, DefaultLabeledEdge> graphAdapter1 = new JGraphXAdapter<String, DefaultLabeledEdge>(
					rule.lhsGraph);
			JGraphXAdapter<String, DefaultLabeledEdge> graphAdapter2 = new JGraphXAdapter<String, DefaultLabeledEdge>(
					rule.rhsGraph);

			mxIGraphLayout layout1 = new mxHierarchicalLayout(graphAdapter1);
			layout1.execute(graphAdapter1.getDefaultParent());

			mxIGraphLayout layout2 = new mxHierarchicalLayout(graphAdapter2);
			layout2.execute(graphAdapter2.getDefaultParent());

			JPanel prefixNodePanel = new JPanel();
			prefixNodePanel.setLayout(new BoxLayout(prefixNodePanel, BoxLayout.Y_AXIS));

			JPanel titlePanel = new JPanel();
			JLabel titleLabel = new JLabel(customizedRules.settingInfo);
			titlePanel.add(titleLabel);

			JPanel gPanel = new JPanel();
			gPanel.add(new mxGraphComponent(graphAdapter1));
			gPanel.add(new mxGraphComponent(graphAdapter2));

			gPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			// gPanel.setPreferredSize(new Dimension(gPanel.getSize().width +
			// 20, gPanel.getSize().height + 20));

			JTextArea textArea = new JTextArea(10, 20);
			JScrollPane scrollInfoPane = new JScrollPane(textArea);
			textArea.setEditable(false);
			String text = rule.ruleInfo;
			//
			// String text = rule.lhs.getData().getPatternLatticeNodeIndex() +
			// "=>"
			// + rule.rhs.getData().getPatternLatticeNodeIndex() + "\n";
			//
			// text += "support: " + rule.support + "\n";
			// text += "confidence: " + rule.confidence + "\n";
			// text += "min occ: " +
			// Arrays.toString(rule.minOccurrences.toArray()) + "\n";
			// text += "min occ size: " + rule.minOccurrences.size() + "\n";
			//
			// text += "LHS:\n";
			//
			// text += getLatticeNodeInfo(rule.lhs);
			//
			// text += "RHS:\n";
			// text += getLatticeNodeInfo(rule.rhs);
			//
			 textArea.setText(text);

			prefixNodePanel.add(titlePanel);
			prefixNodePanel.add(gPanel);
			prefixNodePanel.add(scrollInfoPane);
			//
			prefixNodePanel.setSize(prefixNodePanel.getSize().width + 20, prefixNodePanel.getSize().height + 20);

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
}
