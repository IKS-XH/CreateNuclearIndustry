package com.iksxh.create_nuclear_industry.industrial;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/** 服务端单世界逐 tick 额度账本；节点身份共用额度，模拟绝不修改账本，单位 mB/t。 */
public final class IndustrialFlowBudget {
    private long tick = Long.MIN_VALUE;
    private final Long2IntOpenHashMap used = new Long2IntOpenHashMap();
    private final ArrayDeque<Path> temporary = new ArrayDeque<>();
    private final java.util.IdentityHashMap<Group,GroupState> grouped = new java.util.IdentityHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<GroupState> groupOwners = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();

    /** 无分支静态管段的共享额度身份；成员为物理坐标，两档容量分别计算，不持世界。 */
    static final class Group {
        final long[] nodes;
        final byte[] tiers;
        final int mask;
        boolean live=true;
        Group(long[] nodes,byte[] tiers) {
            if(nodes.length==0||nodes.length!=tiers.length)throw new IllegalArgumentException("静态管段成员无效");
            this.nodes=nodes.clone();this.tiers=tiers.clone();int mask=0;
            var unique=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
            for(int i=0;i<nodes.length;i++){if(!unique.add(nodes[i])||tiers[i]<0||tiers[i]>1)throw new IllegalArgumentException("静态管段重复或档位无效");mask|=1<<tiers[i];}
            this.mask=mask;
        }
        long key(){return nodes[0];}
        int capacity(int low,int high){return mask==1?low:mask==2?high:Math.min(low,high);}
    }
    /** 基础用量按档位取最大；只有整段一致增加的额度才暂存为delta，回退前展开。 */
    private static final class GroupState {
        final Group group;
        int low,high,delta;
        GroupState(Group group){this.group=group;}
        int remaining(int low,int high) {
            int left=Integer.MAX_VALUE;
            if((group.mask&1)!=0)left=Math.min(left,Math.max(0,low-this.low-delta));
            if((group.mask&2)!=0)left=Math.min(left,Math.max(0,high-this.high-delta));
            return left;
        }
    }

    /** 图分割/合并或原逐格入口重入前，把已收费虚拟边恢复成原物理坐标账本。 */
    void flushGroups() {
        for(var state:grouped.values())if(state.delta>0)for(long node:state.group.nodes)used.put(node,Math.addExact(used.get(node),state.delta));
        grouped.clear();groupOwners.clear();
    }
    private void beginTick(long now){if(now!=tick){used.clear();grouped.clear();groupOwners.clear();tick=now;}}
    private GroupState state(Group group) {
        GroupState prior=grouped.get(group);if(prior!=null)return prior;
        for(long node:group.nodes){var other=groupOwners.get(node);if(other!=null&&other.group!=group){flushGroups();break;}}
        var next=new GroupState(group);
        for(int i=0;i<group.nodes.length;i++){int value=used.get(group.nodes[i]);if(group.tiers[i]==0)next.low=Math.max(next.low,value);else next.high=Math.max(next.high,value);}
        grouped.put(group,next);for(long node:group.nodes)groupOwners.put(node,next);return next;
    }
    private int remaining(long now,Group group,int low,int high) {
        if(now!=tick)return Math.max(0,group.capacity(low,high));
        if(!group.live)flushGroups();
        return state(group).remaining(low,high);
    }

    /** 同一世界节点的本次有效上限；同路重复节点取最小值，只收费一次。 */
    public record Limit(long node, int capacity) {}

    /** 一笔同步交易独占的原始数值路径；重复额度取最小值，过路量取最大，不持有世界或许可。 */
    static final class Path {
        private long[] nodes=new long[16];
        private int[] capacities=new int[16],crossed=new int[16];
        private Group[] groups=new Group[16];
        private int[] low=new int[16],high=new int[16];
        private boolean[] unbounded=new boolean[16];
        private int size;
        private boolean compressed;
        private final Long2IntOpenHashMap indices=new Long2IntOpenHashMap();
        Path(){indices.defaultReturnValue(-1);}
        void clear(){Arrays.fill(groups,0,size,null);size=0;indices.clear();compressed=false;}
        void add(long node,int capacity) {
            int index=indices.get(node);
            if(index>=0){if(unbounded[index]&&groups[index]!=null)demote(groups[index]);capacities[index]=Math.min(capacities[index],capacity);unbounded[index]=false;return;}
            if(size==nodes.length){int length=size*2;nodes=Arrays.copyOf(nodes,length);capacities=Arrays.copyOf(capacities,length);crossed=Arrays.copyOf(crossed,length);groups=Arrays.copyOf(groups,length);low=Arrays.copyOf(low,length);high=Arrays.copyOf(high,length);unbounded=Arrays.copyOf(unbounded,length);}
            indices.put(node,size);nodes[size]=node;capacities[size]=capacity;crossed[size]=0;unbounded[size]=false;size++;
        }
        /** 普通管只登记介质/几何身份，不用大常量充当共享流量额度。重复的有限节点仍优先。 */
        void addUnbounded(long node) {
            if(indices.get(node)>=0)return;add(node,Integer.MAX_VALUE);unbounded[indices.get(node)]=true;
        }
        /** 已见证的纯管组仍保留用于介质标记，但不创建GroupState或累计用量。 */
        void addUnboundedGroup(Group group) {
            int index=indices.get(group.key());
            if(index<0){addUnbounded(group.key());index=indices.get(group.key());groups[index]=group;low[index]=high[index]=Integer.MAX_VALUE;}
            else if(groups[index]!=group){for(long node:group.nodes)addUnbounded(node);return;}
            compressed=true;
        }
        void addGroup(Group group,int low,int high) {
            int index=indices.get(group.key());
            if(index<0){add(group.key(),group.capacity(low,high));index=indices.get(group.key());groups[index]=group;this.low[index]=low;this.high[index]=high;}
            else {if(groups[index]!=group)throw new IllegalStateException("同路静态管段身份冲突");this.low[index]=Math.min(this.low[index],low);this.high[index]=Math.min(this.high[index],high);capacities[index]=Math.min(capacities[index],group.capacity(low,high));}
            unbounded[index]=false;
            compressed=true;
        }
        int size(){return size;}
        long key(int index){return nodes[index];}
        Group group(int index){return groups[index];}
        boolean hasGroup(Group group){int index=indices.get(group.key());return index>=0&&groups[index]==group;}
        /** 前半路已压缩、后半路因回调变更而回退时，先恢复本笔的逐格容量再取min。 */
        void demote(Group group) {
            int index=indices.get(group.key());if(index<0||groups[index]!=group)return;
            int lo=low[index],hi=high[index];boolean free=unbounded[index];groups[index]=null;capacities[index]=free?Integer.MAX_VALUE:group.tiers[0]==0?lo:hi;
            for(int i=1;i<group.nodes.length;i++){if(free)addUnbounded(group.nodes[i]);else add(group.nodes[i],group.tiers[i]==0?lo:hi);}
        }
        int capacity(long node){int index=indices.get(node);return index<0?0:capacities[index];}
        void cross(long node,int amount){int index=indices.get(node);if(index>=0)crossed[index]=Math.max(crossed[index],amount);}
        int takeCrossed(long node){int index=indices.get(node);if(index<0)return 0;int amount=crossed[index];crossed[index]=0;return amount;}
    }

    /** 转速只影响单泵输送额，正反转共用额定值；非法配置或非有限转速拒绝运输。 */
    public static int pumpLimit(int rated, double ratedRpm, double rpm) {
        if (rated <= 0 || !Double.isFinite(ratedRpm) || ratedRpm <= 0 || !Double.isFinite(rpm)) return 0;
        return (int) Math.floor(rated * Math.min(1, Math.abs(rpm) / ratedRpm));
    }

    /** 服务端只读跳过证明：自身物理额度已耗尽且无缓存，不能跨tick续用，也不清理旧账。 */
    boolean canSkipPump(long now,long node,int capacity,boolean hasCache) {
        if(hasCache||now!=tick)return false;
        var owner=groupOwners.get(node);long consumed=(long)used.get(node)+(owner==null?0:owner.delta);
        return consumed>=capacity;
    }

    /** 保留列表入口并租借独占临时路径；读取外部列表时重入不覆盖外层去重结果。 */
    public int reserve(long now, List<Limit> path, int requested, boolean execute) {
        flushGroups();
        if (requested <= 0 || path.isEmpty()) return 0;
        // 首节点已无余量时整条路径必拒绝；不遍历后续节点、不分配去重表，也不清理旧tick账本。
        Limit first = path.getFirst();
        if (first.capacity() <= 0 || now == tick && used.getOrDefault(first.node(), 0) >= first.capacity()) return 0;
        Path prepared=temporary.pollFirst();if(prepared==null)prepared=new Path();
        try {
            for(Limit limit:path){if(limit.capacity()<=0)return 0;prepared.add(limit.node(),limit.capacity());}
            return reserve(now,prepared,requested,execute);
        } finally {prepared.clear();temporary.addFirst(prepared);}
    }

    /** 仅读取本笔已采集的数值额度；调用者每笔仍实时查询路径资格，模拟跨tick不清账。 */
    int reserve(long now,Path path,int requested,boolean execute) {
        if(requested<=0||path.size==0)return 0;
        if(path.compressed)return reserveCompressed(now,path,requested,execute);
        flushGroups();
        int amount=requested;
        for(int i=0;i<path.size;i++) {
            if(path.unbounded[i])continue;
            int capacity=path.capacities[i];if(capacity<=0)return 0;
            amount=Math.min(amount,Math.max(0,capacity-(now==tick?used.get(path.nodes[i]):0)));
        }
        if(execute&&amount>0) {
            beginTick(now);
            for(int i=0;i<path.size;i++)if(!path.unbounded[i]){long node=path.nodes[i];used.put(node,Math.addExact(used.get(node),amount));}
        }
        return amount;
    }
    /** 与旧单元素List/Limit实收费等价，避免每个过路节点另建容器。 */
    int reserveSingle(long now,long node,int capacity,int requested,boolean execute) {
        flushGroups();return rawSingle(now,node,capacity,requested,execute);
    }
    private int rawSingle(long now,long node,int capacity,int requested,boolean execute) {
        if(requested<=0||capacity<=0)return 0;
        if(groupOwners.containsKey(node))flushGroups();
        int amount=Math.min(requested,Math.max(0,capacity-(now==tick?used.get(node):0)));
        if(execute&&amount>0){beginTick(now);used.put(node,Math.addExact(used.get(node),amount));}
        return amount;
    }
    private int reserveCompressed(long now,Path path,int requested,boolean execute) {
        int amount=requested;
        for(int i=0;i<path.size;i++) {
            if(path.unbounded[i])continue;
            if(path.capacities[i]<=0)return 0;
            var group=path.groups[i];
            if(group==null&&now==tick&&groupOwners.containsKey(path.nodes[i]))flushGroups();
            int left=group==null?Math.max(0,path.capacities[i]-(now==tick?used.get(path.nodes[i]):0)):remaining(now,group,path.low[i],path.high[i]);
            amount=Math.min(amount,left);
        }
        if(execute&&amount>0){beginTick(now);for(int i=0;i<path.size;i++)charge(now,path,path.nodes[i],amount);}
        return amount;
    }
    /** 沿原路径完成真实收费；回调重入耗尽部分成员时逐格截取，不能把整段一并少收费。 */
    int charge(long now,Path path,long key,int requested) {
        int i=path.indices.get(key);if(i<0||requested<=0)return 0;
        if(path.unbounded[i])return requested;
        var group=path.groups[i];
        if(group==null)return rawSingle(now,key,path.capacities[i],requested,true);
        if(group.live&&requested<=remaining(now,group,path.low[i],path.high[i])){beginTick(now);var current=state(group);current.delta=Math.addExact(current.delta,requested);return requested;}
        flushGroups();int charged=requested;
        for(int n=0;n<group.nodes.length;n++)charged=Math.min(charged,rawSingle(now,group.nodes[n],group.tiers[n]==0?path.low[i]:path.high[i],requested,true));
        return charged;
    }
}
