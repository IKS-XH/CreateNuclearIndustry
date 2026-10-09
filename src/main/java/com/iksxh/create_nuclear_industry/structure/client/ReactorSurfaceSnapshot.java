package com.iksxh.create_nuclear_industry.structure.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** 原子发布给模型线程的不可变成员索引；没有 Level、BE、NBT 或可变集合引用。 */
public final class ReactorSurfaceSnapshot {
    private static final ReactorSurfaceSnapshot EMPTY = new ReactorSurfaceSnapshot(Map.of());
    private final Map<BlockPos, Member> members;

    ReactorSurfaceSnapshot(Map<BlockPos, Member> members) {
        this.members = Map.copyOf(members);
    }

    public static ReactorSurfaceSnapshot empty() { return EMPTY; }

    /** 只返回该块真正朝外面的结构归属；内部、歧义、未知以及未加载均为空。 */
    public Optional<Member> findSurface(BlockPos pos, Direction face) {
        Member member = members.get(pos);
        return member != null && member.outwardFaces().contains(face) ? Optional.of(member) : Optional.empty();
    }

    Map<BlockPos, Member> members() { return members; }

    /** 不可变显示成员；世界坐标与 bounds 单位为方块，身份由 dimension/owner/generation 共同决定。 */
    public record Member(String dimension, BlockPos ownerPos, UUID ownerGeneration, long revision,
                         BlockPos origin, BlockPos maxInclusive, String expectedBlockId,
                         BlockPos pos, Set<Direction> outwardFaces) {
        public Member {
            ownerPos = ownerPos.immutable();
            origin = origin.immutable();
            maxInclusive = maxInclusive.immutable();
            pos = pos.immutable();
            outwardFaces = Set.copyOf(outwardFaces);
        }
    }
}
