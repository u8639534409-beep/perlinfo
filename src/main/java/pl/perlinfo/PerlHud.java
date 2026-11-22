package pl.perlinfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rysuje na ekranie: powiekszona perle, tracer, znacznik ladowania i panel ze strzalka. */
public final class PerlHud {
    private PerlHud() {
    }

    /** Do kiedy pokazywac alarm testowy. */
    public static long demoUntil = 0;
    private static long demoStart = 0;

    public static void startDemo() {
        demoStart = System.currentTimeMillis();
        demoUntil = demoStart + 5000L;
    }

    private static final int PANEL_W = 190;
    private static final int MARGIN = 8;
    private static final int TEAL = 0x2EE6C8;
    private static final int VIOLET = 0x9B6DFF;
    private static final int RED = 0xFF4D6D;

    private record Row(String name, double seconds, double progress, double angle, double dist) {
    }

    public static void drawArrow(DrawContext ctx, int cx, int cy, double angle, int color) {
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0);
        m.multiply(RotationAxis.POSITIVE_Z.rotation((float) angle));
        ctx.fill(0, -6, 1, -5, color);
        ctx.fill(-1, -5, 2, -4, color);
        ctx.fill(-2, -4, 3, -3, color);
        ctx.fill(-1, -3, 2, 4, color);
        m.pop();
    }

    private static void drawLine(DrawContext ctx, float x0, float y0, float x1, float y1, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x0, y0, 0);
        m.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        ctx.fill(0, -1, (int) len, 1, color);
        m.pop();
    }

    /** Kat do celu wzgledem kierunku patrzenia (0 = przed Toba, dodatni = w prawo). */
    private static double relAngle(MinecraftClient c, Vec3d target) {
        double yaw = Math.toRadians(c.player.getYaw());
        double dx = target.x - c.player.getX();
        double dz = target.z - c.player.getZ();
        double localX = dx * (-Math.cos(yaw)) + dz * (-Math.sin(yaw));
        double localZ = dx * (-Math.sin(yaw)) + dz * Math.cos(yaw);
        return Math.atan2(localX, localZ);
    }

    public static void render(DrawContext ctx) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.world == null || c.options.hudHidden) return;

        long now = System.currentTimeMillis();
        boolean demo = now < demoUntil;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        List<Row> rows = new ArrayList<>();

        if (demo) {
            double total = (demoUntil - demoStart) / 1000.0;
            double left = Math.max(0, (demoUntil - now) / 1000.0);
            rows.add(new Row("Gracz123", left, 1.0 - left / total, 0.6, 34));
        }

        for (Pearls.Pearl p : Pearls.active()) {
            Vec3d pos = p.entity.getPos();
            Vec3d target = p.landing != null ? p.landing : pos;
            double dist = c.player.getPos().distanceTo(target);
            rows.add(new Row(p.owner, p.seconds(), p.progress(), relAngle(c, target), dist));

            // tracer do perly
            Proj.P pp = Proj.project(c, pos, w, h);
            if (PerlConfig.tracers) {
                drawLine(ctx, w / 2f, h, pp.x(), pp.y(), 0xFF000000 | TEAL);
            }

            // powiekszona perla
            if (PerlConfig.enlarge && pp.onScreen()) {
                float s = PerlConfig.enlargeSize / 100f;
                MatrixStack m = ctx.getMatrices();
                m.push();
                m.translate(pp.x() - 8 * s, pp.y() - 8 * s, 0);
                m.scale(s, s, 1f);
                ctx.drawItem(new ItemStack(Items.ENDER_PEARL), 0, 0);
                m.pop();
            }

            // znacznik miejsca ladowania
            if (PerlConfig.landing && p.landing != null) {
                Proj.P lp = Proj.project(c, p.landing, w, h);
                if (lp.onScreen()) {
                    int lx = (int) lp.x();
                    int ly = (int) lp.y();
                    int col = 0xFF000000 | (p.seconds() < 1.5 ? RED : VIOLET);
                    ctx.fill(lx - 6, ly - 1, lx + 7, ly + 1, col);
                    ctx.fill(lx - 1, ly - 6, lx + 1, ly + 7, col);
                    ctx.drawCenteredTextWithShadow(c.textRenderer,
                            Text.literal(p.owner + "  " + String.format(Locale.ROOT, "%.1f s", p.seconds())),
                            lx, ly - 18, col);
                }
            }
        }

        if (rows.isEmpty() || !PerlConfig.arrow) return;
        drawPanel(ctx, c, rows, now, w, h);
    }

    private static void drawPanel(DrawContext ctx, MinecraftClient c, List<Row> all, long now, int w, int h) {
        List<Row> rows = all.size() > 3 ? all.subList(0, 3) : all;

        float scale = PerlConfig.arrowSize / 100f;
        int ph = 26 + rows.size() * 24 + 2;
        int pxS = Math.round(PANEL_W * scale);
        int pyS = Math.round(ph * scale);

        int col = PerlConfig.arrowPos % 3;
        int row = PerlConfig.arrowPos / 3;
        int x = col == 0 ? MARGIN : col == 1 ? (w - pxS) / 2 : w - pxS - MARGIN;
        int y = row == 0 ? MARGIN : row == 1 ? (h - pyS) / 2 : h - pyS - MARGIN;
        x = Math.max(0, x);
        y = Math.max(0, y);

        double pulse = 0.5 + 0.5 * Math.sin(now / 150.0);
        int alpha = 150 + (int) (105 * pulse);
        int border = (alpha << 24) | TEAL;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);

        ctx.fillGradient(0, 0, PANEL_W, ph, 0xE6101C2B, 0xE6070B14);
        ctx.drawBorder(0, 0, PANEL_W, ph, border);
        ctx.fill(0, 0, 3, ph, 0xFF000000 | TEAL);

        ctx.drawItem(new ItemStack(Items.ENDER_PEARL), 8, 4);
        ctx.drawTextWithShadow(c.textRenderer,
                Text.literal("PERŁA W LOCIE").formatted(Formatting.BOLD), 28, 8, 0xFF000000 | TEAL);
        ctx.fill(8, 22, PANEL_W - 8, 23, 0x44FFFFFF);

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = 26 + i * 24;
            int color = 0xFF000000 | (r.seconds() < 1.5 ? RED : TEAL);

            drawArrow(ctx, 16, ry + 11, r.angle(), color);
            ctx.drawTextWithShadow(c.textRenderer, Text.literal(r.name()), 32, ry + 1, 0xFFFFFFFF);
            ctx.drawTextWithShadow(c.textRenderer,
                    Text.literal(String.format(Locale.ROOT, "%.1f s", r.seconds())), PANEL_W - 52, ry + 1, color);
            ctx.drawTextWithShadow(c.textRenderer,
                    Text.literal(String.format(Locale.ROOT, "%.0f m", r.dist())), 32, ry + 11, 0xFFAAAAAA);

            // pasek postepu lotu
            int bx = 76;
            int bw = PANEL_W - 76 - 10;
            ctx.fill(bx, ry + 13, bx + bw, ry + 17, 0xFF1A2236);
            ctx.fill(bx, ry + 13, bx + (int) (bw * r.progress()), ry + 17, color);
        }
        m.pop();
    }
}
