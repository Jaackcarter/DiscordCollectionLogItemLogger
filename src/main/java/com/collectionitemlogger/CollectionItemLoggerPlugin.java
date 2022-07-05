package com.collectionitemlogger;

import com.google.common.base.Strings;
import com.google.inject.Provides;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import okhttp3.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
	name = "Discord Collection Log Item Logger"
)
public class CollectionItemLoggerPlugin extends Plugin
{
	private static boolean shouldSendMessage = false;
	private static final String COLLECTION_ITEM_MESSAGE = "New item added to your collection log:";
	private int ticksWaited = 0;

	@Inject
	private Client client;

	@Inject
	private CollectionItemLoggerConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private DrawManager drawManager;

	@Provides
	CollectionItemLoggerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollectionItemLoggerConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{

	}

	@Override
	protected void shutDown() throws Exception
	{

	}

	@Subscribe
	public void onUsernameChanged(UsernameChanged usernameChanged)
	{
		resetState();
	}

	private void resetState()
	{
		shouldSendMessage = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState().equals(GameState.LOGIN_SCREEN))
		{
			resetState();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (shouldSendMessage && config.sendCollectionLogItem() && client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS) != null)
		{
			shouldSendMessage = false;
			String item = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS).getText();
			sendCollectionLogItemMessage(item);
		}

		if (ticksWaited < 2)
		{
			ticksWaited++;
			return;
		}

		ticksWaited = 0;
	}

	private void sendCollectionLogItemMessage(String item)
	{
		String localName = client.getLocalPlayer().getName();

		String messageString = config.collectionLogItemMessage()
				.replaceAll("\\$name", localName);

		DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
		discordWebhookBody.setContent(messageString);
		sendWebhook(discordWebhookBody, config.sendCollectionLogItemScreenshot());
	}

	private void sendWebhook(DiscordWebhookBody discordWebhookBody, boolean sendScreenshot)
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", GSON.toJson(discordWebhookBody));

		if (sendScreenshot)
		{
			sendWebhookWithScreenshot(url, requestBodyBuilder);
		}
		else
		{
			buildRequestAndSend(url, requestBodyBuilder);
		}
	}

	private void sendWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		drawManager.requestNextFrameListener(image ->
		{
			BufferedImage bufferedImage = (BufferedImage) image;
			byte[] imageBytes;
			try
			{
				imageBytes = convertImageToByteArray(bufferedImage);
			}
			catch (IOException e)
			{
				log.warn("Error converting image to byte array", e);
				return;
			}

			requestBodyBuilder.addFormDataPart("file", "image.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes));
			buildRequestAndSend(url, requestBodyBuilder);
		});
	}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.build();
		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Error submitting webhook", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}

	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}
}
