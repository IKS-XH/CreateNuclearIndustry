package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 验证青金石粉的客户端资源和服务端标签文件合同；真实注册与标签合并另由 GameTest 验证。 */
class LapisDustDataContractTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final String ASSETS = "assets/create_nuclear_industry/";
    private static final String ITEM_ID = "create_nuclear_industry:lapis_dust";
    private static final String LANGUAGE_KEY = "item.create_nuclear_industry.lapis_dust";

    @Test
    void generatedModelResolvesToDedicatedReadableTransparentTexture() throws IOException {
        JsonObject model = readJson(ASSETS + "models/item/lapis_dust.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        String texture = model.getAsJsonObject("textures").get("layer0").getAsString();
        assertEquals("create_nuclear_industry:item/lapis_dust", texture);
        String[] location = texture.split(":", 2);
        Path png = RESOURCES.resolve("assets/" + location[0] + "/textures/" + location[1] + ".png");
        assertTrue(Files.isRegularFile(png), "模型引用的专用纹理必须存在");
        var image = ImageIO.read(png.toFile());
        assertNotNull(image, "纹理必须是可解码 PNG");
        assertTrue(image.getWidth() > 0, "纹理必须具有有效像素尺寸");
        assertEquals(image.getWidth(), image.getHeight(), "静态物品纹理必须为正方形");
        assertTrue(image.getColorModel().hasAlpha(), "粉末轮廓外必须支持透明背景");
        int transparent = 0;
        int visible = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha == 0) transparent++;
                if (alpha > 0) visible++;
            }
        }
        assertTrue(transparent > 0, "背景不能全部不透明");
        assertTrue(visible > 0, "纹理不能为空白");
    }

    @Test
    void bothLocalesNameTheSameFormalItem() throws IOException {
        assertTrue(readJson(ASSETS + "lang/zh_cn.json").has(LANGUAGE_KEY), "缺少中文物品名称");
        assertTrue(readJson(ASSETS + "lang/en_us.json").has(LANGUAGE_KEY), "缺少英文物品名称");
        assertEquals("青金石粉", readJson(ASSETS + "lang/zh_cn.json").get(LANGUAGE_KEY).getAsString());
        assertEquals("Lapis Dust", readJson(ASSETS + "lang/en_us.json").get(LANGUAGE_KEY).getAsString());
    }

    @Test
    void specificDustTagOnlyAppendsLapisDustAndParentReferencesThatTag() throws IOException {
        JsonObject specific = readJson("data/c/tags/item/dusts/lapis.json");
        JsonObject parent = readJson("data/c/tags/item/dusts.json");
        assertFalse(specific.get("replace").getAsBoolean());
        assertFalse(parent.get("replace").getAsBoolean());
        assertEquals(List.of(ITEM_ID), specific.getAsJsonArray("values").asList().stream()
                .map(value -> value.getAsString()).toList());
        assertEquals(List.of("#c:dusts/lapis"), parent.getAsJsonArray("values").asList().stream()
                .map(value -> value.getAsString()).toList());
    }

    @Test
    void lapisDustHasNoRecipeInAnyBundledNamespace() throws IOException {
        try (var files = Files.walk(RESOURCES.resolve("data"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                if (file.toString().replace('\\', '/').contains("/recipe/")) {
                    assertFalse(Files.readString(file).contains("lapis_dust"),
                            "本卡不允许增加制粉或以青金石粉为输入的配方：" + file);
                }
            }
        }
    }

    private static JsonObject readJson(String relativePath) throws IOException {
        Path path = RESOURCES.resolve(relativePath);
        assertTrue(Files.isRegularFile(path), "缺失资源：" + relativePath);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
