package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** GUI专用协议只受当前服务器菜单授权；旧世界四控件的写协议不再注册。 */
public final class ReactorAutomationNetwork {
    private ReactorAutomationNetwork() { }
    public record Input(ReactorAdministratorCommands.Request request) implements CustomPacketPayload {
        public static final Type<Input> TYPE=new Type<>(ResourceLocation.parse("create_nuclear_industry:reactor_administrator"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=new StreamCodec<>() {
            public Input decode(RegistryFriendlyByteBuf buffer) {
                int container=buffer.readVarInt();var session=buffer.readUUID(); var burner=buffer.readBlockPos(); var port=buffer.readBlockPos();
                var generation=buffer.readUUID(); long epoch=buffer.readLong(), revision=buffer.readLong(), sequence=buffer.readLong();
                int action=buffer.readVarInt(),mode=buffer.readVarInt(),depth=buffer.readInt(),heat=buffer.readInt();
                return new Input(new ReactorAdministratorCommands.Request(container,session,burner,port,generation,epoch,revision,sequence,
                        action>=0&&action<4?ReactorAdministratorCommands.Action.values()[action]:null,
                        new ReactorAdministratorCommands.Draft(mode>=0&&mode<3?ReactorAutomationState.Mode.values()[mode]:null,depth,heat)));
            }
            public void encode(RegistryFriendlyByteBuf buffer,Input packet) {
                var r=packet.request(); buffer.writeVarInt(r.containerId());buffer.writeUUID(r.sessionId()); buffer.writeBlockPos(r.burnerPos()); buffer.writeBlockPos(r.portPos());
                buffer.writeUUID(r.ownerGeneration()); buffer.writeLong(r.authorityEpoch()); buffer.writeLong(r.settingsRevision()); buffer.writeLong(r.clientSequence());
                // 三个独立安全动作没有设置草稿；固定宽度空占位不改变其服务器权威语义。
                buffer.writeVarInt(r.action().ordinal()); buffer.writeVarInt(r.draft()==null?-1:r.draft().mode().ordinal());
                buffer.writeInt(r.draft()==null?0:r.draft().depthPercent());buffer.writeInt(r.draft()==null?0:r.draft().heatHuPerTick());
            }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Reply(int containerId,java.util.UUID sessionId,long sequence,boolean accepted,String reason,CompoundTag view) implements CustomPacketPayload {
        public static final Type<Reply> TYPE=new Type<>(ResourceLocation.parse("create_nuclear_industry:reactor_administrator_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=new StreamCodec<>() {
            public Reply decode(RegistryFriendlyByteBuf buffer) { return new Reply(buffer.readVarInt(),buffer.readUUID(),buffer.readLong(),buffer.readBoolean(),buffer.readUtf(128),buffer.readNbt()); }
            public void encode(RegistryFriendlyByteBuf buffer,Reply packet) { buffer.writeVarInt(packet.containerId());buffer.writeUUID(packet.sessionId()); buffer.writeLong(packet.sequence()); buffer.writeBoolean(packet.accepted()); buffer.writeUtf(packet.reason(),128); buffer.writeNbt(packet.view()); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("3").playToServer(Input.TYPE,Input.CODEC,(packet,context)->context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer player) {
                var reply=ReactorAdministratorCommands.apply(player,packet.request());
                PacketDistributor.sendToPlayer(player,new Reply(packet.request().containerId(),packet.request().sessionId(),reply.sequence(),reply.accepted(),reply.reason(),reply.projection().data()));
            }
        })).playToClient(Reply.TYPE,Reply.CODEC,(packet,context)->context.enqueueWork(()->{
            if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) com.iksxh.create_nuclear_industry.automation.client.ReactorAutomationClientEvents.apply(packet);
        }));
    }
}
