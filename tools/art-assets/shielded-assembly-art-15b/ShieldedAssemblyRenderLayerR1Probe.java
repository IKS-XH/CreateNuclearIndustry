import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.NamedRenderTypeManager;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.ElementsModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * R1离线原生层探针：真实BlockModel/ElementsModel烘焙与BakedModel层查询，
 * 不创建Minecraft客户端、窗口、GL上下文或修改资源。默认IRON仅复现未映射自有方块的solid回落；
 * PNG直接供真实sprite，所有分件仍按原几何解析。输出范围不包含实际GPU显示或用户验收。
 */
public final class ShieldedAssemblyRenderLayerR1Probe {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path RESOURCE = ROOT.resolve("src/main/resources/assets/create_nuclear_industry");
    private static final Path PARTS = RESOURCE.resolve("models/block/shielded_assembly_station/parts");

    /** 复用仓库现有原生模型夹具的sprite构造，仅替换成实际PNG；不模拟UV或层选择。 */
    private static final class ActualSprite extends TextureAtlasSprite {
        ActualSprite(ResourceLocation texture) throws Exception {
            super(ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"), contents(texture), 16, 16, 0, 0);
        }

        private static SpriteContents contents(ResourceLocation texture) throws Exception {
            Path png = RESOURCE.resolve("textures/" + texture.getPath() + ".png");
            try (var stream = Files.newInputStream(png)) {
                return new SpriteContents(texture, new FrameSize(16, 16), NativeImage.read(stream), ResourceMetadata.EMPTY);
            }
        }
    }

    /** 仅调用原生预注册方法，不发送MOD总线事件；保持锁定版本预注册命名层完全原样。 */
    private static void vanillaNamedLayers() throws Exception {
        Map<ResourceLocation, RenderTypeGroup> groups = new HashMap<>();
        var register = NamedRenderTypeManager.class.getDeclaredMethod("preRegisterVanillaRenderTypes", Map.class);
        register.setAccessible(true);
        register.invoke(null, groups);
        var field = NamedRenderTypeManager.class.getDeclaredField("RENDER_TYPES");
        field.setAccessible(true);
        field.set(null, ImmutableMap.copyOf(groups));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("仅接受R1结果文件路径");
        // 离线探针没有FML启动器；空mod清单仅满足原生FeatureFlags的读取，不发现或运行mod事件。
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        vanillaNamedLayers();
        Map<ResourceLocation, ActualSprite> sprites = new HashMap<>();
        List<Map<String, Object>> bindings = new ArrayList<>();
        int errors = 0;
        try {
            for (int part = 0; part < 8; part++) {
                for (String state : List.of("off", "on")) {
                    Path file = PARTS.resolve("part_" + part + "_" + state + ".json");
                    BlockModel model = BlockModel.fromString(Files.readString(file));
                    int windowFaces = (int) model.getElements().stream().flatMap(element -> element.faces.values().stream())
                            .filter(face -> face.texture().equals("#window")).count();
                    var baked = new ElementsModel(model.getElements()).bake(model.customData, null, material -> {
                        try {
                            ActualSprite sprite = sprites.get(material.texture());
                            if (sprite == null) {
                                sprite = new ActualSprite(material.texture());
                                sprites.put(material.texture(), sprite);
                            }
                            return sprite;
                        } catch (Exception failure) {
                            throw new IllegalStateException("实际PNG/sprite加载失败", failure);
                        }
                    }, BlockModelRotation.X0_Y0, ItemOverrides.EMPTY);
                    var layers = baked.getRenderTypes(Blocks.IRON_BLOCK.defaultBlockState(), RandomSource.create(42), ModelData.EMPTY);
                    boolean cutout = layers.contains(RenderType.cutoutMipped());
                    boolean solid = layers.contains(RenderType.solid());
                    boolean correct = windowFaces > 0 ? cutout && !solid : solid && !cutout;
                    if (!correct) errors++;
                    int quads = baked.getQuads(null, null, RandomSource.create(42)).size();
                    for (Direction side : Direction.values()) quads += baked.getQuads(null, side, RandomSource.create(42)).size();
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("target", ROOT.relativize(file).toString().replace('\\', '/'));
                    entry.put("windowFaces", windowFaces);
                    entry.put("hint", model.customData.getRenderTypeHint() == null ? null : model.customData.getRenderTypeHint().toString());
                    entry.put("cutoutMipped", cutout);
                    entry.put("solid", solid);
                    entry.put("quadCount", quads);
                    entry.put("correct", correct);
                    bindings.add(entry);
                }
            }
        } finally {
            for (ActualSprite sprite : sprites.values()) sprite.contents().close();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", "ART-SHIELDED-ASSEMBLY-15B-R1");
        result.put("passed", errors == 0);
        result.put("incorrectBindings", errors);
        result.put("bindings", bindings);
        result.put("scope", "锁定MC/NeoForge原生解析、完整元素烘焙、实际PNG sprite与IRON默认回落；无GPU或游戏验收");
        String json = new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(result);
        Path output = ROOT.resolve(args[0]);
        if (Files.exists(output)) throw new IllegalStateException("原始RED/GREEN结果已经存在，禁止覆盖");
        Files.createDirectories(output.getParent());
        Files.writeString(output, json + "\n");
        System.out.println("NATIVE_BINDINGS: models=16, incorrect=" + errors + ", passed=" + (errors == 0));
        if (errors != 0) System.exit(1);
    }
}
