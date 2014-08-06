package org.jrenner.fps.net.client;

import com.badlogic.gdx.math.Vector3;

/** Record an entity's state at a specific tick/frame, used for prediction and interpolation */
public class EntityFrame {
	public int inputTick;
	public Vector3 position = new Vector3();
	public Vector3 rotation = new Vector3();
	public Vector3 velocity = new Vector3();

	public void set(EntityFrame other) {
		inputTick = other.inputTick;
		position.set(other.position);
		rotation.set(other.rotation);
		velocity.set(other.velocity);
	}
}

