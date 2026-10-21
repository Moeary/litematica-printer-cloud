package me.aleksilassila.litematica.printer.audit.mixin;

import me.aleksilassila.litematica.printer.audit.AuditPacketTrace;
import me.aleksilassila.litematica.printer.audit.PrinterAuditServer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class AuditClientUseMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void observeLocalWorld(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> result) {
        if (!AuditPacketTrace.ENABLED) return;
        AuditPacketTrace.log("CLIENT_USE", AuditPacketTrace.hand(player)
                + " hit=" + hit.getBlockPos().toShortString() + " face=" + hit.getDirection()
                + " clicked=" + PrinterAuditServer.describeTarget(player.level(), hit.getBlockPos())
                + " adjacent=" + PrinterAuditServer.describeTarget(player.level(), hit.getBlockPos().relative(hit.getDirection())));
    }
}
