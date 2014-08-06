package org.jrenner.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.NumberUtils;
import org.jrenner.fps.utils.Compression;
import org.jrenner.fps.utils.ObjectSize;

import java.io.Serializable;

/** an experimental class that could be used to record and playback gameplay */
public class Recorder implements Serializable {
	private Array<Snapshot> history = new Array<>(512);
	private int recordIntervalFrames;
	private boolean recording;

	public Recorder(int frameInterval) {
		recordIntervalFrames = frameInterval;
		Array<Snapshot> loaded = load();
		if (loaded != null) history = loaded;
	}

	public void update(Vector3 pos, Vector3 velocity, Quaternion rotation) {
		if (Main.frame % recordIntervalFrames != 0) return;
		Snapshot snap = new Snapshot();
		System.out.println("Snapshot: " + ObjectSize.getObjectSize(snap));
		snap.position.set(pos);
		//snap.velocity.set(velocity);
		snap.rotation.set(rotation);
		history.add(snap);
	}

	public boolean isRecording() {
		return recording;
	}

	public int getFrameInterval() {
		return recordIntervalFrames;
	}

	public void begin() {
		history.clear();
		recording = true;
	}

	public void end() {
		recording = false;
	}

	public Array<Snapshot> getRecording() {
		return new Array<>(history);
	}

	boolean useJson = false;

	public void write() {
		if (useJson) {
			writeJson();
		} else {
			writeBytes();
		}
	}

	private static final String demoDir = ".fps-demo/";

	public Array<Snapshot> load() {
		if (useJson) {
			return loadJson();
		} else {
			return loadBytes();
		}
	}

	private void writeBytes() {
		FileHandle fh = Gdx.files.external(demoDir + "history.dat");
		byte[] bytes = new byte[Snapshot.SNAPSHOT_SIZE * history.size];
		int idx = 0;
		for (Snapshot snap : history) {
			idx = snap.serialize(bytes, idx);
		}
		System.out.println("finished write, bytes length: " + bytes.length + ", idx: " + idx);
		fh.writeBytes(bytes, false);
	}

	private Array<Snapshot> loadBytes() {
		String filename = "history.dat";
		FileHandle histFile = Gdx.files.external(demoDir + filename);
		if (!histFile.exists()) {
			Log.error("playback history file doesn't exist, skip loading: " + filename);
			return null;
		}
		byte[] bytes = histFile.readBytes();
		int len = bytes.length;
		Log.debug("history files bytes: " + len);
		if (len % Snapshot.SNAPSHOT_SIZE != 0) {
			Log.error("playback history length error, expected divisible by: " + Snapshot.SNAPSHOT_SIZE + ", remainder: " + len % Snapshot.SNAPSHOT_SIZE);
			return null;
		}
		Array<Snapshot> snapshots = new Array<>();
		int bytesProcessed = 0;
		while (bytesProcessed < len) {
			snapshots.add(Snapshot.deserialize(bytes, bytesProcessed));
			bytesProcessed += Snapshot.SNAPSHOT_SIZE;
		}
		return snapshots;
	}

	private void writeJson() {
		Json json = new Json();
		String data = json.toJson(history);
		FileHandle fh = Gdx.files.external(demoDir + "history.json.gz");
		byte[] bytes = Compression.writeCompressedString(data);
		if (bytes == null) {
			Log.error("Could not write compressed playback (JSON)");
		} else {
			int byteCount = bytes.length;
			fh.writeBytes(bytes, false);
			Log.debug("Wrote compressed playback: " + byteCount + " bytes");
		}
	}

	private Array<Snapshot> loadJson() {
		Json json = new Json();
		String filename = demoDir + "history.json.gz";
		FileHandle histFile = Gdx.files.external(filename);
		if (!histFile.exists()) {
			Log.error("playback history file doesn't exist, skip loading: " + filename);
			return null;
		}
		byte[] bytes = histFile.readBytes();
		String data = Compression.decompressToString(bytes);
		System.out.println(data);
		return (Array<Snapshot>) json.fromJson(Array.class, data);
	}

	public static class Snapshot implements Serializable {
		public Vector3 position = new Vector3();
		public Quaternion rotation = new Quaternion();
		//public Vector3 velocity = new Vector3();

		public static final int SNAPSHOT_SIZE = (4 + 3 + 3) * 4; // (pos + rot + vel) * 4 bytes per float

		public static Snapshot deserialize(byte[] bytes, int idx) {
			Snapshot snap = new Snapshot();
			int i = idx;
			// position
			snap.position.x = Tools.readFloatFromBytes(bytes, i);
			i += 4;
			snap.position.y = Tools.readFloatFromBytes(bytes, i);
			i += 4;
			snap.position.z = Tools.readFloatFromBytes(bytes, i);
			// rotation
			i += 4;
			snap.rotation.x = Tools.readFloatFromBytes(bytes, i);
			i += 4;
			snap.rotation.y = Tools.readFloatFromBytes(bytes, i);
			i += 4;
			snap.rotation.z = Tools.readFloatFromBytes(bytes, i);
			i += 4;
			snap.rotation.w = Tools.readFloatFromBytes(bytes, i);
			// velocity
			/*i += 4;
			snap.velocity.x = Tools.readFloatFromBytes(bytes, i);
			i += 4;
			snap.velocity.y = Tools.readFloatFromBytes(bytes, i);
			i += 4;
			snap.velocity.z = Tools.readFloatFromBytes(bytes, i);*/
			return snap;
		}

		public int serialize(byte[] bytes, int offset) {
			int idx = offset;
			idx = Tools.writeFloatToBytes(position.x, bytes, idx);
			idx = Tools.writeFloatToBytes(position.y, bytes, idx);
			idx = Tools.writeFloatToBytes(position.z, bytes, idx);
			idx = Tools.writeFloatToBytes(rotation.x, bytes, idx);
			idx = Tools.writeFloatToBytes(rotation.y, bytes, idx);
			idx = Tools.writeFloatToBytes(rotation.z, bytes, idx);
			idx = Tools.writeFloatToBytes(rotation.w, bytes, idx);
			/*idx = Tools.writeFloatToBytes(velocity.x, bytes, idx);
			idx = Tools.writeFloatToBytes(velocity.y, bytes, idx);
			idx = Tools.writeFloatToBytes(velocity.z, bytes, idx);*/
			System.out.println("serialized snapshot");
			for (int i = 0; i < SNAPSHOT_SIZE; i++) {
				System.out.printf("[%d]: %d\n", i+offset, bytes[i+offset]);
			}
			return idx;
		}

		public void prettyPrint() {
			System.out.println("--- Snapshot ---");
			Tools.print(position, "position");
			Tools.print(rotation, "rotation");
		}
	}	
}
