package org.jrenner.fps.net;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import org.jrenner.fps.LevelStatic;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.net.client.ClientUpdate;
import org.jrenner.fps.net.packages.BulletPackage;
import org.jrenner.fps.net.packages.ChatMessage;
import org.jrenner.fps.net.packages.ClientRequest;
import org.jrenner.fps.net.packages.CommandPackage;
import org.jrenner.fps.net.packages.EntityInfoRequest;
import org.jrenner.fps.net.packages.EntityUpdate;
import org.jrenner.fps.net.packages.ServerMessage;
import org.jrenner.fps.net.packages.ServerUpdate;

public class NetManager {
	public static String host = "localhost";
	//public static String host = "www.superior-tactics.com";
	public static int tcpPort = 31055;
	public static int udpPort = 32055;
	public static int writeBufferSize = 256000;
	public static int objectBufferSize = 128000;

	public static void registerKryoClasses(Kryo k) {
		Class[] classes = new Class[]{
				ServerUpdate.class,
				EntityUpdate.class,
				EntityUpdate[].class,
				Vector3.class,
				Vector3[].class,
				Matrix4.class,
				float[].class,
				ClientUpdate.class,
				CommandPackage.class,
				ClientRequest.class,
				ServerMessage.class,
				ServerMessage.AssignPlayerEntityId.class,
				ServerMessage.DestroyEntity.class,
				ServerMessage.ServerInfo.class,
				ServerMessage.LevelGeometry.class,
				ChatMessage.class,
				EntityInfoRequest.class,
				EntityInfoRequest.Response.class,
				BulletPackage.class,
				Entity.EntityGraphicsType.class,
				LevelStatic.class,
				LevelStatic[].class,
		};
		for (Class clazz : classes) {
			k.register(clazz);
		}
	}
}
