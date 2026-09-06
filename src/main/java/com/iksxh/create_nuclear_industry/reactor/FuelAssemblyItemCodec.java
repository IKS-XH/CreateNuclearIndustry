package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * 正式燃料物品与加载器无关列状态之间的适配器。
 *
 * <p>燃料列只保存耐久整数和燃耗小数余量；本类负责在服务端事务边界读取真实
 * {@link ItemStack} 的耐久，并在取出时重建物品。列完整度、温度和缓存热量不会写入
 * 物品栈，从而避免物品状态与反应堆快照形成第二份权威副本。</p>
 */
public final class FuelAssemblyItemCodec {
    private FuelAssemblyItemCodec() {
    }

    /** 判断物品栈是否为本模组正式的新燃料组件。只接受一个正式注册 ID。 */
    public static boolean isFreshFuel(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.is(ModItems.FRESH_FUEL_ASSEMBLY.get());
    }

    /** 判断物品栈是否为首发唯一的冷却乏燃料产物。 */
    public static boolean isCooledSpentFuel(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.is(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());
    }

    /** 判断物品栈是否属于换料端口允许持久化的燃料组件形态。 */
    public static boolean isValidStoredFuel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (stack.getCount() != 1) {
            return false;
        }
        if (isCooledSpentFuel(stack)) {
            return true;
        }
        if (!isFreshFuel(stack)) {
            return false;
        }
        try {
            readFreshFuel(stack);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /** 判断物品是否为可参与模拟的正式燃料组件，包括已耗尽但尚未转换的旧栈。 */
    public static boolean isFuelAssembly(ItemStack stack) {
        return isValidStoredFuel(stack) && stack != null && !stack.isEmpty();
    }

    /**
     * 从新燃料物品栈读取原版耐久语义。
     *
     * @param stack 至少包含一个新燃料组件的物品栈
     * @return 物品栈当前的已损伤值与最大耐久
     * @throws IllegalArgumentException 物品不是正式新燃料或耐久值非法时抛出
     */
    public static FuelAssemblyState readFreshFuel(ItemStack stack) {
        if (!isFreshFuel(stack)) {
            throw new IllegalArgumentException("a formal fresh fuel assembly is required");
        }
        int maxDamage = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        if (maxDamage <= 0 || damage < 0 || damage > maxDamage) {
            throw new IllegalArgumentException("fresh fuel durability is outside the item range");
        }
        return FuelAssemblyState.installed(maxDamage, damage);
    }

    /** 用列中尚未耗尽的耐久状态重建一个单件新燃料物品栈。 */
    public static ItemStack writeFreshFuel(FuelAssemblyState state) {
        if (state == null || !state.present() || state.exhausted()) {
            throw new IllegalArgumentException("a usable fuel assembly state is required");
        }
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        if (state.maxDamage() != stack.getMaxDamage()) {
            throw new IllegalArgumentException("fuel durability does not match the registered item");
        }
        stack.setDamageValue(state.damage());
        return stack;
    }

    /** 将端口物品转换为内存中的模拟投影；冷却乏燃料映射为已耗尽状态。 */
    public static FuelAssemblyState simulationState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FuelAssemblyState.empty();
        }
        if (isFreshFuel(stack)) {
            return readFreshFuel(stack);
        }
        if (isCooledSpentFuel(stack) && stack.getCount() == 1) {
            return FuelAssemblyState.installed(
                    ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY,
                    ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY);
        }
        throw new IllegalArgumentException("a valid stored fuel assembly is required");
    }

    /** 按旧快照中的耐久投影创建迁移用端口物品。 */
    public static ItemStack fromLegacyState(FuelAssemblyState state) {
        if (state == null || !state.present()) {
            return ItemStack.EMPTY;
        }
        return state.exhausted()
                ? createCooledSpentFuel()
                : writeFreshFuel(state);
    }

    /** 复制完整 ItemStack，只修改原版耐久组件并固定为单件。 */
    public static ItemStack copyWithDamage(ItemStack stack, int damage) {
        if (!isFreshFuel(stack) || stack.getCount() != 1
                || damage < 0 || damage > stack.getMaxDamage()) {
            throw new IllegalArgumentException("a single fresh fuel assembly is required");
        }
        ItemStack copy = stack.copyWithCount(1);
        copy.setDamageValue(damage);
        return copy;
    }

    /** 创建首发规定的冷却乏燃料产物；该物品不携带热态或温度字段。 */
    public static ItemStack createCooledSpentFuel() {
        return new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());
    }
}
