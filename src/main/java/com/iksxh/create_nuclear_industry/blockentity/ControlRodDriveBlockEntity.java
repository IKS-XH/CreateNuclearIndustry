package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderBehaviour;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderPhase;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderResponsePayload;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentGoggleDisplay;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Create 滑块的驱动器宿主。
 *
 * <p>驱动器只保存客户端展示所需的列提示和滑块值，不复制反应堆快照；权威状态
 * 始终由仪表端口方块实体拥有，服务端校验也必须通过仪表端口完成。</p>
 */
public final class ControlRodDriveBlockEntity extends P1MinimalBlockEntity
        implements IHaveGoggleInformation {
    private static final String CONTROL_ROD_INTEGRITY_AVAILABLE_KEY =
            "ControlRodIntegrityAvailable";
    private static final String CONTROL_ROD_INTEGRITY_KEY = "ControlRodIntegrity";
    private ControlRodSliderBehaviour slider;
    private int clientColumnX = -1;
    private int clientColumnZ = -1;
    private Double serverControlRodIntegrity;
    private Double clientControlRodIntegrity;

    public ControlRodDriveBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.CONTROL_ROD_DRIVE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        slider = new ControlRodSliderBehaviour(this);
        behaviours.add(slider);
    }

    public ControlRodSliderBehaviour slider() {
        return slider;
    }

    public int clientColumnX() {
        return clientColumnX;
    }

    public int clientColumnZ() {
        return clientColumnZ;
    }

    /** 返回客户端最近一次同步的控制棒列完整度；服务端权威状态不通过该副本读取。 */
    public Double clientControlRodIntegrity() {
        return clientControlRodIntegrity;
    }

    /** 结构扫描提供列映射；驱动器不得在本地推断控制棒列。 */
    public void setServerColumnHint(int columnX, int columnZ) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("server slider column cannot be changed on the client");
        }
        if (clientColumnX == columnX && clientColumnZ == columnZ) {
            return;
        }
        clientColumnX = columnX;
        clientColumnZ = columnZ;
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /** 只更新 Create 控件显示的服务端非持久化值，并同步给客户端。 */
    public void setServerDisplayedDepthPercent(int depthPercent) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("server slider display cannot be changed on the client");
        }
        int clamped = Math.max(0, Math.min(100, depthPercent));
        if (slider.getValue() == clamped) {
            return;
        }
        slider.setServerDisplayedValue(clamped);
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /**
     * 同步该驱动器对应控制棒列的客户端显示完整度。
     *
     * <p>完整度来自仪表端口正式 tick 生成的遥测；空值表示结构有效但尚未完成一次
     * 成功正式 tick，数据只写入客户端更新包，不写入驱动器持久化状态。</p>
     */
    void setServerControlRodIntegrity(Double integrity) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("control rod integrity can only be changed on the server");
        }
        if (integrity != null
                && (!Double.isFinite(integrity) || integrity < 0.0D || integrity > 1.0D)) {
            throw new IllegalArgumentException("control rod integrity must be between 0 and 1");
        }
        if (java.util.Objects.equals(serverControlRodIntegrity, integrity)) {
            return;
        }
        serverControlRodIntegrity = integrity;
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /** 仅将最终服务端响应应用到客户端展示缓存，不写入客户端权威状态。 */
    public void applyClientSliderResponse(ControlRodSliderResponsePayload response) {
        if (level == null || !level.isClientSide || response == null) {
            return;
        }
        if (response.phase() != ControlRodSliderPhase.COMMIT
                && response.phase() != ControlRodSliderPhase.CANCEL) {
            return;
        }
        clientColumnX = response.columnX();
        clientColumnZ = response.columnZ();
        if (response.accepted() || response.hasAuthoritativeDepth()) {
            slider.setClientDisplayedValue(response.depthPercent());
        }
    }

    /** 读取仪表端口唯一权威快照，不创建驱动器本地副本。 */
    public ReactorSnapshot readAuthoritativeSnapshot(ReactorInstrumentPortBlockEntity owner) {
        if (owner == null) {
            throw new IllegalArgumentException("reactor instrument port is required");
        }
        return owner.snapshot();
    }

    /** 只有已经同步列坐标的控制棒驱动器才显示对应控制棒列完整度。 */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (clientColumnX < 0 || clientColumnX > 2 || clientColumnZ < 0 || clientColumnZ > 2) {
            return false;
        }
        tooltip.add(Component.translatable(
                "goggle.create_nuclear_industry.reactor.control_rod_summary"));
        if (clientControlRodIntegrity == null) {
            tooltip.add(Component.translatable(
                    "goggle.create_nuclear_industry.reactor.runtime_data_waiting"));
            return true;
        }
        ReactorInstrumentGoggleDisplay.appendControlRodColumnTooltip(
                tooltip,
                new ReactorInstrumentTelemetry.ControlRodColumnTelemetry(
                        new CoreColumnPosition(clientColumnX, clientColumnZ),
                        clientControlRodIntegrity));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putInt("ControlRodColumnX", clientColumnX);
            tag.putInt("ControlRodColumnZ", clientColumnZ);
            tag.putBoolean(CONTROL_ROD_INTEGRITY_AVAILABLE_KEY,
                    serverControlRodIntegrity != null);
            if (serverControlRodIntegrity != null) {
                tag.putDouble(CONTROL_ROD_INTEGRITY_KEY, serverControlRodIntegrity);
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            clientColumnX = tag.contains("ControlRodColumnX") ? tag.getInt("ControlRodColumnX") : -1;
            clientColumnZ = tag.contains("ControlRodColumnZ") ? tag.getInt("ControlRodColumnZ") : -1;
            clientControlRodIntegrity = null;
            if (tag.getBoolean(CONTROL_ROD_INTEGRITY_AVAILABLE_KEY)
                    && tag.contains(CONTROL_ROD_INTEGRITY_KEY)) {
                double integrity = tag.getDouble(CONTROL_ROD_INTEGRITY_KEY);
                if (Double.isFinite(integrity) && integrity >= 0.0D && integrity <= 1.0D) {
                    clientControlRodIntegrity = integrity;
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ReactorStructureLifecycle.rescanAroundNow(level, worldPosition);
        }
    }
}
