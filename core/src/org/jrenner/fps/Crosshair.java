package org.jrenner.fps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Crosshair {

	// TODO this is temporary, we eventually want an asset loaded png, so it is managed
	public static Sprite create() {
		int w = View.width() / 100;
		int h = w;
		Pixmap pix = new Pixmap(w, h, Pixmap.Format.RGBA8888);
		pix.setColor(1f, 1f, 1f, 0.8f);
		pix.drawLine(w/2, 0, w/2, h);
		pix.drawLine(0, h/2, w, h/2);
		pix.setColor(0f, 0f, 0f, 0f);
		pix.drawPixel(w/2, h/2);
		Texture tex = new Texture(pix);
		Sprite sprite = new Sprite(tex);
		return sprite;
		// TODO we don't dispose anything, this is just a temporary crosshair
	}
}
