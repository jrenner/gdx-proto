package org.jrenner.fps.net;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.jrenner.fps.LevelBuilder;
import org.jrenner.fps.LevelStatic;
import org.jrenner.fps.Log;

import java.io.IOException;

import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.net.client.ClientUpdate;
import org.jrenner.fps.net.packages.BulletPackage;
import org.jrenner.fps.net.packages.ChatMessage;
import org.jrenner.fps.net.packages.ClientRequest;
import org.jrenner.fps.net.packages.EntityInfoRequest;
import org.jrenner.fps.net.packages.ServerMessage;
import org.jrenner.fps.net.packages.ServerUpdate;
import org.jrenner.fps.utils.Pooler;

public class NetServer extends AbstractServer {
	private Server server;
	public Array<Connection> connectedClients = new Array<>();
	private ObjectMap<Connection, ClientInfo> clientMap;
	public Array<Integer> playerEntityIds = new Array<>();
	public static int lagMin;
	public static int lagMax;
	public static boolean simulateLag = false;


	public NetServer() {
		clientMap = new ObjectMap<>();
		server = new Server(NetManager.writeBufferSize, NetManager.objectBufferSize);
		NetManager.registerKryoClasses(server.getKryo());
		try {
			server.bind(NetManager.tcpPort, NetManager.udpPort);
			server.start();
			server.addListener(createListener(simulateLag));
			Log.debug("server is listening at: " + NetManager.host + ":" + NetManager.tcpPort);
		} catch (IOException e) {
			Log.error(e.toString());
			throw new GdxRuntimeException(e);
		}
	}

	private void handleConnect(Connection conn) {
		Log.debug("connected to server: " + conn);
		ClientInfo clientInfo = new ClientInfo();
		clientMap.put(conn, clientInfo);
		connectedClients.add(conn);
	}

	private void handleDisconnect(Connection conn) {
		Log.debug("disconnected from server: " + conn);
		ClientInfo clientInfo = clientMap.get(conn);
		Entity.destroy(clientInfo.playerEntityId);
		connectedClients.removeValue(conn, true);
	}

	private void handleReceived(Connection conn, Object obj) {
		//AsLog.debug("received from client (" + conn + "): " + obj);
		if (obj instanceof ClientRequest) {
			handleClientRequest(conn, (ClientRequest) obj);
		} else if (obj instanceof ClientUpdate) {
			ClientUpdate clientUpdate = (ClientUpdate) obj;
			ClientInfo clientInfo = clientMap.get(conn);
			clientInfo.lastInputTick = clientUpdate.inputTick;
			handleClientUpdate(clientUpdate);
		} else if (obj instanceof EntityInfoRequest) {
			handleEntityInfoRequest(conn, (EntityInfoRequest) obj);
		} else if (obj instanceof ChatMessage) {
			ChatMessage chat = (ChatMessage) obj;
			chat.playerId = clientMap.get(conn).playerEntityId;
			queueChatMessage((ChatMessage) obj);
		} else {
			if (!obj.getClass().getName().contains("com.esotericsoftware.kryonet")) {
				String err = "unhandled object from client to server: " + obj;
				Log.debug(err);
				queueChatMessage(new ChatMessage(err));
			}
		}
	}

	private void handleIdle(Connection conn) {}

	@Override
	public void update() {
		super.update();
	}

	private Listener createListener(boolean lagListener) {
		Listener listener = new Listener() {
			@Override
			public void connected(Connection connection) {
				handleConnect(connection);
			}

			@Override
			public void disconnected(Connection connection) {
				handleDisconnect(connection);
			}

			@Override
			public void received(Connection connection, Object object) {
				handleReceived(connection, object);
			}

			@Override
			public void idle(Connection connection) {
				handleIdle(connection);
			}
		};
		if (lagListener) {
			return new Listener.LagListener(lagMin, lagMax, listener);
		} else {
			return listener;
		}
	}

	@Override
	public void sendUpdateToClients() {
		ServerUpdate serverUpdate = ServerUpdate.createServerUpdate();
		for (Connection client : connectedClients) {
			ClientInfo clientInfo = clientMap.get(client);
			serverUpdate.playerInputTick = clientInfo.lastInputTick;
			client.sendUDP(serverUpdate);
		}
		serverUpdate.free();
	}

	public void handleClientRequest(Connection conn, ClientRequest req) {
		ClientInfo info = clientMap.get(conn);
		switch (req) {
			case CreateNewPlayerAssignID:
				DynamicEntity player = createPlayer();
				ServerMessage.AssignPlayerEntityId playerAssignment = new ServerMessage.AssignPlayerEntityId();
				playerAssignment.id = player.id;
				info.playerEntityId = player.id;
				Log.debug("server creating new player, assigning id: " + player.id);
				sendPlayerConnectedChatMessage(player.id);
				playerEntityIds.add(player.id);
				conn.sendTCP(playerAssignment);
				break;
			case RequestServerInfo:
				ServerMessage.ServerInfo serverInfo = new ServerMessage.ServerInfo();
				serverInfo.tickNum = tickNum;
				serverInfo.tickInterval = tickInterval;
				serverInfo.serverName = serverName;
				serverInfo.serverMsg = serverMsg;
				conn.sendTCP(serverInfo);
				break;
			case RequestLevelGeometry:
				ServerMessage.LevelGeometry levelGeometry = new ServerMessage.LevelGeometry();
				levelGeometry.staticPieces = LevelBuilder.staticPieces.toArray(LevelStatic.class);
				conn.sendTCP(levelGeometry);
				break;
			case RequestResetPlayerPosition:
				ClientInfo clientInfo = clientMap.get(conn);
				Log.debug("resetting player position due to client request, id: " + clientInfo.playerEntityId);
				resetPlayerPosition(clientInfo.playerEntityId);
				break;
			default:
				throw new GdxRuntimeException("unhandled");
		}
	}

	public void handleEntityInfoRequest(Connection conn, EntityInfoRequest req) {
		Entity ent = Entity.getEntityById(req.id);
		// TODO in the future put more fields into the response
		EntityInfoRequest.Response resp = new EntityInfoRequest.Response();
		resp.isPlayer = isPlayerEntity(ent.id);
		resp.id = ent.id;
		if (isPlayerEntity(ent.id)) {
			resp.graphicsType = Entity.EntityGraphicsType.Model;
		} else {
			resp.graphicsType = Entity.EntityGraphicsType.Decal;
		}
		conn.sendTCP(resp);
	}

	public boolean isPlayerEntity(int id) {
		return playerEntityIds.contains(id, false);
	}

	@Override
	public void processBulletHits() {
		synchronized (bulletHitQueue) {
			BulletPackage bpack = new BulletPackage();
			bpack.locations = new Vector3[bulletHitQueue.size];
			for (int i = 0; i < bulletHitQueue.size; i++) {
				bpack.locations[i] = bulletHitQueue.get(i);
			}
			server.sendToAllUDP(bpack);
			for (Vector3 v3 : bulletHitQueue) {
				Pooler.free(v3);
			}
		}
		bulletHitQueue.clear();
	}

	@Override
	public void processChatMessages() {
		synchronized (chatQueue) {
			for (ChatMessage msg : chatQueue) {
				server.sendToAllTCP(msg);
			}
			chatQueue.clear();
		}
	}

	@Override
	protected void sendDestroyedEntityMessage(int id) {
		ServerMessage.DestroyEntity destroyMsg = new ServerMessage.DestroyEntity();
		destroyMsg.id = id;
		server.sendToAllTCP(destroyMsg);
		String text = "Entity " + id + " has been destroyed";
		ChatMessage chatMsg = new ChatMessage(text);
		queueChatMessage(chatMsg);
	}
}
