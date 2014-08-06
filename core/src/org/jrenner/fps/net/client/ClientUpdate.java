package org.jrenner.fps.net.client;

import com.badlogic.gdx.utils.Pool;
import org.jrenner.fps.net.packages.CommandPackage;

public class ClientUpdate implements Pool.Poolable {
	private static Pool<ClientUpdate> clientUpdatePool = new Pool<ClientUpdate>() {
		@Override
		protected ClientUpdate newObject() {
			return new ClientUpdate();
		}
	};

	/** tick number this input was sent at, used for client-side prediction, not related to main server timing tick */
	public int inputTick;
	public int entityId;
	public CommandPackage cmdPack = new CommandPackage();

	public void reset() {
		inputTick = -1;
		entityId = -1;
	}

	public void set(ClientUpdate other) {
		entityId = other.entityId;
		cmdPack.commandBits = other.cmdPack.commandBits;
	}

	public static ClientUpdate obtain() {
		return clientUpdatePool.obtain();
	}

	public static void free(ClientUpdate update) {
		clientUpdatePool.free(update);
	}
}
