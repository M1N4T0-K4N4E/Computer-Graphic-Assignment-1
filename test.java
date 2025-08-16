import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class test extends JPanel implements ActionListener {
    private Image bgScene1;
    private Timer timer;
    private int frame = 0;
    private final int FPS = 30;

    public test() {
        setPreferredSize(new Dimension(600, 600));
        bgScene1 = new ImageIcon("hospital.jpg").getImage(); // โรงพยาบาล
        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (frame < FPS * 3) {
            drawScene1(g2); // โรงพยาบาล
        } else if (frame < FPS * 4) {
            drawScene2(g2); // ฉากดำ + ข้อความ
        } else if (frame < FPS * 6) {
            drawScene3(g2); // Fade in + มือ
        } else if (frame < FPS * 8) {
            drawScene4(g2); // พระเอกบนเตียง
        } else if (frame < FPS * 10) {
            drawScene5(g2); // หมอเดินเข้ามา
        } else if (frame < FPS * 13) {
            drawScene6(g2); // หมอนั่งพูด
        } else {
            drawScene7(g2); // กระโดดหน้าต่าง
        }
    }

    // Scene 1: โรงพยาบาล
    private void drawScene1(Graphics2D g) {
        g.drawImage(bgScene1, 0, 0, getWidth(), getHeight(), this);
        g.setColor(Color.black);
        String subtitle = "Hospital";
        g.setFont(new Font("Arial", Font.CENTER_BASELINE, 20));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(subtitle);
        g.drawString(subtitle, (getWidth() - textWidth) / 2, getHeight() - 30); // ให้ subtitle อยู่ห่างจากขอบล่าง 30px

        // g.setColor(new Color(200, 230, 255)); // ฟ้าสดใส
        // g.fillRect(0, 0, getWidth(), getHeight());

        // g.setColor(Color.DARK_GRAY);
        // g.fillRect(100, 250, 400, 250); // ตึก

        // g.setColor(Color.GRAY);
        // g.fillRect(250, 200, 100, 50); // ป้าย

        // g.setColor(Color.WHITE);
        // g.drawString("Hospital", 265, 230);

        // // เส้นโค้งเป็นประตูโค้ง
        // g.setColor(Color.LIGHT_GRAY);
        // g.drawArc(280, 400, 40, 60, 0, 180);
    }

    // Scene 2: ฉากมืด
    private void drawScene2(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.WHITE);
        g.drawString("Where is here ?", 250, 300);
    }

    // Scene 3: Fade in + มือ
    private void drawScene3(Graphics2D g) {
        float alpha = (frame - FPS * 4) / (float) (FPS * 2);
        int r = (int) (200 * alpha);
        int gr = (int) (230 * alpha);
        int b = (int) (255 * alpha);

        g.setColor(new Color(r, gr, b));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.drawString("มือนี่มัน...", 270, 250);

        // มือเป็นวงกลม (midpoint circle)
        g.setColor(Color.BLACK);
        drawMidpointCircle(g, 300, 300, 50);
    }

    // Scene 4: พระเอกบนเตียง (ตัวก้างปลา)
    private void drawScene4(Graphics2D g) {
        g.setColor(new Color(255, 255, 240));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.DARK_GRAY);
        g.drawRect(200, 350, 200, 50); // เตียง

        g.setColor(Color.BLACK);
        drawStickFigure(g, 300, 320, 30); // พระเอกนั่ง
    }

    // Scene 5: หมอเดินเข้ามา
    private void drawScene5(Graphics2D g) {
        g.setColor(new Color(255, 255, 240));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.drawRect(450, 250, 50, 100); // ประตู

        // เคลื่อนไหวเข้ามา
        int doctorX = 500 - (frame - FPS * 8) * 3;
        g.setColor(Color.BLUE);
        drawStickFigure(g, doctorX, 320, 20);

        g.setColor(Color.BLACK);
        g.drawString("ตื่นแล้วหรอครับคุณฮุน", 200, 100);
    }

    // Scene 6: หมอนั่งพูด
    private void drawScene6(Graphics2D g) {
        g.setColor(new Color(255, 255, 240));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.drawArc(200, 400, 100, 50, 0, 180); // เก้าอี้

        g.setColor(Color.RED);
        drawStickFigure(g, 300, 320, 30); // พระเอก
        g.setColor(Color.BLUE);
        drawStickFigure(g, 250, 320, 30); // หมอ

        g.setColor(Color.BLACK);
        g.drawString("ใช่ครับ คุณชื่อฮุนครับ ที่นี่คือโรงพยาบาลครับ", 100, 100);
    }

    // Scene 7: กระโดดหน้าต่าง
    private void drawScene7(Graphics2D g) {
        g.setColor(new Color(200, 230, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.drawString("ฮุน: ฮุน เซน!?...", 250, 100);

        int y = 320 - ((frame - FPS * 13) % 30) * 5; // กระโดดขึ้น
        drawStickFigure(g, 300, y, 30);
    }

    // วาดตัวก้างปลา
    private void drawStickFigure(Graphics g, int x, int y, int headRadius) {
        // หัว
        drawMidpointCircle(g, x, y - headRadius - 10, headRadius);
        // ตัว
        g.drawLine(x, y, x, y + 50);
        // แขน
        g.drawLine(x, y + 10, x - 30, y + 30);
        g.drawLine(x, y + 10, x + 30, y + 30);
        // ขา
        g.drawLine(x, y + 50, x - 20, y + 80);
        g.drawLine(x, y + 50, x + 20, y + 80);
    }

    // Midpoint Circle Algorithm
    private void drawMidpointCircle(Graphics g, int xc, int yc, int r) {
        int x = 0, y = r;
        int d = 1 - r;
        drawCirclePoints(g, xc, yc, x, y);
        while (x < y) {
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
            drawCirclePoints(g, xc, yc, x, y);
        }
    }

    private void drawCirclePoints(Graphics g, int xc, int yc, int x, int y) {
        g.fillRect(xc + x, yc + y, 1, 1);
        g.fillRect(xc - x, yc + y, 1, 1);
        g.fillRect(xc + x, yc - y, 1, 1);
        g.fillRect(xc - x, yc - y, 1, 1);
        g.fillRect(xc + y, yc + x, 1, 1);
        g.fillRect(xc - y, yc + x, 1, 1);
        g.fillRect(xc + y, yc - x, 1, 1);
        g.fillRect(xc - y, yc - x, 1, 1);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame++;
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("WHAT IF I REBORNED");
        test panel = new test();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
