package me.aleksilassila.litematica.printer.interfaces.compat;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A small versioned, optional channel understood only by CloudStorage. */
public record CloudDemandPayload(String materials) implements CustomPacketPayload {
    public static final Type<CloudDemandPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("cloudstorage","demand"));
    public static final StreamCodec<FriendlyByteBuf,CloudDemandPayload> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(512),CloudDemandPayload::materials,CloudDemandPayload::new);
    public static void register() { PayloadTypeRegistry.playC2S().register(TYPE,CODEC); }
    @Override public Type<CloudDemandPayload> type() { return TYPE; }
}
