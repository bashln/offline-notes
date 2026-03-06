package com.offlinenotes.domain

enum class GroupingMode(
    val storageValue: String,
    val displayName: String
) {
    BY_TAG("by_tag", "Tag"),
    BY_FOLDER("by_folder", "Pasta"),
    BY_TYPE("by_type", "Tipo");

    companion object {
        fun fromStorageValue(value: String?): GroupingMode {
            return entries.firstOrNull { it.storageValue == value } ?: BY_TAG
        }
    }
}
