package ml.jammy.msicksty;

import java.awt.*;

public class Righthook {
    public int x1;
    public int y1;
    public int w;
    public int h;

    public Righthook(int x1, int y1, int w, int h) {
        this.x1 = x1;
        this.y1 = y1;
        this.w = w;
        this.h = h;
    }

    public void draw(final Graphics g) {
        g.setColor(new Color(255, 10, 10, 200));
        ((Graphics2D) g).setStroke(new BasicStroke(3));
        g.drawRect(x1, y1, w, h);
    }
}
