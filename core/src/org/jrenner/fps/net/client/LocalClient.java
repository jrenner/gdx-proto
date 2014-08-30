package org.jrenner.fps.net.client;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.TimeUtils;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.View;
import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.net.LocalServer;
import org.jrenner.fps.net.packages.ChatMessage;

/** LocalClient is basically a mock client used in single-player, pairs with LocalServer */
public class LocalClient extends AbstractClient {
	private LocalServer server;
	@Override
	public void connectToServer() {
		if (!Main.isLocalServer()) {
			throw new GdxRuntimeException("cannot create local client without local server. server type is: " + Main.serverType);
		}
		server = (LocalServer) Main.inst.server;
		DynamicEntity playerEnt = server.createPlayer();
		assignClientPlayerToId(playerEnt.id);
		Log.debug("connected, created player: " + playerEnt.id);
		server.sendPlayerConnectedChatMessage(playerEnt.id);
	}

	@Override
	public void disconnectedFromServer() {
		if (player != null) {
			player.entity.destroy();
		}
		Log.debug("disconnected");
	}

	@Override
	public void sendChatMessage(ChatMessage chat) {
		chat.playerId = 0;
		chat.createTime = TimeUtils.millis();
		View.inst.hud.addChatMessage(chat);
	}

	@Override
	public void requestResetPosition() {
		server.resetPlayerPosition(playerId);
	}

	@Override
	public void update() {
		// nothing
	}
}
