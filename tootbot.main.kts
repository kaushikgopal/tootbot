#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("joda-time:joda-time:2.12.2")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.10.0")
@file:DependsOn("com.squareup.okio:okio:3.0.0")
@file:DependsOn("com.squareup.moshi:moshi:1.13.0")
@file:DependsOn("com.squareup.moshi:moshi-adapters:1.13.0")
@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.13.0")

@file:Repository("https://s01.oss.sonatype.org/content/repositories/snapshots/")
@file:DependsOn("social.bigbone:bigbone:2.0.0-SNAPSHOT")


import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.FileSystem
import okio.Path.Companion.toPath
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import social.bigbone.MastodonClient
import social.bigbone.api.entity.Status
import java.util.*


val jsonFeedUrl = "https://kau.sh/index.json"
val tootsFile = "./toots.csv".toPath()

val mastodonInstance = "mastodon.social"
val mastodonToken = ""

println(" *** 🤖Tootbot 🏁 *** ")

/*
 * **********************
 * Load blog feed json
 * **********************
 */

data class Page(
    val title: String,
    val url: String,
    @Json(name = "date_published")
    val publishedDate: Date,
    @Json(name = "file_path")
    val id: String,
    @Json(name = "summary")
    val summary: String?,
) {
  fun statusText(): String {
    return """
        $title

        $url #blog
      """.trimIndent()
  }
}

data class Feed(
    val title: String,
    val description: String,
    @Json(name = "items")
    val pages: List<Page>
)

// download RSS feed file
var feed: Feed

val request = Request.Builder()
    .url(jsonFeedUrl)
    .build()

val jsonParser: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .add(Date::class.java, Rfc3339DateJsonAdapter())
    .build()

OkHttpClient().newCall(request).execute().use { response ->
  feed = jsonParser
      .adapter(Feed::class.java)
      .fromJson(response.body!!.source()) as Feed
}

println("🤖 found ${feed.pages.count()} pages in the feed")

/*
 * **********************
 * load tooted file
 * **********************
 */

data class Tooted(
    val filePathId: String,
    val tootId: String,
)

var tooted = mutableListOf<Tooted>()

FileSystem.SYSTEM.read(tootsFile) {
  while (true) {
    val line = readUtf8Line() ?: break
    val (path, tootId) = line.split(',', ignoreCase = false, limit = 2)
    tooted.add(Tooted(path, tootId))
  }
}

println("🤖 tooted ${tooted.count()} times before")

/*
 * **********************
 * collect un-tooted
 * **********************
 */

val forceToot = listOf(
    "blog/2022-08-20-mac-mini-tailscale-benefits-tips-vpn-vps/index.md",
)

val tootedFilePaths = tooted.map { it.filePathId }
val tootable: List<Page> = feed.pages
    .filterNot { page -> page.id in tootedFilePaths }
    .filter { page ->
      LocalDate(page.publishedDate) == LocalDateTime.now().toLocalDate() ||
          page.id in forceToot
    }

println("🤖 about to send ${tootable.count()} toots now")

/*
 * **********************
 * toot the un-tooted
 * **********************
 */

val mastodonClient = MastodonClient.Builder(mastodonInstance)
    .accessToken(mastodonToken)
    .build()

//mastodonClient.timelines
//  .getHomeTimeline(Range())
//  .execute()
//  .part
//  .forEach {
//    println("🐘 ${it.content}")
//  }


tootable.forEach { page ->
  try {

    val request = mastodonClient.statuses.postStatus(
        status = page.statusText(),
        inReplyToId = null,
        mediaIds = null,
        sensitive = false,
        spoilerText = null,
        visibility = Status.Visibility.Unlisted
    )

    val status = request.execute()
    println("🐘 posted status at ${status.id}")  // 109798419127349990
    tooted.add(Tooted(page.id, status.id))

  } catch (e: Exception) {
    println("\uD83D\uDED1\uD83D\uDED1\uD83D\uDED1 error ${e.localizedMessage}")
  }

}


/*
 * **********************
 * update tooted file
 * **********************
 */

FileSystem.SYSTEM.write(tootsFile) {
  tooted.forEach { tooted ->
    writeUtf8(tooted.filePathId)
    writeUtf8(",")
    writeUtf8(tooted.tootId)
    writeUtf8("\n")
  }
}


println(" *** Tootbot ✅ *** ")
