
<img src="./tootbot.png" width="100" height="100" alt="mastodon elephant toot empty">

# Tootbot

Every time I create a new post in this blog I like to toot or tweet
about it. Instead of manually doing this, I have my Mac Mini run
this shell script on a cron job, which does it automatically for
me.

The way it works is pretty simple:

* Download my latest blog feed as a json file.
* Check for any new blog posts.
* Compares with a local CSV file that records previous toots.
* Posts new toots to Mastodon.

This is used by [kau.sh](https://kau.sh) to auto post new blog posts to
his [Mastodon](https://mastodon.kau.sh).

# How to run:

This script is written in Kotlin and can be run from anyone's command line

```sh
brew install kotlin
kotlin tootbot.main.kts
```

# Script variables:

### 1. Mastodon instance name

_Should be obvious_

```kt
val mastodonInstance = "mastodon.social"
//val mastodonInstance = "hachyderm.io"
```

### 2. Mastodon Token

Head on over to your Mastodon Profile -> Developer settings page and create
an application. After creating a new application you should find "Your access token".

```kt
// Settings page link:
  // https://mastodon.social/settings/applications
  // https://hachyderm.io/settings/applications
val mastodonToken = "BUXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX_AAAAAA"
```

### 3. Json feed url

Your blog should make available a [json feed](https://www.jsonfeed.org/) which
the script will automatically download and parse for new blog posts. If you
use [Hugo](https://gohugo.io/) my blog theme [Henry](https://kau.sh/henry) has
[a template you can use](https://github.com/kaushikgopal/henry-hugo/blob/master/layouts/_default/list.json.json).

```kt
val jsonFeedUrl = "https://your-blog/feed.json"
```

### 4. Previously tooted

After posting a toot to Mastodon, this script will write the status to a CSV
file locally called "toots.csv". It simply jots a map of your blog post id and
the Mastodon status number.

It additionally also uses this file to prevent duplicate tooting of the same blog post.
So remember to _not clear_ this file, if you've started using this script.

```kt
val tootsFile = "./toots.csv".toPath()
```

### 5. Force a toot

There are times I want to forcefully toot a blog post that otherwise wouldn't
get picked up by the script (e.g. a blog post posted before today). For these
cases I like adding the blog post id in this variable, which will make sure
to toot the blog post anyway.

```kt
val forceToot = listOf(
    "blog/2023-02-02-own-your-online-presence/index.md",
)
```

# How I use it:

For now, I run this script on a cron job from command line, on a Mac mini server
that I use. I could dockerize it and make it even more easy but this works for
me and is pretty simple, so no current plans for that.

<img width="422" alt="SCR-20230203-qiz" src="https://user-images.githubusercontent.com/1042037/216743859-10d401b1-c8d5-40aa-88e1-b5e9fb9163aa.png">


