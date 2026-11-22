# News sources

## How SL uses news

- **What it reads:** each publisher's public RSS feed.
- **What it shows:** the headline, a **plain-text excerpt capped at 280 characters**, the image the feed itself provides, the publisher's name and the publication time.
- **What it never does:** copy full articles. "Read full story" opens the original article on the publisher's site.
- **Background refresh:** every 6 hours, only on a working connection and when the battery is not low. It starts only after you first open News, and can be turned off.
- **Storage:** headlines are cached on the phone for 7 days. Saved and read stories are kept until you remove them.

## Default feeds (`app/.../news/NewsFeeds.kt`)

| Category | Publisher | Feed |
|---|---|---|
| Nigeria | Premium Times | https://www.premiumtimesng.com/feed |
| Nigeria | Punch | https://punchng.com/feed/ |
| Nigeria | Channels TV | https://www.channelstv.com/feed/ |
| World | BBC News | https://feeds.bbci.co.uk/news/world/rss.xml |
| World | Al Jazeera | https://www.aljazeera.com/xml/rss/all.xml |
| Technology | BBC News | https://feeds.bbci.co.uk/news/technology/rss.xml |
| Technology | TechCabal | https://techcabal.com/feed/ |
| Business | BBC News | https://feeds.bbci.co.uk/news/business/rss.xml |
| Business | Nairametrics | https://nairametrics.com/feed/ |
| Sports | BBC Sport | https://feeds.bbci.co.uk/sport/rss.xml |
| Entertainment | BBC News | https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml |
| Education | BBC News | https://feeds.bbci.co.uk/news/education/rss.xml |
| Science | BBC News | https://feeds.bbci.co.uk/news/science_and_environment/rss.xml |
| Health | BBC News | https://feeds.bbci.co.uk/news/health/rss.xml |
| Lifestyle | none yet | The tab shows an empty state |

These addresses could not be checked from the build environment. A feed that fails is reported in the app ("Couldn't reach: …") and never breaks the others.

## Before commercial release: licensing

Public RSS does **not** automatically mean commercial reuse is allowed. For example, the BBC's feed terms permit personal, non-commercial use. Before SL is sold or monetised:

1. Ask each publisher for written permission to display headlines and excerpts in a commercial app, **or**
2. Switch to a licensed news API with commercial terms, by implementing the `NewsProvider` interface and changing the binding in `di/LauncherModule.kt`.

AI summaries, explanations and daily briefings also need an AI service plus rights to process article text. These are not enabled.
