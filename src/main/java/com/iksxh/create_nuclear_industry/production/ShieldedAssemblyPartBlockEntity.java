package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** 从属格仅保存主控坐标和归属标识，不保存物料、不承担动力或生产tick。 */
public final class ShieldedAssemblyPartBlockEntity extends BlockEntity {
    private BlockPos masterPos;
    private UUID ownerId;

    public ShieldedAssemblyPartBlockEntity(BlockPos pos, BlockState state) {
        super(FuelProcessingContent.SHIELDED_ASSEMBLY_PART_BE.get(), pos, state);
    }

    public void bind(BlockPos masterPos, UUID ownerId) {
        this.masterPos = masterPos.immutable();
        this.ownerId = ownerId;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        if (level instanceof ServerLevel server) server.scheduleTick(worldPosition, getBlockState().getBlock(), 20);
    }

    public BlockPos masterPos() { return masterPos; }
    public UUID ownerId() { return ownerId; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (masterPos != null && ownerId != null) {
            tag.putLong("MasterPos", masterPos.asLong());
            tag.putUUID("OwnerId", ownerId);
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        masterPos = tag.contains("MasterPos") ? BlockPos.of(tag.getLong("MasterPos")) : null;
        ownerId = tag.hasUUID("OwnerId") ? tag.getUUID("OwnerId") : null;
    }

    /** 初始区块同步和后续归属更新都携带同一坐标/UUID，供客户端护目镜代理定位。 */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 从属加载后启动有限间隔归属复核；主控区块暂不可见时等待后续计划刻。 */
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server)
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 20);
    }

    /** 返回false只表示主控区块尚未加载；已加载且归属错误的代理静默清理，不制造掉落。 */
    public boolean verifyOwnerIfLoaded() {
        if (level == null || isRemoved() || !level.hasChunkAt(worldPosition)) return true;
        if (masterPos != null && !level.hasChunkAt(masterPos)) return false;
        if (masterPos == null || !(level.getBlockEntity(masterPos) instanceof ShieldedAssemblyBlockEntity master)
                || !master.matchesOwner(ownerId)
                || ShieldedAssemblyStructure.master(level, worldPosition, getBlockState()) != master)
            level.removeBlock(worldPosition, false);
        return true;
    }
}
