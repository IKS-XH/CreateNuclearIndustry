package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** 每个服务端事故的唯一物料账；不持有世界引用。已完成记录保留去重，物料只在托管或目标一处可取。 */
public final class ReactorAccidentLedger {
    /** 单列真实组件的托管。原件留作审计，只有payload是可移交载荷，finish后永久为空。 */
    public static final class Column {
        public final BlockPos port, bottom;
        public final UUID portGeneration;
        private final ItemStack original;
        private ItemStack payload;
        private final boolean waste;
        private boolean captured, settled;
        public Column(BlockPos port, BlockPos bottom, UUID generation, ItemStack original, boolean waste) {
            this.port = port.immutable(); this.bottom = bottom.immutable(); portGeneration = generation;
            this.original = original.copy(); payload = original.copy(); this.waste = waste;
        }
        public ItemStack original() { return original.copy(); }
        public ItemStack payload() { return payload.copy(); }
        public boolean waste() { return waste; }
        public boolean captured() { return captured; }
        public boolean settled() { return settled; }
        /** 只能在原端口验证并移走真实单件后调用；不能用事件投影调用此入口。 */
        public void capture(ItemStack actualPayload) {
            if (captured || settled) return;
            payload = actualPayload.copy(); captured = true;
        }
        /** 目标已接受唯一载荷后写收据；重复调用不再产生输出。 */
        public void finish() { payload = ItemStack.EMPTY; settled = true; }
        public CompoundTag save(HolderLookup.Provider registries) {
            var t = new CompoundTag(); t.putLong("Port", port.asLong()); t.putLong("Bottom", bottom.asLong());
            t.putUUID("Generation", portGeneration); t.putBoolean("Waste", waste);
            t.putBoolean("Captured", captured); t.putBoolean("Settled", settled);
            if (!original.isEmpty()) t.put("Original", original.save(registries));
            if (!payload.isEmpty()) t.put("Payload", payload.save(registries)); return t;
        }
        public static Column load(CompoundTag t, HolderLookup.Provider registries) {
            var c = new Column(BlockPos.of(t.getLong("Port")), BlockPos.of(t.getLong("Bottom")), t.getUUID("Generation"),
                    ItemStack.parseOptional(registries, t.getCompound("Original")), t.getBoolean("Waste"));
            c.payload = ItemStack.parseOptional(registries, t.getCompound("Payload"));
            c.captured = t.getBoolean("Captured"); c.settled = t.getBoolean("Settled");
            if (c.settled) c.payload = ItemStack.EMPTY; return c;
        }
    }
    /** 一个确定性世界目标及一次性收据；expected是创建时方块状态ID，避免重放破坏替换物。 */
    public static final class Action {
        public final BlockPos pos; public final int expected; public final boolean external;
        public final UUID entityGeneration;
        public boolean done;
        public Action(BlockPos pos, int expected, boolean external, UUID generation) {
            this.pos = pos.immutable(); this.expected = expected; this.external = external; entityGeneration = generation;
        }
        CompoundTag save() {
            var t = new CompoundTag(); t.putLong("Pos", pos.asLong()); t.putInt("Expected", expected);
            t.putBoolean("External", external); t.putBoolean("Done", done);
            if (entityGeneration != null) t.putUUID("Entity", entityGeneration); return t;
        }
        static Action load(CompoundTag t) {
            var a = new Action(BlockPos.of(t.getLong("Pos")), t.getInt("Expected"), t.getBoolean("External"),
                    t.hasUUID("Entity") ? t.getUUID("Entity") : null); a.done = t.getBoolean("Done"); return a;
        }
    }
    public final UUID id, generation;
    public final String dimension, reason;
    public final BlockPos owner, min, max;
    public final AccidentParameters parameters;
    public final long coldMb, hotMb;
    public final double releasedHeatHu;
    public final List<Column> columns;
    public final List<Action> actions;
    public final Set<UUID> shocked = new HashSet<>();
    /** 创建时已有效的邻堆世代允许承受排定壳损伤；之后新成型的世代始终受保护。 */
    public final Set<UUID> knownOwners = new HashSet<>();
    public boolean shockComplete;
    public int cursor, worldActions, externalBroken, peakActions;
    private boolean completionReceipt;
    public ReactorAccidentLedger(UUID id, UUID generation, String dimension, String reason, BlockPos owner,
            BlockPos min, BlockPos max, AccidentParameters parameters, long cold, long hot, double heat,
            List<Column> columns, List<Action> actions) {
        this.id=id; this.generation=generation; this.dimension=dimension; this.reason=reason;
        this.owner=owner.immutable(); this.min=min.immutable(); this.max=max.immutable(); this.parameters=parameters;
        coldMb=cold; hotMb=hot; releasedHeatHu=heat; this.columns=new ArrayList<>(columns); this.actions=new ArrayList<>(actions);
    }
    /** 任务只从未完成走向完成；已确认终态不再逐tick重扫历史列/动作。此派生缓存不持久化。 */
    public boolean complete() {
        if(!completionReceipt)completionReceipt=shockComplete&&columns.stream().allMatch(Column::settled)&&actions.stream().allMatch(a->a.done);
        return completionReceipt;
    }
    public CompoundTag save(HolderLookup.Provider registries) {
        var t=new CompoundTag(); t.putUUID("Id",id); t.putUUID("Generation",generation); t.putString("Dimension",dimension);
        t.putString("Reason",reason); t.putLong("Owner",owner.asLong()); t.putLong("Min",min.asLong()); t.putLong("Max",max.asLong());
        t.put("Parameters",parameters.save()); t.putLong("ColdMb",coldMb); t.putLong("HotMb",hotMb); t.putDouble("HeatHu",releasedHeatHu);
        var c=new ListTag(); columns.forEach(v->c.add(v.save(registries))); t.put("Columns",c);
        var a=new ListTag(); actions.forEach(v->a.add(v.save())); t.put("Actions",a);
        var s=new ListTag(); shocked.forEach(v->{var p=new CompoundTag(); p.putUUID("Player",v); s.add(p);}); t.put("Shocked",s);
        var owners=new ListTag();knownOwners.forEach(v->{var p=new CompoundTag();p.putUUID("Generation",v);owners.add(p);});t.put("KnownOwners",owners);
        t.putBoolean("ShockComplete",shockComplete); t.putInt("Cursor",cursor); t.putInt("WorldActions",worldActions);
        t.putInt("ExternalBroken",externalBroken); t.putInt("Peak",peakActions); return t;
    }
    public static ReactorAccidentLedger load(CompoundTag t, HolderLookup.Provider registries) {
        List<Column> c=new ArrayList<>(); for(Tag v:t.getList("Columns",Tag.TAG_COMPOUND)) c.add(Column.load((CompoundTag)v,registries));
        List<Action> a=new ArrayList<>(); for(Tag v:t.getList("Actions",Tag.TAG_COMPOUND)) a.add(Action.load((CompoundTag)v));
        var l=new ReactorAccidentLedger(t.getUUID("Id"),t.getUUID("Generation"),t.getString("Dimension"),t.getString("Reason"),
                BlockPos.of(t.getLong("Owner")),BlockPos.of(t.getLong("Min")),BlockPos.of(t.getLong("Max")),
                AccidentParameters.load(t.getCompound("Parameters")),t.getLong("ColdMb"),t.getLong("HotMb"),t.getDouble("HeatHu"),c,a);
        for(Tag v:t.getList("Shocked",Tag.TAG_COMPOUND)) l.shocked.add(((CompoundTag)v).getUUID("Player"));
        for(Tag v:t.getList("KnownOwners",Tag.TAG_COMPOUND))l.knownOwners.add(((CompoundTag)v).getUUID("Generation"));
        l.shockComplete=t.getBoolean("ShockComplete"); l.cursor=t.getInt("Cursor"); l.worldActions=t.getInt("WorldActions");
        l.externalBroken=t.getInt("ExternalBroken"); l.peakActions=t.getInt("Peak"); return l;
    }
}
