package cz.muni.fi.network;

import java.awt.*;
import javax.swing.*;


/**
 * Created by MiHu on 2.12.2016.
 */
public class Graph extends JPanel {
    double[] data;
    final int PAD = 20;
    int xLength;

    public Graph(double[] data, int xLength) {
        this.data = data;
        this.xLength = xLength;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.drawLine(PAD, PAD, PAD, h - PAD);
        g2.drawLine(PAD, h - PAD, w - PAD, h - PAD);
        double xScale = ((double)(w - 2 * PAD)) / (double)(xLength + 1);
        double maxValue = data[0];
        double yScale = (h - 2 * PAD) / maxValue;
        // The origin location.
        int x0 = PAD;
        int y0 = h - PAD;
        g2.setPaint(Color.MAGENTA);
        for (int j = 0; j < data.length; j++) {
            int x = x0 + (int) Math.round(xScale * (j + 1));
            int y = y0 - (int) Math.round(yScale * data[j]);
            g2.fillOval(x - 1, y - 1, 3, 3);
        }
    }

}
