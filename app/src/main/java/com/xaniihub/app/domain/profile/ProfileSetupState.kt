package com.xaniihub.app.domain.profile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Remembers whether the user has actually entered body parameters.
 *
 * Skipping the onboarding used to save a full set of defaults (70 kg, 175 cm, 27 years, 1.2).
 * Calories were then computed for a 70 kg person and shown as if that were the user's own data -
 * which is exactly the "calories do not follow my weight" complaint, only reached through a
 * different door. The skip path now writes nothing: the repository keeps using its own fallbacks
 * for the calculation, and this flag lets the profile screen ask for a real weight instead of
 * silently pretending it already has one.
 */
object ProfileSetupState {
    private const val PREFS = "ringwalk_profile_setup"
    private const val KEY_BODY_PARAMS_PROVIDED = "body_params_provided"

    /** False only when we know the user never entered body parameters. */
    var bodyParamsProvided by mutableStateOf(true)
        private set

    /**
     * @param assumeProvided value used for installations that predate this flag. Anyone who had
     * already finished onboarding went through a profile write, so they must not be nagged.
     */
    fun init(context: Context, assumeProvided: Boolean) {
        bodyParamsProvided = prefs(context).getBoolean(KEY_BODY_PARAMS_PROVIDED, assumeProvided)
    }

    fun setBodyParamsProvided(context: Context, provided: Boolean) {
        bodyParamsProvided = provided
        prefs(context).edit().putBoolean(KEY_BODY_PARAMS_PROVIDED, provided).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
