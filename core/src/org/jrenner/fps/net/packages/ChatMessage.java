package org.jrenner.fps.net.packages;

public class ChatMessage {
	public int playerId = -1;
	public String text;
	public long createTime;

	public ChatMessage() {}

	public ChatMessage(String txt) {
		this.text = txt;
	}

	@Override
	public String toString() {
		return String.format("playerId: %d, createTime: %d, text: %s",
				playerId, createTime, text);
	}
}
