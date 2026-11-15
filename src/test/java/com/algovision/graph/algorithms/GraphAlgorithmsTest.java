package com.algovision.graph.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.algovision.graph.model.DijkstraEvent;
import com.algovision.graph.model.DijkstraStep;
import com.algovision.graph.model.Graph;
import com.algovision.graph.model.GraphNode;
import com.algovision.graph.model.GraphTraversalEvent;
import com.algovision.graph.model.GraphTraversalStep;
import org.junit.jupiter.api.Test;

class GraphAlgorithmsTest {
    @Test
    void bfsVisitsOnlyTheConnectedComponentFromSelectedStart() {
        Graph graph = new Graph();
        GraphNode one = graph.addNode();
        GraphNode two = graph.addNode();
        GraphNode three = graph.addNode();
        GraphNode four = graph.addNode();
        GraphNode five = graph.addNode();
        graph.connect(one.id(), two.id());
        graph.connect(two.id(), three.id());
        graph.connect(four.id(), five.id());

        List<GraphTraversalStep> steps = new BreadthFirstSearch().generateSteps(graph, four.id());
        GraphTraversalStep complete = steps.getLast();

        assertEquals(Set.of(four.id(), five.id()), complete.visitedNodes());
        assertEquals(List.of(four.id(), five.id()), complete.traversalOrder());
        assertEquals(GraphTraversalEvent.COMPLETE, complete.event());
        assertTrue(steps.stream().anyMatch(step -> step.event() == GraphTraversalEvent.INSPECT_EDGE));
    }

    @Test
    void bfsHandlesSingleNodeAndEdgelessGraphs() {
        Graph graph = new Graph();
        GraphNode first = graph.addNode();
        GraphTraversalStep singleNode = new BreadthFirstSearch()
                .generateSteps(graph, first.id()).getLast();
        GraphNode second = graph.addNode();

        GraphTraversalStep complete = new BreadthFirstSearch()
                .generateSteps(graph, second.id()).getLast();

        assertEquals(Set.of(first.id()), singleNode.visitedNodes());
        assertEquals(Set.of(second.id()), complete.visitedNodes());
        assertEquals(List.of(second.id()), complete.traversalOrder());
        assertEquals(2, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
        assertFalse(complete.visitedNodes().contains(first.id()));
    }

    @Test
    void dfsHandlesCyclesWithoutVisitingAnyNodeTwice() {
        Graph graph = new Graph();
        GraphNode one = graph.addNode();
        GraphNode two = graph.addNode();
        GraphNode three = graph.addNode();
        GraphNode four = graph.addNode();
        graph.connect(one.id(), two.id());
        graph.connect(two.id(), three.id());
        graph.connect(three.id(), one.id());
        graph.connect(three.id(), four.id());

        List<GraphTraversalStep> steps = new DepthFirstSearch().generateSteps(graph, one.id());
        GraphTraversalStep complete = steps.getLast();

        assertEquals(Set.of(one.id(), two.id(), three.id(), four.id()), complete.visitedNodes());
        assertEquals(complete.traversalOrder().size(),
                new HashSet<>(complete.traversalOrder()).size());
        assertTrue(steps.stream().anyMatch(step -> step.event() == GraphTraversalEvent.BACKTRACK));
        assertTrue(steps.stream().anyMatch(step -> step.event() == GraphTraversalEvent.INSPECT_EDGE));
    }

    @Test
    void dijkstraFindsWeightedShortestPathFromDifferentSourcesAndHandlesTies() {
        Graph graph = new Graph();
        GraphNode one = graph.addNode();
        GraphNode two = graph.addNode();
        GraphNode three = graph.addNode();
        GraphNode four = graph.addNode();
        GraphNode five = graph.addNode();
        graph.connect(one.id(), two.id(), 2);
        graph.connect(one.id(), three.id(), 1);
        graph.connect(two.id(), four.id(), 2);
        graph.connect(three.id(), four.id(), 3);
        graph.connect(four.id(), five.id(), 0);

        DijkstraStep result = new DijkstraShortestPath()
                .generateSteps(graph, one.id(), five.id()).getLast();
        DijkstraStep fromThree = new DijkstraShortestPath()
                .generateSteps(graph, three.id(), one.id()).getLast();

        assertEquals(BigInteger.valueOf(4), result.shortestDistance());
        assertValidPath(graph, result.shortestPath(), one.id(), five.id(), 4);
        assertEquals(BigInteger.ONE, fromThree.shortestDistance());
        assertValidPath(graph, fromThree.shortestPath(), three.id(), one.id(), 1);
        assertEquals(DijkstraEvent.COMPLETE, result.event());
    }

    @Test
    void dijkstraHandlesUnreachableAndSameNodeDestinations() {
        Graph graph = new Graph();
        GraphNode source = graph.addNode();
        GraphNode destination = graph.addNode();

        DijkstraShortestPath algorithm = new DijkstraShortestPath();
        DijkstraStep unreachable = algorithm.generateSteps(
                graph, source.id(), destination.id()).getLast();
        DijkstraStep sameNode = algorithm.generateSteps(
                graph, source.id(), source.id()).getLast();

        assertNull(unreachable.shortestDistance());
        assertTrue(unreachable.shortestPath().isEmpty());
        assertEquals(BigInteger.ZERO, sameNode.shortestDistance());
        assertEquals(List.of(source.id()), sameNode.shortestPath());
    }

    @Test
    void algorithmsRejectAnEmptyGraphWithoutProducingInvalidSteps() {
        Graph graph = new Graph();
        assertTrue(graph.nodes().isEmpty());
        assertTrue(graph.edges().isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new BreadthFirstSearch().generateSteps(graph, 1));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DepthFirstSearch().generateSteps(graph, 1));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DijkstraShortestPath().generateSteps(graph, 1, 1));
    }

    @Test
    void graphCopyPreservesIdsWeightsAndCanBeEditedIndependently() {
        Graph graph = new Graph();
        GraphNode first = graph.addNode();
        GraphNode removed = graph.addNode();
        GraphNode last = graph.addNode();
        graph.connect(first.id(), last.id(), 7);
        graph.removeNode(removed.id());

        Graph copy = graph.copy();
        copy.setEdgeWeight(first.id(), last.id(), 2);
        GraphNode nextCopyNode = copy.addNode();

        assertEquals(2, copy.getEdge(first.id(), last.id()).weight());
        assertEquals(7, graph.getEdge(first.id(), last.id()).weight());
        assertEquals(4, nextCopyNode.id());
        assertEquals(List.of(first, last, nextCopyNode), copy.nodes());
    }

    private void assertValidPath(
            Graph graph, List<Integer> path, int source, int destination, long expectedDistance) {
        assertFalse(path.isEmpty());
        assertEquals(source, path.getFirst());
        assertEquals(destination, path.getLast());
        long distance = 0;
        for (int index = 1; index < path.size(); index++) {
            distance += graph.getEdge(path.get(index - 1), path.get(index)).weight();
        }
        assertEquals(expectedDistance, distance);
    }
}
