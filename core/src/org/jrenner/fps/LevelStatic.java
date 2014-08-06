package org.jrenner.fps;

import com.badlogic.gdx.math.Matrix4;

/** An array of these represents the static geometry in the level, and is sent over the net
 * from server to client, loading the level for the client.
 */
public class LevelStatic {
	public String modelName;
	public Matrix4 mtx = new Matrix4();
}
