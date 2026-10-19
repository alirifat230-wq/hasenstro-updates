package com.md.hasenstro.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    private const val API =
        "https://api.github.com/repos/alirifat230-wq/hasenstro-updates/releases/latest"

    fun check(activity: Activity) {
        Thread {
            try {
                val connection = URL(API).openConnection()
                    as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "Accept", "application/vnd.github+json"
                )

                val data = connection.inputStream.bufferedReader().use {
                    it.readText()
                }
                connection.disconnect()

                val release = JSONObject(data)
                if (release.optBoolean("draft") ||
                    release.optBoolean("prerelease")) return@Thread

                val tag = release.optString("tag_name")
                    .removePrefix("v")
                val latest = tag.split(".").map {
                    it.toIntOrNull() ?: 0
                }
                val current = BuildConfig.VERSION_NAME.split(".").map {
                    it.toIntOrNull() ?: 0
                }

                var comparison = 0
                for (i in 0 until maxOf(latest.size, current.size)) {
                    val a = latest.getOrElse(i) { 0 }
                    val b = current.getOrElse(i) { 0 }
                    if (a != b) {
                        comparison = a.compareTo(b)
                        break
                    }
                }

                if (comparison <= 0) return@Thread

                val assets = release.getJSONArray("assets")
                var apkUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name")
                            .endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isBlank() ||
                    !apkUrl.startsWith("https://github.com/")) {
                    return@Thread
                }

                activity.runOnUiThread {
                    if (activity.isFinishing) return@runOnUiThread

                    AlertDialog.Builder(activity)
                        .setTitle("নতুন আপডেট পাওয়া গেছে")
                        .setMessage(
                            "নতুন ভার্সন: $tag\n\n" +
                            release.optString("body", "Hasenstro আপডেট")
                        )
                        .setNegativeButton("পরে") { _, _ -> }
                        .setPositiveButton("আপডেট করুন") { _, _ ->
                            downloadAndInstall(activity, apkUrl)
                        }
                        .show()
                }
            } catch (_: Exception) {
                // নেটওয়ার্ক বা Release না থাকলে অ্যাপ স্বাভাবিকভাবে চলবে।
            }
        }.start()
    }

    private fun downloadAndInstall(activity: Activity, apkUrl: String) {
        if (android.os.Build.VERSION.SDK_INT >= 26 &&
            !activity.packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(activity)
                .setTitle("ইনস্টল করার অনুমতি দিন")
                .setMessage(
                    "Hasenstro আপডেট ইনস্টল করতে এই অ্যাপের " +
                    "জন্য অনুমতি চালু করুন।"
                )
                .setNegativeButton("বাতিল") { _, _ -> }
                .setPositiveButton("সেটিংস") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                }
                .show()
            return
        }

        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(apkUrl).openConnection()
                    as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true

                val directory = File(
                    activity.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS
                    ) ?: activity.cacheDir,
                    "updates"
                )
                if (!directory.exists()) directory.mkdirs()

                val apk = File(directory, "hasenstro-update.apk")
                connection.inputStream.use { input ->
                    apk.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                activity.runOnUiThread {
                    try {
                        val uri = FileProvider.getUriForFile(
                            activity,
                            "${activity.packageName}.fileprovider",
                            apk
                        )
                        val install = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                uri,
                                "application/vnd.android.package-archive"
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        activity.startActivity(install)
                    } catch (_: Exception) {
                        Toast.makeText(
                            activity,
                            "APK ইনস্টল স্ক্রিন খোলা যায়নি",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (_: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "আপডেট ডাউনলোড করা যায়নি। আবার চেষ্টা করুন।",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
