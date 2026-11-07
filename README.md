# Infinity What: Mekanism Addition

简体中文 | [English](#english)

---

## 简体中文

Infinity What 的可选 Mekanism 联动附属。它把“无限资源”的思路延伸到 Mekanism 的化学品与辐射系统：一个可配置的无限化学品源、一条从辐射到核化学品的完整产线，以及专用的辐射储罐。

本体（`Infinity What`）不需要安装 Mekanism；只有本附属需要。未安装 Mekanism 时本附属不会加载。

### 功能

- **无限源机器** —— 1.19.2 / 1.20.1 / 1.20.4 为**三台机器并行**：
  - **无限化学品发生器** —— 可配置的无限 Mekanism 化学品源。可选气体与颜料，按设定速率持续输出，支持逐面开关、黑名单/白名单、维度黑名单和专用服务器开关。
  - **无限灌注发生器** —— 放入红石、碳、真菌等可灌注物品后，无限提供对应灌注类型。
  - **无限浆液发生器** —— 通过加压管道或相邻机器导入浆液后，无限提供该浆液。
  - **1.21.1**：Mekanism 统一了化学品类型，只保留**一台**无限化学品发生器，可选注册表中的任意化学品（气体、灌注、颜料、浆液）；原来的灌注/浆液发生器已移除。
  - `infwhat_mekanism:unknown_radiation` 故意禁止选择，避免绕过辐射产线。
  - 裂变燃料、核废料、乏核废料、钚、钋、反物质属于危险化学品，需要先安装**危险气体遏制升级**。
- **辐射汲取海绵** —— 一次性辐射清理器。放在辐射区后清除半径内（默认 5 区块）的全部辐射源并饱和；也可以右键抽干玩家身上的累积剂量，把剂量转化为饱和海绵的数据。
- **辐射榨取机** —— 消耗饱和海绵，产出不明辐射物质，并返还一个全新的工作海绵。基础 40 tick、250 FE/t。
- **辐射萃取机** —— 按电路选择的配方，把不明辐射物质分离为核废料、乏核废料、钋、钚、裂变燃料或反物质。
- **不明辐射物质储罐** —— 4,000,000 mB 专用储罐；破坏时会把内部化学品一并带走，不会泄漏。
- **危险气体遏制升级 / 辐射配方电路** —— 前者解锁危险化学品，后者切换萃取配方。
- **JEI / Jade 集成** —— 分馏配方页、机器信息提示。两者均为可选依赖。
- **创造模式标签页** —— 附属物品并入 Infinity What 本体标签页；Mekanism 标签页中保留一个预填充不明辐射物质的创造化学品储罐。

### 支持的版本

| Minecraft | 加载器 | 加载器版本 | Mekanism 版本 | Java |
|---|---|---|---|---|
| 1.19.2 | Forge | 43.5.x | 10.3.x | 17 |
| 1.20.1 | Forge | 47.4.x | 10.4.x | 17 |
| 1.20.4 | NeoForge | 20.4.x | 10.5.x | 17 |
| 1.21.1 | NeoForge | 21.1.x | 10.7.x | 21 |

依赖：

- **必需**：Infinity What 本体 `infwhat` >= 2.0.0（实测 2.1.0b）
- **必需**：Mekanism，版本范围见上表

### 安装

1. 安装与游戏版本对应的 Forge / NeoForge。
2. 将 **Infinity What 本体**、**Mekanism** 和**本附属**的对应 jar 放入 `mods`。
3. 启动游戏。首次启动会生成 `config/infwhat_mekanism.toml`。

### 配置

配置文件为 `config/infwhat_mekanism.toml`。主要段落与默认值：

```toml
[gas_tap]
	enableGasTap = true
	allowOnDedicatedServer = false
	maxRatePerTick = 1000000
	defaultRatePerTick = 1000000

[gas_filter]
	mode = "BLACKLIST"
	list = []

[dimensions]
	blacklist = []

[radiation_sponge]
	enableRadiationSponge = true
	allowRadiationSpongeOnDedicatedServer = false
	radiusChunks = 5
	minRadiation = 1.0E-4
	clearPlayerRadiation = true
	playerDoseToChunks = 1.0
	playerDoseFactor = 1.0
	playerYieldCap = 9000
	nauseaTicks = 40
	nauseaAmplifier = 4

[radiation_machines]
	enableRadiationMachines = true
	# 旧版键，保留以兼容旧配置，不再参与产量计算
	gasPerChunk = 64
	radiationGasFactor = 1.0
	yieldEquilibrium = 8192
	overloadSteepness = 1.5
	extractorTankCapacity = 3200000
	fractionatorTankCapacity = 2000

[radiation_gas]
	unknownRadiationStrength = 5.0

[unknown_radiation_tank]
	capacity = 4000000
```

### 产量模型

环境路径：`R` 为海绵清除的放射源强度之和，`S_max` 为不明辐射物质强度（默认 5.0）。

```text
X = R / S_max

X <= K：Y = X × (2 - X / K)
X >  K：Y = K / (1 + a × ((X - K) / K)^2)

K = 8192 mB，a = 1.5
最终 Y = min(储罐容量, max(1, round(Y)))
```

玩家路径：`Y = min(9000, ln(1 + D))`，`D` 为右键瞬间的玩家剂量（Sv）。

行为：`X < K` 时逐轮增长（约翻倍），`X = K` 时稳定，`X > K` 时平滑回落但不归零。旧的“64 mB 泄漏刷 5000+ mB”增殖链已不存在。

### 版本差异

- **1.21.1**：Mekanism 10.7 将气体/灌注/颜料/浆液统一为 `Chemical`。因此只保留一个无限化学品发生器，原先的无限灌注发生器和无限浆液发生器已移除；旧存档中已放置的这两个方块会变成丢失方块。化学品翻译键为 `chemical.infwhat_mekanism.unknown_radiation`。
- **1.19.2 / 1.20.1 / 1.20.4**：保留无限灌注发生器与无限浆液发生器；气体龙头支持气体和颜料。
- **1.20.4 / 1.21.1**：辐射榨取机的复合模型使用 `neoforge:composite` 与 `neoforge_data`。
- **专用服务器**：气体龙头和辐射海绵默认不工作，需要分别开启 `gas_tap.allowOnDedicatedServer` 和 `radiation_sponge.allowRadiationSpongeOnDedicatedServer`。

### 从源码构建

切换到需要的版本分支后，下载源码并执行：

```bash
./gradlew build -x createMinecraftArtifacts
```

产物位于 `build/libs/`。JDK：1.19.2 / 1.20.1 / 1.20.4 使用 17，1.21.1 使用 21。

### 许可证

[Apache License 2.0](LICENSE)

---

## English

Optional Mekanism add-on for Infinity What. It extends the "infinite resources" idea into Mekanism chemicals and radiation: a configurable infinite chemical source, a complete radiation-to-nuclear-chemical production line, and a dedicated radiation tank.

The core mod (`Infinity What`) does not require Mekanism; only this add-on does. Without Mekanism installed, the add-on does not load.

### Features

- **Infinite source machines** — 1.19.2 / 1.20.1 / 1.20.4 ship **three machines running in parallel**:
  - **Infinite Chemical Generator** — a configurable infinite Mekanism chemical source. Gases and pigments, output at a configurable rate, with per-side output, blacklist/whitelist, dimension blacklist and a dedicated-server switch.
  - **Infinite Infusion Generator** — feed it an infusable item such as redstone, carbon or fungus and it sources that infusion type forever.
  - **Infinite Slurry Generator** — import slurry through a pressurized tube or an adjacent machine and it sources that slurry forever.
  - **1.21.1**: Mekanism unified its chemical types, so only the single **Infinite Chemical Generator** remains and it can select any registered chemical (gas, infusion, pigment, slurry); the infusion and slurry generators were removed.
  - `infwhat_mekanism:unknown_radiation` is intentionally forbidden so the radiation line cannot be bypassed.
  - Fissile fuel, nuclear waste, spent nuclear waste, plutonium, polonium and antimatter are dangerous and require the **Dangerous Gas Containment Upgrade**.
- **Radiation Sponge** — a one-shot radiation cleaner. Placed in a radioactive area it removes every radiation source in its chunk radius (5 by default) and saturates; right-clicking it drains the player's accumulated dose and stores that as sponge data.
- **Radiation Extractor** — consumes a saturated sponge, produces unknown radiation substance and returns a fresh working sponge. 40 ticks, 250 FE/t.
- **Radiation Fractionator** — splits unknown radiation substance into nuclear waste, spent nuclear waste, polonium, plutonium, fissile fuel or antimatter according to the installed formula circuit.
- **Unknown Radiation Tank** — 4,000,000 mB dedicated tank; breaking it keeps the chemical inside the dropped item instead of spilling it.
- **Dangerous Gas Containment Upgrade / Radiation Formula Circuit** — one unlocks dangerous chemicals, the other selects the fractionation formula.
- **JEI / Jade integration** — fractionation recipe pages and machine tooltips. Both optional.
- **Creative tab** — add-on items join the core Infinity What tab; Mekanism's own tab keeps a prefilled creative chemical tank of unknown radiation substance.

### Supported versions

| Minecraft | Loader | Loader version | Mekanism | Java |
|---|---|---|---|---|
| 1.19.2 | Forge | 43.5.x | 10.3.x | 17 |
| 1.20.1 | Forge | 47.4.x | 10.4.x | 17 |
| 1.20.4 | NeoForge | 20.4.x | 10.5.x | 17 |
| 1.21.1 | NeoForge | 21.1.x | 10.7.x | 21 |

Dependencies:

- **Required**: Infinity What core `infwhat` >= 2.0.0 (tested with 2.1.0b)
- **Required**: Mekanism, version range as listed above

### Installation

1. Install Forge or NeoForge for your game version.
2. Put the matching **Infinity What core**, **Mekanism** and **this add-on** jars into `mods`.
3. Launch the game. `config/infwhat_mekanism.toml` is generated on first start.

### Configuration

The config file is `config/infwhat_mekanism.toml`. The main sections and defaults are shown in the Chinese section above; keys are identical.

### Yield model

Environment path: `R` is the sum of removed radiation source magnitudes, `S_max` is the configured strength of the unknown radiation substance (default 5.0).

```text
X = R / S_max

X <= K: Y = X * (2 - X / K)
X >  K: Y = K / (1 + a * ((X - K) / K)^2)

K = 8192 mB, a = 1.5
final Y = min(tank capacity, max(1, round(Y)))
```

Player path: `Y = min(9000, ln(1 + D))`, where `D` is the player's dose in Sv at the moment of right-clicking.

Behaviour: below `K` the yield grows (roughly doubles per cycle), at `K` it is stable, above `K` it decays smoothly but never collapses to zero. The old "64 mB leak turns into 5000+ mB" loop is gone.

### Version notes

- **1.21.1**: Mekanism 10.7 unifies gases, infusions, pigments and slurries into one `Chemical` type. Only the single Infinite Chemical Generator remains; the former infinite infusion and slurry generators were removed, and already-placed blocks will become missing blocks in old worlds. The chemical translation key is `chemical.infwhat_mekanism.unknown_radiation`.
- **1.19.2 / 1.20.1 / 1.20.4**: the infinite infusion and slurry generators are still present; the chemical generator supports gases and pigments.
- **1.20.4 / 1.21.1**: the extractor composite model uses `neoforge:composite` and `neoforge_data`.
- **Dedicated servers**: the chemical generator and radiation sponge are disabled by default; enable `gas_tap.allowOnDedicatedServer` and `radiation_sponge.allowRadiationSpongeOnDedicatedServer`.

### Building from source

```bash
./gradlew build -x createMinecraftArtifacts
```

The jar lands in `build/libs/`. JDK: 17 for 1.19.2 / 1.20.1 / 1.20.4, 21 for 1.21.1.

### License

[Apache License 2.0](LICENSE)
