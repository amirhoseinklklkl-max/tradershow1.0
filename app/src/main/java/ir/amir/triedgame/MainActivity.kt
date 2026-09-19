package ir.amir.triedgame

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ir.amir.triedgame.ads.AdManager
import ir.amir.triedgame.billing.BillingManager
import ir.amir.triedgame.data.GameRepository
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.GameViewModelFactory
import ir.amir.triedgame.ui.navigation.MainNavGraph
import ir.amir.triedgame.ui.screens.RegistrationScreen
import ir.amir.triedgame.ui.theme.TraderShowTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: GameRepository
    private var adManager: AdManager? = null
    private var billingManager: BillingManager? = null

    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        repository = GameRepository(applicationContext)
        adManager = AdManager(this, repository).also { it.prepare() }
        billingManager = BillingManager(
            activity = this,
            onWalletTopUp = { usd -> viewModel.creditUsdFromPurchase(usd) }
        )
        billingManager?.start()

        viewModel.loadOrCreateProfile(repository.loadProfile())

        setContent {
            TraderShowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val profile = viewModel.profile
                    if (profile == null) {
                        RegistrationScreen(onRegister = { first, last ->
                            viewModel.registerNewUser(first, last)
                        })
                    } else {
                        MainNavGraph(viewModel, adManager, billingManager)
                    }
                }
            }
        }
    }

    /** Full immersive mode: hides the status bar and system navigation bar,
     * like most games. A swipe from the screen edge briefly reveals them
     * again (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE) without permanently
     * exiting immersive mode. */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) viewModel.onAppResumed()
    }

    override fun onDestroy() {
        billingManager?.dispose()
        super.onDestroy()
    }
}
