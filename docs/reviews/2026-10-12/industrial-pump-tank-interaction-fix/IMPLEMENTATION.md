# 工业泵深色伪图案与创造罐容器交互窄修

执行者实际读取主AGENTS与本批活动卡，并应用minecraft-modding/testing/resource-pack、systematic-debugging、TDD及verification-before-completion。版本保持MC1.21.1、Java21、NeoForge21.1.219、Create6.0.10-280、Ponder1.0.82、Flywheel1.0.6；未按技能新示例升级。

## 实际改动

泵未找到独立旧方向glyph。锁定COG大面UV[0,9,9,14]采样32×32 PNG中的近黑实体孔，其x3..14/y19..26共96点为RGBA(0,0,8,255)；这是本批对截图深色三角确定的材质消费来源，实际投影去除效果仍须同处GPU复看。按PM重新确认的最小补片，仅将这96点SVG色改为相邻同面钢灰#747e86，再单张导出PNG；其他928点与全部alpha逐像素保持，未改Renderer、三模型、COG几何、青箭头、outputDirection、流量/SU或其他七张材质。produce/verify增加`--pump-only`，sources记录96点派生补片；未来生产器从锁定Create像素生成的本张结果与当前SVG/PNG逐像素一致。没有调用全量生产器。

创造罐原能力包装使原Create对CreativeSmartFluidTank的类型判断失效。新CreateTankPlayerInteractionMixin只包原FluidTankBlock.useItemOn内两处容器helper，先完整执行原helper；仅其成功且是实际服务端同BE、合法创造controller、非锅炉路径时，用既有原库存设置身份或清EMPTY。普通罐、失败、客户端、桶数量及返还仍走原调用链。原CreateTankCapabilityRestrictionMixin和RestrictedFluidHandler逐字保持，无裸能力返回、许可或ThreadLocal。当前只注册冷却液桶，没有热液桶；不新增热桶或泛用容器路线，外部热液/SC禁运保持。

最终11来源/资源/工具/测试路径与SHA见`build/reports/industrial-pump-tank-interaction-fix/sources-before-assemble.json`，所有修改均在本卡写集。普通apply_patch不能写src后按PM批准升级相同精确写入；无自动审查拒绝、Git写或用户dirty清理。原仅LF/CRLF stat变化的无关源没有格式化。

## 原始失败与通过证据

01真实探针首次错误使用双命名空间，结构启动失败原位保留，未到行为断言。02改用本库templateNamespace后，原生产实际1/1业务RED：冷液桶helper返回SUCCESS后创造罐仍未标记身份，独立世界自然保存退出1。03新增能力代表JUnit2/2退出0，覆盖外部热液/SC模拟与执行拒绝、读库存副本及普通冷液/水有限交易。04实际BlockState手持组3/3、27秒自然退出0并完整保存，覆盖冷液桶标记/清除与原数量返还、副手水与非容器失败/手隔离、普通罐冷液/水逐桶1000mB有限进出。

05/05b把近黑点误当alpha0的初始判断被实际UV/PNG诊断反证，原日志/XML和源快照保留；未据此改生产，这不是render层业务RED。06正确像素RED为1/1断言失败：实际COG面仍采到近黑伪图案。07仅单张生产/验证均退出0，96点/928点/alpha及未来producer身份在`pump-patch-identity.json`。08仅新像素用例加原青箭头真实六向正反表面用例2/2，18秒退出0；没有重跑管网连接全矩阵、桶代表组、旧全域或fullbuild。两次最终JUnit共4/4，原XML分别保存在03与08目录，不把历史诊断失败计入通过数。

## 唯一制品

09唯一必要`.\gradlew.bat assemble`为1秒exit0，compileJava/processResources复用，jar实际执行。最终标准包为3,046,298字节，SHA256 `1829ef1cc99039fb4a340d7794d5a9adf58da34955a3e2582ea7a1770a74530c`，同源完整冻结副本在本reportroot的`frozen-jar/`。用户改名`create_nuclear_industry-0.1.0_261012.jar`仍3,039,185字节/ecbe0e38…，没有覆盖或改名；PM此前保存的旧包也保持。

`package-identity.json`实际核验verified/errors为空：11/11最终来源、3/3资源source/build/JAR、2/2新生产class编译/JAR；12个非补片保护源保持。与用户原ecbe包逐条比较仅5项变化：新Mixin/class、新GameTest/class、隔离空结构、新Mixin登记、单张泵PNG；其余2145条JAR字节保持。因此泵Renderer/几何/青箭头、原能力包装、热工、运行显示前置和其他七材质均没有打包漂移。

报告交PM作同次独立窄审。未启动客户端、复制/覆盖用户世界或关闭本批人工门；真实GPU中深色伪图案消失及用户桶标记/清除仍按HANDOFF复看，不能以离线像素和服务端探针代替用户视觉验收。
