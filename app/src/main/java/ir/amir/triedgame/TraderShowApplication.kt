package ir.amir.triedgame

import android.app.Application
import com.adivery.sdk.Adivery

class TraderShowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Adivery.configure(this, BuildConfig.ADIVERY_APP_ID)
        Adivery.setLoggingEnabled(BuildConfig.DEBUG)
    }
}
