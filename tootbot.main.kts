#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("joda-time:joda-time:2.12.2")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.10.0")
@file:DependsOn("com.squareup.okio:okio:3.0.0")
@file:DependsOn("com.squareup.moshi:moshi:1.13.0")
@file:DependsOn("com.squareup.moshi:moshi-adapters:1.13.0")
@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.13.0")
@file:DependsOn("org.twitter4j:twitter4j-core:4.0.7")


@file:Repository("https://s01.oss.sonatype.org/content/repositories/snapshots/")
@file:DependsOn("social.bigbone:bigbone:2.0.0-SNAPSHOT")


import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.FileSystem
import okio.Path.Companion.toPath
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import social.bigbone.MastodonClient
import social.bigbone.api.entity.Status
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

val jsonFeedUrl = "https://kau.sh/index.json"
val tootsFile = "./toots.csv".toPath()
val forceTweet = listOf(
    "",
)
val forceToot = listOf(
    "",
)

val mastodonToken = ""
val mastodonInstance = "mastodon.social"

val twitterConsumerKey = ""
val twitterConsumerSecret = ""
val twitterAccessToken = ""
val twitterAccessTokenSecret = ""


println(" *** 🤖Tootbot 🏁 *** ")

data class Page(
    val title: String,
    val url: String,
    @Json(name = "date_published")
    val publishedDate: Date,
    val id: String,
    @Json(name = "summary")
    val summary: String?,
) {
  fun statusText(): String {
    return if (summary?.isNotBlank() == true) {
      """
        $title

        $summary

        $url #blog
      """
    } else {
      """
        $title

        $url #blog
      """
    }.trimIndent()
  }
}

data class Feed(
    val title: String,
    val description: String,
    @Json(name = "items")
    val pages: List<Page>
)

data class Tooted(
    val postId: String,
    var tootId: String,
    var tweetId: Long,
)

/*
 * **********************
 * Load blog feed json
 * **********************
 */

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

var tootedList = mutableListOf<Tooted>()

FileSystem.SYSTEM.read(tootsFile) {
  while (true) {
    val line = readUtf8Line() ?: break
    val (path, tootId, tweetId) = line.split(',', ignoreCase = false, limit = 3)
    tootedList.add(Tooted(path, tootId, tweetId.toLong()))
  }
}

println("🤖 tooted ${tootedList.count()} times before")

/*
 * **********************
 * collect un-tooted
 * **********************
 */

val tootableList: List<Page> = feed.pages
    .filter { page ->
      val notTootedBefore = tootedList.find { it.postId == page.id }?.tootId?.isBlank() ?: true

      val publishedRecently =
          LocalDate(page.publishedDate).isAfter(LocalDate.now().minusDays(2))

      page.id in forceToot || (notTootedBefore && publishedRecently)
    }

val tweetableList: List<Page> = feed.pages
    .filter { page ->
      val notTweetedBefore = tootedList.find { it.postId == page.id }?.tweetId == (0 ?: true)
      val publishedToday =
          LocalDate(page.publishedDate) == LocalDateTime.now().toLocalDate()

      page.id in forceTweet || (notTweetedBefore && publishedToday)
    }


println("🤖 about to send ${tootableList.count()} toots now")

/*
 * **********************
 * toot the un-tooted
 * **********************
 */

tootableList.forEach { page ->
  val tootedStatus = Tooted(page.id, "", 0)

  try {
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

    val request = mastodonClient.statuses.postStatus(
        status = page.statusText(),
        inReplyToId = null,
        mediaIds = null,
        sensitive = false,
        spoilerText = null,
        visibility = Status.Visibility.Unlisted
    )

    val status = request.execute()
    tootedStatus.tootId = status.id
    println("🐘 posted status at ${status.id}")  // 109798419127349990

    tootedList.add(tootedStatus)
  } catch (e: Exception) {
    println("\uD83D\uDED1\uD83D\uDED1\uD83D\uDED1 error ${e.localizedMessage}")
  }
}

if (twitterConsumerKey.isNotBlank()) {
  tweetableList.forEach { page ->
    val tootedStatus = tootedList.find { page.id == it.postId } ?: Tooted(page.id, "", 0)

    try {
      val twitterClient = TwitterFactory(
          ConfigurationBuilder()
              .setOAuthConsumerKey(twitterConsumerKey)
              .setOAuthConsumerSecret(twitterConsumerSecret)
              .setOAuthAccessToken(twitterAccessToken)
              .setOAuthAccessTokenSecret(twitterAccessTokenSecret)
              .build()
      ).instance

      val status = twitterClient.updateStatus(page.statusText())
      tootedStatus.tweetId = status.id

      println("🐥 posted status at ${status.id}")

    } catch (e: Exception) {
      println("\uD83D\uDED1\uD83D\uDED1\uD83D\uDED1 error ${e.localizedMessage}")
    }

    tootedList.add(tootedStatus)
  }
}

/*
 * **********************
 * update tooted file
 * **********************
 */

FileSystem.SYSTEM.write(tootsFile) {
  tootedList.forEach { tooted ->
    writeUtf8(tooted.postId)
    writeUtf8(",")
    writeUtf8(tooted.tootId)
    writeUtf8(",")
    writeUtf8(tooted.tweetId.toString())
    writeUtf8("\n")
  }
}


println(" *** Tootbot ✅ *** ")
