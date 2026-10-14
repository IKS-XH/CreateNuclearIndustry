# 15C负责人实际预览复核

2026-10-14，root使用computer-use/CUA在IAB Tab1打开`http://127.0.0.1:4195/`，操作真实页面控件并查看截图。预览使用正式模型/PNG和同一生产动作JSON，是示范输入，未连接用户Minecraft。页面capture固定保存实际canvas和输入/姿态身份，随后原样复制至本目录，不重新绘图或修改像素。

## 已实看画面与姿态

- [制造10%正常窗](manufacture-align-normal.png)：一侧执行杆向内倾斜，另一侧待机，肩座固定；[捕获身份](manufacture-align-normal.json)记录北向、10%与真实模型输入。
- [制造84%升台剖视](manufacture-lift-cutaway.png)：双臂待机，台面与钳口一起抬升，底座/导柱固定；[身份](manufacture-lift-cutaway.json)中carriage Y=1。
- [封装40%正常窗](sealing-lock-normal.png)：双臂向内短移/锁紧，台面固定；[身份](sealing-lock-normal.json)中实际工序为sealing。
- [暂停前](paused-before.json)/[暂停后](paused-after.json)：running=true、paused=true，phase都0.4、visual progress都5120，两次pose逐字段相同。此为页面暂停示范，不补写Minecraft实机暂停通过。
- [过载](overloaded.json)：running=true、paused=false、overloaded=true，仍phase0.4/progress5120。
- [恢复再停](resumed-stopped.png)/[身份](resumed-stopped.json)：解除过载后恢复到41.25%，停止后保留约5280工时，未追补此前等待时间。
- [完成回待机](completed-idle.json)：output=true、phase0、stage待机，八组位移/角度全部为零。

以上捕获ready=true/errors=[]，实际8组Box3和108个mesh源角点匹配；模型/灯/窗和静态完整物品可辨。网页中心补偿、原生矩阵、同源HTTP及客户端定向结果由B报告分别绑定，不将原生/网页结果合并成Minecraft动画验收。

## 东向取景首次反馈

[首次东向画面](east-framing-first.png)/[身份](east-framing-first.json)保留：机构与壳体对齐，但整机围主控转向后中心位移，相机还看固定原点，半边被viewport裁掉。已派B仅修预览camera目标/剖视方向，Java/动作JSON/rig/既有17项定向保持。修后复看证据随后追加，不能把首次画面算取景通过。

第一轮修正后重新加载真实页面，选择东向、默认yaw=-35/pitch=17、制造40%，[第二次画面](east-framing-second.png)/[身份](east-framing-second.json)仍显示右侧被裁出画布。此时capture里的camera target已为[-1,1,0]，所以目标数值检查不等于实际取景通过。已要求B只读实际整机世界包络、相机矩阵和画布投影后继续窄修；本次失败原图保留，未据此重跑Java测试或提前打包。

## 最终取景窄修复看

第二次窄修以实际库mesh组成的整机世界Box3八角投影到相机空间，同时按画布宽高比满足水平与垂直视野，并预留10%边距。capture新增真实world/view/projection矩阵、NDC八角、canvas/WebGL viewport及scissor状态。最终scene SHA256为`47a60f430d1aa5806ca67c40bc4f1a01f1de1d1bfa737f946f6b9fdc46720de2`；动作JSON、rig和游戏客户端代码未变。

负责人重新加载后实看[东向](east-framing-final.png)、[南向](south-framing-final.png)、[西向](west-framing-final.png)默认斜视/制造40%/剖视，三张完整canvas均无边缘裁切；各同名JSON为ready=true、errors=[]、withinViewport=true，实际包络八角在画布内，剖面随整机朝向变化。东向worldBox为[-2,0,-1]到[0,2,1]、实际349×475画布，NDC水平极值约±0.909。南/东从背侧看到保留半壳，西向可见剖开后的机构，未把不同视角下的遮挡算作模型消失。

恢复北向正常窗并切正面实看[制造10%](north-manufacture-final.png)/[身份](north-manufacture-final.json)和[封装40%](north-sealing-final.png)/[身份](north-sealing-final.json)，双臂交替与同时锁紧可区分、固定肩座和底座保持；[完整静态物品](item-final.png)保持原GUI形状。两份捕获同样ready=true/errors=[]/withinViewport=true。以上关闭本批网页取景问题，完成负责人预览复看；实际Minecraft工序动画/停恢复仍待用户集中观察，不关闭或重开其它功能门。
