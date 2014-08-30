package org.jrenner.fps.entity;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import org.jrenner.fps.Direction;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.Physics;
import org.jrenner.fps.Player;
import org.jrenner.fps.Tools;
import org.jrenner.fps.effects.BlueExplosion;
import org.jrenner.fps.graphics.EntityBillboard;
import org.jrenner.fps.graphics.EntityModel;
import org.jrenner.fps.move.FlyingMovement;
import org.jrenner.fps.move.Movement;

public abstract class Entity {
	public int id = -1;
	public static Array<Entity> list;
	public static Array<Integer> destroyQueue;
	public static IntMap<Entity> idMap;
	public static Array<Integer> usedIDs;
	protected static Quaternion q = new Quaternion();
	protected static Matrix4 mtx = new Matrix4();
	public EntityInterpolator interpolator = new EntityInterpolator(this);
	public float health = 100f;


	protected btCollisionObject body;
	protected Vector3 bodyOffset = new Vector3();
	public Movement movement;
	public boolean onGround;
	public float distFromGround;
	/** height, width, depth dimensions of the entity */
	protected Vector3 dimen = new Vector3();
	protected Quaternion rotation = new Quaternion();
	/** the Physics class collision callbacks will set collision position change, which is processed during update */
	protected Vector3 collisionPositionChanges = new Vector3();
	protected int collisionPositionChangeCount = 0;

	/** Entities have three choices for graphical representation: a 3d model, a billboard (decal), and none */
	private EntityModel entityModel;

	/** null for non-player entity */
	protected Player player;

	public static enum EntityGraphicsType {
		Decal,
		Model,
		None
	}

	public Entity(EntityGraphicsType graphicsType) {
		synchronized (Entity.list) {
			list.add(this);
		}
		switch(graphicsType) {
			case Decal:
				//entityDecal = new EntityDecal(this);
				entityModel = new EntityBillboard(this);
				break;
			case Model:
				entityModel = new EntityModel(this);
				break;
			case None:
				// do nothing!
				break;
			default:
				throw new GdxRuntimeException("unhandled enum type");
		}
	}

	public static Entity getEntityById(int id) {
		Entity ent = idMap.get(id);
		if (ent == null) {
			if (!Main.isClient()) {
				throw new GdxRuntimeException("Fatal Error: Server could not find entity by id: " + id);
				//Log.error("Server could not find entity by id: " + id);
			} else {
				// this is where the client sees an entity it hasn't heard of before, and asks the server
				// for info about it in order to create it
				Log.debug("Client couldn't find entity by id, requesting entity info from server");
				Main.getNetClient().requestEntityInfo(id);
			}
		}
		return ent;
	}

	public static boolean entityWithIdExists(int id) {
		synchronized (list) {
			for (Entity ent : list) {
				if (ent.id == id) {
					return true;
				}
			}
		}
		return false;
	}

	private static int nextEntityId;

	public static void assignEntityID(Entity ent) {
		if (usedIDs.contains(nextEntityId, false)) {
			nextEntityId++;
		}
		int id = nextEntityId;
		nextEntityId++;
		assignEntityID(ent, id);
	}

	public static void assignEntityID(Entity ent, int id) {
		for (Entity e : list) {
			if (e.id == id) {
				throw new GdxRuntimeException("Fatal Error: Cannot assign id to entity, other entity with id already exists: " + id);
			}
		}
		if (ent.id != -1) {
			throw new GdxRuntimeException("Fatal Error: Entity has already been assigned id: " + ent.id);
		}
		ent.id = id;
		usedIDs.add(id);
		idMap.put(id, ent);
	}

	public static void init() {
		list = new Array<>();
		destroyQueue = new Array<>();
		idMap = new IntMap<>();
		usedIDs = new Array<>();
		DynamicEntity.list = new Array<>();
		EntityModel.list = new Array<>();
	}

	public static void updateAll(float timeStep) {
		for (int i = 0; i < Entity.list.size; i++) {
			Entity ent = Entity.list.get(i);
			ent.update(timeStep);
		}
		processDestroyQueue();
	}

	void update(float timeStep) {
		// the server sets entity data directly, no need to handle updates from server or interpolate
		if (!Main.isServer()) {
			interpolator.update();
			if (player == null) {
				updateTransforms();
				return; // don't simulate movement/physics of non-player entities for clients
						// they are simulated completely server-side
			}
		}
		handleCollisions();
		updateDistFromGround();
		int groundRayHits = Physics.lastDistanceFromGroundRayHitCount;
		float avgDist = Physics.lastDistanceFromGroundAvgDist;
		// when the ray went the full length and did not hit the ground, NaN is the return value
		onGround = false;
		float embedThreshold = 0f;
		if (!Float.isNaN(distFromGround)) {
			Vector3 vel = getVelocity();
			if (distFromGround < 0.1f) {
				adjustPosition(tmp.set(0f, -distFromGround, 0f));
				if (vel.y < 0f) vel.y = 0f;
				onGround = true;
			}
			if (distFromGround < embedThreshold && groundRayHits == 4 && avgDist <= 0f) {
				// penetrating into the ground
				if (player != null) {
					Log.debug("ground embed adjust");
				}
				getPosition().y += -distFromGround;
			} else if (distFromGround > 0f) {
				Vector3 velocity = getVelocity();
				// cap velocity to distance from ground
				if (velocity.y < 0 && distFromGround - velocity.y <= 0f) {
					velocity.y = -distFromGround;
					System.out.println("cap velocity: " + velocity.y);
				}
			}
		}
		movement.update(timeStep, onGround);
		updateTransforms();
	}

	/** The way bullet works, there might be multiple collision points when two object collide with each other.
	 *  Therefore, we take the average of the position change caused by each collision and apply it as the final
	 *  position change.
	 */
	public void handleCollisions() {
		if (collisionPositionChangeCount > 0) {
			collisionPositionChanges.scl(1f / collisionPositionChangeCount);
			collisionPositionChanges.scl(-1f); // subtraction
			adjustPosition(collisionPositionChanges);
			collisionPositionChangeCount = 0;
			collisionPositionChanges.setZero();
		}
	}

	// TODO check this works properly
	public void faceTowards(Vector3 targ) {
		float desired = Tools.getAngleFromAtoB(movement.getPosition(), targ, Vector3.Y);
		rotation.setEulerAngles(-desired, rotation.getPitch(), rotation.getRoll());
	}

	public void updateTransforms() {
		// physics body transform
		q.setEulerAngles(rotation.getYaw(), 0f, 0f); // physics bodies only care about yaw
		mtx.set(q);
		mtx.setTranslation(tmp.set(getPosition()).add(bodyOffset));
		body.setWorldTransform(mtx);
	}

	public void setDestination(Vector3 d) {
		movement.setDestination(d);
	}

	public void setRelativeDestination(Vector3 delta) {
		tmp.set(movement.getPosition()).add(delta);
		setDestination(tmp);
	}

	/** Stops ground units from moving up when looking up.
	 * for example, if an entity is rotated to be facing almost straight up,
	 * this method relativizes the destination to be "in front of" the entity
	 * on the xz (ground) plane */
	public void setRelativeDestinationByYaw(Vector3 delta) {
		// TODO what happens if one of the added vectors == Vector3.Y? i.e. straight up or straight down
		relativizeByYaw(delta);
		tmp.set(movement.getPosition()).add(delta);
		setDestination(tmp);
	}

	/** transform vector based on the current rotation, but set pitch to zero, useful for relative directions like "forward" and "back" */
	public Vector3 relativizeByYaw(Vector3 v) {
		q.setEulerAngles(rotation.getYaw(), 0f, 0f);
		q.transform(v);
		return v;
	}

	/** transform vector by current rotation, makes vector relative to current facing */
	public Vector3 relativize(Vector3 v) {
		rotation.transform(v);
		return v;
	}


	public void setPosition(Vector3 pos) {
		if (pos == null) throw new NullPointerException();
		if (movement == null) throw new NullPointerException();
		movement.setPosition(pos);
	}

	public void setPosition(float x, float y, float z) {
		setPosition(tmp.set(x, y, z));
	}

	public void adjustPosition(Vector3 delta) {
		movement.getPosition().add(delta);
	}

	public void setVelocity(Vector3 vel) {
		movement.getVelocity().set(vel);
	}

	// TODO is there a more correct way to handle collision position changes?
	/** combines all collision position changes, which are averaged when processed during update */
	public void addCollisionPositionChange(Vector3 posDelta) {
		collisionPositionChangeCount++;
		collisionPositionChanges.add(posDelta);
	}

	public Movement getMovement() {
		return movement;
	}

	static Vector3 tmp = new Vector3();
	static Vector3 tmp2 = new Vector3();
	static Vector3 tmp3 = new Vector3();

	public Vector3 getPosition() {
		return movement.getPosition();
	}

	public Vector3 getVelocity() {
		return movement.getVelocity();
	}

	public void setRotation(Quaternion newRot) {
		rotation.set(newRot);
	}

	public void adjustRotation(Direction.Rotation rot) {
		System.out.println("adjust rotation: " + rot.vector);
		float rotSpeed = 4f;
		adjustYaw(-rot.vector.x * rotSpeed);
		adjustPitch(-rot.vector.y * rotSpeed);
		adjustRoll(-rot.vector.z * rotSpeed);
	}

	public Quaternion getRotation() {
		return rotation;
	}

	public void adjustVelocity(Vector3 delta) {
		movement.getVelocity().add(delta);
	}

	/** adjust velocity based on relative directions. i.e. Vector3.Z == forward, (0, 1, 1) == forward-up */
	public void adjustVelocityRelativeByYaw(Vector3 delta) {
		adjustVelocity(relativizeByYaw(delta));
	}

	public void setYawPitchRoll(float y, float p, float r) {
		getRotation().setEulerAngles(y, p, r);
	}

	public void setYawPitchRoll(Vector3 rot) {
		getRotation().setEulerAngles(rot.x, rot.y, rot.z);
	}

	public void lookAt(Vector3 pos) {
		tmp.set(pos).sub(getPosition());
		q.setFromCross(Vector3.Z, tmp.nor());
		setYawPitchRoll(q.getYaw(), getPitch(), getRoll());
	}

	public float getYaw() {
		return rotation.getYaw();
	}


	public void setYaw(float amt) {
		rotation.setEulerAngles(amt, rotation.getPitch(), rotation.getRoll());
	}

	public void adjustYaw(float amt) {
		float yaw = getYaw();
		yaw += amt;
		setYaw(yaw);
	}

	public float getPitch() {
		return rotation.getPitch();
	}

	public void setPitch(float amt) {
		rotation.setEulerAngles(rotation.getYaw(), amt, rotation.getRoll());
	}

	public void adjustPitch(float amt) {
		float pitch = getPitch();
		// avoid gimbal lock
		// technically could use Quaternions for free rotation, but not necessary for FPS
		pitch = MathUtils.clamp(pitch + amt, -89f, 89f);
		setPitch(pitch);
	}

	public float getRoll() {
		return rotation.getRoll();
	}

	public void setRoll(float amt) {
		rotation.setEulerAngles(rotation.getYaw(), rotation.getPitch(), amt);
	}

	public void adjustRoll(float amt) {
		float roll = getRoll();
		roll += amt;
		setRoll(roll);
	}

	public Vector3 getDimensions() {
		return dimen;
	}

	public btCollisionObject getBody() {
		return body;
	}

	public boolean isFlyingEntity() {
		return movement instanceof FlyingMovement;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		if (player == null) throw new GdxRuntimeException("cannot set to null (yet!)");
		this.player = player;
	}

	public static void destroy(int id) {
		Entity ent = getEntityById(id);
		ent.destroy();
	}

	private boolean destroyed;

	public void destroy() {
		if (destroyed) return;
		destroyed = true;
		synchronized (destroyQueue) {
			Log.debug("destroy entity, id: " + id);
			destroyQueue.add(id);
			if (Main.isServer()) {
				Main.inst.server.queueDestroyedEntity(id);
			}
			if (Main.isClient()) {
				new BlueExplosion(getPosition());
			}
		}
	}

	protected void removeFromGame() {
		synchronized (Entity.list) {
			list.removeValue(this, true);
		}
		if (entityModel != null) {
			EntityModel.list.removeValue(entityModel, true);
		}
		Physics.inst.removeBody(body);
	}

	public static void processDestroyQueue() {
		synchronized (destroyQueue) {
			for (int id : destroyQueue) {
				synchronized (Entity.list) {
					for (Entity ent : list) {
						if (ent.id == id) {
							ent.removeFromGame();
							// id should be unique, unless something is broken
							break;
						}
					}
				}
			}
			destroyQueue.clear();
		}
	}

	public void applyDamage(float dmg) {
		//Log.debug("Entity[" + id + "] took damage: " + dmg);
		health -= dmg;
		if (health <= 0f) {
			destroy();
		}
	}

	public void updateDistFromGround() {
		if (Main.isMobile()) {
			distFromGround = Physics.inst.distanceFromGroundFast(movement.getPosition(), dimen);
		} else {
			distFromGround = Physics.inst.distanceFromGround(movement.getPosition(), dimen);
		}
	}

	public EntityModel getEntityModel() {
		return entityModel;
	}

	public float getHeight() {
		return dimen.y;
	}

	public float getWidth() {
		return dimen.x;
	}

	public float getDepth() {
		return dimen.z;
	}

	// TODO make this more accurate
	public float getRadius() {
		return Math.max(dimen.z, (Math.max(dimen.x, dimen.y)));
	}

}

