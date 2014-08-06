package org.jrenner.fps.net;

public class ClientInfo {
	public int playerEntityId = -1;
	/** latest tick at which input was received from this player, used for client-side prediction */
	public int lastInputTick;
}
