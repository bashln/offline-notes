package com.offlinenotes.domain

enum class FileTypeFilter(
    val storageValue: String,
    val displayName: String
) {
    ALL("all", "Todos"),
    ORG("org", "Org"),
    MARKDOWN("markdown", "Markdown");

    companion object {
        fun fromStorageValue(value: String?): FileTypeFilter {
            return entries.firstOrNull { it.storageValue == value } ?: ALL
        }
    }
}
