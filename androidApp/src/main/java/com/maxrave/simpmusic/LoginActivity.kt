package com.maxrave.simpmusic

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("isLoggedIn", false)) {
            abrirMain()
            return
        }

        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bgColor = if (isNightMode) Color.parseColor("#0F0F0F") else Color.parseColor("#F5F5F5")
        val cardColor = if (isNightMode) Color.parseColor("#1A1A1A") else Color.WHITE
        val textColorPrimary = if (isNightMode) Color.WHITE else Color.BLACK
        val textColorSecondary = if (isNightMode) Color.parseColor("#AAAAAA") else Color.parseColor("#606060")
        val inputBgColor = if (isNightMode) Color.parseColor("#272727") else Color.parseColor("#F0F0F0")
        val inputHintColor = if (isNightMode) Color.parseColor("#757575") else Color.parseColor("#9E9E9E")
        val brandRed = Color.parseColor("#FF0000")

        val rootFrameLayout = FrameLayout(this).apply {
            setBackgroundColor(bgColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = 40f
            }
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(70, 80, 70, 80)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val logoId = resources.getIdentifier("mono", "drawable", packageName)
        val logoImage = ImageView(this).apply {
            if (logoId != 0) {
                setImageResource(logoId)
            }
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                setMargins(0, 0, 20, 0)
            }
        }
        if (logoId != 0) {
            headerLayout.addView(logoImage)
        }

        val titleText = TextView(this).apply {
            text = "YouTube Music"
            textSize = 24f
            setTextColor(textColorPrimary)
            setTypeface(Typeface.create("sans-serif", Typeface.BOLD))
            gravity = Gravity.CENTER_VERTICAL
        }
        headerLayout.addView(titleText)

        val subText = TextView(this).apply {
            text = "Tu música, tu estilo, en cualquier momento. \uD83C\uDFA7\uD83D\uDD25"
            textSize = 15f
            setTextColor(textColorSecondary)
            setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
            setLineSpacing(0f, 1.2f)
        }

        val inputBackground = GradientDrawable().apply {
            setColor(inputBgColor)
            cornerRadius = 24f
        }

        val usernameInput = EditText(this).apply {
            hint = "usuario"
            setHintTextColor(inputHintColor)
            setTextColor(textColorPrimary)
            background = inputBackground
            setPadding(50, 45, 50, 45)
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }
        }

        val passwordContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 50) }
        }

        val passwordInput = EditText(this).apply {
            hint = "contraseña"
            setHintTextColor(inputHintColor)
            setTextColor(textColorPrimary)
            background = inputBackground
            setPadding(50, 45, 150, 45)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val eyeIconDrawable = EyeDrawable(textColorPrimary)
        val togglePasswordButton = ImageView(this).apply {
            setImageDrawable(eyeIconDrawable)
            setPadding(20, 20, 20, 20)
            layoutParams = FrameLayout.LayoutParams(
                100, 100
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setMargins(0, 0, 20, 0)
            }
            var isVisible = false
            setOnClickListener {
                isVisible = !isVisible
                eyeIconDrawable.isEyeOpen = isVisible
                if (isVisible) {
                    passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                passwordInput.setSelection(passwordInput.text.length)
            }
        }

        passwordContainer.addView(passwordInput)
        passwordContainer.addView(togglePasswordButton)

        val errorText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.parseColor("#FF5252"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(0, 0, 0, 30)
        }

        val loginButton = Button(this).apply {
            text = "ENTRAR"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(brandRed)
                cornerRadius = 24f
            }
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                140
            )
        }

        val versionText = TextView(this).apply {
            text = "V1.5.0g"
            textSize = 12f
            setTextColor(textColorSecondary)
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 0)
        }

        cardLayout.addView(headerLayout)
        cardLayout.addView(subText)
        cardLayout.addView(usernameInput)
        cardLayout.addView(passwordContainer)
        cardLayout.addView(errorText)
        cardLayout.addView(loginButton)
        cardLayout.addView(versionText)
        mainContainer.addView(cardLayout)
        rootFrameLayout.addView(mainContainer)
        setContentView(rootFrameLayout)

        loginButton.setOnClickListener {
            val user = usernameInput.text.toString().trim()
            val pass = passwordInput.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                errorText.text = "Por favor, complete los campos"
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            errorText.visibility = View.GONE
            loginButton.text = "CARGANDO..."
            loginButton.isEnabled = false
            loginButton.background = GradientDrawable().apply {
                setColor(Color.parseColor("#B30000"))
                cornerRadius = 24f
            }

            lifecycleScope.launch {
                val result = performLogin(user, pass)

                if (result == "SUCCESS") {
                    prefs.edit()
                        .putBoolean("isLoggedIn", true)
                        .putString("saved_user", user)
                        .putString("saved_pass", pass)
                        .apply()

                    if (logoId != 0) {
                        val location = IntArray(2)
                        logoImage.getLocationInWindow(location)
                        val rootLocation = IntArray(2)
                        rootFrameLayout.getLocationInWindow(rootLocation)

                        val relativeX = location[0] - rootLocation[0]
                        val relativeY = location[1] - rootLocation[1]

                        val startSize = logoImage.width
                        val endSize = (resources.displayMetrics.widthPixels * 0.6).toInt()

                        val ghostLogo = ImageView(this@LoginActivity).apply {
                            setImageResource(logoId)
                            layoutParams = FrameLayout.LayoutParams(startSize, startSize)
                            translationX = relativeX.toFloat()
                            translationY = relativeY.toFloat()
                        }
                        rootFrameLayout.addView(ghostLogo)

                        logoImage.visibility = View.INVISIBLE

                        cardLayout.animate()
                            .scaleX(0.8f)
                            .scaleY(0.8f)
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                cardLayout.visibility = View.INVISIBLE
                            }
                            .start()

                        rootFrameLayout.post {
                            val targetX = (rootFrameLayout.width - endSize) / 2f
                            val targetY = (rootFrameLayout.height - endSize) / 2f

                            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                                duration = 600
                                interpolator = OvershootInterpolator(1.2f)
                                addUpdateListener { animation ->
                                    val progress = animation.animatedValue as Float
                                    val currentSize = startSize + ((endSize - startSize) * progress).toInt()
                                    val currentX = relativeX + ((targetX - relativeX) * progress)
                                    val currentY = relativeY + ((targetY - relativeY) * progress)

                                    ghostLogo.layoutParams = FrameLayout.LayoutParams(currentSize, currentSize)
                                    ghostLogo.translationX = currentX
                                    ghostLogo.translationY = currentY
                                }
                            }
                            animator.start()
                        }

                        rootFrameLayout.postDelayed({
                            abrirMain()
                        }, 1800)
                    } else {
                        abrirMain()
                    }

                } else {
                    loginButton.text = "ENTRAR"
                    loginButton.isEnabled = true
                    loginButton.background = GradientDrawable().apply {
                        setColor(brandRed)
                        cornerRadius = 24f
                    }
                    errorText.text = result
                    errorText.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun abrirMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun getCustomMacAddress(): String {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "1A2B3C4D5E6F7A8B"
        var processed = androidId.trimStart('0')
        if (processed.isEmpty()) {
            processed = "1A2B3C4D5E6F7A8B"
        }
        processed = processed.padEnd(16, 'A')
        processed = processed.substring(0, 16).uppercase()
        return processed.chunked(2).joinToString(":")
    }

    private suspend fun performLogin(user: String, pass: String): String = withContext(Dispatchers.IO) {
        try {
            val deviceMac = getCustomMacAddress()
            val userEnc = URLEncoder.encode(user, "UTF-8")
            val passEnc = URLEncoder.encode(pass, "UTF-8")
            val macEnc = URLEncoder.encode(deviceMac, "UTF-8")

            val encryptedBytes = intArrayOf(
                109, 121, 121, 117, 120, 63, 52, 52, 108, 102, 119, 106, 123, 126, 115, 117, 102, 115, 106, 113,
                120, 51, 113, 102, 121, 114, 117, 125, 51, 104, 116, 114, 52, 126, 116, 122, 121, 122, 103, 106,
                52, 117, 102, 115, 106, 113, 52, 102, 117, 110, 52, 117, 113, 102, 126, 106, 119, 100, 102, 117,
                110, 51, 117, 109, 117
            )
            val urlBuilder = java.lang.StringBuilder()
            for (byteVal in encryptedBytes) {
                urlBuilder.append((byteVal - 5).toChar())
            }
            val urlReal = urlBuilder.toString()
            val urlString = "$urlReal?username=$userEnc&password=$passEnc&mac=$macEnc"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)

                if (jsonObject.has("user_info")) {
                    val userInfo = jsonObject.getJSONObject("user_info")
                    val auth = userInfo.optInt("auth", 0)

                    if (auth == 1) {
                        val status = userInfo.optString("status", "Active")
                        val isInvalidStatus = status.equals("Expired", ignoreCase = true) ||
                            status.equals("Banned", ignoreCase = true) ||
                            status.equals("Disabled", ignoreCase = true)

                        if (isInvalidStatus) {
                            return@withContext "Cuenta inactiva: $status"
                        }

                        return@withContext "SUCCESS"
                    } else {
                        return@withContext userInfo.optString("status", "Acceso denegado")
                    }
                } else {
                    return@withContext "Respuesta de servidor inválida"
                }
            } else {
                return@withContext "Error del servidor: ${connection.responseCode}"
            }
        } catch (e: Exception) {
            return@withContext "Error de red: revisa tu conexión"
        }
    }
}

class EyeDrawable(color: Int) : Drawable() {
    var isEyeOpen = false
        set(value) {
            field = value
            invalidateSelf()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val cx = w / 2f
        val cy = h / 2f

        val path = Path()
        path.moveTo(w * 0.15f, cy)
        path.quadTo(cx, h * 0.15f, w * 0.85f, cy)
        path.quadTo(cx, h * 0.85f, w * 0.15f, cy)
        canvas.drawPath(path, paint)

        canvas.drawCircle(cx, cy, w * 0.15f, paint)

        if (!isEyeOpen) {
            canvas.drawLine(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.85f, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}