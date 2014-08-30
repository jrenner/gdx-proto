package org.jrenner.fps.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import org.jrenner.fps.Main;
import org.jrenner.fps.Tools;
import org.jrenner.fps.net.client.EntityFrame;
import org.jrenner.fps.net.packages.EntityUpdate;

public class EntityInterpolator {
	private Entity entity;
	private EntityFrame oldFrame = new EntityFrame();
	private EntityFrame currentFrame = new EntityFrame();
	private int lastInputTickFromServer;
	private EntityUpdate queuedUpdate;
	private long lastUpdateTime;
	public Vector3 posError = new Vector3();

	public EntityInterpolator(Entity ent) {
		this.entity = ent;
	}

	public void handleUpdateFromServer(EntityUpdate upd, int serverInputTick) {
		synchronized (this) {
			queuedUpdate = upd;
			lastInputTickFromServer = serverInputTick;
		}
	}

	public void update() {
		if (queuedUpdate != null) {
			oldFrame.set(currentFrame);
			currentFrame.inputTick = lastInputTickFromServer;
			currentFrame.position.set(queuedUpdate.getPosition());
			currentFrame.rotation.set(queuedUpdate.getRotation());
			queuedUpdate = null;

			lastUpdateTime = TimeUtils.millis();
			if (entity.getPlayer() != null) {
				posError.set(entity.getPosition()).sub(currentFrame.position);
			}
		}
		// use client prediction for self, interpolate all other entities
		if (Main.isClient() && Main.inst.client.player.entity == entity) {
			clientPrediction();
		} else {
			interpolate(oldFrame, currentFrame);
		}
	}

	public float getInterpolationAlpha() {
		float alpha = (TimeUtils.millis() - lastUpdateTime) / Main.getNetClient().tickInterval;
		return MathUtils.clamp(alpha, 0f, 1.0f);
	}

	private static Vector3 tmp = new Vector3();

	/** Interpolate position and rotation based on updates received from server */
	private void interpolate(EntityFrame prev, EntityFrame next) {
		entity.getPosition().set(prev.position).lerp(next.position, getInterpolationAlpha());
		tmp.set(prev.rotation).slerp(next.rotation, getInterpolationAlpha());
		entity.setYawPitchRoll(tmp);
	}

	/** we store all old state to be replayed on top of server updates */
	private Array<EntityFrame> clientFrames = new Array<>();
	private Vector3 lastPosition = new Vector3();
	private Vector3 nextPosition = new Vector3();

	private void clientPrediction() {
		lastPosition.set(entity.getPosition());
		EntityFrame frame = new EntityFrame();
		frame.inputTick = Main.getNetClient().inputTick;
		Vector3 currentVel = tmp.set(entity.getVelocity());
		frame.velocity.set(currentVel);
		clientFrames.add(frame);
		if (clientFrames.size > 200) {
			clientFrames.removeIndex(0);
		}
		// get rid of input frames older than authoritative server update
		for (int i = clientFrames.size - 1; i >= 0; i--) {
			EntityFrame cf = clientFrames.get(i);
			if (cf.inputTick <= currentFrame.inputTick) {
				clientFrames.removeIndex(i);
			}
		}
		// update to last authoritative server position
		entity.setPosition(currentFrame.position);
		// apply all client input that is newer than last server update
		for (EntityFrame cf : clientFrames) {
			entity.getVelocity().set(cf.velocity.x, cf.velocity.y, cf.velocity.z);
			float timeStep = 1f;
			entity.movement.applyVelocity(timeStep, false);
		}
		// finished applying client input, the result is our predicted position
		nextPosition.set(entity.getPosition());
		// now restore pre-prediction velocity
		entity.setVelocity(currentVel);

		// now we interpolate from our last position to our new predicted position, to smooth out errors
		boolean interpEnabled = true;
		if (interpEnabled) {
			float alpha = 0.1f;
			entity.getPosition().set(lastPosition).lerp(nextPosition, alpha);
		} else {
			entity.setPosition(nextPosition);
		}
	}

}
