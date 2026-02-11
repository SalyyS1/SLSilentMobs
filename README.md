# SLSilentMobs

**Plugin Private/Silent Mobs & Instanced Loot cho RPG Server Minecraft**

> Phiên bản: 1.0.0 | Tác giả: SalyVn | API: Spigot 1.17+ | Java: 17

---

## 📋 Mục Lục

- [Giới Thiệu](#-giới-thiệu)
- [Yêu Cầu](#-yêu-cầu)
- [Cài Đặt](#-cài-đặt)
- [Tính Năng](#-tính-năng)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Config](#-config)
- [Use Cases RPG](#-use-cases-rpg)

---

## 🎮 Giới Thiệu

**SLSilentMobs** là plugin cho phép server RPG tạo ra trải nghiệm mob cá nhân hóa cho từng người chơi. Lấy cảm hứng từ hệ thống mob của **Wynncraft**, plugin cung cấp 3 tính năng chính:

1. **Silent Mobs (Manual)** — Spawn mob chỉ 1 player thấy
2. **Global Silent Mode** — Toàn bộ mob trên server tự động trở thành private
3. **Silent Item Drops** — Item rơi chỉ người giết thấy (instanced loot)

---

## 📦 Yêu Cầu

| Plugin | Phiên bản | Bắt buộc? |
|--------|-----------|-----------|
| **Spigot/Paper** | 1.17+ | ✅ Bắt buộc |
| **ProtocolLib** | 5.4.0+ | ✅ Bắt buộc |
| **MythicMobs** | 5.9+ | ❌ Tùy chọn (hỗ trợ mob custom) |

---

## 🔧 Cài Đặt

1. Tải file `SLSilentMobs-1.0.0.jar` từ thư mục `target/`
2. Đặt vào thư mục `plugins/` của server
3. Đảm bảo đã cài **ProtocolLib**
4. Khởi động lại server
5. File `config.yml` sẽ tự động tạo trong `plugins/SLSilentMobs/`

---

## ✨ Tính Năng

### 1. 🔒 Silent Mobs (Manual)

Spawn mob mà **chỉ 1 player được chỉ định mới nhìn thấy**. Tất cả player khác trên server hoàn toàn không thấy con mob đó.

- Hỗ trợ cả **Vanilla mob** và **MythicMobs**
- Mob chỉ tấn công (target) player chủ sở hữu
- Tự động despawn khi:
  - Player chủ sở hữu thoát game
  - Hết thời gian timeout
  - Plugin bị disable/reload
- Player mới join server cũng **không thấy** mob (fix bằng ProtocolLib)

### 2. 🌍 Global Silent Mode (Kiểu Wynncraft)

Khi bật chế độ này, **TẤT CẢ mob spawn trên server** tự động trở thành silent:

- Mob spawn tự nhiên/spawner/MythicMobs → chỉ player **gần nhất** (trong radius) nhìn thấy
- **Whitelist**: Mob boss/đặc biệt trong whitelist **KHÔNG bị silent** → visible cho tất cả player
- Hỗ trợ whitelist cả Vanilla EntityType và MythicMobs ID
- Khi player chủ quit → mob tự chuyển cho player gần nhất (reassign)

**Lợi ích:**
- Player farm mob hiệu quả hơn, không tranh nhau
- Boss vẫn visible để cả party/guild đánh chung
- Giảm lag vì mỗi player chỉ render mob của mình

### 3. 💎 Silent Item Drops (Instanced Loot)

Item rơi ra khi mob chết → **chỉ player giết mới nhìn thấy và nhặt được**:

- Player khác hoàn toàn không thấy item trên mặt đất
- Player khác không thể nhặt item
- EXP được cộng trực tiếp cho killer
- Item tự biến mất sau timeout (mặc định 60 giây)
- Có thể áp dụng cho: chỉ silent mob, hoặc tất cả mob trên server

### 4. ⏰ Auto Timeout & Cleanup

- Mob silent tự despawn sau thời gian chỉ định
- Task tự động chạy mỗi 5 giây để cleanup mob hết hạn
- Giới hạn số mob tối đa mỗi player

### 5. 🛡️ Anti-Target

- Mob silent chỉ target player chủ sở hữu
- Không tấn công player khác (dù player khác đứng cạnh)

---

## 🎯 Commands

### Cơ bản

| Command | Mô tả |
|---------|--------|
| `/silentmob spawn <player> <mob> [số lượng] [level] [world] [x] [y] [z]` | Spawn mob silent cho player |
| `/silentmob despawn <player> [mob]` | Xóa mob silent của player |
| `/silentmob despawnall` | Xóa TẤT CẢ mob silent trên server |
| `/silentmob list [player]` | Xem danh sách mob silent đang active |
| `/silentmob reload` | Reload config |

### Global Silent

| Command | Mô tả |
|---------|--------|
| `/silentmob global on` | Bật chế độ Global Silent |
| `/silentmob global off` | Tắt chế độ Global Silent |
| `/silentmob global status` | Xem trạng thái Global Silent |
| `/silentmob global whitelist add <mob>` | Thêm mob vào whitelist (không bị silent) |
| `/silentmob global whitelist remove <mob>` | Xóa mob khỏi whitelist |

### Aliases

Có thể dùng: `/sm` hoặc `/slmob` thay cho `/silentmob`

### Ví dụ

```bash
# Spawn 1 zombie silent cho Steve
/silentmob spawn Steve ZOMBIE

# Spawn 5 skeleton level 10 cho Steve tại tọa độ chỉ định
/silentmob spawn Steve SKELETON 5 10 world 100 65 200

# Spawn MythicMob "DungeonGoblin" cho Steve
/silentmob spawn Steve DungeonGoblin

# Xóa tất cả zombie silent của Steve
/silentmob despawn Steve ZOMBIE

# Bật global silent + whitelist boss
/silentmob global on
/silentmob global whitelist add WorldBoss_Dragon
/silentmob global whitelist add ENDER_DRAGON
```

---

## 🔑 Permissions

| Permission | Mô tả | Mặc định |
|-----------|--------|----------|
| `silentmob.use` | Sử dụng command `/silentmob` | OP |
| `silentmob.admin` | Commands admin: `global`, `reload`, `despawnall` | OP |

---

## ⚙️ Config

File config tại `plugins/SLSilentMobs/config.yml`:

### Cài đặt chung

```yaml
settings:
  max-amount: 50           # Số mob tối đa mỗi lệnh spawn
  default-timeout: 300     # Thời gian tồn tại (giây), 0 = vô hạn
  despawn-on-quit: true    # Despawn khi player thoát game
  mob-target-owner-only: true  # Mob chỉ target chủ sở hữu
  death-notification: true # Thông báo khi mob chết
  max-mobs-per-player: 10  # Giới hạn mob/player
```

### Global Silent Mode

```yaml
global-silent:
  enabled: false           # Bật/tắt chế độ global
  assign-radius: 32        # Bán kính tìm player gần nhất
  whitelist:               # Mob KHÔNG bị silent (boss)
    vanilla:
      - ENDER_DRAGON
      - WITHER
      - WARDEN
      - ELDER_GUARDIAN
    mythicmobs:             # Thêm ID MythicMob vào đây
      - WorldBoss_Dragon
  reassign-on-owner-quit: true   # Chuyển mob cho player khác khi owner quit
  despawn-if-no-player: true     # Despawn nếu không có player nào
```

### Silent Item Drops

```yaml
silent-drops:
  enabled: true
  apply-to-silent-mobs-only: true  # Chỉ áp dụng cho silent mob
  apply-globally: false             # true = TẤT CẢ mob đều silent drop
  drop-timeout: 60                  # Giây trước khi item biến mất
```

### Tin nhắn (Messages)

Tất cả tin nhắn đều tùy chỉnh được trong config. Hỗ trợ mã màu `&` (VD: `&a` = xanh lá, `&c` = đỏ, `&e` = vàng).

---

## 🗡️ Use Cases RPG

### 1. Quest Mob cá nhân
```bash
# NPC giao quest → spawn mob riêng cho player
/silentmob spawn Steve QuestBoss_Goblin 1 5
```
Player khác không thấy mob quest, không can thiệp được.

### 2. Farm Zone
```bash
# Bật global silent → mỗi player farm mob riêng
/silentmob global on
```
Không tranh mob, không KS (Kill Steal). Mỗi player tự thấy và farm mob của mình.

### 3. Dungeon Boss (Whitelist)
```bash
# Boss dungeon visible cho cả party
/silentmob global whitelist add DungeonBoss_Dragon
```
Boss nằm trong whitelist → tất cả player thấy → cả nhóm cùng đánh.

### 4. Instanced Loot
Khi bật `silent-drops`, item rơi chỉ killer thấy → không tranh nhau nhặt đồ.

### 5. PvE Event
```bash
# Spawn nhiều mob cho 1 player trong event
/silentmob spawn Steve EventZombie 10 1 world 100 65 200
```

---

## 📁 Cấu Trúc Source Code

```
src/main/java/vn/saly/silentmobs/
├── SLSilentMobs.java              # Main plugin class
├── command/
│   ├── SilentMobCommand.java      # Command handler
│   └── SilentMobTab.java          # Tab completer
├── global/
│   ├── GlobalSilentManager.java   # Auto-silent all mobs
│   └── GlobalSilentConfig.java    # Whitelist config
├── listener/
│   ├── SilentMobDeathListener.java
│   ├── SilentMobTargetListener.java
│   └── PlayerConnectionListener.java
├── loot/
│   ├── SilentDropManager.java     # Instanced loot
│   └── SilentDropListener.java
├── manager/
│   └── SilentMobManager.java      # Central mob manager
├── model/
│   └── SilentMob.java             # Mob data model
├── task/
│   └── MobTimeoutTask.java        # Auto-cleanup
└── visibility/
    └── EntityHider.java           # ProtocolLib packet hiding
```

---

## 🔧 Build

```bash
mvn clean package
```

File JAR sẽ nằm tại `target/SLSilentMobs-1.0.0.jar`

---

## 📄 License

Plugin được phát triển bởi **SalyVn** cho server RPG.
