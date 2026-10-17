package com.iksxh.create_nuclear_industry.mixin;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** 读取传送带尚未合并的变更队列，防止同tick入带漏源或已取出载荷双算。 */
@Mixin(BeltInventory.class)
public interface RadiationBeltAccessor {
    @Accessor("toInsert") List<TransportedItemStack> radiation$getToInsert();
    @Accessor("toRemove") List<TransportedItemStack> radiation$getToRemove();
}
