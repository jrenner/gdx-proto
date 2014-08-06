package org.jrenner.fps.net.packages;

import org.jrenner.fps.LevelStatic;

public class ServerMessage {

	public static class AssignPlayerEntityId {
		public int id;
	}

	public static class DestroyEntity {
		public int id;
	}

	public static class ServerInfo {
		public int tickNum;
		public float tickInterval;
		public String serverName;
		public String serverMsg;
	}

	public static class LevelGeometry {
		public LevelStatic[] staticPieces;
	}

}
