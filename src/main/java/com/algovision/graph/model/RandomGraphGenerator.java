package com.algovision.graph.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class RandomGraphGenerator {
    private static final int MIN_NODES = 7;
    private static final int MAX_NODES = 9;
    private static final int MAX_WEIGHT = 9;

    private RandomGraphGenerator() {
    }

    public static Graph generate(Random random) {
        Objects.requireNonNull(random, "random");
        Graph graph = new Graph();
        int nodeCount = MIN_NODES + random.nextInt(MAX_NODES - MIN_NODES + 1);
        List<GraphNode> nodes = new ArrayList<>(nodeCount);
        for (int index = 0; index < nodeCount; index++) {
            nodes.add(graph.addNode());
        }

        Set<Long> connectedPairs = new HashSet<>();
        for (int index = 1; index < nodeCount; index++) {
            int neighbor = random.nextInt(index);
            graph.connect(nodes.get(index).id(), nodes.get(neighbor).id(), randomWeight(random));
            connectedPairs.add(pairKey(index, neighbor));
        }

        List<int[]> remainingPairs = new ArrayList<>();
        for (int first = 0; first < nodeCount; first++) {
            for (int second = first + 1; second < nodeCount; second++) {
                if (!connectedPairs.contains(pairKey(first, second))) {
                    remainingPairs.add(new int[] { first, second });
                }
            }
        }
        java.util.Collections.shuffle(remainingPairs, random);

        int targetEdges = nodeCount + random.nextInt(nodeCount / 2 + 1);
        int additionalEdges = targetEdges - graph.edgeCount();
        for (int index = 0; index < additionalEdges; index++) {
            int[] pair = remainingPairs.get(index);
            graph.connect(nodes.get(pair[0]).id(), nodes.get(pair[1]).id(), randomWeight(random));
        }
        return graph;
    }

    private static long pairKey(int first, int second) {
        int lower = Math.min(first, second);
        int upper = Math.max(first, second);
        return ((long) lower << 32) | (upper & 0xffffffffL);
    }

    private static int randomWeight(Random random) {
        return 1 + random.nextInt(MAX_WEIGHT);
    }
}
