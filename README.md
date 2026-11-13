# AlgoVision

AlgoVision is a Java desktop application for learning data structures and algorithms through interactive visualizations and AI-assisted code review. The dark-themed Swing application includes the dashboard, sorting visualizer, graph visualizer, A* pathfinding, algorithm race, and AI Code Analyzer workspaces.

## Migration status

The dashboard and all five workspaces run through the Swing entry point. The project builds with Java 21 and uses Swing for its desktop UI.

## Features

### Sorting Visualizer

Visualize these sorting algorithms using a Swing `Graphics2D` bar panel and timer-driven animation:

- Bubble Sort
- Selection Sort
- Insertion Sort
- Merge Sort
- Quick Sort

Enter up to 50 comma-separated integers or generate a random array. Start, pause, resume, reset, and adjust animation speed. The workspace reports comparisons, swaps or moves, array size, execution status, and algorithm time and space complexity.

### Algorithm Race Mode

Compare two different supported sorting algorithms on identical copies of the same random or comma-separated input. The Swing workspace prepares both runs concurrently off the Event Dispatch Thread, then advances each lane with its own Swing timer. Comparisons, swaps, moves, total operations, execution time, and completion status are tracked independently; the winner is reported only after both lanes finish. Reset or leaving the workspace cancels preparation or stops playback, and pause/resume preserves each lane's current step. Execution time measures step generation and recording before playback, excluding animation, so a single race is not a universal algorithm ranking.

### Graph Visualizer

Build and explore undirected weighted graphs:

- Use explicit Select / Move, Add Node, and Add Edge canvas modes.
- Drag nodes to reposition them; choose two nodes in Add Edge mode to connect them.
- Select an edge to update its weight; edge weights appear on the canvas.
- Delete selected nodes or edges, clear the graph, or generate a random connected graph with unique edges and non-negative weights.
- Run Breadth-First Search (BFS), Depth-First Search (DFS), or Dijkstra's shortest-path algorithm.

Select source and destination nodes, then start, pause, resume, or reset playback and adjust its speed. The workspace shows the current operation, traversal order and frontier, visited/examined counts, Dijkstra distances, and the final shortest path. Zero-weight manual edges are supported; negative weights are rejected.

The Swing graph canvas paints immutable visualization snapshots with `Graphics2D`; mouse interaction is handled separately from rendering, and BFS, DFS, and Dijkstra reuse the existing graph model and algorithm step snapshots. Random graphs use a connected spanning tree plus a bounded set of unique additional edges.

### AI Code Analyzer

The Swing AI Code Analyzer sends C++ or Java DSA source code asynchronously to Groq's OpenAI-compatible Chat Completions API. Scrollable result tabs show time and space complexity, bugs and edge cases, code review, optimization suggestions, optimized code, interview questions, and code explanation. The optimized code can be copied to the clipboard. The analyzer requires an internet connection and the existing Groq API configuration.

The analyzer reads `GROQ_API_KEY` from `.env` in the project root. Copy `.env.example` to `.env` and replace its placeholder with your key. The `.env` file is ignored by Git. Keep credentials out of source control; move API communication behind a secured backend before distributing the app.

### A* Pathfinding Visualizer

Use the Pathfinding sidebar workspace to place a start and destination, draw or erase walls with click-and-drag, generate random obstacles, and animate A* on a four-directional grid. The display distinguishes the open frontier, explored cells, the current cell, evaluated neighbors, and the final shortest path. Start, pause, resume, reset, and speed controls are available alongside visited-cell, explored-node, path-length, and execution-status statistics.

A* uses unit movement costs, a priority queue ordered by `f(n) = g(n) + h(n)`, and Manhattan distance as its heuristic. The existing algorithm, grid model, and immutable visualization-step snapshots are reused by the Swing workspace. Search-step generation runs off the Event Dispatch Thread, and a single Swing timer drives rendering.

## Requirements

- Java Development Kit (JDK) 21
- Maven

The application uses Swing and the Java 21 standard library for its desktop UI and HTTP client.

## Build and run

From the project directory:

```shell
mvn clean compile
mvn exec:java
```

## Project structure

```text
src/
  main/
    java/com/algovision/
      algorithms/       Sorting algorithm implementations
      ai/               Groq configuration, API service, and analysis response model
      controller/       Sorting, graph, pathfinding, race, and analyzer controllers
      graph/algorithms/ BFS, DFS, and Dijkstra implementations
      graph/model/      Graph, edge, node, and visualization-step models
      model/            Sorting visualization models
      pathfinding/      A* algorithm, grid, and step models
      race/             Sorting race results and execution timing
      visualization/    Sorting bars, graph canvas, and pathfinding grid
      MainApp.java      Swing application entry point, dashboard, and navigation
pom.xml                 Maven configuration
```

## AI Code Analyzer

The Groq integration preserves its configured endpoint, model, request format, response parsing, and prompts. Requests use `HttpClient.sendAsync()`; responses and errors update Swing components on the Event Dispatch Thread.
