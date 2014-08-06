package org.jrenner.fps.net.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.Player;
import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.event.ClientEvent;
import org.jrenner.fps.net.NetManager;
import org.jrenner.fps.net.packages.BulletPackage;
import org.jrenner.fps.net.packages.ChatMessage;
import org.jrenner.fps.net.packages.ClientRequest;
import org.jrenner.fps.net.packages.CommandPackage;
import org.jrenner.fps.net.packages.EntityInfoRequest;
import org.jrenner.fps.net.packages.ServerMessage;
import org.jrenner.fps.net.packages.ServerUpdate;

import java.io.IOException;

public abstract class AbstractClient {
	public Player player;
	public int playerId = -1;
	public final Array<ServerUpdate> incomingUpdates = new Array<>();

	public abstract void connectToServer();

	public abstract void disconnectedFromServer();

	public void assignPlayerToId(int id) {
		playerId = id;
		DynamicEntity ent = (DynamicEntity) Entity.getEntityById(id);
		if (ent != null) {
			player = new Player();
			player.entity = ent;
			if (Main.inst.client == null) throw new GdxRuntimeException("bad");
			ent.setPlayer(Main.inst.client.player);
		} else {
			//throw new GdxRuntimeException("bad");
			Log.error("couldn't find entity for assignPlayerToId: " + id);
		}
	}

	public abstract void sendChatMessage(ChatMessage chat);

	public abstract void requestResetPosition();

	public abstract void update();
}
