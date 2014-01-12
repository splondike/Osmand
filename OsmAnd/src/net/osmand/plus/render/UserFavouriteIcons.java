package net.osmand.plus.render;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Provides and caches references to the user supplied favourite icons.
 */
public class UserFavouriteIcons {
	private File iconPath = null;
	private Map<String, Bitmap> iconCache = new HashMap<String, Bitmap>();

	public UserFavouriteIcons(File path) {
		this.iconPath = path;
	}

	/**
	 * Looks up the icon corresponding to key.
	 *
	 * @param key The name of the icon to find.
	 * @return The bitmap, or null if it couldn't be found.
	 */
	public Bitmap get(String key) {
		if (this.iconCache.containsKey(key)) {
			return this.iconCache.get(key);
		}
		else {
			Bitmap icon = this.loadIcon(key);
			if (icon != null) {
				this.iconCache.put(key, icon);
			}

			return icon;
		}
	}

	private Bitmap loadIcon(String key) {
		File file = new File(this.iconPath, key + ".png");

		return BitmapFactory.decodeFile(file.getAbsolutePath());
	}
}