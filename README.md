
![mastodon toot elephant](./tootbot.png "Mastodon empty toot")

# Tootbot

This is a simple kscript cli script that will parse a Json blog feed,
read from a csv file which contain status of toosts, toot things that
have not yet been tooted.

This is used by [kau.sh](https://kau.sh) to auto post blog posts to
his [Mastodon](https://mastodon.kau.sh).

## How to run:

```
brew install kotlin
kotlin tootbot.main.kts
# remember to update your mastodon client and auth token in the script
```

I'm starting to improve it more. For now it just runs on a local cron job on my
mac mini.
