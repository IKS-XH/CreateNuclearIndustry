package com.iksxh.create_nuclear_industry.automation.gui;

import com.iksxh.create_nuclear_industry.automation.*;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ReactorAutomationContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

/** 无库存槽菜单只持玩家会话和显示投影；仪表/接口仍唯一拥有核状态和设置。 */
public final class ReactorAdministratorMenu extends AbstractContainerMenu {
    private final ReactorInstrumentPortBlockEntity serverOwner;
    private final ReactorAutomationBinding openedBinding;
    private final UUID viewer;
    private final UUID sessionId;
    private final BlockPos burnerPos;
    private final Object burnerInstance,portInstance;
    private ReactorAdministratorProjection projection;
    private boolean closed;
    private long lastSequence, lastDisplayTime=-10, lastEpoch=-1;
    private String lastPhase="";

    public ReactorAdministratorMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer) {
        super(ReactorAutomationContent.ADMINISTRATOR_MENU.get(),id);
        serverOwner=null; openedBinding=null; viewer=inventory.player.getUUID(); burnerInstance=null; portInstance=null;
        burnerPos=buffer.readBlockPos();sessionId=buffer.readUUID(); projection=new ReactorAdministratorProjection(buffer.readNbt());
    }
    private ReactorAdministratorMenu(int id,Inventory inventory,ReactorInstrumentPortBlockEntity owner,ReactorAutomationBinding binding,UUID sessionId) {
        super(ReactorAutomationContent.ADMINISTRATOR_MENU.get(),id);
        serverOwner=owner; openedBinding=binding; viewer=inventory.player.getUUID(); burnerPos=binding.burnerPos();this.sessionId=sessionId;
        burnerInstance=owner.automationBurner(); portInstance=owner.automationPort(); projection=ReactorAdministratorProjection.from(owner,binding);
    }
    /** 原空手入口打开会话，打开/关闭不启动或停堆，未加载成员不强载。 */
    public static boolean open(ServerPlayer player,BlockPos burnerPos) {
        var binding=ReactorAutomationBlazeBridge.findBinding(player.level(),burnerPos).orElse(null);
        var owner=ReactorAutomationBlazeBridge.owner(player.level(),binding);
        if(player.isSpectator() || !player.canInteractWithBlock(burnerPos,0) || owner==null || !ReactorAutomationService.hasCurrentInstances(owner,binding)) return false;
        var projection=ReactorAdministratorProjection.from(owner,binding);
        var sessionId=UUID.randomUUID();
        player.openMenu(new SimpleMenuProvider((id,inventory,p)->new ReactorAdministratorMenu(id,inventory,owner,binding,sessionId),
                Component.translatable("automation.create_nuclear_industry.title")),buffer->{buffer.writeBlockPos(burnerPos);buffer.writeUUID(sessionId);buffer.writeNbt(projection.data());});
        return true;
    }
    public ReactorAdministratorProjection projection() { return projection; }
    public void receive(ReactorAdministratorProjection next) { projection=next; }
    public BlockPos burnerPos() { return burnerPos; }
    public UUID sessionId() { return sessionId; }
    public ReactorAutomationBinding openedBinding() { return openedBinding; }
    public ReactorInstrumentPortBlockEntity serverOwner() { return serverOwner; }
    public boolean takeSequence(long sequence) {
        if(sequence<=lastSequence) return false;
        lastSequence=sequence; return true;
    }
    @Override public boolean stillValid(Player player) {
        if(serverOwner==null) return !closed;
        if(closed || player.isSpectator() || !viewer.equals(player.getUUID()) || !player.canInteractWithBlock(burnerPos,0)
                || player.level()!=serverOwner.getLevel() || serverOwner.isRemoved() || !serverOwner.structureValid()
                || serverOwner.getLevel().getBlockEntity(serverOwner.getBlockPos())!=serverOwner
                || !serverOwner.automationGeneration().equals(openedBinding.ownerGeneration())) return false;
        var current=ReactorAutomationService.candidate(serverOwner);
        return current!=null && current.portPos().equals(openedBinding.portPos()) && current.burnerPos().equals(burnerPos)
                && ReactorAutomationService.hasCurrentInstances(serverOwner,current)
                && serverOwner.automationBurner()==burnerInstance && serverOwner.automationPort()==portInstance;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
    @Override public void removed(Player player) { closed=true; super.removed(player); }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if(serverOwner==null) return;
        var player=serverOwner.getLevel().getServer().getPlayerList().getPlayer(viewer);
        if(player==null || player.containerMenu!=this) return;
        if(!stillValid(player)) { player.closeContainer(); return; }
        long time=player.level().getGameTime(); var state=serverOwner.automationState();
        if(time-lastDisplayTime<10 && lastPhase.equals(state.phase().name()) && lastEpoch==state.authorityEpoch()) return;
        lastDisplayTime=time; lastPhase=state.phase().name(); lastEpoch=state.authorityEpoch();
        var next=ReactorAdministratorProjection.from(serverOwner,serverOwner.automationBinding());
        if(!next.data().equals(projection.data())) {
            projection=next;
            PacketDistributor.sendToPlayer(player,new ReactorAutomationNetwork.Reply(containerId,sessionId,0,true,"update",next.data()));
        }
    }
}
