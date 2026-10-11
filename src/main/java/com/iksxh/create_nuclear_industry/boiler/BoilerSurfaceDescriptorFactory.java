package com.iksxh.create_nuclear_industry.boiler;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 纯投影已经通过缓存决策的 Form；不读世界、不重判结构，也不把运行状态当作成型状态。 */
public final class BoilerSurfaceDescriptorFactory {
    private BoilerSurfaceDescriptorFactory() { }

    /** 输入维度、owner、实例身份和世界坐标 Form；缺失或投影不合法时返回有界失效信封。 */
    public static BoilerSurfaceDescriptor project(String dimension, BlockPos owner, UUID generation, long revision,
                                                   BoilerStructure.Form form) {
        var unavailable = BoilerSurfaceDescriptor.unavailable(dimension, owner, generation, revision);
        if (form == null) return unavailable;
        try {
            BoilerSurfaceDescriptor.memberCount(form.min(), form.max(), form.partitionY());
            Map<BlockPos, String> roles = new HashMap<>();
            overlay(roles, form.windows(), 1); overlay(roles, List.of(owner), 2);
            overlay(roles, form.waterPorts(), 3); overlay(roles, form.steamPorts(), 4);
            overlay(roles, form.hotPorts(), 5); overlay(roles, form.coldPorts(), 6);
            overlay(roles, List.of(form.valve()), 7); overlay(roles, form.exchangers(), 8);
            var members = new ArrayList<BoilerSurfaceDescriptor.SurfaceMember>();
            for (BlockPos pos : BlockPos.betweenClosed(form.min(), form.max())) {
                var faces = BoilerSurfaceDescriptor.outwardFaces(pos, form.min(), form.max());
                if (faces.isEmpty()) continue;
                String role = roles.remove(pos);
                members.add(new BoilerSurfaceDescriptor.SurfaceMember(pos,
                        role == null ? BoilerSurfaceDescriptor.BLOCK_IDS.getFirst() : role, faces));
            }
            if (!roles.isEmpty()) return unavailable;
            return new BoilerSurfaceDescriptor(dimension, owner, generation, revision, true,
                    form.min(), form.max(), form.partitionY(), members);
        } catch (IllegalArgumentException | NullPointerException exception) { return unavailable; }
    }

    private static void overlay(Map<BlockPos, String> roles, List<BlockPos> positions, int id) {
        for (BlockPos pos : positions) if (roles.putIfAbsent(pos.immutable(), BoilerSurfaceDescriptor.BLOCK_IDS.get(id)) != null)
            throw new IllegalArgumentException("overlapping boiler surface roles");
    }
}
