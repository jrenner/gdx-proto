package org.jrenner.fps.net.packages;

import com.badlogic.gdx.math.Vector3;
import org.jrenner.fps.utils.Pooler;

public class EntityUpdate {
	private int entityId;
	private Vector3 position = Pooler.v3();
	private Vector3 rotation = Pooler.v3();
	private static Vector3 tmp = new Vector3();

	public Vector3 getPosition() {
		tmp.set(position);
		Pooler.free(position);
		return tmp;
	}

	public void setPosition(Vector3 position) {
		this.position.set(position);
	}

	public Vector3 getRotation() {
		tmp.set(rotation);
		Pooler.free(rotation);
		return tmp;
	}

	public void setRotation(float yaw, float pitch, float roll) {
		this.rotation.set(yaw, pitch, roll);
	}

	public int getEntityId() {
		return entityId;
	}

	public void setEntityId(int entityId) {
		this.entityId = entityId;
	}
}
