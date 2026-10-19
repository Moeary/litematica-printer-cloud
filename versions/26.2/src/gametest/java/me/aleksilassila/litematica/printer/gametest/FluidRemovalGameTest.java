package me.aleksilassila.litematica.printer.gametest;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionMode;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.spatial.PlayerHeightFilter;
import me.aleksilassila.litematica.printer.enums.AreaSelectionType;
import me.aleksilassila.litematica.printer.enums.SelectionType;
import me.aleksilassila.litematica.printer.handler.IteratorManager;
import me.aleksilassila.litematica.printer.handler.ModuleManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class FluidRemovalGameTest implements FabricClientGameTest {
    private static final BlockPos FLUID_TARGET = new BlockPos(2, 64, 0);
    private static final int MATERIAL_COUNT = 4;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (GameTestMode.isAnyPerformance()) return;
        if (GameTestMode.isBedrockIntegration()) return;
        if (Boolean.getBoolean("litematica-printer.gametest.quickshulkerStress")) return;
        if (Boolean.getBoolean("litematica-printer.gametest.networkFaults")) return;
        if (Boolean.getBoolean(
                "litematica-printer.gametest.quickshulkerIntegrationOnly")) return;
        if (Boolean.getBoolean("litematica-printer.gametest.quickshulkerPacketLoss")
                || Boolean.getBoolean("litematica-printer.gametest.quickshulkerLegacyPacketLoss")) {
            return;
        }
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            prepareWorld(singleplayer);
            singleplayer.getServer().runCommand("gamemode survival @p");
            singleplayer.getServer().runCommand("tp @p 2.5 65 -3.5");

            context.waitTicks(5);
            singleplayer.getClientLevel().waitForChunksDownload();
            context.waitFor(client -> client.player != null
                    && !client.player.getAbilities().instabuild
                    && client.level != null
                    && client.level.getBlockState(FLUID_TARGET).is(Blocks.WATER)
                    && client.player.getInventory().getItem(9).is(Items.SAND)
                    && client.player.getInventory().getItem(9).getCount() == MATERIAL_COUNT);

            context.runOnClient(client -> {
                verifyAreaSelectionOptions(client.player);
                configureFluidRemoval();
            });
            context.waitFor(client -> client.level != null
                    && client.level.getBlockState(FLUID_TARGET).is(Blocks.SAND)
                    && client.level.getFluidState(FLUID_TARGET).isEmpty());
            context.waitTicks(5);

            FluidRemovalResult result = singleplayer.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var state = server.overworld().getBlockState(FLUID_TARGET);
                int sand = player.getInventory().getNonEquipmentItems().stream()
                        .filter(stack -> stack.is(Items.SAND))
                        .mapToInt(ItemStack::getCount)
                        .sum();
                return new FluidRemovalResult(
                        state.is(Blocks.SAND),
                        state.getFluidState().isEmpty(),
                        server.overworld().getBlockState(FLUID_TARGET.above()).isAir(),
                        sand);
            });

            if (!result.replacedWithSand()
                    || !result.fluidEmpty()
                    || !result.aboveIsAir()
                    || result.remainingSand() != MATERIAL_COUNT - 1) {
                throw new AssertionError("Fluid removal did not replace the water source: " + result);
            }
        } finally {
            context.runOnClient(client -> disableFluidRemoval());
        }
    }

    private static void prepareWorld(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            var level = server.overworld();
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().clearContent();

            for (int x = -2; x <= 6; x++) {
                for (int z = -6; z <= 3; z++) {
                    level.setBlockAndUpdate(
                            new BlockPos(x, 63, z),
                            Blocks.COBBLESTONE.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(FLUID_TARGET, Blocks.WATER.defaultBlockState());
            level.setBlockAndUpdate(FLUID_TARGET.above(), Blocks.AIR.defaultBlockState());
            player.getInventory().setItem(9, new ItemStack(Items.SAND, MATERIAL_COUNT));
            player.inventoryMenu.sendAllDataToRemote();
        });
    }

    /** Exercise actual config loading, dropdown cycling, and unchanged height filtering. */
    private static void verifyAreaSelectionOptions(net.minecraft.client.player.LocalPlayer player) {
        verifyHeightFilterChanges(player);
        if (Configs.Fill.FILL_SELECTION_TYPE.getComment() != null
                || Configs.Fluid.FLUID_SELECTION_TYPE.getComment() != null) {
            throw new AssertionError("Area-selection scope must not create a hover tooltip");
        }
        if (Configs.Print.PRINT_SELECTION_TYPE.getComment() == null) {
            throw new AssertionError("Unrelated printing descriptions must remain available");
        }
        AreaSelectionType[] options = AreaSelectionType.values();
        if (options.length != 3 || options[2].cycle(true) != AreaSelectionType.ALL
                || AreaSelectionType.ALL.cycle(false) != AreaSelectionType.ABOVE_PLAYER) {
            throw new AssertionError("Area-selection dropdown must contain only its three scope choices");
        }
        for (var config : List.of(Configs.Fill.FILL_SELECTION_TYPE, Configs.Fluid.FLUID_SELECTION_TYPE)) {
            var originalValue = config.getOptionListValue();
            try {
                if (config.getDefaultOptionListValue() != AreaSelectionType.ALL) {
                    throw new AssertionError("Area-selection config still uses printing options");
                }
                for (SelectionType legacy : SelectionType.values()) {
                    AreaSelectionType expected = switch (legacy) {
                        case LITEMATICA_SELECTION_BELOW_PLAYER -> AreaSelectionType.BELOW_PLAYER;
                        case LITEMATICA_SELECTION_ABOVE_PLAYER -> AreaSelectionType.ABOVE_PLAYER;
                        default -> AreaSelectionType.ALL;
                    };
                    config.setValueFromString(legacy.getStringValue());
                    if (config.getOptionListValue() != expected) {
                        throw new AssertionError("Lost old area-selection setting: " + legacy);
                    }
                    String saved = config.getStringValue();
                    config.setValueFromString(saved);
                    if (config.getOptionListValue() != expected) {
                        throw new AssertionError("Area-selection setting did not round-trip: " + saved);
                    }
                    IteratorManager oldRange = new IteratorManager();
                    IteratorManager newRange = new IteratorManager();
                    oldRange.tryBuildBox(player, legacy.heightFilter());
                    newRange.tryBuildBox(player, expected.heightFilter());
                    if (!oldRange.getBox().equals(newRange.getBox())) {
                        throw new AssertionError("Area-selection migration changed the scan range: " + legacy);
                    }
                }
            } finally {
                config.setOptionListValue(originalValue);
            }
        }
    }

    private static void verifyHeightFilterChanges(net.minecraft.client.player.LocalPlayer player) {
        var layers = DataManager.getRenderLayerRange();
        var originalMode = layers.getLayerMode();
        try {
            layers.setLayerMode(fi.dy.masa.malilib.util.LayerMode.ALL);
            IteratorManager range = new IteratorManager();
            range.tryBuildBox(player, PlayerHeightFilter.ALL);
            var full = range.getBox();
            if (range.tryBuildBox(player, PlayerHeightFilter.ALL)) {
                throw new AssertionError("An unchanged height filter must reuse the scan range");
            }
            if (!range.tryBuildBox(player, PlayerHeightFilter.BELOW_PLAYER)
                    || range.getBox().maxY != Math.min(full.maxY, (int) Math.floor(player.getY()))
                    || range.getBox().minY != full.minY) {
                throw new AssertionError("Below-player filtering did not rebuild the vertical range");
            }
            if (!range.tryBuildBox(player, PlayerHeightFilter.ABOVE_PLAYER)
                    || range.getBox().minY != Math.max(full.minY, (int) Math.ceil(player.getY()))
                    || range.getBox().maxY != full.maxY) {
                throw new AssertionError("Above-player filtering did not rebuild the vertical range");
            }
            if (!range.tryBuildBox(player, PlayerHeightFilter.ALL)
                    || !full.equals(range.getBox())) {
                throw new AssertionError("Removing the height filter must restore the original scan range");
            }
        } finally {
            layers.setLayerMode(originalMode);
        }
    }

    private static void configureFluidRemoval() {
        var selectionManager = DataManager.getSelectionManager();
        if (selectionManager.getSelectionMode() != SelectionMode.SIMPLE) {
            selectionManager.switchSelectionMode();
        }
        AreaSelection selection = DataManager.getSimpleArea();
        Box box = selection.getSubRegionBox(selection.getName());
        if (box == null) box = selection.getSelectedSubRegionBox();
        if (box == null) {
            throw new AssertionError("Litematica simple selection has no box");
        }
        box.setPos1(FLUID_TARGET);
        box.setPos2(FLUID_TARGET);

        Configs.Core.LAG_CHECK.setBooleanValue(false);
        Configs.Core.WORK_RANGE.setDoubleValue(6.0D);
        Configs.Placement.PRINT_USE_PACKET.setBooleanValue(false);
        Configs.Placement.PLACE_INTERVAL.setIntegerValue(0);
        Configs.Placement.PLACE_BLOCKS_PER_TICK.setIntegerValue(1);
        Configs.Placement.PLACE_COOLDOWN.setIntegerValue(3);
        // Exercise supported placement. Fluid removal must click the block
        // below the target instead of placing a falling block above it.
        Configs.Print.PLACE_IN_AIR.setBooleanValue(false);
        Configs.Print.ENABLED.setBooleanValue(false);
        Configs.Fill.ENABLED.setBooleanValue(false);
        Configs.Bedrock.ENABLED.setBooleanValue(false);
        Configs.Fluid.FLUID_SELECTION_TYPE.setOptionListValue(AreaSelectionType.ALL);
        Configs.Fluid.FILL_FLOWING_FLUID.setBooleanValue(true);
        Configs.Fluid.FLUID_REPLACE_BLOCK_LIST.setStrings(List.of("minecraft:sand"));
        Configs.Fluid.FLUID_LIST.setStrings(List.of("minecraft:water"));
        Configs.Fluid.ENABLED.setBooleanValue(true);
        ModuleManager.FLUID_REMOVAL.resetScanState();
        Configs.Core.WORK_SWITCH.setBooleanValue(true);
    }

    private static void disableFluidRemoval() {
        Configs.Core.WORK_SWITCH.setBooleanValue(false);
        Configs.Fluid.ENABLED.setBooleanValue(false);
        ModuleManager.FLUID_REMOVAL.resetScanState();
    }

    private record FluidRemovalResult(boolean replacedWithSand,
                                      boolean fluidEmpty,
                                      boolean aboveIsAir,
                                      int remainingSand) {
    }
}
