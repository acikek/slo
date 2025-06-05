package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ExtendedLevelDirectory;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SelectJarCandidateScreen extends Screen {

	public static final Component TITLE = Component.translatable("gui.slo.selectJarCandidate.info");
	public static final Component USE_FILE = Component.translatable("gui.slo.selectJarCandidate.useFile");

	public Screen parent;
	public ExtendedLevelDirectory directory;

	public JarSelectionList list;
	public Button selectButton;

	public SelectJarCandidateScreen(Screen parent, ExtendedLevelDirectory directory) {
		super(TITLE);
		this.parent = parent;
		this.directory = directory;
	}

	@Override
	protected void init() {
		list = addRenderableWidget(new JarSelectionList());
		selectButton = addRenderableWidget(Button.builder(USE_FILE, button -> submit()).pos(width / 2 - 155, height - 48).width(150).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose()).pos(width / 2 + 5, height - 48).width(150).build());
		selectButton.active = false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		guiGraphics.drawCenteredString(font, title, width / 2, 36, 0xFFFFFF);
	}

	public void submit() {
		try {
			directory.slo$setJarPath(list.getSelected().candidate);
			directory.slo$writeSloProperties();
			LoadServerLevelScreen.load(minecraft, parent, directory, null);
		}
		catch (IOException e) {
			Slo.LOGGER.error("Failed to load server world", e);
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	public class JarSelectionList extends ObjectSelectionList<JarSelectionList.Entry> {

		public JarSelectionList() {
			super(SelectJarCandidateScreen.this.minecraft, SelectJarCandidateScreen.this.width, SelectJarCandidateScreen.this.height - 120, 60, 60, 24);
			directory.slo$jarCandidates().forEach(candidate -> addEntry(new Entry(candidate)));
		}

		@Override
		public void setSelected(@Nullable SelectJarCandidateScreen.JarSelectionList.Entry entry) {
			super.setSelected(entry);
			selectButton.active = true;
		}

		@Override
		public boolean keyPressed(int i, int j, int k) {
			if (super.keyPressed(i, j, k)) {
				return true;
			}
			if (CommonInputs.selected(i) && getSelected() != null) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				submit();
			}
			return false;
		}

		public class Entry extends ObjectSelectionList.Entry<Entry> {

			public String candidate;
			public long lastClickTime = 0;

			public Entry(String candidate) {
				this.candidate = candidate;
			}

			@Override
			public boolean mouseClicked(double d, double e, int i) {
				setSelected(this);
				if (Util.getMillis() - lastClickTime >= 250L) {
					lastClickTime = Util.getMillis();
					return super.mouseClicked(d, e, i);
				}
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				submit();
				return true;
			}

			@Override
			public @NotNull Component getNarration() {
				return Component.translatable("narrator.select", candidate);
			}

			@Override
			public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
				guiGraphics.drawCenteredString(SelectJarCandidateScreen.this.font, candidate, k + l / 2, j + m / 2 - SelectJarCandidateScreen.this.font.lineHeight / 2, 0xFFFFFF);
			}
		}
	}
}
