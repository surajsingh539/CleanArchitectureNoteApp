package com.plcoding.cleanarchitecturenoteapp

import com.plcoding.cleanarchitecturenoteapp.feature_note.data.data_source.NoteDao
import com.plcoding.cleanarchitecturenoteapp.feature_note.data.repository.NoteRepositoryImpl
import com.plcoding.cleanarchitecturenoteapp.feature_note.domain.model.Note
import com.plcoding.cleanarchitecturenoteapp.feature_note.domain.repository.NoteRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test


class NoteRepositoryImplTest {

    private lateinit var noteDao: NoteDao
    private lateinit var repository: NoteRepository

    private val sampleNote = Note(
        id = 1,
        title = "Test Note",
        content = "This is a test note",
        timestamp = System.currentTimeMillis(),
        color =  0xFFAA00
    )

    @Before
    fun setUp() {
        noteDao = mockk(relaxed = true)
        repository = NoteRepositoryImpl(noteDao)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getNotes returns expected notes`() = runTest {
        val notes = listOf(sampleNote)
        every { noteDao.getNotes() } returns flowOf(notes)

        val result = repository.getNotes().toList()
        assertEquals(notes, result[0])
        verify { noteDao.getNotes() }
    }

    @Test
    fun `getNoteById returns expected note`() = runTest {
        // Correct way to mock suspend function
        coEvery { noteDao.getNoteById(1) } returns sampleNote

        // Call suspend function within runTest
        val result = repository.getNoteById(1)

        assertEquals(sampleNote, result)

        // Verify suspend function call
        coVerify { noteDao.getNoteById(1) }
    }

    @Test
    fun `insertNote calls dao insert`() = runTest {
        repository.insertNote(sampleNote)

        coVerify { noteDao.insertNote(sampleNote) }
    }

    @Test
    fun `deleteNote calls dao delete`() = runTest {
        repository.deleteNote(sampleNote)

        coVerify { noteDao.deleteNote(sampleNote) }
    }
}