package net.osmand.plus.helpers;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.routing.VoiceRouter.VoiceMessageListener;

public class LockHelper implements SensorEventListener {

	private static final int SENSOR_SENSITIVITY = 4;

	@Nullable
	private WakeLock wakeLock = null;

	private Handler uiHandler;
	private OsmandApplication app;
	private CommonPreference<Integer> turnScreenOnTime;
	private CommonPreference<Boolean> turnScreenOnSensor;


	@Nullable
	private LockUIAdapter lockUIAdapter;
	private Runnable lockRunnable;
	private VoiceMessageListener voiceMessageListener;

	public interface LockUIAdapter {

		void lock();

		void unlock();
	}

	public LockHelper(final OsmandApplication app) {
		this.app = app;
		uiHandler = new Handler();
		OsmandSettings settings = app.getSettings();
		turnScreenOnTime = settings.TURN_SCREEN_ON_TIME_INT;
		turnScreenOnSensor = settings.TURN_SCREEN_ON_SENSOR;

		lockRunnable = new Runnable() {
			@Override
			public void run() {
				lock();
			}
		};
		voiceMessageListener = new VoiceMessageListener() {
			@Override
			public void onVoiceMessage() {
				unlockEvent();
			}
		};
		app.getRoutingHelper().getVoiceRouter().addVoiceMessageListener(voiceMessageListener);
	}

	private void releaseWakeLocks() {
		if (wakeLock != null) {
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
			wakeLock = null;
		}
	}

	private void unlock(long timeInMills) {
		releaseWakeLocks();
		if (lockUIAdapter != null) {
			lockUIAdapter.unlock();
		}

		PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
		if (pm != null) {
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
					| PowerManager.ACQUIRE_CAUSES_WAKEUP, "tso:wakelocktag");
			wakeLock.acquire(timeInMills);
		}
	}

	private void lock() {
		releaseWakeLocks();
		if (lockUIAdapter != null) {
			lockUIAdapter.lock();
		}
	}

	private void timedUnlock(final long millis) {
		uiHandler.removeCallbacks(lockRunnable);
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				unlock(millis);
			}
		});
		uiHandler.postDelayed(lockRunnable, millis);
	}

	private void unlockEvent() {
		int unlockTime = turnScreenOnTime.get();
		if (unlockTime > 0) {
			timedUnlock(unlockTime * 1000L);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
			if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
				unlockEvent();
			}
		}
	}

	private void switchSensorOn() {
		switchSensor(true);
	}

	private void switchSensorOff() {
		switchSensor(false);
	}

	private void switchSensor(boolean on) {
		SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager != null) {
			Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			if (on) {
				sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
			} else {
				sensorManager.unregisterListener(this);
			}
		}
	}

	private boolean isSensorEnabled() {
		return turnScreenOnSensor.get() && app.getRoutingHelper().isFollowingMode();
	}

	public void onStart(@NonNull Activity activity) {
		if (wakeLock == null) {
			switchSensorOff();
		}
	}

	public void onStop(@NonNull Activity activity) {
		if (!activity.isFinishing() && isSensorEnabled()) {
			switchSensorOn();
		}
	}

	public void setLockUIAdapter(@Nullable LockUIAdapter adapter) {
		lockUIAdapter = adapter;
	}

}