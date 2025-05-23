package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ExtendedLevelDirectory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SelectServerTypeScreen extends Screen {

    public static final Component TITLE = Component.literal("Select Server Type");
    public static final ResourceLocation INTEGRATED_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    public ServerTypeSelectionList selectionList;

    public SelectServerTypeScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        selectionList = addRenderableWidget(new ServerTypeSelectionList());
        layout.addTitleHeader(TITLE, font);
        layout.addToContents(new ServerTypeSelectionList());
        var footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> {}).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, (button) -> onClose()).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        selectionList.updateSize(width, layout);
    }

    public class ServerTypeSelectionList extends ObjectSelectionList<ServerTypeSelectionList.Entry> {

        public ServerTypeSelectionList() {
            super(SelectServerTypeScreen.this.minecraft, SelectServerTypeScreen.this.width, SelectServerTypeScreen.this.layout.getContentHeight(), SelectServerTypeScreen.this.layout.getHeaderHeight(), 36);
            addEntry(new Entry(INTEGRATED_ICON, Component.literal("Integrated"), Component.literal("The default Minecraft server")));
            Slo.worldPresets.forEach((id, directory) -> addEntry(new Entry(id, directory)));
        }

        @Override
        public int getRowWidth() {
            return 305;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {

            public ResourceLocation icon;
            public Component name;
            public Component description;

            public Entry(ResourceLocation icon, Component name, Component description) {
                this.icon = icon;
                this.name = name;
                this.description = description;
            }

            public Entry(String id, ExtendedLevelDirectory directory) {
                this(directory.slo$loadIconTexture(), Component.translatableWithFallback("preset.slo." + id, directory.slo$directory().directoryName()), directory.slo$motd() != null ? Component.literal(directory.slo$motd()) : null);
            }

            @Override
            public Component getNarration() {
                return null;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
                guiGraphics.blit(RenderType::guiTextured, icon, k, j, 0.0F, 0.0F, 32, 32, 32, 32);
                if (Minecraft.getInstance().options.touchscreen().get() || bl || getSelected() == this && isFocused()) {
                    guiGraphics.fill(k, j, k + 32, j + 32, -1601138544);
                }
                guiGraphics.drawString(SelectServerTypeScreen.this.font, name, k + 32 + 2, j + 1, 0xFFFFFF);
                if (description == null) {
                    return;
                }
                var lines = minecraft.font.split(description, l - 32 - 2);
                for (int index = 0; index < Math.min(lines.size(), 2); index++) {
                    guiGraphics.drawString(minecraft.font, lines.get(index), k + 32 + 2, j + 9 * (index + 1) + 3, -8355712);
                }
            }
        }
    }
}
