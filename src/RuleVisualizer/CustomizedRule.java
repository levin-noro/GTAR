package RuleVisualizer;

import org.jgrapht.graph.ListenableDirectedGraph;

import utilities.DefaultLabeledEdge;

public class CustomizedRule {

	ListenableDirectedGraph<String, DefaultLabeledEdge> lhsGraph;
	ListenableDirectedGraph<String, DefaultLabeledEdge> rhsGraph;
	String ruleInfo;

	public CustomizedRule(ListenableDirectedGraph<String, DefaultLabeledEdge> lhsGraph,
			ListenableDirectedGraph<String, DefaultLabeledEdge> rhsGraph, String ruleInfo) {
		this.lhsGraph = lhsGraph;
		this.rhsGraph = rhsGraph;
		this.ruleInfo = ruleInfo;
	}
}
