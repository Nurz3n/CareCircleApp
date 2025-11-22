package com.carecircleclient;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

/** Loads icons from classpath (/icons/...) or filesystem (src/icons/... or ./icons/...). HQ downscale to avoid blur. */
public final class Icons {
    public static final int DEFAULT_LABEL_ICON_SIZE  = 24;
    public static final int DEFAULT_BUTTON_ICON_SIZE = 24;

    public static Icon icon(String baseName, String glyphFallback, int size) {
        Icon png = loadPngByBase(baseName, size);
        return png != null ? png : glyph(glyphFallback == null ? "•" : glyphFallback, size);
    }
    public static Icon icon(String baseName, String glyphFallback) {
        return icon(baseName, glyphFallback, DEFAULT_LABEL_ICON_SIZE);
    }

    public static Icon iconFile(String filePath, int size) {
        File f = new File(filePath);
        if (f.exists()) return scaleHQ(new ImageIcon(f.getAbsolutePath()).getImage(), size);
        return null;
    }

    // ----- internals -----
    private static Icon loadPngByBase(String baseName, int size) {
        URL url = Icons.class.getResource("/icons/" + baseName + ".png");
        if (url != null) return scaleHQ(new ImageIcon(url).getImage(), size);

        File f1 = new File("src/icons/" + baseName + ".png");
        if (f1.exists()) return scaleHQ(new ImageIcon(f1.getAbsolutePath()).getImage(), size);

        File f2 = new File("icons/" + baseName + ".png");
        if (f2.exists()) return scaleHQ(new ImageIcon(f2.getAbsolutePath()).getImage(), size);

        return null;
    }

    /** High-quality scale to a square size. */
    private static Icon scaleHQ(Image src, int size) {
        int w = size, h = size;
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return new ImageIcon(dst);
    }

    private static Icon glyph(String text, int sizePx) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Font base = c.getFont() != null ? c.getFont() : UIManager.getFont("Label.font");
                g2.setFont(base.deriveFont(Font.PLAIN, Math.max(12f, sizePx)));
                FontMetrics fm = g2.getFontMetrics();
                int ty = y + (sizePx - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, ty);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return sizePx; }
            @Override public int getIconHeight() { return sizePx; }
        };
    }

    private Icons() {}
}
