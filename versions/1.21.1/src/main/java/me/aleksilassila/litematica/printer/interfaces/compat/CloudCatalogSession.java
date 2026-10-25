package me.aleksilassila.litematica.printer.interfaces.compat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Arrays;
import java.util.stream.Collectors;

/** CloudStorage v3 discovery handshake. Payloads select a catalogue; validated clicks transfer stock. */
final class CloudCatalogSession {
    private static String lastRequest = "";
    private static int sentAt = -100;
    private static int firstRequested = -100;

    private CloudCatalogSession() {}

    static void reset() { lastRequest = ""; sentAt = -100; firstRequested = -100; }

    static boolean waiting(LocalPlayer player) {
        // Retain the current print job during the initial reply, but let missing stock time out.
        return !lastRequest.isEmpty() && player.tickCount-firstRequested < 20;
    }

    static String tokenId(LocalPlayer player, ItemStack stack) {
        if (!QuickShulkerInventory.isSingleShulker(stack)) return "";
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return "";
        CompoundTag data = custom.copyTag().getCompound("PublicBukkitValues");
        if (data.getInt("cloudstorage:catalog_version") < 3
                || !player.getUUID().toString().equals(data.getString("cloudstorage:owner"))) return "";
        return data.getString("cloudstorage:token");
    }

    static int find(LocalPlayer player) {
        for (int slot = 0; slot < QuickShulkerInventory.storageSize(player.getInventory()); slot++) {
            if (!tokenId(player, player.getInventory().getItem(slot)).isEmpty()) return slot;
        }
        return -1;
    }

    /** Wait for the server's actual catalogue update before opening the virtual box. */
    static boolean prepare(LocalPlayer player, Item[] items) {
        int slot = find(player);
        if (slot < 0) return true;
        ItemStack token = player.getInventory().getItem(slot);
        CompoundTag data = token.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("PublicBukkitValues");
        if (data.getInt("cloudstorage:page") >= 0 || !data.getString("cloudstorage:materials").isBlank()) return true;
        String request = Arrays.stream(items).distinct().limit(8)
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).collect(Collectors.joining(","));
        boolean ready = QuickShulkerInventory.contents(token).stream()
                .anyMatch(stored -> Arrays.asList(items).contains(stored.getItem()));
        int elapsed = player.tickCount - sentAt;
        String identity = tokenId(player, token) + ":" + request;
        if (!identity.equals(lastRequest) || (!ready && elapsed >= 20)) {
            if (!ClientPlayNetworking.canSend(CloudDemandPayload.TYPE)) return false;
            if (!identity.equals(lastRequest)) firstRequested = player.tickCount;
            ClientPlayNetworking.send(new CloudDemandPayload(request));
            lastRequest = identity;
            sentAt = player.tickCount;
            return false;
        }
        return ready && elapsed >= 2;
    }

    /** Only plain main-inventory building resources may make room; never a requested item. */
    static int returnCandidate(Inventory inventory, Item[] requested) {
        for (int slot = 9; slot < QuickShulkerInventory.storageSize(inventory); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)
                    || QuickShulkerInventory.isShulker(stack) || stack.getMaxStackSize() <= 1
                    || stack.is(Items.PLAYER_HEAD) || stack.is(Items.BEDROCK) || stack.is(Items.BARRIER)
                    || stack.is(Items.COMMAND_BLOCK) || stack.is(Items.CHAIN_COMMAND_BLOCK)
                    || stack.is(Items.REPEATING_COMMAND_BLOCK) || stack.is(Items.STRUCTURE_BLOCK)
                    || stack.is(Items.JIGSAW) || stack.is(Items.STRUCTURE_VOID)
                    || Arrays.asList(requested).contains(stack.getItem())) continue;
            if (ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()))) return slot;
        }
        return -1;
    }
}
