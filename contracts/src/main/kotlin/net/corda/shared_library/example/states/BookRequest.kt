package net.corda.shared_library.example.states

import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.contracts.UniqueIdentifier

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class BookRequest(val bookUUID: UniqueIdentifier, val requester: Party, val studentUUID: UniqueIdentifier, val timestamp: String)
