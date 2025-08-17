/*
Assignment: WHAT IF I REBORNED 
66050946 Phawothai Na Pairee
66050859 Pannawat Srithongnark

Algorithms implemented :
- Lines: Bresenham 
- Curves: Quadratic Bézier via De Casteljau sampling
- Circles/Ellipses: Midpoint circle & midpoint ellipse
- Polygon fill: Scanline algorithm
- Alpha compositing (premultiplied) for motion blur & polish

References:
Algorithms from:
- J. E. Bresenham (1965). "Algorithm for computer control of a digital plotter." IBM Systems Journal, 4(1), 25–30.
    (basis for integer line rasterization used in line()/thickLine())

- P. de Casteljau (1959). "Courbes à pôles" (Citroën internal report). Commonly referenced via standard CG texts.
    (basis for quadratic Bézier sampling used in qBezier())

- J. D. Foley, A. van Dam, S. K. Feiner, J. F. Hughes (1990/1995). Computer Graphics: Principles and Practice (2nd ed.).
    (midpoint circle/ellipse, scanline polygon fill fundamentals)

- T. Porter, T. Duff (1984). "Compositing Digital Images." SIGGRAPH Computer Graphics, 18(3), 253–259.
    (alpha compositing rationale; premultiplied blending used in pblend())

Credits & Notes:
- Motion blur is simulated by layered, alpha-attenuated "ghost" draws (see sceneStreet()/drawTruck()).
- Vignette and film grain are simple post-process passes implemented in software over the pixel buffer.

*/

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class Assignment1_66050946_66050859{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("WHAT IF I REBORNED — Slime World");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            PixelPanel p = new PixelPanel(600, 600);
            f.setContentPane(p);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            new Thread(p).start();
        });
    }
}

class PixelPanel extends JPanel implements Runnable {
    // ================= Fields =================
    private static final double FPS = 60.0; // Frames per second
    private static final long   NANO_PER_SEC = 1_000_000_000L; // ns/second

    // --- Timeline (seconds) ---
    private final double T1 = 2.0;   // street & truck hit
    private final double T2 = 3.0;   // fade to black
    private final double T3 = 8.0;   // slime world (loop duration)

    // --- Scene layout constants ---
    private static final int SKY_BASE_Y = 360;

    private static final int ROAD_Y_TOP    = 430;
    private static final int ROAD_Y_BOTTOM = 600;

    private static final int DASH_WIDTH = 50;
    private static final int DASH_GAP   = 30;   // step = width + gap = 80
    private static final int DASH_Y     = 510;
    private static final int DASH_THICK = 10;

    private static final int GROUND_Y = ROAD_Y_TOP; // baseline ตัวละคร/รถ

    // --- Slime smoothing state (reduce shimmer) ---
    private double rxSm = 130;
    private double rySm = 110;

    // --- Back buffer ---
    private final int W, H;
    private final BufferedImage canvas;
    private final int[] pix;

    private volatile boolean running = true;
    private long t0; // start time (ns)

    // ================= Lifecycle =================
    public PixelPanel(int w, int h) {
        this.W = w;
        this.H = h;
        setPreferredSize(new Dimension(W, H));

        // Create a 32-bit ARGB image and pull its pixel array for fast writes.
        canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        pix = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null); // draw back buffer to screen
    }

    @Override
    public void addNotify() {
        super.addNotify();
        t0 = System.nanoTime(); // record start time when component is added
    }

    @Override
    public void run() {
        final long step = (long) (NANO_PER_SEC / FPS); // target frame step in ns
        long last = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            if (now - last >= step) {
                double t = (now - t0) / 1e9; // seconds since start
                render(t % T3); // loop time within [0, T3)
                repaint();     // schedule paint
                last = now;
            } else {
                try {
                    Thread.sleep(1); // avoid busy-wait
                } catch (Exception ignore) { /* no-op */ }
            }
        }
    }

    // ================= Pixel utils =================
    private static int ARGB(int a, int r, int g, int b) {
        // Combine ARGB components into a single 32-bit int.
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    private void clear(int col) {
        Arrays.fill(pix, col);
    }

    private void pblend(int x, int y, int sr, int sg, int sb, int sa) {
        if ((x | y) < 0 || x >= W || y >= H || sa <= 0) return;

        int idx = y * W + x;
        int dst = pix[idx];

        int da = (dst >>> 24) & 255;
        int dr = (dst >>> 16) & 255;
        int dg = (dst >>> 8)  & 255;
        int db =  dst         & 255;

        // outA = sA + dA*(1 - sA)
        int a = sa + da * (255 - sa) / 255;

        // Premultiplied blend in 0..255 space:
        // outRGB = sRGB*sA + dRGB*(1 - sA)
        int r = (sr * sa + dr * (255 - sa)) / 255;
        int g = (sg * sa + dg * (255 - sa)) / 255;
        int b = (sb * sa + db * (255 - sa)) / 255;

        pix[idx] = ARGB(a, r, g, b);
    }

    // ================= Primitives =================
    // Bresenham line (outline)
    private void line(int x0, int y0, int x1, int y1, int r, int g, int b, int a) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0, y = y0;
        while (true) {
            pblend(x, y, r, g, b, a);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
    }

    // Thick line via small disc stamps (radius rr)
    private void thickLine(int x0, int y0, int x1, int y1, int rr,
                           int r, int g, int b, int a) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0, y = y0;
        while (true) {
            fillCircle(x, y, rr, r, g, b, a);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
    }

    // Filled circle via horizontal spans
    private void fillCircle(int cx, int cy, int rr, int r, int g, int b, int a) {
        int x = 0, y = rr;
        int d = 1 - rr;
        while (x <= y) {
            hspan(cx - x, cx + x, cy + y, r, g, b, a);
            hspan(cx - x, cx + x, cy - y, r, g, b, a);
            hspan(cx - y, cx + y, cy + x, r, g, b, a);
            hspan(cx - y, cx + y, cy - x, r, g, b, a);
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }
    }

    // Horizontal span (filled rectangle)
    private void hspan(int x1, int x2, int y, int r, int g, int b, int a) {
        if (y < 0 || y >= H) return;
        if (x1 > x2) {
            int t = x1; x1 = x2; x2 = t;
        }
        if (x2 < 0 || x1 >= W) return;
        x1 = Math.max(0, x1);
        x2 = Math.min(W - 1, x2);
        for (int x = x1; x <= x2; x++) {
            pblend(x, y, r, g, b, a);
        }
    }

    // Midpoint ellipse (outline)
    private void ellipse(int xc, int yc, int rx, int ry, int r, int g, int b, int a) {
        long rx2 = 1L * rx * rx;
        long ry2 = 1L * ry * ry;

        long x = 0, y = ry;
        long px = 0, py = 2 * rx2 * y;
        long p  = Math.round(ry2 - rx2 * ry + 0.25 * rx2);

        while (px < py) {
            plot4(xc, yc, (int) x, (int) y, r, g, b, a);
            x++; px += 2 * ry2;
            if (p < 0) {
                p += ry2 + px;
            } else {
                y--; py -= 2 * rx2;
                p += ry2 + px - py;
            }
        }

        p = Math.round(ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2);
        while (y >= 0) {
            plot4(xc, yc, (int) x, (int) y, r, g, b, a);
            y--; py -= 2 * rx2;
            if (p > 0) {
                p += rx2 - py;
            } else {
                x++; px += 2 * ry2;
                p += rx2 - py + px;
            }
        }
    }

    private void plot4(int xc, int yc, int x, int y, int r, int g, int b, int a) {
        pblend(xc + x, yc + y, r, g, b, a);
        pblend(xc - x, yc + y, r, g, b, a);
        pblend(xc + x, yc - y, r, g, b, a);
        pblend(xc - x, yc - y, r, g, b, a);
    }

    // Filled ellipse (analytic scanlines)
    private void fillEllipse(int xc, int yc, int rx, int ry, int r, int g, int b, int a) {
        for (int yy = -ry; yy <= ry; yy++) {
            double t = 1.0 - (yy * yy) / (double) (ry * ry);
            if (t < 0) continue;
            int xx = (int) Math.floor(rx * Math.sqrt(t));
            hspan(xc - xx, xc + xx, yc + yy, r, g, b, a);
        }
    }

    // Polygon fill (scanline)
    private void fillPolygon(int[] xs, int[] ys, int n, int r, int g, int b, int a) {
        // Compute y-bounds of polygon
        int ymin = H - 1, ymax = 0;
        for (int i = 0; i < n; i++) {
            ymin = Math.min(ymin, ys[i]);
            ymax = Math.max(ymax, ys[i]);
        }
        ymin = Math.max(0, ymin);
        ymax = Math.min(H - 1, ymax);

        for (int y = ymin; y <= ymax; y++) {
            int m = 0;
            int[] interX = new int[n];

            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                int y0 = ys[i], y1 = ys[j];
                int x0 = xs[i], x1 = xs[j];

                boolean cond = (y0 < y && y1 >= y) || (y1 < y && y0 >= y);
                if (cond) {
                    int x = x0 + (int) Math.round((double) (y - y0) * (x1 - x0) / (double) (y1 - y0));
                    interX[m++] = x;
                }
            }

            Arrays.sort(interX, 0, m);
            for (int k = 0; k + 1 < m; k += 2) {
                hspan(interX[k], interX[k + 1], y, r, g, b, a);
            }
        }
    }

    // Quadratic Bezier polyline (De Casteljau sampling)
    private void qBezier(int x0, int y0, int x1, int y1, int x2, int y2,
                         int r, int g, int b, int a) {
        double len = Math.hypot(x1 - x0, y1 - y0) + Math.hypot(x2 - x1, y2 - y1);
        int steps = Math.max(12, (int) (len / 6));

        int px = x0, py = y0;
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double u = 1 - t;
            int x = (int) Math.round(u * u * x0 + 2 * u * t * x1 + t * t * x2);
            int y = (int) Math.round(u * u * y0 + 2 * u * t * y1 + t * t * y2);
            line(px, py, x, y, r, g, b, a);
            px = x; py = y;
        }
    }

    // ================= Scenes =================
    private double easeOutQuad(double t) {
        return t * (2 - t);
    }

    private double easeInOutSine(double t) {
        return 0.5 * (1 - Math.cos(Math.PI * t));
    }

    private void render(double t) {
        if (t <= T1) {
            sceneStreet(t / T1);
            vignette();
            filmGrain(10); // grain only in street scene
        } else if (t <= T2) {
            double u = (t - T1) / (T2 - T1);
            sceneFadeBlack(u);
            vignette();
            // no grain during fade
        } else {
            double u = (t - T2) / (T3 - T2);
            sceneSlime(u);
            // no grain/vignette in slime scene for stability
        }
    }

    // Compute impact shake offset (only for character/vehicle)
    private int[] impactShake(double u) {
        if (u <= 0.85) return new int[]{0, 0};
        double k = (u - 0.85) / 0.15;
        int dx = (int) (Math.sin(60 * k) * 8);
        int dy = (int) (Math.cos(50 * k) * 6);
        return new int[]{dx, dy};
    }

    // --- Scene 1: street, stickman, truck with motion blur, impact shake ---
    private void sceneStreet(double u) {
        // Background gradient (stable)
        for (int y = 0; y < H; y++) {
            double k = y / (double) H;
            int r = (int) (20 * (1 - k) + 5 * k);
            int g = (int) (25 * (1 - k) + 10 * k);
            int b = (int) (40 * (1 - k) + 15 * k);
            for (int x = 0; x < W; x++) {
                pix[y * W + x] = ARGB(255, r, g, b);
            }
        }

        // Parallax skyline (polygons)
        int base = SKY_BASE_Y;
        int[] xs1 = {0, 80, 120, 200, 240, 320, 380, 460, 520, 600, 600, 0};
        int[] ys1 = {base, 300, 330, 280, 340, 310, 290, 320, 300, 280, ROAD_Y_BOTTOM, ROAD_Y_BOTTOM};
        fillPolygon(xs1, ys1, xs1.length, 35, 45, 70, 255);

        int[] xs2 = {0, 60, 140, 180, 260, 300, 360, 420, 480, 540, 600, 600, 0};
        int[] ys2 = {base + 30, 340, 350, 330, 360, 340, 355, 340, 360, 335, 350, ROAD_Y_BOTTOM, ROAD_Y_BOTTOM};
        fillPolygon(xs2, ys2, xs2.length, 25, 35, 55, 255);

        // Road
        fillPolygon(new int[]{0, W, W, 0},
                    new int[]{ROAD_Y_TOP, ROAD_Y_TOP, ROAD_Y_BOTTOM, ROAD_Y_BOTTOM},
                    4, 40, 40, 45, 255);

        // Dashed center line
        for (int x = 0; x < W; x += (DASH_WIDTH + DASH_GAP)) {
            fillPolygon(new int[]{x, x + DASH_WIDTH, x + DASH_WIDTH, x},
                        new int[]{DASH_Y, DASH_Y, DASH_Y + DASH_THICK, DASH_Y + DASH_THICK},
                        4, 220, 220, 140, 200);
        }

        // Compute shake (apply only to foreground objects)
        int[] sh = impactShake(u);
        int shakeX = sh[0];
        int shakeY = sh[1];

        // Stickman walking
        int sy = GROUND_Y;
        int sx = 100 + (int) (180 * u);
        drawStickman(sx + shakeX, sy + shakeY);

        // Truck motion with simple motion blur (multi-ghosts)
        int truckStart = W + 120;
        int truckEnd   = sx + 10;
        int tx = (int) (truckStart + (truckEnd - truckStart) * u);
        for (int i = 0; i < 6; i++) {
            int off   = i * 14;
            int alpha = (int) (220 * Math.pow(0.75, i));
            drawTruck(tx + off + shakeX, sy - 10 + shakeY, alpha);
        }
    }

    private void drawStickman(int x, int groundY) {
        int headR = 16;
        int headY = groundY - 88;
        int headX = x;

        // Head
        fillCircle(headX, headY, headR, 240, 230, 220, 255);

        // Body + legs (thick lines)
        thickLine(headX, headY + headR, headX, groundY - 30, 1, 235, 235, 235, 255);
        thickLine(headX, groundY - 30, headX - 22, groundY, 1, 235, 235, 235, 255);
        thickLine(headX, groundY - 30, headX + 22, groundY, 1, 235, 235, 235, 255);

        // Arms (Bezier swing)
        qBezier(headX, headY + headR + 10, headX - 18, groundY - 70, headX - 34, groundY - 60,
                235, 235, 235, 255);
        qBezier(headX, headY + headR + 10, headX + 18, groundY - 70, headX + 34, groundY - 60,
                235, 235, 235, 255);
    }

    private void drawTruck(int x, int y, int alpha) {
        // Body rectangle
        fillPolygon(new int[]{x, x + 180, x + 180, x},
                    new int[]{y - 60, y - 60, y, y},
                    4, 70, 130, 180, alpha);

        // Cabin polygon
        fillPolygon(new int[]{x + 130, x + 180, x + 180, x + 130},
                    new int[]{y - 100, y - 100, y - 60, y - 60},
                    4, 100, 170, 210, alpha);

        // Window polygon
        fillPolygon(new int[]{x + 140, x + 175, x + 175, x + 140},
                    new int[]{y - 95,  y - 95,  y - 70, y - 70},
                    4, 200, 230, 250, (int) (alpha * 0.7));

        // Wheels (filled circles)
        fillCircle(x + 30,  y, 22, 40, 40, 40, alpha);
        fillCircle(x + 100, y, 22, 40, 40, 40, alpha);
        fillCircle(x + 170, y, 22, 40, 40, 40, alpha);

        // Wheel hubs
        fillCircle(x + 30,  y, 9, 120, 120, 120, alpha);
        fillCircle(x + 100, y, 9, 120, 120, 120, alpha);
        fillCircle(x + 170, y, 9, 120, 120, 120, alpha);
    }

    // --- Scene 2: fade to black ---
    private void sceneFadeBlack(double u) {
        clear(ARGB(255, 0, 0, 0)); // clear to black first (keeps fade predictable)
        int a = (int) (255 * u);   // fade alpha

        // Blend black rectangle over full frame using alpha 'a'
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                pblend(x, y, 0, 0, 0, a);
            }
        }
    }

    // --- Scene 3: Slime world ---
    private void sceneSlime(double u) {
        // Oceanic gradient background
        for (int y = 0; y < H; y++) {
            double k = y / (double) H;
            int r = (int) (5 * (1 - k) + 20 * k);
            int g = (int) (15 * (1 - k) + 80 * k);
            int b = (int) (10 * (1 - k) + 60 * k);
            for (int x = 0; x < W; x++) {
                pix[y * W + x] = ARGB(255, r, g, b);
            }
        }

        int cx = W / 2;
        int cy = H / 2 + 30;
        double rx0 = 130, ry0 = 110; // initial radii

        // Breathing with smoothing to avoid pixel jitter along ellipse boundary
        double pulse    = 0.08 * Math.sin(u * Math.PI * 6);
        double rxTarget = rx0 * (1 + pulse + 0.25 * easeInOutSine(u));
        double ryTarget = ry0 * (1 - pulse + 0.12 * easeOutQuad(u));

        // Low-pass filter (temporal smoothing). Smaller s = smoother, more lag.
        double s = 0.25;
        rxSm = rxSm + s * (rxTarget - rxSm);
        rySm = rySm + s * (ryTarget - rySm);

        int rx = (int) Math.round(rxSm);
        int ry = (int) Math.round(rySm);

        // Slime body: radial shading & rim light
        slimeBody(cx, cy, rx, ry);

        // Eyes (dark ellipses)
        int ex   = (int) (rx * 0.40);
        int eyOff = (int) (-ry * 0.18);
        int eRx  = (int) (rx * 0.26);
        int eRy  = (int) (ry * 0.20);
        fillEllipse(cx - ex, cy + eyOff, eRx, eRy, 30, 40, 45, 255);
        fillEllipse(cx + ex, cy + eyOff, eRx, eRy, 30, 40, 45, 255);

        // Highlights (small circles)
        int hx = (int) (cx - ex - eRx * 0.15);
        int hy = (int) (cy + eyOff - eRy * 0.15);
        int hr = 9 + (int) (3 * Math.sin(u * 15));
        fillCircle(hx,              hy, hr, 255, 255, 255, 200);
        fillCircle(hx + 2 * ex,     hy, hr, 255, 255, 255, 200);

        // Smile (Bezier)
        int mw = (int) (rx * (0.5 + 0.15 * Math.sin(u * 4 * Math.PI)));
        int mh = (int) (ry * 0.18);
        qBezier(cx - mw / 2, cy + (int) (ry * 0.3),
                cx,          cy + (int) (ry * 0.3) + mh,
                cx + mw / 2, cy + (int) (ry * 0.3), 40, 70, 45, 255);

        // Floating droplets (orbiting)
        for (int i = 0; i < 16; i++) {
            double ang = i * (2 * Math.PI / 16.0) + u * 6;
            int px = cx + (int) ((rx + 18) * Math.cos(ang));
            int py = cy + (int) ((ry + 18) * Math.sin(ang));
            int pr = 6 + (int) (3 * Math.sin(u * 15 + i));
            int al = 110 + (int) (110 * Math.sin(u * 15 + i));
            al = Math.max(0, Math.min(255, al));
            fillCircle(px, py, pr, 180, 250, 200, al);
        }
    }

    private void slimeBody(int cx, int cy, int rx, int ry) {
        // Fill ellipse with radial-like shading & off-center inner light
        for (int yy = -ry; yy <= ry; yy++) {
            double yTerm = (yy * yy) / (double) (ry * ry);
            if (yTerm > 1) continue;
            int xx = (int) Math.floor(rx * Math.sqrt(1 - yTerm));

            for (int xxp = -xx; xxp <= xx; xxp++) {
                double nx = xxp / (double) rx;
                double ny = yy  / (double) ry;
                double d = Math.sqrt(nx * nx + ny * ny); // 0..1 distance from center

                int r = (int) (20 + (70  - 20) * (1 - d));
                int g = (int) (50 + (180 - 50) * (1 - d));
                int b = (int) (30 + (140 - 30) * (1 - d));

                // subtle inner light (off-center)
                double lx = (xxp - rx * 0.2) / (rx * 1.2);
                double ly = (yy  - ry * 0.3) / (ry * 1.2);
                double l  = Math.exp(-(lx * lx + ly * ly) * 2.5);

                int rr = clamp(r + (int) (30 * l));
                int gg = clamp(g + (int) (40 * l));
                int bb = clamp(b + (int) (35 * l));

                pblend(cx + xxp, cy + yy, rr, gg, bb, 230);
            }
        }
        // Rim light
        ellipse(cx, cy, rx, ry, 180, 255, 220, 180);
    }

    private int clamp(int v) {
        return (v < 0) ? 0 : Math.min(255, v);
    }

    // ================= Post-process =================
    private void vignette() {
        int cx = W / 2;
        int cy = H / 2;
        double maxd = Math.hypot(cx, cy);

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double d = Math.hypot(x - cx, y - cy) / maxd;
                int a = (int) (180 * Math.pow(d, 2.2));
                if (a > 0) pblend(x, y, 0, 0, 0, a);
            }
        }
    }

    private void filmGrain(int strength) {
        Random r = new Random(1234);
        int samples = W * H / strength;
        for (int i = 0; i < samples; i++) {
            int x = r.nextInt(W);
            int y = r.nextInt(H);
            int k = r.nextInt(21) - 10; // noise in [-10, +10]

            int idx = y * W + x;
            int c = pix[idx];

            int a = (c >>> 24) & 255;
            int rr = (c >>> 16) & 255;
            int gg = (c >>> 8)  & 255;
            int bb =  c         & 255;

            rr = clamp(rr + k);
            gg = clamp(gg + k);
            bb = clamp(bb + k);

            pix[idx] = ARGB(a, rr, gg, bb);
        }
    }
}