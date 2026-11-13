package com.algovision.visualization;

import com.algovision.graph.model.GraphEdge;

public interface GraphCanvasListener {
    void onEmptyCanvasClick(double xRatio, double yRatio);

    void onNodeClick(int nodeId);

    void onEdgeClick(GraphEdge edge);
}
