package com.collectionitemlogger;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("collectionitemlogger")
public interface CollectionItemLoggerConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Greeting",
		description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello";
	}

	// Webhook settings
	@ConfigSection(
			name = "Webhook Settings",
			description = "The config for webhook content notifications",
			position = 0,
			closedByDefault = true
	)
	String webhookConfig = "webhookConfig";

	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL",
			description = "The Discord Webhook URL to send screenshots/messages to.",
			section = webhookConfig,
			position = 0
	)
	String webhook();
	// end Webhook settings

	// Collection log settings
	@ConfigSection(
			name = "Collection Log Items",
			description = "The config for collection log item notifications",
			position = 3,
			closedByDefault = true
	)
	String collectionLogItemConfig = "collectionLogItemConfig";

	@ConfigItem(
			keyName = "includeCollectionLogItems",
			name = "Send Collection Log Item Notifications",
			description = "Send messages to discord when you get a new collection log item.",
			section = collectionLogItemConfig
	)
	default boolean sendCollectionLogItem() { return false; }

	default String collectionLogItemMessage() { return "$name has received a new collection log item."; }

	@ConfigItem(
			keyName = "sendCollectionLogItemScreenshot",
			name = "Include screenshots",
			description = "Include a screenshot with the discord notification when you receive a new collection log item.",
			section = collectionLogItemConfig,
			position = 100
	)
	default boolean sendCollectionLogItemScreenshot() {
		return false;
	}
	// end Collection log settings

}
