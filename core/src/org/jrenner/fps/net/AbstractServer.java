package org.jrenner.fps.net;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import org.jrenner.fps.Direction;
import org.jrenner.fps.GameWorld;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.Player;
import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.event.ServerEvent;
import org.jrenner.fps.event.ServerEventManager;
import org.jrenner.fps.net.client.ClientUpdate;
import org.jrenner.fps.net.packages.ChatMessage;
import org.jrenner.fps.net.packages.CommandPackage;
import org.jrenner.fps.utils.Pooler;

import static org.jrenner.fps.net.packages.CommandPackage.BACK;
import static org.jrenner.fps.net.packages.CommandPackage.DOWN;
import static org.jrenner.fps.net.packages.CommandPackage.FORWARD;
import static org.jrenner.fps.net.packages.CommandPackage.JUMP;
import static org.jrenner.fps.net.packages.CommandPackage.SHOOT;
import static org.jrenner.fps.net.packages.CommandPackage.STRAFE_LEFT;
import static org.jrenner.fps.net.packages.CommandPackage.STRAFE_RIGHT;
import static org.jrenner.fps.net.packages.CommandPackage.UP;

public abstract class AbstractServer {
	protected Array<ClientUpdate> updateQueue = new Array<>();
	long lastFrameTime = System.currentTimeMillis();
	long accumulatedTime;
	public int tickNum;
	public static final long tickInterval = 50; // ms
	public static final String serverName = "Test Server";
	public static final String serverMsg = "Welcome to the test server";
	protected final Array<Integer> destroyedEntityQueue = new Array<>();
	protected final Array<ChatMessage> chatQueue = new Array<>();
	public ServerEventManager serverEventManager = new ServerEventManager();

	public void handleClientUpdate(ClientUpdate clientUpdate) {
		synchronized (updateQueue) {
			updateQueue.add(clientUpdate);
		}
	}

	private Vector3 transVec = new Vector3();
	private Vector3 rotVec = new Vector3();

	/** Server handles incoming data from clients here */
	public void processClientUpdates() {
		//Log.debug("processing client updates: " + updateQueue.size);
		synchronized (updateQueue) {
			for (ClientUpdate update : updateQueue) {
				if (update == null) {
					Log.debug("null update! skipping");
					return;
				}
				int id = update.entityId;
				Entity ent = Entity.getEntityById(id);
				if (ent == null) {
					Log.debug("entity is null, not processing update");
					return;
				}
				CommandPackage cmd = update.cmdPack;
				transVec.setZero();
				rotVec.setZero();

				int bits = cmd.commandBits;

				// Translation
				if ((bits & FORWARD) != 0) addTranslation(Direction.Translation.Forward);
				if ((bits & BACK) != 0) addTranslation(Direction.Translation.Back);
				if ((bits & STRAFE_LEFT) != 0) addTranslation(Direction.Translation.Left);
				if ((bits & STRAFE_RIGHT) != 0) addTranslation(Direction.Translation.Right);
				if ((bits & UP) != 0) addTranslation(Direction.Translation.Up);
				if ((bits & DOWN) != 0) addTranslation(Direction.Translation.Down);

				// Rotation
				ent.setYawPitchRoll(cmd.yaw, cmd.pitch, cmd.roll);

				if (ent instanceof DynamicEntity) {
					DynamicEntity dynEnt = (DynamicEntity) ent;
					if ((bits & JUMP) != 0) {
						dynEnt.jump();
					}
					if ((bits & SHOOT) != 0) {
						dynEnt.startShoot();
					} else {
						dynEnt.stopShoot();
					}
				}
				if (!transVec.isZero()) {
					if (ent.isFlyingEntity()) {
						ent.setRelativeDestination(transVec);
					} else {
						ent.setRelativeDestinationByYaw(transVec);
					}
				} else {
					ent.setDestination(null);
				}
			}
			updateQueue.clear();
		}
	}

	public abstract void sendUpdateToClients();

	private void addTranslation(Direction.Translation translation) {
		transVec.add(translation.vector);
	}

	public void update() {
		processDestroyedEntities();
		processChatMessages();
		maintainMonsterCount();
		serverEventManager.process();
		if (Main.isLocalServer()) {
			return;
		}
		processClientUpdates();
		long currentTime = TimeUtils.millis();
		long delta = currentTime - lastFrameTime;
		lastFrameTime = currentTime;
		accumulatedTime += delta;
		//Log.debug("server time delta: " + delta);
		while (accumulatedTime >= tickInterval) {
			tick();
			accumulatedTime -= tickInterval;
		}
	}

	public void tick() {
		tickNum++;
		sendUpdateToClients();
		processBulletHits();
	}

	private void maintainMonsterCount() {
		synchronized (Entity.list) {
			if (Entity.list.size < 20) {
				serverEventManager.addEventToQueue(new ServerEvent.CreateMonster());
			}
		}
	}

	protected final Array<Vector3> bulletHitQueue = new Array<>();

	public void queueBulletHit(Vector3 loc) {
		synchronized (bulletHitQueue) {
			Vector3 bulletHitLoc = Pooler.v3();
			bulletHitQueue.add(bulletHitLoc.set(loc));
		}
	}

	public abstract void processBulletHits();

	public void queueDestroyedEntity(int entityId) {
		synchronized (destroyedEntityQueue) {
			if (!destroyedEntityQueue.contains(entityId, false)) {
				destroyedEntityQueue.add(entityId);
			}
		}
	}

	public void processDestroyedEntities() {
		synchronized (destroyedEntityQueue) {
			for (int id : destroyedEntityQueue) {
				sendDestroyedEntityMessage(id);
			}
			destroyedEntityQueue.clear();
		}
	}

	protected abstract void sendDestroyedEntityMessage(int id);

	public void queueChatMessage(ChatMessage chatMsg) {
		synchronized (chatQueue) {
			chatQueue.add(chatMsg);
		}
	}

	public abstract void processChatMessages();

	public void sendPlayerConnectedChatMessage(int id) {
		ChatMessage chat = new ChatMessage("Player " + id + " has connected");
		chat.playerId = -1;
		chat.createTime = TimeUtils.millis();
		queueChatMessage(chat);
	}

	public void setupGame() {
		for (int i = 0; i < 20; i++) {
			createMonster();
		}
	}

	public void createMonster() {
		Log.debug("create monster");
		DynamicEntity ent = DynamicEntity.createEntityNoId(Entity.EntityGraphicsType.Decal);
		Entity.assignEntityID(ent);
		ent.setPosition(MathUtils.random(20f, 100f), 50f, MathUtils.random(20f, 100f));
	}

	public DynamicEntity createPlayer() {
		DynamicEntity playerEnt = DynamicEntity.createEntityNoId(Entity.EntityGraphicsType.Model);
		Entity.assignEntityID(playerEnt);
		Player player = new Player();
		player.entity = playerEnt;
		playerEnt.setPlayer(new Player());
		return playerEnt;
	}

	public void resetPlayerPosition(int id) {
		DynamicEntity playerEnt = (DynamicEntity) Entity.getEntityById(id);
		float x = MathUtils.random(0f, GameWorld.WORLD_WIDTH);
		float y = MathUtils.random(100f, 200f);
		float z = MathUtils.random(0f, GameWorld.WORLD_DEPTH);
		playerEnt.setPosition(x, y, z);
	}
}
