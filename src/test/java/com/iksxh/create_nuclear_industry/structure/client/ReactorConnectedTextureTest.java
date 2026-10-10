package com.iksxh.create_nuclear_industry.structure.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.CTContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 只验证CT消费边界，不重复L1的同步/加载生命周期矩阵或02A像素检查。
 * 字面几何与ID是手工预期；所有断言运行生产判断、作用域或模型包装，禁止源码字符串测试。
 */
class ReactorConnectedTextureTest {
    private static final String WINDOW = "create_nuclear_industry:reactor_window";
    private static final String CASING = "create_nuclear_industry:reactor_casing";
    private static final String HOT = "create_nuclear_industry:reactor_hot_port";
    private static final UUID GENERATION = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private static final BlockPos ORIGIN = new BlockPos(10, 20, 30);
    private static final BlockPos MAX = new BlockPos(15, 24, 37);
    private static final BlockPos OWNER = new BlockPos(10, 21, 31);

    @AfterEach
    void clearFixturePublication() {
        // 清理属于测试的发布，不给生产代码增加reset/destroy入口。
        ReactorSurfaceSnapshots.publish(null, ReactorSurfaceSnapshot.empty());
    }

    @Test
    void sameOwnerDifferentMaterialsConnectOnEachOfSixRealPlanes() {
        // 会捕获同block限制、世界Y套到所有面或写死五格的错误。
        List<FacePair> cases = List.of(
                new FacePair(Direction.NORTH, new BlockPos(11, 21, 30), new BlockPos(12, 21, 30)),
                new FacePair(Direction.SOUTH, new BlockPos(11, 21, 37), new BlockPos(12, 21, 37)),
                new FacePair(Direction.WEST, new BlockPos(10, 21, 31), new BlockPos(10, 22, 31)),
                new FacePair(Direction.EAST, new BlockPos(15, 21, 31), new BlockPos(15, 22, 31)),
                new FacePair(Direction.DOWN, new BlockPos(11, 20, 31), new BlockPos(12, 20, 31)),
                new FacePair(Direction.UP, new BlockPos(11, 24, 31), new BlockPos(12, 24, 31)));
        for (FacePair pair : cases) {
            assertTrue(ReactorConnectedTextureBehaviour.canConnect(
                    member(pair.from(), pair.face(), CASING), member(pair.to(), pair.face(), HOT),
                    CASING, HOT, pair.face()), pair.face().name());
        }
    }

    @Test
    void differentDimensionOwnerOrGenerationNeverConnectsDespiteMatchingBlocks() {
        // 任一身份分量被漏查，就会把接壤的独立结构连成一面。
        var a = member(new BlockPos(11, 21, 30), Direction.NORTH, CASING);
        var b = member(new BlockPos(12, 21, 30), Direction.NORTH, CASING);
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                changed(b, "minecraft:the_nether", b.ownerPos(), b.ownerGeneration(), b.origin(), b.maxInclusive()),
                CASING, CASING, Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                changed(b, b.dimension(), new BlockPos(15, 21, 31), b.ownerGeneration(), b.origin(), b.maxInclusive()),
                CASING, CASING, Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                changed(b, b.dimension(), b.ownerPos(), UUID.fromString("00000000-0000-0000-0000-000000000124"), b.origin(), b.maxInclusive()),
                CASING, CASING, Direction.NORTH));
    }

    @Test
    void localReplacementUnknownMaterialOrMissingMemberClosesTheConnection() {
        // 会捕获只相信缓存expectedID、允许内部燃料棒或漏掉无记录降级的错误。
        var a = member(new BlockPos(11, 21, 30), Direction.NORTH, CASING);
        var b = member(new BlockPos(12, 21, 30), Direction.NORTH, HOT);
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a, b, "minecraft:air", HOT, Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a, b, CASING, "minecraft:stone", Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a, null, CASING, HOT, Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                member(b.pos(), Direction.NORTH, "create_nuclear_industry:reactor_fuel_rod"),
                CASING, "create_nuclear_industry:reactor_fuel_rod", Direction.NORTH));
    }

    @Test
    void wrongOutwardFaceInteriorPlaneAndSeparatedTilesAreRejected() {
        // 会捕获只比较坐标轴、忽略真实外沿/bounds或误连跨格点的错误。
        var a = member(new BlockPos(11, 21, 30), Direction.NORTH, CASING);
        assertTrue(ReactorConnectedTextureBehaviour.isUsableMember(a, a.pos(), Direction.NORTH, CASING));
        assertFalse(ReactorConnectedTextureBehaviour.isUsableMember(a, a.pos(), Direction.SOUTH, CASING));
        assertFalse(ReactorConnectedTextureBehaviour.isUsableMember(a, new BlockPos(12, 21, 30), Direction.NORTH, CASING));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                member(new BlockPos(12, 21, 31), Direction.NORTH, HOT), CASING, HOT, Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                member(new BlockPos(16, 21, 30), Direction.NORTH, HOT), CASING, HOT, Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                member(new BlockPos(13, 21, 30), Direction.NORTH, HOT), CASING, HOT, Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                changed(member(new BlockPos(12, 21, 30), Direction.NORTH, HOT), "minecraft:overworld", OWNER,
                        GENERATION, ORIGIN, new BlockPos(14, 24, 37)), CASING, HOT, Direction.NORTH));
    }

    @Test
    void reliableContextKeepsOneSnapshotWhenPublicationChangesDuringTheBuild() {
        // 会捕获邻接查询偷偷重capture，从同一次模型重建中混入另一版本的错误。
        ClientLevel level = identityOnlyLevel();
        var old = snapshot(member(new BlockPos(11, 21, 30), Direction.NORTH, CASING));
        var next = snapshot(member(new BlockPos(12, 21, 30), Direction.NORTH, HOT));
        ReactorSurfaceSnapshots.publish(level, old);
        ReactorConnectedTextures.withCapturedSnapshot(level, () -> {
            assertSame(old, ReactorConnectedTextures.currentSnapshot(level));
            ReactorSurfaceSnapshots.publish(level, next);
            assertSame(old, ReactorConnectedTextures.currentSnapshot(level));
            return null;
        });
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(level));
    }

    @Test
    void nestedBuildRestoresOuterSnapshotAndCannotUseAnotherContext() {
        // 会捕获嵌套覆盖、错误世界上下文套用外层快照或缺少finally恢复。
        ClientLevel outer = identityOnlyLevel();
        ClientLevel inner = identityOnlyLevel();
        var a = snapshot(member(new BlockPos(11, 21, 30), Direction.NORTH, CASING));
        var b = snapshot(member(new BlockPos(12, 21, 30), Direction.NORTH, HOT));
        ReactorSurfaceSnapshots.publish(outer, a);
        ReactorConnectedTextures.withCapturedSnapshot(outer, () -> {
            assertSame(a, ReactorConnectedTextures.currentSnapshot(outer));
            assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(inner));
            ReactorSurfaceSnapshots.publish(inner, b);
            assertThrows(IllegalStateException.class, () -> ReactorConnectedTextures.withCapturedSnapshot(inner, () -> {
                assertSame(b, ReactorConnectedTextures.currentSnapshot(inner));
                throw new IllegalStateException("测试嵌套中断");
            }));
            assertSame(a, ReactorConnectedTextures.currentSnapshot(outer));
            return null;
        });
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(outer));
    }

    @Test
    void unknownOldAndUnscopedContextsNeverBorrowGlobalSnapshot() {
        // 会捕获消费者无作用域时使用诊断capture()，或忽略上下文适用性的错误。
        ClientLevel current = identityOnlyLevel();
        ClientLevel old = identityOnlyLevel();
        var known = snapshot(member(new BlockPos(11, 21, 30), Direction.NORTH, CASING));
        ReactorSurfaceSnapshots.publish(current, known);
        BlockAndTintGetter unknown = blockContext(false);
        for (BlockAndTintGetter context : List.of(unknown, old)) {
            ReactorConnectedTextures.withCapturedSnapshot(context, () -> {
                assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(context));
                return null;
            });
        }
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(current));
        ReactorConnectedTextureBehaviour behaviour = new ReactorConnectedTextureBehaviour(id -> null);
        assertNull(behaviour.getDataType(unknown, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), Direction.NORTH));
    }

    @Test
    void registrationEmitsFourteenSpritePairsAndSevenBlockFactoriesOnlyOnce() {
        // 注册边界的键若误加block/，Create会静默跳过；这里运行真实幂等登记器。
        var registration = new ReactorConnectedTextures.Registration();
        Map<ResourceLocation, ResourceLocation> emitted = new HashMap<>();
        Map<ResourceLocation, NonNullFunction<BakedModel, ? extends BakedModel>> factories = new HashMap<>();
        var shifts = new ArrayList<CTSpriteShiftEntry>();
        java.util.function.BiFunction<ResourceLocation, ResourceLocation, CTSpriteShiftEntry> shiftFactory = (original, target) -> {
            assertNull(emitted.put(original, target), "重复sprite登记");
            CTSpriteShiftEntry entry = new CTSpriteShiftEntry(AllCTTypes.RECTANGLE);
            shifts.add(entry);
            return entry;
        };
        java.util.function.BiConsumer<ResourceLocation, NonNullFunction<BakedModel, ? extends BakedModel>> registrar = (key, factory) ->
                assertNull(factories.put(key, factory), "重复block包装登记");
        registration.initialize(shiftFactory, registrar);
        registration.initialize(shiftFactory, registrar);
        assertEquals(Set.of(id("reactor_casing"), id("reactor_window"), id("reactor_instrument_port"),
                id("reactor_cold_port"), id("reactor_hot_port"), id("reactor_refueling_port"), id("control_rod_drive")), factories.keySet());
        List<String> names = List.of("reactor_casing_side", "reactor_casing_top", "reactor_casing_bottom", "reactor_hot_port_side",
                "reactor_hot_port_top", "reactor_cold_port_side", "reactor_cold_port_top", "reactor_window", "reactor_instrument_port_side",
                "reactor_instrument_port_top", "reactor_refueling_port_side", "reactor_refueling_port_top", "control_rod_drive_side", "control_rod_drive_top");
        assertEquals(14, emitted.size());
        for (String name : names) assertEquals(id("block/reactor_ct/" + name), emitted.get(id("block/" + name)));
        assertEquals(14, shifts.size());
        BakedModel original = new PlainModel();
        for (var factory : factories.values()) assertNotSame(original, factory.apply(original));
        assertNull(registration.shiftForSprite(id("block/unrelated")));
    }

    @Test
    void quadSpriteSelectsItsOwnShiftRegardlessOfBlockIdentity() {
        // 真实quad sprite不同于block ID；按block猜图会替换错误面或丢掉顶底。
        CTSpriteShiftEntry top = new CTSpriteShiftEntry(AllCTTypes.RECTANGLE);
        CTSpriteShiftEntry side = new CTSpriteShiftEntry(AllCTTypes.RECTANGLE);
        Map<ResourceLocation, CTSpriteShiftEntry> entries = Map.of(id("block/reactor_casing_top"), top, id("block/reactor_casing_side"), side);
        var behaviour = new ReactorConnectedTextureBehaviour(entries::get);
        try (SpriteContents topContents = spriteContents(id("block/reactor_casing_top"));
             SpriteContents sideContents = spriteContents(id("block/reactor_casing_side"));
             SpriteContents unknownContents = spriteContents(id("block/unknown"))) {
            assertSame(top, behaviour.getShift(Blocks.STONE.defaultBlockState(), Direction.UP, new TestSprite(topContents)));
            assertSame(side, behaviour.getShift(Blocks.STONE.defaultBlockState(), Direction.NORTH, new TestSprite(sideContents)));
            assertNull(behaviour.getShift(Blocks.STONE.defaultBlockState(), Direction.UP, new TestSprite(unknownContents)));
        }
    }

    @Test
    void wrappedModelKeepsOriginalQuadForUnknownContextAndPropagatesGatherFailure() {
        // 运行实际final getModelData，防止包装器遗漏gather作用域或回退仍替换原quad。
        PlainModel original = new PlainModel();
        var model = ReactorConnectedTextures.wrapModel(original, new ReactorConnectedTextureBehaviour(id -> null));
        BlockAndTintGetter unknown = blockContext(false);
        ModelData data = model.getModelData(unknown, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), ModelData.EMPTY);
        assertSame(original.quad, model.getQuads(Blocks.AIR.defaultBlockState(), Direction.NORTH, RandomSource.create(1), data, null).getFirst());
        assertThrows(IllegalStateException.class, () -> model.getModelData(blockContext(true), BlockPos.ZERO,
                Blocks.AIR.defaultBlockState(), ModelData.EMPTY));
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(unknown));
    }

    @Test
    void nativeContextAndActualModelGatherUseTheCapturedPublication() {
        // 此用例贯穿真实六参connectsTo/buildContext与final getModelData，而非复制索引算法。
        BlockState casing = registeredState("reactor_casing");
        BlockState hot = registeredState("reactor_hot_port");
        BlockPos from = new BlockPos(11, 21, 30);
        BlockPos to = new BlockPos(12, 21, 30);
        var a = member(from, Direction.NORTH, CASING);
        var b = member(to, Direction.NORTH, HOT);
        var published = new ReactorSurfaceSnapshot(Map.of(from, a, to, b));
        FixtureLevel level = readableLevel(pos -> pos.equals(from) ? casing : pos.equals(to) ? hot : Blocks.AIR.defaultBlockState());
        var behaviour = new ReactorConnectedTextureBehaviour(id -> null);
        ReactorSurfaceSnapshots.publish(level, published);
        assertNull(behaviour.getDataType(level, from, casing, Direction.NORTH));
        assertFalse(behaviour.connectsTo(casing, hot, level, from, to, Direction.NORTH));
        ReactorConnectedTextures.withCapturedSnapshot(level, () -> {
            assertSame(AllCTTypes.RECTANGLE, behaviour.getDataType(level, from, casing, Direction.NORTH));
            assertNull(behaviour.getDataType(level, from, Blocks.STONE.defaultBlockState(), Direction.NORTH));
            assertNull(behaviour.getDataType(level, from, casing, Direction.SOUTH));
            assertTrue(behaviour.connectsTo(casing, hot, level, from, to, Direction.NORTH));
            assertFalse(behaviour.connectsTo(casing, Blocks.STONE.defaultBlockState(), level, from, to, Direction.NORTH));
            var context = behaviour.buildContext(level, from, casing, Direction.NORTH, AllCTTypes.RECTANGLE.getContextRequirement());
            // 锁定Create的北面原生右方向为西，因此东侧相邻格属于left；不复制索引算法。
            assertFalse(context.right);
            assertTrue(context.left);
            level.reader = pos -> pos.equals(from) ? casing : Blocks.AIR.defaultBlockState();
            assertFalse(behaviour.connectsTo(casing, hot, level, from, to, Direction.NORTH));
            return null;
        });
        // 第一次局部读取撤销全局发布；整个super仍只能看到进入gather时捕获的原版本。
        int[] reads = {0};
        level.reader = pos -> {
            assertSame(published, ReactorConnectedTextures.currentSnapshot(level));
            if (reads[0]++ == 0) ReactorSurfaceSnapshots.publish(level, ReactorSurfaceSnapshot.empty());
            return pos.equals(from) ? casing : pos.equals(to) ? hot : Blocks.AIR.defaultBlockState();
        };
        var original = new PlainModel();
        var model = ReactorConnectedTextures.wrapModel(original, behaviour);
        ModelData data = model.getModelData(level, from, casing, ModelData.EMPTY);
        assertTrue(reads[0] > 1, "实际执行super的多面与邻接读取");
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(level));
        assertSame(original.quad, model.getQuads(casing, Direction.NORTH, RandomSource.create(1), data, null).getFirst());
        level.reader = pos -> { throw new IllegalStateException("测试可靠世界局部读中断"); };
        ReactorSurfaceSnapshots.publish(level, published);
        assertThrows(IllegalStateException.class, () -> model.getModelData(level, from, casing, ModelData.EMPTY));
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(level));
    }

    private record FacePair(Direction face, BlockPos from, BlockPos to) { }

    @Test
    void windowDomainIsDirectedAndSixPlanesAllowOnlyStrictTangentialDiagonals() {
        var from = member(new BlockPos(11, 21, 30), Direction.NORTH, WINDOW);
        var east = member(new BlockPos(12, 21, 30), Direction.NORTH, CASING);
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(from, east, WINDOW, CASING, Direction.NORTH));
        assertTrue(ReactorConnectedTextureBehaviour.canConnect(east, from, CASING, WINDOW, Direction.NORTH));
        List<FacePair> planes = List.of(
                new FacePair(Direction.NORTH, new BlockPos(11,21,30), new BlockPos(12,22,30)),
                new FacePair(Direction.SOUTH, new BlockPos(11,21,37), new BlockPos(12,22,37)),
                new FacePair(Direction.WEST, new BlockPos(10,21,31), new BlockPos(10,22,32)),
                new FacePair(Direction.EAST, new BlockPos(15,21,31), new BlockPos(15,22,32)),
                new FacePair(Direction.DOWN, new BlockPos(11,20,31), new BlockPos(12,20,32)),
                new FacePair(Direction.UP, new BlockPos(11,24,31), new BlockPos(12,24,32)));
        for (FacePair pair : planes) {
            var a=member(pair.from(),pair.face(),WINDOW);var b=member(pair.to(),pair.face(),WINDOW);
            assertTrue(ReactorConnectedTextureBehaviour.canConnect(a,b,WINDOW,WINDOW,pair.face()),pair.face().name());
            assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,b,WINDOW,"minecraft:air",pair.face()));
            assertFalse(ReactorConnectedTextureBehaviour.canConnect(a,
                    changed(b,b.dimension(),b.ownerPos(),UUID.randomUUID(),b.origin(),b.maxInclusive()),WINDOW,WINDOW,pair.face()));
        }
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(from,member(new BlockPos(13,21,30),Direction.NORTH,WINDOW),WINDOW,WINDOW,Direction.NORTH));
        assertFalse(ReactorConnectedTextureBehaviour.canConnect(from,member(new BlockPos(12,21,31),Direction.NORTH,WINDOW),WINDOW,WINDOW,Direction.NORTH));
    }

    @Test
    void nativeWindowContextKeepsRealConcaveCornersAndDoesNotJoinInstrumentOrOnlyCorner() {
        BlockPos center=new BlockPos(12,22,30);BlockState window=registeredState("reactor_window");
        var behaviour=new ReactorConnectedTextureBehaviour(id->null);
        for (int[] scenario : List.of(new int[]{255,54},new int[]{223,53},new int[]{9,17},new int[]{41,20},new int[]{32,0})) {
            var states=new HashMap<BlockPos,BlockState>();var members=new HashMap<BlockPos,ReactorSurfaceSnapshot.Member>();
            states.put(center,window);members.put(center,member(center,Direction.NORTH,WINDOW));
            int[][] offsets={{0,1},{0,-1},{1,0},{-1,0},{1,1},{-1,1},{1,-1},{-1,-1}};
            for(int bit=0;bit<8;bit++) if((scenario[0]&(1<<bit))!=0) {
                BlockPos pos=center.offset(offsets[bit][0],offsets[bit][1],0);
                states.put(pos,window);members.put(pos,member(pos,Direction.NORTH,WINDOW));
            }
            FixtureLevel level=readableLevel(pos->states.getOrDefault(pos,Blocks.AIR.defaultBlockState()));
            ReactorSurfaceSnapshots.publish(level,new ReactorSurfaceSnapshot(members));
            ReactorConnectedTextures.withCapturedSnapshot(level,()->{
                assertSame(AllCTTypes.OMNIDIRECTIONAL,behaviour.getDataType(level,center,window,Direction.NORTH));
                CTContext context=behaviour.buildContext(level,center,window,Direction.NORTH,AllCTTypes.OMNIDIRECTIONAL.getContextRequirement());
                assertEquals(scenario[1],AllCTTypes.OMNIDIRECTIONAL.getTextureIndex(context));
                return null;
            });
        }
        // 仪表打断右边时，右上即使有窗也不能通过原生双边门控。
        BlockPos right=center.west();BlockPos up=center.above();BlockPos diagonal=right.above();
        BlockState instrument=registeredState("reactor_instrument_port");
        Map<BlockPos,BlockState> states=Map.of(center,window,right,instrument,up,window,diagonal,window);
        FixtureLevel level=readableLevel(pos->states.getOrDefault(pos,Blocks.AIR.defaultBlockState()));
        ReactorSurfaceSnapshots.publish(level,new ReactorSurfaceSnapshot(Map.of(center,member(center,Direction.NORTH,WINDOW),
                right,member(right,Direction.NORTH,"create_nuclear_industry:reactor_instrument_port"),up,member(up,Direction.NORTH,WINDOW),
                diagonal,member(diagonal,Direction.NORTH,WINDOW))));
        ReactorConnectedTextures.withCapturedSnapshot(level,()->{
            var context=behaviour.buildContext(level,center,window,Direction.NORTH,AllCTTypes.OMNIDIRECTIONAL.getContextRequirement());
            assertFalse(context.right);assertFalse(context.topRight);assertTrue(context.up);return null;
        });
    }

    @Test
    void actualNativeFortySevenIndicesAreRecordedForOfflineSvgMapping() throws Exception {
        // 只归一化角门控输入；索引完全调用锁定Create，禁止实现第二套UV公式。
        Map<Integer,Integer> canonicalToIndex=new TreeMap<>();
        for(int raw=0;raw<256;raw++) {
            CTContext c=new CTContext();c.up=(raw&1)!=0;c.down=(raw&2)!=0;c.left=(raw&4)!=0;c.right=(raw&8)!=0;
            c.topLeft=c.up&&c.left&&(raw&16)!=0;c.topRight=c.up&&c.right&&(raw&32)!=0;
            c.bottomLeft=c.down&&c.left&&(raw&64)!=0;c.bottomRight=c.down&&c.right&&(raw&128)!=0;
            int mask=(c.up?1:0)|(c.down?2:0)|(c.left?4:0)|(c.right?8:0)|(c.topLeft?16:0)|(c.topRight?32:0)|(c.bottomLeft?64:0)|(c.bottomRight?128:0);
            canonicalToIndex.put(mask,AllCTTypes.OMNIDIRECTIONAL.getTextureIndex(c));
        }
        assertEquals(47,canonicalToIndex.size());assertEquals(47,Set.copyOf(canonicalToIndex.values()).size());
        assertEquals(0,canonicalToIndex.get(0));assertEquals(54,canonicalToIndex.get(255));assertEquals(53,canonicalToIndex.get(223));
        assertEquals(17,canonicalToIndex.get(9));assertEquals(20,canonicalToIndex.get(41));
        String json="[\n"+canonicalToIndex.entrySet().stream().map(e->"  {\"mask\":"+e.getKey()+",\"index\":"+e.getValue()+"}")
                .collect(java.util.stream.Collectors.joining(",\n"))+"\n]\n";
        Files.writeString(Path.of("build/reports/art/ART-REACTOR-02R1/native-window-contexts.json"),json);
        // 实际离线交付表必须与本次Create输出相等，不能仅让两个离线公式互证。
        var mapping=com.google.gson.JsonParser.parseString(Files.readString(Path.of("tools/art-assets/reactor-ct-r1/mapping.json"))).getAsJsonObject();
        Map<Integer,Integer> delivered=new TreeMap<>();
        for(var element:mapping.getAsJsonArray("window_contexts")) {
            var pair=element.getAsJsonObject();
            assertNull(delivered.put(pair.get("mask").getAsInt(),pair.get("index").getAsInt()),"mapping不允许重复canonical");
        }
        assertEquals(canonicalToIndex,delivered);
        Set<Integer> fillers=new java.util.HashSet<>();
        for(var element:mapping.getAsJsonArray("window_fallback_indices"))assertTrue(fillers.add(element.getAsInt()));
        assertEquals(17,fillers.size());
        for(int i=0;i<64;i++)assertEquals(!canonicalToIndex.containsValue(i),fillers.contains(i));
    }

    @Test
    void windowEntryUsesSameTypeAsReliableGetDataType() {
        // 调用initializeOnce实际使用的Create工厂，而非在注入工厂里伪造正确type。
        var entry=ReactorConnectedTextures.createShift(id("block/reactor_window"),id("block/reactor_ct/reactor_window"));
        assertSame(AllCTTypes.OMNIDIRECTIONAL,entry.getType());
        BlockState window=registeredState("reactor_window");BlockPos pos=new BlockPos(12,22,30);
        FixtureLevel level=readableLevel(p->p.equals(pos)?window:Blocks.AIR.defaultBlockState());
        ReactorSurfaceSnapshots.publish(level,snapshot(member(pos,Direction.NORTH,WINDOW)));
        var behaviour=new ReactorConnectedTextureBehaviour(id->entry);
        ReactorConnectedTextures.withCapturedSnapshot(level,()->{
            assertSame(entry.getType(),behaviour.getDataType(level,pos,window,Direction.NORTH));return null;
        });
        var side=ReactorConnectedTextures.createShift(id("block/reactor_casing_side"),id("block/reactor_ct/reactor_casing_side"));
        assertSame(AllCTTypes.RECTANGLE,side.getType());
    }

    @Test
    void sharedWindowInterfacesAreMaskedOnBothSidesAndModelDataNeverHidesOuterOrUnknownFaces() {
        BlockState window=registeredState("reactor_window");BlockPos a=new BlockPos(12,22,30);BlockPos b=a.east();
        Map<BlockPos,BlockState> states=Map.of(a,window,b,window);
        FixtureLevel level=readableLevel(pos->states.getOrDefault(pos,Blocks.AIR.defaultBlockState()));
        var ma=member(a,Direction.NORTH,WINDOW);var mb=member(b,Direction.NORTH,WINDOW);
        ReactorSurfaceSnapshots.publish(level,new ReactorSurfaceSnapshot(Map.of(a,ma,b,mb)));
        SixFaceModel original=new SixFaceModel();var model=ReactorConnectedTextures.wrapModel(original,new ReactorConnectedTextureBehaviour(id->null));
        ModelData da=model.getModelData(level,a,window,ModelData.EMPTY);ModelData db=model.getModelData(level,b,window,ModelData.EMPTY);
        level.reader=pos->{throw new AssertionError("getQuads禁止再次读取世界");};
        assertTrue(model.getQuads(window,Direction.EAST,RandomSource.create(1),da,null).isEmpty());
        assertTrue(model.getQuads(window,Direction.WEST,RandomSource.create(1),db,null).isEmpty());
        assertEquals(1,model.getQuads(window,Direction.NORTH,RandomSource.create(1),da,null).size());
        assertEquals(1,model.getQuads(window,Direction.SOUTH,RandomSource.create(1),da,null).size());
        assertEquals(1,model.getQuads(window,Direction.WEST,RandomSource.create(1),da,null).size());
        var filtered=model.getQuads(window,null,RandomSource.create(1),da,null);
        assertEquals(5,filtered.size());assertFalse(filtered.stream().anyMatch(q->q.getDirection()==Direction.EAST));
        assertEquals(6,original.quads.size());
        assertEquals(6,model.getQuads(window,null,RandomSource.create(1),ModelData.EMPTY,null).size());
        ModelData unknown=model.getModelData(blockContext(false),a,window,da);
        assertEquals(6,model.getQuads(window,null,RandomSource.create(1),unknown,null).size());
        level.reader=pos->states.getOrDefault(pos,Blocks.AIR.defaultBlockState());
        ReactorSurfaceSnapshots.publish(level,new ReactorSurfaceSnapshot(Map.of(a,ma,b,
                changed(mb,mb.dimension(),new BlockPos(15,21,31),mb.ownerGeneration(),mb.origin(),mb.maxInclusive()))));
        ModelData differentOwner=model.getModelData(level,a,window,da);
        assertEquals(6,model.getQuads(window,null,RandomSource.create(1),differentOwner,null).size());
        BlockState casing=registeredState("reactor_casing");level.reader=pos->pos.equals(a)?casing:window;
        ReactorSurfaceSnapshots.publish(level,new ReactorSurfaceSnapshot(Map.of(a,member(a,Direction.NORTH,CASING),b,mb)));
        ModelData nonWindow=model.getModelData(level,a,casing,da);
        assertEquals(6,model.getQuads(casing,null,RandomSource.create(1),nonWindow,null).size());
    }

    @Test
    void offlineWindowScenesRecordActualGatherAndQuadFilterRatherThanManualHiddenFaces() throws Exception {
        BlockState window=registeredState("reactor_window");BlockState instrument=registeredState("reactor_instrument_port");
        var behaviour=new ReactorConnectedTextureBehaviour(id->null);
        String[][] scenes={{"two_by_two","WW","WW"},{"row","WWW"},{"column","W","W","W"},
                {"ell","WW","W."},{"instrument_hole","WWW","WIW","WWW"},
                {"owners","WWWW","WWWW"},{"complete","WWW","WWW","WWW"}};
        var output=new ArrayList<String>();
        for(String[] scene:scenes) {
            var states=new HashMap<BlockPos,BlockState>();var members=new HashMap<BlockPos,ReactorSurfaceSnapshot.Member>();
            int w=scene[1].length(),h=scene.length-1;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                char material=scene[y+1].charAt(x);if(material=='.')continue;
                BlockPos pos=new BlockPos(14-x,23-y,30);boolean isWindow=material=='W';
                states.put(pos,isWindow?window:instrument);
                var m=member(pos,Direction.NORTH,isWindow?WINDOW:"create_nuclear_industry:reactor_instrument_port");
                if(scene[0].equals("owners")&&x>=2)m=changed(m,m.dimension(),new BlockPos(15,21,31),m.ownerGeneration(),m.origin(),m.maxInclusive());
                members.put(pos,m);
            }
            FixtureLevel level=readableLevel(pos->states.getOrDefault(pos,Blocks.AIR.defaultBlockState()));
            ReactorSurfaceSnapshots.publish(level,new ReactorSurfaceSnapshot(members));
            var model=ReactorConnectedTextures.wrapModel(new SixFaceModel(),behaviour);var cells=new ArrayList<String>();
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                BlockPos pos=new BlockPos(14-x,23-y,30);BlockState state=states.get(pos);if(state==null)continue;
                ModelData data=model.getModelData(level,pos,state,ModelData.EMPTY);
                List<Direction> visible=model.getQuads(state,null,RandomSource.create(1),data,null).stream().map(BakedQuad::getDirection).toList();
                int index=ReactorConnectedTextures.withCapturedSnapshot(level,()->{
                    var type=behaviour.getDataType(level,pos,state,Direction.NORTH);
                    return type.getTextureIndex(behaviour.buildContext(level,pos,state,Direction.NORTH,type.getContextRequirement()));
                });
                if(scene[0].equals("two_by_two")&&x==0&&y==0)assertFalse(visible.contains(Direction.WEST));
                String dirs=visible.stream().map(d->"\""+d.name()+"\"").collect(java.util.stream.Collectors.joining(","));
                cells.add("{\"x\":"+x+",\"y\":"+y+",\"material\":\""+scene[y+1].charAt(x)+"\",\"index\":"+index+",\"visible\":["+dirs+"]}");
            }
            output.add("{\"name\":\""+scene[0]+"\",\"width\":"+w+",\"height\":"+h+",\"cells\":["+String.join(",",cells)+"]}");
        }
        Files.writeString(Path.of("build/reports/art/ART-REACTOR-02R1/native-window-scenes.json"),"[\n"+String.join(",\n",output)+"\n]\n");
    }

    /** 六面原模型夹具用于真实mask消费，输入列表不可变；非测试生产代码执行全部过滤。 */
    private static final class SixFaceModel implements BakedModel {
        private final List<BakedQuad> quads=java.util.Arrays.stream(Direction.values()).map(d->new BakedQuad(new int[32],-1,d,null,true)).toList();
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random) {return side==null?quads:quads.stream().filter(q->q.getDirection()==side).toList();}
        @Override public boolean useAmbientOcclusion(){return false;}
        @Override public boolean isGui3d(){return true;}
        @Override public boolean usesBlockLight(){return true;}
        @Override public boolean isCustomRenderer(){return false;}
        @Override public TextureAtlasSprite getParticleIcon(){return null;}
        @Override public ItemOverrides getOverrides(){return ItemOverrides.EMPTY;}
    }

    private static ReactorSurfaceSnapshot.Member member(BlockPos pos, Direction face, String material) {
        return new ReactorSurfaceSnapshot.Member("minecraft:overworld", OWNER, GENERATION, 4,
                ORIGIN, MAX, material, pos, Set.of(face));
    }

    private static ReactorSurfaceSnapshot.Member changed(ReactorSurfaceSnapshot.Member member, String dimension,
                                                         BlockPos owner, UUID generation, BlockPos origin, BlockPos max) {
        return new ReactorSurfaceSnapshot.Member(dimension, owner, generation, member.revision(), origin, max,
                member.expectedBlockId(), member.pos(), member.outwardFaces());
    }

    private static ReactorSurfaceSnapshot snapshot(ReactorSurfaceSnapshot.Member member) {
        return new ReactorSurfaceSnapshot(Map.of(member.pos(), member));
    }

    /** 只创建世界身份，不调用其未初始化区块/网络字段，也不启动Minecraft客户端。 */
    private static ClientLevel identityOnlyLevel() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (ClientLevel) ((Unsafe) field.get(null)).allocateInstance(ClientLevel.class);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法构造身份用ClientLevel", exception);
        }
    }

    /** 只替代外部世界读取：正常场景为空气，异常场景模拟局部读失败；不会伪造快照。 */
    private static BlockAndTintGetter blockContext(boolean failReads) {
        return (BlockAndTintGetter) Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(),
                new Class<?>[]{BlockAndTintGetter.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getBlockState")) {
                        if (failReads) throw new IllegalStateException("测试局部读中断");
                        return Blocks.AIR.defaultBlockState();
                    }
                    if (method.getName().equals("getHeight")) return 384;
                    if (method.getName().equals("getMinBuildHeight")) return -64;
                    if (method.getName().equals("getFluidState")) return Blocks.AIR.defaultBlockState().getFluidState();
                    throw new AssertionError("消费路径不应读取：" + method.getName());
                });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", path);
    }

    private static BlockState registeredState(String name) {
        var block = BuiltInRegistries.BLOCK.get(id(name));
        assertEquals(id(name), BuiltInRegistries.BLOCK.getKey(block), "测试使用实际已注册的合法反应堆材质");
        return block.defaultBlockState();
    }

    /** 替代区块读取的真实ClientLevel子类；不创建世界，只由测试给出有限局部方块。 */
    private static final class FixtureLevel extends ClientLevel {
        private java.util.function.Function<BlockPos, BlockState> reader;

        private FixtureLevel() {
            super(null, null, null, null, 0, 0, null, null, false, 0L);
            throw new AssertionError("只允许Unsafe分配测试外部世界边界");
        }

        @Override public BlockState getBlockState(BlockPos pos) { return reader.apply(pos); }
    }

    private static FixtureLevel readableLevel(java.util.function.Function<BlockPos, BlockState> reader) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            FixtureLevel level = (FixtureLevel) ((Unsafe) field.get(null)).allocateInstance(FixtureLevel.class);
            level.reader = reader;
            return level;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法构造局部读取用ClientLevel", exception);
        }
    }

    private static SpriteContents spriteContents(ResourceLocation name) {
        return new SpriteContents(name, new FrameSize(16, 16), new NativeImage(16, 16, false), ResourceMetadata.EMPTY);
    }

    private static final class TestSprite extends TextureAtlasSprite {
        private TestSprite(SpriteContents contents) {
            super(ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"), contents, 16, 16, 0, 0);
        }
    }

    /** 外部原模型的最小完整BakedModel接口，CT包装和quad保留行为由生产代码执行。 */
    private static final class PlainModel implements BakedModel {
        private final BakedQuad quad = new BakedQuad(new int[32], -1, Direction.NORTH, null, true);
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return List.of(quad); }
        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return true; }
        @Override public boolean usesBlockLight() { return true; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return null; }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }
}
