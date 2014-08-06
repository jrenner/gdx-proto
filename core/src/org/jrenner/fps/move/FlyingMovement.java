package org.jrenner.fps.move;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class FlyingMovement extends Movement {
	private static Vector2 tmpv2 = new Vector2();

	public FlyingMovement(int entityId) {
		super(entityId);
	}

	@Override
	void accelerate(float timeStep) {
		Vector3 diff = tmp;
		diff.set(destination).sub(position);
		acceleration.set(diff).nor().scl(accelRate).scl(timeStep);
		velocity.add(acceleration);
	}
}
