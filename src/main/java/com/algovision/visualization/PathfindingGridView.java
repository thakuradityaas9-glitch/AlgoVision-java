package com.algovision.visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.algovision.pathfinding.model.AStarStep;
import com.algovision.pathfinding.model.GridPosition;
import com.algovision.pathfinding.model.PathfindingGrid;

public final class PathfindingGridView extends JPanel {
    private static final Color GRID_BACKGROUND = new Color(0x0c1426);
    private static final Color EMPTY_COLOR = new Color(0x172238);
    private static final Color START_COLOR = new Color(0x2fb889);
    private static final Color END_COLOR = new Color(0xe56776);
    private static final Color WALL_COLOR = new Color(0x060a13);
    private static final Color OPEN_COLOR = new Color(0x355b82);
    private static final Color CURRENT_COLOR = new Color(0xf0a64b);
    private static final Color NEIGHBOR_COLOR = new Color(0x42b8d0);
    private static final Color EXPLORED_COLOR = new Color(0x247e83);
    private static final Color PATH_COLOR = new Color(0x8056dc);
    private static final Color CELL_BORDER_COLOR = new Color(0x26334c);

    private final PathfindingGrid grid;
    private Consumer<GridPosition> cellActionHandler;
    private AStarStep currentStep;
    private List<GridPosition> visiblePath = List.of();
    private GridPosition lastActionCell;
    private boolean drawing;

    public PathfindingGridView(PathfindingGrid grid) {
        this.grid = Objects.requireNonNull(grid, "grid");
        setOpaque(true);
        setBackground(GRID_BACKGROUND);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setPreferredSize(new Dimension(760, 450));
        setMinimumSize(new Dimension(360, 240));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                drawing = true;
                lastActionCell = null;
                applyCellAction(event.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                drawing = false;
                lastActionCell = null;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (isEnabled() && drawing
                        && (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                    applyCellAction(event.getPoint());
                }
            }
        });
    }

    public void setCellActionHandler(Consumer<GridPosition> handler) {
        cellActionHandler = Objects.requireNonNull(handler, "handler");
    }

    public void render(AStarStep step) {
        currentStep = step;
        visiblePath = step == null ? List.of() : step.path();
        repaint();
    }

    public void render(AStarStep step, List<GridPosition> visiblePath) {
        currentStep = Objects.requireNonNull(step, "step");
        this.visiblePath = List.copyOf(visiblePath);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(0.7f));
        int width = getWidth();
        int height = getHeight();

        for (int row = 0; row < grid.rows(); row++) {
            int top = row * height / grid.rows();
            int bottom = (row + 1) * height / grid.rows();
            for (int column = 0; column < grid.columns(); column++) {
                int left = column * width / grid.columns();
                int right = (column + 1) * width / grid.columns();
                int cellWidth = Math.max(1, right - left - 1);
                int cellHeight = Math.max(1, bottom - top - 1);
                GridPosition position = new GridPosition(row, column);
                g2.setColor(cellColor(position));
                g2.fillRect(left, top, cellWidth, cellHeight);
                g2.setColor(CELL_BORDER_COLOR);
                g2.drawRect(left, top, cellWidth, cellHeight);
            }
        }
        g2.dispose();
    }

    private void applyCellAction(Point point) {
        GridPosition position = positionAt(point);
        if (position == null || position.equals(lastActionCell) || cellActionHandler == null) {
            return;
        }
        lastActionCell = position;
        cellActionHandler.accept(position);
    }

    private GridPosition positionAt(Point point) {
        if (getWidth() <= 0 || getHeight() <= 0
                || point.x < 0 || point.x >= getWidth()
                || point.y < 0 || point.y >= getHeight()) {
            return null;
        }
        return new GridPosition(
                point.y * grid.rows() / getHeight(),
                point.x * grid.columns() / getWidth());
    }

    private Color cellColor(GridPosition position) {
        if (position.equals(grid.start())) {
            return START_COLOR;
        }
        if (position.equals(grid.end())) {
            return END_COLOR;
        }
        if (grid.isWall(position)) {
            return WALL_COLOR;
        }
        if (currentStep == null) {
            return EMPTY_COLOR;
        }
        if (visiblePath.contains(position)) {
            return PATH_COLOR;
        }
        if (position.equals(currentStep.currentCell())) {
            return CURRENT_COLOR;
        }
        if (position.equals(currentStep.evaluatedNeighbor())) {
            return NEIGHBOR_COLOR;
        }
        if (currentStep.exploredCells().contains(position)) {
            return EXPLORED_COLOR;
        }
        if (currentStep.openCells().contains(position)) {
            return OPEN_COLOR;
        }
        return EMPTY_COLOR;
    }
}
