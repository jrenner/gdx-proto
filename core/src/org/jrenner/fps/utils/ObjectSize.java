package org.jrenner.fps.utils;

import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/** Get the approximate size of an object through serialization */
public class ObjectSize {
	public static long getObjectSize(Serializable ser) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(ser);
			oos.close();
		} catch (Exception e) {
			throw new GdxRuntimeException(e.toString());
		}
		return baos.size();
	}
}
