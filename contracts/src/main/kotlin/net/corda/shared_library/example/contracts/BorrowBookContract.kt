package net.corda.shared_library.example.contracts

import net.corda.shared_library.example.states.BookState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [BookState], which in turn encapsulates an [BookState].
 *
 * For a new [BookState] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [BookState].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
class BorrowBookContract : Contract {
    companion object {
        @JvmStatic
        val ID = "net.corda.shared_library.example.contracts.BorrowBookContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Borrow>()
        requireThat {
            // Generic constraints around the Book transaction.
            "Only one input state should be created." using (tx.inputs.size == 1)
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<BookState>().single()
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // Book-specific constraints.
            "The Book's borrower must be in entitle list." using (out.holder in out.entitleParties)
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Borrow : Commands
    }
}
