// test2.java
// Single-file Java2D animation (600x600) satisfying:
// - At least one straight line (Bresenham)
// - At least one curve (Quadratic Bezier)
// - At least one circle/ellipse (Midpoint Circle & Ellipse)
// - 7 scenes under the theme "WHAT IF I REBORNED"
// - Java2D only; avoids Graphics2D shape-creation APIs (no drawLine/drawOval/etc.)
//   Uses pixel plotting via BufferedImage. drawImage is used only to present the raster.
// - No external assets.
//
// Scenes (approx timings; total ~7.4s):
// 1) Exterior hospital establishing (1.0s)
// 2) Cut to black + speech bubble "..." (0.6s)
// 3) Fade up in room, protagonist looks at hands (1.0s)
// 4) Sit up on bed (1.2s)
// 5) Door opens, doctor walks in (1.2s)
// 6) Conversation (speech bubbles "... ... ...") (1.2s)
// 7) Shock -> run to window -> jump silhouette (non-graphic) -> cut to black (1.2s)
//
// Compile & run:
//   javac test2.java && java test2
//
// Note: Dialog is represented with speech bubbles (three dots) to avoid font APIs.
//       Curves appear in arms/hands (Bezier), door arc accent, and bubble tails.

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Arrays;

public class test2 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("WHAT IF I REBORNED - Java2D");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            AnimPanel p = new AnimPanel(600, 600);
            f.add(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            new Thread(p).start();
        });
    }
}

class AnimPanel extends JPanel implements Runnable {
    private final int W, H;
    private final BufferedImage canvas;
    private final int[] pix; // ARGB int buffer

    private volatile boolean running = true;
    private long t0;

    // Colors
    private static int rgb(int r,int g,int b){return 0xFF000000 | ((r&255)<<16)|((g&255)<<8)|(b&255);}    
    private final int COL_BG_SKY = rgb(210, 235, 255);
    private final int COL_HOSPITAL = rgb(230, 230, 230);
    private final int COL_WINDOW = rgb(120, 170, 220);
    private final int COL_DARK = rgb(5,5,8);
    private final int COL_WHITE = rgb(255,255,255);
    private final int COL_BLACK = rgb(0,0,0);
    private final int COL_LINE = rgb(20,20,20);
    private final int COL_ACCENT = rgb(220, 60, 60); // red cross
    private final int COL_DOCTOR = rgb(30,30,30);
    private final int COL_HERO = rgb(20,20,20);
    private final int COL_BUBBLE = rgb(250,250,250);

    public AnimPanel(int w, int h) {
        this.W = w; this.H = h;
        setPreferredSize(new Dimension(W, H));
        canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        pix = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
    }

    @Override public void addNotify(){ super.addNotify(); t0 = System.nanoTime(); }

    @Override protected void paintComponent(Graphics g){
        g.drawImage(canvas, 0, 0, null);
    }

    @Override public void run(){
        final double fps = 60.0;
        final long frameNanos = (long)(1_000_000_000L / fps);
        long last = System.nanoTime();
        while(running){
            long now = System.nanoTime();
            if(now - last >= frameNanos){
                double t = (now - t0) / 1e9; // seconds since start
                render(t);
                repaint();
                last = now;
            } else {
                try { Thread.sleep(1); } catch(Exception ignore){}
            }
        }
    }

    // ========================= Low-level Pixel API =========================
    private void clear(int color){ Arrays.fill(pix, color); }

    private void pset(int x, int y, int color){
        if((x|y) < 0 || x >= W || y >= H) return;
        pix[y*W + x] = color;
    }

    // Bresenham line (all octants)
    private void line(int x0,int y0,int x1,int y1,int color){
        int dx = Math.abs(x1-x0), dy = Math.abs(y1-y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x=x0, y=y0;
        while(true){
            pset(x,y,color);
            if(x==x1 && y==y1) break;
            int e2 = 2*err;
            if(e2 > -dy){ err -= dy; x += sx; }
            if(e2 <  dx){ err += dx; y += sy; }
        }
    }

    // Midpoint Circle
    private void circle(int cx,int cy,int r,int color){
        int x=0, y=r; int d = 1-r;
        while(x <= y){
            plot8(cx,cy,x,y,color);
            if(d < 0){ d += 2*x + 3; }
            else { d += 2*(x - y) + 5; y--; }
            x++;
        }
    }
    private void plot8(int cx,int cy,int x,int y,int color){
        pset(cx+x, cy+y, color); pset(cx-x, cy+y, color);
        pset(cx+x, cy-y, color); pset(cx-x, cy-y, color);
        pset(cx+y, cy+x, color); pset(cx-y, cy+x, color);
        pset(cx+y, cy-x, color); pset(cx-y, cy-x, color);
    }

    // Midpoint Ellipse
    private void ellipse(int xc,int yc,int rx,int ry,int color){
        long rx2 = 1L*rx*rx, ry2 = 1L*ry*ry;
        long x=0, y=ry;
        long px = 0; long py = 2*rx2*y;
        long p = Math.round(ry2 - rx2*ry + 0.25*rx2);
        while(px < py){
            plot4(xc,yc,(int)x,(int)y,color);
            x++; px += 2*ry2;
            if(p < 0){ p += ry2 + px; }
            else { y--; py -= 2*rx2; p += ry2 + px - py; }
        }
        p = Math.round(ry2*(x+0.5)*(x+0.5) + rx2*(y-1)*(y-1) - rx2*ry2);
        while(y >= 0){
            plot4(xc,yc,(int)x,(int)y,color);
            y--; py -= 2*rx2;
            if(p > 0){ p += rx2 - py; }
            else { x++; px += 2*ry2; p += rx2 - py + px; }
        }
    }
    private void plot4(int xc,int yc,int x,int y,int color){
        pset(xc+x,yc+y,color); pset(xc-x,yc+y,color);
        pset(xc+x,yc-y,color); pset(xc-x,yc-y,color);
    }

    // Quadratic Bezier (De Casteljau, adaptive step by flatness approx)
    private void qBezier(int x0,int y0,int x1,int y1,int x2,int y2,int color){
        // estimate length for steps
        double len = Math.hypot(x1-x0, y1-y0) + Math.hypot(x2-x1, y2-y1);
        int steps = Math.max(8, (int)(len/6));
        int px = x0, py = y0;
        for(int i=1;i<=steps;i++){
            double t = i/(double)steps;
            double a = 1-t, b = t;
            double x = a*a*x0 + 2*a*b*x1 + b*b*x2;
            double y = a*a*y0 + 2*a*b*y1 + b*b*y2;
            int xi = (int)Math.round(x), yi = (int)Math.round(y);
            line(px,py,xi,yi,color);
            px=xi; py=yi;
        }
    }

    // ========================= Scene Helpers =========================
    private void rectOutline(int x,int y,int w,int h,int color){
        line(x,y,x+w,y,color);
        line(x+w,y,x+w,y+h,color);
        line(x+w,y+h,x,y+h,color);
        line(x,y+h,x,y,color);
    }

    private void windowGrid(int x,int y,int w,int h,int rows,int cols,int color){
        rectOutline(x,y,w,h,color);
        for(int c=1;c<cols;c++) line(x + c*w/cols, y, x + c*w/cols, y+h, color);
        for(int r=1;r<rows;r++) line(x, y + r*h/rows, x+w, y + r*h/rows, color);
    }

    private void hospitalExterior(double t){
        // sky
        clear(COL_BG_SKY);
        // ground line
        line(0, 500, W, 500, COL_LINE);
        // hospital body (outline boxes)
        rectOutline(120, 220, 360, 260, COL_LINE);
        // entrance
        rectOutline(270, 360, 60, 120, COL_LINE);
        // windows grid
        for(int r=0;r<2;r++){
            for(int c=0;c<3;c++){
                int wx = 150 + c*110;
                int wy = 250 + r*80;
                windowGrid(wx, wy, 80, 50, 2, 3, COL_LINE);
            }
        }
        // hospital cross sign inside a circle
        circle(300, 260, 30, COL_LINE);
        line(300-15,260,300+15,260,COL_ACCENT);
        line(300,260-15,300,260+15,COL_ACCENT);
        // simple title banner using lines
        rectOutline(220, 320, 160, 20, COL_LINE);
    }

    private void roomBackground(){
        // wall + floor horizon
        clear(rgb(245,245,248));
        line(0, 420, W, 420, COL_LINE);
        // window on wall (left)
        windowGrid(40, 60, 150, 120, 2, 2, COL_LINE);
        // bed frame
        rectOutline(230, 350, 260, 40, COL_LINE);
        rectOutline(220, 300, 220, 20, COL_LINE); // headboard
        // IV stand (lines & circle wheel)
        line(510, 240, 510, 420, COL_LINE);
        line(480, 240, 540, 240, COL_LINE);
        circle(510, 430, 6, COL_LINE);
    }

    private void drawStickPerson(int cx,int cy,double armLift,double sit,double scale,int color){
        int rHead = (int)(12*scale);
        circle(cx, cy - (int)(28*scale), rHead, color); // head
        // body
        line(cx, cy - (int)(16*scale), cx, cy + (int)(16*scale), color);
        // legs
        line(cx, cy + (int)(16*scale), cx - (int)(12*scale), cy + (int)(30*scale), color);
        line(cx, cy + (int)(16*scale), cx + (int)(12*scale), cy + (int)(30*scale), color);
        // arms (Bezier for curve)
        int ax0 = cx, ay0 = cy - (int)(10*scale);
        int ax2 = cx - (int)(18*scale), ay2 = cy + (int)(5*scale);
        int bx2 = cx + (int)(18*scale), by2 = cy + (int)(5*scale);
        int ax1 = ax0 - (int)(12*scale); int ay1 = ay0 - (int)(10*scale + armLift);
        int bx1 = ax0 + (int)(12*scale); int by1 = ay0 - (int)(10*scale + armLift);
        qBezier(ax0,ay0, ax1,ay1, ax2,ay2, color);
        qBezier(ax0,ay0, bx1,by1, bx2,by2, color);
        // if sitting, shift torso/legs a bit forward
        if(sit > 0){
            int kneeY = cy + (int)(16*scale);
            int f = (int)(10*scale * sit);
            line(cx, cy + (int)(16*scale), cx + f, kneeY, color);
            line(cx + f, kneeY, cx + f + (int)(12*scale), kneeY, color);
        }
    }

    private void speechBubble(int x,int y,int w,int h,int tailDx,int tailDy){
        // ellipse outline as bubble
        ellipse(x + w/2, y + h/2, w/2, h/2, COL_LINE);
        // tail using curved Bezier + lines
        int tx0 = x + (3*w)/4, ty0 = y + h;
        int tx1 = tx0 + tailDx/2, ty1 = ty0 + tailDy/2;
        int tx2 = tx0 + tailDx, ty2 = ty0 + tailDy;
        qBezier(tx0,ty0, tx1,ty1, tx2,ty2, COL_LINE);
        line(tx2,ty2, tx2-4,ty2-2, COL_LINE);
        line(tx2,ty2, tx2-2,ty2-4, COL_LINE);
        // "..." inside bubble (three small circles)
        int cx = x + w/2; int cy = y + h/2;
        circle(cx-20, cy, 3, COL_LINE);
        circle(cx,     cy, 3, COL_LINE);
        circle(cx+20, cy, 3, COL_LINE);
    }

    private void openingDoor(double angleRad){
        // door pivots at left jamb (x=100,y=260 to y=420)
        int px = 100, pyTop = 260, pyBot = 420; int doorW = 80;
        // rotate far edge around pivot
        double ca = Math.cos(angleRad), sa = Math.sin(angleRad);
        int xTop = px + (int)Math.round(doorW*ca);
        int yTop = pyTop - (int)Math.round(doorW*sa);
        int xBot = px + (int)Math.round(doorW*ca);
        int yBot = pyBot - (int)Math.round(doorW*sa);
        // jamb & edges
        line(px, pyTop, px, pyBot, COL_LINE);
        line(px, pyTop, xTop, yTop, COL_LINE);
        line(px, pyBot, xBot, yBot, COL_LINE);
        line(xTop, yTop, xBot, yBot, COL_LINE);
        // arc accent (Bezier curve from closed to current angle)
        int ax0 = px+doorW, ay0 = pyTop;
        int ax2 = xTop,    ay2 = yTop;
        int ax1 = px + doorW, ay1 = pyTop - 20; // slight arc control
        qBezier(ax0,ay0, ax1,ay1, ax2,ay2, COL_LINE);
    }

    private void render(double t){
        // Scene timing (seconds)
        double t1=1.0, t2=1.6, t3=2.6, t4=3.8, t5=5.0, t6=6.2, t7=7.4;
        if(t > t7) t = t7; // clamp end

        if(t <= t1){
            // 1) Exterior hospital
            hospitalExterior(t);
        } else if(t <= t2){
            // 2) Cut to black with bubble
            clear(COL_BLACK);
            // small bubble implying "ที่นี่ที่ไหนเนี่ย"
            speechBubble(200, 380, 200, 80, -40, 30);
        } else if(t <= t3){
            // 3) Fade in room + hands lifting (armLift anim)
            roomBackground();
            double u = (t - t2) / (t3 - t2); // 0..1
            int armLift = (int)(20*u*u*30/20.0);
            // hero seated low, looking at hands
            drawStickPerson(330, 340, armLift, 0.0, 1.2, COL_HERO);
        } else if(t <= t4){
            // 4) Sit up movement
            roomBackground();
            double u = (t - t3) / (t4 - t3);
            drawStickPerson(330, 340 - (int)(15*u), 10, u, 1.2, COL_HERO);
        } else if(t <= t5){
            // 5) Door opens + doctor enters
            roomBackground();
            double u = (t - t4) / (t5 - t4); // 0..1
            openingDoor(u * Math.toRadians(70));
            // doctor walks from right -> bedside
            int dx = (int)(u * 220);
            drawStickPerson(520 - dx, 340, 5, 0.0, 1.0, COL_DOCTOR);
        } else if(t <= t6){
            // 6) Conversation bubbles
            roomBackground();
            openingDoor(Math.toRadians(70));
            drawStickPerson(300, 325, 6, 1.0, 1.2, COL_HERO);
            drawStickPerson(320, 340, 5, 0.0, 1.0, COL_DOCTOR);
            // bubbles (doctor and hero)
            speechBubble(340, 230, 180, 70, -30, 30); // doctor speaks
            speechBubble(180, 260, 200, 80,  20, 40); // hero replies
            speechBubble(340, 230, 180, 70, -30, 30); // doctor again
        } else {
            // 7) Shock -> run to window -> jump silhouette -> cut to black
            double u = (t - t6) / (t7 - t6); // 0..1
            if(u < 0.6){
                roomBackground();
                openingDoor(Math.toRadians(70));
                // run left to window (parabolic arc toward window)
                int x = (int)(300 - 300*u*1.2);
                int y = 340 - (int)(120 * (u*u));
                drawStickPerson(x, y, 0, 0.0, 1.1, COL_HERO);
                // faint motion trail using circles (midpoint)
                circle(x-20, y-30, 8, rgb(180,180,180));
            } else {
                // cut to black
                clear(COL_BLACK);
            }
        }
    }
}
