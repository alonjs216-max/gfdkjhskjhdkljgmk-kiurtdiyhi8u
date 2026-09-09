package com.xaniihub.app.tracking

object TrackingConstants {
    const val TRACKING_CHANNEL_ID = "xaniihub_tracking"
    const val INACTIVITY_CHANNEL_ID = "xaniihub_inactivity"
    const val TRACKING_NOTIFICATION_ID = 1001
    const val INACTIVITY_NOTIFICATION_ID = 1002
    const val PREFS_NAME = "xaniihub_runtime"
    const val PREF_LATEST_STEPS = "latest_steps"
    const val PREF_LATEST_STEPS_DATE = "latest_steps_date"
    const val PREF_LAST_SENSOR_EVENT_TIME = "last_sensor_event_time"
    const val PREF_DAILY_GOAL = "daily_goal"

    /**
     * Raw TYPE_STEP_COUNTER value of the last ingested reading: the single baseline used to
     * derive step deltas. It is stored as a float because the sensor reports floats, which is
     * why it needs its own key - writing a float into one of the legacy int keys would throw
     * ClassCastException on existing installs.
     */
    const val PREF_LAST_SENSOR_COUNTER = "last_sensor_counter_value"

    /** Legacy keys of the old double-baseline bookkeeping. No longer read or written. */
    const val PREF_LATEST_COUNTER = "latest_sensor_counter"
    const val PREF_REPOSITORY_COUNTER = "repository_sensor_counter"
    const val PREF_COUNTER_DATE = "repository_sensor_counter_date"
}
