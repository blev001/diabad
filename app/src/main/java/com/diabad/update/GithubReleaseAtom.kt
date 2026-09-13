package com.diabad.update

/**
 * Parses GitHub Releases Atom without the GitHub REST API (which 403s
 * unauthenticated cloud IPs). Tag `v20` is the versionCode the phone compares.
 */
data class GithubAtomRelease(
    val tag: String,
    val title: String,
    val notes: String?,
)

object GithubReleaseAtom {

    private val entryRegex = Regex(
        "<entry>(.*?)</entry>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val linkRegex = Regex(
        "<link[^>]*href=\"([^\"]+)\"[^>]*>",
        RegexOption.IGNORE_CASE,
    )
    private val titleRegex = Regex(
        "<title[^>]*>(.*?)</title>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val contentRegex = Regex(
        "<content[^>]*>(.*?)</content>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val tagFromLinkRegex = Regex("/releases/tag/([^/\"\\s]+)")
    private val tagFromIdRegex = Regex("/(v?\\d+)\\s*</id>", RegexOption.IGNORE_CASE)

    fun firstRelease(feed: String): GithubAtomRelease? {
        val entryBlock = entryRegex.find(feed)?.groupValues?.getOrNull(1) ?: return null
        val link = linkRegex.find(entryBlock)?.groupValues?.getOrNull(1).orEmpty()
        val title = unescapeXml(titleRegex.find(entryBlock)?.groupValues?.getOrNull(1).orEmpty())
            .trim()
        val notesRaw = unescapeXml(contentRegex.find(entryBlock)?.groupValues?.getOrNull(1).orEmpty())
            .trim()
        val notes = notesRaw.takeIf { it.isNotBlank() }
        val tag = tagFromLinkRegex.find(link)?.groupValues?.getOrNull(1)
            ?: tagFromIdRegex.find(entryBlock)?.groupValues?.getOrNull(1)
            ?: return null
        return GithubAtomRelease(tag = tag, title = title, notes = notes)
    }

    fun parseVersionCode(raw: String): Int? {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore(' ')
            .trim()
        return cleaned.toIntOrNull()
    }

    fun unescapeXml(value: String): String {
        val decoded = value
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        return decoded.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")
    }
}
