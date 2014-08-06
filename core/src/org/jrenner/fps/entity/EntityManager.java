package org.jrenner.fps.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class EntityManager {
	public static void entityTranslationMove(Entity ent, Vector3 trans) {
		if (ent.isFlyingEntity()) {
			ent.setRelativeDestination(trans);
		} else {
			ent.setRelativeDestinationByYaw(trans);
		}
	}

	public static void entityRotate(Entity ent, float yaw, float pitch, float roll) {
		if (!MathUtils.isZero(yaw)) ent.adjustYaw(yaw);
		if (!MathUtils.isZero(pitch)) ent.adjustPitch(pitch);
		if (!MathUtils.isZero(roll)) ent.adjustRoll(roll);
	}
}
