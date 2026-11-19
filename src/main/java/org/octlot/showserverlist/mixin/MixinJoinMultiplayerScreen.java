package org.octlot.showserverlist.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.octlot.showserverlist.ServerListMemory;
import org.octlot.showserverlist.folder.FolderActions;
import org.octlot.showserverlist.folder.FolderEntry;
import org.octlot.showserverlist.folder.FolderStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen {
    @Shadow
    private Button joinButton;
    @Shadow
    protected ServerSelectionList serverSelectionList;
    @Shadow
    private ServerData editingServer;
    @Unique
    private Button directConnectionButton;
    @Unique
    private SpriteIconButton moveToFolderButton;

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 3
            )
    )
    public LayoutElement showserverlist$redirectCreateDirectConnectButton(LinearLayout instance, LayoutElement child) {
        if (isInWorld()) {
            this.directConnectionButton = (Button) child;
        }
        return instance.addChild(child);
    }
    @Unique
    private boolean isInWorld() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }
    @Inject(method = "join", at = @At("HEAD"), cancellable = true)
    public void showserverlist$cancelJoin(ServerData data, CallbackInfo ci){
        if (isInWorld()) {
            ci.cancel();
        }
    }
    @Inject(method = "init", at = @At("TAIL"))
    public void showserverlist$onInit(CallbackInfo ci){
        ServerListMemory.restore(this.serverSelectionList);
        showserverlist$addFolderButtons();
        if (isInWorld()) {
            this.joinButton.active = false;
            this.directConnectionButton.active = false;
        }
    }

    @Unique
    private void showserverlist$addFolderButtons() {
        JoinMultiplayerScreen screen = (JoinMultiplayerScreen) (Object) this;
        int y = 6;
        int x = screen.width - 28;
        screen.addRenderableWidget(
                SpriteIconButton.builder(
                        Component.translatable("showserverlist.folder.new"),
                        button -> FolderActions.createFolder(screen),
                        true
                ).width(20).sprite(Identifier.fromNamespaceAndPath("showserverlist", "folder_add"), 16, 16).withTootip().build()
        ).setPosition(x, y);
        this.moveToFolderButton = screen.addRenderableWidget(
                SpriteIconButton.builder(
                        Component.translatable("showserverlist.folder.move"),
                        button -> {
                            if (this.serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry entry) {
                                FolderActions.moveServer(screen, entry.getServerData());
                            }
                        },
                        true
                ).width(20).sprite(Identifier.fromNamespaceAndPath("showserverlist", "folder_move"), 16, 16).withTootip().build()
        );
        this.moveToFolderButton.setPosition(x - 24, y);
        showserverlist$updateFolderButtons();
    }

    @Unique
    private void showserverlist$updateFolderButtons() {
        if (this.moveToFolderButton != null) {
            this.moveToFolderButton.active = this.serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry;
        }
    }

    @Inject(method = "editServerCallback", at = @At("HEAD"))
    public void showserverlist$keepFolderOnEdit(boolean result, CallbackInfo ci){
        if (result && this.serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry entry) {
            FolderStore.get().migrateServer(entry.getServerData().ip, this.editingServer.ip);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    public void showserverlist$rememberListState(CallbackInfo ci){
        ServerListMemory.save(this.serverSelectionList);
    }

    @Inject(method = "onSelectedChange", at = @At("TAIL"))
    public void showserverlist$onSelectedChange(CallbackInfo ci){
        showserverlist$updateFolderButtons();
        if (this.serverSelectionList.getSelected() instanceof FolderEntry) {
            this.joinButton.active = false;
        }
        if (isInWorld()) {
            this.joinButton.active = false;
            this.directConnectionButton.active = false;
        }
    }
}
