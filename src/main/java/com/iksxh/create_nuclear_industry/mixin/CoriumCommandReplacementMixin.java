package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.accident.MoltenCoriumEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.*;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 仅原版setblock/fill最终BlockInput.place且有权限时允许有意管理清除；作用域finally撤销。 */
@Mixin({SetBlockCommand.class,FillCommand.class})
public abstract class CoriumCommandReplacementMixin {
    @WrapOperation(method={"setBlock","fillBlocks"},at=@At(value="INVOKE",target="Lnet/minecraft/commands/arguments/blocks/BlockInput;place(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)Z"))
    private static boolean corium$command(BlockInput input,ServerLevel level,BlockPos pos,int flags,Operation<Boolean> original,@Local(argsOnly=true) CommandSourceStack source){
        return MoltenCoriumEffects.command(level,pos,source.hasPermission(2),()->original.call(input,level,pos,flags));
    }
}
