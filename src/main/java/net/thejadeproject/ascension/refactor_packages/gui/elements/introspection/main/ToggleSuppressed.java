package net.thejadeproject.ascension.refactor_packages.gui.elements.introspection.main;

import net.lucent.easygui.gui.UIFrame;
import net.lucent.easygui.gui.textures.ITextureData;
import net.lucent.easygui.gui.textures.TextureDataSubsection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.thejadeproject.ascension.AscensionCraft;
import net.thejadeproject.ascension.data_attachments.ModAttachments;
import net.thejadeproject.ascension.refactor_packages.gui.elements.general.BetterButton;
import net.thejadeproject.ascension.refactor_packages.network.server_bound.ToggleSuppressedPacket;

public class ToggleSuppressed extends BetterButton {
    ResourceLocation textureIdentifier = ResourceLocation.fromNamespaceAndPath(
            AscensionCraft.MOD_ID,
            "textures/gui/main/menu_gui.png"
    );


    ITextureData ticked = new TextureDataSubsection(
            textureIdentifier,
            256,260,
            12,232,
            9,8
    );

    public ToggleSuppressed(UIFrame frame, int x, int y) {

        super(frame, x, y);
        setWidth(13);
        setHeight(13);
    }
    public boolean isToggled(){
        return Minecraft.getInstance().player.getData(ModAttachments.ENTITY_DATA).isSuppressed();
    }

    @Override
    public void onClick() {
        super.onClick();
        PacketDistributor.sendToServer(new ToggleSuppressedPacket(!isToggled()));
        Minecraft.getInstance().player.getData(ModAttachments.ENTITY_DATA).setSuppressed(!isToggled());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if(isToggled()) ticked.renderAt(guiGraphics,2,3);
        if(isHovered()) guiGraphics.fill(0,0,getWidth(),getHeight(),1687195792);
    }
}
