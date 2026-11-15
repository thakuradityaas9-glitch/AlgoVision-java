package com.algovision.graph.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.algovision.graph.algorithms.BreadthFirstSearch;

class RandomGraphGeneratorTest {
    @Test
    void repeatedGraphsAreConnectedUniqueWeightedAndHaveValidSizes() {
        Random random = new Random(491027L);
        for (int iteration = 0; iteration < 500; iteration++) {
            Graph graph = RandomGraphGenerator.generate(random);
            int nodeCount = graph.nodeCount();
            int edgeCount = graph.edgeCount();

            assertTrue(nodeCount >= 7 && nodeCount <= 9);
            assertTrue(edgeCount >= nodeCount - 1);
            assertTrue(edgeCount <= nodeCount + nodeCount / 2);
            assertEquals(edgeCount, new HashSet<>(graph.edges()).size());
            assertTrue(graph.edges().stream().allMatch(edge ->
                    edge.fromId() != edge.toId() && edge.weight() >= 0));
            assertEquals(nodeCount, new BreadthFirstSearch()
                    .generateSteps(graph, graph.nodes().getFirst().id())
                    .getLast().visitedNodes().size());

            Set<Integer> ids = new HashSet<>();
            graph.nodes().forEach(node -> assertTrue(ids.add(node.id())));
            assertFalse(graph.edges().isEmpty());
        }
    }

    @Test
    void generatorRejectsNullRandomSource() {
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> RandomGraphGenerator.generate(null));
    }
}
