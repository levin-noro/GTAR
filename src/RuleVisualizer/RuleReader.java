package RuleVisualizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;

import utilities.DefaultLabeledEdge;

public class RuleReader {

	// focus was Officer|countries:Sweden, h:1 , e:4, s:0.8, c:0.8, deltaT:5
	// ([(Officer*_1210315984) , (Address_47491804) ],
	// [REGISTERED_ADDRESS=((Officer*_1210315984) ,(Address_47491804)
	// )])=>([(Officer*_1210315984) , (Address_47491804) ,
	// (Country_1814723699)], [REGISTERED_ADDRESS=((Officer*_1210315984)
	// ,(Address_47491804) ),
	// NOTYPE=((Officer*_1210315984) ,(Country_1814723699)
	// )])1.0;1.0;0.9999999999997952;0.9999999999997952;11964;2;3;1;11
	// ([(Officer*_1210315984) , (Country_1296074566) ],
	// [NOTYPE=((Officer*_1210315984) ,(Country_1296074566)
	// )])=>([(Officer*_1210315984) , (Address_47491804) , (Country_1814723699)
	// ], [REGISTERED_ADDRESS=((Officer*_1210315984) ,(Address_47491804) ),
	// NOTYPE=((Officer*_1210315984) ,(Country_1814723699)
	// )])1.0;1.0;0.9999999999997952;0.9999999999997952;11964;2;3;2;11

	public CustomizedRules getNextRules(BufferedReader br, int bound) throws Exception {

		List<CustomizedRule> ruleGraphs = new ArrayList<CustomizedRule>();
		String infoLine = null;
		String line = null;
		line = br.readLine();
		if (line == null) {
			return null;
		}
		line = line.trim();

		while (line != null && line.trim().equals("")) {
			line = br.readLine();
		}


		if (line != null && line.startsWith("focus was")) {
			// latticeAsAGraph = new ListenableDirectedGraph<Integer,
			// DefaultEdge>(DefaultEdge.class);

			int i = 0;
			infoLine = line;
			while ((line = br.readLine()) != null) {
				System.out.println("line" + i +": " + line);
				i++;
				if (line.equals("")) {
					break;
				}
				String[] mainComponents = line.split(Pattern.quote(")]);"));
				String[] ruleSides = mainComponents[0].split("=>");
				ListenableDirectedGraph<String, DefaultLabeledEdge> lhsGraph = getGraphFromString(ruleSides[0]);
				// mainComponents[1] =
				// mainComponents[1].replaceAll(Pattern.quote("=>"), "");
				ListenableDirectedGraph<String, DefaultLabeledEdge> rhsGraph = getGraphFromString(ruleSides[1]);

				String[] ruleInfos = mainComponents[1].split(";");
				String ruleInfo = "LHS Support: " + ruleInfos[0] + "\n";
				ruleInfo += "RHS Support: " + ruleInfos[1] + "\n";
				ruleInfo += "Rule Support: " + ruleInfos[2] + "\n";
				ruleInfo += "Rule Confidence: " + ruleInfos[3] + "\n";
				ruleInfo += "Min Occurrences: " + ruleInfos[4] + "\n";
				ruleInfo += "LHS Level: " + ruleInfos[5] + "\n";
				ruleInfo += "RHS Level: " + ruleInfos[6] + "\n";
				ruleInfo += "LHS discovered index: " + ruleInfos[7] + "\n";
				ruleInfo += "RHS discovered index: " + ruleInfos[8] + "\n";

				ruleGraphs.add(new CustomizedRule(lhsGraph, rhsGraph, ruleInfo));

			}
		}

		if (ruleGraphs.size() > bound) {
			return new CustomizedRules(ruleGraphs.subList(0, bound), infoLine);
		}
		return new CustomizedRules(ruleGraphs, infoLine);
	}

	private static ListenableDirectedGraph<String, DefaultLabeledEdge> getGraphFromString(String graphString) {

		// ([(Officer*_1210315984) , (Address_47491804) ],
		// [REGISTERED_ADDRESS=((Officer*_1210315984) ,(Address_47491804) )])
		// nodes], [edges

		ListenableDirectedGraph<String, DefaultLabeledEdge> graph = new ListenableDirectedGraph<String, DefaultLabeledEdge>(
				DefaultLabeledEdge.class);

		String[] nodeAndEdges = graphString.split("\\], \\[");
		nodeAndEdges[0] = nodeAndEdges[0].replace("[", "");
		String[] nodes = nodeAndEdges[0].split(",");
		for (String node : nodes) {
			String cleanNode = node.replaceAll(Pattern.quote("("), "").replaceAll(Pattern.quote(")"), "").trim();
			graph.addVertex(cleanNode);
		}

		nodeAndEdges[1] = nodeAndEdges[1].replace("]", "");
		String[] edges = nodeAndEdges[1].split(Pattern.quote("),"));
		for (String edge : edges) {
			String[] edgeTypeAndSrcDest = edge.split("=");
			String edgeType = edgeTypeAndSrcDest[0].trim().replaceAll(" ", "");
			edgeTypeAndSrcDest[1] = edgeTypeAndSrcDest[1].replaceAll(Pattern.quote("("), "")
					.replaceAll(Pattern.quote(")"), "");
			String[] endPoints = edgeTypeAndSrcDest[1].split(",");

			graph.addEdge(endPoints[0].trim(), endPoints[1].trim(), new DefaultLabeledEdge(edgeType));

		}

		return graph;
	}
}
