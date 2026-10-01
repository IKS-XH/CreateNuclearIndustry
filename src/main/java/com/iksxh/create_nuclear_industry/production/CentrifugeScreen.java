package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 无额外纹理的客户端状态面板；只绘制服务端菜单同步的数字和暂停原因。 */
public final class CentrifugeScreen extends AbstractContainerScreen<CentrifugeMenu> {
    public CentrifugeScreen(CentrifugeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xff20262a);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xff465457);
        graphics.fill(x + 7, y + 15, x + 169, y + 78, 0xff1b2528);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slotBackground(graphics, x + 7 + col * 18, y + 83 + row * 18);
        }
        for (int col = 0; col < 9; col++) slotBackground(graphics, x + 7 + col * 18, y + 141);
        slotBackground(graphics, x + 61, y + 34);
        slotBackground(graphics, x + 97, y + 34);
        graphics.fill(x + 8, y + 70, x + 168, y + 75, 0xff343a3d);
        graphics.fill(x + 8, y + 70, x + 8 + menu.status(4) * 160 / 1000, y + 75, 0xffc78d42);
    }

    private static void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xff0e1517);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xff8d9896);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xff253033);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 5, 0xfff3ead1, false);
        graphics.drawString(font, Component.translatable("gui.create_nuclear_industry.centrifuge.slurry", menu.status(0)), 8, 18, 0xffe7d9ba, false);
        graphics.drawString(font, Component.translatable("gui.create_nuclear_industry.centrifuge.water", menu.status(1)), 8, 29, 0xffe7d9ba, false);
        graphics.drawString(font, Component.translatable("gui.create_nuclear_industry.centrifuge.speed", menu.status(5)), 8, 52, 0xffe7d9ba, false);
        graphics.drawString(font, Component.translatable("gui.create_nuclear_industry.centrifuge.bearing", menu.status(6)), 88, 52, 0xffe7d9ba, false);
        String reason = CentrifugeBlockEntity.PauseReason.values()[Math.clamp(menu.status(7), 0,
                CentrifugeBlockEntity.PauseReason.values().length - 1)].name().toLowerCase();
        graphics.drawString(font, Component.translatable("gui.create_nuclear_industry.centrifuge.pause." + reason),
                8, 61, 0xffe7d9ba, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** 只在客户端注册屏幕，避免 dedicated server 解析客户端类型。 */
    @EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}
        @SubscribeEvent
        public static void onMenus(RegisterMenuScreensEvent event) {
            event.register(FuelProcessingContent.CENTRIFUGE_MENU.get(), CentrifugeScreen::new);
        }
    }
}
