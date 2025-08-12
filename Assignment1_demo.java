import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class Assignment1_demo extends JPanel implements ActionListener {

    private Timer timer;
    private double t = 0; // time counter

    public Assignment1_demo() {
        timer = new Timer(33, this); // ~30 FPS
        timer.start();
    }

    // Midpoint Circle Algorithm
    private void drawMidpointCircle(Graphics2D g, int xc, int yc, int r) {
        int x = 0;
        int y = r;
        int p = 1 - r;

        while (x <= y) {
            g.fillRect(xc + x, yc + y, 1, 1);
            g.fillRect(xc - x, yc + y, 1, 1);
            g.fillRect(xc + x, yc - y, 1, 1);
            g.fillRect(xc - x, yc - y, 1, 1);
            g.fillRect(xc + y, yc + x, 1, 1);
            g.fillRect(xc - y, yc + x, 1, 1);
            g.fillRect(xc + y, yc - x, 1, 1);
            g.fillRect(xc - y, yc - x, 1, 1);

            if (p < 0) {
                p += 2 * x + 3;
            } else {
                p += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }
    }

    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double seconds = t / 30.0;

        // Background gradient
        GradientPaint gp = new GradientPaint(0, 0, Color.CYAN, getWidth(), getHeight(), Color.MAGENTA);
        g.setPaint(gp);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Stage 1: zoom to face
        if (seconds < 1.5) {
            g.setColor(Color.WHITE);
            AffineTransform at = g.getTransform();
            double scale = 1 + seconds; // zoom
            g.translate(getWidth() / 2, getHeight() / 2);
            g.scale(scale, scale);

            // Head
            g.setColor(Color.PINK);
            drawMidpointCircle(g, 0, 0, 50);

            // Eyes
            g.setColor(Color.BLACK);
            drawMidpointCircle(g, -20, -10, 5);
            drawMidpointCircle(g, 20, -10, 5);

            // Mouth (curve)
            g.setStroke(new BasicStroke(2));
            QuadCurve2D mouth = new QuadCurve2D.Float(-20, 20, 0, 35, 20, 20);
            g.draw(mouth);

            g.setTransform(at);
        }

        // Stage 2: dramatic pose
        else if (seconds < 4) {
            AffineTransform at = g.getTransform();
            g.translate(getWidth() / 2, getHeight() / 2);
            g.rotate(Math.sin(seconds) * 0.1); // slight sway

            // Body
            g.setColor(Color.PINK);
            drawMidpointCircle(g, 0, -60, 40); // head
            g.setColor(Color.BLUE);
            g.fillRect(-20, -20, 40, 80); // body

            // Arms (lines)
            g.setStroke(new BasicStroke(5));
            g.drawLine(-20, -10, -80, 20);
            g.drawLine(20, -10, 80, 20);

            // Legs (curves)
            g.setStroke(new BasicStroke(5));
            QuadCurve2D leftLeg = new QuadCurve2D.Float(-10, 60, -30, 100, -20, 140);
            QuadCurve2D rightLeg = new QuadCurve2D.Float(10, 60, 30, 100, 20, 140);
            g.draw(leftLeg);
            g.draw(rightLeg);

            g.setTransform(at);
        }

        // Stage 3: logo
        else {
            g.setFont(new Font("SansSerif", Font.BOLD, 40));
            g.setColor(Color.WHITE);
            String text = "WHAT IF I REBORNED?";
            FontMetrics fm = g.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = getHeight() / 2;
            g.drawString(text, tx, ty);

            // Flare ellipse
            g.setColor(new Color(255, 255, 0, 100));
            g.fillOval(tx - 50, ty - 50, fm.stringWidth(text) + 100, 100);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        t++;
        if (t > 150) t = 0; // loop every 5 sec
        repaint();
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Anime Intro Demo");
        Assignment1_demo panel = new Assignment1_demo();
        f.add(panel);
        f.setSize(600, 600);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
