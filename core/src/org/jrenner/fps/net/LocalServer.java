package org.jrenner.fps.net;

import com.badlogic.gdx.utils.TimeUtils;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.View;
import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.event.ClientEvent;
import org.jrenner.fps.net.packages.ChatMessage;
import org.jrenner.fps.net.packages.ServerMessage;

public class LocalServer extends AbstractServer {
	@Override
	public void sendUpdateToClients() {
		// no clients
	}

	@Override
	public void processBulletHits() {
		// nothing to process
	}

	@Override
	public void processChatMessages() {
		for (ChatMessage chat : chatQueue) {
			//Main.inst.clientEventManager.addEventToQueue(new ClientEvent.ReceivedChatMessage(chat));
			chat.createTime = TimeUtils.millis();
			Log.debug("process chat message: " + chat);
			View.inst.hud.addChatMessage(chat);
		}
		chatQueue.clear();
	}

	@Override
	protected void sendDestroyedEntityMessage(int id) {
		Log.debug("local destroy entity: " + id);
		ServerMessage.DestroyEntity destroyMsg = new ServerMessage.DestroyEntity();
		destroyMsg.id = id;
		String text = "Entity " + id + " has been destroyed";
		ChatMessage chatMsg = new ChatMessage(text);
		chatMsg.createTime = TimeUtils.millis();
		queueChatMessage(chatMsg);
	}
}
