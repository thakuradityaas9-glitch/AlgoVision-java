# 🚀 AlgoVision

### Visualize. Understand. Master Algorithms.

**AlgoVision** is an interactive Java desktop application designed to make learning and understanding Data Structures and Algorithms more intuitive through real-time visualizations, algorithm comparisons, graph exploration, pathfinding, and AI-powered code analysis.

Instead of simply reading algorithm implementations, AlgoVision lets you see how algorithms execute, understand their behavior, compare their performance, and explore the logic behind them.

---

## ✨ Features

### 1. Sorting Visualizer
Explore popular sorting algorithms through interactive animations.

**Supported Algorithms:**
- Bubble Sort
- Selection Sort
- Insertion Sort
- Merge Sort
- Quick Sort

**Features:**
- Real-time sorting animations.
- Adjustable animation speed.
- Visual representation of comparisons and swaps.
- Start, pause, and reset controls.
- Understand how different algorithms manipulate data.

### 2. Graph Visualizer

An interactive environment to create, explore, and visualize graph algorithms.

**Supported Algorithms:**
- Breadth First Search (BFS)
- Depth First Search (DFS)
- Dijkstra's Shortest Path Algorithm

**Features:**
- Interactive graph creation.
- Add and manipulate graph nodes and edges.
- Generate random graphs.
- Visualize traversal sequences.
- Observe shortest-path exploration.
- Interactive controls and algorithm execution.

### 3. A* Pathfinding Visualizer

Understand how the A* search algorithm finds an optimal path between two points.

**Features:**
- Interactive grid-based environment.
- Place start and destination points.
- Create obstacles.
- Visualize the pathfinding process.
- Observe explored nodes and the final path.

### 4. Sorting Race 🏁

Compare two sorting algorithms running simultaneously.

**Features:**
- Execute two sorting algorithms side by side.
- Independent visualizations.
- Real-time comparison and swap statistics.
- Individual execution timers.
- Winner detection.
- Understand the practical differences between sorting algorithms.

### 5. AI Code Analyzer 🤖

An AI-powered coding assistant integrated into AlgoVision using the Groq API.

Submit your code and receive an AI-generated analysis covering:

- Time complexity analysis.
- Space complexity analysis.
- Potential bugs and logical errors.
- Edge cases.
- Code explanations.
- Optimization suggestions.
- Improved code suggestions.
- DSA interview questions and answers.

The goal is not just to identify whether code works, but to understand how it works and how it can be improved.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Core application development |
| Java Swing | Desktop graphical user interface |
| Java AWT | Graphics and rendering |
| Graphics2D | Algorithm visualizations |
| Swing Timer | Animation and event-driven execution |
| Maven | Dependency management and build automation |
| Java HttpClient | HTTP communication |
| Groq API | AI-powered code analysis |

**Note:** AlgoVision uses Java Swing for its desktop interface. It does not require JavaFX.

---

## 📂 Project Structure

```text
AlgoVision/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── algovision/
│   │               ├── MainApp.java
│   │               ├── algorithms/
│   │               ├── ai/
│   │               ├── controller/
│   │               ├── graph/
│   │               ├── pathfinding/
│   │               └── race/
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── algovision/
│
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

---

## ⚙️ Installation & Setup

### Prerequisites

Make sure you have installed:

- Java Development Kit (JDK) 21
- Apache Maven
- Git

### 1. Clone the repository

```bash
git clone https://github.com/thakuradityaas9-glitch/AlgoVision-java.git
```

### 2. Navigate into the project

```bash
cd AlgoVision-java
```

### 3. Configure the API key

Create a `.env` file in the project root.

Add your Groq API key:

```env
GROQ_API_KEY=your_groq_api_key_here
```

You can obtain an API key from the Groq developer platform.

**Important:** Never commit your `.env` file or expose your API key publicly.

### 4. Build the project

```bash
mvn clean package
```

### 5. Run AlgoVision

```bash
mvn exec:java
```

If the Maven execution plugin requires an explicit main class:

```bash
mvn exec:java "-Dexec.mainClass=com.algovision.MainApp"
```

---

## 🧪 Running Tests

Run the complete test suite:

```bash
mvn clean test
```

Build the application:

```bash
mvn clean package
```

---

## 🧠 What Makes AlgoVision Different?

AlgoVision brings multiple DSA learning experiences into one desktop application.

Rather than treating algorithms as isolated code snippets, it focuses on interactive learning:

- **Visual Learning:** Watch algorithms execute instead of only reading their implementations.
- **Algorithm Comparison:** Understand differences through simultaneous execution.
- **Graph Exploration:** Experiment with traversal and shortest-path algorithms.
- **Pathfinding:** Understand how search algorithms explore a problem space.
- **AI Assistance:** Get explanations, complexity analysis, debugging insights, and optimization suggestions.

---

## 🔮 Future Scope

Potential future improvements include:

- More sorting algorithms.
- Additional graph algorithms such as Kruskal's and Prim's.
- Dynamic Programming visualizations.
- Data structure visualizations.
- More advanced algorithm comparisons.
- Expanded AI-powered DSA assistance.
- Custom input and dataset support.

---

## 📜 License

This project is currently maintained as a private repository.

---

### ⭐ AlgoVision

**Don't just learn algorithms. Visualize them. Understand them. Master them.**
