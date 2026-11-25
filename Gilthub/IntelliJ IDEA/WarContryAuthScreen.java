
package com.warcontry.auth;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;

public class WarContryAuthScreen extends Screen {

    private EditBox usernameField;
    private EditBox passwordField;
    private EditBox codeField;

    public WarContryAuthScreen() {
        super(Component.literal("Connexion War Contry"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 65;

        usernameField = new EditBox(
                this.font, centerX - 100, top, 200, 20,
                Component.literal("Pseudo")
        );
        usernameField.setMaxLength(32);
        usernameField.setHint(Component.literal("Pseudo du compte"));
        addRenderableWidget(usernameField);

        passwordField = new EditBox(
                this.font, centerX - 100, top + 27, 200, 20,
                Component.literal("Mot de passe")
        );
        passwordField.setMaxLength(128);
        passwordField.setHint(Component.literal("Mot de passe"));
        passwordField.setFormatter((text, cursor) ->
                FormattedCharSequence.forward(
                        "*".repeat(text.length()), Style.EMPTY
                )
        );
        addRenderableWidget(passwordField);

        codeField = new EditBox(
                this.font, centerX - 100, top + 54, 200, 20,
                Component.literal("Code 2FA")
        );
        codeField.setMaxLength(16);
        codeField.setHint(Component.literal("Code e-mail si demandé"));
        addRenderableWidget(codeField);

        addRenderableWidget(Button.builder(
                Component.literal("Se connecter / Vérifier"),
                button -> submit()
        ).bounds(centerX - 100, top + 84, 200, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Retour"),
                button -> onClose()
        ).bounds(centerX - 100, top + 110, 200, 20).build());

        setInitialFocus(usernameField);
    }

    private void submit() {
        WarContryAuthNetwork.CHANNEL.sendToServer(
                new WarContryAuthLoginPacket(
                        usernameField.getValue(),
                        passwordField.getValue(),
                        "",
                        codeField.getValue()
                )
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        graphics.drawCenteredString(
                this.font,
                "Connexion à votre compte War Contry",
                this.width / 2,
                this.height / 2 - 91,
                0xFFFFFF
        );

        graphics.drawString(
                this.font, "Pseudo",
                this.width / 2 - 100,
                this.height / 2 - 78,
                0xFFFFFF
        );

        graphics.drawString(
                this.font, "Mot de passe",
                this.width / 2 - 100,
                this.height / 2 - 51,
                0xFFFFFF
        );

        graphics.drawString(
                this.font, "Code 2FA (si demandé)",
                this.width / 2 - 100,
                this.height / 2 - 24,
                0xFFFFFF
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
