package com.salesnetwork.avon.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateCheckerTest {
    private fun manifest(code: Int, minimum: Int = 1, channel: String = "beta", sha: String = "a".repeat(64)) = """
        {
          "schemaVersion": 1,
          "versionCode": $code,
          "versionName": "1.2.0-beta.1",
          "channel": "$channel",
          "apkUrl": "https://raw.githubusercontent.com/msalcedofernand-afk/sales-network-app-releases/beta/releases/sales-network-beta.apk",
          "sha256": "$sha",
          "minSupportedVersionCode": $minimum,
          "releasedAt": "2026-09-09T20:00:00Z",
          "releaseNotes": ["Cambio seguro"]
        }
    """.trimIndent()

    @Test fun `rejects downgrade and accepts highest valid version`() {
        val older = AppUpdateChecker.parseManifest(manifest(44), "beta")!!
        val newer = AppUpdateChecker.parseManifest(manifest(46), "beta")!!
        assertEquals(46, AppUpdateChecker.selectUpdate(listOf(older, newer), 45)?.versionCode)
        assertNull(AppUpdateChecker.selectUpdate(listOf(older), 45))
    }

    @Test fun `rejects minimum newer than downloadable apk`() {
        assertNull(AppUpdateChecker.parseManifest(manifest(code = 45, minimum = 46), "beta"))
    }

    @Test fun `rejects wrong channel placeholder hash and unapproved url`() {
        assertNull(AppUpdateChecker.parseManifest(manifest(46, channel = "stable"), "beta"))
        assertNull(AppUpdateChecker.parseManifest(manifest(46, sha = "0".repeat(64)), "beta"))
        assertNull(AppUpdateChecker.parseManifest(manifest(46).replace("raw.githubusercontent.com", "example.com"), "beta"))
    }
}
