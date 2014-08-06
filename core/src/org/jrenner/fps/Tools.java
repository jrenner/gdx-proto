package org.jrenner.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btVector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.StringBuilder;

import java.util.Arrays;

/** a messy junk drawer */
// TODO clean up this junk!
public class Tools {

	public static String fmt(float f, String name) {
		if (name == null) name = "";
		return String.format("%s: %.3f", name, f);
	}

	public static String fmt(float f) {
		return fmt(f, null);
	}

	public static void print(float f) {
		print(f, null);
	}

	public static void print(float f, String name) {
		System.out.println(fmt(f, name));
	}

	public static String fmt(Vector3 vec) {
		return fmt(vec, null);
	}

	public static String fmt(Vector3 vec, String name) {
		if (name == null) name = "";
		return String.format("(V3) %s x: %.3f, y: %.3f, z: %.3f", name, vec.x, vec.y, vec.z);
	}

	public static String fmt(Vector3 vec, int precision) {
		return fmt(vec, "", precision);
	}

	public static String fmt(Vector3 vec, String name, int precision) {
		if (name == null) name = "";
		String base = "(V3) %s x: %.PRECf, y: %.PRECf, z: %.PRECf";
		String fmtString = base.replaceAll("PREC", Integer.toString(precision));
		return String.format(fmtString, name, vec.x, vec.y, vec.z);
	}

	public static void print(Vector3 vec) {
		print(vec, null);
	}

	public static void print(Vector3 vec, String name) {
		System.out.println(Tools.fmt(vec, name));
	}

	public static String fmt(MeshPartBuilder.VertexInfo vi) {
		return fmt(vi, null);
	}

	public static String fmt(MeshPartBuilder.VertexInfo vi, String name) {
		if (name == null) name = "";
		StringBuilder sb = new StringBuilder();
		sb.append("VertexInfo: ").append(name).append("\n");
		sb.append("\t").append(fmt(vi.position, "position")).append("\n");
		sb.append("\t").append(fmt(vi.color)).append("\n");
		sb.append("\t").append(fmt(vi.normal, "normal"));
		return sb.toString();
	}

	public static void print(MeshPartBuilder.VertexInfo vi) {
		System.out.println(fmt(vi, null));
	}

	public static void print(MeshPartBuilder.VertexInfo vi, String name) {
		System.out.println(fmt(vi, name));
	}

	public static String fmt(Color color, String name) {
		if (name == null) name = "";
		return String.format("(Color) %s R:%.1f G:%.1f B:%.1f A:%.1f", name, color.r, color.g, color.b, color.a);
	}

	public static String fmt(Color color) {
		return fmt(color, null);
	}

	public static void print(Color color, String name) {
		System.out.println(Tools.fmt(color, name));
	}

	public static void print(Color color) {
		print(color, null);
	}

	public static String fmt(Quaternion q, String name) {
		if (name == null) name = "";
		return String.format("(Q %s) - yaw: %.0f, pitch: %.0f, roll: %.0f -- w: %.2f, x: %.2f, y: %.2f, z: %.2f",
				name, q.getYaw(), q.getPitch(), q.getRoll(), q.w, q.x, q.y, q.z);
	}

	public static String fmt(Quaternion q) {
		return fmt(q, null);
	}

	public static void print(Quaternion q, String name) {
		System.out.println(fmt(q, name));
	}

	public static void print(Quaternion q) {
		System.out.println(fmt(q, null));
	}


	private static Matrix4 mtx = new Matrix4();
	private static Quaternion q = new Quaternion();

	public static void rotateAround(Vector3 position, Vector3 axis, float angle) {
		q.setFromAxis(axis, angle);
		mtx.set(q);
		position.prj(mtx);
		/*tmp.set(point);
		tmp.sub(position);
		position.add(tmp);
		position.rotate(axis, angle);
		tmp.rotate(axis, angle);
		position.add(-tmp.x, -tmp.y, -tmp.z);*/
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static Vector3 crsTmp = new Vector3();

	public static Vector3 getDownFromVector(Vector3 vec) {
		crsTmp.set(vec).nor();
		crsTmp.crs(Vector3.Y); // cross by world up to get a right handle angle vector (facing forward)
		tmp.set(vec).nor();
		return tmp.crs(crsTmp).nor(); // get the relative "down"
	}

	public static Vector3 getUpFromVector(Vector3 vec) {
		crsTmp.set(vec).nor();
		crsTmp.crs(Vector3.Y); // cross by world up to get a right handle angle vector (facing forward)
		tmp.set(vec).nor();
		return crsTmp.crs(tmp).nor(); // get the relative "down"
	}

	public static Vector3 getRightFromVector(Vector3 vec) {
		crsTmp.set(vec).nor();
		crsTmp.crs(Vector3.Y); // cross by world up to get a right handle angle vector
		//Tools.print(crsTmp, "right");
		//Tools.print(vec, "original");
		return crsTmp.nor();
	}

	public static Vector3 getLeftFromVector(Vector3 vec) {
		crsTmp.set(vec).nor();
		tmp.set(Vector3.Y).crs(crsTmp);
		return tmp.nor();
	}

	/** return angle in degrees */
	public static float getAngleFromAtoB(Vector2 a, Vector2 b) {
		float rawAngle = (float) Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x));
		return (rawAngle - 90) % 360;
	}

	public static float getAngleFromAtoB(float ax, float ay, float bx, float by) {
		return (float) Math.toDegrees(Math.atan2(by - ay, bx - ax));
	}

	private static Vector2 va = new Vector2();
	private static Vector2 vb = new Vector2();

	public static float getAngleFromAtoB(Vector3 a, Vector3 b, Vector3 axis) {
		if (axis.equals(Vector3.Y)) {
			va.set(a.x, a.z);
			vb.set(b.x, b.z);
			return getAngleFromAtoB(va, vb);
		} else {
			throw new IllegalArgumentException("bad argument for vector angle");
		}
	}

	static Vector3 tmp = new Vector3();
	static Vector3 tmp2 = new Vector3();
	static Vector3 tmp3 = new Vector3();


	/** thanks to lordjone from #libgdx! */
	public static void faceDirectionZ(Quaternion q, Vector3 direction) {
		Vector3 axisZ = tmp.set(direction).nor();
		Vector3	axisY = tmp2.set(tmp).crs(Vector3.Y).nor().crs(tmp).nor();
		Vector3	axisX = tmp3.set(axisY).crs(axisZ).nor();
		q.setFromAxes(false, axisX.x, axisY.x, axisZ.x,
				axisX.y, axisY.y, axisZ.y,
				axisX.z, axisY.z, axisZ.z);
	}

	public static void faceDirectionY(Quaternion q, Vector3 direction) {
		throw new GdxRuntimeException("THIS IS NOT WORKING! DONT USE IT!");
		/*Vector3 axisZ = tmp.set(direction).nor();
		Vector3	axisY = tmp2.set(tmp).crs(Vector3.X).nor().crs(tmp).nor();
		Vector3	axisX = tmp3.set(axisY).crs(axisZ).nor();
		q.setFromAxes(false, axisX.x, axisY.x, axisZ.x,
				axisX.y, axisY.y, axisZ.y,
				axisX.z, axisY.z, axisZ.z);*/
	}

	public static float constrainAngle180(float angle) {
		while (angle > 180) {
			angle = angle - 360;
		}
		while (angle < -180) {
			angle = angle + 360;
		}
		return angle;

	}

	// TODO This algorithm is bad, but it's random enough for now
	public static Vector3 randomUnitVector() {
		float x = MathUtils.random(0f, 1f);
		float y = MathUtils.random(0f, 1f);
		float z = MathUtils.random(0f, 1f);
		return tmp.set(x, y, z).nor();
	}

	public static Color randomColor(float lo, float hi) {
		Color col = new Color();
		col.r = MathUtils.random(lo, hi);
		col.g = MathUtils.random(lo, hi);
		col.b = MathUtils.random(lo, hi);
		col.a = 1f;
		return col;
	}

	public static Color randomColor() {
		return randomColor(0f, 1f);
	}

	public static void calculateRectFaceNormal(Vector3 norm, Vector3 a, Vector3 b) {
		a.nor();
		b.nor();
		norm.set(b).crs(a).nor();
	}

	public static void arraySwap(Object[] arr, int i1, int i2) {
		Object tmp = arr[i1];
		arr[i1] = arr[i2];
		arr[i2] = tmp;
	}

	public static void reverseArray(Object[] arr) {
		for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
			Object tmp = arr[i];
			arr[i] = arr[j];
			arr[j] = tmp;
		}
	}

	public static void dispose(Disposable disp) {
		dispose(disp, "");
	}

	public static void dispose(Disposable disp, String name) {
		Log.debug("dispose: " + name + " -- " + disp);
		disp.dispose();
	}

	public static Preferences getPrefs(String name) {
		return Gdx.app.getPreferences("org.jrenner.fps." + name);
	}

	public static float readFloatFromBytes(byte[] bytes, int offset) {
		int i = 0;
		int value = bytes[0 + offset] & 0xFF;
		value |= bytes[1 + offset]<<(8) & 0xFFFF;
		value |= bytes[2 + offset]<<(16) & 0xFFFFFF;
		value |= bytes[3 + offset]<<(24) & 0xFFFFFFFF;
		return NumberUtils.intBitsToFloat(value);
	}

	public static int writeFloatToBytes(float f, byte[] bytes, int offset) {
		int value = NumberUtils.floatToIntBits(f);
		bytes[0 + offset] = (byte) (value & 0xFF);
		bytes[1 + offset] = (byte) (value>>8 & 0xFF);
		bytes[2 + offset] = (byte) (value>>16 & 0xFF);
		bytes[3 + offset] = (byte) (value>>24 & 0xFF);
		// return next idx
		return offset + 4;
	}
}
