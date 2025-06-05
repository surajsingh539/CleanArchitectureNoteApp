package com.plcoding.cleanarchitecturenoteapp

import app.cash.turbine.test
import com.plcoding.cleanarchitecturenoteapp.feature_note.domain.model.Note
import com.plcoding.cleanarchitecturenoteapp.feature_note.domain.use_case.NoteUseCases
import com.plcoding.cleanarchitecturenoteapp.feature_note.domain.use_case.*
import com.plcoding.cleanarchitecturenoteapp.feature_note.domain.util.NoteOrder
import com.plcoding.cleanarchitecturenoteapp.feature_note.domain.util.OrderType
import com.plcoding.cleanarchitecturenoteapp.feature_note.presentation.notes.NotesEvent
import com.plcoding.cleanarchitecturenoteapp.feature_note.presentation.notes.NotesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class NotesViewModelTest {

    private lateinit var viewModel: NotesViewModel
    private lateinit var useCases: NoteUseCases

    private val testDispatcher = StandardTestDispatcher()

    private val fakeNotes = listOf(
        Note(title = "Title1", content = "Content1", timestamp = 1L, color = 0, id = 1),
        Note(title = "Title2", content = "Content2", timestamp = 2L, color = 1, id = 2)
    )

    private val getNotesUseCase = mockk<GetNotesUseCase>()
    private val deleteNoteUseCase = mockk<DeleteNoteUseCase>(relaxed = true)
    private val addNoteUseCase = mockk<AddNoteUseCase>(relaxed = true)
    private val getNoteUseCase = mockk<GetNoteUseCase>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        coEvery { getNotesUseCase(any()) } returns flowOf(fakeNotes)

        useCases = NoteUseCases(
            deleteNoteUseCase = deleteNoteUseCase,
            getNotesUseCase = getNotesUseCase,
            addNoteUseCase = addNoteUseCase,
            getNote = getNoteUseCase
        )

        viewModel = NotesViewModel(useCases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Initial state contains fetched notes`() = runTest {
        advanceUntilIdle()
        assertEquals(fakeNotes, viewModel.state.value.notes)
    }

    @Test
    fun `DeleteNote sets recentlyDeletedNote`() = runTest {
        val noteToDelete = fakeNotes.first()

        viewModel.onEvent(NotesEvent.DeleteNote(noteToDelete))
        advanceUntilIdle()

        coVerify { deleteNoteUseCase(noteToDelete) }
        assertEquals(noteToDelete, viewModel.recentlyDeletedNote)
    }

    @Test
    fun `RestoreNote calls addNote and clears recentlyDeletedNote`() = runTest {
        val noteToRestore = fakeNotes.first()

        // Simulate deletion first
        viewModel.onEvent(NotesEvent.DeleteNote(noteToRestore))
        advanceUntilIdle()

        // Restore
        viewModel.onEvent(NotesEvent.RestoreNote)
        advanceUntilIdle()

        coVerify { addNoteUseCase(noteToRestore) }
        assertNull(viewModel.recentlyDeletedNote)
    }

    @Test
    fun `ToggleOrderSection updates visibility flag`() {
        val initial = viewModel.state.value.isOrderSectionVisible
        viewModel.onEvent(NotesEvent.ToggleOrderSection)
        assertEquals(!initial, viewModel.state.value.isOrderSectionVisible)
    }

    @Test
    fun `Order event with same class and order type does not refetch notes`() {
        viewModel.onEvent(NotesEvent.Order(NoteOrder.Date(OrderType.Descending)))

        // Should not call getNotesUseCase again since it's the same order
        coVerify(exactly = 1) { getNotesUseCase(any()) } // Only the init call
    }

    @Test
    fun `Order event with different order triggers new fetch`() = runTest {
        viewModel.onEvent(NotesEvent.Order(NoteOrder.Title(OrderType.Ascending)))
        advanceUntilIdle()

        coVerify {
            getNotesUseCase(match {
                it is NoteOrder.Title && it.orderType is OrderType.Ascending
            })
        }
    }
}
