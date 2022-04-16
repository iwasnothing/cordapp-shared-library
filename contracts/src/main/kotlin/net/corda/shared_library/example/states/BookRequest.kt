package net.corda.shared_library.example.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class BookRequest(val bookUUID: UniqueIdentifier, val requester: Party, val studentUUID: UniqueIdentifier, val timestamp: String)
