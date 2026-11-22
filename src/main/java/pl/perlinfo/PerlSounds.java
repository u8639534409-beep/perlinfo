package pl.perlinfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.List;

/** 10 dzwiekow do wyboru; glosnosc 0-150%. */
public final class PerlSounds {
    private PerlSounds() {
    }

    public record Option(String name, SoundEvent event) {
    }

    private static Option opt(String name, String id) {
        return new Option(name, SoundEvent.of(Identifier.ofVanilla(id)));
    }

    public static final List<Option> OPTIONS = List.of(
            opt("Dzwon", "block.bell.use"),
            opt("Enderman", "entity.enderman.teleport"),
            opt("Ostrzeżenie Wardena", "entity.warden.nearby_close"),
            opt("Grzmot", "entity.lightning_bolt.thunder"),
            opt("Ghast", "entity.ghast.warn"),
            opt("Klątwa Strażnika", "entity.elder_guardian.curse"),
            opt("Smok Endu", "entity.ender_dragon.growl"),
            opt("Kowadło", "block.anvil.land"),
            opt("Strzała", "entity.arrow.hit_player"),
            opt("Poziom", "entity.player.levelup")
    );

    public static void play() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null || c.getSoundManager() == null) return;
        int i = Math.max(0, Math.min(OPTIONS.size() - 1, PerlConfig.soundIndex));
        float vol = PerlConfig.volume / 100f;
        if (vol <= 0f) return;
        c.getSoundManager().play(PositionedSoundInstance.master(OPTIONS.get(i).event(), 1.0f, vol));
    }
}
