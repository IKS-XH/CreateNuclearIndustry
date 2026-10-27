package com.iksxh.create_nuclear_industry.boiler;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 一次事故的冻结世界计划；方向、参数、对象绑定及进度独立于控制器保存，不能重抽或重复伤害。 */
public final class BoilerAccidentLedger {
    public final UUID id,generation;
    public final BlockPos owner,origin;
    public final Direction direction;
    public final boolean burst;
    public final double pressure,radius;
    public final BoilerAccidentState.Parameters parameters;
    public final List<Target> targets;
    public final Set<UUID> damaged=new HashSet<>();
    public int cursor;
    public boolean shockComplete;
    /** 原部件持久身份；运行状态不参与身份比对，同态新放置会显式撤销旧认领。 */
    public static final class Target {
        private static final String IDENTITY="CniBoilerAccidentTarget";
        public final BlockPos pos;
        public final BlockState expected;
        private UUID entityIdentity;
        private boolean expectsEntity,invalidated;
        public Target(ServerLevel level,BlockPos pos) {
            this.pos=pos.immutable();expected=level.getBlockState(pos);bind(level);
        }
        private Target(BlockPos pos,BlockState expected,boolean expectsEntity,UUID entityIdentity,boolean invalidated) {
            this.pos=pos;this.expected=expected;this.expectsEntity=expectsEntity;this.entityIdentity=entityIdentity;this.invalidated=invalidated;
        }
        /** 真实事故冻结时给原BE分配一次持久凭证；只标识对象，不复制/扣取运行库存或热量。 */
        public void bind(ServerLevel level){
            if(entityIdentity!=null)return;var be=level.getBlockEntity(pos);expectsEntity=be!=null;
            if(be!=null){
                var tag=be.getPersistentData();
                if(!tag.hasUUID(IDENTITY)){
                    tag.putUUID(IDENTITY,UUID.randomUUID());
                    // 内部身份不改变红石输出；仅将已加载原区块置脏，避免setChanged读取导体后的第二邻格。
                    var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                    if(chunk!=null)chunk.setUnsaved(true);
                }
                entityIdentity=tag.getUUID(IDENTITY);
            }
        }
        public boolean matches(ServerLevel level) {
            var actual=level.getBlockState(pos);
            if(invalidated||!actual.is(expected.getBlock())||!expectsEntity&&actual!=expected)return false;
            return matchesEntity(level.getBlockEntity(pos));
        }
        /** 普通采掘getDrops发生在removeBlock之后，须核LootContext原BE而不能查已为空气的现场。 */
        public boolean ownsDrop(BlockState state,BlockEntity original){return !invalidated&&state.is(expected.getBlock())&&matchesEntity(original);}
        private boolean matchesEntity(BlockEntity be){return expectsEntity?be!=null&&entityIdentity!=null&&be.getPersistentData().hasUUID(IDENTITY)&&entityIdentity.equals(be.getPersistentData().getUUID(IDENTITY)):be==null;}
        public void replaced(){invalidated=true;}
        CompoundTag save(){var t=new CompoundTag();t.putLong("Pos",pos.asLong());t.put("State",NbtUtils.writeBlockState(expected));t.putBoolean("HasEntity",expectsEntity);if(entityIdentity!=null)t.putUUID("Identity",entityIdentity);t.putBoolean("Invalidated",invalidated);return t;}
        static Target load(CompoundTag t,HolderLookup.Provider r){return new Target(BlockPos.of(t.getLong("Pos")),NbtUtils.readBlockState(r.lookupOrThrow(Registries.BLOCK),t.getCompound("State")),t.getBoolean("HasEntity"),t.hasUUID("Identity")?t.getUUID("Identity"):null,t.getBoolean("Invalidated"));}
    }
    public BoilerAccidentLedger(UUID id,UUID generation,BlockPos owner,BlockPos origin,Direction direction,boolean burst,
            double pressure,BoilerAccidentState.Parameters parameters,List<Target> targets) {
        this.id=id;this.generation=generation;this.owner=owner.immutable();this.origin=origin.immutable();this.direction=direction;
        this.burst=burst;this.pressure=pressure;this.parameters=parameters;this.radius=burst?parameters.radius(pressure):0;this.targets=List.copyOf(targets);
        shockComplete=!burst;
    }
    public boolean complete(){return shockComplete&&cursor>=targets.size();}
    /** 以外表面破口为球心的朝外半球；不从大炉中心计算，更不会伤到整座炉的背面。 */
    public boolean inJet(Vec3 point) {
        Vec3 delta=point.subtract(Vec3.atCenterOf(origin));
        return delta.dot(Vec3.atLowerCornerOf(direction.getNormal()))>=-.01&&delta.lengthSqr()<=radius*radius;
    }
    public CompoundTag save(){
        var t=new CompoundTag();t.putUUID("Id",id);t.putUUID("Generation",generation);t.putLong("Owner",owner.asLong());t.putLong("Origin",origin.asLong());
        t.putInt("Direction",direction.get3DDataValue());t.putBoolean("Burst",burst);t.putDouble("Pressure",pressure);t.put("Parameters",BoilerAccidentState.saveParameters(parameters));
        t.putInt("Cursor",cursor);t.putBoolean("ShockComplete",shockComplete);var list=new ListTag();targets.forEach(a->list.add(a.save()));t.put("Targets",list);
        var receipts=new ListTag();damaged.forEach(id->{var r=new CompoundTag();r.putUUID("Entity",id);receipts.add(r);});t.put("Damaged",receipts);return t;
    }
    public static BoilerAccidentLedger load(CompoundTag t,HolderLookup.Provider r){
        var targets=new ArrayList<Target>();for(Tag item:t.getList("Targets",Tag.TAG_COMPOUND))targets.add(Target.load((CompoundTag)item,r));
        var l=new BoilerAccidentLedger(t.getUUID("Id"),t.getUUID("Generation"),BlockPos.of(t.getLong("Owner")),BlockPos.of(t.getLong("Origin")),Direction.from3DDataValue(t.getInt("Direction")),t.getBoolean("Burst"),t.getDouble("Pressure"),BoilerAccidentState.loadParameters(t.getCompound("Parameters")),targets);
        l.cursor=Math.clamp(t.getInt("Cursor"),0,targets.size());l.shockComplete=t.getBoolean("ShockComplete");
        for(Tag item:t.getList("Damaged",Tag.TAG_COMPOUND)){var e=(CompoundTag)item;if(e.hasUUID("Entity"))l.damaged.add(e.getUUID("Entity"));}return l;
    }
}
