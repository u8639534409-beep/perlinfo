package pl.perlinfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** Rzutowanie punktu ze swiata na ekran (HUD). */
public final class Proj {
    private Proj() {
    }

    /** x,y: pozycja na ekranie (przycieta do krawedzi), front: przed kamera, onScreen: widoczny. */
    public record P(float x, float y, boolean front, boolean onScreen) {
    }

    public static P project(MinecraftClient c, Vec3d target, int w, int h) {
        Camera cam = c.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());

        Vec3d fwd = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d up = right.crossProduct(fwd);

        double tanHalf = Math.tan(Math.toRadians(c.options.getFov().getValue()) / 2.0);
        double aspect = (double) w / h;

        Vec3d d = target.subtract(camPos);
        double x = d.dotProduct(right);
        double y = d.dotProduct(up);
        double z = d.dotProduct(fwd);

        double ndcX;
        double ndcY;
        boolean front = z > 0.1;
        if (front) {
            ndcX = x / (z * tanHalf * aspect);
            ndcY = y / (z * tanHalf);
        } else {
            double len = Math.hypot(x, y);
            if (len < 1e-4) {
                x = 0;
                y = -1;
                len = 1;
            }
            ndcX = x / len * 10;
            ndcY = y / len * 10;
        }

        boolean onScreen = front && Math.abs(ndcX) <= 1 && Math.abs(ndcY) <= 1;
        double m = Math.max(Math.abs(ndcX), Math.abs(ndcY));
        if (m > 1) {
            ndcX /= m;
            ndcY /= m;
        }
        float sx = (float) (w / 2.0 * (1 + ndcX * 0.95));
        float sy = (float) (h / 2.0 * (1 - ndcY * 0.9));
        return new P(sx, sy, front, onScreen);
    }
}
