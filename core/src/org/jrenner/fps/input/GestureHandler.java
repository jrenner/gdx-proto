package org.jrenner.fps.input;

import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import org.jrenner.fps.Main;
import org.jrenner.fps.Player;
import org.jrenner.fps.entity.DynamicEntity;

public class GestureHandler extends GestureDetector {

	public static GestureHandler createGestureHandler() {
		GestureListener gest = new GestureListener() {
			@Override
			public boolean touchDown(float x, float y, int pointer, int button) {
				return false;
			}

			@Override
			public boolean tap(float x, float y, int count, int button) {
				return false;
			}

			@Override
			public boolean longPress(float x, float y) {
				return false;
			}

			@Override
			public boolean fling(float velocityX, float velocityY, int button) {
				return false;
			}

			@Override
			public boolean pan(float x, float y, float deltaX, float deltaY) {
				if (Main.isMobile()) {
					Player player = Main.inst.client.player;
					if (player != null) {
						player.entity.adjustYaw(deltaX * -0.5f);
						player.entity.adjustPitch(deltaY * 0.5f);
					}
					return true;
				}
				return false;
			}

			@Override
			public boolean panStop(float x, float y, int pointer, int button) {
				return false;
			}

			@Override
			public boolean zoom(float initialDistance, float distance) {
				return false;
			}

			@Override
			public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
				return false;
			}
		};
		return new GestureHandler(gest);
	}

	public GestureHandler(GestureListener listener) {
		super(listener);
	}
}
