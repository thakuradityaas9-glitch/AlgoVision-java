package com.algovision;

import com.algovision.controller.GraphVisualizerController;
import com.algovision.controller.PathfindingController;
import com.algovision.controller.AlgorithmRaceController;
import com.algovision.controller.SortingVisualizerController;

import java.util.Objects;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    private BorderPane root;
    private Button dashboardNavButton;
    private Button sortingNavButton;
    private Button graphNavButton;
    private Button pathfindingNavButton;
    private Button raceNavButton;
    private SortingVisualizerController sortingController;
    private GraphVisualizerController graphController;
    private PathfindingController pathfindingController;
    private AlgorithmRaceController raceController;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.getStyleClass().add("app-root");
        sortingController = new SortingVisualizerController();
        graphController = new GraphVisualizerController();
        pathfindingController = new PathfindingController();
        raceController = new AlgorithmRaceController();
        root.setLeft(createSidebar());
        root.setCenter(createDashboard());

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());

        stage.setTitle("AlgoVision");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(30);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(248);
        sidebar.setPadding(new Insets(28, 18, 24, 18));

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("A");
        logo.getStyleClass().add("brand-mark");
        Label title = new Label("AlgoVision");
        title.getStyleClass().add("brand-title");
        brand.getChildren().addAll(logo, title);

        VBox navigation = new VBox(8);
        navigation.getStyleClass().add("navigation");
        dashboardNavButton = createNavButton("Dashboard", true);
        sortingNavButton = createNavButton("Sorting Visualizer", false);
        graphNavButton = createNavButton("Graph Visualizer", false);
        pathfindingNavButton = createNavButton("Pathfinding", false);
        raceNavButton = createNavButton("Algorithm Race", false);
        dashboardNavButton.setOnAction(event -> showDashboard());
        sortingNavButton.setOnAction(event -> showSortingVisualizer());
        graphNavButton.setOnAction(event -> showGraphVisualizer());
        pathfindingNavButton.setOnAction(event -> showPathfinding());
        raceNavButton.setOnAction(event -> showAlgorithmRace());
        navigation.getChildren().addAll(
                dashboardNavButton,
                sortingNavButton,
                graphNavButton,
                pathfindingNavButton,
                raceNavButton,
                createNavButton("AI Code Analyzer", false));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("LEARN BY SEEING");
        footer.getStyleClass().add("sidebar-footer");

        sidebar.getChildren().addAll(brand, navigation, spacer, footer);
        return sidebar;
    }

    private void showDashboard() {
        sortingController.pause();
        graphController.pause();
        pathfindingController.pause();
        raceController.pause();
        root.setCenter(createDashboard());
        setSelectedNavigation(dashboardNavButton);
    }

    private void showSortingVisualizer() {
        graphController.pause();
        pathfindingController.pause();
        raceController.pause();
        root.setCenter(sortingController.getView());
        setSelectedNavigation(sortingNavButton);
    }

    private void showGraphVisualizer() {
        sortingController.pause();
        pathfindingController.pause();
        raceController.pause();
        root.setCenter(graphController.getView());
        setSelectedNavigation(graphNavButton);
    }

    private void showPathfinding() {
        sortingController.pause();
        graphController.pause();
        raceController.pause();
        root.setCenter(pathfindingController.getView());
        setSelectedNavigation(pathfindingNavButton);
    }

    private void showAlgorithmRace() {
        sortingController.pause();
        graphController.pause();
        pathfindingController.pause();
        root.setCenter(raceController.getView());
        setSelectedNavigation(raceNavButton);
    }

    private void setSelectedNavigation(Button selectedButton) {
        dashboardNavButton.getStyleClass().remove("selected");
        sortingNavButton.getStyleClass().remove("selected");
        graphNavButton.getStyleClass().remove("selected");
        pathfindingNavButton.getStyleClass().remove("selected");
        raceNavButton.getStyleClass().remove("selected");
        if (!selectedButton.getStyleClass().contains("selected")) {
            selectedButton.getStyleClass().add("selected");
        }
    }

    private Button createNavButton(String text, boolean selected) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        if (selected) {
            button.getStyleClass().add("selected");
        }
        return button;
    }

    private ScrollPane createDashboard() {
        VBox content = new VBox(28);
        content.getStyleClass().add("dashboard-content");
        content.setPadding(new Insets(42, 48, 36, 48));

        VBox introduction = new VBox(10);
        Label eyebrow = new Label("YOUR ALGORITHMS, MADE VISIBLE");
        eyebrow.getStyleClass().add("eyebrow");
        Label heading = new Label("Welcome to AlgoVision");
        heading.getStyleClass().add("welcome-heading");
        Label subtitle = new Label("Understand algorithms. Visualize logic. Master DSA.");
        subtitle.getStyleClass().add("welcome-subtitle");
        introduction.getChildren().addAll(eyebrow, heading, subtitle);

        HBox cards = new HBox(18,
                createFeatureCard("01", "Sorting Algorithms",
                        "Watch common sorting techniques organize data, step by step.", "EXPLORE SORTING"),
                createFeatureCard("02", "Graph Algorithms",
                        "See how graph traversal and search algorithms move through nodes.", "EXPLORE GRAPHS"),
                createFeatureCard("03", "Pathfinding Algorithms",
                        "Visualize how algorithms discover a route across a grid.", "EXPLORE PATHFINDING"));
        cards.setAlignment(Pos.CENTER);

        VBox projectInfo = new VBox(10);
        projectInfo.getStyleClass().add("project-card");
        Label projectTitle = new Label("A clearer way to learn DSA");
        projectTitle.getStyleClass().add("project-title");
        Label projectDescription = new Label(
                "AlgoVision is an interactive learning space for exploring data structures "
                        + "and algorithms. Follow each step, build intuition, and make complex "
                        + "ideas easier to understand.");
        projectDescription.getStyleClass().add("project-description");
        projectDescription.setWrapText(true);
        projectInfo.getChildren().addAll(projectTitle, projectDescription);

        content.getChildren().addAll(introduction, cards, projectInfo);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox createFeatureCard(String number, String title, String description, String action) {
        VBox card = new VBox(14);
        card.getStyleClass().add("feature-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(220);
        card.setMinHeight(225);

        Label cardNumber = new Label(number);
        cardNumber.getStyleClass().add("card-number");
        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("card-title");
        cardTitle.setWrapText(true);
        Label cardDescription = new Label(description);
        cardDescription.getStyleClass().add("card-description");
        cardDescription.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label actionLabel = new Label(action + "  >");
        actionLabel.getStyleClass().add("card-action");
        card.getChildren().addAll(cardNumber, cardTitle, cardDescription, spacer, actionLabel);
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
