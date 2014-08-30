package org.jrenner.fps.event;

import org.jrenner.fps.LevelStatic;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.net.packages.ChatMessage;

public abstract class ClientEvent {
	// TODO pool events everywhere
	/** Tell the client which entity id is its player entity */
	public static class AssignPlayerId extends ClientEvent {
		public int id;

		public AssignPlayerId(int id) {
			this.id = id;
		}
	}

	/** Tell the client to create an entity to match one that already exists on the server */
	public static class CreateEntity extends ClientEvent {
		public int id;
		public boolean isPlayer;
		public Entity.EntityGraphicsType graphicsType;

		public CreateEntity(int id, boolean isPlayer, Entity.EntityGraphicsType graphicsType) {
			this.id = id;
			this.isPlayer = isPlayer;
			this.graphicsType = graphicsType;
		}
	}

	public static class DestroyEntity extends ClientEvent {
		public int id;

		public DestroyEntity(int id) {
			this.id = id;
		}
	}

	/** Tell the client when a bullet has hit something and to create the effect */
	public static class CreateBullet extends ClientEvent {
		public float x, y, z;

		public CreateBullet(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	public static class ReceivedChatMessage extends ClientEvent {
		public ChatMessage chat;

		public ReceivedChatMessage() {}

		public ReceivedChatMessage(ChatMessage chat) {
			this.chat = chat;
		}
	}

	public static class CreateLevelStatics extends ClientEvent {
		public LevelStatic[] staticPieces;

		public CreateLevelStatics(LevelStatic[] staticPieces) {
			this.staticPieces = staticPieces;
		}
	}
}
