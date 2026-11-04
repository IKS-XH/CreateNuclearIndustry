package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerWindowBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPortBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 高压锅炉四条 Ponder 客户端教学故事线。模板中的炉体均为合法完整结构；液位、管内流向和动力
 * 只用于当前 Ponder 临时世界的可视讲解，不调用服务端账本、热工结算或正式流体交易。
 */
public final class BoilerPonderScenes {
    private static final int OX = 3;
    private static final int OY = 0;
    private static final int OZ = 3;
    private static final int FRONT = OZ;
    private static final BlockPos WATER_PORT = new BlockPos(4, 1, FRONT);
    private static final BlockPos WATER_TANK = new BlockPos(4, 1, 0);
    private static final BlockPos HOT_PORT = new BlockPos(8, 0, FRONT);
    private static final BlockPos HOT_TANK = new BlockPos(8, 0, 0);
    private static final BlockPos COLD_PORT = new BlockPos(4, 3, FRONT);
    private static final BlockPos COLD_TANK = new BlockPos(4, 3, 0);
    private static final BlockPos CONTROLLER = new BlockPos(6, 2, FRONT);
    private static final BlockPos NORMAL_PORT = new BlockPos(4, 4, FRONT);
    private static final BlockPos SUPERCRITICAL_PORT = new BlockPos(8, 4, FRONT);
    private static final BlockPos NORMAL_TANK = new BlockPos(4, 4, 0);
    private static final BlockPos SUPERCRITICAL_TANK = new BlockPos(8, 4, 0);

    private BoilerPonderScenes() {
    }

    /** 展示可变尺寸合法小锅炉的底部、双区隔层、侧口、顶阀及完整成型顺序。 */
    public static void highPressureBoilerBuild(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("high_pressure_boiler_build", "High-pressure boiler: Build and zones");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.72F);
        // 基础板绕开与炉底同处Y=0的7×7占位，避免底板和炉底重复淡入。
        Selection basePlate = util.select().fromTo(0, 0, 0, 12, 0, 2)
                .add(util.select().fromTo(0, 0, 10, 12, 0, 12))
                .add(util.select().fromTo(0, 0, 3, 2, 0, 9))
                .add(util.select().fromTo(10, 0, 3, 12, 0, 9));
        scene.world().showSection(basePlate, Direction.UP);
        Selection whole = util.select().fromTo(OX, OY, OZ, OX + 6, OY + 6, OZ + 6);

        Selection bottom = util.select().fromTo(OX, OY, OZ, OX + 6, OY, OZ + 6);
        scene.world().showSection(bottom, Direction.UP);
        scene.idle(15);
        note(scene, bottom, util.vector().centerOf(6, 0, 6), 90,
                "By default, length, width, and height are each 5–16 blocks and can be adjusted separately. This example is 7×7×7.");

        Selection water = util.select().fromTo(OX, 1, OZ, OX + 6, 2, OZ + 6);
        scene.world().showSection(water, Direction.DOWN);
        scene.idle(15);
        Selection waterFace = frontCasing(util, 1, 2);
        scene.world().hideSection(waterFace, Direction.SOUTH);
        fillWindow(scene, new BlockPos(5, 1, FRONT), .65F);
        note(scene, water, util.vector().centerOf(6, 2, 6), 90,
                "Place the built-in heat exchangers on the base. Hot inlets share this layer and may replace non-corner bottom-edge casing.");
        scene.world().showSection(waterFace, Direction.SOUTH);

        Selection partition = util.select().fromTo(OX, 3, OZ, OX + 6, 3, OZ + 6);
        scene.world().showSection(partition, Direction.DOWN);
        scene.idle(15);
        Selection partitionFace = frontCasing(util, 3, 3);
        scene.world().hideSection(partitionFace, Direction.SOUTH);
        note(scene, partition, util.vector().centerOf(6, 3, 6), 90,
                "Complete the partition with reheating sections and casing. Cold outlets share its layer and stay off the edges.");
        scene.world().showSection(partitionFace, Direction.SOUTH);

        Selection steam = util.select().fromTo(OX, 4, OZ, OX + 6, 5, OZ + 6);
        scene.world().showSection(steam, Direction.DOWN);
        scene.idle(15);
        Selection steamFace = frontCasing(util, 4, 5);
        scene.world().hideSection(steamFace, Direction.SOUTH);
        note(scene, steam, util.vector().centerOf(6, 5, 6), 90,
                "Leave at least one layer above and below the partition. Place water and controller ports in the water zone.");
        note(scene, steam, util.vector().centerOf(6, 5, 6), 90,
                "Place steam ports in the steam zone. Side ports face horizontally outward.");
        scene.world().showSection(steamFace, Direction.SOUTH);

        Selection roof = util.select().fromTo(OX, 6, OZ, OX + 6, 6, OZ + 6);
        scene.world().showSection(roof, Direction.DOWN);
        scene.idle(8);
        note(scene, roof, util.vector().centerOf(6, 6, 6), 90,
                "Place safety valves on non-edge roof blocks and keep each outlet clear. Add water, steam, and coolant ports as needed.");
        note(scene, whole, util.vector().centerOf(6, 3, 6), 95,
                "Effective heat loops equal the smaller count of built-in heat exchangers and reheating sections.");
        scene.markAsFinished();
    }

    /** 展示给水、冷热液回路、独立动力泵及水位和升温升压的先后关系。 */
    public static void highPressureBoilerOperation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("high_pressure_boiler_operation", "High-pressure boiler: Connect and run");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.72F);
        scene.showBasePlate();
        Selection boiler = util.select().fromTo(OX, OY, OZ, OX + 6, OY + 6, OZ + 6);
        scene.world().showSection(boiler, Direction.DOWN);
        scene.idle(12);
        note(scene, boiler, util.vector().centerOf(6, 2, 6), 95,
                "By default, length, width, and height are each 5–16 blocks and can be adjusted separately.");

        Selection waterDrive = util.select().position(5, 1, 0).add(util.select().position(5, 1, 1));
        Selection hotDrive = util.select().position(9, 0, 0).add(util.select().position(9, 0, 1));
        Selection coldWestDrive = util.select().position(5, 3, 0).add(util.select().position(5, 3, 1));
        Selection coldEastDrive = util.select().position(9, 3, 0).add(util.select().position(9, 3, 1));
        Selection feed = util.select().fromTo(4, 1, 0, 4, 1, 2).add(waterDrive);
        Selection hot = util.select().fromTo(8, 0, 0, 8, 0, 2).add(hotDrive);
        Selection cold = util.select().fromTo(4, 3, 0, 4, 3, 2)
                .add(util.select().fromTo(8, 3, 0, 8, 3, 2))
                .add(coldWestDrive).add(coldEastDrive);
        Selection front = frontCasing(util, 1, 5);
        scene.world().hideSection(front, Direction.SOUTH);
        fillWindow(scene, new BlockPos(5, 1, FRONT), 0);
        note(scene, util.select().fromTo(OX, 1, OZ, OX + 6, 2, OZ + 6),
                util.vector().centerOf(6, 2, 6), 90,
                "Water-zone volume determines water capacity; steam-zone volume determines steam capacity.");
        scene.world().showSection(feed, Direction.SOUTH);
        scene.idle(12);
        setTankFluid(scene, WATER_TANK, new FluidStack(Fluids.WATER, 1000));
        scene.world().setKineticSpeed(util.select().position(4, 1, 1), -64);
        scene.world().setKineticSpeed(waterDrive, 64);
        scene.effects().rotationSpeedIndicator(new BlockPos(4, 1, 1));
        scene.effects().rotationDirectionIndicator(util.grid().at(4, 1, 1));
        fillWindow(scene, new BlockPos(5, 1, FRONT), .35F);
        scene.overlay().showBigLine(PonderPalette.BLUE, util.vector().centerOf(WATER_TANK),
                util.vector().centerOf(WATER_PORT), 70);
        note(scene, feed, util.vector().centerOf(WATER_PORT), 95,
                "Fill the water zone first. Then circulate coolant on separate lines.");

        scene.world().showSection(cold, Direction.SOUTH);
        scene.idle(12);
        setTankFluid(scene, COLD_TANK, new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 1000));
        setTankFluid(scene, new BlockPos(8, 3, 0), new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 1000));
        scene.world().setKineticSpeed(util.select().position(4, 3, 1), -64);
        scene.world().setKineticSpeed(util.select().position(8, 3, 1), -64);
        scene.world().setKineticSpeed(coldWestDrive, 64);
        scene.world().setKineticSpeed(coldEastDrive, 64);
        scene.effects().rotationSpeedIndicator(new BlockPos(4, 3, 1));
        scene.effects().rotationSpeedIndicator(new BlockPos(8, 3, 1));
        scene.effects().rotationDirectionIndicator(util.grid().at(4, 3, 1));
        scene.effects().rotationDirectionIndicator(util.grid().at(8, 3, 1));
        scene.overlay().showBigLine(PonderPalette.BLUE, util.vector().centerOf(COLD_PORT),
                util.vector().centerOf(COLD_TANK), 70);
        note(scene, cold, util.vector().centerOf(COLD_PORT), 95,
                "Start the cold-coolant return before supplying heat. A blocked return limits further heat intake.");

        scene.world().showSection(hot, Direction.SOUTH);
        scene.idle(12);
        setTankFluid(scene, HOT_TANK, new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1000));
        scene.world().setKineticSpeed(util.select().position(8, 0, 1), 64);
        scene.world().setKineticSpeed(hotDrive, 64);
        scene.effects().rotationSpeedIndicator(new BlockPos(8, 0, 1));
        scene.effects().rotationDirectionIndicator(util.grid().at(8, 0, 1));
        scene.overlay().showBigLine(PonderPalette.RED, util.vector().centerOf(HOT_TANK),
                util.vector().centerOf(HOT_PORT), 70);
        note(scene, hot, util.vector().centerOf(HOT_PORT), 95,
                "With water supplied and cold return flowing, start the hot-coolant input to heat the boiler.");

        fillWindow(scene, new BlockPos(5, 1, FRONT), .8F);
        fillWindow(scene, new BlockPos(7, 2, FRONT), .45F);
        note(scene, util.select().fromTo(OX, 1, OZ, OX + 6, 2, OZ + 6),
                util.vector().centerOf(3, 1, 6), 95,
                "With other conditions equal, a larger water zone warms more slowly.");
        note(scene, util.select().fromTo(OX, 4, OZ, OX + 6, 5, OZ + 6),
                util.vector().centerOf(6, 4, 6), 95,
                "Water boils before steam is reheated; a larger steam zone pressurizes more slowly.");
        scene.overlay().showOutlineWithText(boiler, 95)
                .text("Use goggles to check boiler status, temperature, pressure, and effective heat loops.")
                .attachKeyFrame().pointAt(util.vector().centerOf(6, 2, 6)).placeNearTarget();
        scene.idle(115);
        scene.world().showSection(front, Direction.SOUTH);
        scene.markAsFinished();
    }

    /** 展示两个汽种的独立库存、分开的管路，以及汽口选择和压力下限控件。 */
    public static void highPressureBoilerSteam(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("high_pressure_boiler_steam", "High-pressure boiler: Steam output and pressure");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.72F);
        scene.showBasePlate();
        Selection boiler = util.select().fromTo(OX, OY, OZ, OX + 6, OY + 6, OZ + 6);
        Selection ordinary = util.select().fromTo(4, 4, 0, 4, 4, 2);
        Selection supercritical = util.select().fromTo(8, 4, 0, 8, 4, 2);
        scene.world().showSection(boiler, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(ordinary, Direction.SOUTH);
        scene.world().showSection(supercritical, Direction.SOUTH);
        scene.idle(10);
        setTankFluid(scene, NORMAL_TANK, new FluidStack(TurbineContent.STEAM.get(), 1000));
        setTankFluid(scene, SUPERCRITICAL_TANK, FluidStack.EMPTY);
        setSteamSelection(scene, NORMAL_PORT, 0);
        setSteamSelection(scene, SUPERCRITICAL_PORT, 1);
        scene.overlay().showBigLine(PonderPalette.WHITE, util.vector().centerOf(NORMAL_PORT),
                util.vector().centerOf(NORMAL_TANK), 70);
        scene.overlay().showBigLine(PonderPalette.GREEN, util.vector().centerOf(SUPERCRITICAL_PORT),
                util.vector().centerOf(SUPERCRITICAL_TANK), 70);
        note(scene, ordinary.add(supercritical), util.vector().centerOf(6, 4, 2), 105,
                "Both steam types can leave the boiler actively through legal industrial pipes. These outlet lines need no external pump.");
        note(scene, ordinary.add(supercritical), util.vector().centerOf(6, 4, 2), 100,
                "New steam is classified by actual temperature and pressure. Ordinary and supercritical steam use separate inventories.");
        note(scene, boiler, util.vector().centerOf(6, 4, 6), 100,
                "Both inventories share zone capacity and boiler pressure. Changing the minimum does not change stored steam types.");

        scene.overlay().showScrollInput(util.vector().blockSurface(NORMAL_PORT, Direction.NORTH).add(0, .44, 0), Direction.NORTH, 70);
        scene.overlay().showControls(util.vector().topOf(NORMAL_PORT), Pointing.DOWN, 70).rightClick();
        scene.overlay().showScrollInput(util.vector().blockSurface(SUPERCRITICAL_PORT, Direction.NORTH).add(0, .44, 0), Direction.NORTH, 70);
        scene.overlay().showControls(util.vector().topOf(SUPERCRITICAL_PORT), Pointing.DOWN, 70).rightClick();
        note(scene, util.select().position(NORMAL_PORT).add(util.select().position(SUPERCRITICAL_PORT)),
                util.vector().topOf(NORMAL_PORT), 95,
                "Choose a steam type at each outlet with Create's native option. Each drains only its selected inventory; no downgrade conversion occurs.");
        scene.overlay().showScrollInput(util.vector().blockSurface(CONTROLLER, Direction.NORTH), Direction.NORTH, 70);
        scene.overlay().showControls(util.vector().topOf(CONTROLLER), Pointing.DOWN, 70).rightClick();
        scene.idle(35);
        setPressureControlValue(scene, 10);
        scene.overlay().showScrollInput(util.vector().blockSurface(CONTROLLER, Direction.NORTH), Direction.NORTH, 70);
        note(scene, util.select().position(CONTROLLER), util.vector().topOf(CONTROLLER), 100,
                "The minimum output pressure ranges from 0–100. Lower it to 10%. It gates normal output; it is not a target boiler pressure or safety-valve pressure.");
        setTankFluid(scene, SUPERCRITICAL_TANK, new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 1000));
        scene.overlay().showBigLine(PonderPalette.GREEN, util.vector().centerOf(SUPERCRITICAL_PORT),
                util.vector().centerOf(SUPERCRITICAL_TANK), 70);
        note(scene, supercritical, util.vector().centerOf(6, 4, 2), 105,
                "Stored, heat-qualified supercritical steam can drain to the shared minimum below the new-steam classification pressure. Its type stays unchanged; keep steam types on separate lines.");
        scene.markAsFinished();
    }

    /** 两个独立阀的净空与堵塞、停止新收热及干烧/超压风险；不启动正式事故。 */
    public static void highPressureBoilerSafety(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("high_pressure_boiler_safety", "高压锅炉：泄压与安全");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.72F);
        scene.showBasePlate();
        Selection boiler = util.select().fromTo(3, 0, 3, 9, 6, 9);
        Selection valves = util.select().position(6, 6, 6).add(util.select().position(8, 6, 6));
        scene.world().showSection(boiler, Direction.DOWN);
        scene.idle(20);
        scene.rotateCameraY(35);
        note(scene, valves, util.vector().topOf(6, 6, 6), 138,
                "顶面非边框可安装多个安全阀，每个阀正上方一格都必须保持空气；护目镜可查畅通阀数和总排量。");
        ventLines(scene, util, true, true);
        note(scene, valves, util.vector().topOf(8, 6, 6), 132,
                "默认90%炉压开启、80%关闭；每个畅通阀最多排512 mB/t，不会随炉内容积放大。");
        scene.world().setBlock(new BlockPos(6, 7, 6), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), false);
        scene.world().showSection(util.select().position(6, 7, 6), Direction.DOWN);
        scene.idle(20);
        ventLines(scene, util, false, true);
        note(scene, valves.add(util.select().position(6, 7, 6)), util.vector().centerOf(6, 7, 6), 120,
                "堵住一个阀只会阻止它排汽，另一个畅通阀仍能泄压；增加有效阀门并保持所有出口畅通。");
        scene.world().setBlock(new BlockPos(8, 7, 6), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), false);
        scene.world().showSection(util.select().position(8, 7, 6), Direction.DOWN);
        scene.idle(20);
        note(scene, valves, util.vector().topOf(8, 6, 6), 149,
                "全部阀门堵塞就无法泄压。先停热并疏通汽路，不要把出汽压力下限当成安全阀或目标炉压。");
        scene.world().setBlock(new BlockPos(6, 7, 6), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(8, 7, 6), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        scene.effects().indicateRedstone(CONTROLLER);
        note(scene, util.select().position(CONTROLLER), util.vector().centerOf(CONTROLLER), 149,
                "控制器接红石可停止新收热；无热液或冷液回路堵塞也会停收热，但已有工质和余热不会凭空消失。");
        Selection front = frontCasing(util, 1, 5);
        scene.world().hideSection(front, Direction.SOUTH);
        scene.idle(20);
        fillWindow(scene, new BlockPos(5, 1, FRONT), 0);
        note(scene, util.select().position(WATER_PORT), util.vector().centerOf(WATER_PORT), 172,
                "无水且炉壁达到沸点时，默认连续20秒干烧会损毁一件换热器或再加热段；持续供水，停热降温后再维护。");
        // 移开正面与顶盖后再高亮底层部件；这里只呈现损毁结果，不提交正式事故或掉落。
        scene.world().hideSection(util.select().fromTo(3, 6, 3, 9, 6, 9), Direction.UP);
        scene.idle(20);
        scene.world().hideSection(util.select().fromTo(4, 3, 4, 8, 3, 8), Direction.UP);
        scene.idle(20);
        BlockPos damaged = new BlockPos(5, 0, 6);
        note(scene, util.select().position(damaged), util.vector().topOf(damaged), 115,
                "干烧损坏的部件不会掉落完整设备。先解除危险，再补回缺失部件并重新检查结构。");
        scene.world().setBlock(damaged, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        note(scene, util.select().position(CONTROLLER), util.vector().centerOf(CONTROLLER), 160,
                "默认额定耐压100%：超出后累计耐受10秒，达到150%立即向随机一面喷爆；压力越高，危险范围越大。");
        note(scene, util.select().position(CONTROLLER), util.vector().centerOf(CONTROLLER), 149,
                "默认降到95%及以下并持续2秒，才清除累计超压时间；短暂回落不能把危险计时重新开始。");
        scene.overlay().showControls(util.vector().topOf(CONTROLLER), Pointing.DOWN, 70)
                .withItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        note(scene, util.select().position(CONTROLLER), util.vector().centerOf(CONTROLLER), 155,
                "危险时手动拆炉会直接触发对应事故，超压时优先喷爆。先停热、泄压、降温，再正常拆除；锅炉事故不会产生核废料。");
        scene.markAsFinished();
    }

    /** 只画各自阀口的排汽方向，不消耗临时或正式工质。 */
    private static void ventLines(CreateSceneBuilder scene, SceneBuildingUtil util, boolean first, boolean second) {
        if (first) scene.overlay().showBigLine(PonderPalette.WHITE, util.vector().topOf(6, 6, 6), util.vector().topOf(6, 8, 6), 90);
        if (second) scene.overlay().showBigLine(PonderPalette.WHITE, util.vector().topOf(8, 6, 6), util.vector().topOf(8, 8, 6), 90);
    }

    /** 暂时隐藏正面壳格以直接查看水区、汽区和完整隔层，端口与观察窗保持可见。 */
    private static Selection frontCasing(SceneBuildingUtil util, int minY, int maxY) {
        Selection casing = null;
        for (int y = minY; y <= maxY; y++) {
            for (int x = OX; x <= OX + 6; x++) {
                boolean port = (x == 4 && y == 1) || (x == 8 && y == 2)
                        || ((x == 4 || x == 8) && (y == 3 || y == 4));
                boolean controller = x == 6 && y == 2;
                boolean window = (x == 5 || x == 7) && (y == 1 || y == 2 || y == 4 || y == 5);
                if (port || controller || window) continue;
                if (casing == null)
                    casing = util.select().position(x, y, FRONT);
                else
                    casing = casing.add(util.select().position(x, y, FRONT));
            }
        }
        return casing;
    }

    /** Ponder正文寿命比设定时长多10tick，因此下一段前留20tick，确保净间隔不少于10tick。 */
    private static void note(CreateSceneBuilder scene, Selection target, net.minecraft.world.phys.Vec3 point,
                             int duration, String text) {
        scene.overlay().showOutlineWithText(target, duration).text(text)
                .attachKeyFrame().pointAt(point).placeNearTarget();
        scene.idle(duration + 20);
    }

    /** 在当前 Ponder 临时世界设置窗口液位；该显示不修改控制器库存或服务端运行状态。 */
    private static void fillWindow(CreateSceneBuilder scene, BlockPos position, float fill) {
        scene.world().modifyBlockEntity(position, BoilerWindowBlockEntity.class,
                window -> window.setFill(fill));
    }

    /** 直接改写临时 Create 储罐，用来标明流体去向，不触发正式端口能力或交易。 */
    private static void setTankFluid(CreateSceneBuilder scene, BlockPos position, FluidStack fluid) {
        scene.world().modifyBlockEntity(position, FluidTankBlockEntity.class, tank -> {
            tank.getTankInventory().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (!fluid.isEmpty())
                tank.getTankInventory().fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        });
    }

    /** 设置临时汽口的Create原生选项；顺序0为普通蒸汽，1为超临界蒸汽。 */
    private static void setSteamSelection(CreateSceneBuilder scene, BlockPos position, int value) {
        scene.world().modifyBlockEntity(position, BoilerPortBlockEntity.class, port -> {
            ScrollValueBehaviour selection = port.getBehaviour(ScrollValueBehaviour.TYPE);
            if (selection != null)
                selection.setValue(value);
        });
    }

    /** 只改Ponder临时客户端实体的原生滚动框显示；客户端回调会拒绝正式账本写入。 */
    private static void setPressureControlValue(CreateSceneBuilder scene, int value) {
        scene.world().modifyBlockEntity(CONTROLLER, BoilerControllerBlockEntity.class, controller -> {
            ScrollValueBehaviour control = controller.getAllBehaviours().stream()
                    .filter(ScrollValueBehaviour.class::isInstance)
                    .map(ScrollValueBehaviour.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("临时控制器缺少压力滚动控件"));
            control.setValue(value);
        });
    }
}
