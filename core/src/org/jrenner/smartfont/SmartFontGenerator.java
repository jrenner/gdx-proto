package org.jrenner.smartfont;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jrenner.smartfont.writer.BitmapFontWriter;

/** Generate and cache freetype fonts dynamically.
 * see: https://github.com/jrenner/gdx-smart-font */
public class SmartFontGenerator {
	private static final String TAG = "SmartFontGenerator";
	private boolean forceGeneration;
	private String generatedFontDir;
	private int referenceScreenWidth;
	// TODO figure out optimal page size automatically
	private int pageSize;

	public SmartFontGenerator() {
		forceGeneration = false;
		generatedFontDir = "generated-fonts/";
		referenceScreenWidth = 1280;
		pageSize = 512; // size of atlas pages for font pngs
	}


	/** Will load font from file. If that fails, font will be generated and saved to file.
	 * @param fontFile the actual font (.otf, .ttf)
	 * @param fontName the name of the font, i.e. "arial-small", "arial-large", "monospace-10"
	 *                 This will be used for creating the font file names
	 * @param fontSize size of font when screen width equals referenceScreenWidth */
	public BitmapFont createFont(FileHandle fontFile, String fontName, int fontSize) {
		BitmapFont font = null;
		// if fonts are already generated, just load from file
		Preferences fontPrefs = Gdx.app.getPreferences("org.jrenner.smartfont");
		int displayWidth = fontPrefs.getInteger("display-width", 0);
		int displayHeight = fontPrefs.getInteger("display-height", 0);
		boolean loaded = false;
		if (displayWidth != Gdx.graphics.getWidth() || displayHeight != Gdx.graphics.getHeight()) {
			Gdx.app.debug(TAG, "Screen size change detected, regenerating fonts");
		} else {
			try {
				// try to load from file
				Gdx.app.debug(TAG, "Loading generated font from file cache");
				font = new BitmapFont(getFontFile(fontName + ".fnt"));
				loaded = true;
			} catch (GdxRuntimeException e) {
				Gdx.app.error(TAG, e.getMessage());
				Gdx.app.debug(TAG, "Couldn't load pre-generated fonts. Will generate fonts.");
			}
		}
		if (!loaded || forceGeneration) {
			forceGeneration = false;
			float width = Gdx.graphics.getWidth();
			float ratio = width / referenceScreenWidth; // use 1920x1280 as baseline, arbitrary
			float baseSize = 28f; // for 28 sized fonts at baseline width above

			// store screen width for detecting screen size change
			// on later startups, which will require font regeneration
			fontPrefs.putInteger("display-width", Gdx.graphics.getWidth());
			fontPrefs.putInteger("display-height", Gdx.graphics.getHeight());
			fontPrefs.flush();

			font = generateFontWriteFiles(fontName, fontFile, fontSize, pageSize, pageSize);
		}
		return font;
	}

	/** Convenience method for generating a font, and then writing the fnt and png files.
	 * Writing a generated font to files allows the possibility of only generating the fonts when they are missing, otherwise
	 * loading from a previously generated file.
	 * @param fontFile
	 * @param fontSize
	 */
	private BitmapFont generateFontWriteFiles(String fontName, FileHandle fontFile, int fontSize, int pageWidth, int pageHeight) {
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);

		PixmapPacker packer = new PixmapPacker(pageWidth, pageHeight, Pixmap.Format.RGBA8888, 2, false);
		FreeTypeFontGenerator.FreeTypeBitmapFontData fontData = generator.generateData(fontSize, FreeTypeFontGenerator.DEFAULT_CHARS, false, packer);
		Array<PixmapPacker.Page> pages = packer.getPages();
		TextureRegion[] texRegions = new TextureRegion[pages.size];
		for (int i=0; i<pages.size; i++) {
			PixmapPacker.Page p = pages.get(i);
			Texture tex = new Texture(new PixmapTextureData(p.getPixmap(), p.getPixmap().getFormat(), false, false, true)) {
				@Override
				public void dispose () {
					super.dispose();
					getTextureData().consumePixmap().dispose();
				}
			};
			tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
			texRegions[i] = new TextureRegion(tex);
		}
		BitmapFont font = new BitmapFont(fontData, texRegions, false);
		saveFontToFile(font, fontSize, fontName, packer);
		generator.dispose();
		packer.dispose();
		return font;
	}

	private void saveFontToFile(BitmapFont font, int fontSize, String fontName, PixmapPacker packer) {
		FileHandle fontFile = getFontFile(fontName + ".fnt"); // .fnt path
		FileHandle pixmapDir = getFontFile(fontName); // png dir path
		BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.Text);

		String[] pageRefs = BitmapFontWriter.writePixmaps(packer.getPages(), pixmapDir, fontName);
		Gdx.app.debug(TAG, String.format("Saving font [%s]: fontfile: %s, pixmapDir: %s\n", fontName, fontFile, pixmapDir));
		// here we must add the png dir to the page refs
		for (int i = 0; i < pageRefs.length; i++) {
			pageRefs[i] = fontName + "/" + pageRefs[i];
		}
		BitmapFontWriter.writeFont(font.getData(), pageRefs, fontFile, new BitmapFontWriter.FontInfo(fontName, fontSize), 1, 1);
	}

	private FileHandle getFontFile(String filename) {
		return Gdx.files.local(generatedFontDir + filename);
	}

	// GETTERS, SETTERS -----------------------
	
	public void setForceGeneration(boolean force) {
		forceGeneration = force;
	}
	
	public boolean getForceGeneration() {
		return forceGeneration;
	}

	/** Set directory for storing generated fonts */
	public void setGeneratedFontDir(String dir) {
		generatedFontDir = dir;
	}

	/** @see org.jrenner.smartfont.SmartFontGenerator#setGeneratedFontDir(String) */
	public String getGeneratedFontDir() {
		return generatedFontDir;
	}

	/** Set the reference screen width for computing sizes.  If reference width is 1280, and screen width is 1280
	 * Then the fontSize paramater will be unaltered when creating a font.  If the screen width is 720, the font size
	 * will by scaled down to (720 / 1280) of original size. */
	public void setReferenceScreenWidth(int width) {
		referenceScreenWidth = width;
	}

	/** @see org.jrenner.smartfont.SmartFontGenerator#setReferenceScreenWidth(int) */
	public int getReferenceScreenWidth() {
		return referenceScreenWidth;
	}

	/** Set the width and height of the png files to which the fonts will be saved.
	 * In the future it would be nice for page size to be automatically set to the optimal size
	 * by the font generator.  In the mean time it must be set manually. */
	public void setPageSize(int size) {
		pageSize = size;
	}

	/** @see org.jrenner.smartfont.SmartFontGenerator#setPageSize(int) */
	public int getPageSize() {
		return pageSize;
	}
}
