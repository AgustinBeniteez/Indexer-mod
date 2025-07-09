# Indexer Mod

## Description
Indexer is a mod for Minecraft 1.20.1 that helps you organize and manage your items. This automation system allows you to filter and automatically distribute the contents of your Chests/Containers, eliminating the need to manually sort your resources.

With Indexer, you can deposit all your items in a central point and the system will take care of distributing them to the appropriate containers according to the filters you have configured, saving you time and keeping your base perfectly organized!

## Developer
- [AgustinBenitez](https://github.com/Agustinbeniteez)

## Main Components

### Indexer Controller
The brain of the entire system. Place this block at the center of your storage network and connect it to a DropBox (special container) where you'll deposit the items you want to sort. The Controller can detect connectors up to 250 blocks away, allowing you to create massive and complex storage systems.

### Indexer Pipe
These blocks connect the Indexer Controller with the Indexer Connectors, forming the distribution network. They are easy to craft and yield 10 units per recipe, allowing you to create extensive distribution systems with few resources.

### Indexer Connector
Place these blocks next to your chests, barrels, furnaces, or other containers. Each Connector can be configured with a specific filter to determine which items it will accept. Items that match the filter will be automatically sent from the Controller to the connected container.

### DropBox
A special container with 54 slots (double that of a normal chest) where you can deposit all the items you want to sort. The Indexer Controller will extract the items from here and distribute them according to the configured filters.

### Speed Upgrades
The mod includes two levels of speed upgrades:
- **Basic Upgrade**: Allows the Controller to transfer up to 4 items at a time per cycle. Crafted with gold ingots in the corners, redstone dust in the remaining slots, and an Indexer Controller in the center.
- **Advanced Upgrade**: Allows transferring up to 16 items at a time per cycle. Crafted by surrounding a Basic Upgrade with diamonds.

## Special Features

### Furnace Compatibility
The system automatically detects furnaces and can refill fuel (coal or charcoal) without additional configuration.

### Wide Range
The Controller can detect and manage connectors up to 250 blocks away, allowing you to create storage systems that span your entire base.

### No Limits
There is no restriction on the number of connectors you can use in your system, allowing you to expand it as much as you need.

### Intuitive Interface
Easily configure your connector filters with a simple and straightforward interface. Just place the item you want to filter in the available slot.

## How to Use

1. **Basic Setup**:
   - Place the Indexer Controller at the center of your system.
   - Connect Indexer Pipes from the controller to the connectors.
   - Place Indexer Connectors next to your containers.

2. **Filter Configuration**:
   - Right-click on each Indexer Connector to open its interface.
   - Place the item you want to filter in the available slot.

3. **Usage**:
   - Deposit items in the Indexer Controller.
   - Items will be automatically sent to connected containers according to the configured filters.
   - If an item does not match any filter, it will remain in the controller.

## Requirements
- Minecraft 1.20.1
- Forge 47.1.0 or higher

## Installation
1. Install Minecraft Forge for version 1.20.1.
2. Download the mod's .jar file.
3. Place the .jar file in the "mods" folder of your Minecraft installation.
4. Start Minecraft with the Forge profile.

## Download New Versions
Download the latest versions of the mod at: `https://www.curseforge.com/minecraft/mc-mods/indexer`

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

The MIT License is a permissive license that allows you to do almost anything with this project, like making and distributing closed source versions, as long as you include the original copyright and license notice.

## Issues

If you find any problem or have a suggestion to improve the mod, please create an issue in the GitHub repository by following these steps:

1. Go to the [project repository](https://github.com/Agustinbeniteez/Indexer-mod) on GitHub.
2. Click on the "Issues" tab.
3. Click on the "New Issue" button.
4. Provide a descriptive title for the issue.
5. Describe the problem or suggestion in detail. Include:
   - Version of the mod you are using
   - Minecraft and Forge version
   - Steps to reproduce the problem (if it's a bug)
   - Screenshots or logs if possible
6. Click on "Submit new issue".

Your feedback is very important to improve the mod. Thank you for contributing!

## Contributing

Contributions to the Indexer Mod are welcome! If you'd like to contribute to the project, here's how you can do it:

1. **Fork the Repository**: Click the "Fork" button at the top right of the [project repository](https://github.com/Agustinbeniteez/Indexer-mod) to create your own copy.

2. **Clone Your Fork**: Clone your forked repository to your local machine.
   ```
   git clone https://github.com/YOUR-USERNAME/Indexer-mod.git
   ```

3. **Create a Branch**: Create a new branch for your feature or bugfix.
   ```
   git checkout -b feature/your-feature-name
   ```

4. **Make Changes**: Implement your changes, following the existing code style.

5. **Test Your Changes**: Make sure your changes work correctly and don't break existing functionality.

6. **Commit Your Changes**: Commit your changes with a clear and descriptive commit message.
   ```
   git commit -m "v1.0.#"
   ```

7. **Push to Your Fork**: Push your changes to your forked repository.
   ```
   git push origin feature/your-feature-name
   ```

8. **Create a Pull Request**: Go to the original repository and click on "New Pull Request". Select your fork and the branch you created, then submit the pull request with a clear description of your changes.

### Contribution Guidelines

- Follow the existing code style and conventions
- Write clear, descriptive commit messages
- Include comments in your code when necessary
- Update documentation if needed
- Make sure your code works with Minecraft 1.20.1 and Forge 47.1.0+

Thank you for helping improve the Indexer Mod!