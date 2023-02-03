#!/usr/bin/env kotlin

@file:Repository("https://jitpack.io")
@file:DependsOn("com.github.yschimke:okurl-script:2.1.0")
@file:Repository("https://s01.oss.sonatype.org/content/repositories/snapshots/")
@file:DependsOn("social.bigbone:bigbone:2.0.0-SNAPSHOT")


import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.joda.time.LocalDateTime
import org.joda.time.LocalDate
import social.bigbone.MastodonClient
import social.bigbone.api.entity.Status


val jsonFeedFile = "./../kau.sh/public/index.json".toPath()
val tootsFile = "./toots.csv".toPath()

val mastodonInstance = ""
val mastodonToken = ""

println(" *** 🤖Tootbot 🏁 *** ")

/*
 * **********************
 * Load blog feed json
 * **********************
 */

data class Page(
  val title: String,
  @Json(name = "date_published")
  val publishedDate: Date,
  @Json(name = "file_path")
  val filePath: String,
  val description: String?,
)

data class Feed(
  val title: String,
  val description: String,
  @Json(name = "items")
  val pages: List<Page>
)

// go through kau.sh RSS feed
val jsonFile: String = FileSystem.SYSTEM
  .source(jsonFeedFile)
  .buffer()
  .readUtf8()

val jsonParser: Moshi = Moshi.Builder()
  .add(KotlinJsonAdapterFactory())
  .add(Date::class.java, Rfc3339DateJsonAdapter())
  .build()

val feed: Feed = jsonParser
  .adapter(Feed::class.java)
  .fromJson(jsonFile) as Feed

println("🤖 found ${feed.pages.count()} pages")

/*
 * **********************
 * load tooted file
 * **********************
 */

data class Tooted(
  val filePath: String,
  val tootId: String?,
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
  "blog/2022-09-24-awk-1-oneliner-dollar-explanation/index.md",
)

val tootedFilePaths = tooted.map { it.filePath }
val tootable: List<Page> = feed.pages
  .filterNot { page -> page.filePath in tootedFilePaths }
  .filter { page ->
    LocalDate(page.publishedDate) == LocalDateTime.now().toLocalDate() ||
        page.filePath in forceToot
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

try {
	// using previously defined client with access token
	val request = mastodonClient.statuses.postStatus(
		status = "Hello World! #HelloWorld 🐘",
		inReplyToId = null,
		mediaIds = null,
		sensitive = false,
		spoilerText = null,
		visibility = Status.Visibility.Unlisted
	)

  val status = request.execute()
  println("🐘 posted status at ${status.id}")  // 109798419127349990

} catch (e: Exception) {
	// error handling
}

/*
 * **********************
 * update tooted file
 * **********************
 */

println(" *** Tootbot ✅ *** ")
