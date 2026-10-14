package com.iksxh.create_nuclear_industry.production;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** 注入同一生产动作JSON，验证批准分工、父升降和实际JOML/PoseStack四向矩阵。 */
final class ShieldedAssemblyAnimationTest {
    private static final Path DATA = Path.of("src/main/resources/assets/create_nuclear_industry/shielded_assembly/animation.json");
    private static ShieldedAssemblyAnimation animation() throws IOException {
        try (var reader = Files.newBufferedReader(DATA)) { return ShieldedAssemblyAnimation.read(reader); }
    }

    @Test void manufactureAlternatesArmsThenAlignsAndPressesWithFixedBases() throws IOException {
        var animation = animation();
        var left = animation.pose("manufacture", .1);
        assertEquals(14, left.groups().get("left_tool").rotation().z(), 1e-8);
        assertEquals(0, left.groups().get("right_tool").rotation().z());
        var right = animation.pose("manufacture", .36);
        assertEquals(-14, right.groups().get("right_tool").rotation().z(), 1e-8);
        assertEquals(0, right.groups().get("left_tool").rotation().z());
        var both = animation.pose("manufacture", .6);
        assertEquals(14, both.groups().get("left_tool").rotation().z(), 1e-8);
        assertEquals(-14, both.groups().get("right_tool").rotation().z(), 1e-8);
        for (double phase : new double[]{0, .1, .36, .6, .84, .99, 1}) {
            var pose = animation.pose("manufacture", phase);
            for (String base : List.of("left_base", "right_base", "fixture_base"))
                assertEquals(ShieldedAssemblyAnimation.Transform.IDLE, pose.groups().get(base));
        }
        assertEquals(1, animation.pose("manufacture", .84).groups().get("fixture_carriage").translation().y());
        assertEquals(ShieldedAssemblyAnimation.Transform.IDLE,
                animation.pose("manufacture", .84).groups().get("left_tool"));
    }

    @Test void sealingHoldsFixtureClosesBothArmsAndUsesShortLockBeforeRelease() throws IOException {
        var animation = animation();
        var pose = animation.pose("sealing", .4);
        assertEquals(1.5, pose.groups().get("left_tool").translation().x());
        assertEquals(-1.5, pose.groups().get("right_tool").translation().x());
        assertEquals(6, pose.groups().get("left_tool").rotation().y());
        assertEquals(-6, pose.groups().get("right_tool").rotation().y());
        assertEquals(.1, pose.groups().get("fixture_jaw_left").translation().x());
        assertEquals(-.1, pose.groups().get("fixture_jaw_right").translation().x());
        assertEquals(0, pose.groups().get("fixture_carriage").translation().y());
        assertEquals(-6, animation.pose("sealing", .49).groups().get("left_tool").rotation().y());
        for (String operation : List.of("manufacture", "sealing", "unknown"))
            for (double phase : new double[]{0, 1, Double.NaN})
                assertTrue(animation.pose(operation, phase).groups().values().stream()
                        .allMatch(ShieldedAssemblyAnimation.Transform.IDLE::equals));
    }

    @Test void fixtureJawsInheritCarriageExactlyOnceAndBaseRemainsFixed() throws IOException {
        var animation = animation();
        var pose = animation.pose("manufacture", .84);
        var jaw = animation.matrix(pose, "fixture_jaw_left", 0).transformPosition(new Vector3f(1, 1, 1));
        assertEquals(1.00625, jaw.x(), 1e-6);
        assertEquals(1.0625, jaw.y(), 1e-6);
        assertEquals(1, jaw.z(), 1e-6);
        var base = animation.matrix(pose, "fixture_base", 0).transformPosition(new Vector3f(1, 1, 1));
        assertEquals(new Vector3f(1, 1, 1), base);
    }

    @Test void localMotionPrecedesFacingAboutControllerRatherThanWorldOrWholeCenter() throws IOException {
        var animation = animation();
        var pose = animation.pose("sealing", .30);
        var pivot = new Vector3f(10f / 16, 20f / 16, 14.5f / 16);
        float[] angles = {0, (float) -Math.PI / 2, (float) Math.PI, (float) Math.PI / 2};
        float[][] expected = {{.71875f, 1.25f, .90625f}, {.09375f, 1.25f, .71875f},
                {.28125f, 1.25f, .09375f}, {.90625f, 1.25f, .28125f}};
        for (int index = 0; index < angles.length; index++) {
            var actual = animation.matrix(pose, "left_tool", angles[index]).transformPosition(new Vector3f(pivot));
            assertEquals(expected[index][0], actual.x(), 1e-6);
            assertEquals(expected[index][1], actual.y(), 1e-6);
            assertEquals(expected[index][2], actual.z(), 1e-6);
        }
    }

    @Test void productionDataBindsActualRigAndNativePoseStackKeepsRotationNormals() throws Exception {
        var data = JsonParser.parseString(Files.readString(DATA)).getAsJsonObject();
        var bytes = Files.readAllBytes(Path.of("tools/art-assets/shielded-assembly-animation-15c/rig.json"));
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                data.get("rigSha256").getAsString());
        var animation = animation();
        var matrix = animation.matrix(animation.pose("manufacture", .1), "left_tool", (float) -Math.PI / 2);
        var stack = new PoseStack();
        stack.mulPose(matrix);
        assertEquals(matrix, stack.last().pose());
        assertEquals(1, stack.last().normal().transform(new Vector3f(0, 1, 0)).length(), 1e-6);
    }

    @Test void smoothBoundariesHaveNoPoseJumpAndBadDataFailsForSafeIdleFallback() throws IOException {
        var animation = animation();
        double before = animation.pose("manufacture", .06 - 1e-6).groups().get("left_tool").rotation().z();
        double after = animation.pose("manufacture", .06 + 1e-6).groups().get("left_tool").rotation().z();
        assertEquals(before, after, 1e-6);
        var invalid = JsonParser.parseString(Files.readString(DATA)).getAsJsonObject();
        invalid.getAsJsonObject("operations").getAsJsonObject("manufacture").getAsJsonArray("frames")
                .get(1).getAsJsonObject().addProperty("at", 0);
        JsonObject duplicate = invalid;
        assertThrows(IllegalArgumentException.class, () -> ShieldedAssemblyAnimation.read(new StringReader(duplicate.toString())));
        invalid = JsonParser.parseString(Files.readString(DATA)).getAsJsonObject();
        invalid.getAsJsonObject("operations").getAsJsonObject("manufacture").getAsJsonArray("frames")
                .get(1).getAsJsonObject().getAsJsonObject("pose").getAsJsonObject("left_tool")
                .getAsJsonArray("rotation").set(1, new com.google.gson.JsonPrimitive(6));
        JsonObject mixed = invalid;
        assertThrows(IllegalArgumentException.class, () -> ShieldedAssemblyAnimation.read(new StringReader(mixed.toString())));
    }

    @Test void exportSmallNativePoseOracleForSameDataBrowserParity() throws Exception {
        var animation = animation();
        var cases = new ArrayList<Map<String, Object>>();
        for (Object[] example : new Object[][]{{"manufacture", .03}, {"manufacture", .1},
                {"manufacture", .36}, {"manufacture", .6}, {"manufacture", .84},
                {"sealing", .3}, {"sealing", .4}, {"sealing", .49}, {"sealing", .98}}) {
            String operation = (String) example[0];
            double phase = (double) example[1];
            var pose = animation.pose(operation, phase);
            var matrices = new LinkedHashMap<String, float[]>();
            var points = new LinkedHashMap<String, Object>();
            for (String group : ShieldedAssemblyAnimation.GROUP_NAMES) {
                matrices.put(group, animation.matrix(pose, group, 0).get(new float[16]));
                var model = JsonParser.parseString(Files.readString(Path.of(
                        "src/main/resources/assets/create_nuclear_industry/models/block/shielded_assembly_station/animation/" + group + ".json"))).getAsJsonObject();
                var corner = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("from");
                var source = new Vector3f(corner.get(0).getAsFloat(), corner.get(1).getAsFloat(), corner.get(2).getAsFloat());
                var world = animation.matrix(pose, group, 0).transformPosition(new Vector3f(source).div(16));
                points.put(group, Map.of("source", new float[]{source.x(), source.y(), source.z()},
                        "world", new float[]{world.x(), world.y(), world.z()}));
            }
            var facings = new LinkedHashMap<String, Object>();
            var leftSource = new Vector3f(8.75f, 15.5f, 12.5f);
            for (var facing : Map.of("north", 0f, "east", (float) -Math.PI / 2,
                    "south", (float) Math.PI, "west", (float) Math.PI / 2).entrySet()) {
                var matrix = animation.matrix(pose, "left_tool", facing.getValue());
                var world = matrix.transformPosition(new Vector3f(leftSource).div(16));
                facings.put(facing.getKey(), Map.of("angle", facing.getValue(), "matrix", matrix.get(new float[16]),
                        "source", new float[]{leftSource.x(), leftSource.y(), leftSource.z()},
                        "world", new float[]{world.x(), world.y(), world.z()}));
            }
            cases.add(Map.of("operation", operation, "phase", phase, "stage", pose.stage(),
                    "groups", pose.groups(), "matrices", matrices, "points", points, "facings", facings));
        }
        var target = Path.of("build/reports/art/ART-SHIELDED-ASSEMBLY-15C/client/pose-parity.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, new Gson().toJson(Map.of("animationSha256",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(DATA))),
                "cases", cases)));
        assertEquals(9, cases.size());
    }
}
