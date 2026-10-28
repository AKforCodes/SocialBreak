package com.socialbreak.android.data

object DomainFilterRepository {

    val CASUAL_GAMING_PORTALS = listOf(
        "1001games.com",
        "addictinggames.com",
        "agame.com",
        "armorgames.com",
        "chess.com",
        "coolmathgames.com",
        "crazygames.com",
        "friv.com",
        "gamefaqs.com",
        "gamehouse.com",
        "kick.com",
        "kizi.com",
        "kongregate.com",
        "lichess.org",
        "miniclip.com",
        "newgrounds.com",
        "poki.com",
        "roblox.com",
        "silvergames.com",
        "twitch.tv",
        "y8.com"
    )

    val ONLINE_CASINOS = listOf(
        "888casino.com",
        "888poker.com",
        "casino.com",
        "casumo.com",
        "goldenlionwins.com",
        "jackpotcity.com",
        "leovegas.com",
        "megaways.casino",
        "mrgreen.com",
        "partycasino.com",
        "planet7casino.com",
        "pokerstars.com",
        "roobet.com",
        "royalpanda.com",
        "spincasino.com",
        "stake.com"
    )

    val SPORTS_BETTING = listOf(
        "bet365.com",
        "betfred.com",
        "betmgm.com",
        "betway.com",
        "bwin.com",
        "caesars.com",
        "draftkings.com",
        "fanduel.com",
        "ladbrokes.com",
        "paddypower.com",
        "pinnacle.com",
        "prizepicks.com",
        "skybet.com",
        "sportingbet.com",
        "underdogfantasy.com",
        "unibet.com",
        "williamhill.com"
    )

    val ADULT_CONTENT = listOf(
        "adulttime.com",
        "bangbros.com",
        "bongacams.com",
        "brazzers.com",
        "cam4.com",
        "chaturbate.com",
        "clips4sale.com",
        "eporner.com",
        "erome.com",
        "fansly.com",
        "livejasmin.com",
        "manyvids.com",
        "motherless.com",
        "naughtyamerica.com",
        "onlyfans.com",
        "porn.com",
        "pornhub.com",
        "realitykings.com",
        "redtube.com",
        "spankbang.com",
        "stripchat.com",
        "xhamster.com",
        "xnxx.com",
        "xvideos.com",
        "youporn.com"
    )

    data class SocialAppMeta(
        val id: String,
        val displayName: String,
        val domains: List<String>,
        val nativePackage: String,
        val cleanUrl: String
    )

    val SOCIAL_APPS = listOf(
        SocialAppMeta("instagram", "Instagram", listOf("instagram.com"), "com.instagram.android", "https://www.instagram.com/direct/inbox/"),
        SocialAppMeta("tiktok", "TikTok", listOf("tiktok.com", "tiktokcdn.com", "tiktokcdn-us.com", "tiktokstaticb.com"), "com.zhiliaoapp.musically", "https://www.tiktok.com/messages"),
        SocialAppMeta("facebook", "Facebook", listOf("facebook.com", "fb.me", "fb.gg"), "com.facebook.katana", "https://m.facebook.com/messages/"),
        SocialAppMeta("youtube", "YouTube", listOf("youtube.com"), "com.google.android.youtube", "https://m.youtube.com"),
        SocialAppMeta("snapchat", "Snapchat", listOf("snapchat.com", "sc-cdn.net", "sc-static.net", "sc-janus.com"), "com.snapchat.android", "https://web.snapchat.com"),
        SocialAppMeta("twitter", "Twitter", listOf("twitter.com", "t.co", "twimg.com"), "com.twitter.android", "https://x.com/messages/"),
        SocialAppMeta("x", "X", listOf("x.com"), "com.twitter.android", "https://x.com/messages/"),
        SocialAppMeta("reddit", "Reddit", listOf("reddit.com", "redditmedia.com", "redditstatic.com"), "com.reddit.frontpage", "https://www.reddit.com/message/inbox"),
        SocialAppMeta("threads", "Threads", listOf("threads.net"), "com.instagram.barcelona", "https://www.threads.net"),
        SocialAppMeta("pinterest", "Pinterest", listOf("pinterest.com"), "com.pinterest", "https://www.pinterest.com"),
        SocialAppMeta("linkedin", "LinkedIn", listOf("linkedin.com"), "com.linkedin.android", "https://www.linkedin.com/messaging/"),
        SocialAppMeta("discord", "Discord", listOf("discord.com"), "com.discord", "https://discord.com/channels/@me"),
        SocialAppMeta("messenger", "Messenger", listOf("messenger.com"), "com.facebook.orca", "https://www.messenger.com"),
        SocialAppMeta("bereal", "BeReal", listOf("bereal.com"), "com.bereal.ft", "https://bereal.com"),
        SocialAppMeta("vsco", "VSCO", listOf("vsco.co"), "com.vsco.cam", "https://vsco.co"),
        SocialAppMeta("tumblr", "Tumblr", listOf("tumblr.com"), "com.tumblr", "https://www.tumblr.com")
    )
}
