package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.google.common.collect.EvictingQueue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ServerConsoleScreen extends Screen {

    private final Queue<String> queue = EvictingQueue.create(100);
    private double scrollAmount;
    private boolean scrollMax;
    private CommandSuggestions suggestions;

    public Output output;
    public EditBox input;

    public ServerConsoleScreen() {
        super(GameNarrator.NO_TITLE);
    }

    @Override
    protected void init() {
        output = addRenderableWidget(new Output());
        input = addRenderableWidget(new EditBox(minecraft.fontFilterFishy, 20, height - 30, width - 40, 20, Component.empty()) {
            @Override
            protected @NotNull MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(ServerConsoleScreen.this.suggestions.getNarrationMessage());
            }
        });
        input.setHint(Component.literal("Enter a command..."));
        suggestions = new CommandSuggestions(minecraft, this, input, font, true, true, 0, 7, false, -805306368);
        suggestions.setAllowHiding(false);
        suggestions.updateCommandInfo();
        input.setResponder(string -> suggestions.updateCommandInfo());
        setInitialFocus(input);
    }

    public void addLine(String line) {
        queue.add(line);
        if (output != null) {
            output.addLine(line);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderTransparentBackground(guiGraphics);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_ESCAPE && input.isFocused()) {
            input.setFocused(false);
            return true;
        }
        if (suggestions.keyPressed(i, j, k)) {
            return true;
        }
        else if (i == 257) {
            if (!input.isFocused()) {
                setFocused(input);
                input.active = true;
                input.setEditable(true);
                return true;
            }
            else if (input.isFocused() && !input.getValue().isEmpty()) {
                var stdin = Slo.serverProcess.getOutputStream();
                var writer = new BufferedWriter(new OutputStreamWriter(stdin));
                try {
                    writer.write(input.getValue() + "\n");
                    writer.flush();
                } catch (IOException e) {
                    Slo.LOGGER.error("Failed to write to server output", e);
                }
                input.setValue("");
                return true;
            }
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        g = Mth.clamp(g, -1.0, 1.0);
        if (suggestions.mouseScrolled(g)) {
            return true;
        }
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (suggestions.mouseClicked((int) d, (int) e, i)) {
            return true;
        }
        return super.mouseClicked(d, e, i);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        //suggestions.render(guiGraphics, i + 20, j + 20);
    }

    @Override
    public void onClose() {
        scrollAmount = output.scrollAmount();
        scrollMax = scrollAmount == output.maxScrollAmount();
        super.onClose();
    }

    public class Output extends AbstractContainerWidget {

        public final List<AbstractWidget> widgets = new ArrayList<>();
        public int widgetsHeight;

        public Output() {
            super(20, 10, ServerConsoleScreen.this.width - 40, ServerConsoleScreen.this.height - 50, CommonComponents.EMPTY);
            for (var line : queue) {
                addLine(line);
            }
            setScrollAmount(scrollMax ? maxScrollAmount() : scrollAmount);
        }

        public void addLine(String line) {
            boolean max = scrollAmount() == maxScrollAmount();
            var widget = new MultiLineTextWidget(Component.literal(line), font);
            widget.setPosition(getX() + 3, getY() + widgetsHeight + 3);
            widget.setMaxWidth(getWidth() + 17 - getX());
            widgets.add(widget);
            widgetsHeight += widget.getHeight();
            if (max) {
                setScrollAmount(maxScrollAmount());
            }
        }

        @Override
        protected int contentHeight() {
            return widgetsHeight + 5;
        }

        @Override
        protected double scrollRate() {
            return 10.0;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            guiGraphics.fill(getX(), getY(), getWidth() + 20, getHeight() + 10, -16777216);
            guiGraphics.enableScissor(getX(), getY() + 3, getWidth() + 20 - 3, getHeight() + 10 - 3);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0, -scrollAmount(), 0.0);
            var copy = new ArrayList<>(widgets);
            for (AbstractWidget line : copy) {
                line.render(guiGraphics, i, j, f);
            }
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
            renderScrollbar(guiGraphics);
            guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), isFocused() ? -1 : -6250336);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return widgets;
        }
    }
}
