package org.jrenner.fps.net.packages;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.utils.Pooler;
import org.jrenner.fps.entity.Entity;

/** Contains game state information that is sent over the net from Server to Client */
public class ServerUpdate implements Pool.Poolable {
	public int tickNum;
	public int playerInputTick; // the last input processed from the player who will receive this update
	public EntityUpdate[] entityUpdates;
	private boolean processed;

	public static ServerUpdate createServerUpdate() {
		ServerUpdate serverUpdate = Pooler.serverUpdate();
		synchronized (Entity.list) {
			serverUpdate.tickNum = Main.inst.server.tickNum;
			serverUpdate.entityUpdates = new EntityUpdate[Entity.list.size];
			for (int i = 0; i < Entity.list.size; i++) {
				Entity ent = Entity.list.get(i);
				EntityUpdate entUpd = Pooler.entityUpdate();
				entUpd.setEntityId(ent.id);
				entUpd.setPosition(ent.getPosition());
				entUpd.setRotation(ent.getYaw(), ent.getPitch(), ent.getRoll());
				serverUpdate.entityUpdates[i] = entUpd;
			}
		}
		return serverUpdate;
	}

	public void free() {
		for (EntityUpdate entUpd : entityUpdates) {
			Pooler.free(entUpd);
		}
		Pooler.free(this);
	}

	public void applyUpdates() {
		if (processed) throw new GdxRuntimeException("serverUpdate already processed");
		for (EntityUpdate entUpdate : entityUpdates) {
			Entity ent = Entity.getEntityById(entUpdate.getEntityId());
			if (ent != null) {
				if (Main.inst.client.player == null && ent.id == Main.inst.client.playerId) {
					Main.inst.client.assignClientPlayerToId(ent.id);
					Log.debug("Entity matched player id, entity was assigned to player: " + ent.id);
				}
				ent.interpolator.handleUpdateFromServer(entUpdate, playerInputTick);
			}
		}
		processed = true;
		free();
	}

	@Override
	public void reset() {
		entityUpdates = null;
		processed = false;
	}

}
