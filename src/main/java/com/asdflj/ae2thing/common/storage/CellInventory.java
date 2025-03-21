package com.asdflj.ae2thing.common.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.common.item.IItemInventoryHandler;
import com.asdflj.ae2thing.common.storage.backpack.AdventureBackpackHandler;
import com.asdflj.ae2thing.common.storage.backpack.BackPackHandler;
import com.asdflj.ae2thing.common.storage.backpack.BaseBackpackHandler;
import com.asdflj.ae2thing.common.storage.backpack.FTRBackpackHandler;
import com.asdflj.ae2thing.util.ModAndClassUtil;
import com.darkona.adventurebackpack.item.ItemAdventureBackpack;
import com.darkona.adventurebackpack.util.Wearing;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.crossmod.thaumcraft.AspectUtil;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.Platform;
import de.eydamos.backpack.item.ItemBackpackBase;
import de.eydamos.backpack.util.BackpackUtil;
import forestry.storage.items.ItemBackpack;

public class CellInventory implements ITCellInventory {

    protected final ItemStack cellItem;
    protected IStorageItemCell cellType;
    protected final ISaveProvider container;
    protected final EntityPlayer player;
    protected final List<IInventory> modInv = new ArrayList<>();
    protected final List<BaseBackpackHandler> fluidInv = new ArrayList<>();
    protected IItemList<IAEItemStack> cellItems = null;

    public CellInventory(final ItemStack o, final ISaveProvider c, final EntityPlayer p) throws AppEngException {
        if (o == null) {
            throw new AppEngException("ItemStack was used as a cell, but was not a cell!");
        }
        cellItem = o;
        container = c;
        player = p;
        this.cellType = (IStorageItemCell) this.cellItem.getItem();
    }

    @SuppressWarnings("unchecked")
    private void getAllInv() {
        if (ModAndClassUtil.FTR) {
            this.modInv.addAll(
                getModInv(
                    (player) -> Arrays.stream(player.inventory.mainInventory)
                        .filter(x -> x != null && x.getItem() instanceof ItemBackpack)
                        .map(x -> new FTRBackpackHandler(player, x))
                        .collect(Collectors.toList())));
        }
        if (ModAndClassUtil.ADVENTURE_BACKPACK) {
            this.modInv.addAll(
                getModInv(
                    (player) -> Arrays.stream(player.inventory.mainInventory)
                        .filter(x -> x != null && x.getItem() instanceof ItemAdventureBackpack)
                        .map(AdventureBackpackHandler::new)
                        .collect(Collectors.toList())));
            ItemStack wearingBackpack = Wearing.getWearingBackpack(player);
            if (wearingBackpack != null) {
                modInv.add(new AdventureBackpackHandler(wearingBackpack));
            }
        }
        if (ModAndClassUtil.BACKPACK) {
            this.modInv.addAll(
                getModInv(
                    (player) -> Arrays.stream(player.inventory.mainInventory)
                        .filter(
                            x -> x != null && x.getItem() instanceof ItemBackpackBase
                                && !BackpackUtil.isEnderBackpack(x))
                        .map(x -> new BackPackHandler(player, x))
                        .collect(Collectors.toList())));
        }
        this.modInv.addAll(
            getModInv(
                (player) -> Arrays.stream(player.inventory.mainInventory)
                    .filter(
                        x -> AE2ThingAPI.instance()
                            .isBackpackItemInv(x))
                    .map(
                        x -> AE2ThingAPI.instance()
                            .getBackpackInv(x))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())));

        for (IInventory inv : this.modInv) {
            if (inv instanceof BaseBackpackHandler bbh && bbh.hasFluidTank()) {
                this.fluidInv.add((BaseBackpackHandler) inv);
            }
        }
    }

    private List<IInventory> getModInv(IModInv inv) {
        return inv.getInv(this.player);
    }

    @Override
    public ItemStack getItemStack() {
        return cellItem;
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return null;
    }

    @Override
    public IInventory getConfigInventory() {
        return null;
    }

    @Override
    public IInventory getUpgradesInventory() {
        return null;
    }

    @Override
    public int getBytesPerType() {
        return 0;
    }

    @Override
    public boolean canHoldNewItem(ItemStack is) {
        for (IInventory inv : this.modInv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.isItemValidForSlot(i, is)) return true;
                else if (inv.getStackInSlot(i) == null) break;
            }
        }
        return false;
    }

    @Override
    public long getTotalBytes() {
        return 0;
    }

    @Override
    public long getFreeBytes() {
        return 0;
    }

    @Override
    public long getUsedBytes() {
        return 0;
    }

    @Override
    public long getTotalItemTypes() {
        return 0;
    }

    @Override
    public long getStoredItemCount() {
        return 0;
    }

    @Override
    public long getStoredItemTypes() {
        return 0;
    }

    @Override
    public long getRemainingItemTypes() {
        return 0;
    }

    @Override
    public long getRemainingItemCount() {
        return 0;
    }

    @Override
    public int getUnusedItemCount() {
        return 0;
    }

    @Override
    public int getStatusForCell() {
        return 0;
    }

    @Override
    public String getOreFilter() {
        return null;
    }

    private FluidStack injectFluid(FluidStack fs) {
        if (ModAndClassUtil.THE && AspectUtil.isEssentiaGas(fs)) return fs;
        FluidStack injectFluid = fs.copy();
        for (BaseBackpackHandler inv : this.fluidInv) {
            for (FluidTank ft : inv.getFluidTanks()) {
                int added = ft.fill(injectFluid, true);
                inv.markFluidAsDirty();
                if (added > 0) {
                    injectFluid.amount -= added;
                }
                if (injectFluid.amount <= 0) {
                    return injectFluid;
                }
            }
        }
        return injectFluid;
    }

    private ItemStack injectItem(ItemStack is) {
        ItemStack injectItem = is.copy();
        for (IInventory inv : this.modInv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.isItemValidForSlot(i, injectItem)) {
                    ItemStack added = injectItem.copy();
                    if (inv.getStackInSlot(i) == null) {
                        added.stackSize = Math.min(added.getMaxStackSize(), injectItem.stackSize);
                        inv.setInventorySlotContents(i, added);
                    } else {
                        ItemStack slotItem = inv.getStackInSlot(i)
                            .copy();
                        added.stackSize = Math.min(added.getMaxStackSize() - slotItem.stackSize, injectItem.stackSize);
                        slotItem.stackSize += added.stackSize;
                        inv.setInventorySlotContents(i, slotItem);
                    }
                    injectItem.stackSize -= added.stackSize;
                    if (injectItem.stackSize <= 0) {
                        return injectItem;
                    }
                } else if (inv.getStackInSlot(i) == null) {
                    break;
                }
            }
        }
        return injectItem;
    }

    private void tryToLoadCellItems() {
        if (this.cellItems == null) {
            this.loadCellItems();
        }
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        if (input == null) {
            return null;
        }
        if (input.getStackSize() == 0) {
            return null;
        }
        if (this.cellType.isBlackListed(this.cellItem, input)) {
            return input;
        }

        if (mode == Actionable.MODULATE) {
            this.tryToLoadCellItems();
            ItemStack is;
            if (input.getItem() instanceof ItemFluidDrop) {
                is = ItemFluidDrop.newStack(
                    this.injectFluid(Objects.requireNonNull(ItemFluidDrop.getFluidStack(input.getItemStack()))));
            } else {
                is = this.injectItem(Objects.requireNonNull(input.getItemStack()));
            }
            if (is == null || is.stackSize == 0) {
                this.getCellItems()
                    .add(input);
                return null;
            } else {
                IAEItemStack l = input.copy();
                IAEItemStack noAdded = AEApi.instance()
                    .storage()
                    .createItemStack(is);
                l.decStackSize(noAdded.getStackSize());
                this.getCellItems()
                    .add(l);
                return noAdded;
            }

        }
        return null;
    }

    protected IItemList<IAEItemStack> getCellItems() {
        if (this.cellItems == null) {
            this.loadCellItems();
        }
        return this.cellItems;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null) {
            return null;
        }
        IAEItemStack result = null;

        final IAEItemStack l = this.getCellItems()
            .findPrecise(request);

        if (l != null && l.getStackSize() != 0) {
            result = l.copy();
            if (mode == Actionable.SIMULATE) {
                return result.getStackSize() > request.getStackSize() ? request : result;
            }
            if (mode == Actionable.MODULATE) {
                ItemStack extracted;
                if (request.getItem() instanceof ItemFluidDrop) {
                    extracted = extractFluid(
                        Objects.requireNonNull(ItemFluidDrop.getFluidStack(request.getItemStack())));
                } else {
                    extracted = extractItem(request.getItemStack());
                }
                l.decStackSize(extracted.stackSize);
                result.setStackSize(extracted.stackSize);
            }
        }

        return result;
    }

    private ItemStack extractFluid(FluidStack extractFluid) {
        FluidStack extFluid = extractFluid.copy();
        for (BaseBackpackHandler inv : this.fluidInv) {
            for (FluidTank tank : inv.getFluidTanks()) {
                if (tank.getFluid() == null) continue;
                if (extFluid.getFluid() == tank.getFluid()
                    .getFluid()) {
                    FluidStack result = tank.drain(extFluid.amount, true);
                    extFluid.amount -= result.amount;
                    inv.markFluidAsDirty();
                    if (extFluid.amount <= 0) {
                        return ItemFluidDrop.newStack(extractFluid);
                    }
                }
            }
        }
        extFluid.amount = extractFluid.amount - extFluid.amount;
        return ItemFluidDrop.newStack(extFluid);
    }

    private ItemStack extractItem(ItemStack extractItem) {
        ItemStack extItem = extractItem.copy();
        for (IInventory inv : this.modInv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack is = inv.getStackInSlot(i);
                if (Platform.isSameItemPrecise(is, extItem)) {
                    int size = is.stackSize;
                    if (size > extItem.stackSize) {
                        is.splitStack(extItem.stackSize);
                        inv.setInventorySlotContents(i, is.copy());
                        return extractItem;
                    } else {
                        inv.setInventorySlotContents(i, null);
                        extItem.stackSize -= size;
                    }
                    if (extItem.stackSize <= 0) {
                        return extractItem;
                    }
                }
            }
        }
        extItem.stackSize = extractItem.stackSize - extItem.stackSize;
        return extItem;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        for (final IAEItemStack i : this.getCellItems()) {
            out.add(i);
        }

        return out;
    }

    @Override
    public StorageChannel getChannel() {
        return ((IItemInventoryHandler) Objects.requireNonNull(this.cellItem.getItem())).getChannel();
    }

    @Override
    public double getIdleDrain(ItemStack is) {
        return 0;
    }

    @Override
    public void loadCellItems() {
        if (this.cellItems == null) {
            this.getAllInv();
            this.cellItems = AEApi.instance()
                .storage()
                .createPrimitiveItemList();
        }
        cellItems.resetStatus();
        for (IInventory inv : this.modInv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack is = inv.getStackInSlot(i);
                if (is == null) continue;
                cellItems.add(
                    AEApi.instance()
                        .storage()
                        .createItemStack(is));
            }
        }
        for (BaseBackpackHandler inv : this.fluidInv) {
            for (FluidTank tank : inv.getFluidTanks()) {
                IAEItemStack is = ItemFluidDrop.newAeStack(tank.getFluid());
                if (is != null) cellItems.add(is);
            }
        }
    }
}
