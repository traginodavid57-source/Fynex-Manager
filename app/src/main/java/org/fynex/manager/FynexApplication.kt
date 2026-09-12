package org.fynex.manager

import android.app.Application
import com.topjohnwu.superuser.Shell
import org.fynex.manager.core.plugin.PluginManager
import org.fynex.manager.core.settings.PreferencesManager

class FynexApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesManager = PreferencesManager(this)

        // Initialize libsu root shell config
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )

        // Initialize Plugins
        PluginManager.init(this)
    }

    companion object {
        lateinit var instance: FynexApplication
            private set
    }
}
