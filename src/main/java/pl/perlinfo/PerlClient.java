package pl.perlinfo;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PerlClient implements ClientModInitializer {

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        PerlConfig.load();

        // Domyslny klawisz: H (zmienisz w Opcje -> Sterowanie).
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.perlinfo.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.perlinfo"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new PerlScreen());
                }
            }
            Pearls.tick(client);
        });

        // /perlinfo friend add NICK  oraz  /perlinfo friend remove NICK
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("perlinfo")
                        .then(literal("friend")
                                .then(literal("add").then(argument("nick", StringArgumentType.word()).executes(ctx -> {
                                    String nick = StringArgumentType.getString(ctx, "nick");
                                    boolean ok = PerlConfig.addFriend(nick);
                                    ctx.getSource().sendFeedback(Text.literal(ok
                                            ? "[PerlInfo] Dodano znajomego: " + nick
                                            : "[PerlInfo] Już jest na liście: " + nick)
                                            .formatted(ok ? Formatting.GREEN : Formatting.RED));
                                    return ok ? 1 : 0;
                                })))
                                .then(literal("remove").then(argument("nick", StringArgumentType.word()).executes(ctx -> {
                                    String nick = StringArgumentType.getString(ctx, "nick");
                                    PerlConfig.removeFriend(nick);
                                    ctx.getSource().sendFeedback(Text.literal("[PerlInfo] Usunięto: " + nick)
                                            .formatted(Formatting.YELLOW));
                                    return 1;
                                }))))));

        HudRenderCallback.EVENT.register((context, tickCounter) -> PerlHud.render(context));
    }
}
