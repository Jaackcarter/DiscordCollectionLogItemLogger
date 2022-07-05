package com.collectionitemlogger;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CollectionItemLoggerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CollectionItemLoggerPlugin.class);
		RuneLite.main(args);
	}
}