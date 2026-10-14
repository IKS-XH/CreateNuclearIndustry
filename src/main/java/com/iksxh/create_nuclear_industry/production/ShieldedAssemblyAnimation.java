package com.iksxh.create_nuclear_industry.production;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;

/** 仅客户端消费的声明式姿态算法；Reader由渲染器生命周期注入，模型像素16等于一方块。 */
public final class ShieldedAssemblyAnimation {
    public static final List<String> GROUP_NAMES = List.of("left_base", "left_tool", "right_base", "right_tool",
            "fixture_base", "fixture_carriage", "fixture_jaw_left", "fixture_jaw_right");
    /** 不可变三轴量，位移以模型像素计，角度以度计。 */
    public record Vector(double x, double y, double z) {}
    /** 先绕实际pivot旋转，再局部平移；固定组必须保持零变换。 */
    public record Transform(Vector translation, Vector rotation) {
        public static final Transform IDLE = new Transform(new Vector(0, 0, 0), new Vector(0, 0, 0));
    }
    /** 一帧完整八组姿态，stage仅描述视觉阶段，不提交生产事务。 */
    public record Pose(String stage, Map<String, Transform> groups) {}
    private record Group(Vector pivot, String parent, boolean fixed) {}
    private record Keyframe(double at, String stage, Map<String, Transform> pose) {}
    private final Map<String, Group> groups;
    private final Map<String, List<Keyframe>> operations;

    private ShieldedAssemblyAnimation(Map<String, Group> groups, Map<String, List<Keyframe>> operations) {
        this.groups = Map.copyOf(groups);
        this.operations = Map.copyOf(operations);
    }

    /** 读取已绑定rig的同源数据；坏结构/越界动作抛出异常，调用方应绘原三件安全待机。 */
    public static ShieldedAssemblyAnimation read(Reader reader) {
        try {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            require(data.get("schemaVersion").getAsInt() == 1
                    && "model_pixels_degrees".equals(data.get("units").getAsString())
                    && "smoothstep".equals(data.get("easing").getAsString())
                    && "XYZ".equals(data.get("rotationOrder").getAsString())
                    && data.get("rigSha256").getAsString().matches("[0-9a-f]{64}"), "动作数据版本/单位不符");
            require(vector(data, "facingPivot").equals(new Vector(8, 0, 8)), "整机朝向必须绕主控格");
            var groupData = data.getAsJsonObject("groups");
            require(groupData.keySet().equals(Set.copyOf(GROUP_NAMES)), "必须恰好八个批准分件");
            var groups = new LinkedHashMap<String, Group>();
            for (String name : GROUP_NAMES) {
                var entry = groupData.getAsJsonObject(name);
                Vector pivot = vector(entry, "pivot");
                require(within(pivot, new Vector(0, 0, 0), new Vector(32, 32, 32)), "pivot超出整机");
                String parent = entry.get("parent").isJsonNull() ? "" : entry.get("parent").getAsString();
                require(parent.equals(name.startsWith("fixture_jaw_") ? "fixture_carriage" : ""), "父升降关系不符");
                boolean fixed = entry.get("fixed").getAsBoolean();
                require(fixed == name.endsWith("_base"), "静止底座不能随工具活动");
                groups.put(name, new Group(pivot, parent, fixed));
            }
            var operationData = data.getAsJsonObject("operations");
            require(operationData.keySet().equals(Set.of("manufacture", "sealing")), "仅允许真实两工序");
            var operations = new LinkedHashMap<String, List<Keyframe>>();
            for (String operation : List.of("manufacture", "sealing")) {
                var entry = operationData.getAsJsonObject(operation);
                String domain = entry.get("toolDomain").getAsString();
                require(domain.equals(operation.equals("manufacture") ? "manufacture_align" : "sealing_lock"), "工具域不能混合");
                var frames = new ArrayList<Keyframe>();
                double previous = -1;
                for (var raw : entry.getAsJsonArray("frames")) {
                    var frame = raw.getAsJsonObject();
                    double at = frame.get("at").getAsDouble();
                    require(Double.isFinite(at) && at > previous && at >= 0 && at <= 1, "阶段进度必须严格递增");
                    previous = at;
                    String stage = frame.get("stage").getAsString();
                    require(!stage.isBlank(), "阶段说明不能为空");
                    var pose = idleGroups();
                    var specified = frame.getAsJsonObject("pose");
                    require(GROUP_NAMES.containsAll(specified.keySet()), "未知分件动作");
                    for (var field : specified.entrySet()) {
                        var transform = field.getValue().getAsJsonObject();
                        pose.put(field.getKey(), new Transform(vector(transform, "translation"), vector(transform, "rotation")));
                    }
                    for (String name : GROUP_NAMES) {
                        Transform transform = pose.get(name);
                        if (groups.get(name).fixed()) require(transform.equals(Transform.IDLE), "底座动作必须为零");
                        else {
                            JsonObject bounds = data.getAsJsonObject("domains")
                                    .getAsJsonObject(name.endsWith("_tool") ? domain : "fixture_press").getAsJsonObject(name);
                            require(within(transform.translation(), vector(bounds, "translationMin"), vector(bounds, "translationMax"))
                                    && within(transform.rotation(), vector(bounds, "rotationMin"), vector(bounds, "rotationMax")), "分件动作越过rig约束");
                        }
                    }
                    if (operation.equals("sealing")) require(pose.get("fixture_carriage").equals(Transform.IDLE), "封装平台必须静止");
                    require(!pressing(pose) || toolsIdle(pose), "升降与工具动作不能相交");
                    if (!frames.isEmpty()) {
                        var prior = frames.getLast().pose();
                        // 全组共用smoothstep插值；两端跨工具/升降域也会在中间产生穿模，必须拒绝。
                        require(!(pressing(prior) && !toolsIdle(pose)) && !(pressing(pose) && !toolsIdle(prior)), "相邻阶段混合升降与工具");
                    }
                    frames.add(new Keyframe(at, stage, Map.copyOf(pose)));
                }
                require(frames.size() >= 2 && frames.getFirst().at() == 0 && frames.getLast().at() == 1
                        && frames.getFirst().pose().values().stream().allMatch(Transform.IDLE::equals)
                        && frames.getLast().pose().values().stream().allMatch(Transform.IDLE::equals), "批次两端必须待机");
                operations.put(operation, List.copyOf(frames));
            }
            return new ShieldedAssemblyAnimation(groups, operations);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("屏蔽装配室动作数据无效", exception);
        }
    }

    /** 输入真实工序和0..1视觉进度；非法进度/未开工安全待机，阶段缓入缓出且批末退回。 */
    public Pose pose(String operation, double phase) {
        var frames = operations.get(operation);
        if (frames == null || !Double.isFinite(phase) || phase <= 0 || phase >= 1)
            return new Pose("待机", Map.copyOf(idleGroups()));
        int index = 0;
        while (index + 1 < frames.size() - 1 && phase >= frames.get(index + 1).at()) index++;
        Keyframe from = frames.get(index), to = frames.get(index + 1);
        double fraction = (phase - from.at()) / (to.at() - from.at());
        double eased = fraction * fraction * (3 - 2 * fraction);
        var pose = new LinkedHashMap<String, Transform>();
        for (String name : GROUP_NAMES) {
            Transform start = from.pose().get(name), end = to.pose().get(name);
            pose.put(name, new Transform(lerp(start.translation(), end.translation(), eased),
                    lerp(start.rotation(), end.rotation(), eased)));
        }
        return new Pose(from.stage(), Map.copyOf(pose));
    }

    /**
     * 输出实际方块单位矩阵F*D：局部绕pivot后位移、钳口继承父升降，再绕主控[.5,0,.5]转朝向。
     * facingAngle以弧度计，北0/东负π/2/南π/西正π/2；调用方不能再次补分件坐标。
     */
    public Matrix4f matrix(Pose pose, String name, float facingAngle) {
        Group group = groups.get(name);
        Transform transform = pose.groups().get(name);
        var matrix = new Matrix4f().translate(.5f, 0, .5f).rotateY(facingAngle).translate(-.5f, 0, -.5f);
        if (!group.parent().isEmpty()) translate(matrix, pose.groups().get(group.parent()).translation());
        translate(matrix, transform.translation());
        translate(matrix, group.pivot());
        Vector rotation = transform.rotation();
        matrix.rotateXYZ((float) Math.toRadians(rotation.x()), (float) Math.toRadians(rotation.y()),
                (float) Math.toRadians(rotation.z()));
        Vector pivot = group.pivot();
        return matrix.translate((float) -pivot.x() / 16, (float) -pivot.y() / 16, (float) -pivot.z() / 16);
    }

    private static LinkedHashMap<String, Transform> idleGroups() {
        var groups = new LinkedHashMap<String, Transform>();
        GROUP_NAMES.forEach(name -> groups.put(name, Transform.IDLE));
        return groups;
    }
    private static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalArgumentException(reason);
    }
    private static Vector vector(JsonObject object, String field) {
        var array = object.getAsJsonArray(field);
        require(array.size() == 3, "必须为三轴量");
        Vector value = new Vector(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
        require(Double.isFinite(value.x()) && Double.isFinite(value.y()) && Double.isFinite(value.z()), "三轴量必须有限");
        return value;
    }
    private static boolean within(Vector value, Vector min, Vector max) {
        return value.x() >= min.x() && value.x() <= max.x() && value.y() >= min.y() && value.y() <= max.y()
                && value.z() >= min.z() && value.z() <= max.z();
    }
    private static boolean toolsIdle(Map<String, Transform> pose) {
        return pose.get("left_tool").equals(Transform.IDLE) && pose.get("right_tool").equals(Transform.IDLE);
    }
    private static boolean pressing(Map<String, Transform> pose) {
        return pose.get("fixture_carriage").translation().y() != 0;
    }
    private static Vector lerp(Vector from, Vector to, double amount) {
        return new Vector(from.x() + (to.x() - from.x()) * amount,
                from.y() + (to.y() - from.y()) * amount, from.z() + (to.z() - from.z()) * amount);
    }
    private static void translate(Matrix4f matrix, Vector value) {
        matrix.translate((float) value.x() / 16, (float) value.y() / 16, (float) value.z() / 16);
    }
}
