# ApplyDCU - Minecraft Application System Plugin

A comprehensive Minecraft plugin for managing role applications with an intuitive GUI system and automated workflow management.

## 🌟 Features

- **Interactive Application System**: GUI-based application process with customizable questions
- **Role Management**: Support for multiple application roles with different requirements
- **Application Tracking**: Complete status tracking (in-progress, accepted, denied, inactive)
- **Auto-Expiration**: Automatic denial of applications after 14 days
- **Inactivity Management**: Automatic role removal after 18 days of inactivity
- **LuckPerms Integration**: Seamless permission management
- **Denial System**: Detailed denial reasons with admin tracking
- **Reload Support**: Hot-reload configuration without server restart

## 📋 Requirements

- **Minecraft Server**: Spigot/Paper 1.16+
- **Java**: 8 or higher
- **Dependencies**: 
  - LuckPerms (for permission management)
  - JSON Simple (included)

## 🚀 Installation

1. Download the latest release from the [Releases](../../releases) page
2. Place the `ApplyDCU.jar` file in your server's `plugins` folder
3. Ensure LuckPerms is installed and configured
4. Start/restart your server
5. Configure the plugin using `/apply reload`

## ⚙️ Configuration

The plugin creates a `config.yml` file with the following structure:

```yaml
gui:
  titles:
    applications: "Applications"
    manage: "Manage Application"

permissions:
  # Define role permissions here
  moderator: "group.moderator"
  admin: "group.admin"

roles:
  # Define available roles and their questions
  moderator:
    questions:
      - "Why do you want to become a moderator?"
      - "How would you handle rule violations?"
      - "What experience do you have with moderation?"

# Application expiry time (in days)
expiry_days: 14
# Inactivity check time (in days)  
inactivity_days: 18
```

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/apply` | `applydcu.use` | Open the main application GUI |
| `/apply <role>` | `applydcu.apply` | Start application for specific role |
| `/apply accept <player>` | `applydcu.admin` | Accept a player's application |
| `/apply deny <player>` | `applydcu.admin` | Deny a player's application |
| `/apply status` | `applydcu.use` | Check your application status |
| `/apply reload` | `applydcu.admin` | Reload the plugin configuration |
| `/apply list` | `applydcu.admin` | View all pending applications |

## 🔧 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `applydcu.use` | Basic plugin usage | true |
| `applydcu.apply` | Submit applications | true |
| `applydcu.admin` | Administrative commands | op |
| `applydcu.manage` | Manage applications | op |

## 📁 File Structure

```
plugins/ApplyDCU/
├── config.yml                 # Main configuration
├── applications/              # Application data
│   ├── <uuid>.yml            # Individual application files
│   └── <uuid>.json           # Player status tracking
└── plugin.yml                # Plugin metadata
```

## 🔄 Application Workflow

1. **Application Start**: Player uses `/apply <role>` or GUI
2. **Question Process**: Interactive chat-based questionnaire
3. **Submission**: Application automatically saved and submitted
4. **Review**: Admins review via GUI or commands
5. **Decision**: Accept/Deny with optional reason
6. **Tracking**: Status tracked in player's history

## 🛠️ Developer Information

- **Author**: Axmon/Amon/Anonventions
- **Version**: 1.0.0 (In Development)
- **API**: Bukkit/Spigot API
- **Language**: Java
- **License**: [Add your license here]

## 🐛 Known Issues

- Character application system needs implementation
- Some permission handling requires refinement
- GUI responsiveness may need optimization

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

- **Issues**: [GitHub Issues](../../issues)
- **Documentation**: [Wiki](../../wiki)
- **Discord**: [Add your Discord server]

## 📜 License

This project is licensed under the [License Name] - see the [LICENSE](LICENSE) file for details.

---

**Note**: This plugin is currently in Version 1 development phase. Some features may be incomplete or require additional testing.
