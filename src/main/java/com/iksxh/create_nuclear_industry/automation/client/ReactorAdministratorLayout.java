package com.iksxh.create_nuclear_industry.automation.client;

/** 客户端和定向检查共用的GUI单位几何，不依赖Minecraft客户端类。 */
public final class ReactorAdministratorLayout {
    public static final int WIDTH=312, HEIGHT=224;
    public record Rect(int x,int y,int width,int height) {
        public boolean intersects(Rect other) { return x<other.x+other.width && other.x<x+width && y<other.y+other.height && other.y<y+height; }
        public boolean contains(double px,double py) { return px>=x && py>=y && px<x+width && py<y+height; }
    }
    /** 四份文本草稿的同一生产模型；确认刷新只对未编辑草稿生效，不持有菜单版本或客户端授权。 */
    public record DraftFields(String depth,String heat,String reserve,String minimum) {
        public static DraftFields confirmed(net.minecraft.nbt.CompoundTag view) {
            return new DraftFields(Integer.toString(view.getInt("Depth")),Integer.toString(view.getInt("Heat")),Integer.toString(view.getInt("ReserveTarget")),Integer.toString(view.getInt("MinimumHeat")));
        }
        /** 与Screen完整APPLY共用的五字段转换；不替代服务器范围/身份校验。 */
        public com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands.Draft toDraft(com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Mode mode) {
            return new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands.Draft(mode,Integer.parseInt(depth),Integer.parseInt(heat),Integer.parseInt(reserve),Integer.parseInt(minimum));
        }
        public DraftFields synchronize(DraftFields confirmed,com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Mode mode,boolean dirty) {
            if(dirty)return this;
            // 完整草稿按确认版本同步四份偏好，不能以仅刷新可见框的旧缓存覆盖另一玩家隐藏字段。
            return confirmed;
        }
    }
    private ReactorAdministratorLayout() { }
    public static Rect stop() { return new Rect(226,198,74,20); }
    public static Rect cell(int columns,int rows,int x,int z) {
        if(columns<1 || columns>14 || rows<1 || rows>14 || x<0 || x>=columns || z<0 || z>=rows) throw new IllegalArgumentException("堆芯网格坐标非法");
        int pitch=Math.min(10,140/Math.max(columns,rows));
        return new Rect(16+x*pitch,46+z*pitch,pitch-1,pitch-1);
    }
}
