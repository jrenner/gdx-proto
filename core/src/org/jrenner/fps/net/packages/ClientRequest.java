package org.jrenner.fps.net.packages;

public enum ClientRequest {
	CreateNewPlayerAssignID, // create a new entity for this player, and assign an entity ID
	RequestServerInfo,
	RequestLevelGeometry,
	RequestResetPlayerPosition,
}
