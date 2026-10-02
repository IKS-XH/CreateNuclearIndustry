package com.iksxh.create_nuclear_industry.production;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 单台离心机的权威账本。数量为 mB/件，进度为额定工作量，磨损以整数积分保存。
 * 模拟调用只读；密闭批次保留配方身份及输出快照，使卸载和重载均不重抽结果。
 */
public final class CentrifugeState {
    public static final int TANK_CAPACITY = 4000;
    public static final int SLOT_CAPACITY = 64;
    public static final long WEAR_LIMIT = 128L * 144000L * 1024L;

    public int slurryMb;
    public int waterMb;
    public ItemStack enriched = ItemStack.EMPTY;
    public ItemStack depleted = ItemStack.EMPTY;
    public Batch batch;
    public double progress;
    public long wearUnits;
    public double wearRemainder;
    public int stableTicks;
    public float observedSpeed;

    /** 已接管的一批，结果与输入流体在开始时固定。 */
    public record Batch(ResourceLocation recipeId, FluidStack input, ItemStack enriched,
                        ItemStack depleted, FluidStack water, int work) {
        public Batch {
            if (recipeId == null || input.isEmpty() || enriched.isEmpty() || depleted.isEmpty()
                    || water.isEmpty() || work <= 0) {
                throw new IllegalArgumentException("离心批次缺少输入、产物或工作量");
            }
            input = input.copy();
            enriched = enriched.copy();
            depleted = depleted.copy();
            water = water.copy();
        }
    }

    public boolean canFit(Batch candidate) {
        return canFitStack(enriched, candidate.enriched)
                && canFitStack(depleted, candidate.depleted)
                && (long) waterMb + candidate.water.getAmount() <= TANK_CAPACITY;
    }

    private static boolean canFitStack(ItemStack stored, ItemStack incoming) {
        return (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, incoming))
                && (long) stored.getCount() + incoming.getCount() <= Math.min(SLOT_CAPACITY, incoming.getMaxStackSize());
    }

    /** 开始时先检查全部输出，再一次接管料浆；失败时账本完全不变。 */
    public boolean begin(Batch candidate) {
        if (batch != null || slurryMb < candidate.input.getAmount() || !canFit(candidate)) {
            return false;
        }
        slurryMb -= candidate.input.getAmount();
        batch = candidate;
        progress = 0;
        return true;
    }

    /** 输出三项一次提交；若任一项堵塞，继续保留密闭批次与进度。 */
    public boolean finish() {
        if (batch == null || !canFit(batch)) {
            return false;
        }
        enriched = merge(enriched, batch.enriched);
        depleted = merge(depleted, batch.depleted);
        waterMb += batch.water.getAmount();
        batch = null;
        progress = 0;
        return true;
    }

    private static ItemStack merge(ItemStack stored, ItemStack incoming) {
        if (stored.isEmpty()) {
            return incoming.copy();
        }
        ItemStack result = stored.copy();
        result.grow(incoming.getCount());
        return result;
    }

    /** 每个服务端 tick 更新速度稳定窗；速度数值或符号变化均重置连续等待。 */
    public void observeSpeed(float speed) {
        if (Float.compare(speed, observedSpeed) != 0) {
            observedSpeed = speed;
            stableTicks = 0;
        } else if (speed != 0 && stableTicks < 20) {
            stableTicks++;
        }
    }

    /**
     * 仅在确有密闭批次且全部输出可容纳时增加进度与磨损。磨损积分避免浮点误差使轴承永不耗尽。
     * 返回是否完成并成功提交；调用方必须先经过稳定、转速和过载检查。
     */
    public boolean advance(float speed) {
        if (batch == null || !canFit(batch) || wearUnits >= WEAR_LIMIT || speed == 0 || stableTicks < 20) {
            return false;
        }
        double effective = Math.min(Math.abs((double) speed), 256.0);
        progress += effective / 128.0;
        double wearIncrement = Math.max(0, effective - 128.0) * 1024.0 + wearRemainder;
        long wholeUnits = (long) wearIncrement;
        wearRemainder = wearIncrement - wholeUnits;
        wearUnits = Math.min(WEAR_LIMIT, wearUnits + wholeUnits);
        if (progress >= batch.work) {
            return finish();
        }
        return false;
    }

    public boolean repair() {
        if (observedSpeed != 0 || wearUnits < WEAR_LIMIT) {
            return false;
        }
        wearUnits = 0;
        wearRemainder = 0;
        return true;
    }

    /** 便携物品与方块实体共用此序列化格式，避免拆放和世界存档出现两套结果。 */
    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("CentrifugeDataVersion", 1);
        tag.putInt("SlurryMb", slurryMb);
        tag.putInt("WaterMb", waterMb);
        tag.put("Enriched", enriched.saveOptional(registries));
        tag.put("Depleted", depleted.saveOptional(registries));
        tag.putDouble("Progress", progress);
        tag.putLong("WearUnits", wearUnits);
        tag.putDouble("WearRemainder", wearRemainder);
        tag.putInt("StableTicks", stableTicks);
        tag.putFloat("ObservedSpeed", observedSpeed);
        if (batch != null) {
            CompoundTag sealed = new CompoundTag();
            sealed.putString("RecipeId", batch.recipeId().toString());
            sealed.put("Input", batch.input().save(registries));
            sealed.put("Enriched", batch.enriched().saveOptional(registries));
            sealed.put("Depleted", batch.depleted().saveOptional(registries));
            sealed.put("Water", batch.water().save(registries));
            sealed.putInt("Work", batch.work());
            tag.put("Batch", sealed);
        }
    }

    /** 读取同一账本快照；不从当前配方表重建已经接管的批次。 */
    public void read(CompoundTag tag, HolderLookup.Provider registries) {
        slurryMb = Math.clamp(tag.getInt("SlurryMb"), 0, TANK_CAPACITY);
        waterMb = Math.clamp(tag.getInt("WaterMb"), 0, TANK_CAPACITY);
        enriched = ItemStack.parseOptional(registries, tag.getCompound("Enriched"));
        depleted = ItemStack.parseOptional(registries, tag.getCompound("Depleted"));
        progress = Math.max(0, tag.getDouble("Progress"));
        wearUnits = Math.clamp(tag.getLong("WearUnits"), 0, WEAR_LIMIT);
        wearRemainder = Math.clamp(tag.getDouble("WearRemainder"), 0, 0.999999999);
        stableTicks = Math.clamp(tag.getInt("StableTicks"), 0, 20);
        observedSpeed = tag.getFloat("ObservedSpeed");
        batch = null;
        if (tag.contains("Batch")) {
            CompoundTag sealed = tag.getCompound("Batch");
            try {
                batch = new Batch(ResourceLocation.parse(sealed.getString("RecipeId")),
                        FluidStack.parseOptional(registries, sealed.getCompound("Input")),
                        ItemStack.parseOptional(registries, sealed.getCompound("Enriched")),
                        ItemStack.parseOptional(registries, sealed.getCompound("Depleted")),
                        FluidStack.parseOptional(registries, sealed.getCompound("Water")), sealed.getInt("Work"));
            } catch (IllegalArgumentException ignored) {
                progress = 0;
            }
        }
    }
}
