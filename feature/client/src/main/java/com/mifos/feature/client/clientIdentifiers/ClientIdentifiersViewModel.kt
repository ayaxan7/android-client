/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */
package com.mifos.feature.client.clientIdentifiers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mifos.core.common.utils.Constants
import com.mifos.core.common.utils.Resource
import com.mifos.core.data.repository.ClientIdentifierDialogRepository
import com.mifos.core.domain.useCases.DeleteIdentifierUseCase
import com.mifos.core.domain.useCases.GetClientIdentifiersUseCase
import com.mifos.core.objects.noncore.Identifier
import com.mifos.core.objects.noncore.IdentifierPayload
import com.mifos.feature.client.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientIdentifiersViewModel @Inject constructor(
    private val getClientIdentifiersUseCase: GetClientIdentifiersUseCase,
    private val deleteIdentifierUseCase: DeleteIdentifierUseCase,
    private val clientIdentifierDialogRepository: ClientIdentifierDialogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val clientId = savedStateHandle.getStateFlow(key = Constants.CLIENT_ID, initialValue = 0)

    private val _clientIdentifiersUiState =
        MutableStateFlow<ClientIdentifiersUiState>(ClientIdentifiersUiState.Loading)
    val clientIdentifiersUiState = _clientIdentifiersUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible = _dialogVisible.asStateFlow()

    // Store the identifier being edited (null means we're creating a new one)
    private val _identifierToEdit = MutableStateFlow<Identifier?>(null)
    val identifierToEdit = _identifierToEdit.asStateFlow()

    fun showCreateIdentifierDialog() {
        _identifierToEdit.value = null
        _dialogVisible.value = true
    }

    fun showEditIdentifierDialog(identifier: Identifier) {
        _identifierToEdit.value = identifier
        _dialogVisible.value = true
    }

    fun hideCreateIdentifierDialog() {
        _dialogVisible.value = false
        _identifierToEdit.value = null
    }

    fun refreshIdentifiersList(clientId: Int) {
        _isRefreshing.value = true
        loadIdentifiers(clientId = clientId)
        _isRefreshing.value = false
    }

    fun loadIdentifiers(clientId: Int) = viewModelScope.launch(Dispatchers.IO) {
        getClientIdentifiersUseCase(clientId).collect { result ->
            when (result) {
                is Resource.Error ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.Error(
                            R.string.feature_client_failed_to_load_client_identifiers,
                        )

                is Resource.Loading ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.Loading

                is Resource.Success ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.ClientIdentifiers(result.data ?: emptyList())
            }
        }
    }

    fun deleteIdentifier(clientId: Int, identifierId: Int) = viewModelScope.launch(Dispatchers.IO) {
        deleteIdentifierUseCase(clientId, identifierId).collect { result ->
            when (result) {
                is Resource.Error ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.Error(R.string.feature_client_failed_to_delete_identifier)

                is Resource.Loading ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.Loading

                is Resource.Success -> {
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.IdentifierDeletedSuccessfully
                    loadIdentifiers(clientId)
                }
            }
        }
    }

    fun loadClientIdentifierTemplate(clientId: Int) = viewModelScope.launch(Dispatchers.IO) {
        clientIdentifierDialogRepository.getClientIdentifierTemplate(clientId).collect { result ->
            when (result) {
                is Resource.Error ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.Error(R.string.feature_client_failed_to_load_identifiers)

                is Resource.Loading ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.Loading

                is Resource.Success ->
                    _clientIdentifiersUiState.value =
                        ClientIdentifiersUiState.ClientIdentifierTemplate(
                            result.data ?: com.mifos.core.objects.noncore.IdentifierTemplate(),
                        )
            }
        }
    }

    fun createClientIdentifier(clientId: Int, identifierPayload: IdentifierPayload) =
        viewModelScope.launch(Dispatchers.IO) {
            clientIdentifierDialogRepository.createClientIdentifier(clientId, identifierPayload).collect { result ->
                when (result) {
                    is Resource.Error ->
                        _clientIdentifiersUiState.value =
                            ClientIdentifiersUiState.Error(R.string.feature_client_failed_to_create_identifier)

                    is Resource.Loading ->
                        _clientIdentifiersUiState.value =
                            ClientIdentifiersUiState.Loading

                    is Resource.Success -> {
                        _clientIdentifiersUiState.value =
                            ClientIdentifiersUiState.IdentifierCreatedSuccessfully
                        hideCreateIdentifierDialog()
                        loadIdentifiers(clientId)
                    }
                }
            }
        }

//    fun updateClientIdentifier(clientId: Int, identifierId: Int, identifierPayload: IdentifierPayload) =
//        viewModelScope.launch(Dispatchers.IO) {
//            clientIdentifierDialogRepository.updateClientIdentifier(clientId, identifierId, identifierPayload).collect { result ->
//                when (result) {
//                    is Resource.Error ->
//                        _clientIdentifiersUiState.value =
//                            ClientIdentifiersUiState.Error(R.string.feature_client_failed_to_update_identifier)
//
//                    is Resource.Loading ->
//                        _clientIdentifiersUiState.value =
//                            ClientIdentifiersUiState.Loading
//
//                    is Resource.Success -> {
//                        _clientIdentifiersUiState.value =
//                            ClientIdentifiersUiState.IdentifierUpdatedSuccessfully
//                        hideCreateIdentifierDialog()
//                        loadIdentifiers(clientId)
//                    }
//                }
//            }
//        }
}
