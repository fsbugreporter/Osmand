package net.osmand.plus.mapmarkers.adapters;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;


public class CoordinateInputAdapter extends RecyclerView.Adapter<MapMarkerItemViewHolder> {

	public static final String ADAPTER_POSITION_KEY = "adapter_position_key";
	private final GPXUtilities.GPXFile gpx;

	private OsmandApplication app;

	private UiUtilities uiUtilities;
	private UpdateLocationViewCache updateViewCache;

	private boolean nightTheme;

	private View.OnClickListener listener;
	private View.OnClickListener actionsListener;

	public void setOnClickListener(View.OnClickListener listener) {
		this.listener = listener;
	}
	
	public void setOnActionsClickListener(View.OnClickListener actionsListener) {
		this.actionsListener = actionsListener;
	}
	
	public CoordinateInputAdapter(OsmandApplication app, GPXUtilities.GPXFile gpx) {
		this.app = app;
		this.gpx = gpx;

		uiUtilities = app.getUIUtilities();
		updateViewCache = uiUtilities.getUpdateLocationViewCache();
		nightTheme = !app.getSettings().isLightContent();
	}

	@NonNull
	@Override
	public MapMarkerItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.map_marker_item_new, parent, false);
		view.setOnClickListener(listener);
		return new MapMarkerItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final MapMarkerItemViewHolder holder, int position) {
		GPXUtilities.WptPt wpt = getItem(position);

		holder.iconDirection.setVisibility(View.VISIBLE);
		holder.icon.setImageDrawable(getColoredIcon(R.drawable.ic_action_flag_dark, wpt.colourARGB));
		holder.mainLayout.setBackgroundColor(getResolvedColor(nightTheme ? R.color.ctx_menu_bg_dark : R.color.bg_color_light));
		holder.title.setTextColor(getResolvedColor(nightTheme ? R.color.ctx_menu_title_color_dark : R.color.color_black));
		holder.divider.setBackgroundColor(getResolvedColor(nightTheme ? R.color.route_info_divider_dark : R.color.dashboard_divider_light));
		holder.optionsBtn.setImageDrawable(getColoredIcon(R.drawable.ic_overflow_menu_white, R.color.icon_color));
		holder.optionsBtn.setBackgroundColor(ContextCompat.getColor(app, nightTheme ? R.color.ctx_menu_bg_dark : R.color.bg_color_light));
		holder.iconReorder.setVisibility(View.GONE);
		holder.numberText.setVisibility(View.VISIBLE);
		holder.numberText.setText(String.valueOf(position + 1));
		holder.description.setVisibility(View.GONE);
		holder.optionsBtn.setOnClickListener(actionsListener);

		boolean singleItem = getItemCount() == 1;
		boolean fistItem = position == 0;
		boolean lastItem = position == getItemCount() - 1;

		holder.topDivider.setVisibility(fistItem ? View.VISIBLE : View.GONE);
		holder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);
		holder.divider.setVisibility((!singleItem && !lastItem) ? View.VISIBLE : View.GONE);

		holder.title.setText(wpt.name);
		uiUtilities.updateLocationView(updateViewCache, holder.iconDirection, holder.distance, wpt.lat, wpt.lon);
	}

	@Override
	public int getItemCount() {
		return gpx.getPointsSize();
	}

	public boolean isEmpty() {
		return getItemCount() == 0;
	}


	public GPXUtilities.WptPt getItem(int position) {
		return gpx.getPoints().get(position);
	}

	public void removeItem(int position) {
		if (position != RecyclerView.NO_POSITION) {
			gpx.deleteWptPt(getItem(position));
			notifyDataSetChanged();
		}
	}

	private Drawable getColoredIcon(@DrawableRes int resId, @ColorRes int colorResId) {
		return uiUtilities.getIcon(resId, colorResId);
	}

	@ColorInt
	private int getResolvedColor(@ColorRes int colorResId) {
		return ContextCompat.getColor(app, colorResId);
	}
}
