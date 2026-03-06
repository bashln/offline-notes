package com.offlinenotes.viewmodel

import com.offlinenotes.domain.FileTypeFilter
import com.offlinenotes.domain.GroupingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupingLogicTest {
    @Test
    fun folderGroupKey_usesRootWhenPathHasNoFolder() {
        val key = folderGroupKeyForRelativePath("note.md")

        assertEquals("folder:root", key)
    }

    @Test
    fun folderGroupKey_usesNestedFolderWhenPathHasSubdirectories() {
        val key = folderGroupKeyForRelativePath("work/project-a/notes.md")

        assertEquals("folder:work/project-a", key)
    }

    @Test
    fun typeGroupKey_detectsOrgCaseInsensitive() {
        assertEquals("type:org", typeGroupKeyForName("MEETING.ORG"))
        assertEquals("type:md", typeGroupKeyForName("todo.md"))
    }

    @Test
    fun groupingMode_fromStorageValue_fallsBackToTag() {
        assertEquals(GroupingMode.BY_TAG, GroupingMode.fromStorageValue("unknown"))
        assertEquals(GroupingMode.BY_FOLDER, GroupingMode.fromStorageValue("by_folder"))
    }

    @Test
    fun fileTypeFilter_fromStorageValue_fallsBackToAll() {
        assertEquals(FileTypeFilter.ALL, FileTypeFilter.fromStorageValue("other"))
        assertEquals(FileTypeFilter.MARKDOWN, FileTypeFilter.fromStorageValue("markdown"))
    }
}
