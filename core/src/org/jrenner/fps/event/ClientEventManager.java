package org.jrenner.fps.event;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jrenner.fps.LevelBuilder;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.Tools;
import org.jrenner.fps.View;
import org.jrenner.fps.effects.BulletHit;
import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.entity.Entity;

import static org.jrenner.fps.event.ClientEvent.*;

public class ClientEventManager {
	private final Array<ClientEvent> eventQueue = new Array<>();
	private static final Vector3 tmp = new Vector3();

	/** called from the main game thread, not the network thread, should be thread-safe with most game logic operations */
	public void process() {
		synchronized (eventQueue) {
			for (ClientEvent event : eventQueue) {
				if (event instanceof AssignPlayerId) {
					AssignPlayerId assignPlayer = (AssignPlayerId) event;
					Main.inst.client.assignClientPlayerToId(assignPlayer.id);
					Log.debug("Player assigned to entity with ID: " + assignPlayer.id);
				} else if (event instanceof CreateEntity) {
					CreateEntity create = (CreateEntity) event;
					DynamicEntity dynEnt = (DynamicEntity) Entity.getEntityById(create.id);
					if (dynEnt != null) {
						Log.debug("entity already exists, aborting.");
					} else {
						dynEnt = DynamicEntity.createEntity(create.id, create.isPlayer, create.graphicsType);
						Log.debug("created entity: " + dynEnt.id);
					}
				} else if (event instanceof DestroyEntity) {
					DestroyEntity destroy = (DestroyEntity) event;
					Entity ent = Entity.getEntityById(destroy.id);
					if (ent == null) {
						Log.error("couldn't find entity to destroy, id: " + destroy.id);
					} else {
						ent.destroy();
						Log.debug("destroyed entity, id: " + destroy.id);
					}
				} else if (event instanceof CreateBullet) {
					// if this is both client and server, don't duplicate the bullet, it was already created by the server
					if (!Main.isServer()) {
						CreateBullet cb = (CreateBullet) event;
						tmp.set(cb.x, cb.y, cb.z);
						Log.debug("create bullet hit: " + Tools.fmt(tmp));
						new BulletHit(tmp);
					}
				} else if (event instanceof ReceivedChatMessage) {
					ReceivedChatMessage msg = (ReceivedChatMessage) event;
					View.inst.hud.addChatMessage(msg.chat);
				} else if (event instanceof CreateLevelStatics) {
					LevelBuilder.buildStatics(((CreateLevelStatics) event).staticPieces);
				} else {
					throw new GdxRuntimeException("unhandled client event: " + event);
				}
			}
		}
		eventQueue.clear();
	}

	public void addEventToQueue(ClientEvent event) {
		synchronized (eventQueue) {
			eventQueue.add(event);
		}
	}
}
