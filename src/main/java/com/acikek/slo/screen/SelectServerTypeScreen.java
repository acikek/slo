package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ExtendedLevelDirectory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class SelectServerTypeScreen extends Screen {

    public static final Component TITLE = Component.literal("Select Server Type");

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
            Slo.worldPresets.forEach((id, directory) -> addEntry(new Entry(id, directory)));
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {

            public String id;
            public ExtendedLevelDirectory directory;

            public Entry(String id, ExtendedLevelDirectory directory) {
                this.id = id;
                this.directory = directory;
                directory.slo$loadIconTexture();
            }

            @Override
            public Component getNarration() {
                return null;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
                if (directory.slo$iconTexture() != null) {
                    guiGraphics.blit(RenderType::guiTextured, directory.slo$iconTexture(), k, j, 0.0F, 0.0F, 32, 32, 32, 32);
                }
                /*guiGraphics.blit(RenderType::guiTextured, directory., k, j, 0.0F, 0.0F, 32, 32, 32, 32);
                FormattedCharSequence formattedCharSequence = this.nameDisplayCache;
                MultiLineLabel multiLineLabel = this.descriptionDisplayCache;
                if (this.showHoverOverlay() && ((Boolean)this.minecraft.options.touchscreen().get() || bl || this.parent.getSelected() == this && this.parent.isFocused())) {
                    guiGraphics.fill(k, j, k + 32, j + 32, -1601138544);
                    int q = n - k;
                    int r = o - j;
                    if (!this.pack.getCompatibility().isCompatible()) {
                        formattedCharSequence = this.incompatibleNameDisplayCache;
                        multiLineLabel = this.incompatibleDescriptionDisplayCache;
                    }

                    if (this.pack.canSelect()) {
                        if (q < 32) {
                            guiGraphics.blitSprite(RenderType::guiTextured, TransferableSelectionList.SELECT_HIGHLIGHTED_SPRITE, k, j, 32, 32);
                        } else {
                            guiGraphics.blitSprite(RenderType::guiTextured, TransferableSelectionList.SELECT_SPRITE, k, j, 32, 32);
                        }*/
                guiGraphics.drawCenteredString(SelectServerTypeScreen.this.font, id, k + l / 2, j + m / 2 - SelectServerTypeScreen.this.font.lineHeight / 2, 0xFFFFFF);
            }
        }
    }
}
