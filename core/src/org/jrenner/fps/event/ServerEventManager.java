package org.jrenner.fps.event;

import com.badlogic.gdx.utils.Array;
import org.jrenner.fps.Main;

import static org.jrenner.fps.event.ServerEvent.*;

public class ServerEventManager {
	private final Array<ServerEvent> eventQueue = new Array<>();

	public void process() {
		 synchronized (eventQueue) {
			 for (ServerEvent event : eventQueue) {
				 if (event instanceof CreateMonster) {
					 CreateMonster monster = (CreateMonster) event;
					 Main.inst.server.createMonster();
				 }
			 }
			 eventQueue.clear();
		 }
	}

	public void addEventToQueue(ServerEvent event) {
		synchronized (eventQueue) {
			eventQueue.add(event);
		}
	}
}
