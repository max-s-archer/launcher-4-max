package com.maxarcher.launcher

import android.content.Intent
import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val systemHomeEvents = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparentSystemBarColor = getColor(R.color.launcher4max_transparent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparentSystemBarColor),
            navigationBarStyle = SystemBarStyle.dark(transparentSystemBarColor),
        )
        installHomeBackBehavior()
        setContent {
            Launcher4MaxTheme {
                Launcher4MaxApp(systemHomeEvents = systemHomeEvents)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            systemHomeEvents.tryEmit(Unit)
        }
    }

    private fun installHomeBackBehavior() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val roleManager = getSystemService(RoleManager::class.java)
                    if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true) return

                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            },
        )
    }
}
