package com.iksxh.create_nuclear_industry.production;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 实际Minecraft模型解析器检查已选A的正式几何、组合坐标与PNG引用；不模拟客户端转速或GPU绘制。 */
class CentrifugeArt14BModelTest {
    private static final Path MODELS = Path.of("src/main/resources/assets/create_nuclear_industry/models/block");
    private static final Path SELECTED_A = Path.of("tools/art-assets/centrifuge-concepts-14/assets/a");
    private static final Path TEXTURES = Path.of("src/main/resources/assets/create_nuclear_industry/textures/block");
    private static final List<String> PARTS = List.of("lower", "upper", "rotor", "shaft");
    private static final List<String> SLOTS = List.of("casing", "brass", "glass", "dark", "machined", "fastener");

    @Test void runtimeModelsKeepSelectedAGeometryAndRealTextureBindings() throws Exception {
        for (String part : PARTS) {
            BlockModel selected = selected(part);
            BlockModel installed = installed(part);
            assertEquals(selected.getElements().size(), installed.getElements().size(), part + "不得漏装已选A部件");
            for (int index = 0; index < selected.getElements().size(); index++)
                assertElement(selected.getElements().get(index), installed.getElements().get(index), 0);
            assertTextures(installed);
        }
        BlockElement shaft = installed("shaft").getElements().getFirst();
        assertVector(new Vector3f(6, 0, 6), shaft.from, 0, "输入轴下端必须为下格本地Y0");
        assertVector(new Vector3f(10, 8, 10), shaft.to, 0, "真实输入轴应为4×4×8，不能恢复正立方接头");
    }

    @Test void wholeItemKeepsUpperOffsetAndRotationOrigins() throws Exception {
        BlockModel item = parse(MODELS.resolve("enrichment_centrifuge_item.json"));
        assertEquals(346, item.getElements().size(), "物品必须含四部件整机，不能漏独立轴或只显示下格");
        int offset = 0;
        for (String part : PARTS) {
            List<BlockElement> elements = selected(part).getElements();
            for (int index = 0; index < elements.size(); index++)
                assertElement(elements.get(index), item.getElements().get(offset + index), part.equals("upper") ? 16 : 0);
            offset += elements.size();
        }
        assertTextures(item);
    }

    @Test void everyReferencedFormalPngDecodesAtSelectedPixelSize() throws Exception {
        for (String slot : SLOTS) {
            var image = ImageIO.read(TEXTURES.resolve("enrichment_centrifuge_" + slot + ".png").toFile());
            assertNotNull(image, slot + "必须是实际可解码PNG");
            assertEquals(16, image.getWidth(), slot + "保持原16²像素密度");
            assertEquals(16, image.getHeight(), slot + "保持原16²像素密度");
        }
    }

    private static BlockModel selected(String part) throws Exception { return parse(SELECTED_A.resolve(part + ".json")); }
    private static BlockModel installed(String part) throws Exception {
        return parse(MODELS.resolve("enrichment_centrifuge" + (part.equals("lower") ? "" : "_" + part) + ".json"));
    }
    /** 直接调用锁定1.21.1/NeoForge的BlockModel反序列化，不在测试里实现另一套JSON模型规则。 */
    private static BlockModel parse(Path path) throws Exception { return BlockModel.fromString(Files.readString(path)); }

    private static void assertTextures(BlockModel model) {
        for (String slot : SLOTS) {
            assertNotNull(model.textureMap.get(slot), "六槽应全部转换成正式资源ID：" + slot);
            var material = model.textureMap.get(slot).left().orElseThrow();
            assertEquals(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "block/enrichment_centrifuge_" + slot),
                    material.texture(), "不能残留设计稿虚拟namespace或旧四槽材质");
        }
    }

    /** 组合物品只允许upper抬升16单位；旋转origin的解析结果以格为单位，因此同步抬升一格。 */
    private static void assertElement(BlockElement expected, BlockElement actual, float yOffset) {
        assertVector(expected.from, actual.from, yOffset, "几何from保持所选A");
        assertVector(expected.to, actual.to, yOffset, "几何to保持所选A");
        assertEquals(expected.shade, actual.shade);
        if (expected.rotation == null) assertNull(actual.rotation);
        else {
            assertNotNull(actual.rotation);
            assertVector(expected.rotation.origin(), actual.rotation.origin(), yOffset / 16, "upper旋转中心应随部件整体抬升");
            assertEquals(expected.rotation.axis(), actual.rotation.axis());
            assertEquals(expected.rotation.angle(), actual.rotation.angle());
            assertEquals(expected.rotation.rescale(), actual.rotation.rescale());
        }
        assertEquals(expected.faces.keySet(), actual.faces.keySet(), "不能在转换时漏面");
        for (Direction side : expected.faces.keySet()) {
            var expectedFace = expected.faces.get(side);
            var actualFace = actual.faces.get(side);
            assertEquals(expectedFace.texture(), actualFace.texture());
            assertEquals(expectedFace.cullForDirection(), actualFace.cullForDirection());
            assertEquals(expectedFace.tintIndex(), actualFace.tintIndex());
            assertEquals(expectedFace.uv().rotation, actualFace.uv().rotation);
            assertArrayEquals(expectedFace.uv().uvs, actualFace.uv().uvs, 1e-6f, "原面UV及像素密度保持");
        }
    }

    private static void assertVector(Vector3f expected, Vector3f actual, float yOffset, String message) {
        assertEquals(expected.x(), actual.x(), 1e-6f, message);
        // Minecraft先解析原数再加16与直接解析已合并十进制值可差一个float ULP；只容纳这一级舍入误差。
        float shiftedY = expected.y() + yOffset;
        assertEquals(shiftedY, actual.y(), Math.max(1e-6f, Math.ulp(shiftedY) * 2), message);
        assertEquals(expected.z(), actual.z(), 1e-6f, message);
    }
}
