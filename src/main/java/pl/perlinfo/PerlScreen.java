package pl.perlinfo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Uuids;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Ustawienia (klawisz H): zakladka Opcje i zakladka Znajomi. */
public class PerlScreen extends Screen {

    private static final int PW = 400;
    private static final int PH = 304;

    private static final int TEAL = 0xFF2EE6C8;
    private static final int VIOLET = 0xFF9B6DFF;
    private static final int PINK = 0xFFFF4D6D;

    private record Hit(int x1, int y1, int x2, int y2, boolean inList, Runnable action) {
    }

    private record SliderDef(int x, int y, int w, int h, int min, int max,
                             IntSupplier get, IntConsumer set, String label, String suffix) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<SliderDef> sliders = new ArrayList<>();
    private SliderDef dragging;

    private int tab = 0; // 0 = Opcje, 1 = Znajomi
    private double scroll = 0;
    private int listTop;
    private int listBottom;
    private TextFieldWidget nameField;
    private String message = "";

    public PerlScreen() {
        super(Text.literal("PerlInfo"));
    }

    @Override
    protected void init() {
        int left = (width - PW) / 2;
        int top = (height - PH) / 2;
        nameField = new TextFieldWidget(textRenderer, left + 12, top + PH - 58, 250, 18, Text.literal("Nick"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Nick znajomego").formatted(Formatting.DARK_GRAY));
        addDrawableChild(nameField);
    }

    private SkinTextures skinOf(String name) {
        if (client != null && client.getNetworkHandler() != null) {
            PlayerListEntry e = client.getNetworkHandler().getPlayerListEntry(name);
            if (e != null) return e.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(name));
    }

    private void addFriend() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        if (client != null && client.getNetworkHandler() != null) {
            PlayerListEntry e = client.getNetworkHandler().getPlayerListEntry(name);
            if (e != null) name = e.getProfile().getName();
        }
        if (PerlConfig.addFriend(name)) {
            message = "Dodano: " + name;
            nameField.setText("");
        } else {
            message = "Już jest na liście albo nick jest pusty.";
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        hits.clear();
        sliders.clear();

        int left = (width - PW) / 2;
        int top = (height - PH) / 2;
        long now = System.currentTimeMillis();
        double pulse = 0.5 + 0.5 * Math.sin(now / 400.0);

        ctx.fillGradient(left, top, left + PW, top + PH, 0xF0141B2E, 0xF0070A14);
        int ba = 140 + (int) (100 * pulse);
        ctx.drawBorder(left, top, PW, PH, (ba << 24) | 0x9B6DFF);
        ctx.drawBorder(left + 2, top + 2, PW - 4, PH - 4, 0x22FFFFFF);
        ctx.fill(left, top, left + 14, top + 2, TEAL);
        ctx.fill(left, top, left + 2, top + 14, TEAL);
        ctx.fill(left + PW - 14, top + PH - 2, left + PW, top + PH, TEAL);
        ctx.fill(left + PW - 2, top + PH - 14, left + PW, top + PH, TEAL);

        ctx.drawItem(new ItemStack(Items.ENDER_PEARL), left + PW / 2 - 56, top + 5);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("PERLINFO").formatted(Formatting.BOLD), left + PW / 2 + 6, top + 10, TEAL);

        button(ctx, left + 12, top + 26, 90, 16, "Opcje", tab == 0, mouseX, mouseY, false, () -> {
            tab = 0;
            nameField.setFocused(false);
        });
        button(ctx, left + 106, top + 26, 90, 16, "Znajomi (" + PerlConfig.friends.size() + ")", tab == 1,
                mouseX, mouseY, false, () -> {
                    tab = 1;
                    scroll = 0;
                });

        nameField.visible = tab == 1;
        if (tab == 0) {
            renderOptions(ctx, left, top, mouseX, mouseY);
        } else {
            renderFriends(ctx, left, top, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ------------------------------------------------------------ OPCJE
    private void renderOptions(DrawContext ctx, int left, int top, int mouseX, int mouseY) {
        label(ctx, left + 12, top + 48, "OPCJE");

        toggle(ctx, left + 12, top + 60, 186, 18, "1. Powiększ perłę", PerlConfig.enlarge, mouseX, mouseY,
                () -> PerlConfig.enlarge = !PerlConfig.enlarge);
        toggle(ctx, left + 12, top + 82, 186, 18, "2. Tracersy do perły", PerlConfig.tracers, mouseX, mouseY,
                () -> PerlConfig.tracers = !PerlConfig.tracers);
        toggle(ctx, left + 12, top + 104, 186, 18, "5. Dźwięk przy rzucie", PerlConfig.sound, mouseX, mouseY,
                () -> PerlConfig.sound = !PerlConfig.sound);
        toggle(ctx, left + 202, top + 60, 186, 18, "3. Strzałka na ekranie", PerlConfig.arrow, mouseX, mouseY,
                () -> PerlConfig.arrow = !PerlConfig.arrow);
        toggle(ctx, left + 202, top + 82, 186, 18, "Znacznik lądowania", PerlConfig.landing, mouseX, mouseY,
                () -> PerlConfig.landing = !PerlConfig.landing);
        toggle(ctx, left + 202, top + 104, 186, 18, "Pokaż też moje perły", PerlConfig.showOwn, mouseX, mouseY,
                () -> PerlConfig.showOwn = !PerlConfig.showOwn);

        slider(ctx, left + 12, top + 128, 186, 16, 100, 800, () -> PerlConfig.enlargeSize,
                v -> PerlConfig.enlargeSize = v, "Rozmiar perły", "%");
        slider(ctx, left + 12, top + 148, 186, 16, 0, 150, () -> PerlConfig.volume,
                v -> PerlConfig.volume = v, "Głośność (cicho - głośno)", "%");
        slider(ctx, left + 202, top + 128, 186, 16, 50, 200, () -> PerlConfig.arrowSize,
                v -> PerlConfig.arrowSize = v, "Rozmiar strzałki", "%");
        button(ctx, left + 202, top + 148, 186, 16, "Podgląd alarmu", false, mouseX, mouseY, false, () -> {
            PerlHud.startDemo();
            PerlSounds.play();
        });

        label(ctx, left + 12, top + 170, "DŹWIĘK (kliknij, aby wybrać)");
        for (int i = 0; i < PerlSounds.OPTIONS.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = left + 12 + col * 94;
            int by = top + 182 + row * 17;
            int idx = i;
            button(ctx, bx, by, 90, 15, (i + 1) + ". " + PerlSounds.OPTIONS.get(i).name(),
                    PerlConfig.soundIndex == i, mouseX, mouseY, false, () -> {
                        PerlConfig.soundIndex = idx;
                        PerlConfig.save();
                        PerlSounds.play();
                    });
        }

        label(ctx, left + 202, top + 170, "POZYCJA STRZAŁKI NA EKRANIE");
        int px = left + 202;
        int py = top + 182;
        int pw = 186;
        int ph = 83;
        ctx.fill(px, py, px + pw, py + ph, 0xFF05080F);
        ctx.drawBorder(px, py, pw, ph, 0x889B6DFF);
        int cw = pw / 3;
        int ch = ph / 3;
        for (int i = 0; i < 9; i++) {
            int cx = px + (i % 3) * cw;
            int cy = py + (i / 3) * ch;
            boolean sel = PerlConfig.arrowPos == i;
            boolean hover = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch;
            if (sel) {
                ctx.fill(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 0x442EE6C8);
            } else if (hover) {
                ctx.fill(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 0x229B6DFF);
            }
            ctx.drawBorder(cx, cy, cw, ch, 0x33FFFFFF);
            if (sel) {
                float f = 0.7f + (PerlConfig.arrowSize - 50) / 150f;
                MatrixStack m = ctx.getMatrices();
                m.push();
                m.translate(cx + cw / 2f, cy + ch / 2f, 0);
                m.scale(f, f, 1f);
                PerlHud.drawArrow(ctx, 0, 0, 0.6, TEAL);
                m.pop();
            }
            int idx = i;
            hits.add(new Hit(cx, cy, cx + cw, cy + ch, false, () -> {
                PerlConfig.arrowPos = idx;
                PerlConfig.save();
            }));
        }

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Ustawienia zapisują się automatycznie  |  ESC zamyka"),
                left + PW / 2, top + PH - 16, 0xFF4B5470);
    }

    // ------------------------------------------------------------ ZNAJOMI
    private void renderFriends(DrawContext ctx, int left, int top, int mouseX, int mouseY) {
        label(ctx, left + 12, top + 48, "ZNAJOMI - ich perły są ignorowane we wszystkich opcjach");

        int lx1 = left + 12;
        int lx2 = left + PW - 12;
        listTop = top + 60;
        listBottom = top + PH - 66;

        List<String> friends = new ArrayList<>(PerlConfig.friends);
        int rowH = 28;
        int total = friends.size() * rowH;
        int viewH = listBottom - listTop;
        double maxScroll = Math.max(0, total - viewH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        ctx.fill(lx1, listTop, lx2, listBottom, 0x55000000);
        if (friends.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Brak znajomych. Dodaj nick poniżej.").formatted(Formatting.GRAY),
                    (lx1 + lx2) / 2, listTop + viewH / 2 - 4, 0xFFAAAAAA);
        }

        ctx.enableScissor(lx1, listTop, lx2, listBottom);
        for (int i = 0; i < friends.size(); i++) {
            int ry = listTop + i * rowH - (int) scroll;
            if (ry + rowH < listTop || ry > listBottom) continue;
            String name = friends.get(i);

            ctx.fill(lx1 + 1, ry + 1, lx2 - 1, ry + rowH - 1, 0x33FFFFFF);
            PlayerSkinDrawer.draw(ctx, skinOf(name), lx1 + 4, ry + 3, 22);
            ctx.drawTextWithShadow(textRenderer, Text.literal(name).formatted(Formatting.GREEN, Formatting.BOLD),
                    lx1 + 32, ry + 10, 0xFFFFFFFF);
            button(ctx, lx2 - 28, ry + 4, 22, 20, "X", false, mouseX, mouseY, true,
                    () -> PerlConfig.removeFriend(name));
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(12, (int) (viewH * (viewH / (double) total)));
            int barY = listTop + (int) ((viewH - barH) * (scroll / maxScroll));
            ctx.fill(lx2 - 3, barY, lx2 - 1, barY + barH, 0xAAFFFFFF);
        }

        button(ctx, left + 268, top + PH - 58, 120, 18, "Dodaj", false, mouseX, mouseY, false, this::addFriend);
        if (!message.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(message, 370))
                    .formatted(Formatting.GRAY), left + 14, top + PH - 36, 0xFFAAAAAA);
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Lista zapisuje się automatycznie  |  ESC zamyka"),
                left + PW / 2, top + PH - 16, 0xFF4B5470);
    }

    // ------------------------------------------------------------ widgety
    private void label(DrawContext ctx, int x, int y, String text) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(text), x, y, VIOLET);
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String text, boolean selected,
                        int mouseX, int mouseY, boolean inList, Runnable action) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h
                && (!inList || (mouseY >= listTop && mouseY < listBottom));
        int bg = selected ? 0xFF1B4F55 : 0xFF1C2238;
        if (hover) bg = selected ? 0xFF26666E : 0xFF2B3354;
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawBorder(x, y, w, h, selected ? TEAL : 0xFF39446A);
        String shown = textRenderer.trimToWidth(text, w - 6);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(shown), x + w / 2, y + (h - 8) / 2,
                selected ? 0xFFFFFFFF : 0xFFC9D2F0);
        hits.add(new Hit(x, y, x + w, y + h, inList, action));
    }

    private void toggle(DrawContext ctx, int x, int y, int w, int h, String text, boolean on,
                        int mouseX, int mouseY, Runnable action) {
        Runnable wrapped = () -> {
            action.run();
            PerlConfig.save();
        };
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = on ? 0xFF1B4F55 : 0xFF3A1A2A;
        if (hover) bg = on ? 0xFF26666E : 0xFF52243B;
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawBorder(x, y, w, h, on ? TEAL : PINK);
        ctx.fill(x + 4, y + h / 2 - 2, x + 8, y + h / 2 + 2, on ? TEAL : PINK);
        String shown = textRenderer.trimToWidth(text, w - 16);
        ctx.drawTextWithShadow(textRenderer, Text.literal(shown), x + 12, y + (h - 8) / 2, 0xFFFFFFFF);
        hits.add(new Hit(x, y, x + w, y + h, false, wrapped));
    }

    private void slider(DrawContext ctx, int x, int y, int w, int h, int min, int max,
                        IntSupplier get, IntConsumer set, String label, String suffix) {
        SliderDef def = new SliderDef(x, y, w, h, min, max, get, set, label, suffix);
        sliders.add(def);
        int v = get.getAsInt();
        double ratio = (v - min) / (double) (max - min);
        ctx.fill(x, y, x + w, y + h, 0xFF0F1526);
        ctx.fill(x, y, x + (int) (w * ratio), y + h, 0xFF2B8F94);
        ctx.drawBorder(x, y, w, h, dragging == def ? TEAL : 0xFF39446A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label + ": " + v + suffix),
                x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private void applySlider(SliderDef def, double mouseX) {
        double ratio = Math.max(0.0, Math.min(1.0, (mouseX - def.x()) / def.w()));
        def.set().accept(def.min() + (int) Math.round(ratio * (def.max() - def.min())));
    }

    // ------------------------------------------------------------ wejscie
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (SliderDef def : sliders) {
                if (mouseX >= def.x() && mouseX < def.x() + def.w()
                        && mouseY >= def.y() && mouseY < def.y() + def.h()) {
                    dragging = def;
                    applySlider(def, mouseX);
                    return true;
                }
            }
            for (Hit hit : hits) {
                boolean inside = mouseX >= hit.x1() && mouseX < hit.x2() && mouseY >= hit.y1() && mouseY < hit.y2();
                if (!inside) continue;
                if (hit.inList() && (mouseY < listTop || mouseY >= listBottom)) continue;
                hit.action().run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            applySlider(dragging, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            PerlConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 18;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && tab == 1 && nameField != null && nameField.isFocused()) {
            addFriend();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        PerlConfig.save();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
