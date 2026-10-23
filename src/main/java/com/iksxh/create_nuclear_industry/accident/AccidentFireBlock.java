package com.iksxh.create_nuclear_industry.accident;

import java.util.UUID;
import com.iksxh.create_nuclear_industry.content.AccidentContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 有身份租约的有限事故火；保留原生火的接触/扑灭形状，不调度蔓延、门户、声效或粒子。 */
public final class AccidentFireBlock extends BaseFireBlock implements EntityBlock {
    public static final MapCodec<AccidentFireBlock> CODEC=simpleCodec(AccidentFireBlock::new);
    public AccidentFireBlock(Properties properties){super(properties,1);}
    @Override protected MapCodec<? extends BaseFireBlock> codec(){return CODEC;}
    @Override protected boolean canBurn(BlockState state){return false;}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof Lease lease&&lease.dangerous())super.entityInside(state,level,pos,entity);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Lease(pos,state);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof Lease lease)lease.extinguished();
        super.onRemove(state,level,pos,next,moving);
    }
    /** 方块实体仅保存火点身份，不存计时或物料；同坐标后来火点不会继承旧租约。 */
    public static final class Lease extends BlockEntity {
        private UUID accident, identity;
        public Lease(BlockPos pos,BlockState state){super(AccidentContent.FIRE_BE.get(),pos,state);}
        public void bind(UUID accident,UUID identity){this.accident=accident;this.identity=identity;setChanged();}
        public boolean matches(UUID accident,UUID identity){return accident.equals(this.accident)&&identity.equals(this.identity);}
        public boolean dangerous() {
            if(!(level instanceof net.minecraft.server.level.ServerLevel server)||accident==null)return false;
            var ledger=ReactorAccidentSavedData.get(server).byId(accident);var f=ledger==null?null:ledger.thermal.fire(identity);
            return f!=null&&f.pos.equals(worldPosition)&&f.dangerous();
        }
        public void extinguished() {
            if(!(level instanceof net.minecraft.server.level.ServerLevel server)||accident==null)return;
            var data=ReactorAccidentSavedData.get(server);var ledger=data.byId(accident);var f=ledger==null?null:ledger.thermal.fire(identity);
            if(f!=null){ledger.thermal.endFire(f);ledger.thermal.cleaned(f);data.refreshWork(ledger);data.setDirty();}
        }
        @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
            super.saveAdditional(tag,registries);if(accident!=null){tag.putUUID("Accident",accident);tag.putUUID("Identity",identity);}
        }
        @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
            super.loadAdditional(tag,registries);accident=tag.hasUUID("Accident")?tag.getUUID("Accident"):null;
            identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):null;
        }
    }
}
