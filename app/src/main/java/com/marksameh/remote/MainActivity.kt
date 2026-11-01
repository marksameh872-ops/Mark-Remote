package com.marksameh.remote

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {
    private val projectionRequest = 701
    private var socket: WebSocket? = null
    private lateinit var status: TextView
    private lateinit var codeInput: EditText
    private val relayUrl = BuildConfig.RELAY_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(245,247,251)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 42, 28, 24)
            setBackgroundColor(android.graphics.Color.rgb(245,247,251))
        }
        fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(this).apply {
                text = value; textSize = size; setTextColor(color)
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            }
        root.addView(text("MARK REMOTE", 13f, 0xff2457d6.toInt(), true))
        root.addView(text("Help someone, remotely.", 27f, 0xff18243b.toInt(), true).apply {
            setPadding(0, 14, 0, 8)
        })
        root.addView(text("Connect two Android phones with the owner's permission.", 15f, 0xff65718a.toInt()))
        status = text("Not connected", 14f, 0xff65718a.toInt()).apply { setPadding(0, 28, 0, 18) }
        root.addView(status)
        val host = Button(this).apply { text = "Share this phone"; isAllCaps = false }
        root.addView(host, match(56))
        codeInput = EditText(this).apply {
            hint = "Enter 6-digit session code"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
        }
        root.addView(codeInput, match(56))
        val join = Button(this).apply { text = "Connect to another phone"; isAllCaps = false }
        root.addView(join, match(56))
        val accessibility = Button(this).apply { text = "Enable remote-control permission"; isAllCaps = false }
        root.addView(accessibility, match(56))
        root.addView(text("The sharing phone must approve screen capture and manually enable Accessibility before gestures can be performed.", 13f, 0xff65718a.toInt()).apply {
            setPadding(0, 20, 0, 0)
        })
        host.setOnClickListener { startHosting() }
        join.setOnClickListener { joinSession(codeInput.text.toString(), "viewer") }
        accessibility.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        setContentView(root)
    }

    private fun match(height: Int) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply { topMargin = 10 }

    private fun startHosting() {
        val code = (100000 + SecureRandom().nextInt(900000)).toString()
        status.text = "Creating session $code…"
        connect(code, "host") {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            @Suppress("DEPRECATION")
            startActivityForResult(manager.createScreenCaptureIntent(), projectionRequest)
            Toast.makeText(this, "Share this code with the person helping you: $code", Toast.LENGTH_LONG).show()
        }
    }

    private fun joinSession(code: String, role: String) {
        if (!code.matches(Regex("\\d{6}"))) {
            Toast.makeText(this, "Enter a valid 6-digit code", Toast.LENGTH_SHORT).show(); return
        }
        status.text = "Connecting…"
        connect(code, role) { status.text = "Connected to session $code" }
    }

    private fun connect(code: String, role: String, onJoined: () -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(relayUrl).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("type", "join").put("code", code).put("role", role).toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    val msg = JSONObject(text)
                    when (msg.optString("type")) {
                        "joined" -> onJoined()
                        "error" -> status.text = msg.optString("message")
                        "peer-joined" -> status.text = "A helper joined session $code"
                        "peer-left" -> status.text = "The other phone disconnected"
                        "gesture" -> RemoteAccessibilityService.performRemoteGesture(
                            msg.optDouble("x"), msg.optDouble("y"),
                            msg.optDouble("endX", msg.optDouble("x")),
                            msg.optDouble("endY", msg.optDouble("y")),
                            msg.optLong("duration", 250L)
                        )
                    }
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread { status.text = "Connection failed: " + (t.message ?: "unknown error") }
            }
        })
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionRequest) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val service = Intent(this, ScreenShareService::class.java)
                    .putExtra("resultCode", resultCode).putExtra("data", data)
                androidx.core.content.ContextCompat.startForegroundService(this, service)
                status.text = "Screen sharing approved. Use the notification to stop."
            } else status.text = "Screen sharing was not approved."
        }
    }

    override fun onDestroy() {
        socket?.close(1000, "App closed")
        super.onDestroy()
    }
}
