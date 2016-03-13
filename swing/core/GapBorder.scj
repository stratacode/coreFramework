import javax.swing.border.AbstractBorder;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * A border with skips a pre-defined space or gap where the title would go.  You can put arbitrary components into this space then but
 * it looks like a normal swing titled border.
 */
public class GapBorder extends AbstractBorder {
   protected Border border;
   public int gapX;
   public int gapW;
   public int ascent = 20;
   public int descent = 4;

   static protected final int EDGE_SPACING = 2;

   static protected final int TEXT_SPACING = 2;

   protected Font   titleFont;

   public GapBorder() {
   }

   public GapBorder(Border border, int gx, int gw) {
      this.border = border;
      this.gapX = gx;
      this.gapW = gw;
   }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Border border = getBorder();

        Point textLoc = new Point();

        Rectangle grooveRect = new Rectangle(x + EDGE_SPACING, y + EDGE_SPACING,
                                             width - (EDGE_SPACING * 2),
                                             height - (EDGE_SPACING * 2));
        Font font = g.getFont();
        Color color = g.getColor();

        g.setFont(getFont(c));

        JComponent jc = (c instanceof JComponent) ? (JComponent)c : null;
        int         fontHeight = ascent + descent;
        int         diff;
        Insets      insets;

        if (border != null) {
            insets = border.getBorderInsets(c);
        } else {
            insets = new Insets(0, 0, 0, 0);
        }

        diff = Math.max(0, ((ascent/2) + TEXT_SPACING) - EDGE_SPACING);
        grooveRect.y += diff;
        grooveRect.height -= diff;
        textLoc.y = (grooveRect.y - descent) + (insets.top + ascent + descent)/2;

        textLoc.x = grooveRect.x + gapX;

        // If title is positioned in middle of border AND its fontsize
        // is greater than the border's thickness, we'll need to paint
        // the border in sections to leave space for the component's background
        // to show through the title.
        //
        if (border != null) {
           if ((grooveRect.y > textLoc.y - ascent)) {
               Rectangle clipRect = new Rectangle();

               // save original clip
               Rectangle saveClip = g.getClipBounds();

                // paint strip left of text
                clipRect.setBounds(saveClip);
                if (computeIntersection(clipRect, x, y, textLoc.x-1-x, height)) {
                    g.setClip(clipRect);
                    border.paintBorder(c, g, grooveRect.x, grooveRect.y, grooveRect.width, grooveRect.height);
                }

                // paint strip right of text
                clipRect.setBounds(saveClip);
                if (computeIntersection(clipRect, textLoc.x+gapW+1, y, x+width-(textLoc.x+gapW+1), height)) {
                    g.setClip(clipRect);
                    border.paintBorder(c, g, grooveRect.x, grooveRect.y, grooveRect.width, grooveRect.height);
                }

                 // paint strip below text
                 clipRect.setBounds(saveClip);
                 if (computeIntersection(clipRect, gapX-1, textLoc.y+descent, gapW+2, y+height-textLoc.y-descent)) {
                     g.setClip(clipRect);
                     border.paintBorder(c, g, grooveRect.x, grooveRect.y, grooveRect.width, grooveRect.height);
                 }

                // restore clip
                g.setClip(saveClip);

            } else {
                border.paintBorder(c, g, grooveRect.x, grooveRect.y,
                                  grooveRect.width, grooveRect.height);
            }
        }

        g.setFont(font);
        g.setColor(color);
    }

    /**
     * Reinitialize the insets parameter with this Border's current Insets.
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        FontMetrics fm;

        Border border = getBorder();
        if (border != null) {
            if (border instanceof AbstractBorder) {
                ((AbstractBorder)border).getBorderInsets(c, insets);
            } else {
                // Can't reuse border insets because the Border interface
                // can't be enhanced.
                Insets i = border.getBorderInsets(c);
                insets.top = i.top;
                insets.right = i.right;
                insets.bottom = i.bottom;
                insets.left = i.left;
            }
        } else {
            insets.left = insets.top = insets.right = insets.bottom = 0;
        }

        insets.left += EDGE_SPACING + TEXT_SPACING;
        insets.right += EDGE_SPACING + TEXT_SPACING;
        insets.top += EDGE_SPACING + TEXT_SPACING;
        insets.bottom += EDGE_SPACING + TEXT_SPACING;

        if (c == null)    {
            return insets;
        }

        Font font = getFont(c);

        fm = c.getFontMetrics(font);

        insets.top += ascent + descent;
        return insets;
    }

    public boolean isBorderOpaque() { return false; }

    public Border getBorder()       {
        Border b = border;
        if (b == null)
            b = UIManager.getBorder("TitledBorder.border");
        return b;
    }

    public Font getTitleFont()      {
        Font f = titleFont;
        if (f == null)
            f = UIManager.getFont("TitledBorder.font");
        return f;
    }

    public void setBorder(Border border)    {
        this.border = border;
    }

    /**
     * Sets the title-font of the titled border.
     * @param titleFont the font for the border title
     */
    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
    }

    /**
     * Returns the minimum dimensions this border requires
     * in order to fully display the border and title.
     * @param c the component where this border will be drawn
     */
    public Dimension getMinimumSize(Component c) {
        Insets insets = getBorderInsets(c);
        Dimension minSize = new Dimension(insets.right+insets.left,
                                          insets.top+insets.bottom);
        Font font = getFont(c);
        FontMetrics fm = c.getFontMetrics(font);
        JComponent jc = (c instanceof JComponent) ? (JComponent)c : null;
        minSize.width += gapW;
        return minSize;
    }

    protected Font getFont(Component c) {
        Font font;
        if ((font = getTitleFont()) != null) {
            return font;
        } else if (c != null && (font = c.getFont()) != null) {
            return font;
        }
        return new Font(Font.DIALOG, Font.PLAIN, ascent + descent);
    }

    private static boolean computeIntersection(Rectangle dest,
                                               int rx, int ry, int rw, int rh) {
        int x1 = Math.max(rx, dest.x);
        int x2 = Math.min(rx + rw, dest.x + dest.width);
        int y1 = Math.max(ry, dest.y);
        int y2 = Math.min(ry + rh, dest.y + dest.height);
        dest.x = x1;
        dest.y = y1;
        dest.width = x2 - x1;
        dest.height = y2 - y1;

        if (dest.width <= 0 || dest.height <= 0) {
            return false;
        }
        return true;
    }
}
