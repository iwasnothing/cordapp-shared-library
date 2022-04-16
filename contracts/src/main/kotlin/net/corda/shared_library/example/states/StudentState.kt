package net.corda.shared_library.example.states

import net.corda.shared_library.example.contracts.StudentContract
import net.corda.shared_library.example.schema.StudentSchemaV1
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
 * The state object recording Student of a school library
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the Student.
 * @param lender the party issuing the Student.
 * @param borrower the party receiving and approving the Student.
 */
@BelongsToContract(StudentContract::class)
data class StudentState(val fullname: String,
                    val studentId: String,
                    val school: Party,
                    val mobile: String,
                    val email: String,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(school)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is StudentSchemaV1 -> StudentSchemaV1.PersistentStudent(
                    this.school.name.toString(),
                    this.fullname,
                    this.studentId,
                    this.mobile,
                    this.email,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(StudentSchemaV1)
}
