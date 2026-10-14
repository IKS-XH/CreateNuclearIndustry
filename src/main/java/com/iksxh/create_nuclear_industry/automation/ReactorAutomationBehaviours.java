package com.iksxh.create_nuclear_industry.automation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.*;
/** 旧开发夹具几何仍可读取，正式燃烧室不注册世界四控件；核控制只经当前GUI会话。 */
public final class ReactorAutomationBehaviours {
    public static final List<BehaviourType<Control>> TYPES=List.of(new BehaviourType<>("cni_mode"),new BehaviourType<>("cni_depth"),new BehaviourType<>("cni_heat"),new BehaviourType<>("cni_reset"));
    private ReactorAutomationBehaviours() {}
    /** 本地HU刻度转绝对目标，范围仍由当前SERVER配置决定，单位HU/t。 */
    public static int boundedHeat(int base,int localValue,int row,int maxHeat) {
        int step=row==0?1:row==1?10:100;
        long absolute=(long)base+(localValue-50L)*step;
        return (int)Math.max(0,Math.min(maxHeat,absolute));
    }
    public static void add(BlazeBurnerBlockEntity burner,List<BlockEntityBehaviour> behaviours) {
        // 用户改用专用GUI后不再向Create行为表加入旧写入口。
    }
    public static void refresh(BlazeBurnerBlockEntity burner,CompoundTag view) {
        for(int i=0;i<4;i++) {
            var c=burner.getBehaviour(TYPES.get(i)); if(c==null) continue;
            c.value=i==0?view.getInt("Mode"):i==1?view.getInt("Depth"):i==2?view.getInt("Heat"):0;
        }
    }
    public static final class Control extends ScrollValueBehaviour {
        public final BlazeBurnerBlockEntity burner; public final ReactorAutomationService.Command command;
        private int heatBoardBase;
        public Control(BlazeBurnerBlockEntity burner,ReactorAutomationService.Command command) {
            super(Component.translatable("automation.create_nuclear_industry.control."+command.name().toLowerCase(Locale.ROOT)),burner,new Transform(burner,command.ordinal()));
            this.burner=burner; this.command=command;
            between(0,command==ReactorAutomationService.Command.MODE?2:command==ReactorAutomationService.Command.DEPTH?100:command==ReactorAutomationService.Command.HEAT?1000000:1);
            withFormatter(v->command==ReactorAutomationService.Command.DEPTH?v+"%":command==ReactorAutomationService.Command.HEAT?v+" HU/t":
                    Component.translatable("automation.create_nuclear_industry."+(command==ReactorAutomationService.Command.MODE?"mode."+v:"reset."+v)).getString());
        }
        @Override public BehaviourType<?> getType() { return TYPES.get(command.ordinal()); }
        @Override public boolean isActive() { return false; }
        /** 只空手操作，任何手持燃料/工具继续进入Create原交互。 */
        @Override public boolean bypassesInput(ItemStack held) { return !held.isEmpty(); }
        @Override public boolean mayInteract(Player player) { return player.getMainHandItem().isEmpty() && !player.isSpectator(); }
        @Override public void setValueSettings(Player player,ValueSettings setting,boolean ctrl) { }
        @Override public void setValue(int value) { }
        @Override public boolean readFromClipboard(HolderLookup.Provider p,CompoundTag tag,Player player,Direction side,boolean simulate) { return false; }
        @Override public void write(CompoundTag tag,HolderLookup.Provider p,boolean packet) { }
        @Override public void read(CompoundTag tag,HolderLookup.Provider p,boolean packet) { }
        @Override public ValueSettingsBoard createBoard(Player player,BlockHitResult hit) {
            heatBoardBase=getValue();
            var board=command==ReactorAutomationService.Command.HEAT
                    ?new ValueSettingsBoard(label,100,10,List.of(Component.literal("1 HU"),Component.literal("10 HU"),Component.literal("100 HU")),
                        new ValueSettingsFormatter(v->Component.literal(absoluteValue(v)+" HU/t")))
                    :super.createBoard(player,hit);
            if(player.level().isClientSide) com.iksxh.create_nuclear_industry.automation.client.ReactorAutomationClientEvents.prepare(this,board);
            return board;
        }
        /** HU数值板为当前目标两侧局部刻度；三行倍率1/10/100，服务端仍验证绝对上限。 */
        public int absoluteValue(ValueSettings setting) {
            if(command!=ReactorAutomationService.Command.HEAT) return setting.value();
            var view=((ReactorAutomationBlazeBridge.Access)burner).cniAutomationView();
            return boundedHeat(heatBoardBase,setting.value(),setting.row(),view.getInt("MaxHeat"));
        }
        @Override public ValueSettings getValueSettings() { return command==ReactorAutomationService.Command.HEAT?new ValueSettings(0,50):super.getValueSettings(); }
        @Override public void onShortInteract(Player player,InteractionHand hand,Direction side,BlockHitResult hit) {
            if(player.level().isClientSide && command==ReactorAutomationService.Command.MODE)
                com.iksxh.create_nuclear_industry.automation.client.ReactorAutomationClientEvents.send(this,(getValue()+1)%3);
        }
    }
    /** 四象限同源显示/命中。只启用背对接口的外侧面，隐藏接口面和底面不命中。 */
    public static final class Transform extends CenteredSideValueBoxTransform {
        private final BlazeBurnerBlockEntity burner; private final int index;
        public Transform(BlazeBurnerBlockEntity burner,int index) { this.burner=burner; this.index=index; scale=.32f; }
        @Override protected boolean isSideActive(BlockState state,Direction side) {
            if(side.getAxis()==Direction.Axis.Y || burner==null || !(burner instanceof ReactorAutomationBlazeBridge.Access a)) return false;
            var view=a.cniAutomationView(); if(!view.hasUUID("Generation")) return false;
            BlockPos port=BlockPos.of(view.getLong("Port"));
            return burner.getBlockPos().relative(side.getOpposite()).equals(port);
        }
        @Override public Vec3 getLocalOffset(LevelAccessor level,BlockPos pos,BlockState state) {
            double u=index%2==0?.25:.75, y=index<2?.75:.25;
            return switch(direction) { case NORTH->new Vec3(1-u,y,0);case SOUTH->new Vec3(u,y,1);case WEST->new Vec3(0,y,u);case EAST->new Vec3(1,y,1-u);default->new Vec3(.5,.5,.5); };
        }
        @Override public float getScale() { return .32f; }
    }
}
