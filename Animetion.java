import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

import javax.swing.*;

/**
 * animation.java
 * แอนิเมชัน 600x600 — stickman โดนรถชนแล้วลืมตาเป็นสไลม์
 * - ใช้ Java2D API + midpoint ellipse & circle algorithms
 * - มี motion blur รถตอนวิ่งชน
 * - ความยาวแอนิเมชัน 8 วินาที
 */
public class Animetion {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("WHAT IF I REBORNED — stickman isekai slime vibe");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            RebornPanel panel = new RebornPanel(600, 600);
            f.setContentPane(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            panel.start();
        });
    }
}

class RebornPanel extends JPanel {
    private final int W, H;
    private Timer timer;
    private long startTime;
    private final int FPS = 60;
    private final int DURATION_MS = 8000; // 8 วินาที
    private MidpointEllipse midpointEllipse = new MidpointEllipse();
    private MidpointCircle midpointCircle = new MidpointCircle();

    public RebornPanel(int w, int h) {
        this.W = w;
        this.H = h;
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(10, 12, 20));
    }

    public void start() {
        startTime = System.currentTimeMillis();
        timer = new Timer(1000 / FPS, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > DURATION_MS) {
                startTime = System.currentTimeMillis(); // loop
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        long t = System.currentTimeMillis() - startTime;
        double tt = t / (double) DURATION_MS;

        // แบ่งเป็น 3 ฉากตามเวลา
        if (tt < 0.25) {
            paintScene1(g, tt / 0.25); // 0..1 in first 2 seconds
        } else if (tt < 0.375) {
            paintScene2(g, (tt - 0.25) / 0.125); // fade out 2-3s
        } else {
            paintScene3(g, (tt - 0.375) / 0.625); // slime scene 3-8s
        }

        g.dispose();
    }

    // ฉาก 1: stickman เดิน+รถวิ่งชน + motion blur
    private void paintScene1(Graphics2D g, double progress) {
        // background
        GradientPaint bg = new GradientPaint(0, 0, new Color(20, 20, 40), 0, H, new Color(5, 5, 15));
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);

        int groundY = H - 100;

        // ground line
        g.setColor(new Color(30, 80, 50));
        g.fillRect(0, groundY, W, H - groundY);

        // stickman position: เดินจากซ้ายกลางจอ (x from 100 to 280)
        int stickmanX = 100 + (int) (180 * progress);
        int stickmanY = groundY;

        drawStickman(g, stickmanX, stickmanY);

        // รถวิ่งจากขวาเข้ามา (x from 650 to 280)
        int carStartX = W + 50;
        int carEndX = stickmanX + 15; // ใกล้ stickman เลย
        int carX = (int) (carStartX + (carEndX - carStartX) * progress);

        // motion blur effect รถ: วาดรถซ้อน 5 ชั้นโปร่งใสลดหลั่น
       for (int i = 0; i < 5; i++) {
            float alpha = 0.15f * (5 - i);
            int blurX = carX + i * 15;  // blur ขยับเยอะขึ้นให้ชัดเจน
            drawTruck(g, blurX, groundY - 20, alpha);
        }
    }

    // ฉาก 2: fade to black (หน้าจอดำ)
    private void paintScene2(Graphics2D g, double progress) {
        // สีดำ fade in
        g.setColor(new Color(0, 0, 0, (int) (255 * progress)));
        g.fillRect(0, 0, W, H);
    }

    // ฉาก 3: ลืมตาเป็นสไลม์
    private void paintScene3(Graphics2D g, double progress) {
        // ค่อย ๆ fade in background เป็นโทนสีน้ำทะเลเขียว
        Color bgStart = new Color(5, 15, 10);
        Color bgEnd = new Color(20, 80, 60);
        Color bgColor = blendColors(bgStart, bgEnd, progress);
        g.setColor(bgColor);
        g.fillRect(0, 0, W, H);

        int cx = W / 2;
        int cy = H / 2 + 30;

        // animate slime ellipse (pulse + ปรับขนาดตาม easing)
        double baseRx = 130;
        double baseRy = 110;
        double pulse = 0.1 * Math.sin(progress * Math.PI * 6);
        double rx = baseRx * (1 + pulse + 0.3 * easeOutBounce(progress));
        double ry = baseRy * (1 - pulse + 0.15 * easeOutQuad(progress));

        // สร้าง path ellipse ด้วย midpoint ellipse algorithm
        GeneralPath slime = midpointEllipse.createEllipsePath(cx, cy, (int) rx, (int) ry);

        // gradient fill slime
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Double(cx - rx * 0.2, cy - ry * 0.3),
                (float) (Math.max(rx, ry) * 1.2f),
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(110, 210, 190, 255), new Color(70, 180, 140, 200), new Color(20, 50, 30, 160)}
        );
        g.setPaint(paint);
        g.fill(slime);

        // ขอบสไลม์
        g.setStroke(new BasicStroke(4f));
        g.setColor(new Color(180, 255, 220, 180));
        g.draw(slime);

        // ตา (ellipse + circle highlight)
        double ex = rx * 0.4;
        double ey = ry * 0.15;
        double eyeW = rx * 0.26;
        double eyeH = ry * 0.2;
        double offsetY = -ry * 0.18;

        // ตาขาว (ellipse)
        GeneralPath leftEye = midpointEllipse.createEllipsePath(cx - (int) ex, (int) (cy + offsetY), (int) eyeW, (int) eyeH);
        GeneralPath rightEye = midpointEllipse.createEllipsePath(cx + (int) ex, (int) (cy + offsetY), (int) eyeW, (int) eyeH);

        g.setColor(new Color(30, 40, 45));
        g.fill(leftEye);
        g.fill(rightEye);

        // ไฮไลท์ตา (ใช้ midpoint circle algorithm)
        int highlightR = 8 + (int) (4 * Math.sin(progress * Math.PI * 5));
        g.setColor(new Color(255, 255, 255, 200));
        midpointCircle.fillMidpointCircle(g, cx - (int) ex - (int) (eyeW * 0.15), (int) (cy + offsetY - eyeH * 0.15), highlightR);
        midpointCircle.fillMidpointCircle(g, cx + (int) ex - (int) (eyeW * 0.15), (int) (cy + offsetY - eyeH * 0.15), highlightR);

        // ปากโค้งยิ้ม (เส้นโค้ง)
        double mouthW = rx * (0.5 + 0.15 * Math.sin(progress * Math.PI * 4));
        double mouthH = ry * 0.18;
        QuadCurve2D mouth = new QuadCurve2D.Double(cx - mouthW / 2, cy + ry * 0.3,
                cx, cy + ry * 0.3 + mouthH,
                cx + mouthW / 2, cy + ry * 0.3);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(40, 70, 45));
        g.draw(mouth);

        // particle drops รอบ ๆ slime (ใช้ midpoint circle algorithm)
        Random rand = new Random(42);
        for (int i = 0; i < 15; i++) {
            double angle = i * (2 * Math.PI / 15) + progress * 6;
            int px = cx + (int) ((rx + 15) * Math.cos(angle));
            int py = cy + (int) ((ry + 15) * Math.sin(angle));
            int pr = 7 + (int) (3 * Math.sin(progress * 15 + i));
            int alpha = (int) (120 + 120 * Math.sin(progress * 15 + i));
            alpha = Math.max(0, Math.min(255, alpha));
            g.setColor(new Color(180, 250, 200, alpha));
            midpointCircle.fillMidpointCircle(g, px, py, pr);
        }
    }

    // ฟังก์ชันวาด stickman แบบเรียบง่าย
    private void drawStickman(Graphics2D g, int x, int groundY) {
        g.setStroke(new BasicStroke(4f));
        g.setColor(new Color(240, 230, 220));

        int headR = 20;
        int headX = x;
        int headY = groundY - 120;

        // หัวกลม
        g.fillOval(headX - headR, headY - headR, headR * 2, headR * 2);

        // ลำตัว
        g.drawLine(headX, headY + headR, headX, groundY - 40);

        // แขน
        g.drawLine(headX, headY + headR + 20, headX - 30, groundY - 80);
        g.drawLine(headX, headY + headR + 20, headX + 30, groundY - 80);

        // ขา
        g.drawLine(headX, groundY - 40, headX - 25, groundY);
        g.drawLine(headX, groundY - 40, headX + 25, groundY);
    }

    // ฟังก์ชันวาดรถแบบเรียบง่าย (มี motion blur alpha)
    private void drawTruck(Graphics2D g, int x, int y, float alpha) {
        Composite oldComp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // ตัวถังหลักของรถบรรทุก (ใหญ่ขึ้น)
        g.setColor(new Color(70, 130, 180)); // ฟ้าเข้ม
        g.fillRect(x, y - 70, 180, 60);

        // ห้องคนขับ (ใหญ่ขึ้น)
        g.setColor(new Color(100, 170, 210));
        g.fillRect(x + 130, y - 110, 50, 40);

        // กระจก
        g.setColor(new Color(200, 230, 250, 180));
        g.fillRect(x + 140, y - 105, 35, 30);

        // ล้อรถบรรทุก (ล้อใหญ่ 3 ล้อ)
        g.setColor(new Color(40, 40, 40));
        g.fillOval(x + 20, y - 30, 50, 50);
        g.fillOval(x + 90, y - 30, 50, 50);
        g.fillOval(x + 160, y - 30, 50, 50);

        // วงล้อด้านใน (สีเทา)
        g.setColor(new Color(120, 120, 120));
        g.fillOval(x + 35, y - 15, 20, 20);
        g.fillOval(x + 105, y - 15, 20, 20);
        g.fillOval(x + 175, y - 15, 20, 20);

        g.setComposite(oldComp);
    }

    // ผสมสีสองสี
    private Color blendColors(Color c1, Color c2, double t) {
        t = Math.min(1, Math.max(0, t));
        int r = (int) (c1.getRed() + t * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + t * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }

    // easing functions for slime pulse animation
    private double easeOutBounce(double t) {
        if (t < (1 / 2.75)) {
            return 7.5625 * t * t;
        } else if (t < (2 / 2.75)) {
            t -= (1.5 / 2.75);
            return 7.5625 * t * t + .75;
        } else if (t < (2.5 / 2.75)) {
            t -= (2.25 / 2.75);
            return 7.5625 * t * t + .9375;
        } else {
            t -= (2.625 / 2.75);
            return 7.5625 * t * t + .984375;
        }
    }

    private double easeOutQuad(double t) {
        return t * (2 - t);
    }
}

/**
 * MidpointEllipse
 * วาดวงรีด้วย Midpoint Ellipse Algorithm
 * มีเมธอดสร้าง GeneralPath เพื่อใช้ fill หรือ stroke ได้
 */
class MidpointEllipse {
    public GeneralPath createEllipsePath(int cx, int cy, int rx, int ry) {
        GeneralPath path = new GeneralPath();
        int x = 0, y = ry;

        // เริ่มวาดจาก (cx + x, cy + y)
        path.moveTo(cx + x, cy + y);

        // Region 1
        int ry2 = ry * ry;
        int rx2 = rx * rx;
        int twoRy2 = 2 * ry2;
        int twoRx2 = 2 * rx2;
        int px = 0;
        int py = twoRx2 * y;

        // Decision parameter
        int p = (int) (ry2 - (rx2 * ry) + (0.25 * rx2));

        while (px < py) {
            x++;
            px += twoRy2;
            if (p < 0) {
                p += ry2 + px;
            } else {
                y--;
                py -= twoRx2;
                p += ry2 + px - py;
            }
            plotEllipsePoints(path, cx, cy, x, y);
        }

        // Region 2
        p = (int) (ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2);
        while (y > 0) {
            y--;
            py -= twoRx2;
            if (p > 0) {
                p += rx2 - py;
            } else {
                x++;
                px += twoRy2;
                p += rx2 - py + px;
            }
            plotEllipsePoints(path, cx, cy, x, y);
        }

        path.closePath();
        return path;
    }

    // วาด 4 จุด symmetric ellipse
    private void plotEllipsePoints(GeneralPath path, int cx, int cy, int x, int y) {
        path.lineTo(cx + x, cy + y);
        path.lineTo(cx - x, cy + y);
        path.lineTo(cx - x, cy - y);
        path.lineTo(cx + x, cy - y);
    }
}

/**
 * MidpointCircle
 * วาดวงกลมเติมเต็มด้วย Midpoint Circle Algorithm
 */
class MidpointCircle {
    public void fillMidpointCircle(Graphics2D g, int cx, int cy, int r) {
        int x = 0;
        int y = r;
        int d = 1 - r;
        drawCirclePoints(g, cx, cy, x, y);
        while (x < y) {
            x++;
            if (d < 0) {
                d += 2 * x + 1;
            } else {
                y--;
                d += 2 * (x - y) + 1;
            }
            drawCirclePoints(g, cx, cy, x, y);
        }
    }

    // วาด scanline horizontal จากจุด symmetric ของวงกลม เพื่อ fill
    private void drawCirclePoints(Graphics2D g, int cx, int cy, int x, int y) {
        // วาดเส้น horizontal ให้เต็มวงกลมที่ตำแหน่ง y
        drawHorizontalLine(g, cx - x, cx + x, cy + y);
        drawHorizontalLine(g, cx - x, cx + x, cy - y);
        drawHorizontalLine(g, cx - y, cx + y, cy + x);
        drawHorizontalLine(g, cx - y, cx + y, cy - x);
    }

    private void drawHorizontalLine(Graphics2D g, int x1, int x2, int y) {
        g.drawLine(x1, y, x2, y);
    }
}