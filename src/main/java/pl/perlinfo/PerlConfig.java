package pl.perlinfo;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Ustawienia i lista znajomych, zapisywane w config/perlinfo.properties. */
public final class PerlConfig {
    private PerlConfig() {
    }

    // 1. powiekszenie perly
    public static boolean enlarge = true;
    public static int enlargeSize = 300; // procent, 100 = 16 px
    // 2. tracery
    public static boolean tracers = true;
    // 3. strzalka na ekranie
    public static boolean arrow = true;
    public static int arrowPos = 1;      // 0-8, rzedami od lewego gornego rogu
    public static int arrowSize = 100;   // procent
    // 5. dzwiek
    public static boolean sound = true;
    public static int soundIndex = 0;    // 0-9
    public static int volume = 100;      // 0-150 procent
    // dodatki
    public static boolean landing = true;
    public static boolean showOwn = false;

    /** 4. znajomi: ich perly sa ignorowane. */
    public static final List<String> friends = new ArrayList<>();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("perlinfo.properties");
    }

    private static int num(Properties p, String key, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean bool(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)));
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(f)) {
                p.load(in);
            }
            enlarge = bool(p, "enlarge", true);
            enlargeSize = num(p, "enlargeSize", 300, 100, 800);
            tracers = bool(p, "tracers", true);
            arrow = bool(p, "arrow", true);
            arrowPos = num(p, "arrowPos", 1, 0, 8);
            arrowSize = num(p, "arrowSize", 100, 50, 200);
            sound = bool(p, "sound", true);
            soundIndex = num(p, "soundIndex", 0, 0, 9);
            volume = num(p, "volume", 100, 0, 150);
            landing = bool(p, "landing", true);
            showOwn = bool(p, "showOwn", false);

            friends.clear();
            String raw = p.getProperty("friends", "");
            for (String name : raw.split(",")) {
                String n = name.trim();
                if (!n.isEmpty()) friends.add(n);
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("enlarge", String.valueOf(enlarge));
            p.setProperty("enlargeSize", String.valueOf(enlargeSize));
            p.setProperty("tracers", String.valueOf(tracers));
            p.setProperty("arrow", String.valueOf(arrow));
            p.setProperty("arrowPos", String.valueOf(arrowPos));
            p.setProperty("arrowSize", String.valueOf(arrowSize));
            p.setProperty("sound", String.valueOf(sound));
            p.setProperty("soundIndex", String.valueOf(soundIndex));
            p.setProperty("volume", String.valueOf(volume));
            p.setProperty("landing", String.valueOf(landing));
            p.setProperty("showOwn", String.valueOf(showOwn));
            p.setProperty("friends", String.join(",", friends));
            try (OutputStream out = Files.newOutputStream(file())) {
                p.store(out, "PerlInfo");
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        String k = name.toLowerCase(Locale.ROOT);
        for (String f : friends) {
            if (f.toLowerCase(Locale.ROOT).equals(k)) return true;
        }
        return false;
    }

    public static boolean addFriend(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || isFriend(n)) return false;
        friends.add(n);
        save();
        return true;
    }

    public static void removeFriend(String name) {
        String k = name.toLowerCase(Locale.ROOT);
        friends.removeIf(f -> f.toLowerCase(Locale.ROOT).equals(k));
        save();
    }
}
