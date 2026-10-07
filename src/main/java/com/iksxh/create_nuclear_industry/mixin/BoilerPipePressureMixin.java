package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.boiler.BoilerPressureConnection;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.FluidNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 仅记录本模组锅炉在 Create 管连接上的压力份额。原生 wipe 同时清除记录，
 * 下一次锅炉 tick 才重新加压；读盘先扣除已保存份额，避免重新加载时翻倍。
 */
@Mixin(value = PipeConnection.class, remap = false)
abstract class BoilerPipePressureMixin implements BoilerPressureConnection {
    @Unique private final Map<Long, float[]> createNuclearIndustry$boilerPressure = new HashMap<>();
    @Shadow private Optional<FluidNetwork> network;

    @Override
    public void createNuclearIndustry$forgetSteamEndpointNetwork() {
        // 原生reset保留旧source provider；快速跨汽种后该provider已永久失效，必须让manageFlows新建网络。
        // 只丢第三层对象，第二层流体和全部压力贡献继续由Create管理，不能改写任何外部库存。
        network = Optional.empty();
    }

    @Override
    public void createNuclearIndustry$setBoilerPressure(long owner, boolean inbound, float pressure) {
        PipeConnection connection = (PipeConnection) (Object) this;
        float[] previous = createNuclearIndustry$boilerPressure.computeIfAbsent(owner, ignored -> new float[2]);
        int index = inbound ? 0 : 1;
        float target = Float.isFinite(pressure) ? Math.max(0, pressure) : 0;
        float difference = target - previous[index];
        if (difference != 0) {
            connection.addPressure(inbound, difference);
            previous[index] = target;
        }
        if (previous[0] == 0 && previous[1] == 0) createNuclearIndustry$boilerPressure.remove(owner);
    }

    @Inject(method = "wipePressure", at = @At("TAIL"), remap = false)
    private void createNuclearIndustry$afterWipe(CallbackInfo callback) {
        createNuclearIndustry$boilerPressure.clear();
    }

    @Inject(method = "serializeNBT", at = @At("TAIL"), remap = false)
    private void createNuclearIndustry$writeOwnedPressure(CompoundTag tag, HolderLookup.Provider registries,
                                                           boolean clientPacket, CallbackInfo callback) {
        if (createNuclearIndustry$boilerPressure.isEmpty()) return;
        PipeConnection connection = (PipeConnection) (Object) this;
        float inbound = 0, outward = 0;
        for (float[] contribution : createNuclearIndustry$boilerPressure.values()) {
            inbound += contribution[0];
            outward += contribution[1];
        }
        CompoundTag connectionData = tag.getCompound(connection.side.getName());
        connectionData.putFloat("CniBoilerPressureIn", inbound);
        connectionData.putFloat("CniBoilerPressureOut", outward);
    }

    @Inject(method = "deserializeNBT", at = @At("TAIL"), remap = false)
    private void createNuclearIndustry$readOwnedPressure(CompoundTag tag, HolderLookup.Provider registries,
                                                          net.minecraft.core.BlockPos pos, boolean clientPacket,
                                                          CallbackInfo callback) {
        createNuclearIndustry$boilerPressure.clear();
        if (clientPacket) return;
        PipeConnection connection = (PipeConnection) (Object) this;
        CompoundTag data = tag.getCompound(connection.side.getName());
        float inbound = data.getFloat("CniBoilerPressureIn");
        float outward = data.getFloat("CniBoilerPressureOut");
        if (Float.isFinite(inbound) && inbound > 0)
            connection.addPressure(true, -Math.min(inbound, connection.getPressure().getFirst()));
        if (Float.isFinite(outward) && outward > 0)
            connection.addPressure(false, -Math.min(outward, connection.getPressure().getSecond()));
    }
}
