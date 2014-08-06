package org.jrenner.fps.net.client;

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
import org.jrenner.fps.Tools;
import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.event.ClientEvent;
import org.jrenner.fps.net.NetManager;
import org.jrenner.fps.net.packages.BulletPackage;
import org.jrenner.fps.net.packages.ChatMessage;
import org.jrenner.fps.net.packages.ClientRequest;
import org.jrenner.fps.net.packages.CommandPackage;
import org.jrenner.fps.net.packages.EntityInfoRequest;
import org.jrenner.fps.net.packages.EntityUpdate;
import org.jrenner.fps.net.packages.ServerMessage;
import org.jrenner.fps.net.packages.ServerUpdate;

import java.io.IOException;

public class NetClient extends AbstractClient {
	private Client client;
	public ClientUpdate clientUpdate = new ClientUpdate();
	/** will be set to server's tickInterval after connecting */
	public float tickInterval;
	/** server tick number */
	public int tickNum;
	/** this client's input tick number, not related to server tick */
	public int inputTick;
	public int ping;
	private int pingRefreshInterval = 1000; // ms
	private long lastPingUpdateTime;
	private int clientTimeout = 5000; // ms
	public int playerId = -1;
	public final Array<ServerUpdate> incomingUpdates = new Array<>();

	public NetClient() {
		client = new Client(NetManager.writeBufferSize, NetManager.objectBufferSize);
		client.addListener(createListener());
		NetManager.registerKryoClasses(client.getKryo());
		client.start();
		connect();
	}

	public void sendClientUpdateToServer(CommandPackage cmdPack) {
		if (player != null) {
			clientUpdate.reset();
			clientUpdate.entityId = player.entity.id;
			clientUpdate.cmdPack = cmdPack;
			clientUpdate.inputTick = inputTick++;
			client.sendUDP(clientUpdate);
		}
		// update ping
		long now = TimeUtils.millis();
		if (now - lastPingUpdateTime >= pingRefreshInterval) {
			lastPingUpdateTime = now;
			client.updateReturnTripTime();
		}
	}

	public void connect() {
		try {
			client.connect(clientTimeout, NetManager.host, NetManager.tcpPort, NetManager.udpPort);
		} catch (IOException e) {
			throw new GdxRuntimeException(e);
		}
	}

	public void handleConnect(Connection conn) {
		connectToServer();
	}

	private void handleDisconnect(Connection conn) {
	}

	@Override
	public void connectToServer() {
		Log.debug("connected to server");
		client.sendTCP(ClientRequest.CreateNewPlayerAssignID);
		requestServerInfo();
		requestLevelGeometry();
	}

	public void requestServerInfo() {
		client.sendTCP(ClientRequest.RequestServerInfo);
	}

	public void requestLevelGeometry() {
		client.sendTCP(ClientRequest.RequestLevelGeometry);
	}


	@Override
	public void disconnectedFromServer() {
		Log.debug("disconnected from server");
	}

	private long highestTickNumUpdateReceived;
	private long lastServerUpdateTime;

	private void handleReceived(Connection conn, Object obj) {
		//Log.debug("received from server: " + obj);
		if (obj == null) {
			Log.error("received null object from kryonet");
		}
		if (obj instanceof ServerMessage.AssignPlayerEntityId) {
			handleAssignPlayerEntityId((ServerMessage.AssignPlayerEntityId) obj);
		}
		else if (obj instanceof ServerMessage.DestroyEntity) {
			ServerMessage.DestroyEntity destroy = (ServerMessage.DestroyEntity) obj;
			Main.inst.clientEventManager.addEventToQueue(new ClientEvent.DestroyEntity(destroy.id));
		}
		else if (obj instanceof ServerMessage.ServerInfo) {
			handleServerInfo((ServerMessage.ServerInfo) obj);
		}
		else if (obj instanceof ServerMessage.LevelGeometry) {
			handleLevelGeometry((ServerMessage.LevelGeometry) obj);
		}
		else if (obj instanceof ChatMessage) {
			ChatMessage chat = (ChatMessage) obj;
			// creation time based on when client receives it, override the value
			chat.createTime = TimeUtils.millis();
			Main.inst.clientEventManager.addEventToQueue(new ClientEvent.ReceivedChatMessage(chat));
		}
		else if (obj instanceof ServerUpdate) {
			ServerUpdate serverUpdate = (ServerUpdate) obj;
			if (serverUpdate.tickNum <= highestTickNumUpdateReceived) {
				Log.debug("discarding outdated serverupdate, ticknum: " + serverUpdate.tickNum + ", current tick: " + highestTickNumUpdateReceived);
				return;
			}
			/*long now = TimeUtils.millis();
			long elapsed = now - lastServerUpdateTime;
			lastServerUpdateTime = now;
			Log.debug("server update elapsed time (tick: " + serverUpdate.tickNum + "): " + elapsed);*/
			handleUpdateFromServer(serverUpdate);
		}
		else if (obj instanceof EntityInfoRequest.Response) {
			handleEntityInfoRequestResponse((EntityInfoRequest.Response) obj);
		}
		else if (obj instanceof FrameworkMessage.Ping) {
			if (((FrameworkMessage.Ping) obj).isReply) {
				ping = client.getReturnTripTime();
				//System.out.println("new ping: " + ping);
			}
		}
		else if (obj instanceof BulletPackage) {
			handleBulletPackage((BulletPackage) obj);
		} else {
			//Log.debug("unhandled object from server: " + obj);
			if (!obj.getClass().getName().contains("com.esotericsoftware.kryonet")) {
				throw new GdxRuntimeException("unhandled object from server: " + obj);
			}
		}
	}

	private void handleIdle(Connection conn) {}


	private Listener createListener() {
		return new Listener() {
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
	}

	// Process Incoming

	private void handleUpdateFromServer(ServerUpdate upd) {
		synchronized (incomingUpdates) {
			incomingUpdates.add(upd);
		}
	}

	@Override
	public void update() {
		processServerUpdateQueue();
	}

	private void processServerUpdateQueue() {
		synchronized (incomingUpdates) {
			for (ServerUpdate upd : incomingUpdates) {
				upd.applyUpdates();
			}
			incomingUpdates.clear();
		}
	}

	private void handleAssignPlayerEntityId(ServerMessage.AssignPlayerEntityId assign) {
		Main.inst.clientEventManager.addEventToQueue(new ClientEvent.AssignPlayerId(assign.id));
	}

	private static final Vector3 tmp = new Vector3();

	private void handleBulletPackage(BulletPackage bpack) {
		//Log.debug("client creating bullets: " + bpack.locations.length);
		for (Vector3 hitLoc : bpack.locations) {
			//Log.debug("\tlocation: " + Tools.fmt(hitLoc));
			Main.inst.clientEventManager.addEventToQueue(new ClientEvent.CreateBullet(hitLoc.x, hitLoc.y, hitLoc.z));
		}
	}

	private void handleServerInfo(ServerMessage.ServerInfo info) {
		this.tickNum = info.tickNum;
		this.tickInterval = info.tickInterval;
		Log.info("Server Name: " + info.serverName);
		Log.info("Server Message: " + info.serverMsg);
		Log.info("Set tick from server: " + info.tickNum);
	}

	private void handleEntityInfoRequestResponse(EntityInfoRequest.Response resp) {
		Log.debug("EntityInfo response: " + resp.id);
		Main.inst.clientEventManager.addEventToQueue(new ClientEvent.CreateEntity(resp.id, resp.graphicsType));
	}

	private void handleLevelGeometry(ServerMessage.LevelGeometry geo) {
		Log.debug("Server send level static geometry information, size: " + geo.staticPieces.length);
		Main.inst.clientEventManager.addEventToQueue(new ClientEvent.CreateLevelStatics(geo.staticPieces));
	}

	// Send Outgoing

	private IntIntMap entityIdInfoRequestTicks = new IntIntMap();
	private int requestInterval = 10; // server ticks

	public void requestEntityInfo(int id) {
		int lastRequestTime = entityIdInfoRequestTicks.get(id, -1);
		if (lastRequestTime == -1 || (tickNum - lastRequestTime) > requestInterval) {
			EntityInfoRequest req = new EntityInfoRequest();
			req.id = id;
			client.sendTCP(req);
			entityIdInfoRequestTicks.put(id, tickNum);
			Log.debug("sent entity info request for id: " + id);
		} else {
			Log.error("Already requested info for entity, id: " + id + ". Must wait a bit");
		}
	}

	@Override
	public void sendChatMessage(ChatMessage chat) {
		client.sendTCP(chat);
	}

	@Override
	public void requestResetPosition() {
		client.sendTCP(ClientRequest.RequestResetPlayerPosition);
	}
}
