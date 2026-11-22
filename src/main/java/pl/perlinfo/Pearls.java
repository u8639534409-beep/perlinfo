package pl.perlinfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sledzi perly w locie: kto rzucil, gdzie spadnie i za ile ticków. */
public final class Pearls {
    private Pearls() {
    }

    public static final class Pearl {
        public final EnderPearlEntity entity;
        public String owner = "Nieznany";
        public boolean resolved;
        public boolean ignored;
        public int age;
        public Vec3d landing;
        public int ticksLeft = 0;
        public int totalTicks = 0;

        Pearl(EnderPearlEntity entity) {
            this.entity = entity;
        }

        public double seconds() {
            return ticksLeft / 20.0;
        }

        public double progress() {
            if (totalTicks <= 0) return 0;
            return Math.max(0.0, Math.min(1.0, 1.0 - (double) ticksLeft / totalTicks));
        }
    }

    private static final Map<Integer, Pearl> map = new HashMap<>();

    public static void tick(MinecraftClient c) {
        if (c.world == null || c.player == null) {
            map.clear();
            return;
        }

        Set<Integer> present = new HashSet<>();
        for (Entity e : c.world.getEntities()) {
            if (!(e instanceof EnderPearlEntity pearl) || pearl.isRemoved()) continue;
            present.add(pearl.getId());

            Pearl p = map.get(pearl.getId());
            if (p == null) {
                p = new Pearl(pearl);
                map.put(pearl.getId(), p);
            }
            p.age++;
            if (!p.resolved) resolve(c, p);
            if (p.resolved && !p.ignored) simulate(c, p);
        }
        map.keySet().retainAll(present);
    }

    private static void resolve(MinecraftClient c, Pearl p) {
        Entity owner = p.entity.getOwner();
        if (owner == null && p.age < 5) return; // wlasciciel dociera chwile po spawnie

        p.resolved = true;
        p.owner = owner != null ? owner.getName().getString() : "Nieznany";
        boolean mine = owner == c.player;
        p.ignored = (mine && !PerlConfig.showOwn) || PerlConfig.isFriend(p.owner);

        if (!p.ignored && PerlConfig.sound) {
            PerlSounds.play();
        }
    }

    /** Symuluje lot perly (grawitacja 0.03, opor 0.99) az do uderzenia w blok. */
    private static void simulate(MinecraftClient c, Pearl p) {
        Vec3d pos = p.entity.getPos();
        Vec3d vel = p.entity.getVelocity();
        Vec3d landing = null;
        int ticks = 300;
        int voidY = c.world.getBottomY() - 16;

        for (int i = 0; i < 300; i++) {
            Vec3d next = pos.add(vel);
            BlockHitResult hit = c.world.raycast(new RaycastContext(
                    pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p.entity));
            if (hit.getType() != HitResult.Type.MISS) {
                landing = hit.getPos();
                ticks = i + 1;
                break;
            }
            pos = next;
            vel = vel.multiply(0.99).add(0, -0.03, 0);
            if (pos.y < voidY) {
                landing = pos;
                ticks = i + 1;
                break;
            }
        }

        p.landing = landing;
        p.ticksLeft = ticks;
        if (p.totalTicks == 0 && landing != null) {
            p.totalTicks = ticks;
        }
    }

    /** Perly do pokazania (od tej, ktora spadnie najszybciej). */
    public static List<Pearl> active() {
        List<Pearl> list = new ArrayList<>();
        for (Pearl p : map.values()) {
            if (p.resolved && !p.ignored) list.add(p);
        }
        list.sort(Comparator.comparingInt(p -> p.ticksLeft));
        return list;
    }
}
