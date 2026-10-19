package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.blockentity.P1MinimalBlockEntity;
import com.iksxh.create_nuclear_industry.content.ReactorAutomationContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** 接口只持久化玩家设定；核状态、加载交接和当前运行权仍由仪表持有。 */
public final class ReactorAutomationPortBlockEntity extends P1MinimalBlockEntity {
    /** 玩家偏好仅含模式、百分比棒深和毛HU/t目标；设置不包含当前运行许可。 */
    public record Settings(ReactorAutomationState.Mode mode,int manualDepthPercent,int targetHeatHuPerTick) {
        public Settings {
            if(mode==null || manualDepthPercent<0 || manualDepthPercent>100 || targetHeatHuPerTick<0)
                throw new IllegalArgumentException("管理员设定非法");
        }
    }
    private com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity owner;
    /** 只保存当前已加载所有者身份，失联不加载区块，也不持久化此引用。 */
    public com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity owner() {
        return owner != null && !owner.isRemoved() && owner.getLevel() == level && owner.structureValid() ? owner : null;
    }
    public void bindOwner(com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity next) { owner=next; }
    /** 在实例撤离前清除旧所有者并立即撤权；同坐标重放不得继承旧面板会话。 */
    public void invalidateOwner() {
        var previous=owner;owner=null;
        ReactorAutomationService.onPortRemoved(previous,this);
    }
    /** Create的final setRemoved通过remove回调；覆盖此真实入口而非final方法。 */
    @Override public void remove() { invalidateOwner();super.remove(); }
    @Override public void onChunkUnloaded() { invalidateOwner();super.onChunkUnloaded(); }
    private Settings settings=new Settings(ReactorAutomationState.Mode.MANUAL,100,0);
    private long settingsRevision;
    public long settingsRevision() { return settingsRevision; }
    public ReactorAutomationPortBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorAutomationContent.PORT_ENTITY.get(),pos,state);
    }
    public Settings settings() { return settings; }
    /** 仅服务端校验命令后写入；客户端只消费NBT显示副本。 */
    public void applySettings(Settings next) {
        if(level!=null && level.isClientSide) throw new IllegalStateException("客户端不能提交管理员设置");
        // 当前菜单仍能查看/STOP，但加载期间不能原子提交新偏好覆盖待核验意图；START/RESET由原phase拒绝。
        if(owner!=null&&ReactorAutomationResume.pending(owner))throw new IllegalArgumentException("startup_interlock");
        if(!next.equals(settings)) { long revision=Math.addExact(settingsRevision,1); settings=next; settingsRevision=revision; setChanged(); }
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider lookup,boolean packet) {
        super.write(tag,lookup,packet);
        tag.putInt("AutomationMode",settings.mode().ordinal());
        tag.putInt("AutomationDepth",settings.manualDepthPercent());
        tag.putInt("AutomationHeat",settings.targetHeatHuPerTick());
        tag.putLong("AutomationSettingsRevision",settingsRevision);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet) {
        super.read(tag,lookup,packet);
        int mode=tag.getInt("AutomationMode");
        settingsRevision=Math.max(0,tag.getLong("AutomationSettingsRevision"));
        // 无法识别的保存模式不构造可运行偏好；实际运行仍需当前服务端会话授予许可。
        var savedMode=mode==ReactorAutomationState.Mode.MANUAL.ordinal()?ReactorAutomationState.Mode.MANUAL
                :mode==ReactorAutomationState.Mode.CONSTANT.ordinal()?ReactorAutomationState.Mode.CONSTANT:ReactorAutomationState.Mode.STOP;
        settings=new Settings(savedMode,
                Math.max(0,Math.min(100,tag.contains("AutomationDepth")?tag.getInt("AutomationDepth"):100)),
                Math.max(0,tag.getInt("AutomationHeat")));
    }
}
