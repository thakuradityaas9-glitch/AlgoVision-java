package com.algovision.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public final class SyntaxCodeEditor extends JPanel {
    private static final Color KEYWORD = new Color(0xc5a5ff);
    private static final Color STRING = new Color(0xa6d189);
    private static final Color COMMENT = new Color(0x71819d);
    private static final Color NUMBER = new Color(0xffc777);
    private static final Color PROBLEM_LINE = new Color(0x542a3a);
    private static final Pattern STRINGS =
            Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");
    private static final Pattern NUMBERS = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern CPP_JAVA_COMMENTS =
            Pattern.compile("//.*$|/\\*[\\s\\S]*?\\*/", Pattern.MULTILINE);
    private static final Pattern PYTHON_COMMENTS = Pattern.compile("#.*$",
            Pattern.MULTILINE);

    private final JTextPane editor = new JTextPane();
    private final LineNumberGutter gutter = new LineNumberGutter();
    private final Set<Integer> problemLines = new TreeSet<>();
    private boolean highlightQueued;
    private String language = "C++";

    public SyntaxCodeEditor(boolean editable) {
        setLayout(new java.awt.BorderLayout());
        setOpaque(true);
        setBackground(AlgoVisionTheme.SURFACE_BACKGROUND);
        editor.setName("codeEditor");
        editor.setFont(new Font("Consolas", Font.PLAIN, 13));
        editor.setForeground(AlgoVisionTheme.TEXT_PRIMARY);
        editor.setCaretColor(AlgoVisionTheme.TEXT_PRIMARY);
        editor.setSelectionColor(AlgoVisionTheme.PRIMARY_SELECTED);
        editor.setBackground(AlgoVisionTheme.SURFACE_BACKGROUND);
        // JTextPane has no setTabSize API.  The document property is the Swing-supported
        // equivalent used when tab stops are laid out.
        editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
        editor.setEditable(editable);
        editor.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setName("codeEditorScroll");
        scroll.setBorder(BorderFactory.createLineBorder(AlgoVisionTheme.BORDER));
        scroll.getViewport().setBackground(AlgoVisionTheme.SURFACE_BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        gutter.setOpaque(true);
        gutter.setBackground(AlgoVisionTheme.PANEL_BACKGROUND);
        scroll.setRowHeaderView(gutter);
        add(scroll, java.awt.BorderLayout.CENTER);

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                documentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                documentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                gutter.revalidate();
                gutter.repaint();
            }
        });
        setToolTipText("Edit C++, Java, or Python source code.");
        scheduleHighlight();
    }

    public JTextPane editor() {
        return editor;
    }

    public void setLanguage(String selectedLanguage) {
        language = selectedLanguage == null ? "" : selectedLanguage;
        scheduleHighlight();
    }

    public void setProblemLines(Set<Integer> lines) {
        problemLines.clear();
        if (lines != null) {
            lines.stream().filter(line -> line != null && line > 0)
                    .forEach(problemLines::add);
        }
        editor.getHighlighter().removeAllHighlights();
        var root = editor.getDocument().getDefaultRootElement();
        for (int line : problemLines) {
            if (line > root.getElementCount()) {
                continue;
            }
            var element = root.getElement(line - 1);
            int start = element.getStartOffset();
            int end = Math.min(element.getEndOffset(), editor.getDocument().getLength());
            if (end > start) {
                try {
                    editor.getHighlighter().addHighlight(start, end,
                            new DefaultHighlighter.DefaultHighlightPainter(PROBLEM_LINE));
                } catch (BadLocationException exception) {
                    throw new IllegalStateException("Could not highlight a reported code line.", exception);
                }
            }
        }
        gutter.revalidate();
        gutter.repaint();
    }

    public Set<Integer> problemLines() {
        return Set.copyOf(problemLines);
    }

    private void documentChanged() {
        gutter.revalidate();
        gutter.repaint();
        scheduleHighlight();
    }

    private void scheduleHighlight() {
        if (highlightQueued) {
            return;
        }
        highlightQueued = true;
        SwingUtilities.invokeLater(() -> {
            highlightQueued = false;
            applySyntaxHighlighting();
        });
    }

    private void applySyntaxHighlighting() {
        StyledDocument document = editor.getStyledDocument();
        int length = document.getLength();
        if (length == 0) {
            return;
        }
        SimpleAttributeSet base = new SimpleAttributeSet();
        StyleConstants.setForeground(base, AlgoVisionTheme.TEXT_PRIMARY);
        StyleConstants.setBold(base, false);
        document.setCharacterAttributes(0, length, base, true);

        SimpleAttributeSet keywordStyle = style(KEYWORD, true);
        Matcher keywords = Pattern.compile(keywordPattern(language)).matcher(editor.getText());
        applyMatches(document, keywords, keywordStyle);
        applyMatches(document, NUMBERS.matcher(editor.getText()), style(NUMBER, false));
        Pattern comments = "Python".equals(language) ? PYTHON_COMMENTS : CPP_JAVA_COMMENTS;
        applyMatches(document, comments.matcher(editor.getText()), style(COMMENT, false));
        applyMatches(document, STRINGS.matcher(editor.getText()), style(STRING, false));
    }

    private String keywordPattern(String selectedLanguage) {
        String keywords = switch (selectedLanguage) {
            case "Java" -> "abstract|assert|boolean|break|byte|case|catch|char|class|"
                    + "const|continue|default|do|double|else|enum|extends|final|finally|"
                    + "float|for|if|implements|import|instanceof|int|interface|long|"
                    + "native|new|package|private|protected|public|return|short|static|"
                    + "strictfp|super|switch|synchronized|this|throw|throws|transient|"
                    + "try|void|volatile|while|var|record";
            case "Python" -> "and|as|assert|async|await|break|class|continue|def|del|"
                    + "elif|else|except|False|finally|for|from|global|if|import|in|"
                    + "is|lambda|nonlocal|not|or|pass|raise|return|True|try|while|with|yield";
            default -> "alignas|auto|bool|break|case|catch|char|class|const|constexpr|"
                    + "continue|default|delete|do|double|else|enum|explicit|extern|false|"
                    + "float|for|friend|if|inline|int|long|namespace|new|nullptr|private|"
                    + "protected|public|return|short|signed|sizeof|static|struct|switch|"
                    + "template|this|throw|true|try|typedef|typename|union|unsigned|using|"
                    + "virtual|void|volatile|while|include";
        };
        return "\\b(?:" + keywords + ")\\b";
    }

    private SimpleAttributeSet style(Color color, boolean bold) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        StyleConstants.setBold(attributes, bold);
        return attributes;
    }

    private void applyMatches(StyledDocument document, Matcher matcher,
            SimpleAttributeSet attributes) {
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(),
                    matcher.end() - matcher.start(), attributes, true);
        }
    }

    private final class LineNumberGutter extends JComponent {
        @Override
        public Dimension getPreferredSize() {
            int lines = editor.getDocument().getDefaultRootElement().getElementCount();
            FontMetrics metrics = editor.getFontMetrics(editor.getFont());
            int digits = Integer.toString(Math.max(1, lines)).length();
            int width = 13 + metrics.charWidth('8') * digits + (problemLines.isEmpty() ? 0 : 8);
            return new Dimension(width, editor.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            graphics.setFont(editor.getFont());
            FontMetrics metrics = graphics.getFontMetrics();
            int lineHeight = metrics.getHeight();
            int baseline = metrics.getAscent() + 5;
            int count = editor.getDocument().getDefaultRootElement().getElementCount();
            for (int line = 1; line <= count; line++) {
                int y = baseline + (line - 1) * lineHeight;
                if (y - metrics.getAscent() > getHeight()) {
                    break;
                }
                if (problemLines.contains(line)) {
                    graphics.setColor(AlgoVisionTheme.ERROR);
                    graphics.fillOval(4, y - metrics.getAscent() + 4, 5, 5);
                }
                graphics.setColor(problemLines.contains(line)
                        ? AlgoVisionTheme.ERROR : AlgoVisionTheme.TEXT_MUTED);
                String number = Integer.toString(line);
                graphics.drawString(number, getWidth() - metrics.stringWidth(number) - 6, y);
            }
            graphics.setColor(AlgoVisionTheme.BORDER);
            graphics.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        }
    }
}
