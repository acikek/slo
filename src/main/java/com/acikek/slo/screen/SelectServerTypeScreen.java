package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ExtendedWorldCreationUiState;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class SelectServerTypeScreen extends Screen {

    public static final Component TITLE = Component.translatable("gui.slo.selectServerType.title");
    public static final Component DRAG_AND_DROP = Component.translatable("gui.slo.selectServerType.dragAndDrop").withStyle(ChatFormatting.GRAY);
    public static final Component ADD_TYPES_CONFIRM = Component.translatable("gui.slo.selectServerType.addTypes");
    public static final Component ADD_TYPES_FAILURE = Component.translatable("gui.slo.selectServerType.addTypes.fail");
    public static final Component ADD_TYPES_FAILURE_INFO = Component.translatable("gui.slo.selectServerType.addTypes.fail.info");
    public static final Component INTEGRATED_NAME = Component.translatable("gui.slo.integratedServer.name");
    public static final Component INTEGRATED_DESCRIPTION = Component.translatable("gui.slo.integratedServer.description");
    public static final ResourceLocation INTEGRATED_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");

    public static final SystemToast.SystemToastId ADD_TYPES_FAILURE_TOAST = new SystemToast.SystemToastId();

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
        var linearLayout = layout.addToHeader(LinearLayout.vertical().spacing(5));
        linearLayout.defaultCellSetting().alignHorizontallyCenter();
        linearLayout.addChild(new StringWidget(title, font));
        linearLayout.addChild(new StringWidget(DRAG_AND_DROP, font));
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

    @Override
    public void onFilesDrop(List<Path> list) {
        var fileNames = String.join(", ", list.stream().map(path -> FilenameUtils.removeExtension(path.getFileName().toString())).toList());
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes && applyFiles(list)) {
				SystemToast.add(minecraft.getToastManager(), ADD_TYPES_FAILURE_TOAST, ADD_TYPES_FAILURE, ADD_TYPES_FAILURE_INFO);
            }
            minecraft.setScreen(this);
        }, ADD_TYPES_CONFIRM, Component.literal(fileNames)));
        super.onFilesDrop(list);
    }

    public Map<Path, ExtendedLevelDirectory> acceptFiles(List<Path> paths) {
        var presetsDirectory = Slo.presetsDirectory();
        if (!Files.isDirectory(presetsDirectory)) {
            presetsDirectory.toFile().mkdirs();
        }
        Map<Path, ExtendedLevelDirectory> result = new HashMap<>();
        for (var path : paths) {
            String outputName;
            try {
                outputName = FileUtil.findAvailableName(Slo.presetsDirectory(), FilenameUtils.getBaseName(path.getFileName().toString()), "");
            }
            catch (IOException e) {
                Slo.LOGGER.error("Failed to find available directory for '{}'", path, e);
                continue;
            }
            var outputDirectory = Slo.presetsDirectory().resolve(outputName);
            if (path.toFile().isDirectory()) {
                try {
                    FileUtils.copyDirectory(path.toFile(), outputDirectory.toFile());
                }
                catch (IOException e) {
                    Slo.LOGGER.error("Failed to copy preset directory '{}'", path, e);
                    continue;
                }
            }
            else if (FilenameUtils.getExtension(path.getFileName().toString()).equals("zip")) {
                try (var zipFile = new ZipFile(path.toFile())) {
                    var entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        var entryFile = new File(outputDirectory.toFile(), entry.getName());
                        if (entry.isDirectory()) {
                            entryFile.mkdirs();
                            continue;
                        }
                        entryFile.getParentFile().mkdirs();
                        try (var inputStream = zipFile.getInputStream(entry); var outputStream = new FileOutputStream(entryFile)) {
                            inputStream.transferTo(outputStream);
                        }
                    }
                }
                catch (Exception e) {
                    Slo.LOGGER.error("Failed to extract preset '{}'", path, e);
                    continue;
                }
            }
			else if (FilenameUtils.getExtension(path.getFileName().toString()).equals("jar")) {
				try {
					Files.createDirectory(outputDirectory);
					try (var writer = new FileWriter(outputDirectory.resolve("slo.properties").toFile())) {
						writer.write("jar-path=" + path.getFileName().toString());
					}
					try (var writer = new FileWriter(outputDirectory.resolve("server.properties").toFile())) {
						writer.write("motd=" + path.getFileName().toString());
					}
					FileUtils.copyFile(path.toFile(), outputDirectory.resolve(path.getFileName().toString()).toFile());
				}
				catch (Exception e) {
					Slo.LOGGER.error("Failed to write to output directory '{}'", outputDirectory, e);
				}
			}
            else {
                Slo.LOGGER.warn("Invalid preset file: {}", path);
                result.put(outputDirectory, null);
                continue;
            }
            var presetDirectory = ExtendedLevelDirectory.create(outputDirectory, false, false);
            if (!presetDirectory.slo$isServer()) {
                Slo.LOGGER.warn("Not a server world preset: {}", path);
                try {
                    FileUtils.deleteDirectory(outputDirectory.toFile());
                }
                catch (IOException e) {
                    Slo.LOGGER.error("Failed to delete invalid preset directory '{}'", outputDirectory, e);
                }
                result.put(outputDirectory, null);
                continue;
            }
            result.put(outputDirectory, presetDirectory);
        }
        return result;
    }

	public boolean applyFiles(List<Path> paths) {
		boolean error = false;
		for (var entry : acceptFiles(paths).entrySet()) {
			if (entry.getValue() != null) {
				Slo.worldPresets.put(entry.getValue().slo$directory().directoryName(), entry.getValue());
				selectionList.addEntry(selectionList.new Entry(entry.getValue()));
			} else if (!error) {
				error = true;
			}
		}
		return error;
	}

    public void updateState() {
        if (selectionList.getSelected() == null) {
            return;
        }
        var directory = selectionList.getSelected().directory;
        if (directory != null && directory != ((ExtendedWorldCreationUiState) creationState).slo$presetDirectory()) {
            Slo.updateCreationState(directory, creationState);
        }
    }

    public void updateAndClose() {
        updateState();
        creationState.onChanged();
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
            int integratedIndex = addEntry(new Entry(INTEGRATED_ICON, INTEGRATED_NAME, INTEGRATED_DESCRIPTION, null));
            Slo.worldPresets.forEach((id, directory) -> {
                int entryIndex = addEntry(new Entry(directory));
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

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (super.keyPressed(i, j, k)) {
                return true;
            }
            if (CommonInputs.selected(i) && getSelected() != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                updateAndClose();
            }
            return false;
        }

        @Override
        public int addEntry(Entry entry) {
            return super.addEntry(entry);
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {

            public ResourceLocation icon;
            public Component name;
            public Component description;
            public ExtendedLevelDirectory directory;
            public long lastClickTime = 0;

            public Entry(ResourceLocation icon, Component name, Component description, ExtendedLevelDirectory directory) {
                this.icon = icon;
                this.name = name;
                this.description = description;
                this.directory = directory;
            }

            public Entry(ExtendedLevelDirectory directory) {
                this(directory.slo$loadIconTexture(), Component.literal(directory.slo$directory().directoryName()), directory.slo$motd() != null ? Component.literal(directory.slo$motd()) : null, directory);
            }

            @Override
            public boolean mouseClicked(double d, double e, int i) {
                if (Util.getMillis() - lastClickTime >= 250L) {
                    lastClickTime = Util.getMillis();
                    return super.mouseClicked(d, e, i);
                }
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                setSelected(this);
                updateAndClose();
                return true;
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
