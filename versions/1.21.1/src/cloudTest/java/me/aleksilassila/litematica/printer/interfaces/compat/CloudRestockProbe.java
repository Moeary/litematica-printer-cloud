package me.aleksilassila.litematica.printer.interfaces.compat;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ShulkerSource;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;

/** Drives the production request/click path on a real, separately launched Paper connection. */
public final class CloudRestockProbe implements ClientModInitializer {
    private int stage;
    private long started;
    private long stoneMillis;
    private long fullMillis;
    private long fallbackMillis;
    private final boolean coexist=Files.exists(Path.of("cloud-coexist.enabled"));
    private final long deadline=System.currentTimeMillis()+180_000;

    public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(this::tick); }

    private void tick(Minecraft client) {
        if (stage==100) return;
        try {
            if (System.currentTimeMillis()>deadline) throw new AssertionError("Restock test timeout stage="+stage);
            if (client.player==null || client.level==null) return;
            var player=client.player;
            switch (stage) {
                case 0 -> {
                    if (count(client,Items.STONE)!=64 || count(client,Items.RED_CONCRETE)!=64) return;
                    Configs.Print.USE_QUICK_SHULKER.setBooleanValue(true);
                    Configs.Print.SHULKER_SOURCE.setOptionListValue(ShulkerSource.PLUGIN);
                    Configs.Print.SHULKER_COOLDOWN.setIntegerValue(5);
                    player.connection.sendUnsignedCommand("cs deposit all");
                    stage=1;
                }
                case 1 -> {
                    if (count(client,Items.STONE)!=0 || count(client,Items.RED_CONCRETE)!=0) return;
                    player.connection.sendUnsignedCommand("cs shulker auto"); stage=2;
                }
                case 2 -> {
                    if (CloudCatalogSession.find(player)<0) return;
                    started=System.currentTimeMillis();
                    check(me.aleksilassila.litematica.printer.utils.InventoryUtils.switchToItemsResult(player,new Item[]{Items.STONE})
                            == me.aleksilassila.litematica.printer.utils.InventoryUtils.ItemSwitchResult.WAITING,
                            "Printer job must wait for catalogue, not mark the material unavailable");
                    stage=3;
                }
                case 3 -> {
                    if (count(client,Items.STONE)==64 && !QuickShulkerCompat.isBusy()) {
                        stoneMillis=System.currentTimeMillis()-started;
                        check(stoneMillis<3000,"Initial on-demand restock took too long");
                        if (coexist) check(physicalCount(client,Items.STONE)==32,"Cloud must win over an earlier ordinary shulker");
                        player.connection.sendUnsignedCommand("cs deposit all"); stage=4; return;
                    }
                    QuickShulkerCompat.requestShulkerItem(player,new Item[]{Items.STONE});
                }
                case 4 -> {
                    if (count(client,Items.STONE)!=0) return;
                    if (coexist) { started=System.currentTimeMillis(); stage=40; return; }
                    fill(player);
                }
                case 40 -> {
                    if (count(client,Items.GRAVEL)==16 && !QuickShulkerCompat.isBusy()) {
                        // Ax temporarily clears the carried shell while open and restores it on close.
                        // Wait for that real server acknowledgement rather than inspecting prediction.
                        if (physicalCount(client,Items.STONE)!=32) return;
                        fallbackMillis=System.currentTimeMillis()-started;
                        check(fallbackMillis<4000,"Missing cloud stock must fall back to physical shulker");
                        check(physicalCount(client,Items.STONE)==32,"Fallback must preserve unrelated physical contents");
                        check(physicalCount(client,Items.GRAVEL)==0,"Fallback must debit physical shulker");
                        fill(player); return;
                    }
                    QuickShulkerCompat.requestShulkerItem(player,new Item[]{Items.GRAVEL});
                }
                case 5 -> {
                    if (count(client,Items.COBBLESTONE)!=26*64 || count(client,Items.OAK_PLANKS)!=9*64) return;
                    check(QuickShulkerInventory.isInventoryFull(player.getInventory()),"Fixture must fill inventory");
                    // No previous extraction of cobblestone exists: this tests the formerly stuck path.
                    QuickShulkerCompat.onPlayJoin();
                    started=System.currentTimeMillis(); stage=6;
                }
                case 6 -> {
                    if (count(client,Items.RED_CONCRETE)==64 && !QuickShulkerCompat.isBusy()) {
                        fullMillis=System.currentTimeMillis()-started;
                        check(fullMillis<3000,"Full inventory material switch took too long");
                        check(count(client,Items.COBBLESTONE)==25*64,"Return must remove exactly one plain stack");
                        check(count(client,Items.OAK_PLANKS)==9*64,"Hotbar must remain untouched");
                        check(player.containerMenu.getCarried().isEmpty(),"No stranded cursor stack");
                        Files.writeString(Path.of("cloud-restock.json"),"{\"status\":\"passed\",\"stone_ms\":"+stoneMillis+",\"full_inventory_ms\":"+fullMillis+",\"red_concrete\":64,\"returned_cobblestone\":64,\"hotbar_unchanged\":true,\"cloud_priority\":"+coexist+",\"physical_fallback_ms\":"+fallbackMillis+"}");
                        System.out.println("CLOUD_RESTOCK_PROBE: passed stone_ms="+stoneMillis+" full_ms="+fullMillis);
                        stage=100; return;
                    }
                    QuickShulkerCompat.requestShulkerItem(player,new Item[]{Items.RED_CONCRETE});
                }
            }
        } catch (Throwable error) {
            stage=100; error.printStackTrace();
            try { Files.writeString(Path.of("cloud-restock.json"),"{\"status\":\"failed\",\"error\":\"see client log\"}"); }
            catch (Exception ignored) {}
        }
    }
    private void fill(net.minecraft.client.player.LocalPlayer player) throws Exception {
        Files.writeString(Path.of("cloud-fixture.json"),"{\"stage\":\"fill\",\"cloud_slot\":"+CloudCatalogSession.find(player)+"}"); stage=5;
    }
    private int physicalCount(Minecraft client,Item item) {
        return client.player.getInventory().items.stream().filter(QuickShulkerInventory::isSingleShulker)
                .filter(box -> !CloudCatalogSession.isCatalog(box))
                .flatMap(box -> QuickShulkerInventory.contents(box).stream()).filter(stack -> stack.is(item))
                .mapToInt(stack -> stack.getCount()).sum();
    }
    private static int count(Minecraft client,Item item) {
        return client.player.getInventory().items.stream().filter(s->s.is(item)).mapToInt(s->s.getCount()).sum();
    }
    private static void check(boolean condition,String message) { if (!condition) throw new AssertionError(message); }
}
