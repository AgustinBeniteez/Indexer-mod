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

## License
All rights reserved.