package com.iksxh.create_nuclear_industry.structure.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 使用锁定原生BufferSource的真实切换与空批次刷新；不调用GPU、不复制调度算法。 */
class ReactorInternalRenderBuffersTest {
    static ResourceLocation texture(int slot) {
        return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry","dynamic/reactor_coolant/slot_"+slot);
    }
    static RenderType liquid(int slot) { return RenderType.entityTranslucent(texture(slot)); }
    /** 仅观察原生startedBuilders并记录定向结束；父类执行全部实际调度。 */
    static final class NativeBuffers extends MultiBufferSource.BufferSource implements AutoCloseable {
        final List<RenderType> ended=new ArrayList<>();
        NativeBuffers(SequencedMap<RenderType,ByteBufferBuilder> fixed) { super(new ByteBufferBuilder(1536),fixed); }
        boolean active(RenderType type) { return startedBuilders.containsKey(type); }
        SequencedMap<RenderType,ByteBufferBuilder> fixed() { return fixedBuffers; }
        @Override public void endBatch(RenderType type) {
            if(active(type))ended.add(type);
            super.endBatch(type);
        }
        @Override public void close() { endBatch();sharedBuffer.close();fixedBuffers.values().forEach(ByteBufferBuilder::close); }
    }
    static NativeBuffers registered(boolean fixedRod) {
        SequencedMap<RenderType,ByteBufferBuilder> fixed=new LinkedHashMap<>();
        if(fixedRod)fixed.put(RenderType.cutoutMipped(),new ByteBufferBuilder(1536));
        ReactorInternalRenderBuffers.register(new RegisterRenderBuffersEvent(fixed));
        return new NativeBuffers(fixed);
    }
    @Test void bothBerOrdersKeepLiquidPendingUntilRodThenLiquidBoundary() {
        for(boolean liquidFirst:new boolean[]{true,false})try(var buffers=registered(false)) {
            if(liquidFirst)ReactorInternalRenderer.liquidBuffer(buffers,texture(0));
            buffers.getBuffer(RenderType.cutoutMipped());
            if(!liquidFirst)ReactorInternalRenderer.liquidBuffer(buffers,texture(0));
            assertTrue(buffers.active(liquid(0)),"液体应留到全部BER结束，不能在切换棒体时提前刷新");
            if(!liquidFirst)buffers.getBuffer(RenderType.cutoutMipped());
            assertTrue(buffers.active(liquid(0)),"反向交错同样不能刷新液体");
            assertTrue(buffers.ended.isEmpty(),"BER阶段不能提前结束棒体或液体批次");
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,buffers);
            assertEquals(List.of(RenderType.cutoutMipped(),liquid(0)),buffers.ended);
            assertFalse(buffers.active(RenderType.cutoutMipped()));assertFalse(buffers.active(liquid(0)));
        }
    }
    @Test void multipleTexturesAndFixedRodStillFlushOpaqueBeforeEveryLiquid() {
        for(boolean fixedRod:new boolean[]{false,true})try(var buffers=registered(fixedRod)) {
            ReactorInternalRenderer.liquidBuffer(buffers,texture(9));
            buffers.getBuffer(RenderType.cutoutMipped());
            ReactorInternalRenderer.liquidBuffer(buffers,texture(2));
            ReactorInternalRenderer.liquidBuffer(buffers,texture(9));
            assertTrue(buffers.active(liquid(9)));assertTrue(buffers.active(liquid(2)));
            assertTrue(buffers.active(RenderType.cutoutMipped()));assertTrue(buffers.ended.isEmpty());
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,buffers);
            assertEquals(List.of(RenderType.cutoutMipped(),liquid(2),liquid(9)),buffers.ended);
        }
    }
    @Test void onlyAfterBlockEntitiesFlushesAndUnrelatedTypesRemainPending() {
        try(var buffers=registered(true)) {
            var unrelated=RenderType.solid();buffers.getBuffer(unrelated);
            buffers.getBuffer(RenderType.cutoutMipped());ReactorInternalRenderer.liquidBuffer(buffers,texture(0));
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_ENTITIES,buffers);
            assertTrue(buffers.ended.isEmpty());assertTrue(buffers.active(unrelated));
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,buffers);
            assertEquals(List.of(RenderType.cutoutMipped(),liquid(0)),buffers.ended);assertTrue(buffers.active(unrelated));
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,buffers);
            assertEquals(2,buffers.ended.size(),"空边界刷新不能重复结束已有批次");
        }
    }
    /** 仪表最后提交辉光时，液体固定批次不能抢在原生共享辉光之前绘制。 */
    @Test void lastInstrumentGlowFlushesBeforeLiquidWithSharedOrFixedRod() {
        for(boolean fixedRod:new boolean[]{false,true})try(var buffers=registered(fixedRod)) {
            buffers.getBuffer(RenderType.cutoutMipped());
            buffers.getBuffer(RenderType.translucent());
            ReactorInternalRenderer.liquidBuffer(buffers,texture(0));
            assertTrue(buffers.active(RenderType.translucent()),"固定液体取得缓冲后，原共享辉光仍待提交");
            assertFalse(buffers.ended.contains(liquid(0)));
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,buffers);
            assertEquals(List.of(RenderType.cutoutMipped(),RenderType.translucent(),liquid(0)),buffers.ended,
                    "末尾辉光必须在液体写深度前结束，不能留给后续原生endLastBatch");
            assertFalse(buffers.active(RenderType.translucent()));
        }
    }
    /** 多owner共享原辉光层；仅结束棒体/辉光/液体，不能全量刷新仍待提交的solid。 */
    @Test void interleavedOwnersFlushEveryGlowBeforeLiquidAndKeepUnrelatedSolid() {
        for(boolean fixedRod:new boolean[]{false,true})try(var buffers=registered(fixedRod)) {
            buffers.fixed().put(RenderType.solid(),new ByteBufferBuilder(1536));
            buffers.getBuffer(RenderType.solid());
            for(int slot:new int[]{9,2}) {
                buffers.getBuffer(RenderType.cutoutMipped());buffers.getBuffer(RenderType.translucent());
                ReactorInternalRenderer.liquidBuffer(buffers,texture(slot));
            }
            assertTrue(buffers.active(RenderType.translucent()));assertTrue(buffers.active(liquid(9)));assertTrue(buffers.active(liquid(2)));
            var beforeStage=List.copyOf(buffers.ended);
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_ENTITIES,buffers);
            assertEquals(beforeStage,buffers.ended);
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,buffers);
            var expected=fixedRod?List.of(RenderType.cutoutMipped(),RenderType.translucent(),liquid(2),liquid(9))
                    :List.of(RenderType.cutoutMipped(),RenderType.translucent(),RenderType.cutoutMipped(),RenderType.translucent(),liquid(2),liquid(9));
            assertEquals(expected,buffers.ended);assertTrue(buffers.active(RenderType.solid()));
            ReactorInternalRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,buffers);
            assertEquals(expected,buffers.ended,"空边界不得重复提交，也不得结束无关solid");
        }
    }
    @Test void registrationIsExactlyBoundedAndUsesExistingNativeLayers() {
        try(var buffers=registered(false)) {
            assertEquals(256,buffers.fixed().size());long capacity=0;
            for(int slot=0;slot<256;slot++) {
                var type=liquid(slot);assertTrue(buffers.fixed().containsKey(type));capacity+=type.bufferSize();
                assertEquals(1536,type.bufferSize());assertTrue(type.sortOnUpload());
            }
            assertEquals(393216,capacity);assertFalse(buffers.fixed().containsKey(liquid(256)));
        }
    }
    /** 反射仅检查真实CompositeState身份，避免把默认深度/写入状态另抄成公式。 */
    @Test void nativeLiquidShaderDepthAndBlendRemainUnchanged() throws Exception {
        var stateField=liquid(0).getClass().getDeclaredField("state");stateField.setAccessible(true);
        Object state=stateField.get(liquid(0));
        for(var entry:Map.of("shaderState","RENDERTYPE_ENTITY_TRANSLUCENT_SHADER","depthTestState","LEQUAL_DEPTH_TEST",
                "writeMaskState","COLOR_DEPTH_WRITE","transparencyState","TRANSLUCENT_TRANSPARENCY").entrySet()) {
            Field actual=state.getClass().getDeclaredField(entry.getKey());actual.setAccessible(true);
            Field expected=RenderStateShard.class.getDeclaredField(entry.getValue());expected.setAccessible(true);
            assertSame(expected.get(null),actual.get(state));
        }
    }
}
