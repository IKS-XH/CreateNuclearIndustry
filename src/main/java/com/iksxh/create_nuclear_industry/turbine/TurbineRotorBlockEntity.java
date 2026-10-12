package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.display.*;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

/** 跨格叶片渲染锚点，只持有不落盘的可信显示绑定；转子不持有流体、SU 或动力权威。 */
public final class TurbineRotorBlockEntity extends BlockEntity {
    private final UUID memberGeneration=UUID.randomUUID();
    private final TurbineRuntimeLease lease=new TurbineRuntimeLease();
    private TurbineRuntimeSample sample;
    private UUID clientMember;
    private long clientTick=-1;
    private WeakReference<TurbineControllerBlockEntity> clientOwner=new WeakReference<>(null);
    public TurbineRotorBlockEntity(BlockPos pos,BlockState state){super(TurbineContent.ROTOR_BE.get(),pos,state);}

    /** 服务端本转子实例身份，不保存、不作为动力或库存状态。 */
    public UUID runtimeMemberGeneration(){return memberGeneration;}
    /**
     * 消费者唯一查询入口：返回当前可信完整样本或空。0 RPM 为可靠停转；空值须停止动画。
     * 客户端每次查询核对真实成员、控制器实例及全部所需区块，不能凭旧样本恢复旋转。
     */
    public Optional<TurbineRuntimeSample> runtimeDisplay() {
        if(level==null||isRemoved()||sample==null)return Optional.empty();
        if(!level.isClientSide)return bindingValid(sample)?Optional.of(sample):Optional.empty();
        var current=lease.current(clientTick);
        if(current.isEmpty())return Optional.empty();
        TurbineRuntimeSample value=current.orElseThrow();
        // 查询必须核对即将返回的租约样本本身，不能拿新pending包的身份为旧旋转背书。
        if(!value.equals(sample)||!value.member().equals(clientMember)||!bindingValid(value)
                ||clientOwner.get()!=level.getBlockEntity(value.owner())) {
            lease.revoke();return Optional.empty();
        }
        return current;
    }
    /** 只允许服务端 Post 发布器赋完整值或撤销；原生更新包同时服务初次跟踪和心跳。 */
    public void publishRuntime(TurbineRuntimeSample next) {
        if(level==null||level.isClientSide)return;
        if(next!=null&&(!next.member().equals(memberGeneration)||!next.memberPos().equals(worldPosition)))
            throw new IllegalArgumentException("运行样本转子身份错误");
        if(sample==null&&next==null)return;
        sample=next;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag=new CompoundTag();tag.putUUID("RuntimeMember",memberGeneration);
        runtimeDisplay().ifPresent(value->tag.put("TurbineRuntime",value.encode()));return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider registries){receiveTag(tag);}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider registries){receiveTag(packet.getTag());}
    private void receiveTag(CompoundTag tag) {
        if(level!=null&&!level.isClientSide)return;
        UUID incoming=tag.hasUUID("RuntimeMember")?tag.getUUID("RuntimeMember"):null;
        // 合并方块更新可能保留客户端 BE；成员替换必须由租约核对更高的权威成型世代。
        clientMember=incoming;
        sample=TurbineRuntimeSample.decode(tag.getCompound("TurbineRuntime")).orElse(null);
        NeoForge.EVENT_BUS.post(new TurbineRuntimeSyncEvent(this));
    }
    /** 物理客户端收到原生整包后调用；身份必须来自同世界当前控制器，失败立即撤销。 */
    public void acceptRuntimeClient(long now) {
        clientTick=now;
        if(level==null||!level.isClientSide||sample==null||!bindingValid(sample)) {lease.revoke();return;}
        var owner=(TurbineControllerBlockEntity)level.getBlockEntity(sample.owner());
        if(lease.receive(sample,now,owner.runtimeGeneration(),owner.runtimeEpoch(),clientMember))clientOwner=new WeakReference<>(owner);
    }
    /** 客户端事件只推进活动时钟并核验身份；禁止以旧 pending 包续租。 */
    public void tickRuntimeClient(long now){clientTick=now;runtimeDisplay();}
    /** 世界或区块卸载立即撤销，暂停也必须执行。 */
    public void revokeRuntimeClient(){lease.revoke();}
    /** 所属任一几何区块卸载会撤销租约，区块回归必须等新包。 */
    public void unloadRuntimeChunk(ChunkPos chunk){if(sample!=null&&sample.requiredChunks().contains(chunk))lease.revoke();}
    private boolean bindingValid(TurbineRuntimeSample value) {
        if(level==null||isRemoved()||!value.dimension().equals(level.dimension().location().toString())
                ||!value.memberPos().equals(worldPosition)||!level.hasChunkAt(worldPosition)
                ||level.getBlockEntity(worldPosition)!=this||!getBlockState().is(TurbineContent.ROTOR.get())
                ||!getBlockState().getValue(TurbinePartBlock.FORMED)
                ||getBlockState().getValue(TurbinePartBlock.MACHINE_FACING)!=value.facing()
                ||getBlockState().getValue(TurbinePartBlock.DIAMETER).blocks()!=value.diameter())return false;
        for(ChunkPos chunk:value.requiredChunks())if(!level.hasChunk(chunk.x,chunk.z))return false;
        // 两个权威轴位来自同一包的几何；本地拆轴时不等待迟到的控制器更新包。
        for(int end=0;end<2;end++) {
            BlockPos axis=TurbineStructure.at(value.front(),value.facing(),0,0,end==0?0:value.rotors()+1);
            BlockState state=level.getBlockState(axis);
            if(!state.is(TurbineContent.OUTPUT_SHAFT.get())||!state.getValue(TurbinePartBlock.FORMED)
                    ||state.getValue(TurbinePartBlock.MACHINE_FACING)!=value.facing()
                    ||state.getValue(TurbineShaftBlock.END)!=(end==0?TurbineShaftBlock.End.FRONT:TurbineShaftBlock.End.REAR))return false;
        }
        if(!(level.getBlockEntity(value.owner()) instanceof TurbineControllerBlockEntity owner)||owner.isRemoved()
                ||!value.generation().equals(owner.runtimeGeneration())||value.epoch()!=owner.runtimeEpoch())return false;
        return true;
    }
    @Override public void onLoad(){super.onLoad();if(level!=null&&level.isClientSide)NeoForge.EVENT_BUS.post(new TurbineRuntimeSyncEvent(this));}
    @Override public void setRemoved(){lease.revoke();sample=null;super.setRemoved();}
    @Override public void onChunkUnloaded(){lease.revoke();sample=null;super.onChunkUnloaded();}
}
