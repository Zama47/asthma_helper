package com.example.asthmahelper.ui.act

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.domain.clinical.actTotal
import com.example.asthmahelper.domain.model.ActResult
import com.example.asthmahelper.domain.repository.ActResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ActViewModel @Inject constructor(private val repository: ActResultRepository) : ViewModel() {

    val history: StateFlow<List<ActResult>> = repository.getAllResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Сохраняет результат опросника. [answers] — баллы за каждый вопрос (1–5). */
    fun saveResult(answers: List<Int>) {
        viewModelScope.launch {
            repository.insertResult(
                ActResult(
                    timestamp = System.currentTimeMillis(),
                    answers = answers,
                    total = actTotal(answers)
                )
            )
        }
    }

    fun deleteResult(result: ActResult) {
        viewModelScope.launch { repository.deleteResult(result) }
    }
}
