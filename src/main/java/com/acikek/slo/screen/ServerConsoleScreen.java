package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.google.common.collect.EvictingQueue;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ServerConsoleScreen extends Screen {

	public static final Component ENTER_COMMAND = Component.translatable("gui.slo.serverConsole.enterCommand");

	private final Queue<String> logQueue = EvictingQueue.create(256);
	private final List<String> messages = new ArrayList<>();
	private int messagePos = -1;
	private double scrollAmount;
	private boolean scrollMax = true;
	private CommandSuggestions suggestions;

	public Output output;
	public EditBox input;

	public ServerConsoleScreen() {
		super(GameNarrator.NO_TITLE);
	}

	@Override
	protected void init() {
		messagePos = messages.size();
		output = addRenderableWidget(new Output());
		input = addRenderableWidget(new EditBox(minecraft.fontFilterFishy, 20, height - 30, width - 40, 20, Component.empty()) {
			@Override
			protected @NotNull MutableComponent createNarrationMessage() {
				return super.createNarrationMessage().append(ServerConsoleScreen.this.suggestions.getNarrationMessage());
			}
		});
		input.setHint(ENTER_COMMAND);
		suggestions = new CommandSuggestions(minecraft, this, input, font, true, true, 0, 7, false, -805306368);
		suggestions.setAllowSuggestions(false);
		suggestions.updateCommandInfo();
		input.setResponder(string -> {
			suggestions.setAllowSuggestions(true);
			suggestions.updateCommandInfo();
		});
	}

	public void addLine(String line) {
		logQueue.add(line);
		if (output != null) {
			output.addLine(line);
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		renderBackground(guiGraphics);
		super.render(guiGraphics, i, j, f);
	}

	@Override
	public boolean keyPressed(int i, int j, int k) {
		if (output.isFocused() && i == 264 && output.scrollAmount() == output.getMaxScrollAmount()) {
			return true;
		}
		if (input.isFocused() && !input.getValue().isEmpty() && suggestions.keyPressed(i, j, k)) {
			return true;
		}
		if (input.isFocused()) {
			if (i == 265) {
				moveMessagePos(-1);
				return true;
			}
			else if (i == 264) {
				moveMessagePos(1);
				return true;
			}
		}
		if (i == 257) {
			if (!input.isFocused()) {
				setFocused(input);
				input.setEditable(true);
				return true;
			}
			else if (input.isFocused() && !input.getValue().isEmpty()) {
				var stdin = Slo.serverProcess.getOutputStream();
				var writer = new BufferedWriter(new OutputStreamWriter(stdin));
				try {
					writer.write(input.getValue() + "\n");
					writer.flush();
				}
				catch (IOException e) {
					Slo.LOGGER.error("Failed to write to server output", e);
				}
				addLine("> " + input.getValue());
				messages.add(input.getValue());
				messagePos = messages.size();
				input.setValue("");
				return true;
			}
		}
		return super.keyPressed(i, j, k);
	}

	@Override
	protected void changeFocus(ComponentPath componentPath) {
		super.changeFocus(componentPath);
		suggestions.setAllowSuggestions(false);
		suggestions.updateCommandInfo();
	}

	public void moveMessagePos(int d) {
		int newPos = messagePos + d;
		if (newPos < 0 || newPos >= messages.size()) {
			return;
		}
		messagePos = newPos;
		input.setValue(messages.get(messagePos));
	}

	@Override
	public void onClose() {
		scrollAmount = output.scrollAmount();
		scrollMax = scrollAmount == output.getMaxScrollAmount();
		super.onClose();
	}

	public class Output extends AbstractScrollWidget {

		public final List<AbstractWidget> widgets = new ArrayList<>();
		public int widgetsHeight;

		public Output() {
			super(20, 10, ServerConsoleScreen.this.width - 40, ServerConsoleScreen.this.height - 50, CommonComponents.EMPTY);
			for (var line : logQueue) {
				addLine(line);
			}
			setScrollAmount(scrollMax ? getMaxScrollAmount() : scrollAmount);
		}

		public void addLine(String line) {
			boolean max = scrollAmount() == getMaxScrollAmount();
			var widget = new MultiLineTextWidget(Component.literal(line), font);
			widget.setPosition(getX() + 3, getY() + widgetsHeight + 3);
			widget.setMaxWidth(getWidth() + 17 - getX());
			widgets.add(widget);
			widgetsHeight += widget.getHeight();
			if (max) {
				setScrollAmount(getMaxScrollAmount());
			}
		}

		@Override
		public double scrollAmount() {
			return super.scrollAmount();
		}

		@Override
		public int getMaxScrollAmount() {
			return super.getMaxScrollAmount();
		}

		@Override
		protected int getInnerHeight() {
			return widgetsHeight;
		}

		@Override
		protected double scrollRate() {
			return 10.0;
		}

		@Override
		protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
			var copy = new ArrayList<>(widgets);
			for (AbstractWidget line : copy) {
				line.render(guiGraphics, i, j, f);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		}
	}
}
