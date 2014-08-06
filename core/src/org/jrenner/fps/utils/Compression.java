package org.jrenner.fps.utils;

import com.badlogic.gdx.utils.Array;
import org.jrenner.fps.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {
	public static byte[] writeCompressedString(String s) {
		GZIPOutputStream gzout = null;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			gzout = new GZIPOutputStream(bout);
			gzout.write(s.getBytes());
			gzout.flush();
			gzout.close();
			return bout.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (gzout != null) {
				try {
					gzout.flush();
					gzout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static String decompressToString(byte[] bytes) {
		GZIPInputStream gzin = null;
		try {
			gzin = new GZIPInputStream(new ByteArrayInputStream(bytes));
			byte[] buf = new byte[8192];
			byte[] storage = new byte[65536];
			int n = 0;
			int total = 0;
			while (true) {
				n = gzin.read(buf);
				if (n == -1) break;
				// expand to meet needs
				if (total + n >= storage.length) {
					byte[] expanded = new byte[storage.length * 2];
					System.arraycopy(storage, 0, expanded, 0, storage.length);
					storage = expanded;
				}
				System.out.printf("blen: %d, storlen: %d, total: %d, n: %d\n", buf.length, storage.length, total, n);
				System.arraycopy(buf, 0, storage, total, n);
				total += n;
			}
			Log.debug("read " + total + " bytes from compressed files");
			byte[] result = new byte[total];
			System.arraycopy(storage, 0, result, 0, total);
			return new String(result);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (gzin != null) {
				try {
					gzin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;

	}
}
