package com.algovision.visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public final class RaceBarsView extends JPanel {
    private static final Color CANVAS = new Color(0x121a2b);
    private static final Color BASE_BAR = new Color(0x7774f7);
    private static final Color COMPARED_BAR = new Color(0x42b8d2);
    private static final Color SWAPPED_BAR = new Color(0xf06c7d);
    private static final Color PIVOT_BAR = new Color(0xf0a64b);
    private static final Color SORTED_BAR = new Color(0x35b58a);
    private static final Color VALUE_TEXT = new Color(0xc8d1e3);
    private static final Color BASELINE = new Color(0x33405b);
    private static final int HORIZONTAL_PADDING = 18;
    private static final int TOP_PADDING = 28;
    private static final int BOTTOM_PADDING = 24;

    private int[] values = new int[0];
    private boolean[] sortedElements = new boolean[0];
    private int comparedIndexLeft = -1;
    private int comparedIndexRight = -1;
    private int swappedIndexLeft = -1;
    private int swappedIndexRight = -1;
    private int pivotIndex = -1;

    public RaceBarsView() {
        setOpaque(true);
        setBackground(CANVAS);
        setPreferredSize(new Dimension(420, 290));
        setMinimumSize(new Dimension(220, 180));
        setBorder(BorderFactory.createLineBorder(new Color(0x283550)));
    }

    public void render(
            int[] nextValues,
            int comparedLeft,
            int comparedRight,
            int nextPivotIndex,
            boolean[] nextSortedElements) {
        render(nextValues, comparedLeft, comparedRight, -1, -1, nextPivotIndex,
                nextSortedElements);
    }

    /**
     * Updates the immutable rendering snapshot for one animation step.  Rendering only reads
     * this snapshot; algorithm state is never changed from {@link #paintComponent(Graphics)}.
     */
    public void render(
            int[] nextValues,
            int comparedLeft,
            int comparedRight,
            int swappedLeft,
            int swappedRight,
            int nextPivotIndex,
            boolean[] nextSortedElements) {
        Objects.requireNonNull(nextValues, "nextValues");
        Objects.requireNonNull(nextSortedElements, "nextSortedElements");
        if (nextValues.length != nextSortedElements.length) {
            throw new IllegalArgumentException("Values and sorted flags must have equal lengths.");
        }
        values = nextValues.clone();
        comparedIndexLeft = comparedLeft;
        comparedIndexRight = comparedRight;
        swappedIndexLeft = swappedLeft;
        swappedIndexRight = swappedRight;
        pivotIndex = nextPivotIndex;
        sortedElements = nextSortedElements.clone();
        repaint();
    }

    /** Marks the current displayed array as complete without changing its values. */
    public void markCompleted() {
        boolean[] completed = new boolean[values.length];
        java.util.Arrays.fill(completed, true);
        sortedElements = completed;
        comparedIndexLeft = -1;
        comparedIndexRight = -1;
        swappedIndexLeft = -1;
        swappedIndexRight = -1;
        pivotIndex = -1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (values.length == 0) {
                g.setColor(VALUE_TEXT);
                g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
                drawCentered(g, "Empty array", getWidth() / 2, getHeight() / 2);
                return;
            }

            int min = values[0];
            int max = values[0];
            for (int value : values) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            int baseline = getHeight() - BOTTOM_PADDING;
            int availableWidth = Math.max(1, getWidth() - HORIZONTAL_PADDING * 2);
            int availableHeight = Math.max(1, baseline - TOP_PADDING);
            double slotWidth = (double) availableWidth / values.length;
            int gap = values.length > 28 ? 1 : 4;
            int barWidth = Math.max(1, (int) Math.floor(slotWidth) - gap);
            long range = (long) max - min;

            g.setColor(BASELINE);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(HORIZONTAL_PADDING, baseline, getWidth() - HORIZONTAL_PADDING, baseline);

            Font valueFont = getFont().deriveFont(Font.PLAIN, values.length > 18 ? 9f : 11f);
            g.setFont(valueFont);
            FontMetrics metrics = g.getFontMetrics();
            for (int index = 0; index < values.length; index++) {
                int value = values[index];
                double ratio = range == 0 ? 0.62 : ((long) value - min) / (double) range;
                int barHeight = Math.max(4, (int) Math.round(8 + ratio * (availableHeight - 12)));
                int x = HORIZONTAL_PADDING + (int) Math.round(index * slotWidth)
                        + Math.max(0, ((int) Math.floor(slotWidth) - barWidth) / 2);
                int y = baseline - barHeight;

                g.setColor(colorFor(index));
                g.fillRoundRect(x, y, barWidth, barHeight, 5, 5);

                if (values.length <= 24) {
                    String label = Integer.toString(value);
                    int labelX = x + (barWidth - metrics.stringWidth(label)) / 2;
                    g.setColor(VALUE_TEXT);
                    g.drawString(label, labelX, Math.max(metrics.getAscent() + 2, y - 5));
                }
            }
        } finally {
            g.dispose();
        }
    }

    private Color colorFor(int index) {
        if (index == swappedIndexLeft || index == swappedIndexRight) {
            return SWAPPED_BAR;
        }
        if (index == comparedIndexLeft || index == comparedIndexRight) {
            return COMPARED_BAR;
        }
        if (index == pivotIndex) {
            return PIVOT_BAR;
        }
        if (sortedElements[index]) {
            return SORTED_BAR;
        }
        return BASE_BAR;
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int centerY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2,
                centerY + metrics.getAscent() / 2);
    }
}
