"""核对ART-LOGO-11两共享源的精确消费范围，并输出修前/修后身份及窄差异。

不改源码，不使用Git；冻结原件必须与PM协调基线一致。通过逐字重建确认
除一处延迟Item注册与一处icon supplier外，既有注册、展示内容和顺序均保持。
"""

import difflib
import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
REPORTS = REPO / "build/reports/art/ART-LOGO-11"
JAVA = REPO / "src/main/java/com/iksxh/create_nuclear_industry/content"
EXPECTED_BEFORE = {
    "ModItems.java": "6e83859b330c20e003142d15f2211ffabe38c7e864a119d6f2dcb45cf0ecea74",
    "ModCreativeTabs.java": "036f1d9596f37823eb5bf97ec7c192adb9e1596beb0f55553486b2052c887419",
}


def digest(content):
    return hashlib.sha256(content).hexdigest()


def main():
    """对照冻结基线逐字核本批允许的两个变动，失败时保留原始异常。"""
    records = []
    differences = []
    for filename, expected_hash in EXPECTED_BEFORE.items():
        original = (REPORTS / "shared-before" / filename).read_bytes()
        current = (JAVA / filename).read_bytes()
        assert digest(original) == expected_hash, f"{filename}冻结基线不符"
        newline = "\r\n" if b"\r\n" in original else "\n"
        before = original.decode("utf-8")
        if filename == "ModItems.java":
            anchor = "    public static final DeferredItem<Item> FRESH_FUEL_ASSEMBLY = ITEMS.register("
            addition = newline.join([
                "    /**",
                "     * 仅供创造模式标签读取的徽标物品；沿用延迟注册，客户端由平面模型显示。",
                "     * 不加入展示列表或配方，不承载服务端玩法状态。",
                "     */",
                "    public static final DeferredItem<Item> MOD_LOGO = ITEMS.register(",
                '            "mod_logo",',
                "            () -> new Item(new Item.Properties())",
                "    );",
                "",
                "",
            ])
            assert before.count(anchor) == 1
            expected = before.replace(anchor, addition + anchor)
        else:
            old = ".icon(() -> new ItemStack(ModBlocks.EXPERIMENTAL_REACTOR_CASING_ITEM.get()))"
            new = ".icon(() -> new ItemStack(ModItems.MOD_LOGO.get()))"
            assert before.count(old) == 1
            expected = before.replace(old, new)
        assert current == expected.encode("utf-8"), f"{filename}存在允许范围外的逐字变动"
        after = current.decode("utf-8")
        differences.extend(difflib.unified_diff(before.splitlines(keepends=True), after.splitlines(keepends=True),
                           fromfile="shared-before/" + filename, tofile="src/main/java/com/iksxh/create_nuclear_industry/content/" + filename))
        records.append({"path": (JAVA / filename).relative_to(REPO).as_posix(), "before_sha256": digest(original),
                        "after_sha256": digest(current), "only_authorized_change": True})
    (REPORTS / "shared-consumption.diff").write_text("".join(differences), encoding="utf-8", newline="")
    (REPORTS / "shared-identity.json").write_text(json.dumps({"task": "ART-LOGO-11", "files": records,
        "display_items_byte_preserved": True, "other_registrations_byte_preserved": True,
        "supplier_lazy": True, "gameplay_state_added": False}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("UI CHECK PASS=2/2 byte-exact allowed changes; displayItems/ordering/other registrations=preserved")
    for record in records:
        print(f"UI {record['path']} before={record['before_sha256']} after={record['after_sha256']}")
    print("UI native DeferredItem/ItemStack lazy supplier; no recipe/display entry/gameplay state")


if __name__ == "__main__":
    main()
