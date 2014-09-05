
package org.jrenner.fps.graphics;

import org.jrenner.fps.View;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.Pool;

/**
 * The LODRenderable class is responsible for managing the level of detail for an object.
 * HOW: replace @ModelInstance class with this class and use @put() to put some models in it
 * 
 * LOD level 1 is the highest resolution model and the last should be the model with lowest polygon.
 * 
 * In most cases you should use the initial 3 levels provided by default.
 * 
 * TODO be able to texture stream
 * 
 * @author Simon Bothen
 *
 */
public class LODRenderable implements RenderableProvider {
	public static float MAX_LOD_AREA = 40 * 40;
	
	public static int LOD_LEVEL_HIGH = 1;
	public static int LOD_LEVEL_MID = 2;
	public static int LOD_LEVEL_LOW = 3;

	protected static Vector3 dist = new Vector3();

	private Vector3 position;
	private OrderedMap<Integer, ModelInstance> lods;

	/**
	 * This creates a lod with 3 levels
	 * @param model the highres model
	 */
	public LODRenderable (ModelInstance model) {
		this(model, 3);
	}

	/**
	 * Use this class if you want to override the amount of LOD levels
	 * @param model the highres model
	 * @param maxLevel Override the default 3 levels by a custom value
	 */
	public LODRenderable (ModelInstance model, int levels) {
		this.lods = new OrderedMap<>(levels);
		this.position = new Vector3();
		this.lods.put(1, model);
		this.currenctLod = 1;
	}
	
	/**
	 * @param model the model you want to use
	 * @param nodes an array of nodes that will be used for LOD, the first string should be the highres node
	 */
	public LODRenderable (Model model, String... nodes) {
		this.lods = new OrderedMap<>(nodes.length);
		this.position = new Vector3();
		for (int i = 0; i < nodes.length; i++) {
			this.lods.put((i + 1), new ModelInstance(model, nodes[i]));
		}
		this.currenctLod = 1;
	}

	private int currenctLod;
	
	/** Be sure to use calculate to update the LOD model
	 * @return */
	public LODRenderable calculate () {
		if (lods.size <= 1) return this;
		
		// len2 for performance
		currenctLod = (int)((dist.set(View.inst.getCamera().position).sub(position).len2() / MAX_LOD_AREA) * lods.size); 

		// Ensure max size
		if (currenctLod > lods.size) currenctLod = lods.size;

		// Ensure that its higher than 1
		if (currenctLod <= 0) currenctLod = 1;

		// Didn't find LOD TODO Optimize me
		if (!lods.containsKey(currenctLod)) {
			int closest = currenctLod;
			while (!lods.containsKey(closest)) {
				closest--;
				if (closest <= 1) {
					closest = 1;
					break;
				}
			}
			currenctLod = closest;
		}

		return this;
	}

	public LODRenderable updatePosition (Vector3 pos) {
		updateTransformation(pos, null);
		return this;
	}

	public LODRenderable updateTransformation (Vector3 pos, Quaternion rot) {
		if (rot != null && pos == null) {
			getCurrent().transform.rotate(rot);
		} else if (pos != null && rot == null) {
			getCurrent().transform.setToTranslation(pos);
			position.set(pos);
		} else {
			getCurrent().transform.set(pos, rot);
			position.set(pos);
		}
		return this;
	}

	public void put (int lodlevel, ModelInstance model) {
		if (lodlevel <= 0 || lodlevel > lods.size)
			try {
				throw new Exception("LOD Level can not be lower that 1 or higher than the max value");
			} catch (Exception e) {
				e.printStackTrace();
			}
		else
			lods.put(lodlevel, model);
	}
	
	public ModelInstance getCurrent () {
		return lods.get(currenctLod);
	}
	
	public ModelInstance get(int level) {
		return lods.get(level);
	}

	public ModelInstance getFirst () {
		return lods.get(1);
	}

	public ModelInstance getLast () {
		return lods.get(lods.size);
	}

	@Override
	public void getRenderables (Array<Renderable> renderables, Pool<Renderable> pool) {
		lods.get(currenctLod).getRenderables(renderables, pool);
	}
}
