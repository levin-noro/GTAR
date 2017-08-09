package base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.neo4j.graphdb.GraphDatabaseService;

import com.google.common.collect.MinMaxPriorityQueue;

import utilities.DefaultLabeledEdge;
import utilities.Indexer;
import utilities.LatticeNode;
import utilities.PatternNode;

public interface ILattice {

	HashMap<Integer, LatticeNode<ILatticeNodeData>> getLatticeNodeIndex();

	Indexer getLabelAdjacencyIndexer();

	GraphDatabaseService getDataGraph();

	boolean preIsoChecking(ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern);

	VF2GraphIsomorphismInspector<PatternNode, DefaultLabeledEdge> getIsomorphism(
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> abstractPatternGraph,
			ListenableDirectedGraph<PatternNode, DefaultLabeledEdge> newAbsPattern);

	public double getDurationOfIsoChecking();

	public double getDurationOfBiSimChecking();

	public double getDurationOfNewLatticeGeneration();

	public long getNumberOfIsoCheckingRequest();

	public long getNumberOfRealIsoChecking();

	public int getNumberOfComputeTemporalMatchSetDuration();

	public long getNumberOfBiSimCheckingRequest();

	public long getNumberOfRealBiSimChecking();

	public void incNumberOfComputeTemporalMatchSet();

	public double getDurationOfComputeTemporalMatchSet();

	public void updateDurationOfComputeTemporalMatchSet(double newDuration);

	public void resetNumberOfIsoChecking();

	public void resetDurationOfIsoChecking();

	public void resetNumberOfComputeTemporalMatchSet();

	public void resetDurationOfComputeTemporalMatchSet();

	public void resetDurationOfNewLatticeGeneration();

	public void incrementBiSimCheckingRequest();

	public void incrementRealBiSimChecking();

	public void updateDurationOfBiSimChecking(double newDuration);

	void resetDurationOfBiSimChecking();

}
