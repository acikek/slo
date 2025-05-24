package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ExtendedWorldCreationUiState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SelectServerTypeScreen extends Screen {

    public static final Component TITLE = Component.literal("Select Server Type");
    public static final ResourceLocation INTEGRATED_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");

    public Screen parent;
    public WorldCreationUiState creationState;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    public ServerTypeSelectionList selectionList;

    public SelectServerTypeScreen(Screen parent, WorldCreationUiState creationState) {
        super(TITLE);
        this.parent = parent;
        this.creationState = creationState;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(TITLE, font);
        selectionList = layout.addToContents(new ServerTypeSelectionList());
        var footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> updateAndClose()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, (button) -> onClose()).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        selectionList.updateSize(width, layout);
    }

    public void updateAndClose() {
        if (selectionList.getSelected() != null) {
            ((ExtendedWorldCreationUiState) creationState).slo$setPresetDirectory(selectionList.getSelected().directory);
        }
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    public class ServerTypeSelectionList extends ObjectSelectionList<ServerTypeSelectionList.Entry> {

        public ServerTypeSelectionList() {
            super(SelectServerTypeScreen.this.minecraft, SelectServerTypeScreen.this.width, SelectServerTypeScreen.this.layout.getContentHeight(), SelectServerTypeScreen.this.layout.getHeaderHeight(), 36);
            var selectedDirectory = ((ExtendedWorldCreationUiState) creationState).slo$presetDirectory();
            int integratedIndex = addEntry(new Entry(INTEGRATED_ICON, Component.literal("Integrated"), Component.literal("The default local server"), null));
            Slo.worldPresets.forEach((id, directory) -> {
                int entryIndex = addEntry(new Entry(id, directory));
                if (selectedDirectory == directory) {
                    setSelectedIndex(entryIndex);
                }
            });
            if (selectedDirectory == null) {
                setSelectedIndex(integratedIndex);
            }
        }

        @Override
        public int getRowWidth() {
            return 305;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {

            public ResourceLocation icon;
            public Component name;
            public Component description;
            public ExtendedLevelDirectory directory;

            public Entry(ResourceLocation icon, Component name, Component description, ExtendedLevelDirectory directory) {
                this.icon = icon;
                this.name = name;
                this.description = description;
                this.directory = directory;
            }

            public Entry(String id, ExtendedLevelDirectory directory) {
                this(directory.slo$loadIconTexture(), Component.translatableWithFallback("preset.slo." + id, directory.slo$directory().directoryName()), directory.slo$motd() != null ? Component.literal(directory.slo$motd()) : null, directory);
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.translatable("narrator.select", name);
            }

            @Override
            public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
                guiGraphics.blit(RenderType::guiTextured, icon, k, j, 0.0F, 0.0F, 32, 32, 32, 32);
                if (Minecraft.getInstance().options.touchscreen().get() || bl || getSelected() == this && isFocused()) {
                    guiGraphics.fill(k, j, k + 32, j + 32, -1601138544);
                }
                guiGraphics.drawString(minecraft.font, name, k + 32 + 2, j + 1, 0xFFFFFF);
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
