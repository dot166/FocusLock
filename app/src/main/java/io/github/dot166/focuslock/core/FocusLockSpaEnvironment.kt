package io.github.dot166.focuslock.core

import android.content.Context
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.createSettingsPage
import io.github.dot166.focuslock.ui.activity.MainActivity
import io.github.dot166.focuslock.ui.pages.AppUsagePageProvider
import io.github.dot166.focuslock.ui.pages.BlockManagePageProvider
import io.github.dot166.focuslock.ui.pages.HomePageProvider
import io.github.dot166.focuslock.ui.pages.PermissionPageProvider
import io.github.dot166.jlib.app.JLibSpaEnvironment

class FocusLockSpaEnvironment(context: Context): JLibSpaEnvironment(context) {
    override val pageProviderRepository = lazy {
        SettingsPageProviderRepository(
            allPageProviders =
                listOf(
                    HomePageProvider,
                    AppUsagePageProvider,
                    BlockManagePageProvider,
                    PermissionPageProvider
                ),
            rootPages = listOf(HomePageProvider.createSettingsPage()),
        )
    }

    override val browseActivityClass = MainActivity::class.java
}