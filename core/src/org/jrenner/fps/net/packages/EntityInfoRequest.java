package org.jrenner.fps.net.packages;

import org.jrenner.fps.entity.Entity;

/** When client gets unknown entity id, it asks server for the Entity info
 * so it can create the entity
 */
public class EntityInfoRequest {
	public int id;

	public static class Response {
		public int id;
		public Entity.EntityGraphicsType graphicsType;
		// add more things like health, speed, graphics type, etc
	}
}
