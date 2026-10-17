package dev.yogur.toolassist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class SpeedSettingsScreen extends Screen {
    private final Screen parent;
    private SpeedSlider slider;
    public SpeedSettingsScreen(Screen parent) { super(Text.literal("YogurToolAssist 设置")); this.parent = parent; }
    @Override protected void init() {
        slider = addDrawableChild(new SpeedSlider(width / 2 - 110, height / 2 - 15, 220, 20));
        slider.refresh();
        addDrawableChild(ButtonWidget.builder(Text.literal("完成"), button -> close())
            .dimensions(width / 2 - 110, height / 2 + 20, 220, 20).build());
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (slider != null) slider.refresh();
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 30, 0xffffffff);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("声音与附魔光效倍率（0.1–2.0）"), width / 2, 55, 0xffcccccc);
        if (YogurToolAssist.isServerForced()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("服务器已强制倍率，暂时无法修改"), width / 2, 75, 0xffffaa00);
        }
    }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    private static final class SpeedSlider extends SliderWidget {
        SpeedSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), (YogurToolAssist.soundSpeed() - 0.1) / 1.9);
            updateMessage();
        }
        private void refresh() {
            active = !YogurToolAssist.isServerForced();
            value = (YogurToolAssist.soundSpeed() - 0.1) / 1.9;
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Text.literal("声音与附魔光效倍率：" + String.format(java.util.Locale.ROOT, "%.2f", 0.1 + value * 1.9)));
        }
        @Override protected void applyValue() { YogurToolAssist.setSpeed((float) (0.1 + value * 1.9)); }
    }
}
