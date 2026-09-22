import { readFile } from "node:fs/promises";
import vm from "node:vm";

const root = new URL("../", import.meta.url);

/** 拼接实际模块，仅移除模块声明；与离线脚本分别执行，不替换业务函数。 */
export async function modularScript() {
  const sources = await Promise.all(["config", "layout", "simulation", "report", "ui"].map(async (name) =>
    (await readFile(new URL(`src/${name}.js`, root), "utf8"))
      .replace(/^import[\s\S]*?from "[^"\n]+";\s*/gm, "")
      .replace(/^export /gm, "")));
  return sources.join("\n");
}

/** Node 中的最小 DOM 替身用于事件、表单和渲染行为测试，不充当真实浏览器证据。 */
export async function createUiHarness(single = false) {
  const html = await readFile(new URL(single ? "reactor-simulator-single.html" : "index.html", root), "utf8");
  const nodes = new Map();
  class Element {
    constructor(tagName = "div") {
      this.tagName = tagName;
      this.children = [];
      this.dataset = {};
      this.events = {};
      this.style = { setProperty() {} };
      this.classList = { add() {}, remove() {} };
      this.textContent = "";
      this.innerHTML = "";
      this.checked = false;
      this._value = "";
    }
    set id(value) { this._id = value; nodes.set(value, this); }
    get id() { return this._id; }
    set value(value) { this._value = String(value); }
    get value() { return this._value; }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = children; }
    setAttribute(key, value) { this[key] = value; }
    addEventListener(name, callback) { (this.events[name] ??= []).push(callback); }
    async dispatch(name, extra = {}) {
      for (const callback of this.events[name] ?? []) await callback({ target: this, preventDefault() {}, ...extra });
    }
    click() { return this.dispatch("click"); }
    remove() {}
    insertAdjacentHTML(_position, content) { this.innerHTML += content; }
    getContext() { return new Proxy({}, { get: () => () => {} }); }
  }
  for (const match of html.matchAll(/<([\w-]+)\b([^>]*\bid="([^"]+)"[^>]*)>/g)) {
    const node = new Element(match[1]);
    node.id = match[3];
    node.value = match[2].match(/\bvalue="([^"]*)"/)?.[1] ?? "";
  }
  for (const match of html.matchAll(/<select\b[^>]*id="([^"]+)"[^>]*>([\s\S]*?)<\/select>/g)) {
    const options = [...match[2].matchAll(/<option\b([^>]*)>/g)];
    const selected = options.find((entry) => entry[1].includes("selected")) ?? options[0];
    nodes.get(match[1]).value = selected?.[1].match(/value="([^"]*)"/)?.[1] ?? "";
  }
  nodes.set("trend-table tbody", new Element("tbody"));
  const descend = (node) => node.children.flatMap((child) => [child, ...descend(child)]);
  const document = {
    body: new Element("body"),
    createElement: (tag) => new Element(tag),
    addEventListener() {},
    querySelector: (selector) => nodes.get(selector.slice(1)),
    querySelectorAll: (selector) => selector === "#parameter-fields input"
      ? descend(nodes.get("parameter-fields")).filter((node) => node.tagName === "input") : [],
  };
  const downloads = [];
  const timers = new Map();
  let nextTimer = 0;
  const context = vm.createContext({
    document, window: { addEventListener() {} }, Blob,
    URL: { createObjectURL(blob) { downloads.push(blob); return "blob:test"; }, revokeObjectURL() {} },
    setInterval(callback) { timers.set(++nextTimer, callback); return nextTimer; },
    clearInterval(id) { timers.delete(id); }, setTimeout() {},
  });
  const script = single ? html.match(/<script>([\s\S]*?)<\/script>/)[1] : await modularScript();
  vm.runInContext(`${script}\nglobalThis.api = { state, importScene, selectColumn, renderAll,
    stepSnapshot, createInitialSnapshot, buildScene, buildCsv, parseSceneText, createDepthMatrix,
    createPreset, RULE_VERSION, TOOL_VERSION, DEFAULT_CONFIG, DEFAULT_GEOMETRY };`, context);
  return {
    api: context.api, nodes, downloads, timers,
    async import(scene) { await context.api.importScene({ text: async () => JSON.stringify(scene) }); },
    async input(id, value) { const node = nodes.get(id); node.value = value; await node.dispatch("input"); },
    async click(id) { await nodes.get(id).click(); },
    runTimers() { for (const callback of timers.values()) callback(); },
  };
}
