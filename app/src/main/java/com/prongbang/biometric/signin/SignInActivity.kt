package com.prongbang.biometric.signin

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.prongbang.biometric.CIPHERTEXT_WRAPPER
import com.prongbang.biometric.R
import com.prongbang.biometric.SHARED_PREFS_FILENAME
import com.prongbang.biometric.biometric.BiometricPromptUtility
import com.prongbang.biometric.core.CryptographyManager
import kotlinx.android.synthetic.main.activity_sign_in.*

/**
 * https://medium.com/androiddevelopers/biometric-authentication-on-android-part-2-bc4d0dae9863
 * https://github.com/android/security-samples/tree/master/BiometricLoginKotlin
 */
class SignInActivity : AppCompatActivity() {

	private val fakeToken = "875d83ab-4e0d-4a47-813b-573c08fcc11a"
	private var fakeTokenDecrypt = ""
	private val secretKeyName = "MySecretKey"
	private lateinit var biometricPrompt: BiometricPrompt
	private val cryptographyManager by lazy { CryptographyManager() }
	private val ciphertextWrapper
		get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
				applicationContext,
				SHARED_PREFS_FILENAME,
				Context.MODE_PRIVATE,
				CIPHERTEXT_WRAPPER
		)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_sign_in)
		initListener()
	}

	private fun initListener() {
		registerButton.setOnClickListener {
			showBiometricPromptForEncryption()
		}
		biometricButton.setOnClickListener {
			showBiometricPromptForDecryption()
		}
	}

	private fun showBiometricPromptForDecryption() {
		ciphertextWrapper?.let { textWrapper ->
			val canAuthenticate = BiometricManager.from(applicationContext)
					.canAuthenticate()
			if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
				val cipher = cryptographyManager.getInitializedCipherForDecryption(secretKeyName,
						textWrapper.initializationVector)
				biometricPrompt = BiometricPromptUtility.createBiometricPrompt(this,
						::decryptServerTokenFromStorage)
				val promptInfo = BiometricPromptUtility.createPromptInfo(this)
				biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
			}
		}
	}

	private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
		ciphertextWrapper?.let { textWrapper ->
			authResult.cryptoObject?.cipher?.let {
				val plaintext = cryptographyManager.decryptData(textWrapper.ciphertext, it)
				fakeTokenDecrypt = plaintext
				// Now that you have the token, you can query server for everything else
				// the only reason we call this fakeToken is because we didn't really get it from
				// the server. In your case, you will have gotten it from the server the first time
				// and therefore, it's a real token.

				Toast.makeText(this, "Sign In Success : $plaintext", Toast.LENGTH_SHORT)
						.show()
			}
		}
	}

	private fun showBiometricPromptForEncryption() {
		val canAuthenticate = BiometricManager.from(applicationContext)
				.canAuthenticate()
		if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
			val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
			val biometricPrompt = BiometricPromptUtility.createBiometricPrompt(this,
					::encryptAndStoreServerToken)
			val promptInfo = BiometricPromptUtility.createPromptInfo(this)
			biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
		}
	}

	private fun encryptAndStoreServerToken(authResult: BiometricPrompt.AuthenticationResult) {
		authResult.cryptoObject?.cipher?.apply {
			fakeToken.let { token ->
				Log.d(TAG, "The token from server is $token")
				val encryptedServerTokenWrapper = cryptographyManager.encryptData(token, this)
				cryptographyManager.persistCiphertextWrapperToSharedPrefs(
						encryptedServerTokenWrapper,
						applicationContext,
						SHARED_PREFS_FILENAME,
						Context.MODE_PRIVATE,
						CIPHERTEXT_WRAPPER
				)
				Toast.makeText(this@SignInActivity,
						"Register Success : ${encryptedServerTokenWrapper.ciphertext}",
						Toast.LENGTH_SHORT)
						.show()
			}
		}
	}

	companion object {
		const val TAG = "SignInActivity"
		fun navigate(context: Context) {
			context.startActivity(Intent(context, SignInActivity::class.java))
		}
	}
}