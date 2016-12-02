package cz.muni.fi.network;

import java.awt.*;
import javax.swing.*;


/**
 * Created by MiHu on 2.12.2016.
 */
public class Graph extends JPanel {
    double[] data = {2.5, 6.0, 4.2, 7.5, 1.1,1.4,1.5,1.7,1.8,1.9,2.0,2.1,2.2,2.3,1.4};
    final int PAD = 20;

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.drawLine(PAD, PAD, PAD, h - PAD);
        g2.drawLine(PAD, h - PAD, w - PAD, h - PAD);
        double xScale = (w - 2 * PAD) / (data.length + 1);
        double maxValue = 100.0;
        double yScale = (h - 2 * PAD) / maxValue;
        // The origin location.
        int x0 = PAD;
        int y0 = h - PAD;
        g2.setPaint(Color.red);
        for (int j = 0; j < data.length; j++) {
            int x = x0 + (int) (xScale * (j + 1));
            int y = y0 - (int) (yScale * data[j]);
            g2.fillOval(x - 2, y - 2, 4, 4);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(new Graph());
        f.setSize(800, 800);
        f.setLocation(200, 200);
        f.setVisible(true);
    }
}
