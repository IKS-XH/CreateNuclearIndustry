package com.iksxh.create_nuclear_industry.boiler;

/** 服务端纯账本的库存身份；新批次生成后固定，不依赖客户端图标、流体注册或当前炉压。 */
public enum BoilerSteamInventoryKind {
    NORMAL, SUPERCRITICAL
}
