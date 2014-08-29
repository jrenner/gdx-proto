package org.jrenner.fps.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import org.jrenner.fps.Assets;
import org.jrenner.fps.Tools;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class ModelManager {
	public static ModelManager inst;
	private Array<Model> list;

	private Model playerModel;
	private Model billboardTestModel;

	public ModelManager() {
		list = new Array<>();
		inst = this;
	}

	public void init() {
		createPlayerModel();
		createBillboardTest();
	}

	/** players are represented by cubes, with another cube marking the direction it is facing */
	public void createPlayerModel() {
		ModelBuilder mb = new ModelBuilder();
		ModelBuilder mb2 = new ModelBuilder();
		long attr = Usage.Position | Usage.Normal;
		float r = 0.5f;
		float g = 1f;
		float b = 0.75f;
		Material material = new Material(ColorAttribute.createDiffuse(new Color(r, g, b, 1f)));
		Material faceMaterial = new Material(ColorAttribute.createDiffuse(Color.BLUE));
		float w = 1f;
		float d = w;
		float h = 2f;
		mb.begin();
		//playerModel = mb.createBox(w, h, d, material, attr);
		Node node = mb.node("box", mb2.createBox(w, h, d, material, attr));
		// the face is just a box to show which direction the player is facing
		Node faceNode = mb.node("face", mb2.createBox(w/2, h/2, d/2, faceMaterial, attr));
		faceNode.translation.set(0f, 0f, d/2);
		playerModel = mb.end();
	}

	public Model getPlayerModel() {
		return playerModel;
	}

	public void createBillboardTest() {
		ModelBuilder mb = new ModelBuilder();
		mb.begin();

		long attr = Usage.TextureCoordinates | Usage.Position;
		TextureRegion region = Assets.getAtlas().findRegion("sprites/test-guy");
		Material mat = new Material(TextureAttribute.createDiffuse(region.getTexture()));
		mat.set(new BlendingAttribute());
		MeshPartBuilder mpb = mb.part("rect", GL20.GL_TRIANGLES, attr, mat);
		mpb.setUVRange(region);
		// the coordinates are offset so that we can easily set the center position to align with the entity's body
		float sz = 2f; // size
		float b = -sz/2; // base
		float max = sz/2; // max
		Vector3 bl = new Vector3(b, b, b);
		Vector3 br = new Vector3(b, max, b);
		Vector3 tr = new Vector3(max, max, b);
		Vector3 tl = new Vector3(max, b, b);
		Vector3 norm = new Vector3(0f, 0f, 1f);
		mpb.rect(bl, tl, tr, br, norm);
		billboardTestModel = mb.end();
	}

	public Model getBillboardModel() {
		return billboardTestModel;
	}
}
