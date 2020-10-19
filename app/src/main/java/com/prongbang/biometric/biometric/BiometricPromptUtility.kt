package com.prongbang.biometric.biometric

import android.app.Activity
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.prongbang.biometric.R

object BiometricPromptUtility {

	private const val TAG = "BiometricPromptUtility"

	fun createBiometricPrompt(
			activity: FragmentActivity,
			processSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
	): BiometricPrompt {
		val executor = ContextCompat.getMainExecutor(activity)

		val callback = object : BiometricPrompt.AuthenticationCallback() {

			override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
				super.onAuthenticationError(errCode, errString)
				Log.d(TAG, "errCode is $errCode and errString is: $errString")
			}

			override fun onAuthenticationFailed() {
				super.onAuthenticationFailed()
				Log.d(TAG, "Biometric authentication failed for unknown reason.")
			}

			override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
				super.onAuthenticationSucceeded(result)
				Log.d(TAG, "Authentication was successful")
				processSuccess(result)
			}
		}
		return BiometricPrompt(activity, executor, callback)
	}

	fun createPromptInfo(activity: FragmentActivity): BiometricPrompt.PromptInfo =
			BiometricPrompt.PromptInfo.Builder().apply {
				setTitle(activity.getString(R.string.prompt_info_title))
				setSubtitle(activity.getString(R.string.prompt_info_subtitle))
				setDescription(activity.getString(R.string.prompt_info_description))
				setConfirmationRequired(false)
				setNegativeButtonText(activity.getString(R.string.prompt_info_use_app_password))
			}.build()
}