package ml.jammy.msicksty;

import java.awt.*;

public class Arrow {
    public int x1;
    public int x2;
    public int y1;
    public int y2;

    public Arrow(int x, int y) {
        this.x1 = this.x2 = x;
        this.y1 = this.y2 = y;
    }

    public void draw(final Graphics g) {
        g.setColor(new Color(255, 10, 10, 200));
        ((Graphics2D) g).setStroke(new BasicStroke(3));
        g.drawLine(x1, y1, x2, y2);

        final double initAngle = Math.atan2(y1 - y2, x1 - x2);
        lineByAngle(g, x2, y2, (initAngle + Math.toRadians(30)) % (2 * Math.PI), 10);
        lineByAngle(g, x2, y2, (initAngle - Math.toRadians(30)) % (2 * Math.PI), 10);
    }

    protected void lineByAngle(final Graphics g, int x, int y, final double angle, final int l) {
        g.drawLine(x, y, x + (int) (Math.cos(angle) * l), y + (int) (Math.sin(angle) * l));
    }
}
