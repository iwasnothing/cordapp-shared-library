package net.corda.shared_library.example.states

import net.corda.shared_library.example.contracts.BookContract
import net.corda.shared_library.example.schema.BookSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording Book of a school library
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the Book.
 * @param lender the party issuing the Book.
 * @param borrower the party receiving and approving the Book.
 */
@BelongsToContract(BookContract::class)
data class BookState(val title: String,
                    val author: String,
                    val ISBN: String,
                    val isBorrowed: Boolean,
                    val owner: Party,
                    val holder: Party,
                    val notification: UniqueIdentifier?,
                    val borrowDate: String?,
                    val entitleParties: List<Party> ,
                    val requestQueue: List<BookRequest> = listOf(),
                    override val linearId: UniqueIdentifier = UniqueIdentifier(),
                    override val participants: List<AbstractParty> = listof(owner) + entitleParties ):
        LinearState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BookSchemaV1 -> BookSchemaV1.PersistentBook(
                    this.owner.name.toString(),
                    this.holder.name.toString(),
                    this.title,
                    this.author,
                    this.ISBN,
                    this.isBorrowed,
                    this.notification?.id,
                    this.borrowDate,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BookSchemaV1)
}
