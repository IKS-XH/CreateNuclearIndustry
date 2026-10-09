package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.UUID;

/** 只投影已经有效的权威扫描，固定 SIZE 仅出现在适配层；不重判成型、不加载缺失区块。 */
public final class ReactorSurfaceDescriptorFactory {
    private ReactorSurfaceDescriptorFactory() { }

    /** 服务器读取已加载表面实际 ID；任何证据缺失时整台返回失效信封。 */
    public static ReactorSurfaceDescriptor project(Level level, BlockPos owner, UUID generation, long revision,
                                                   ReactorStructureScanner.WorldScanResult scan) {
        String dimension = level.dimension().location().toString();
        var unavailable = ReactorSurfaceDescriptor.unavailable(dimension, owner, generation, revision);
        if (level.isClientSide || scan == null || !scan.valid() || scan.origin() == null) return unavailable;
        BlockPos origin = scan.origin();
        BlockPos max = origin.offset(ReactorStructureDefinition.SIZE - 1,
                ReactorStructureDefinition.SIZE - 1, ReactorStructureDefinition.SIZE - 1);
        var members = new ArrayList<ReactorSurfaceDescriptor.SurfaceMember>();
        try {
            for (BlockPos mutable : BlockPos.betweenClosed(origin, max)) {
                var faces = ReactorSurfaceDescriptor.outwardFaces(mutable, origin, max);
                if (faces.isEmpty()) continue;
                if (!level.hasChunkAt(mutable)) return unavailable;
                members.add(new ReactorSurfaceDescriptor.SurfaceMember(mutable,
                        BuiltInRegistries.BLOCK.getKey(level.getBlockState(mutable).getBlock()).toString(), faces));
            }
            return new ReactorSurfaceDescriptor(dimension, owner, generation, revision, true, origin, max, members);
        } catch (IllegalArgumentException exception) {
            return unavailable;
        }
    }
}
