package org.jrenner.fps;

import com.badlogic.gdx.math.Vector3;

public class Direction {
	public static enum Translation {
		Forward(0f, 0f, 1f),
		Back(0f, 0f, -1f),
		Up(0f, 1f, 0f),
		Down(0f, -1f, 0f),
		Left(1f, 0f, 0f),
		Right(-1f, 0f, 0f)
		;

		public Vector3 vector;

		Translation(float x, float y, float z) {
			vector = new Vector3(x, y, z);
		}
	}

	public static enum Rotation {
		YawLeft(-1f, 0f, 0f),
		YawRight(1f, 0f, 0f),
		PitchUp(0f, 1f, 0f),
		PitchDown(0f, -1f, 0f),
		RollLeft(0f, 0f, 0f),
		RollRight(0f, 0f, 1f),
		;

		public Vector3 vector;

		Rotation(float x, float y, float z) {
			vector = new Vector3(x, y, z);
		}
	}
}
