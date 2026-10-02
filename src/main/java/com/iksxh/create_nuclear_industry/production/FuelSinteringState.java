package com.iksxh.create_nuclear_industry.production;

import net.minecraft.nbt.CompoundTag;

/** 烧结炉唯一的数量与工时账本；数量单位为件，进度单位为有效服务端 tick。 */
public final class FuelSinteringState {
    public static final int CAPACITY = 64;
    public static final int WORK = 400;
    private int input;
    private int output;
    private int progress;

    public int input() { return input; }
    public int output() { return output; }
    public int progress() { return progress; }

    /** 只接受合法生料；返回实际投入件数，模拟调用不改变账本。 */
    public int insert(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), CAPACITY - input);
        if (!simulate) input += accepted;
        return accepted;
    }

    /** 手工取空生料时清除该件工时；部分取料仍有同类生料时继续保留。 */
    public int takeInput(int amount, boolean simulate) {
        int taken = Math.min(Math.max(0, amount), input);
        if (!simulate) {
            input -= taken;
            if (input == 0) progress = 0;
        }
        return taken;
    }

    public int takeOutput(int amount, boolean simulate) {
        int taken = Math.min(Math.max(0, amount), output);
        if (!simulate) output -= taken;
        return taken;
    }

    /** 缺热、缺料或满输出时暂停；完成门再次检查后一次扣料并增产。 */
    public boolean tick(boolean heated) {
        if (!heated || input == 0 || output == CAPACITY) return false;
        progress++;
        if (progress < WORK) return true;
        input--;
        output++;
        progress = 0;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Input", input);
        tag.putInt("Output", output);
        tag.putInt("Progress", progress);
        return tag;
    }

    /** 外部或旧 NBT 只按有界数量恢复，空生料不恢复悬空工时。 */
    public void load(CompoundTag tag) {
        input = Math.clamp(tag.getInt("Input"), 0, CAPACITY);
        output = Math.clamp(tag.getInt("Output"), 0, CAPACITY);
        progress = input == 0 ? 0 : Math.clamp(tag.getInt("Progress"), 0, WORK - 1);
    }
}
