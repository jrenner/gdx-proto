package org.jrenner.fps.move;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import org.jrenner.fps.GameWorld;
import org.jrenner.fps.Log;
import org.jrenner.fps.utils.Pooler;

public abstract class Movement implements Disposable {
	public static Array<Movement> list;
	private static Vector2 tmpv2 = new Vector2();
	static final Vector3 tmp = new Vector3();
	Vector3 position = Pooler.v3();
	Vector3 destination = Pooler.v3();
	boolean hasDest;
	Vector3 velocity = Pooler.v3();
	Vector3 acceleration = Pooler.v3();
	float accelRate = 0.01f;
	float maxHorizontalSpeed = 0.1f;
	float maxVerticalSpeed = 0.1f;
	float braking = 0.98f;
	boolean gravity = false;
	protected float arrivalThreshold = 5f;
	private int entityId;

	public static void init() {
		list = new Array<>();
	}

	public Movement(int entityId) {
		this.entityId = entityId;
		list.add(this);
	}

	public void update(float timeStep, boolean onGround) {
		// cancel downward velocity if touching the ground and moving downwards
		if (onGround) {
			if (velocity.y < 0f) velocity.y = 0f;
		}
		if (!hasDest) {
			if (onGround) {
				// brake while on ground with no destination
				velocity.scl(braking);
			}
		} else {
			// accelerate towards the destination
			accelerate(timeStep);
		}
		applyGravity(onGround);
		applyVelocity(timeStep, true);
		//stayInGameWorld();
	}

	abstract void accelerate(float timeStep);

	void limitVelocity() {
		velocity.y = MathUtils.clamp(velocity.y, -maxVerticalSpeed, maxVerticalSpeed);
		tmpv2.set(velocity.x, velocity.z);
		tmpv2.limit(maxHorizontalSpeed);
		velocity.x = tmpv2.x;
		velocity.z = tmpv2.y;
	}

	public void applyVelocity(float timeStep, boolean limit) {
		limitVelocity();
		tmp.set(velocity).scl(timeStep);
		//System.out.println("add to position: " + Tools.fmt(tmp));
		position.add(tmp);
	}

	public void applyGravity(boolean onGround) {
		if (!gravity) return;
		tmp.set(0f, GameWorld.GRAVITY, 0f);
		/*if (onGround) {
			Vector3 floorNormal = Physics.inst.getFloorNormal(position);
			if (floorNormal.y <= 0.9f) {
				// slide down slopes
				// combine and average gravity and the floor normal
				tmp.nor();
				tmp.add(floorNormal).nor();
				tmp.scl(-GameWorld.GRAVITY);
			}
		}*/
		if (!onGround) {
			velocity.add(tmp);
		}
	}

	public float getBraking() {
		return braking;
	}

	public void setBraking(float braking) {
		this.braking = braking;
	}

	public Vector3 getVelocity() {
		return velocity;
	}

	/** acceleration as applied to velocity */
	public Vector3 getAcceleration() {
		return acceleration;
	}

	/** the rate of acceleration */
	public float getAccelRate() {
		return accelRate;
	}

	public void setAccelRate(float accelRate) {
		this.accelRate = accelRate;
	}

	public float getMaxHorizontalSpeed() {
		return maxHorizontalSpeed;
	}

	public void setMaxHorizontalSpeed(float maxHorizontalSpeed) {
		this.maxHorizontalSpeed = maxHorizontalSpeed;
	}

	public float getMaxVerticalSpeed() {
		return maxVerticalSpeed;
	}

	public void setMaxVerticalSpeed(float maxVerticalSpeed) {
		this.maxVerticalSpeed = maxVerticalSpeed;
	}

	public Vector3 getDestination() {
		return destination;
	}

	public boolean hasDestination() {
		return hasDest;
	}

	public void setDestination(Vector3 newDest) {
		if (newDest == null) {
			hasDest = false;
		} else {
			hasDest = true;
			destination.set(newDest);
		}
	}

	public void cancelDestinationAtThreshold(float range) {
		if (hasDest) {
			if (destination.dst2(position) <= range * range) {
				hasDest = false;
			}
		}
	}

	public boolean isAffectedByGravity() {
		return gravity;
	}

	public void setAffectedByGravity(boolean gravity) {
		this.gravity = gravity;
	}

	public Vector3 getPosition() {
		return position;
	}

	public void setPosition(Vector3 position) {
		this.position.set(position);
	}

	public void stayInGameWorld() {
		boolean out = position.y < -100f || position.x < -20f || position.x > GameWorld.WORLD_WIDTH + 20f ||
				position.z < -20f || position.z > GameWorld.WORLD_DEPTH + 20f;
		if (out) {
			// TODO handle this for real
			// drop in to world randomly
			position.set(MathUtils.random(50f, 100f), 30f, MathUtils.random(50f, 100f));
			Log.debug("Entity was outside of game world, position reset: " + entityId);
		}
	}

	@Override
	public void dispose() {
		Pooler.free(velocity, acceleration, position, destination);
		list.removeValue(this, true);
	}

	public static void disposeAll() {
		for (Movement move : list) {
			move.dispose();
		}
	}
}
