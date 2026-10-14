package com.iksxh.create_nuclear_industry.mixin;
import com.iksxh.create_nuclear_industry.automation.ReactorAutomationBlazeBridge;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
/** 目标类未声明信号方法，增加继承覆盖而非注入不存在方法；派生标记只控制原生信号源缓存。 */
@Mixin(BlazeBurnerBlock.class)
public abstract class BlazeBurnerAutomationBlockMixin extends HorizontalDirectionalBlock {
    protected BlazeBurnerAutomationBlockMixin(Properties p) {super(p);}
    /** 原类只声明useItemOn；增加继承空手入口，燃料/工具仍走原生路径。 */
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player.getMainHandItem().isEmpty() && player instanceof ServerPlayer server && ReactorAdministratorMenu.open(server,pos)) return InteractionResult.CONSUME;
        return super.useWithoutItem(state,level,pos,player,hit);
    }
    @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true)
    private void cniOpenMenu(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<ItemInteractionResult> ci) {
        if(held.isEmpty() && player instanceof ServerPlayer server && ReactorAdministratorMenu.open(server,pos)) ci.setReturnValue(ItemInteractionResult.CONSUME);
    }
    /** BooleanProperty枚举首值不能充当授权；原默认状态显式保持无扩展信号源。 */
    @Inject(method="<init>",at=@At("RETURN"))
    private void cniDefaultMarker(Properties properties,CallbackInfo ci) {registerDefaultState(defaultBlockState().setValue(ReactorAutomationBlazeBridge.SIGNAL_SOURCE,false));}
    @Inject(method="createBlockStateDefinition",at=@At("TAIL"))
    private void cniAddSignalMarker(StateDefinition.Builder<Block,BlockState> builder,CallbackInfo ci) {builder.add(ReactorAutomationBlazeBridge.SIGNAL_SOURCE);}
    @Override protected boolean isSignalSource(BlockState state) { return state.hasProperty(ReactorAutomationBlazeBridge.SIGNAL_SOURCE)&&state.getValue(ReactorAutomationBlazeBridge.SIGNAL_SOURCE); }
    /** 原版反相火把读取实心支撑格的强供电；强弱信号都必须来自当前权威许可。 */
    @Override protected int getDirectSignal(BlockState state,BlockGetter getter,BlockPos pos,Direction side) {
        return getter instanceof Level level?ReactorAutomationBlazeBridge.runPermission(level,pos):0;
    }
    @Override protected int getSignal(BlockState state,BlockGetter getter,BlockPos pos,Direction side) {
        return getter instanceof Level level?ReactorAutomationBlazeBridge.runPermission(level,pos):0;
    }
}
