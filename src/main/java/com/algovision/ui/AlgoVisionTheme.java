package com.algovision.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public final class AlgoVisionTheme {
    public static final Color APP_BACKGROUND = new Color(0x0b1020);
    public static final Color SIDEBAR_BACKGROUND = new Color(0x0e1528);
    public static final Color PANEL_BACKGROUND = new Color(0x10182b);
    public static final Color CARD_BACKGROUND = new Color(0x121b30);
    public static final Color SURFACE_BACKGROUND = new Color(0x0d1426);
    public static final Color CONTROL_BACKGROUND = new Color(0x19243a);
    public static final Color BORDER = new Color(0x202a40);
    public static final Color BORDER_SUBTLE = new Color(0x293650);
    public static final Color PRIMARY = new Color(0x6d68f5);
    public static final Color PRIMARY_HOVER = new Color(0x7972ff);
    public static final Color PRIMARY_SELECTED = new Color(0x242654);
    public static final Color SECONDARY = new Color(0x1a253b);
    public static final Color SECONDARY_HOVER = new Color(0x26334c);
    public static final Color ACCENT = new Color(0x8d89ff);
    public static final Color TEXT_PRIMARY = new Color(0xf3f5ff);
    public static final Color TEXT_BODY = new Color(0xaab5cb);
    public static final Color TEXT_MUTED = new Color(0x8290a9);
    public static final Color TEXT_SUBTLE = new Color(0x56617a);
    public static final Color SUCCESS = new Color(0x69d2a4);
    public static final Color WARNING = new Color(0xffcd86);
    public static final Color ERROR = new Color(0xff9eaa);
    public static final int BUTTON_HEIGHT = 36;

    private AlgoVisionTheme() {
    }

    public static void configureDefaults() {
        UIManager.put("Button.focus", APP_BACKGROUND);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("ComboBoxUI", "javax.swing.plaf.basic.BasicComboBoxUI");
        UIManager.put("ComboBox.background", SURFACE_BACKGROUND);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", PRIMARY_SELECTED);
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.buttonBackground", CONTROL_BACKGROUND);
        UIManager.put("ComboBox.buttonArrowColor", TEXT_BODY);
        UIManager.put("TextField.background", CONTROL_BACKGROUND);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", SURFACE_BACKGROUND);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.caretForeground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.background", PANEL_BACKGROUND);
        UIManager.put("TabbedPane.foreground", TEXT_BODY);
        UIManager.put("TabbedPane.selected", PRIMARY_SELECTED);
        UIManager.put("TabbedPane.contentAreaColor", PANEL_BACKGROUND);
        UIManager.put("TabbedPane.focus", PRIMARY);
        UIManager.put("List.background", SURFACE_BACKGROUND);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", PRIMARY_SELECTED);
        UIManager.put("List.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ProgressBar.background", CONTROL_BACKGROUND);
        UIManager.put("ProgressBar.foreground", PRIMARY);
        UIManager.put("Slider.background", PANEL_BACKGROUND);
        UIManager.put("Slider.foreground", PRIMARY);
        UIManager.put("ScrollPane.background", APP_BACKGROUND);
        UIManager.put("ScrollBar.thumb", new Color(0x303b54));
        UIManager.put("ScrollBar.track", APP_BACKGROUND);
    }

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static void styleButton(JButton button, boolean primary) {
        styleButton(button, primary, 96);
    }

    public static void styleButton(JButton button, boolean primary, int minimumWidth) {
        Color normal = primary ? PRIMARY : SECONDARY;
        Color hover = primary ? PRIMARY_HOVER : SECONDARY_HOVER;
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(normal);
        button.setFont(font(Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? PRIMARY_HOVER : BORDER_SUBTLE),
                new EmptyBorder(8, 12, 8, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Dimension preferred = button.getPreferredSize();
        button.setPreferredSize(new Dimension(Math.max(minimumWidth, preferred.width),
                BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(Math.min(minimumWidth, 84), BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, BUTTON_HEIGHT));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(normal);
            }
        });
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setOpaque(true);
        comboBox.setBackground(SURFACE_BACKGROUND);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));
    }

    public static void styleToggleButton(JToggleButton button) {
        button.setForeground(TEXT_BODY);
        button.setBackground(SECONDARY);
        button.setFont(font(Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE),
                new EmptyBorder(7, 3, 7, 3)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
