# 产品字典

> 英文语义源：[glossary.md](glossary.md)。

| 规范英文术语 | 中文工作术语 | 含义 |
| --- | --- | --- |
| Home | 主页面 | 包含时间、日期、星期和收藏应用的 Launcher4Max 主界面；未加限定的 `Home` 永远不表示 Android 平台导航操作 |
| Android system Home action | Android 系统 Home 操作 | 请求显示当前默认 Launcher 的平台操作，无论它来自手势导航、导航按钮、物理按键还是其他系统入口 |
| Drawer | 应用列表 | 平台暴露的全部可启动应用条目的带索引列表 |
| Settings | 设置 | Launcher4Max 配置与产品信息界面 |
| Launchable entry | 可启动应用条目 | Launcher4Max 可以启动的一个平台目标；主应用与分身是不同条目 |
| Item | 条目 | Drawer 列表、搜索结果或收藏模块中占据一个位置的交互应用元素；条目自身的 inset、网格位置、描边与覆盖都以条目描述，一个条目背后的数据身份仍是可启动应用条目 |
| Favorite | 收藏应用 | 在 Home 只收藏一次的可启动应用条目，位置是一个收藏模块或主页底部常驻应用栏 |
| Favorite main list | 收藏主列表 | Home 基础信息下方，由同级纵向收藏模块和横向收藏织带组成的一个全宽、可纵向滚动有序序列 |
| Favorite module | 收藏模块 | 至少包含一个收藏的持久纵向收藏模块或横向收藏织带 |
| Home bottom app bar | 主页底部常驻应用栏 | Home 底部的一个收藏位置；它不是收藏模块，放在那里的收藏仍然只是一份收藏 |
| Vertical favorite module | 纵向收藏模块 | 全宽模块，其自然展开的全部条目共享一组模块级尺寸、名称位置和每行数量样式 |
| Horizontal favorite ribbon | 横向收藏织带 | 全宽模块，包含固定样式、按内容测量的收藏条目，并使用织带或流式布局 |
| Ribbon layout | 织带布局 | 横向收藏织带的单行横向展示模式，仅在溢出时横向滚动 |
| Flow layout | 流式布局 | 横向收藏织带的多行展示模式，在模块可用宽度内换行且不横向滚动 |
| Add-favorite entry | 新增收藏入口 | 收藏主列表或模块内部的非持久条目；仅由其位置决定它创建新模块还是加入既有模块。编辑模式显示两种入口，一次操作模式只临时显示两个主列表入口 |
| Module header | 模块表头 | 收藏模块上方的一行；名称展示打开时显示模块名称，编辑模式中放置可编辑名称、展示开关、样式入口、整列取消收藏和模块排序拖动柄 |
| Edit mode | 编辑模式 | 从收藏操作或绑定为编辑模式的快捷操作槽位进入，展示全部编辑元素，并且只通过 Back 或中断退出的 Home 状态 |
| One-shot operation | 一次操作模式 | 只完成一次收藏放置的临时 Home 状态；只能从接收 Drawer 拖动收藏进入，只显示这次放置所需的目的地，操作结束即回到普通 Home |
| Style settings panel | 样式设置面板 | Drawer 显示设置与 Home 模块样式编辑器共用的圆角设置面板呈现；每个宿主分别定义自己的内容、模态、位置和行为 |
| Application shortcut | 应用快捷操作 | 应用通过平台暴露的操作 |
| Launcher action | 启动器操作 | 由 Launcher4Max 提供的操作，例如收藏、编辑或卸载 |
| Application action sheet | 应用操作面板 | 包含应用身份、应用快捷操作和启动器操作的模态 Bottom Sheet |
| Section anchor | 分组锚点 | Drawer 的分组标题，例如 A 或 `#`；行内呈现时随分组上方滚动，左侧呈现时占据该分组的起始列，并且只在该分组经过视口时固定在固定顶部应用栏下方 |
| Alphabet index | 字母索引 | Drawer 右侧固定索引，用于在锚点之间跳转 |
| Favorite multi-selection | 收藏多选 | Drawer 的临时模式，为一个已记录的 Home 收藏目标收集一组有顺序且此前未收藏的应用 |
| Screen locking | 锁屏 | 通过 Launcher4Max 用途受限的无障碍服务请求一次 Android 系统锁屏操作的可选能力，由用户绑定到它的基础信息区快捷操作槽位触发 |
| Quick actions | 快捷操作 | 把 Home 基础信息区空白槽位（双击、长按）绑定为 `无操作`、`编辑模式` 或 `锁屏` 的用户可配置注册表 |
| Quick action slot | 快捷操作槽位 | 一个已登记的 Home 基础信息区空白手势，其绑定动作由用户配置；当前契约登记基础信息区双击与长按两个槽位 |
| Default-launcher prompt | 默认启动器提示 | 当 Launcher4Max 不是设备默认 Launcher 时，展示在基础信息区与收藏主列表之间的可关闭 Home 元素；它不是收藏模块，点击后打开与 Settings 默认主屏幕条目相同的系统目标 |
| Privacy statement | 隐私声明 | Settings 中可离线阅读的内容，用于说明 Launcher4Max 当前的数据处理、存储、删除、权限和外部链接边界 |
| Prominent disclosure | 显著披露 | 以启用为目的跳转无障碍设置前单独展示的应用内说明与明确选择；Privacy 正文不能替代它 |
| Badge | 标记 | 平台提供的分身或资料身份视觉标记 |
| Application information | 应用信息 | 系统负责的应用信息与管理界面 |
| Private Space | 私密空间 | 当前产品契约之外的 Android 隐藏资料能力；Launcher4Max 不申请 `ACCESS_HIDDEN_PROFILES` 访问该能力 |
| Backup file | 备份文件 | 一个由用户选择、带 schema 版本号的本地 JSON 文件，包含完整的收藏模块、主页底部常驻应用栏、Drawer 显示设置、快捷操作绑定与图标形状状态；仅由 Settings 中明确的备份操作创建，永不上传或自动写入 |
| Restore | 恢复 | Settings 中读取用户选择的备份文件，并在确认后原子替换当前收藏模块、主页底部常驻应用栏、Drawer 显示设置、快捷操作绑定与图标形状状态的操作 |

产品文档统一使用上述术语。只有实现确实需要额外区分且形成文档时，技术命名才可以不同。

## 界面文案映射

规格术语与面向用户的界面文案是两个独立维度。界面文案不替代上表的规范术语，文案调整也不重命名规格术语。

| 规格术语 | 简体中文界面文案 | 英文界面文案 |
| --- | --- | --- |
| 纵向收藏模块 | 收藏列表 | Favorite list |
| 横向收藏织带 | 收藏织带 | Favorite ribbon |
| 模块表头 | 模块表头 | Module header |
| Ribbon layout | 织带 | Ribbon |
| Flow layout | 流式布局 | Flow |
| 快捷操作 | 快捷操作设置 | Quick action settings |
| 基础信息区双击槽位 | 基础信息区双击 | Basic-information double tap |
| 基础信息区长按槽位 | 基础信息区长按 | Basic-information long press |
| 锁屏 | 锁屏 | Screen lock |
| Backup | 备份收藏与设置 | Back up favorites and settings |
| Restore | 从备份恢复 | Restore from backup |
| Default-launcher prompt title | 尚未设为默认启动器 | Not set as the default launcher yet |
| Default-launcher prompt supporting line | 设为默认后，回到这里 | Set as default to return here |
| Default-launcher prompt dismiss control | 关闭 | Dismiss |
| Home bottom app bar | 主页底部常驻应用栏 | Home bottom app bar |

`收藏主列表` 是 favorite main list 的规范中文工作术语。`home 收藏区` 是同一区域的口语同义说法，不是独立术语。
