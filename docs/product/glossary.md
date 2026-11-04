# Product Glossary

> Public semantic source: English. Chinese counterpart: [glossary.zh-CN.md](glossary.zh-CN.md).

| Canonical English term | Chinese working term | Meaning |
| --- | --- | --- |
| Home | 主页面 | The Launcher4Max primary surface containing time, date, weekday, and favorites; unqualified `Home` never means the Android platform navigation action |
| Android system Home action | Android 系统 Home 操作 | The platform action that requests display of the selected default Launcher, regardless of whether it originates from gesture navigation, a navigation button, a physical key, or another system entry |
| Drawer | 应用列表 | The full indexed list of platform-exposed launchable entries |
| Settings | 设置 | Launcher4Max configuration and product-information surface |
| Launchable entry | 可启动应用条目 | One platform-exposed target that Launcher4Max can launch; a primary app and clone are separate entries |
| Item | 条目 | One interactive application element occupying a position in the Drawer list, search results, or a favorite module; an item's own inset, grid placement, outline, and overlays are all described with item, and the data identity behind one item remains the launchable entry |
| Favorite | 收藏应用 | A launchable entry saved once to Home, either in one favorite module or on the Home bottom app bar |
| Favorite main list | 收藏主列表 | The one full-width, vertically scrolling ordered sequence of peer vertical favorite modules and horizontal favorite ribbons below Home basic information |
| Favorite module | 收藏模块 | A persisted vertical favorite module or horizontal favorite ribbon that contains at least one favorite |
| Home bottom app bar | 主页底部常驻应用栏 | One favorite position at the bottom of Home; it is not a favorite module, and a favorite placed there is still one favorite |
| Vertical favorite module | 纵向收藏模块 | A full-width module whose naturally expanded entries share one module-level size, name placement, and items-per-row style |
| Horizontal favorite ribbon | 横向收藏织带 | Full-width fixed-style module using Ribbon or Flow |
| Ribbon layout | 织带布局 | One-row horizontal mode; scrolls horizontally only on overflow |
| Flow layout | 流式布局 | Multi-row mode; wraps within module width without horizontal scrolling |
| Add-favorite entry | 新增收藏入口 | A non-persisted favorite-main-list or in-module entry whose position alone determines whether it creates a module or adds to an existing one; edit mode shows both kinds, while one-shot operation temporarily shows only the two main-list entries |
| Module header | 模块表头 | The row above a favorite module that shows its name when name visibility is on; in edit mode it holds the editable name, visibility affordance, style affordance, whole-module unfavorite action, and module-reorder handle |
| Edit mode | 编辑模式 | The Home state entered from a favorite action or a basic-information quick-action slot bound to edit mode, exposing every editing affordance and leaving only through Back or interruption |
| One-shot operation | 一次操作模式 | The temporary Home state for one favorite placement, entered only by receiving a Drawer drag-to-favorite; it shows only the destinations needed for that placement and returns to normal Home when the operation ends |
| Style settings panel | 样式设置面板 | The shared rounded settings-panel presentation used by Drawer display settings and the Home module-style editor; each host separately defines its content, modality, placement, and behavior |
| Application shortcut | 应用快捷操作 | An action exposed by the application through the platform |
| Launcher action | 启动器操作 | An action supplied by Launcher4Max, such as favorite, edit, or uninstall |
| Application action sheet | 应用操作面板 | The modal Bottom Sheet containing application identity, application shortcuts, and Launcher actions |
| Section anchor | 分组锚点 | A Drawer section heading such as A or `#`; inline presentation scrolls above its section, while left-side presentation occupies its section's leading column and pins below the fixed top app bar only while that section crosses the viewport |
| Alphabet index | 字母索引 | The fixed right-side Drawer index used to jump between anchors |
| Favorite multi-selection | 收藏多选 | The temporary Drawer mode that collects an ordered set of previously unfavorited applications for one captured Home favorite destination |
| Screen locking | 锁屏 | The optional capability that requests one Android system lock action through Launcher4Max's narrowly scoped accessibility service, triggered by the basic-information quick-action slot the user binds to it |
| Quick actions | 快捷操作 | The user-configurable registry that binds the Home basic-information blank-space slots — double tap and long press — to `No action`, `Edit mode`, or `Screen lock` |
| Quick action slot | 快捷操作槽位 | One registered Home basic-information blank-space gesture whose bound action is user-configurable; the current contract registers the basic-information double-tap and long-press slots |
| Default-launcher prompt | 默认启动器提示 | The dismissible Home element presented between the basic-information region and the favorite main list while Launcher4Max is not the device's default Launcher; it is not a favorite module, and selecting it opens the same system destination as the Settings default-home entry |
| Privacy statement | 隐私声明 | The offline Settings presentation describing Launcher4Max's current data handling, storage, deletion, permission, and external-link boundaries |
| Prominent disclosure | 显著披露 | The separate in-app explanation and affirmative choice shown immediately before an enable-oriented accessibility-settings handoff; it is not replaced by the Privacy statement |
| Badge | 标记 | Platform-provided visual identity for a clone or profile context |
| Application information | 应用信息 | The system-owned information and management surface for an application |
| Private Space | 私密空间 | Android hidden-profile capability outside the current product contract; Launcher4Max does not request `ACCESS_HIDDEN_PROFILES` to access it |
| Backup file | 备份文件 | One user-chosen, schema-versioned local JSON file containing the complete favorite-module, Home bottom app bar, Drawer display-setting, quick-action-binding, and icon-shape state, created only by the explicit Settings backup action and never uploaded or written automatically |
| Restore | 恢复 | The Settings action that reads a user-selected backup file and, after confirmation, atomically replaces the current favorite-module, Home bottom app bar, Drawer display-setting, quick-action-binding, and icon-shape state |

Use these terms consistently in product documents. Technical names may differ only when an implementation distinction is necessary and documented.

## User-facing label mapping

Specification terms and user-facing interface labels are separate. Interface labels do not replace the canonical terms above, and a label change does not rename a specification term.

| Specification term | Simplified Chinese interface label | English interface label |
| --- | --- | --- |
| Vertical favorite module | 收藏列表 | Favorite list |
| Horizontal favorite ribbon | 收藏织带 | Favorite ribbon |
| Module header | 模块表头 | Module header |
| Ribbon layout | 织带 | Ribbon |
| Flow layout | 流式布局 | Flow |
| Quick actions | 快捷操作设置 | Quick action settings |
| Basic-information double-tap slot | 基础信息区双击 | Basic-information double tap |
| Basic-information long-press slot | 基础信息区长按 | Basic-information long press |
| Screen locking | 锁屏 | Screen lock |
| Backup | 备份收藏与设置 | Back up favorites and settings |
| Restore | 从备份恢复 | Restore from backup |
| Default-launcher prompt title | 尚未设为默认启动器 | Not set as the default launcher yet |
| Default-launcher prompt supporting line | 设为默认后，回到这里 | Set as default to return here |
| Default-launcher prompt dismiss control | 关闭 | Dismiss |
| Home bottom app bar | 主页底部常驻应用栏 | Home bottom app bar |

`收藏主列表` is the canonical Chinese working term for the favorite main list. `home 收藏区` is an informal synonym for the same region and is not a separate term.
