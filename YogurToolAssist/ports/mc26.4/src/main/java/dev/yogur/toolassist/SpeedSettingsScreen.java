package dev.yogur.toolassist;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public final class SpeedSettingsScreen extends Screen {
    private final Screen parent;
    private SpeedSlider slider;
    public SpeedSettingsScreen(Screen parent) { super(Component.literal("YogurToolAssist 设置")); this.parent = parent; }
    @Override protected void init() {
        slider = addRenderableWidget(new SpeedSlider(width / 2 - 110, height / 2 - 15, 220, 20));
        slider.refresh();
        addRenderableWidget(Button.builder(Component.literal("完成"), button -> onClose())
            .bounds(width / 2 - 110, height / 2 + 20, 220, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor context) {
        if (slider != null) slider.refresh();
        extractBackground(context);
        super.extractRenderState(context);
        context.centeredText(font, title, width / 2, 30, 0xffffffff);
        context.centeredText(font, Component.literal("声音与附魔光效倍率（0.1–2.0）"), width / 2, 55, 0xffcccccc);
        if (YogurToolAssist.isServerForced()) {
            context.centeredText(font, Component.literal("服务器已强制倍率，暂时无法修改"), width / 2, 75, 0xffffaa00);
        }
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    private static final class SpeedSlider extends AbstractSliderButton {
        SpeedSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), (YogurToolAssist.soundSpeed() - 0.1) / 1.9);
            updateMessage();
        }
        private void refresh() {
            active = !YogurToolAssist.isServerForced();
            value = (YogurToolAssist.soundSpeed() - 0.1) / 1.9;
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Component.literal("声音与附魔光效倍率：" + String.format(java.util.Locale.ROOT, "%.2f", 0.1 + value * 1.9)));
        }
        @Override protected void applyValue() { YogurToolAssist.setSpeed((float) (0.1 + value * 1.9)); }
    }
}
