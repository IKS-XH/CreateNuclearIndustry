package com.iksxh.create_nuclear_industry.accident;

import java.util.UUID;
import com.iksxh.create_nuclear_industry.content.AccidentContent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 服务端残骸唯一单件废料库存；事故/列身份用于动作重入，空残骸无源且不能造料。 */
public final class ReactorCoriumBlockEntity extends BlockEntity {
    private UUID accident;
    private BlockPos column;
    private ItemStack payload=ItemStack.EMPTY;
    public ReactorCoriumBlockEntity(BlockPos pos, BlockState state) { super(AccidentContent.CORIUM_BE.get(),pos,state); }
    public ItemStack payload() { return payload.copy(); }
    public boolean matches(UUID id,BlockPos bottom) { return id.equals(accident)&&bottom.equals(column); }
    /** 只有空库存和空收据可接收；即使玩家已回收，同一动作重放也不能重新灌装。 */
    public boolean accept(UUID id,BlockPos bottom,ItemStack stack) {
        if(level==null||level.isClientSide||accident!=null||!stack.is(AccidentContent.ACCIDENT_FUEL_WASTE.get())||stack.getCount()!=1) return false;
        accident=id;column=bottom.immutable();payload=stack.copy();setChanged();return true;
    }
    /** 先移走唯一库存再产生掉落；所有原生移除路径共用，不经loot表另造一份。 */
    public ItemStack take() { var result=payload; payload=ItemStack.EMPTY;setChanged();return result; }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if(accident!=null){tag.putUUID("Accident",accident);tag.putLong("Column",column.asLong());}
        if(!payload.isEmpty())tag.put("Payload",payload.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);accident=tag.hasUUID("Accident")?tag.getUUID("Accident"):null;
        column=BlockPos.of(tag.getLong("Column"));var parsed=ItemStack.parseOptional(registries,tag.getCompound("Payload"));
        payload=parsed.is(AccidentContent.ACCIDENT_FUEL_WASTE.get())&&parsed.getCount()==1?parsed:ItemStack.EMPTY;
    }
}
