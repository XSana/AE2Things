package com.asdflj.ae2thing.coremod.mixin.ae;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.network.CPacketTerminalBtns;
import com.asdflj.ae2thing.util.NameConst;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.util.item.ItemList;

@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends AEBaseGui {

    @Shadow(remap = false)
    private GuiButton start;

    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> storage;
    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> pending;
    @Shadow(remap = false)
    @Final
    private IItemList<IAEItemStack> missing;
    @Shadow(remap = false)
    @Final
    private List<IAEItemStack> visual;
    private GuiAeButton replan;
    private boolean clickStart = false;

    public MixinGuiCraftConfirm(Container container) {
        super(container);
    }

    @Inject(method = { "actionPerformed", "func_146284_a" }, at = @At(value = "HEAD"), remap = false)
    private void actionPerformed(GuiButton btn, CallbackInfo ci) {
        if (btn == start && this.inventorySlots instanceof ContainerCraftConfirm ccc) {
            clickStart = true;
        } else if (btn == replan && this.inventorySlots instanceof ContainerCraftConfirm) {
            ((ItemList) this.storage).clear();
            ((ItemList) this.pending).clear();
            ((ItemList) this.missing).clear();
            this.visual.clear();
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("GuiCraftConfirm.replan", true));
        }
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = false)
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(
            replan = new GuiAeButton(
                0,
                start.xPosition,
                start.yPosition,
                start.width,
                start.height,
                I18n.format(NameConst.GUI_BUTTON_REPLAN),
                ""));
        this.replan.visible = false;
    }

    @Inject(method = "drawFG", at = @At("HEAD"), remap = false)
    public void drawFG(CallbackInfo ci) {
        if (this.inventorySlots instanceof ContainerCraftConfirm c && (c.isSimulation() || clickStart)) {
            replan.visible = true;
            start.visible = false;
        } else {
            replan.visible = false;
            start.visible = true;
        }
    }
}
