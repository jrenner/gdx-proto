package org.jrenner.fps;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.RayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import org.jrenner.fps.entity.DynamicEntity;
import org.jrenner.fps.entity.Entity;

import static com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags;

// TODO make sure all bullet objects are properly disposed, specifically shapes

public class Physics implements Disposable {
	public static Physics inst;
	btCollisionConfiguration collisionConfig;
	btDispatcher dispatcher;
	MyContactListener contactListener;
	btBroadphaseInterface broadphaseInterface;
	private btCollisionWorld world;
	public Array<btCollisionShape> shapes;
	public Array<btCollisionObject> objects;

	private Array<Disposable> disposables;

	DebugDrawer debugDrawer;

	public Physics() {
		Bullet.init();
		shapes = new Array<>();
		objects = new Array<>();
		disposables = new Array<>();
		if (Main.isClient()) {
			debugDrawer = new DebugDrawer();
			debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
		}
		inst = this;
		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphaseInterface = new btDbvtBroadphase();
		// TODO is it worth comparing performance with other broadphase types?
		//broadphaseInterface = new btSimpleBroadphase();
		//broadphaseInterface = new btAxisSweep3(tmp.set(0f, -10, 0f), tmp2.set(GameWorld.WORLD_WIDTH, 200f, GameWorld.WORLD_DEPTH));
		world = new btCollisionWorld(dispatcher, broadphaseInterface, collisionConfig);
		if (Main.isClient()) {
			world.setDebugDrawer(debugDrawer);
		}
		contactListener = new MyContactListener();

		disposables.add(world);
		if (debugDrawer != null) {
			disposables.add(debugDrawer);
		}
		disposables.add(collisionConfig);
		disposables.add(dispatcher);
		disposables.add(broadphaseInterface);
		disposables.add(contactListener);
	}

	/** Static geometry, being static, will never collide with other static geometry, greatly increasing collision detection performance */
	public void addStaticGeometryToWorld(btCollisionObject obj) {
		short colGroup = STATIC_GEOMETRY;
		short colMask = DYNAMIC_ENTITIES;
		world.addCollisionObject(obj, colGroup, colMask);
	}

	/** Suitable for moving entites that need to interact with each other and static geometry */
	public void addDynamicEntityToWorld(btCollisionObject obj) {
		short colGroup = DYNAMIC_ENTITIES;
		short colMask = DYNAMIC_ENTITIES | STATIC_GEOMETRY;
		world.addCollisionObject(obj, colGroup, colMask);
	}

	// Collision filters
	public static final short STATIC_GEOMETRY  = 0x01;
	public static final short DYNAMIC_ENTITIES = 0x02;
	public static final short ALL_OBJECTS       = 0xFF;

	public static void applyStaticGeometryCollisionFlags(btCollisionObject obj) {
		obj.setCollisionFlags(CollisionFlags.CF_STATIC_OBJECT);
	}
	public static void applyDynamicEntityCollisionFlags(btCollisionObject obj) {
		obj.setCollisionFlags(CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK); // allows ContactListeners to be called for this object
	}

	/** This is where collisions are actually handled */
	class MyContactListener extends ContactListener {
		@Override
		public boolean onContactAdded(btManifoldPoint cp, btCollisionObject colObj0, int partId0, int index0, btCollisionObject colObj1, int partId1, int index1) {
			Entity a = getEntity(colObj0);
			Entity b = getEntity(colObj1);
			if (a != null || b != null) {
				// the distance between the point on body A that collided and the point on body B that collided
				float dist = cp.getDistance();
				// the normal vector of the collision
				cp.getNormalWorldOnB(norm);
				cp.getPositionWorldOnA(worldA);
				cp.getPositionWorldOnA(worldB);
				if (a != null) {
					entityCollision(a, norm, worldA, worldB, dist, b);
				}
				// for object b, we must reverse the normal direction
				if (b != null) {
					entityCollision(b, norm.scl(-1f), worldB, worldA, dist, a);
				}
			}
			return true;
		}

		private Entity getEntity(btCollisionObject obj) {
			if (obj.userData instanceof Entity) return (Entity) obj.userData;
			return null;
		}

		private Vector3 norm = new Vector3();
		private Vector3 worldA = new Vector3();
		private Vector3 worldB = new Vector3();

		/** this is where the magic happens! */
		private void entityCollision(Entity ent, Vector3 normal, Vector3 myPoint, Vector3 otherPoint, float dist, Entity otherEntity) {
			if (Math.abs(dist) < 0.01f) return; // ignoring tiny collisions seems to help stability?

			// Change Position
			tmp.set(myPoint).sub(otherPoint).nor().add(normal); // direction of vel + normal
			tmp.nor();
			Vector3 posDelta = tmp3.set(tmp).scl(dist);
			ent.addCollisionPositionChange(posDelta);

			// Change Velocity
			// slide along the faces of geometry, but bounce off other entities
			Vector3 vel = ent.getVelocity();
			boolean bounce = otherEntity != null;
			if (!bounce) {
				// slide along the wall
				// TODO don't slide "up" vertical inclines of a certain steepness
				projectVectorOntoPlane(vel, normal);
			} else {
				// bounce off of other entities
				/*float speed = vel.len();
				tmp4.set(vel).nor(); // entity unit velocity
				float dotScalar = tmp2.set(tmp4).dot(norm);
				tmp3.set(norm).scl(-2f * dotScalar);
				tmp2.set(tmp4).add(tmp3).nor();
				tmp2.scl(speed);
				// don't bounce up or down
				tmp2.y = 0f;
				ent.adjustVelocity(tmp2);
				otherEntity.adjustVelocity(tmp2.scl(-1f));*/
			}
		}
	}

	private static void projectVectorOntoPlane(Vector3 vec, Vector3 planeNorm) {
		// Formula: vector v1_projected = v1 - Dot(v1, n) * n;
		tmp2.set(planeNorm).scl(tmp.set(vec).dot(planeNorm));
		vec.sub(tmp2);
	}

	public static float TIME_STEP = 1 / 60f;
	public static float TIME_SCALE = 1f; // should probably always be 1f

	public void run() {
		world.performDiscreteCollisionDetection();
	}

	private static Matrix4 mtx = new Matrix4();

	public btCollisionObject createBoxObject(Vector3 boxSize) {
		btCollisionShape shape = new btBoxShape(boxSize);
		shapes.add(shape);
		btCollisionObject obj = new btCollisionObject();
		obj.setCollisionShape(shape);
		obj.setWorldTransform(mtx.idt());
		obj.setUserValue(objects.size);
		objects.add(obj);
		return obj;
	}

	public static btCollisionShape playerShape;

	public btCollisionObject createCapsuleObject(float radius, float height) {
		if (playerShape == null) {
			playerShape = new btCapsuleShape(radius, height);

		}
		shapes.add(playerShape);
		btCollisionObject obj = new btCollisionObject();
		obj.setCollisionShape(playerShape);
		obj.setWorldTransform(mtx.idt());
		obj.setUserValue(objects.size);
		objects.add(obj);
		return obj;
	}

	public void debugDraw() {
		debugDrawer.begin(View.inst.getCamera());
		world.debugDrawWorld();
		System.gc();
		/*for (DynamicEntity ent : DynamicEntity.list) {
			debugDrawDynamicEntity(ent);
		}*/
		debugDrawer.end();
	}

	public void debugDrawDynamicEntity(DynamicEntity ent) {
		world.debugDrawObject(ent.getBody().getWorldTransform(), playerShape, tmp.set(1f, 1f, 1f));
	}

	private static Vector3 tmp = new Vector3();
	private static Vector3 tmp2 = new Vector3();
	private static Vector3 tmp3 = new Vector3();
	private static Vector3 tmp4 = new Vector3();
	private static Vector3 tmp5 = new Vector3();

	private static Vector3[] rectVectors = new Vector3[]{new Vector3(), new Vector3(), new Vector3(), new Vector3()};

	public static int lastDistanceFromGroundRayHitCount;
	public static float lastDistanceFromGroundAvgDist;

	/** finds the distance from the bottom of the passed dimensions to the ground
	 * @return distance to the ground, or Float.NaN when raycast did not hit ground */
	public float distanceFromGround(Vector3 position, Vector3 dimen) {
		lastDistanceFromGroundRayHitCount = 0;
		lastDistanceFromGroundAvgDist = 0f;
		float lowest = Float.NaN;
		float downStep = 2f + dimen.y; // length of ray
		// test four corners
		float x = dimen.x / 4f;
		float z = dimen.z / 4f;
		float rayOriginHeight = 0f;
		// by starting the ray from the top of the dimensions
		// we can catch cases where the dimensions are already partially
		// embedded underneath the ground.
		// in this case, a negative distance will be returned
		rectVectors[0].set(position).add(-x, rayOriginHeight, -z);
		rectVectors[1].set(position).add(-x, rayOriginHeight, z);
		rectVectors[2].set(position).add(x, rayOriginHeight, z);
		rectVectors[3].set(position).add(x, rayOriginHeight, -z);
		for (int i = 0; i < rectVectors.length; i++) {
			Vector3 point = rectVectors[i];
			castRayStaticOnly(point, tmp.set(point).sub(0f, downStep, 0f));
			if (raycastReport.hit) {
				lastDistanceFromGroundAvgDist += raycastReport.hitDistance;
				lastDistanceFromGroundRayHitCount++;
				if (Float.isNaN(lowest) || raycastReport.hitDistance < lowest) {
					lowest = raycastReport.hitDistance;
				}
			}
		}
		// we started the ray from the top, but we want the distance to ground from the bottom
		if (!Float.isNaN(lowest)) {
			// if embedded in ground, a negative value will be returned
			lowest -= (rayOriginHeight + dimen.y/2f);
		}
		lastDistanceFromGroundAvgDist /= lastDistanceFromGroundRayHitCount;
		return lowest;
	}

	public float getShadowHeightAboveGround(Vector3 position) {
		castRay(position, tmp.set(position).sub(0f, Shadow.MAX_SHADOW_HEIGHT, 0f));
		return raycastReport.hitDistance;
	}


	/** the normal of the surface below a position through raycasting */
	public Vector3 getFloorNormal(Vector3 position) {
		float rayDist = 100f;
		castRay(position, tmp.set(position).sub(0f, rayDist, 0f));
		tmp.setZero();
		if (raycastReport.hit) {
			tmp.set(raycastReport.hitNormal);
		}
		return tmp;
	}

	private ClosestRayResultCallback staticRayCallback;
	//private ClosestNotMeRayResultCallback notMeRayCallback;
	private ClosestRayResultCallback rayCallback;


	private static void setCallbackRayPositions(ClosestRayResultCallback cb, Vector3 from, Vector3 to) {
		cb.setRayFromWorld(from);
		cb.setRayToWorld(to);
	}

	public void castRayStaticOnly(Vector3 position, Vector3 end) {
		if (staticRayCallback == null) {
			staticRayCallback = new ClosestRayResultCallback(position, end);
			staticRayCallback.setCollisionFilterGroup(DYNAMIC_ENTITIES);
			staticRayCallback.setCollisionFilterMask(STATIC_GEOMETRY);
		}
		staticRayCallback.setCollisionObject(null);
		staticRayCallback.setClosestHitFraction(1f);
		setCallbackRayPositions(staticRayCallback, position, end);
		executeRayCast(position, end, staticRayCallback);
	}

	public void castRay(Vector3 position, Vector3 end) {
		if (rayCallback == null) {
			rayCallback = new ClosestRayResultCallback(position, end);
			rayCallback.setCollisionFilterGroup(ALL_OBJECTS);
			rayCallback.setCollisionFilterMask(ALL_OBJECTS);
		}
		rayCallback.setCollisionObject(null);
		rayCallback.setClosestHitFraction(1f);
		setCallbackRayPositions(rayCallback, position, end);
		executeRayCast(position, end, rayCallback);
	}

	/*public void castRayNotMe(Vector3 position, Vector3 end, btCollisionObject self) {
		if (notMeRayCallback == null) {
			notMeRayCallback = new ClosestNotMeRayResultCallback(self);
			notMeRayCallback.setCollisionFilterGroup(ALL_OBJECTS);
			notMeRayCallback.setCollisionFilterMask(ALL_OBJECTS);
		}
		notMeRayCallback.setCollisionObject(nu
		rayCallback.getRayFromWorld().setValue(position.x, position.y, position.z);
		rayCallback.getRayToWorld().setValue(end.x, end.y, end.z);ll);
		notMeRayCallback.setClosestHitFraction(1f);
		notMeRayCallback.getRayFromWorld().setValue(position.x, position.y, position.z);
		notMeRayCallback.getRayToWorld().setValue(end.x, end.y, end.z);
		notMeRayCallback.
		executeRayCast(position, end, notMeRayCallback);
	}*/

	private void executeRayCast(Vector3 position, Vector3 end, RayResultCallback callback) {
		raycastReport.reset();
		world.rayTest(position, end, callback);
		raycastReport.hit = callback.hasHit();
		if (raycastReport.hit) {
			float length = position.dst(end);
			raycastReport.hitDistance = length * callback.getClosestHitFraction();
			if (callback instanceof ClosestRayResultCallback) {
				ClosestRayResultCallback cb = (ClosestRayResultCallback) callback;
				Vector3 normal = tmp;
				cb.getHitNormalWorld(tmp);
				raycastReport.hitNormal.set(normal.x, normal.y, normal.z);
			}
		}
	}

	public static class RaycastReport {
		public boolean hit;
		public Vector3 hitNormal = new Vector3();
		public float hitDistance;

		public void reset() {
			hit = false;
			hitNormal.setZero();
			hitDistance = -1f;
		}

		@Override
		public String toString() {
			return String.format("Hit: %s, Distance: %s, Normal: %s", hit, Tools.fmt(hitDistance), Tools.fmt(hitNormal));
		}
	}

	public RaycastReport raycastReport = new RaycastReport();

	public void removeBody(btCollisionObject body) {
		world.removeCollisionObject(body);
		body.dispose();
	}

	@Override
	public void dispose() {
		for (Disposable disp : disposables) {
			/*if (disp instanceof BulletBase) {
				System.out.println("ownership: " + ((BulletBase) disp).hasOwnership());
			}*/
			Tools.dispose(disp);
		}
		for (btCollisionShape shape : shapes) {
			shape.dispose();
		}
		for (btCollisionObject object : objects) {
			object.dispose();
		}
		disposables.clear();
		shapes.clear();
		objects.clear();
	}
}
