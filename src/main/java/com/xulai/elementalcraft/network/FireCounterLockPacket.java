package com.xulai.elementalcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FireCounterLockPacket {
    private final boolean locked;

    public FireCounterLockPacket(boolean locked) {
        this.locked = locked;
    }

    public static void encode(FireCounterLockPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.locked);
    }

    public static FireCounterLockPacket decode(FriendlyByteBuf buf) {
        return new FireCounterLockPacket(buf.readBoolean());
    }

    public static void handle(FireCounterLockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.xulai.elementalcraft.client.FrozenInputBlocker.fireCounterLocked = msg.locked;
        });
        ctx.get().setPacketHandled(true);
    }
}
