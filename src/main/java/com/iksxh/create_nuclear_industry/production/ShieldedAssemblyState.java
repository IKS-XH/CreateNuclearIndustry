package com.iksxh.create_nuclear_industry.production;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 装配台唯一物品与工时账本。最多四个输入槽按活动配方排列，四个输出槽保存同一批的产物。
 * 此类不查询世界；方块实体在服务端确认配方和动力后才允许提交完整批次。
 */
public final class ShieldedAssemblyState {
    public static final int[] COST = {8, 4, 2, 1};
    public static final int WORK = 25_600;
    public static final int OUTPUT_SLOTS = 4;
    private final ItemStack[] input = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final ItemStack[] output = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private boolean disassemblyMode;
    private int progress;
    private String operation = "";
    private String recipeId = "";
    private int[] costs = COST.clone();
    private int work = WORK;

    public String operation() { return operation; }
    public int cost(int slot) { return slot >= 0 && slot < costs.length ? costs[slot] : 0; }
    public int work() { return work; }
    public boolean disassemblyMode() { return disassemblyMode; }

    /** 机器清空后才可切换玩家选择；活动配方工序仍由实际投料决定。 */
    public boolean switchMode() {
        if (progress != 0 || !outputEmpty() || !Arrays.stream(input).allMatch(ItemStack::isEmpty)) return false;
        disassemblyMode = !disassemblyMode;
        return true;
    }

    /** 配方数量或身份变化即废弃旧工时；此调用只配置批次，不替模拟投料选择工序。 */
    public void configure(String mode, String id, int[] quantities, int requiredWork) {
        if (quantities.length < 1 || quantities.length > 4 || requiredWork < 1
                || Arrays.stream(quantities).anyMatch(n -> n < 1 || n > 64))
            throw new IllegalArgumentException("无效装配批次");
        if (!recipeId.equals(id) || !Arrays.equals(costs, quantities) || work != requiredWork) progress = 0;
        recipeId = id;
        costs = quantities.clone();
        work = requiredWork;
    }

    private void unlockIfEmpty() {
        if (progress == 0 && outputEmpty() && Arrays.stream(input).allMatch(ItemStack::isEmpty)) {
            operation = "";
            recipeId = "";
        }
    }

    public ItemStack input(int slot) { return slot >= 0 && slot < 4 ? input[slot].copy() : ItemStack.EMPTY; }
    public ItemStack output() { return output(0); }
    public ItemStack output(int slot) {
        return slot >= 0 && slot < OUTPUT_SLOTS ? output[slot].copy() : ItemStack.EMPTY;
    }
    public int progress() { return progress; }
    public boolean ready() {
        for (int slot = 0; slot < costs.length; slot++) if (input[slot].getCount() < costs[slot]) return false;
        return true;
    }
    public boolean outputEmpty() { return Arrays.stream(output).allMatch(ItemStack::isEmpty); }

    /** 仅在外层已核实材料身份后调用；模拟和不兼容组件均不改写账本。 */
    public ItemStack insert(int slot, ItemStack offered, boolean simulate) {
        return insert(slot, offered, simulate, operation.isEmpty() ? "manufacture" : operation);
    }

    /** 首次真实接受物品才锁定工序；所有面和机械臂复用同一服务端账本。 */
    public ItemStack insert(int slot, ItemStack offered, boolean simulate, String mode) {
        if (!operation.isEmpty() && !operation.equals(mode)) return offered.copy();
        if (slot < 0 || slot >= 4 || offered.isEmpty()) return offered.copy();
        ItemStack stored = input[slot];
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, offered)) return offered.copy();
        int room = Math.min(64, offered.getMaxStackSize()) - stored.getCount();
        int accepted = Math.min(offered.getCount(), Math.max(0, room));
        if (!simulate && accepted > 0) {
            operation = mode;
            if (stored.isEmpty()) input[slot] = offered.copyWithCount(accepted);
            else stored.grow(accepted);
        }
        return offered.copyWithCount(offered.getCount() - accepted);
    }

    /** 提取不足一批后立即清除工时，不能让后续补料继承旧进度。 */
    public ItemStack extractInput(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= 4 || amount <= 0 || input[slot].isEmpty()) return ItemStack.EMPTY;
        ItemStack result = input[slot].copyWithCount(Math.min(amount, input[slot].getCount()));
        if (!simulate) {
            input[slot].shrink(result.getCount());
            if (input[slot].isEmpty()) input[slot] = ItemStack.EMPTY;
            if (!ready()) progress = 0;
            unlockIfEmpty();
        }
        return result;
    }

    public ItemStack extractOutput(int amount, boolean simulate) {
        return extractOutput(0, amount, simulate);
    }
    public ItemStack extractOutput(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= OUTPUT_SLOTS || amount <= 0 || output[slot].isEmpty()) return ItemStack.EMPTY;
        ItemStack result = output[slot].copyWithCount(Math.min(amount, output[slot].getCount()));
        if (!simulate) {
            output[slot].shrink(result.getCount());
            if (output[slot].isEmpty()) output[slot] = ItemStack.EMPTY;
            unlockIfEmpty();
        }
        return result;
    }

    public void invalidateProgress() { progress = 0; unlockIfEmpty(); }

    /** 有效速度以RPM计，单tick最多计256；完成前重新检查全部物料和输出槽。 */
    public boolean advance(float speed, ItemStack freshResult) {
        return advance(speed, List.of(freshResult));
    }

    /** 上一批全部取出才累积新工时，所有产物可容纳后一次扣料和入账。 */
    public boolean advance(float speed, List<ItemStack> results) {
        if (!Float.isFinite(speed) || Math.abs(speed) < 32 || !ready() || !outputEmpty()
                || results.isEmpty() || results.size() > OUTPUT_SLOTS
                || results.stream().anyMatch(stack -> stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()))
            return false;
        progress = (int) Math.min((long) work, (long) progress + (int) Math.min(Math.abs(speed), 256));
        if (progress < work) return true;
        for (int slot = 0; slot < costs.length; slot++) {
            input[slot].shrink(costs[slot]);
            if (input[slot].isEmpty()) input[slot] = ItemStack.EMPTY;
        }
        for (int slot = 0; slot < results.size(); slot++) output[slot] = results.get(slot).copy();
        progress = 0;
        return true;
    }

    /** 完整ItemStack及进度共用同一保存格式，携带物与区块存储均复用此格式。 */
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("DisassemblyMode", disassemblyMode);
        tag.putInt("Progress", progress);
        tag.putString("Operation", operation);
        tag.putString("RecipeId", recipeId);
        tag.putIntArray("Costs", costs);
        tag.putInt("Work", work);
        for (int slot = 0; slot < 4; slot++) if (!input[slot].isEmpty())
            tag.put("Input" + slot, input[slot].save(registries));
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) if (!output[slot].isEmpty())
            tag.put(outputKey(slot), output[slot].save(registries));
        return tag;
    }

    // 首槽沿用旧键保存，原先尚未取出的单件产物可直接保留。
    private static String outputKey(int slot) { return slot == 0 ? "Output" : "Output" + slot; }

    /** 恢复当前版本完整物料和批次快照；世界配方由主控重新校验，不从进度重造产物。 */
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        Arrays.fill(input, ItemStack.EMPTY);
        for (int slot = 0; slot < 4; slot++) {
            ItemStack parsed = ItemStack.parseOptional(registries, tag.getCompound("Input" + slot));
            if (!parsed.isEmpty() && parsed.getCount() <= 64) input[slot] = parsed;
        }
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack parsed = ItemStack.parseOptional(registries, tag.getCompound(outputKey(slot)));
            output[slot] = parsed.getCount() <= parsed.getMaxStackSize() ? parsed : ItemStack.EMPTY;
        }
        disassemblyMode = tag.getBoolean("DisassemblyMode");
        operation = tag.getString("Operation");
        recipeId = tag.getString("RecipeId");
        int[] savedCosts = tag.getIntArray("Costs");
        costs = savedCosts.length >= 1 && savedCosts.length <= 4
                && Arrays.stream(savedCosts).allMatch(n -> n > 0 && n <= 64) ? savedCosts : COST.clone();
        work = Math.max(1, tag.contains("Work") ? tag.getInt("Work") : WORK);
        progress = Math.clamp(tag.getInt("Progress"), 0, work - 1);
        if (!ready()) progress = 0;
        unlockIfEmpty();
    }
}
