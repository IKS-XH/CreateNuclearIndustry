package com.iksxh.create_nuclear_industry.turbine.display;

import com.iksxh.create_nuclear_industry.turbine.TurbineStructure;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/**
 * 服务端同次结算的单个转子只读样本。位置单位方块，index 从前轴后的首转子以 0 开始，
 * rpm 单位转/分，正轴为前轴指向后轴；0 是可靠停转，缺样本才表示不可用。
 * generation 为控制器实例，epoch 为成型世代，member 为转子实例；均不从存档恢复。
 */
public record TurbineRuntimeSample(String dimension, BlockPos owner, UUID generation, int epoch,
        BlockPos front, Direction facing, int diameter, int rotors, int index, UUID member,
        long serverTick, float rpm) {
    public TurbineRuntimeSample {
        if (dimension == null || ResourceLocation.tryParse(dimension) == null || generation == null || member == null
                || owner == null || front == null || facing == null || !facing.getAxis().isHorizontal()
                || epoch < 0 || serverTick < 0 || !Float.isFinite(rpm)
                || (diameter != 3 && diameter != 5 && diameter != 7)
                || rotors < 3 || rotors > 16 || index < 0 || index >= rotors
                || !positionValid(owner) || !positionValid(front)) throw new IllegalArgumentException("运行样本字段非法");
        owner = owner.immutable(); front = front.immutable();
        int dx=owner.getX()-front.getX(), dz=owner.getZ()-front.getZ();
        int x=dx*facing.getClockWise().getStepX()+dz*facing.getClockWise().getStepZ();
        int y=owner.getY()-front.getY();
        int z=dx*facing.getOpposite().getStepX()+dz*facing.getOpposite().getStepZ();
        int radius=(diameter-1)/2, length=rotors+2;
        if (!(z==length/2 || length%2==0 && z==length/2-1)
                || !((Math.abs(x)==radius && y==0) || (x==0 && Math.abs(y)==radius)))
            throw new IllegalArgumentException("控制器不在合法中段侧槽");
        if (!positionValid(TurbineStructure.at(front,facing,0,0,length-1)))
            throw new IllegalArgumentException("机组超出坐标边界");
    }
    private static boolean positionValid(BlockPos pos) {
        return Math.abs((long)pos.getX()) < 30_000_000 && Math.abs((long)pos.getZ()) < 30_000_000
                && pos.getY() >= -2048 && pos.getY() <= 2047;
    }
    /** 精确成员坐标；不搜索附近方块，也不借历史 FORMED 外观推断归属。 */
    public BlockPos memberPos() { return TurbineStructure.at(front,facing,0,0,index+1); }
    /** Create 以世界正轴存储转速；统一到前轴至后轴的局部正轴，不改变真实传动。 */
    public static float localRpm(Direction facing,float actualShaftRpm) {
        return actualShaftRpm*facing.getOpposite().getAxisDirection().getStep();
    }
    /** 有限几何所需区块，用于客户端缺块立即撤销；枚举不加载世界。 */
    public Set<ChunkPos> requiredChunks() {
        Set<ChunkPos> result=new HashSet<>(); int radius=(diameter-1)/2;
        BlockPos a=TurbineStructure.at(front,facing,-radius,0,0);
        BlockPos b=TurbineStructure.at(front,facing,radius,0,rotors+1);
        for(int x=Math.min(a.getX(),b.getX())>>4;x<=(Math.max(a.getX(),b.getX())>>4);x++)
            for(int z=Math.min(a.getZ(),b.getZ())>>4;z<=(Math.max(a.getZ(),b.getZ())>>4);z++)
                result.add(new ChunkPos(x,z));
        return Set.copyOf(result);
    }
    /** 仅用于原生 BE 更新信封，调用方不得写入磁盘或可携数据。 */
    public CompoundTag encode() {
        CompoundTag t=new CompoundTag();t.putString("Dimension",dimension);putPos(t,"Owner",owner);
        t.putUUID("Generation",generation);t.putInt("Epoch",epoch);putPos(t,"Front",front);
        t.putString("Facing",facing.getName());t.putInt("Diameter",diameter);t.putInt("Rotors",rotors);
        t.putInt("Index",index);t.putUUID("Member",member);t.putLong("Tick",serverTick);t.putFloat("Rpm",rpm);return t;
    }
    private static void putPos(CompoundTag t,String key,BlockPos p) {t.putIntArray(key,new int[]{p.getX(),p.getY(),p.getZ()});}
    private static BlockPos pos(CompoundTag t,String key) {
        int[] p=t.getIntArray(key);if(p.length!=3)throw new IllegalArgumentException("坐标必须三维");return new BlockPos(p[0],p[1],p[2]);
    }
    /** 严格检查字段类型及完整几何；任何坏字段都拒绝整包，绝不补用上一包字段。 */
    public static Optional<TurbineRuntimeSample> decode(CompoundTag t) {
        if(t==null)return Optional.empty();
        for(String key:new String[]{"Dimension","Facing"})if(!t.contains(key,Tag.TAG_STRING))return Optional.empty();
        for(String key:new String[]{"Owner","Front","Generation","Member"})if(!t.contains(key,Tag.TAG_INT_ARRAY))return Optional.empty();
        for(String key:new String[]{"Epoch","Diameter","Rotors","Index"})if(!t.contains(key,Tag.TAG_INT))return Optional.empty();
        if(!t.contains("Tick",Tag.TAG_LONG)||!t.contains("Rpm",Tag.TAG_FLOAT)||!t.hasUUID("Generation")||!t.hasUUID("Member"))return Optional.empty();
        try {return Optional.of(new TurbineRuntimeSample(t.getString("Dimension"),pos(t,"Owner"),t.getUUID("Generation"),t.getInt("Epoch"),
                pos(t,"Front"),Direction.byName(t.getString("Facing")),t.getInt("Diameter"),t.getInt("Rotors"),t.getInt("Index"),
                t.getUUID("Member"),t.getLong("Tick"),t.getFloat("Rpm")));}
        catch(IllegalArgumentException e){return Optional.empty();}
    }
}
