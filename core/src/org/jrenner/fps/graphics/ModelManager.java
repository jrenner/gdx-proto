package org.jrenner.fps.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import org.jrenner.fps.Tools;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class ModelManager {
	public static ModelManager inst;
	private Array<Model> list;

	private Model playerModel;

	public ModelManager() {
		list = new Array<>();
		inst = this;
	}

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
		Node faceNode = mb.node("face", mb2.createBox(w/2, h/2, d/2, faceMaterial, attr));
		faceNode.translation.set(w/2, h/2, d/2);
		playerModel = mb.end();
	}

	public Model getPlayerModel() {
		return playerModel;
	}
}
