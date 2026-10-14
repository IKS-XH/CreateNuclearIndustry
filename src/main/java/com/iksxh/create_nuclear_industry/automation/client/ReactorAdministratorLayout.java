package com.iksxh.create_nuclear_industry.automation.client;

/** 客户端和定向检查共用的GUI单位几何，不依赖Minecraft客户端类。 */
public final class ReactorAdministratorLayout {
    public static final int WIDTH=312, HEIGHT=224;
    public record Rect(int x,int y,int width,int height) {
        public boolean intersects(Rect other) { return x<other.x+other.width && other.x<x+width && y<other.y+other.height && other.y<y+height; }
        public boolean contains(double px,double py) { return px>=x && py>=y && px<x+width && py<y+height; }
    }
    private ReactorAdministratorLayout() { }
    public static Rect stop() { return new Rect(226,198,74,20); }
    public static Rect cell(int columns,int rows,int x,int z) {
        if(columns<1 || columns>14 || rows<1 || rows>14 || x<0 || x>=columns || z<0 || z>=rows) throw new IllegalArgumentException("堆芯网格坐标非法");
        int pitch=Math.min(10,140/Math.max(columns,rows));
        return new Rect(16+x*pitch,46+z*pitch,pitch-1,pitch-1);
    }
}
