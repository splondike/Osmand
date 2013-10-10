package net.osmand.plus.render;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.natpryce.maybe.Maybe;

/**
 * Provides and caches references to the user supplied favourite icons.
 */
public class UserFavouriteIcons {
	private File iconPath = null;
	private Map<String, Bitmap> iconCache = new HashMap<String, Bitmap>();

	public UserFavouriteIcons(File path) {
		this.iconPath = path;
	}

	public Maybe<Bitmap> get(String key) {
		if (this.iconCache.containsKey(key)) {
			return Maybe.definitely(this.iconCache.get(key));
		}
		else {
			Maybe<Bitmap> icon = this.loadIcon(key);
			if (icon.isKnown()) {
				this.iconCache.put(key, icon.iterator().next());
			}

			return icon;
		}
	}

	private Maybe<Bitmap> loadIcon(String key) {
		File file = new File(this.iconPath, key + ".png");

		Bitmap icon = BitmapFactory.decodeFile(file.getAbsolutePath());
		if (icon == null) {
			return Maybe.unknown();
		}
		else {
			return Maybe.definitely(icon);
		}
	}
}