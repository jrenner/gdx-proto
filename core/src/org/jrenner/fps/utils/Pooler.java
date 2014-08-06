package org.jrenner.fps.utils;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import org.jrenner.fps.net.packages.CommandPackage;
import org.jrenner.fps.net.packages.EntityUpdate;

import org.jrenner.fps.net.packages.ServerUpdate;

/** convenient access to certain object pools */
public class Pooler {
	private static Vector3Pool v3Pool;
	private static Matrix4Pool mtxPool;
	private static ServerUpdatePool serverUpdatePool;
	private static EntityUpdatePool entityUpdatePool;
	private static MovementPackagePool movementPackagePool;

	public static void init() {
		v3Pool = new Vector3Pool();
		mtxPool = new Matrix4Pool();
		serverUpdatePool = new ServerUpdatePool();
		entityUpdatePool = new EntityUpdatePool();
		movementPackagePool = new MovementPackagePool();
	}



	public static class Vector3Pool extends CountingPool<Vector3> {
		@Override
		protected Vector3 newObject() {
			return new Vector3();
		}
	}
	public static Vector3 v3() {
		return v3Pool.obtain();
	}

	public static void free(Vector3 v3) {
		v3Pool.free(v3);
	}

	public static void free(Vector3 ...v3) {
		for (int i = 0; i < v3.length; i++) {
			v3Pool.free(v3[i]);
		}
	}



	public static class Matrix4Pool extends CountingPool<Matrix4> {
		@Override
		protected Matrix4 newObject() {
			return new Matrix4();
		}
	}

	public static Matrix4 mtx() {
		return mtxPool.obtain();
	}

	public static void free(Matrix4 mtx) {
		mtxPool.free(mtx);
	}

	public static void free(Matrix4 ...mtx) {
		for (int i = 0; i < mtx.length; i++) {
			mtxPool.free(mtx[i]);
		}
	}



	public static class ServerUpdatePool extends CountingPool<ServerUpdate> {
		@Override
		protected ServerUpdate newObject() {
			return new ServerUpdate();
		}
	}
	
	public static ServerUpdate serverUpdate() {
		return serverUpdatePool.obtain();
	}

	public static void free(ServerUpdate upd) {
		serverUpdatePool.free(upd);
	}

	public static void free(ServerUpdate ...upd) {
		for (int i = 0; i < upd.length; i++) {
			serverUpdatePool.free(upd[i]);
		}
	}



	public static class EntityUpdatePool extends CountingPool<EntityUpdate> {
		@Override
		protected EntityUpdate newObject() {
			return new EntityUpdate();
		}
	}

	public static EntityUpdate entityUpdate() {
		return entityUpdatePool.obtain();
	}

	public static void free(EntityUpdate upd) {
		entityUpdatePool.free(upd);
	}

	public static void free(EntityUpdate ...upd) {
		for (int i = 0; i < upd.length; i++) {
			entityUpdatePool.free(upd[i]);
		}
	}



	public static class MovementPackagePool extends CountingPool<CommandPackage> {
		@Override
		protected CommandPackage newObject() {
			return new CommandPackage();
		}
	}

	public static CommandPackage movementPackage() {
		return movementPackagePool.obtain();
	}

	public static void free(CommandPackage upd) {
		movementPackagePool.free(upd);
	}

	public static void free(CommandPackage...upd) {
		for (int i = 0; i < upd.length; i++) {
			movementPackagePool.free(upd[i]);
		}
	}


}
