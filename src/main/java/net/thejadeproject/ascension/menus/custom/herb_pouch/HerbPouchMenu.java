package net.thejadeproject.ascension.menus.custom.herb_pouch;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.thejadeproject.ascension.common.items.data_components.ModDataComponents;
import net.thejadeproject.ascension.common.items.data_components.herb_pouch.HerbPouchComponent;
import net.thejadeproject.ascension.menus.ModMenuTypes;

import java.util.List;

public class HerbPouchMenu extends AbstractContainerMenu {
    private final ItemStack pouchStack;
    private HerbPouchComponent clientPouchData;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 27;
    private static final int HOTBAR_START = 27;
    private static final int HOTBAR_END = 36;

    public HerbPouchMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        super(ModMenuTypes.HERB_POUCH_MENU.get(), containerId);

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.pouchStack = inventory.player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!pouchStack.has(ModDataComponents.HERB_POUCH_DATA.get())) {
            pouchStack.set(ModDataComponents.HERB_POUCH_DATA.get(), new HerbPouchComponent(27 * 64));
        }
        this.clientPouchData = pouchStack.get(ModDataComponents.HERB_POUCH_DATA.get());
    }

    private HerbPouchComponent getPouchData() {
        if (!pouchStack.has(ModDataComponents.HERB_POUCH_DATA.get())) {
            pouchStack.set(ModDataComponents.HERB_POUCH_DATA.get(), new HerbPouchComponent(27 * 64));
        }
        return pouchStack.get(ModDataComponents.HERB_POUCH_DATA.get());
    }

    private void setPouchData(HerbPouchComponent component) {
        pouchStack.set(ModDataComponents.HERB_POUCH_DATA.get(), component);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 0, 0));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 0, 0));
        }
    }

    public HerbPouchComponent getClientPouchData() {
        return clientPouchData;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        boolean fromInv = index >= PLAYER_INV_START && index < PLAYER_INV_END;
        boolean fromHotbar = index >= HOTBAR_START && index < HOTBAR_END;

        if (fromInv || fromHotbar) {
            HerbPouchComponent.InsertResult result = getPouchData().insert(stack);
            setPouchData(result.component());
            setClientPouchData(result.component());

            slot.set(result.remainder());
            slot.setChanged();

            if (result.remainder().getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }

            return original;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).equals(pouchStack);
    }

    public ItemStack getPouchStack() {
        return pouchStack;
    }

    public void setClientPouchData(HerbPouchComponent component) {
        this.clientPouchData = component;

        if (pouchStack.has(ModDataComponents.HERB_POUCH_DATA.get())) {
            pouchStack.set(ModDataComponents.HERB_POUCH_DATA.get(), component);
        }
    }

    public List<ItemStack> getSummaryStacks() {
        if (clientPouchData == null) return List.of();
        return clientPouchData.getSummaryStacks();
    }
}