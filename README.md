# NullBrand

**NullBrand** is an ultra-lightweight proxy plugin for **Velocity** that removes default proxy branding and provides a clean, configurable MOTD with full HEX color support.

Designed to be minimal, fast, and invisible.

---

## ✨ Features

- 🔒 **F3 Branding Override**
  - Replace default proxy branding
  - Fully configurable
  - Supports legacy (`&`) and HEX colors

- 📝 **Custom MOTD**
  - Single or multi-line MOTD
  - HEX (`#RRGGBB`) and legacy color support
  - Applied instantly on ping

- 🔁 **Reload Command**
  - Reload configuration without restarting
  - Command: `/nullbrand reload`
  - Permission-based

- ⚡ **Extremely Lightweight**
  - No schedulers
  - No background tasks
  - No reflection
  - Minimal memory usage

---

## 📦 Requirements

- **Velocity** 3.3.0+
- **WaterFall, BungeeCord** 1.16+
- **Java** 17+

---

## 📂 Installation

1. Download the latest `NullBrand.jar`
2. Place it in your Velocity `plugins/` folder
3. Start the proxy once to generate the config
4. Edit `config.yml` to your liking
5. Reload using `/nullbrand reload`

---

## ⚙️ Configuration (`config.yml`)

```yml
branding:
  enabled: true
  text: "<#6affff>NullBrand</#6affff> &7| &fInvisible Proxy"

motd:
  enabled: true
  lines:
    - "<#6affff>NullBrand</#6affff>"
    - "&7Secure • Fast • Invisible"
