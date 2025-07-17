# cApplications - Minecraft Application System Plugin

![Version Badge](https://img.shields.io/badge/cApplications-v2.0.0-blue?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.6+-green?style=for-the-badge)
![Java Version](https://img.shields.io/badge/Java-8+-orange?style=for-the-badge)

A comprehensive Minecraft plugin for managing role applications with an intuitive GUI system and automated workflow management.

## 🌟 Features

- **Complete Rewrite**: Fully rewritten from the ground up for better performance and maintainability
- **Enhanced GUI System**: Improved interfaces with pagination and better navigation
- **Role Management**: Support for multiple application roles with customizable requirements
- **Deep LuckPerms Integration**: Seamless permission management with group assignments
- **Application Tracking**: Complete status tracking (in-progress, pending, accepted, denied, expired)
- **Admin Tools**: Comprehensive administration tools with statistics and management features
- **JSON-based Storage**: Improved data structure for better reliability
- **Cooldown System**: Configurable application cooldowns to prevent spam
- **Extensive Configuration**: Much more flexible and comprehensive config system

## 📋 Requirements

- **Minecraft Server**: Spigot/Paper 1.20+
- **Java**: 8 or higher
- **Dependencies**: 
  - LuckPerms (for permission management)
  - JSON Simple (included)

## 🚀 Installation

1. Download the latest release from the [Releases](../../releases) page
2. Place the `cApplications-2.0.0.jar` file in your server's `plugins` folder
3. Ensure LuckPerms is installed and configured
4. Start/restart your server
5. Configure the plugin using `/app reload`

## ⚙️ Configuration

The plugin creates several configuration files:

- `config.yml` - Main plugin settings
- `applications.yml` - Role definitions and application questions

Example configuration:

```yaml
# Application roles and questions
applications:
  moderator:
    display_name: '&b&lModerator'
    description: 'Help maintain order and assist players'
    icon: 'IRON_SWORD'
    requirements:
      - 'permission:capplications.apply.moderator'
      - 'Must have clean punishment history'
    questions:
      - '&eWhat is your in-game username? &7[Bot-Proof]'
      - '&eWhy do you want to become a moderator?'
      - '&eHow would you handle a player breaking the rules?'

# Permission mappings
permissions:
  moderator: 'group.moderator'
  builder: 'group.builder'

# GUI settings
gui:
  titles:
    applications: '&6&lApplications &7- Page {page}'
    manage: '&f♜ &7Manage Application'
```

## 🎮 Commands

| Command | Description |
|---------|-------------|
| `/app` or `/application` | Open the main application menu |
| `/app <role>` | Start application for specific role |
| `/app continue` | Continue an in-progress application |
| `/app cancel` | Cancel your current application |
| `/app status` | Check your application status |
| `/app history` | View your application history |
| `/app roles` | View all available roles |
| `/app accept <player>` | Accept a player's application |
| `/app deny <player> [reason]` | Deny a player's application |
| `/app stats` | View application statistics |
| `/app reload` | Reload the plugin configuration |
| `/app purge [days]` | Purge old application records |
| `/app cooldown <player>` | Check a player's application cooldowns |

## 🔧 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `capplications.use` | Basic plugin usage | true |
| `capplications.apply.*` | Apply for all roles | true |
| `capplications.apply.rolename` | Apply for specific role | true |
| `capplications.manage` | Manage applications | op |
| `capplications.admin` | Administrative commands | op |
| `capplications.notify` | Receive application notifications | op |

## 📁 File Structure

```
plugins/cApplications/
├── config.yml                 # Main configuration
├── applications.yml           # Role definitions
├── applications/              # Application data
│   ├── <uuid>.yml            # Individual application files
├── playerdata/                # Player status data
│   ├── <uuid>.json           # Player status tracking
├── logs/                      # Application logs
│   └── actions.log           # Action history
└── backups/                   # Configuration backups
```

## 🔄 Application Workflow

1. **Application Start**: Player uses `/app <role>` or selects from GUI
2. **Question Process**: Interactive chat-based questionnaire
3. **Submission**: Application automatically saved and submitted
4. **Review**: Admins review via GUI or commands
5. **Decision**: Accept/Deny with optional reason
6. **Notification**: Players notified of decisions
7. **Tracking**: Status tracked in player's history with cooldowns

## 🛠️ Developer Information

- **Author**: Anonventions
- **Version**: 2.0.0
- **API**: Bukkit/Spigot API
- **Language**: Java
- **License**: [Add your license here]

## 🐛 Known Issues

- Character application system needs implementation
- Some permission handling requires refinement

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

- **Issues**: [GitHub Issues](../../issues)
- **Documentation**: [Wiki](../../wiki)
- **Discord**: [[Link](https://discord.gg/SG8jvb9WU5)]

## 📜 License

This project is licensed under the [License Name] - see the [LICENSE](LICENSE) file for details.

---

**Note**: Version 2.0.0 represents a complete overhaul of the original ApplyDCU plugin, transforming it into a much more professional, feature-rich, and maintainable application system.
