package com.algovision;

import com.algovision.controller.SortingVisualizerController;
import com.algovision.controller.GraphVisualizerController;
import com.algovision.controller.PathfindingController;
import com.algovision.controller.AlgorithmRaceController;
import com.algovision.controller.AiCodeAnalyzerController;
import com.algovision.ui.AlgoVisionTheme;
import com.algovision.ui.ScrollablePanel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

public final class MainApp {
    private static final Color APP_BACKGROUND = AlgoVisionTheme.APP_BACKGROUND;
    private static final Color SIDEBAR_BACKGROUND = AlgoVisionTheme.SIDEBAR_BACKGROUND;
    private static final Color PANEL_BACKGROUND = AlgoVisionTheme.PANEL_BACKGROUND;
    private static final Color CARD_BACKGROUND = AlgoVisionTheme.CARD_BACKGROUND;
    private static final Color BORDER_COLOR = AlgoVisionTheme.BORDER;
    private static final Color PRIMARY_COLOR = AlgoVisionTheme.PRIMARY;
    private static final Color PRIMARY_TEXT = AlgoVisionTheme.TEXT_PRIMARY;
    private static final Color BODY_TEXT = AlgoVisionTheme.TEXT_BODY;
    private static final Color MUTED_TEXT = AlgoVisionTheme.TEXT_SUBTLE;
    private static final Color ACCENT_TEXT = AlgoVisionTheme.ACCENT;

    private static final String DASHBOARD = "dashboard";
    private static final Map<String, String> WORKSPACES = new LinkedHashMap<>();

    static {
        WORKSPACES.put("Sorting Visualizer", "sorting");
        WORKSPACES.put("Graph Visualizer", "graph");
        WORKSPACES.put("Pathfinding", "pathfinding");
        WORKSPACES.put("Algorithm Race", "race");
        WORKSPACES.put("AI Code Analyzer", "analyzer");
    }

    private final JFrame frame = new JFrame("AlgoVision");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel workspaceCards = new JPanel(cardLayout);
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private GraphVisualizerController graphController;
    private PathfindingController pathfindingController;
    private AlgorithmRaceController raceController;
    private AiCodeAnalyzerController analyzerController;
    private SortingVisualizerController sortingController;

    private MainApp() {
        configureLookAndFeel();
        createWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainApp().show());
    }

    private void configureLookAndFeel() {
        AlgoVisionTheme.configureDefaults();
    }

    private void createWindow() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(APP_BACKGROUND);
        root.add(createSidebar(), BorderLayout.WEST);

        workspaceCards.setBackground(APP_BACKGROUND);
        workspaceCards.add(createDashboard(), DASHBOARD);
        graphController = new GraphVisualizerController();
        pathfindingController = new PathfindingController();
        raceController = new AlgorithmRaceController();
        analyzerController = new AiCodeAnalyzerController();
        sortingController = new SortingVisualizerController();
        WORKSPACES.forEach((name, card) -> {
            if ("Sorting Visualizer".equals(name)) {
                workspaceCards.add(sortingController, card);
            } else if ("Graph Visualizer".equals(name)) {
                workspaceCards.add(graphController, card);
            } else if ("Pathfinding".equals(name)) {
                workspaceCards.add(pathfindingController, card);
            } else if ("Algorithm Race".equals(name)) {
                workspaceCards.add(raceController, card);
            } else if ("AI Code Analyzer".equals(name)) {
                workspaceCards.add(analyzerController, card);
            }
        });
        root.add(workspaceCards, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);
        selectNavigation("Dashboard");
    }

    private void show() {
        frame.setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_BACKGROUND);
        sidebar.setPreferredSize(new Dimension(248, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR),
                new EmptyBorder(28, 18, 24, 18)));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        JLabel logo = new JLabel("A", SwingConstants.CENTER);
        logo.setOpaque(true);
        logo.setBackground(PRIMARY_COLOR);
        logo.setForeground(Color.WHITE);
        logo.setFont(font(Font.BOLD, 19));
        logo.setPreferredSize(new Dimension(38, 38));
        logo.setMinimumSize(new Dimension(38, 38));
        logo.setMaximumSize(new Dimension(38, 38));
        JLabel title = new JLabel("AlgoVision");
        title.setForeground(PRIMARY_TEXT);
        title.setFont(font(Font.BOLD, 18));
        brand.add(logo);
        brand.add(Box.createHorizontalStrut(12));
        brand.add(title);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(new EmptyBorder(34, 0, 0, 0));
        navigation.setAlignmentX(Component.LEFT_ALIGNMENT);
        addNavigationButton(navigation, "Dashboard", DASHBOARD);
        WORKSPACES.forEach((name, card) -> addNavigationButton(navigation, name, card));

        JLabel footer = new JLabel("LEARN BY SEEING");
        footer.setForeground(MUTED_TEXT);
        footer.setFont(font(Font.BOLD, 10));
        footer.setBorder(new EmptyBorder(0, 10, 0, 0));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brand);
        sidebar.add(navigation);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(footer);
        return sidebar;
    }

    private void addNavigationButton(JPanel navigation, String label, String card) {
        JButton button = new JButton(label);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setBorder(new EmptyBorder(0, 13, 0, 13));
        button.setFont(font(Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.addActionListener(event -> showWorkspace(label, card));
        navigationButtons.put(label, button);
        navigation.add(button);
        navigation.add(Box.createVerticalStrut(8));
    }

    private void selectNavigation(String selected) {
        navigationButtons.forEach((label, button) -> {
            boolean isSelected = label.equals(selected);
            button.setBackground(isSelected ? AlgoVisionTheme.PRIMARY_SELECTED : SIDEBAR_BACKGROUND);
            button.setForeground(isSelected ? AlgoVisionTheme.ACCENT : BODY_TEXT);
            button.setFont(font(isSelected ? Font.BOLD : Font.PLAIN, 13));
        });
    }

    private JScrollPane createDashboard() {
        JPanel content = new ScrollablePanel();
        content.setBackground(APP_BACKGROUND);
        content.setBorder(new EmptyBorder(42, 48, 36, 48));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel introduction = new JPanel();
        introduction.setOpaque(false);
        introduction.setLayout(new BoxLayout(introduction, BoxLayout.Y_AXIS));
        introduction.setAlignmentX(Component.LEFT_ALIGNMENT);
        introduction.add(label("YOUR ALGORITHMS, MADE VISIBLE", ACCENT_TEXT, Font.BOLD, 10));
        introduction.add(Box.createVerticalStrut(10));
        introduction.add(label("Welcome to AlgoVision", PRIMARY_TEXT, Font.BOLD, 32));
        introduction.add(Box.createVerticalStrut(10));
        introduction.add(label("Understand algorithms. Visualize logic. Master DSA.",
                BODY_TEXT, Font.PLAIN, 14));

        JPanel featureCards = new JPanel(new GridLayout(1, 3, 18, 0));
        featureCards.setOpaque(false);
        featureCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        featureCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 225));
        featureCards.setPreferredSize(new Dimension(700, 225));
        featureCards.add(createFeatureCard("01", "Sorting Algorithms",
                "Watch common sorting techniques organize data, step by step.",
                "EXPLORE SORTING", "Sorting Visualizer"));
        featureCards.add(createFeatureCard("02", "Graph Algorithms",
                "See how graph traversal and search algorithms move through nodes.",
                "EXPLORE GRAPHS", "Graph Visualizer"));
        featureCards.add(createFeatureCard("03", "Pathfinding Algorithms",
                "Visualize how algorithms discover a route across a grid.",
                "EXPLORE PATHFINDING", "Pathfinding"));

        RoundedPanel projectInfo = new RoundedPanel(PANEL_BACKGROUND, BORDER_COLOR, 16);
        projectInfo.setLayout(new BoxLayout(projectInfo, BoxLayout.Y_AXIS));
        projectInfo.setBorder(new EmptyBorder(22, 24, 22, 24));
        projectInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        projectInfo.add(label("A clearer way to learn DSA", PRIMARY_TEXT, Font.BOLD, 16));
        projectInfo.add(Box.createVerticalStrut(10));
        projectInfo.add(wrappedLabel(
                "AlgoVision is an interactive learning space for exploring data structures "
                        + "and algorithms. Follow each step, build intuition, and make complex "
                        + "ideas easier to understand.",
                BODY_TEXT, 12));

        content.add(introduction);
        content.add(Box.createVerticalStrut(34));
        content.add(featureCards);
        content.add(Box.createVerticalStrut(28));
        JPanel projectStats = new JPanel(new GridLayout(1, 3, 14, 0));
        projectStats.setOpaque(false);
        projectStats.setAlignmentX(Component.LEFT_ALIGNMENT);
        projectStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        projectStats.add(createDashboardStat("05", "Interactive workspaces"));
        projectStats.add(createDashboardStat("05", "Sorting algorithms"));
        projectStats.add(createDashboardStat("03", "Graph algorithms"));
        content.add(projectStats);
        content.add(Box.createVerticalStrut(22));
        content.add(projectInfo);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(APP_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createDashboardStat(String value, String title) {
        RoundedPanel card = new RoundedPanel(CARD_BACKGROUND, BORDER_COLOR, 14);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(13, 16, 12, 16));
        card.setAlignmentY(Component.TOP_ALIGNMENT);
        card.add(label(value, ACCENT_TEXT, Font.BOLD, 22));
        card.add(Box.createVerticalStrut(3));
        card.add(label(title, BODY_TEXT, Font.PLAIN, 11));
        return card;
    }

    private JPanel createFeatureCard(
            String number, String title, String description, String action, String workspace) {
        FeatureCard card = new FeatureCard(() -> navigateTo(workspace));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.setAlignmentY(Component.TOP_ALIGNMENT);
        card.add(label(number, ACCENT_TEXT, Font.BOLD, 11));
        card.add(Box.createVerticalStrut(14));
        card.add(wrappedLabel(title, PRIMARY_TEXT, 16, Font.BOLD));
        card.add(Box.createVerticalStrut(12));
        card.add(wrappedLabel(description, BODY_TEXT, 12));
        card.add(Box.createVerticalGlue());
        card.add(label(action + "  >", AlgoVisionTheme.ACCENT, Font.BOLD, 9));
        card.bindInteractionToChildren();
        return card;
    }

    private void navigateTo(String workspace) {
        String card = WORKSPACES.get(workspace);
        if (card != null) {
            showWorkspace(workspace, card);
        }
    }

    private void showWorkspace(String label, String card) {
        if (sortingController != null && !"Sorting Visualizer".equals(label)) {
            sortingController.pause();
        }
        if (graphController != null && !"Graph Visualizer".equals(label)) {
            graphController.pause();
        }
        if (pathfindingController != null && !"Pathfinding".equals(label)) {
            pathfindingController.pause();
        }
        if (raceController != null && !"Algorithm Race".equals(label)) {
            raceController.pause();
        }
        cardLayout.show(workspaceCards, card);
        selectNavigation(label);
    }

    private JLabel label(String text, Color color, int style, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font(style, size));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel wrappedLabel(String text, Color color, int size) {
        return wrappedLabel(text, color, size, Font.PLAIN);
    }

    private JLabel wrappedLabel(String text, Color color, int size, int style) {
        JLabel label = new JLabel("<html><div style='width: 100%;'>" + escapeHtml(text) + "</div></html>");
        label.setForeground(color);
        label.setFont(font(style, size));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Font font(int style, int size) {
        return AlgoVisionTheme.font(style, size);
    }

    private final class FeatureCard extends RoundedPanel {
        private boolean hovered;

        private FeatureCard(Runnable action) {
            super(CARD_BACKGROUND, AlgoVisionTheme.BORDER_SUBTLE, 16);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            MouseAdapter interaction = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        action.run();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    setHovered(false);
                }
            };
            addMouseListener(interaction);
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "activate");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "activate");
            getActionMap().put("activate", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent event) {
                    action.run();
                }
            });
            interactionListener = interaction;
        }

        private final MouseAdapter interactionListener;

        private void bindInteractionToChildren() {
            for (Component component : getComponents()) {
                bindInteraction(component);
            }
        }

        private void bindInteraction(Component component) {
            component.addMouseListener(interactionListener);
            if (component instanceof java.awt.Container container) {
                for (Component child : container.getComponents()) {
                    bindInteraction(child);
                }
            }
        }

        private void setHovered(boolean value) {
            hovered = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (hovered) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x17223b));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(PRIMARY_COLOR);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color background;
        private final Color border;
        private final int radius;

        private RoundedPanel(Color background, Color border, int radius) {
            this.background = background;
            this.border = border;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}
