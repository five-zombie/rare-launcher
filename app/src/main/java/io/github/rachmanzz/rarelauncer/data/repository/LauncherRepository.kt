package io.github.rachmanzz.rarelauncer.data.repository

import android.Manifest
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import io.github.rachmanzz.rarelauncer.data.model.AppDirectory
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.data.model.AtmospherePreset
import io.github.rachmanzz.rarelauncer.data.model.ContactItem
import io.github.rachmanzz.rarelauncer.data.model.WallpaperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LauncherRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("rare_launcher_prefs", Context.MODE_PRIVATE)
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        val apps = mutableListOf<AppItem>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            val activityName = info.activityInfo.name
            // Skip launcher itself
            if (pkg == context.packageName) continue

            val label = info.loadLabel(packageManager).toString()
            val iconDrawable = try {
                info.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }
            val imageBitmap = iconDrawable?.toSafeBitmap()?.asImageBitmap()

            apps.add(
                AppItem(
                    label = label,
                    packageName = pkg,
                    activityName = activityName,
                    icon = imageBitmap
                )
            )
        }

        apps.sortedBy { it.label.lowercase() }
    }

    fun getAppDirectory(): AppDirectory {
        val jsonString = prefs.getString("single_app_directory", null)
        if (!jsonString.isNullOrEmpty()) {
            try {
                val obj = JSONObject(jsonString)
                val pkgsArray = obj.getJSONArray("packages")
                val pkgs = mutableListOf<String>()
                for (i in 0 until pkgsArray.length()) {
                    pkgs.add(pkgsArray.getString(i))
                }
                return AppDirectory(
                    id = obj.optString("id", "primary_dir"),
                    name = obj.optString("name", "Apps"),
                    packageNames = pkgs
                )
            } catch (e: Exception) {
                // fall through
            }
        }
        return AppDirectory("primary_dir", "Apps", emptyList())
    }

    fun saveAppDirectory(dir: AppDirectory) {
        val obj = JSONObject().apply {
            put("id", dir.id)
            put("name", dir.name)
            val pkgs = JSONArray()
            dir.packageNames.forEach { pkgs.put(it) }
            put("packages", pkgs)
        }
        prefs.edit().putString("single_app_directory", obj.toString()).apply()
    }

    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet("hidden_apps_set", emptySet()) ?: emptySet()
    }

    fun saveHiddenApps(hiddenApps: Set<String>) {
        prefs.edit().putStringSet("hidden_apps_set", hiddenApps).apply()
    }

    fun getLockedApps(): Set<String> {
        return prefs.getStringSet("locked_apps_set", emptySet()) ?: emptySet()
    }

    fun saveLockedApps(lockedApps: Set<String>) {
        prefs.edit().putStringSet("locked_apps_set", lockedApps).apply()
    }

    fun getBookmarkedApps(): List<String>? {
        val raw = prefs.getString("bookmarked_apps_list", null) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveBookmarkedApps(bookmarks: List<String>) {
        val array = JSONArray()
        bookmarks.forEach { array.put(it) }
        prefs.edit().putString("bookmarked_apps_list", array.toString()).apply()
    }

    fun getRecentApps(): List<String> {
        val raw = prefs.getString("recent_apps_list", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun recordRecentApp(packageName: String) {
        val current = getRecentApps().filter { it != packageName }.toMutableList()
        current.add(0, packageName)
        val limited = current.take(20)
        val array = JSONArray()
        limited.forEach { array.put(it) }
        prefs.edit().putString("recent_apps_list", array.toString()).apply()
    }

    fun getMostRecentForegroundPackage(): String? {
        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = runCatching {
            usageStatsManager.queryEvents(now - 1000L * 60 * 30, now) // check last 30 minutes
        }.getOrNull() ?: return null

        val event = UsageEvents.Event()
        var lastPkg: String? = null
        var lastTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                !event.packageName.isNullOrEmpty() &&
                event.packageName != context.packageName &&
                event.timeStamp > lastTime
            ) {
                if (packageManager.getLaunchIntentForPackage(event.packageName) != null) {
                    lastPkg = event.packageName
                    lastTime = event.timeStamp
                }
            }
        }
        return lastPkg
    }

    fun getWallpaperConfig(): WallpaperConfig {
        val presetId = prefs.getString("wallpaper_preset", AtmospherePreset.SPACE.id) ?: AtmospherePreset.SPACE.id
        val preset = AtmospherePreset.values().find { it.id == presetId } ?: AtmospherePreset.SPACE
        val customUri = prefs.getString("wallpaper_custom_uri", null)
        return WallpaperConfig(preset = preset, customImageUri = customUri)
    }

    fun saveWallpaperPreset(preset: AtmospherePreset) {
        prefs.edit()
            .putString("wallpaper_preset", preset.id)
            .remove("wallpaper_custom_uri")
            .apply()
    }

    fun saveCustomWallpaperUri(uri: String) {
        prefs.edit()
            .putString("wallpaper_custom_uri", uri)
            .apply()
    }

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun getQuickContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()

        val seenKeys = HashSet<String>()
        val contactsByNumber = HashMap<String, ContactItem>()

        // 1. Query device contacts
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.STARRED} DESC, " +
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (it.moveToNext() && contactsByNumber.size < 300) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex)?.trim().orEmpty() else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex)?.trim().orEmpty() else ""
                    if (number.isEmpty()) continue

                    val displayName = name.ifEmpty { number }
                    val normalizedNumber = number.filter { ch -> ch.isDigit() || ch == '+' }
                    val dedupKey = "${displayName.lowercase()}|$normalizedNumber"
                    if (!seenKeys.add(dedupKey)) continue

                    val id = if (idIndex >= 0) it.getString(idIndex) ?: dedupKey else dedupKey
                    val isStarred = starredIndex >= 0 && it.getInt(starredIndex) == 1
                    val initials = displayName.split(" ")
                        .mapNotNull { part -> part.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()

                    val contact = ContactItem(
                        id = id,
                        name = displayName,
                        phoneNumber = number,
                        initials = if (initials.isNotEmpty()) initials else "#",
                        subtitle = if (isStarred) "Starred" else "Contact"
                    )
                    contactsByNumber[normalizedNumber] = contact
                    contactsByNumber[number] = contact
                }
            }
        } catch (_: Exception) {
            // fall through
        }

        // 2. Query Recent Call Log (if permitted) and place real recent callers at the top
        val recentCallItems = mutableListOf<ContactItem>()
        val seenRecentNumbers = HashSet<String>()
        if (hasCallLogPermission()) {
            try {
                val callCursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.DATE,
                        CallLog.Calls.TYPE,
                    ),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC LIMIT 50"
                )

                callCursor?.use {
                    val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val cachedNameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                    while (it.moveToNext() && recentCallItems.size < 30) {
                        val number = if (numberIdx >= 0) it.getString(numberIdx)?.trim().orEmpty() else ""
                        if (number.isEmpty()) continue

                        val normalized = number.filter { ch -> ch.isDigit() || ch == '+' }
                        if (!seenRecentNumbers.add(normalized)) continue

                        val matchedContact = contactsByNumber[normalized] ?: contactsByNumber[number]
                        val name = matchedContact?.name
                            ?: if (cachedNameIdx >= 0) it.getString(cachedNameIdx)?.trim().orEmpty().ifEmpty { number } else number

                        val initials = name.split(" ")
                            .mapNotNull { part -> part.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString("")
                            .uppercase()

                        recentCallItems.add(
                            ContactItem(
                                id = "recent_$normalized",
                                name = name,
                                phoneNumber = number,
                                initials = if (initials.isNotEmpty()) initials else "#",
                                subtitle = "Recent Call"
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // fall through
            }
        }

        // Combine: Recent callers first, followed by remaining contacts
        val finalResult = mutableListOf<ContactItem>()
        val finalSeenKeys = HashSet<String>()

        for (item in recentCallItems) {
            val norm = item.phoneNumber.filter { ch -> ch.isDigit() || ch == '+' }
            if (finalSeenKeys.add(norm)) {
                finalResult.add(item)
            }
        }

        for (contact in contactsByNumber.values.distinctBy { it.id }) {
            val norm = contact.phoneNumber.filter { ch -> ch.isDigit() || ch == '+' }
            if (finalSeenKeys.add(norm)) {
                finalResult.add(contact)
            }
        }

        finalResult.take(200)
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openDialer(phoneNumber: String? = null) {
        try {
            val intent = if (!phoneNumber.isNullOrEmpty()) {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
            } else {
                Intent(Intent.ACTION_DIAL)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openMessages(phoneNumber: String? = null) {
        try {
            val intent = if (!phoneNumber.isNullOrEmpty()) {
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}"))
            } else {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openContacts() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openClock() {
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openCalendar() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }
}

private fun Drawable.toSafeBitmap(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap
    }
    val width = if (intrinsicWidth > 0) intrinsicWidth else 96
    val height = if (intrinsicHeight > 0) intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
