import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createUiHarness } from "./ui-harness.js";

async function sample(name = "half") {
  return JSON.parse(await readFile(new URL(`../scenarios/balance-02b-${name}.json`, import.meta.url), "utf8"));
}
function plain(value) { return JSON.parse(JSON.stringify(value)); }
function csvRecords(text) {
  const [header, ...rows] = text.trim().split("\n").map((line) => line.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/).map((cell) => cell.startsWith('"') ? cell.slice(1, -1).replaceAll('""', '"') : cell));
  return rows.map((row) => {
    assert.equal(row.length, header.length, "每一行必须与表头字段数相等");
    return Object.fromEntries(header.map((key, index) => [key, row[index]]));
  });
}

for (const single of [false, true]) {
  const label = single ? "离线单文件" : "模块页面";
  test(`${label}：真实渲染函数显示默认/自定义两个结算倍率`, async () => {
    const ui = await createUiHarness(single);
    for (const [name, heat, burn, power] of [["half", 1.5, 2, 4.5], ["full", 2, 3, 6], ["custom-half", 2, 3, 6]]) {
      await ui.import(await sample(name));
      ui.nodes.get("tick-step").value = "1";
      await ui.click("step-button");
      ui.api.selectColumn("0,0");
      const details = ui.nodes.get("column-details").innerHTML;
      assert.ok(details.includes(`本 tick 损伤产热倍率</dt><dd>${heat}×`), details);
      assert.ok(details.includes(`本 tick 损伤燃耗倍率</dt><dd>${burn}×`), details);
      assert.equal(ui.api.state.lastResult.summary.generatedHeat, power);
      assert.equal(ui.api.state.snapshot.tick, 1);
    }
    assert.match(ui.nodes.get("formula-text").innerHTML, /H\/B.*不代表整堆 SU 效率/);
  });

  test(`${label}：显示结算前倍率，避免使用结算后完整度重新推算`, async () => {
    const ui = await createUiHarness(single);
    const scene = await sample();
    scene.config.actualColdIn = 0;
    scene.config.actualHotOut = 0;
    scene.config.fuelColumnDamageRate = 0.1;
    scene.initialSnapshot.coldCoolantMb = 0;
    await ui.import(scene);
    ui.nodes.get("tick-step").value = "1";
    await ui.click("step-button");
    assert.ok(ui.api.state.snapshot.columns[0].integrity < 0.5);
    ui.api.selectColumn("0,0");
    assert.match(ui.nodes.get("column-details").innerHTML, /本 tick 损伤产热倍率<\/dt><dd>1.5×/);
    assert.match(ui.nodes.get("column-details").innerHTML, /本 tick 损伤燃耗倍率<\/dt><dd>2×/);
  });

  test(`${label}：非法 3/2 与空输入阻止应用、运行、单步和两种导出`, async () => {
    const ui = await createUiHarness(single);
    await ui.import(await sample());
    for (const [heat, burn] of [[3, 2], ["", 3]]) {
      await ui.input("param-fuelColumnDamageHeatMultiplier", heat);
      await ui.input("param-fuelColumnDamageBurnMultiplier", burn);
      await ui.click("apply-reset");
      assert.match(ui.nodes.get("app-status").textContent, /无法应用场景/);
      for (const button of ["run-button", "step-button", "export-json", "export-csv"]) {
        await ui.click(button);
        assert.match(ui.nodes.get("app-status").textContent, /参数无效/);
      }
      assert.equal(ui.api.state.snapshot.tick, 0);
      assert.equal(ui.timers.size, 0);
      assert.equal(ui.downloads.length, 0);
      assert.equal(ui.api.state.config.fuelColumnDamageHeatMultiplier, 2);
    }
  });

  test(`${label}：JSON 往返和 CSV 每行保留实测终点、版本与列倍率`, async () => {
    const ui = await createUiHarness(single);
    await ui.import(await sample("custom-half"));
    ui.nodes.get("tick-step").value = "1";
    await ui.click("step-button");
    await ui.click("export-json");
    const saved = JSON.parse(await ui.downloads.at(-1).text());
    assert.equal(saved.config.fuelColumnDamageHeatMultiplier, 3);
    assert.equal(saved.config.fuelColumnDamageBurnMultiplier, 5);
    assert.equal(saved.ruleVersion, "P1.2-simulator-5");
    assert.equal(saved.toolVersion, "1.3.0");
    assert.equal(saved.resultMetadata.fuelColumnDamageBurnMultiplier, 5);
    await ui.import(saved);
    assert.equal(ui.nodes.get("param-fuelColumnDamageHeatMultiplier").value, "3");
    assert.equal(ui.nodes.get("param-fuelColumnDamageBurnMultiplier").value, "5");
    assert.equal(ui.api.state.lastResult.summary.generatedHeat, 6);
    assert.deepEqual(plain(ui.api.state.snapshot), saved.finalSnapshot);
    await ui.click("export-csv");
    const rows = csvRecords(await ui.downloads.at(-1).text());
    for (const row of rows) {
      assert.equal(row.rule_version, saved.ruleVersion);
      assert.equal(row.fuelColumnDamageHeatMultiplier, "3");
      assert.equal(row.fuelColumnDamageBurnMultiplier, "5");
      assert.equal(row.replay_fuelColumnDamageBurnMultiplier, "5");
    }
    const tick = rows.find((row) => row.row_type === "tick");
    assert.equal(tick.column_key, "");
    assert.equal(tick.coolant_total_capacity_mB, "24000");
    const column = rows.find((row) => row.row_type === "column" && row.column_key === "0,0");
    assert.equal(column.damage_heat_multiplier, "2");
    assert.equal(column.damage_burn_multiplier, "3");
  });

  test(`${label}：旧版本或无版本记录不被重标，续算另起历史并保留来源`, async () => {
    const ui = await createUiHarness(single);
    for (const version of ["P1.2-simulator-4", undefined]) {
      const old = await sample("full");
      delete old.resultMetadata;
      delete old.config.fuelColumnDamageHeatMultiplier;
      delete old.config.fuelColumnDamageBurnMultiplier;
      old.ruleVersion = version;
      old.toolVersion = "1.2.0";
      old.finalSnapshot = structuredClone(old.initialSnapshot);
      old.finalSnapshot.tick = 7;
      old.finalSnapshot.columns[0].fuelRemaining = 2.5;
      old.summary = { tick: 7, gameSeconds: 0.35, generatedHeat: 6, fuelConsumed: 6 / 216000 };
      old.history = [{ summary: old.summary, columns: { "0,0": { generatedHeat: 6, plannedBurn: 6 / 216000 } } }];
      await ui.import(old);
      assert.match(ui.nodes.get("rule-version").textContent, /旧结果/);
      ui.api.selectColumn("0,0");
      assert.match(ui.nodes.get("column-details").innerHTML, /未记录（旧规则）/);
      await ui.click("export-json");
      const saved = JSON.parse(await ui.downloads.at(-1).text());
      assert.equal(saved.ruleVersion, version ?? "unknown");
      assert.equal(saved.resultMetadata.fuelColumnDamageHeatMultiplier, null);
      assert.deepEqual(saved.history, old.history);
      assert.deepEqual(saved.summary, { ...saved.summary, ...old.summary });
      await ui.import(saved);
      await ui.click("export-csv");
      for (const row of csvRecords(await ui.downloads.at(-1).text())) {
        assert.equal(row.rule_version, version ?? "unknown");
        assert.equal(row.fuelColumnDamageBurnMultiplier, "");
        assert.equal(row.replay_fuelColumnDamageBurnMultiplier, "3");
      }
      ui.nodes.get("tick-step").value = "1";
      await ui.click("step-button");
      assert.equal(ui.api.state.history.length, 1);
      assert.equal(ui.api.state.snapshot.tick, 8);
      assert.equal(ui.api.state.resultMetadata.sourceRuleVersion, version ?? "unknown");
      assert.equal(ui.api.state.resultMetadata.sourceTick, 7);
      assert.equal(ui.api.state.lastResult.columns["0,0"].damageBurnMultiplier, 3);
      assert.ok(ui.api.state.snapshot.columns[0].fuelRemaining < 2.5);
      await ui.click("export-json");
      const resumed = JSON.parse(await ui.downloads.at(-1).text());
      assert.equal(resumed.ruleVersion, "P1.2-simulator-5");
      assert.equal(resumed.initialSnapshot.tick, 7);
      assert.equal(resumed.history[0].summary.tick, 8);
    }
  });

  test(`${label}：极大有限终点显示溢出并停止单步和定时运行`, async () => {
    const ui = await createUiHarness(single);
    const scene = await sample("full");
    scene.config.fuelColumnDamageHeatMultiplier = Number.MAX_VALUE / 2;
    scene.config.fuelColumnDamageBurnMultiplier = Number.MAX_VALUE;
    delete scene.resultMetadata;
    await ui.import(scene);
    ui.nodes.get("tick-step").value = "1";
    const before = plain(ui.api.state.snapshot);
    await ui.click("step-button");
    assert.match(ui.nodes.get("app-status").textContent, /数值溢出/);
    assert.equal(ui.api.state.snapshot.tick, 0);
    assert.deepEqual(plain(ui.api.state.snapshot.columns), before.columns);
    assert.equal(ui.api.state.history.length, 0);
    await ui.click("run-button");
    ui.runTimers();
    assert.equal(ui.timers.size, 0);
    assert.equal(ui.api.state.snapshot.tick, 0);
    assert.match(ui.nodes.get("warnings").innerHTML, /数值溢出/);
  });

  test(`${label}：文件改用另一组合法终点后，保留旧结果并从新结算记录实际参数`, async () => {
    const ui = await createUiHarness(single);
    await ui.import(await sample("custom-half"));
    ui.nodes.get("tick-step").value = "1";
    await ui.click("step-button");
    await ui.click("export-json");
    const scene = JSON.parse(await ui.downloads.at(-1).text());
    scene.config.fuelColumnDamageHeatMultiplier = 1.5;
    scene.config.fuelColumnDamageBurnMultiplier = 2.5;
    await ui.import(scene);
    assert.equal(ui.api.state.lastResult.summary.generatedHeat, 6);
    await ui.click("step-button");
    assert.equal(ui.api.state.history.length, 1);
    assert.equal(ui.api.state.lastResult.summary.generatedHeat, 3.75);
    await ui.click("export-json");
    const saved = JSON.parse(await ui.downloads.at(-1).text());
    assert.equal(saved.resultMetadata.fuelColumnDamageHeatMultiplier, 1.5);
    assert.equal(saved.resultMetadata.fuelColumnDamageBurnMultiplier, 2.5);
    assert.equal(saved.history[0].summary.resultMetadata.fuelColumnDamageBurnMultiplier, 2.5);
    assert.equal(saved.initialSnapshot.tick, 1);
    assert.equal(saved.resultMetadata.sourceTick, 1);
  });
}

test("模块与离线同一输入的快照、摘要、JSON/CSV 和页面文案一致", async () => {
  const modular = await createUiHarness(false);
  const single = await createUiHarness(true);
  for (const name of ["healthy", "half", "full", "custom-half"]) {
    const scene = await sample(name);
    for (const ui of [modular, single]) {
      await ui.import(scene);
      ui.nodes.get("tick-step").value = "1";
      await ui.click("step-button");
      ui.api.selectColumn("0,0");
      await ui.click("export-json");
      await ui.click("export-csv");
    }
    assert.deepEqual(plain(modular.api.state.snapshot), plain(single.api.state.snapshot));
    assert.deepEqual(plain(modular.api.state.lastResult), plain(single.api.state.lastResult));
    for (const id of ["column-details", "formula-text", "overview-grid"]) {
      assert.equal(modular.nodes.get(id).innerHTML, single.nodes.get(id).innerHTML);
    }
    assert.equal(await modular.downloads.at(-2).text(), await single.downloads.at(-2).text());
    assert.equal(await modular.downloads.at(-1).text(), await single.downloads.at(-1).text());
  }
});
