package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;
import net.osmand.plus.render.UserFavouriteIcons;

import org.joda.time.DateTime;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.widget.Toast;
import timesparser.Maybe;
import timesparser.TimeExtractor;
import timesparser.WeekIntervals;

public class FavoritesLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private static final int startZoom = 6;
	
	private OsmandMapTileView view;
	private Paint paint;
	private FavouritesDbHelper favorites;
	private Bitmap defaultFavoriteIcon;
	private UserFavouriteIcons userFavouriteIcons;
	private Resources resources;
	
	
	public FavoritesLayer(){
	}
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		
		favorites = view.getApplication().getFavorites();
		
		defaultFavoriteIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_favourite);
		userFavouriteIcons = new UserFavouriteIcons(view.getApplication().getAppPath("favourite_icons"));
		resources = view.getContext().getResources();
	}
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	@Override
	public void destroyLayer() {
		
	}
	

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			// request to load
			final QuadRect latLonBounds = tileBox.getLatLonBounds();
			for (FavouritePoint o : favorites.getFavouritePoints()) {
				if (o.getLatitude() >= latLonBounds.bottom && o.getLatitude() <= latLonBounds.top  && o.getLongitude() >= latLonBounds.left
						&& o.getLongitude() <= latLonBounds.right ) {
					int x = (int) tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());

					Bitmap favoriteIcon = this.userFavouriteIcons.get(o.getCategory());
					if (favoriteIcon == null) favoriteIcon = this.defaultFavoriteIcon;

					BitmapDrawable favIconDrawable = new BitmapDrawable(resources, favoriteIcon);
					Rect bounds = new Rect(x - favoriteIcon.getWidth() / 2, y - favoriteIcon.getHeight() / 2,
										   x + favoriteIcon.getWidth() / 2, y + favoriteIcon.getHeight() / 2);
					favIconDrawable.setBounds(bounds);

					if (isNotOpen(o.getName())) {
						favIconDrawable.setAlpha(128);
					}
					favIconDrawable.draw(canvas);
				}
			}
		}
	}

	private Map<String, WeekIntervals> intervalsCache = new HashMap<String, WeekIntervals>();
	private boolean isNotOpen(String description) {
		WeekIntervals intervals = intervalsCache.get(description);
		if (intervals == null) {
			intervals = FavoritesLayer.parseTimes(description);
			this.intervalsCache.put(description, intervals);
		}
		if (intervals != null) {
			return !intervals.contains(Calendar.getInstance());
		}
		else {
			return false;
		}
	}

	private static WeekIntervals parseTimes(String description) {
		String timeSentence = FavoritesLayer.extractTimeSentence(description);
		if (timeSentence != null) {
			Maybe<WeekIntervals> result = TimeExtractor.parseTimes(timeSentence);
			if (result.isKnown()) {
				return result.iterator().next();
			}
			else {
				return null;
			}
		}

		return null;
	}

	/**
	 * Extracts the time sequence from a description if it exists, minus fluff like fullstops and 'Open '.
	 */
	private static String extractTimeSentence(String description) {
		String trimmedDescription = description.trim();
		Boolean endsWithPeriod = trimmedDescription.endsWith("\\.");
		String withoutTrailingPeriod = endsWithPeriod ? description.substring(0, description.length() - 1) : trimmedDescription;

		String[] sentences = withoutTrailingPeriod.split("\\.");
		if (sentences.length > 0) {
			String lastSentence = sentences[sentences.length - 1].trim();
			if (lastSentence.startsWith("Open ")) {
				String withoutOpen = lastSentence.replaceFirst("Open ", "");
				return withoutOpen;
			}
		}

		return null;
	}
	
	
	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	public void getFavoriteFromPoint(RotatedTileBox tb, PointF point, List<? super FavouritePoint> res) {
		int r = (int) (15 * tb.getDensity());
		int ex = (int) point.x;
		int ey = (int) point.y;
		for (FavouritePoint n : favorites.getFavouritePoints()) {
			int x = (int) tb.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tb.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, r)) {
				res.add(n);
			}
		}
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		List<FavouritePoint> favs = new ArrayList<FavouritePoint>();
		getFavoriteFromPoint(tileBox, point, favs);
		if(!favs.isEmpty()){
			StringBuilder res = new StringBuilder();
			int i = 0;
			for(FavouritePoint fav : favs) {
				if (i++ > 0) {
					res.append("\n\n");
				}
				res.append(view.getContext().getString(R.string.favorite) + " : " + fav.getName());  //$NON-NLS-1$
			}
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}


	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof FavouritePoint){
			return view.getContext().getString(R.string.favorite) + " : " + ((FavouritePoint)o).getName(); //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public String getObjectName(Object o) {
		if(o instanceof FavouritePoint){
			return ((FavouritePoint)o).getName(); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		getFavoriteFromPoint(tileBox, point, res);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof FavouritePoint){
			return new LatLon(((FavouritePoint)o).getLatitude(), ((FavouritePoint)o).getLongitude());
		}
		return null;
	}
	
	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if(o instanceof FavouritePoint) {
			final FavouritePoint a = (FavouritePoint) o;
			OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (itemId == R.string.favourites_context_menu_delete) {
						final Resources resources = view.getContext().getResources();
						Builder builder = new AlertDialog.Builder(view.getContext());
						builder.setMessage(resources.getString(R.string.favourites_remove_dialog_msg, a.getName()));
						builder.setNegativeButton(R.string.default_buttons_no, null);
						builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								favorites.deleteFavourite(a);
								view.refreshMap();
							}
						});
						builder.create().show();
					}
				}
			};
			
			adapter.item(R.string.favourites_context_menu_delete).icons(R.drawable.ic_action_delete_dark, 
					R.drawable.ic_action_delete_light).listen(listener).reg();
		}
	}
	

}


