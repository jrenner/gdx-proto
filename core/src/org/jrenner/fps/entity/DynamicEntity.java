package org.jrenner.fps.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import org.jrenner.fps.Main;
import org.jrenner.fps.Physics;
import org.jrenner.fps.Player;
import org.jrenner.fps.Shadow;
import org.jrenner.fps.effects.BulletHit;
import org.jrenner.fps.move.GroundMovement;

public class DynamicEntity extends Entity {
	public static Array<DynamicEntity> list;
	public Shadow shadow;

	public static DynamicEntity createEntityNoId(EntityGraphicsType graphicsType) {
		if (!Main.isClient()) {
			// server does not care about graphics
			graphicsType = EntityGraphicsType.None;
		}
		DynamicEntity dynEnt = new DynamicEntity(graphicsType);
		dynEnt.id = -1;
		return dynEnt;
	}

	public static DynamicEntity createEntity(int id, boolean isPlayer, EntityGraphicsType graphicsType) {
		DynamicEntity dynEnt = createEntityNoId(graphicsType);
		assignEntityID(dynEnt, id);
		if (Main.isClient() && isPlayer) {
			dynEnt.setPlayer(new Player(dynEnt));
		}
		return dynEnt;
	}

	public DynamicEntity(EntityGraphicsType graphicsType) {
		super(graphicsType);
		DynamicEntity.list.add(this);
		// TODO move dimensions to constructor parameters
		dimen.set(1f, 2f, 1f);
		tmp.set(dimen).scl(0.5f);
		float radius = 0.45f * (float) Math.sqrt((dimen.x * dimen.x) + (dimen.z * dimen.z));
		body = Physics.inst.createCapsuleObject(radius, dimen.y * 0.35f);
		bodyOffset.set(0f, 0.05f, 0f);
		Physics.applyDynamicEntityCollisionFlags(body);
		body.userData = this;

		movement = new GroundMovement(id);
		movement.setAffectedByGravity(true);
		movement.setAccelRate(0.03f);
		movement.setMaxHorizontalSpeed(0.12f);
		movement.setMaxVerticalSpeed(0.6f);
		movement.setBraking(0.9f);
		Physics.inst.addDynamicEntityToWorld(body);

		float shadowRadius = (float) Math.sqrt(dimen.x * dimen.x + dimen.z * dimen.z);
		if (Main.isClient()) {
			shadow = new Shadow(shadowRadius);
		}
	}

	private long lastSetDestTime;
	private int nextDestInterval = 10000;

	@Override
	void update(float timeStep) {
		boolean testSimpleAI = true;
		super.update(timeStep);
		if (testSimpleAI && this.player == null) {
			if (Main.isClient() && Main.inst.client.player != null) {
				// look at the player entity (billboard behaviour, always face the camera)
				lookAt(Main.inst.client.player.entity.getPosition());
			}
			long now = TimeUtils.millis();
			movement.cancelDestinationAtThreshold(3f);
			if ((now - lastSetDestTime) >= nextDestInterval) {
				float x = MathUtils.random(0f, 100f);
				float y = 0f;
				float z = MathUtils.random(0f, 100f);
				setDestination(tmp.set(x, y, z));
				lastSetDestTime = now;
				nextDestInterval = MathUtils.random(5000, 15000);
			}
		}
		if (shooting) {
			if (tickCountdown <= 0) {
				shoot();
				tickCountdown = ticksPerShot;
			}
		}
		if (shadow != null) {
			shadow.update(getPosition());
		}
		tickCountdown--;
	}

	public Vector3 getForwardFacing(Vector3 storage) {
		storage.set(Vector3.Z);
		relativize(storage);
		return storage;
	}

	private static long minJumpInterval = 250;
	private long lastJumpTime = 0;

	public void jump() {
		if (onGround) {
			long now = System.currentTimeMillis();
			if ((now - lastJumpTime) < minJumpInterval) {
				return;
			}
			lastJumpTime = now;
			float jumpStrength = 0.35f;
			adjustVelocity(tmp.set(0f, jumpStrength, 0f));
		}
	}

	private int ticksPerShot = 6;
	private int tickCountdown;
	private boolean shooting;
	private float aimError = 0.025f;

	public void startShoot() {
		shooting = true;
	}

	public void stopShoot() {
		shooting = false;
	}

	public void shoot() {
		// self pos = ray start
		tmp.set(getPosition());
		// store forward vector in tmp2 and keep for processing hit result
		// also add aim error
		float xErr = MathUtils.random(-aimError, aimError);
		float yErr = MathUtils.random(-aimError, aimError);
		tmp2.set(Vector3.Z).add(xErr, yErr, 0f);
		relativize(tmp2);
		float rayLen = 1000f;
		// get forward facing and apply aim error
		tmp3.set(tmp2).scl(rayLen).add(tmp);
		Physics.inst.castRay(tmp, tmp3);
		Physics.RaycastReport ray = Physics.inst.raycastReport;
		if (ray.hit) {
			// position + (forward vector * hitDistance) = hit location
			tmp2.scl(ray.hitDistance).add(tmp);
			new BulletHit(tmp2);
			Main.inst.server.queueBulletHit(tmp2);
			//Log.debug(Tools.fmt(tmp, "hit"));
		} else {
			//Log.debug("no hit");
		}
	}

	@Override
	protected void removeFromGame() {
		super.removeFromGame();
		if (shadow != null) {
			shadow.removeFromGame();
		}
	}

	public boolean isPlayer() {
		return player != null;
	}
}
