package com.prongbang.biometric

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.prongbang.biometric.core.CryptographyManager
import com.prongbang.biometric.signin.SignInActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

	private lateinit var biometricPrompt: BiometricPrompt
	private lateinit var promptInfo: BiometricPrompt.PromptInfo
	private lateinit var ciphertext: ByteArray
	private lateinit var initializationVector: ByteArray
	private var readyToEncrypt: Boolean = false
	private val secretKeyName = "biometric_sample_encryption_key"
	private val cryptographyManager by lazy { CryptographyManager() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		biometricPrompt = instanceOfBiometricPrompt()
		promptInfo = getPromptInfo()

		initView()
	}

	private fun initView() {
		encryptButton.setOnClickListener {
			authenticateToEncrypt()
		}

		decryptButton.setOnClickListener {
			authenticateToDecrypt()
		}

		navigateSignInButton.setOnClickListener {
			SignInActivity.navigate(this)
		}
	}

	private fun authenticateToDecrypt() {
		readyToEncrypt = false
		if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
			val cipher = cryptographyManager.getInitializedCipherForDecryption(secretKeyName, initializationVector)
			biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
		}
	}

	private fun authenticateToEncrypt() {
		readyToEncrypt = true
		if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
			val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
			biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
		}
	}

	/**
	 * https://android-developers.googleblog.com/2019/10/one-biometric-api-over-all-android.html
	 */
	private fun biometricAuthentication() {
		val biometricManager = BiometricManager.from(this)
		val canAuthenticate = biometricManager.canAuthenticate()
		if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
			biometricPrompt.authenticate(promptInfo)
		} else {
			showMessage("could not authenticate because: $canAuthenticate")
		}
	}

	private fun getPromptInfo(): BiometricPrompt.PromptInfo {
		return BiometricPrompt.PromptInfo.Builder()
				.setTitle("Sign in")
				.setSubtitle("Biometric for My App")
				.setDescription("Confirm biometric to continue")
				.setNegativeButtonText("Use Account Password")
				.setConfirmationRequired(false)
				// .setDeviceCredentialAllowed(true) // Allow PIN/pattern/password authentication.
				// Also note that setDeviceCredentialAllowed and setNegativeButtonText are
				// incompatible so that if you uncomment one you must comment out the other
				.build()
	}

	private fun instanceOfBiometricPrompt(): BiometricPrompt {
		val executor = ContextCompat.getMainExecutor(this)

		val callback = object : BiometricPrompt.AuthenticationCallback() {
			override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
				super.onAuthenticationError(errorCode, errString)
				showMessage("$errorCode :: $errString")
			}

			override fun onAuthenticationFailed() {
				super.onAuthenticationFailed()
				showMessage("Authentication failed for an unknown reason")
			}

			override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
				super.onAuthenticationSucceeded(result)
				showMessage("Authentication was successful")
				processData(result.cryptoObject)
			}
		}

		return BiometricPrompt(this, executor, callback)
	}

	/**
	 * https://medium.com/androiddevelopers/using-biometricprompt-with-cryptoobject-how-and-why-aace500ccdb7
	 * https://github.com/isaidamier/blogs.biometrics.cryptoBlog
	 */
	private fun processData(cryptoObject: BiometricPrompt.CryptoObject?) {
		val data = if (readyToEncrypt) {
			val text = inputText.text.toString()
			val encryptedData = cryptographyManager.encryptData(text, cryptoObject?.cipher!!)
			ciphertext = encryptedData.ciphertext
			initializationVector = encryptedData.initializationVector

			String(ciphertext, Charset.forName("UTF-8"))
		} else {
			cryptographyManager.decryptData(ciphertext, cryptoObject?.cipher!!)
		}
		outputText.text = data
	}

	private fun showMessage(message: String) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT)
				.show()
	}
}