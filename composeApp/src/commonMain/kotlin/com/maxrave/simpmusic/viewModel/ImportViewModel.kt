package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.repository.ImportProgress
import com.maxrave.domain.repository.ImportRepository
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import com.mohamedrejeb.calf.core.PlatformContext
import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.io.readByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.import_invalid_file

/**
 * Drives an import of a file produced by the SimpMusic web converter.
 *
 * The picked file is read here rather than in the repository because only the app module knows
 * what a picked file is. [KmpFile] comes from the same Calf picker the backup/restore flow uses,
 * and its `readByteArray` is already cross-platform, so no expect/actual is needed.
 */
class ImportViewModel(
    private val importRepository: ImportRepository,
) : BaseViewModel() {
    private val _importState: MutableStateFlow<ImportProgress?> = MutableStateFlow(null)

    /** `null` while idle; otherwise the latest step of the running or finished import. */
    val importState: StateFlow<ImportProgress?> = _importState.asStateFlow()

    private var importJob: Job? = null

    fun import(
        file: KmpFile,
        context: PlatformContext,
    ) {
        importJob?.cancel()
        importJob =
            viewModelScope.launch {
                _importState.value = ImportProgress.Preparing
                val invalidFileMessage = getString(Res.string.import_invalid_file)
                val json =
                    withContext(Dispatchers.IO) {
                        runCatching { file.readByteArray(context).decodeToString() }
                    }.getOrElse { throwable ->
                        log("import: cannot read picked file - ${throwable.message}")
                        _importState.value = ImportProgress.Error(invalidFileMessage)
                        return@launch
                    }
                importRepository.import(json, invalidFileMessage).collect { progress ->
                    _importState.value = progress
                }
            }
    }

    /** Back to idle, which is what dismisses the progress/result dialog. */
    fun dismiss() {
        importJob?.cancel()
        importJob = null
        _importState.value = null
    }
}
