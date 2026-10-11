package com.iksxh.create_nuclear_industry.boiler.client;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.BiFunction;
import java.util.Objects;

/**
 * 客户端模型的共面连接规则：只消费该次重建捕获的不可变显示快照，不负责结构判定或同步。
 * bounds和邻接距离均以方块为单位；实体背景可跨合法材质，窗口绘图域仅连接窗口。
 */
public final class BoilerConnectedTextureBehaviour extends ConnectedTextureBehaviour {
    private final BiFunction<ResourceLocation, Boolean, CTSpriteShiftEntry> shifts;

    BoilerConnectedTextureBehaviour(BiFunction<ResourceLocation, Boolean, CTSpriteShiftEntry> shifts) {
        this.shifts = Objects.requireNonNull(shifts);
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction face, TextureAtlasSprite sprite) {
        // quad使用的真实sprite决定映射；entry在Catnip重载时更新，不能缓存atlas sprite对象。
        return sprite == null ? null : shifts.apply(sprite.contents().name(), BoilerConnectedTextures.isPartitionFace(face));
    }

    @Override
    public CTType getDataType(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction face) {
        var member = BoilerConnectedTextures.currentSnapshot(world).findSurface(pos, face).orElse(null);
        if (member == null) return null;
        String localId = blockId(world.getBlockState(pos));
        return localId.equals(blockId(state)) && isUsableMember(member, pos, face, localId)
                ? (isWindow(localId) ? AllCTTypes.OMNIDIRECTIONAL : AllCTTypes.RECTANGLE) : null;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter world,
                              BlockPos pos, BlockPos otherPos, Direction face) {
        var snapshot = BoilerConnectedTextures.currentSnapshot(world);
        var a = snapshot.findSurface(pos, face).orElse(null);
        var b = snapshot.findSurface(otherPos, face).orElse(null);
        if (a == null || b == null) return false;
        // 原生buildContext可能使用appearance；仍须核对两格实际局部状态，不能让外观代理替代合法材质。
        String localA = blockId(world.getBlockState(pos));
        String localB = blockId(world.getBlockState(otherPos));
        return localA.equals(blockId(state)) && localB.equals(blockId(other))
                && canConnect(a, b, localA, localB, face);
    }

    /** 核对一格的真实位置、合法材质及inclusive外平面；无记录时保守降级。 */
    static boolean isUsableMember(BoilerSurfaceSnapshot.Member member, BlockPos pos, Direction face, String localId) {
        if (member == null || !member.pos().equals(pos) || !member.outwardFaces().contains(face)
                || !member.expectedBlockId().equals(localId) || !BoilerConnectedTextures.supportsBlock(localId)) return false;
        BlockPos min = member.min();
        BlockPos max = member.maxInclusive();
        if (pos.getX() < min.getX() || pos.getX() > max.getX()
                || pos.getY() < min.getY() || pos.getY() > max.getY()
                || pos.getZ() < min.getZ() || pos.getZ() > max.getZ()) return false;
        int plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? max.get(face.getAxis()) : min.get(face.getAxis());
        return pos.get(face.getAxis()) == plane;
    }

    /**
     * 两格须同dimension/owner/generation、同描述版本和bounds，且是同一外平面的相邻格。
     * 实体背景仅四邻接；窗口只连接窗口，额外允许面内两切向轴各差一的严格对角。
     * 原生buildContext负责两条邻边门控，不能把轴向跨两格当作对角。long差值避免溢出。
     */
    static boolean canConnect(BoilerSurfaceSnapshot.Member a, BoilerSurfaceSnapshot.Member b,
                              String localA, String localB, Direction face) {
        if (a == null || b == null || !isUsableMember(a, a.pos(), face, localA)
                || !isUsableMember(b, b.pos(), face, localB)) return false;
        if (!a.dimension().equals(b.dimension()) || !a.ownerPos().equals(b.ownerPos())
                || !a.ownerGeneration().equals(b.ownerGeneration()) || a.revision() != b.revision()
                || !a.min().equals(b.min()) || !a.maxInclusive().equals(b.maxInclusive()) || a.partitionY() != b.partitionY()) return false;
        long dx = Math.abs((long) a.pos().getX() - b.pos().getX());
        long dy = Math.abs((long) a.pos().getY() - b.pos().getY());
        long dz = Math.abs((long) a.pos().getZ() - b.pos().getZ());
        if (!isWindow(localA)) return dx + dy + dz == 1;
        if (!isWindow(localB)) return false;
        if (dx + dy + dz == 1) return true;
        return switch (face.getAxis()) {
            case X -> dx == 0 && dy == 1 && dz == 1;
            case Y -> dy == 0 && dx == 1 && dz == 1;
            case Z -> dz == 0 && dx == 1 && dy == 1;
        };
    }

    /**
     * 同次模型快照中，仅隐藏具有共同可靠外向面F的切向窗→窗接口D。
     * 结果为六个Direction索引对应的位；无可靠窗口数据明确返回0，外圈/外向/非共享背面保留。
     */
    int sharedWindowFaces(BlockAndTintGetter world, BlockPos pos, BlockState state) {
        String localId = blockId(state);
        if (!isWindow(localId) || !localId.equals(blockId(world.getBlockState(pos)))) return 0;
        var snapshot = BoilerConnectedTextures.currentSnapshot(world);
        int mask = 0;
        for (Direction outward : Direction.values()) {
            var member = snapshot.findSurface(pos, outward).orElse(null);
            if (!isUsableMember(member, pos, outward, localId)) continue;
            for (Direction shared : Direction.values()) {
                if (shared.getAxis() == outward.getAxis() || member.outwardFaces().contains(shared)) continue;
                BlockPos neighbour = pos.relative(shared);
                if (connectsTo(state, world.getBlockState(neighbour), world, pos, neighbour, outward))
                    mask |= 1 << shared.get3DDataValue();
            }
        }
        return mask;
    }

    /** 可靠隔层面位图；绝对世界Y相等且为水平朝外面，局部替换和未知均返回0。 */
    int partitionFaces(BlockAndTintGetter world, BlockPos pos, BlockState state) {
        String local = blockId(state);
        if (!local.equals(blockId(world.getBlockState(pos)))) return 0;
        var snapshot = BoilerConnectedTextures.currentSnapshot(world);
        int mask = 0;
        for (Direction face : Direction.values()) {
            var member = snapshot.findSurface(pos, face).orElse(null);
            if (face.getAxis() != Direction.Axis.Y && isUsableMember(member, pos, face, local)
                    && pos.getY() == member.partitionY()) mask |= 1 << face.get3DDataValue();
        }
        return mask;
    }

    private static boolean isWindow(String localId) {
        return "create_nuclear_industry:high_pressure_boiler_window".equals(localId);
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
