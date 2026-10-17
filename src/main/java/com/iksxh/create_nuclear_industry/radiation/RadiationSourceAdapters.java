package com.iksxh.create_nuclear_industry.radiation;

import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.mixin.*;
import com.iksxh.create_nuclear_industry.storage.*;
import com.iksxh.create_nuclear_industry.production.*;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** 服务端唯一所有者库存的只读适配；一个方块不枚举六面后相加，带分段能力完全跳过。 */
public final class RadiationSourceAdapters {
    public record LocatedStack(Vec3 position, ItemStack stack, boolean rackShielded,Object transportIdentity) {
        public LocatedStack(Vec3 position,ItemStack stack,boolean rackShielded){this(position,stack,rackShielded,null);}
    }
    private RadiationSourceAdapters() {}
    public static void read(BlockEntity be, Consumer<LocatedStack> out) {
        Vec3 position=Vec3.atCenterOf(be.getBlockPos());
        Consumer<ItemStack> item=s -> out.accept(new LocatedStack(position,s,false));
        if(be instanceof com.iksxh.create_nuclear_industry.accident.ReactorCoriumBlockEntity corium) {
            item.accept(corium.payload());
        } else if(be instanceof ReactorPortBlockEntity port) {
            item = s -> out.accept(new LocatedStack(RadiationContainment.sourcePosition((ServerLevel)be.getLevel(),be.getBlockPos()),s,false));
            item.accept(port.fuelAssembly());
        } else if(be instanceof DryStorageBlockEntity rack) {
            for(int slot=0;slot<DryStorageState.MAX_SLOTS;slot++) {
                ItemStack stack=rack.state().item(slot);
                out.accept(new LocatedStack(position,stack,SpentFuelPayload.isValid(stack)));
            }
        } else if(be instanceof BeltBlockEntity belt) {
            // 每段只枚举其控制端，调用方按控制端身份去重；位置仍取真实沿带偏移。
            BeltBlockEntity controller=belt.getControllerBE();
            if(controller==null) return;
            var inventory=controller.getInventory();
            var queues=(RadiationBeltAccessor)inventory;
            var unique=Collections.newSetFromMap(new IdentityHashMap<com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack,Boolean>());
            unique.addAll(inventory.getTransportedItems()); unique.removeAll(queues.radiation$getToRemove());
            unique.addAll(queues.radiation$getToInsert());
            for(var transported:unique) out.accept(new LocatedStack(BeltHelper.getVectorForOffset(controller,transported.beltPosition),transported.stack,false,transported));
        } else if(be instanceof ArmBlockEntity arm) item.accept(((RadiationArmAccessor)arm).radiation$getHeldItem());
        else if(be instanceof DepotBlockEntity depot) item.accept(depot.getHeldItem());
        else if(be instanceof ItemVaultBlockEntity vault) handler(vault.getInventoryOfBlock(),item);
        else if(be instanceof BasinBlockEntity basin) { handler(basin.getInputInventory(),item); handler(basin.getOutputInventory(),item); }
        else if(be instanceof ShieldedAssemblyBlockEntity assembly) {
            for(int slot=0;slot<4;slot++) item.accept(assembly.state().input(slot)); item.accept(assembly.state().output());
        } else if(be instanceof CentrifugeBlockEntity centrifuge) {
            handler(centrifuge.itemPort(Direction.NORTH,false),item);
        } else if(be instanceof FuelSinteringBlockEntity furnace) {
            handler(furnace.itemPort(Direction.UP),item); handler(furnace.itemPort(Direction.NORTH),item);
        } else if(be instanceof Container container) {
            for(int slot=0;slot<container.getContainerSize();slot++) item.accept(container.getItem(slot));
        } else if(be.getLevel() instanceof ServerLevel level) {
            // 公开未知设备只读一个无面能力；不声称覆盖私有库存或跨块远程仓库。
            handler(level.getCapability(Capabilities.ItemHandler.BLOCK,be.getBlockPos(),null),item);
        }
    }
    private static void handler(IItemHandler handler,Consumer<ItemStack> out) {
        if(handler!=null) for(int slot=0;slot<handler.getSlots();slot++) out.accept(handler.getStackInSlot(slot));
    }
}
