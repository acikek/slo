package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.mixin.GenericMessageScreenAccess;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.network.chat.Component;

public abstract class ServerProcessScreen extends GenericDirtMessageScreen {

    private final Component buttonText;

    public ServerProcessScreen(Component status, Component buttonText) {
        super(status);
        this.buttonText = buttonText;
    }

    public void setStatus(Component status) {
		((GenericMessageScreenAccess) this).setTitle(status);
        repositionElements();
    }

    public abstract void exit();

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(buttonText, button -> exit()).pos(width / 2 - 100, height / 4 + 120 + 12).width(200).build());
    }

    public static class ShutDown extends ServerProcessScreen {

        public static final Component SHUT_DOWN = Component.translatable("gui.slo.status.shutDown");
        public static final Component FORCE_STOP = Component.translatable("gui.slo.stop.forceStop");

        public ShutDown() {
            super(SHUT_DOWN, FORCE_STOP);
        }

        @Override
        public void exit() {
            Slo.stop(minecraft, Slo.Status.LEAVING);
        }
    }
}
