package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.blockentity.P1MinimalBlockEntity;
import com.iksxh.create_nuclear_industry.content.ReactorAutomationContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** 接口只持久化玩家设定；核状态、加载交接和当前运行权仍由仪表持有。 */
public final class ReactorAutomationPortBlockEntity extends P1MinimalBlockEntity {
    /** 完整玩家偏好：棒深/余量为百分比，发热与最低值为毛HU/t；设置不包含当前运行许可。 */
    public record Settings(ReactorAutomationState.Mode mode,int manualDepthPercent,int targetHeatHuPerTick,int reservePercent,int minimumHeatHuPerTick) {
        public Settings(ReactorAutomationState.Mode mode,int depth,int heat) {this(mode,depth,heat,20,2);}
        public Settings {
            if (mode==null || manualDepthPercent<0 || manualDepthPercent>100 || targetHeatHuPerTick<0
                    ||reservePercent<0||reservePercent>90||minimumHeatHuPerTick<=0)
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
    private ReactorStressGaugeLink gaugeLink;
    public ReactorStressGaugeLink gaugeLink() {return gaugeLink;}
    /** 仅绑定意图通过服务器校验后保存坐标；同菜单版本推进，位置本身不是调节权。 */
    public void applyGaugeLink(ReactorStressGaugeLink link) {
        if(level!=null&&level.isClientSide)throw new IllegalStateException("客户端不能绑表");
        long revision=Math.addExact(settingsRevision,1);gaugeLink=link;settingsRevision=revision;setChanged();
    }
    public long settingsRevision() { return settingsRevision; }
    public ReactorAutomationPortBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorAutomationContent.PORT_ENTITY.get(),pos,state);
        if(com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.SPEC.isLoaded())settings=defaultSettings();
    }
    private static Settings defaultSettings() {
        var defaults=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.loadFollowParameters();int minimum=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.initialMinimumHeatHuPerTick();
        return new Settings(ReactorAutomationState.Mode.MANUAL,100,0,defaults.defaultTargetStressReservePercent(),minimum);
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
        tag.putInt("AutomationReserve",settings.reservePercent());tag.putInt("AutomationMinimumHeat",settings.minimumHeatHuPerTick());
        if(gaugeLink!=null) {tag.putString("StressGaugeDimension",gaugeLink.dimension());tag.putLong("StressGaugePosition",gaugeLink.position().asLong());}
        tag.putLong("AutomationSettingsRevision",settingsRevision);
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider lookup,boolean packet) {
        super.read(tag,lookup,packet);
        int mode=tag.getInt("AutomationMode");
        settingsRevision=Math.max(0,tag.getLong("AutomationSettingsRevision"));
        var defaults=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.SPEC.isLoaded()?defaultSettings():new Settings(ReactorAutomationState.Mode.MANUAL,100,0);
        // 未明确保存最低值时才构造出厂维护+1，不能改写已有玩家值。
        settings=new Settings(mode==3?ReactorAutomationState.Mode.LOAD_FOLLOW:mode==2?ReactorAutomationState.Mode.CONSTANT:ReactorAutomationState.Mode.MANUAL,
                Math.max(0,Math.min(100,tag.contains("AutomationDepth")?tag.getInt("AutomationDepth"):100)),
                Math.max(0,tag.getInt("AutomationHeat")),
                Math.max(0,Math.min(90,tag.contains("AutomationReserve")?tag.getInt("AutomationReserve"):defaults.reservePercent())),
                Math.max(1,tag.contains("AutomationMinimumHeat")?tag.getInt("AutomationMinimumHeat"):defaults.minimumHeatHuPerTick()));
        gaugeLink=tag.contains("StressGaugeDimension")&&tag.contains("StressGaugePosition")&&!tag.getString("StressGaugeDimension").isBlank()
                ?new ReactorStressGaugeLink(tag.getString("StressGaugeDimension"),BlockPos.of(tag.getLong("StressGaugePosition"))):null;
    }
}
