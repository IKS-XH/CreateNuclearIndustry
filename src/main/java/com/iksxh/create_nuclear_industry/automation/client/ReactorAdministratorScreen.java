package com.iksxh.create_nuclear_industry.automation.client;

import com.iksxh.create_nuclear_industry.automation.ReactorAutomationNetwork;
import com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Mode;
import com.iksxh.create_nuclear_industry.automation.gui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;

/** 三页只读堆芯与数字草稿；开始编辑即冻结版本，服务器新投影不能偷换旧草稿版本。 */
public final class ReactorAdministratorScreen extends AbstractContainerScreen<ReactorAdministratorMenu> {
    private int page,selectedX=-1,selectedZ=-1;
    private Mode draftMode=Mode.MANUAL;
    private EditBox depth,heat;
    private Button modeButton;
    private boolean dirty,loading,resetConfirm,renderedConfigValid;
    private long draftRevision,draftEpoch,sequence,pendingSequence;
    private ReactorAdministratorCommands.Action pendingAction;
    private String status="";
    private String depthText="100",heatText="0";

    public ReactorAdministratorScreen(ReactorAdministratorMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title); imageWidth=ReactorAdministratorLayout.WIDTH; imageHeight=ReactorAdministratorLayout.HEIGHT;
        inventoryLabelY=1000;
    }
    private static Component text(String key,Object... args) { return Component.translatable("automation.create_nuclear_industry.gui."+key,args); }
    private CompoundTag view() { return menu.projection().data(); }
    @Override protected void init() {
        // clearWidgets不会清父容器焦点；页面/配置/尺寸重建先撤销旧输入框，避免不可见旧对象继续接键。
        if(depth!=null)depth.setFocused(false);if(heat!=null)heat.setFocused(false);setFocused(null);
        super.init(); clearWidgets();
        renderedConfigValid=view().getBoolean("ConfigurationValid");
        for(int i=0;i<3;i++) { final int tab=i; addRenderableWidget(Button.builder(text("page"+i),b->{saveText();page=tab;resetConfirm=false;init();})
                .bounds(leftPos+8+i*98,topPos+22,94,18).build()); }
        var stop=ReactorAdministratorLayout.stop();
        addRenderableWidget(Button.builder(text("stop"),b->send(ReactorAdministratorCommands.Action.STOP))
                .bounds(leftPos+stop.x(),topPos+stop.y(),stop.width(),stop.height()).build());
        if(page==0 && view().getBoolean("ConfigurationValid")) {
            if(!dirty) loadConfirmed();
            depth=new EditBox(font,leftPos+80,topPos+143,46,18,text("depth")); depth.setMaxLength(3);
            heat=new EditBox(font,leftPos+178,topPos+143,76,18,text("heat")); heat.setMaxLength(10);
            loading=true; depth.setValue(depthText);heat.setValue(heatText);loading=false;
            depth.setFilter(s->s.matches("[0-9]*"));heat.setFilter(s->s.matches("[0-9]*"));
            heat.setTooltip(Tooltip.create(text("heat")));
            depth.setResponder(s->{if(!loading)beginDraft();depthText=s;});heat.setResponder(s->{if(!loading)beginDraft();heatText=s;});
            addRenderableWidget(depth);addRenderableWidget(heat);
            modeButton=addRenderableWidget(Button.builder(text(draftMode==Mode.MANUAL?"manual":"constant"),b->{beginDraft();draftMode=draftMode==Mode.MANUAL?Mode.CONSTANT:Mode.MANUAL;b.setMessage(text(draftMode==Mode.MANUAL?"manual":"constant"));})
                    .bounds(leftPos+8,topPos+164,112,18).build());
            addRenderableWidget(Button.builder(Component.literal("−"),b->step(depth,-1,100)).bounds(leftPos+60,topPos+143,18,18).build());
            addRenderableWidget(Button.builder(Component.literal("+"),b->step(depth,1,100)).bounds(leftPos+128,topPos+143,18,18).build());
            addRenderableWidget(Button.builder(Component.literal("−"),b->step(heat,-1,view().getInt("MaxHeat"))).bounds(leftPos+158,topPos+143,18,18).build());
            addRenderableWidget(Button.builder(Component.literal("+"),b->step(heat,1,view().getInt("MaxHeat"))).bounds(leftPos+256,topPos+143,18,18).build());
            addRenderableWidget(Button.builder(text("apply"),b->send(ReactorAdministratorCommands.Action.APPLY)).bounds(leftPos+124,topPos+164,80,18).build());
            addRenderableWidget(Button.builder(text("start"),b->send(ReactorAdministratorCommands.Action.START)).bounds(leftPos+208,topPos+164,92,18).build());
        } else { depth=null;heat=null;modeButton=null; }
        if(page==2 && view().getBoolean("ConfigurationValid")) addRenderableWidget(Button.builder(text(resetConfirm?"confirm_reset":"reset"),b->{
            if(resetConfirm) {resetConfirm=false;send(ReactorAdministratorCommands.Action.RESET);} else {resetConfirm=true;init();}
        }).bounds(leftPos+8,topPos+166,150,18).build());
    }
    private void loadConfirmed() {
        var v=view();draftMode=v.getInt("Mode")==Mode.CONSTANT.ordinal()?Mode.CONSTANT:Mode.MANUAL;
        depthText=Integer.toString(v.getInt("Depth"));heatText=Integer.toString(v.getInt("Heat"));
    }
    private void beginDraft() {
        if(!dirty) {var v=view();draftRevision=v.getLong("SettingsRevision");draftEpoch=v.getLong("Epoch");dirty=true;}
    }
    /** 获得输入焦点即固定草稿版本；未键入前的选区/光标准备也不允许被他人新投影替换。 */
    @Override public void setFocused(GuiEventListener next) {
        super.setFocused(next);
        if(next!=null&&(next==depth||next==heat))beginDraft();
    }
    /** 普通seq0只原位刷新未编辑字段；同一EditBox的焦点、光标和选区保持，不触发重建。 */
    private void refreshConfirmedInputs() {
        if(dirty||page!=0||depth==null||heat==null)return;
        var v=view();draftMode=v.getInt("Mode")==Mode.CONSTANT.ordinal()?Mode.CONSTANT:Mode.MANUAL;
        loading=true;
        try {
            if(!depth.isFocused()) {depthText=Integer.toString(v.getInt("Depth"));if(!depth.getValue().equals(depthText))depth.setValue(depthText);}
            if(!heat.isFocused()) {heatText=Integer.toString(v.getInt("Heat"));if(!heat.getValue().equals(heatText))heat.setValue(heatText);}
            if(modeButton!=null)modeButton.setMessage(text(draftMode==Mode.MANUAL?"manual":"constant"));
        } finally {loading=false;}
    }
    private void saveText() { if(depth!=null)depthText=depth.getValue();if(heat!=null)heatText=heat.getValue(); }
    private void step(EditBox box,int delta,int max) {
        int value;try {value=Integer.parseInt(box.getValue());}catch(NumberFormatException invalid){value=0;}
        box.setValue(Integer.toString(Math.max(0,Math.min(max,value+delta*(hasShiftDown()?100:hasControlDown()?10:1)))));
    }
    private void send(ReactorAdministratorCommands.Action action) {
        var v=view();if(!v.hasUUID("Generation")) {status="menu_invalid";return;}
        if(action!=ReactorAdministratorCommands.Action.STOP&&!v.getBoolean("ConfigurationValid")) {status="invalid_config";return;}
        saveText();ReactorAdministratorCommands.Draft draft=null;
        if(action==ReactorAdministratorCommands.Action.APPLY) {
            beginDraft();try {draft=new ReactorAdministratorCommands.Draft(draftMode,Integer.parseInt(depthText),Integer.parseInt(heatText));}
            catch(NumberFormatException invalid) {status="invalid_value";return;}
        }
        pendingAction=action;pendingSequence=++sequence;
        long revision=action==ReactorAdministratorCommands.Action.APPLY?draftRevision:v.getLong("SettingsRevision");
        long epoch=action==ReactorAdministratorCommands.Action.APPLY?draftEpoch:v.getLong("Epoch");
        PacketDistributor.sendToServer(new ReactorAutomationNetwork.Input(new ReactorAdministratorCommands.Request(menu.containerId,menu.sessionId(),
                menu.burnerPos(),BlockPos.of(v.getLong("Port")),v.getUUID("Generation"),epoch,revision,sequence,action,draft)));
        status="pending";
    }
    /** 自动显示更新只改投影；只有对应APPLY成功回包才清理草稿。失败保留原字段和版本。 */
    public void receive(ReactorAutomationNetwork.Reply reply) {
        if(renderedConfigValid!=view().getBoolean("ConfigurationValid")) {saveText();init();}
        if(reply.sequence()!=0 && reply.sequence()==pendingSequence) {
            status=reply.reason();
            if(reply.accepted() && pendingAction==ReactorAdministratorCommands.Action.APPLY) {dirty=false;loadConfirmed();init();}
        } else if(reply.sequence()==0) refreshConfirmedInputs();
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos-1,topPos-1,leftPos+imageWidth+1,topPos+imageHeight+1,0xff8d7460);
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff28343a);
        g.fill(leftPos+5,topPos+43,leftPos+imageWidth-5,topPos+194,0xff172126);
    }
    private void line(GuiGraphics g,int y,Component label) { g.drawString(font,font.plainSubstrByWidth(label.getString(),296),8,y,0xffe0e7e6,false); }
    private static String number(double value) {return String.format(Locale.ROOT,"%.1f",value);}
    private Component yes(boolean value) {return text(value?"yes":"no");}
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,8,7,0xffeec18b,false);var v=view();
        if(!v.getBoolean("Valid")) {line(g,50,text("unavailable"));return;}
        if(!v.getBoolean("ConfigurationValid")) {
            line(g,50,text("result_invalid_config"));
            line(g,65,Component.translatable("automation.create_nuclear_industry.phase."+v.getString("Phase").toLowerCase(Locale.ROOT)));
            return;
        }
        if(page==0) {
            line(g,46,text("size",v.getInt("Width"),v.getInt("Height"),v.getInt("SizeZ"),number(v.getDouble("MaxReachable"))));
            line(g,57,Component.translatable("automation.create_nuclear_industry.phase."+v.getString("Phase").toLowerCase(Locale.ROOT)));
            line(g,68,text("power",number(v.getDouble("ActualHeat")),number(v.getDouble("NetHeat")),number(v.getDouble("AverageHeat"))));
            line(g,79,text("cached",number(v.getDouble("CacheHeat")),number(v.getDouble("CacheExpense")),number(v.getDouble("Maintenance"))));
            line(g,90,text("inventory",v.getLong("ColdMb"),v.getLong("HotMb"),v.getLong("CapacityMb")));
            long capacity=v.getLong("CapacityMb");int cold=capacity<=0?0:(int)Math.min(294,294.0*v.getLong("ColdMb")/capacity);
            int hot=capacity<=0?0:(int)Math.min(294-cold,294.0*v.getLong("HotMb")/capacity);
            g.fill(8,100,302,104,0xff4e565c);g.fill(8,100,8+cold,104,0xff6594b7);g.fill(8+cold,100,8+cold+hot,104,0xffc57c4c);
            line(g,106,text("fuel",v.getInt("Usable"),v.getInt("Spent"),v.getInt("EmptyFuel"),number(v.getDouble("DepthAverage"))));
            line(g,117,text("supply",text("supply_"+v.getString("Supply")),v.getInt("ExternalTicks")));
            line(g,128,text("confirmed",text(v.getInt("Mode")==Mode.CONSTANT.ordinal()?"constant":"manual"),v.getInt("Depth"),v.getInt("Heat")));
            g.drawString(font,text("depth"),8,147,0xffc1cece,false);
            if(v.getInt("Mode")==Mode.CONSTANT.ordinal()&&v.getInt("Heat")<=v.getDouble("MaintenanceCost"))line(g,184,text("net_warning"));
        } else if(page==1) drawCore(g,v);
        else {
            line(g,47,text("reason",Component.translatable("automation.create_nuclear_industry.reason."+v.getString("Reason"))));
            line(g,60,text("protection",yes(v.getBoolean("Duty")),yes(v.getBoolean("Covered")),yes(!v.getBoolean("Jammed"))));
            line(g,73,text("cooling",yes(v.getBoolean("Sample")),yes(v.getBoolean("NextCooling")),number(v.getDouble("Cooling"))));
            String expected=switch(v.getString("Phase")) {case "ARMING"->"signal_wait_low";case "MANUAL","CONSTANT"->"signal_run_low";case "FAULT"->"signal_fault_high";default->"signal_start_high";};
            line(g,86,text("signal",text(v.getBoolean("SignalHigh")?"signal_high":"signal_low"),text(expected)));
            line(g,99,text("startup",Math.round(v.getDouble("StartupCold")*100),Math.round(v.getDouble("StartupHot")*100)));
            line(g,112,text("warning",Math.round(v.getDouble("WarningCold")*100),Math.round(v.getDouble("WarningHot")*100),Math.round(v.getDouble("TripHot")*100)));
            line(g,125,text("maintenance",number(v.getDouble("MaintenanceCost"))));
            line(g,138,text("reset_age",v.getInt("HealthyTicks"),v.getInt("ResetTicks")));
            line(g,151,text("reset_help"));
        }
        if(!status.isEmpty()) g.drawString(font,font.plainSubstrByWidth(text("result_"+status).getString(),208),8,202,0xffeec18b,false);
        else if(dirty) g.drawString(font,text("draft"),8,202,0xffeec18b,false);
    }
    private void drawCore(GuiGraphics g,CompoundTag v) {
        int width=v.getInt("ColumnsX"),rows=v.getInt("ColumnsZ");if(width<1||width>14||rows<1||rows>14)return;
        CompoundTag chosen=null;
        for(var element:v.getList("Columns",Tag.TAG_COMPOUND)) {
            var c=(CompoundTag)element;int x=c.getInt("X"),z=c.getInt("Z");
            var r=ReactorAdministratorLayout.cell(width,rows,x,z);int color=0xff34444c;
            if(c.getString("Role").equals("fuel"))color=c.getString("Fuel").equals("usable")?0xffb49b50:c.getString("Fuel").equals("spent")?0xff735b52:0xff4a4540;
            if(c.getString("Role").equals("control_rod"))color=c.getBoolean("Jammed")?0xff9a4545:0xff507f92;
            boolean selected=x==selectedX&&z==selectedZ;
            if(selected) {g.fill(r.x()-1,r.y()-1,r.x()+r.width()+1,r.y()+r.height()+1,0xffffe4a0);chosen=c;}
            g.fill(r.x(),r.y(),r.x()+r.width(),r.y()+r.height(),color);
            String mark=c.getString("Role").equals("fuel")?"F":c.getString("Role").equals("control_rod")?(c.getBoolean("Jammed")?"!":"C"):"·";
            g.drawString(font,mark,r.x()+1,r.y(),0xffeeeeee,false);
        }
        int y=47;g.drawString(font,text("legend"),163,y,0xffeec18b,false);y+=14;
        g.drawString(font,Component.literal("X →   Z ↓ (N ↑)"),163,y,0xffd7e1e0,false);y+=14;
        if(chosen==null) {g.drawWordWrap(font,text("select"),163,y,139,0xffd7e1e0);return;}
        g.drawString(font,text("column",selectedX+1,selectedZ+1),163,y,0xffe0e7e6,false);y+=14;
        g.drawString(font,text("role_"+chosen.getString("Role")),163,y,0xffe0e7e6,false);y+=14;
        if(chosen.getString("Role").equals("fuel")) {
            g.drawString(font,text("fuel_"+chosen.getString("Fuel")),163,y,0xffe0e7e6,false);y+=14;
            g.drawString(font,text("remaining",Math.round(chosen.getDouble("Remaining")*100)),163,y,0xffe0e7e6,false);y+=14;
            g.drawString(font,text("covered",yes(chosen.getBoolean("Covered"))),163,y,0xffe0e7e6,false);y+=14;
        } else if(chosen.getString("Role").equals("control_rod")) {
            g.drawString(font,text("rod",number(chosen.getDouble("Actual")),number(chosen.getDouble("Target"))),163,y,0xffe0e7e6,false);y+=14;
        }
        if(!chosen.getString("Role").equals("empty")) {
            g.drawString(font,text("integrity",Math.round(chosen.getDouble("Integrity")*100)),163,y,0xffe0e7e6,false);y+=14;
            g.drawString(font,text("column_heat",number(chosen.getDouble("Heat"))),163,y,0xffe0e7e6,false);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(page==1&&button==0) {
            var v=view();int width=v.getInt("ColumnsX"),rows=v.getInt("ColumnsZ");
            if(width>=1&&width<=14&&rows>=1&&rows<=14) for(int cx=0;cx<width;cx++)for(int z=0;z<rows;z++)
                if(ReactorAdministratorLayout.cell(width,rows,cx,z).contains((int)x-leftPos,(int)y-topPos)) {selectedX=cx;selectedZ=z;return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
