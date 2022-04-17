package net.corda.shared_library.example.contracts

import net.corda.shared_library.example.states.BookState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import org.slf4j.LoggerFactory

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
class BookContract : Contract {
    companion object {
        @JvmStatic
        val ID = "net.corda.shared_library.example.contracts.BookContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val logger = LoggerFactory.getLogger("net.corda")
        val command = tx.commands.requireSingleCommand<Commands>()
	logger.info("contract verifying the transaction ${command.value}")
        when (command.value) {
            is Commands.Create -> requireThat {
                logger.info("contract verifying the transaction create commmand")
                // Generic constraints around the Book transaction.
                "No inputs should be consumed when issuing an Book." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                val out = tx.outputsOfType<BookState>().single()
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                // Book-specific constraints.
                "The Book's ISBN must be non-empty." using (out.ISBN.length > 0)
                "The Book's title must be non-empty." using (out.title.length > 0)
            }
            is Commands.Borrow -> { 
                logger.info("contract verifying the transaction borrow commmand")
                requireThat {
                // Generic constraints around the Book transaction.
                "No inputs should be consumed when issuing an Book." using (tx.inputs.size == 1)
                "Only one output state should be created." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<BookState>().single()
                val out = tx.outputsOfType<BookState>().single()
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
                "Book must not be borrowed yet." using (input.isBorrowed == false)
                "holder must be entitled." using (out.holder in out.entitleParties)
                "somebody requested the book." using (out.requestQueue.size == 0)
                }
            }
            is Commands.Return -> {
                logger.info("contract verifying the transaction return commmand")
                requireThat {
                // Generic constraints around the Book transaction.
                "No inputs should be consumed when issuing an Book." using (tx.inputs.size == 1)
                "Only one output state should be created." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<BookState>().single()
                val out = tx.outputsOfType<BookState>().single()
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
                "request queue must be matched to the borrow status." using ( out.isBorrowed == (input.requestQueue.size > 0) )
                }
            }
            is Commands.AddRequest -> {
                logger.info("contract verifying the transaction AddRequest commmand")
                requireThat {
                // Generic constraints around the Book transaction.
                "No inputs should be consumed when issuing an Book." using (tx.inputs.size == 1)
                "Only one output state should be created." using (tx.outputs.size == 1)
                val input = tx.inputsOfType<BookState>().single()
                val out = tx.outputsOfType<BookState>().single()
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
                "requested book must be borrowed already." using (input.isBorrowed == true)
                "requester must be added to the queue." using (out.requestQueue.size > 0)
                val the_requester:Party = out.requestQueue.last().requester
                "requester must be entitled." using (the_requester in out.entitleParties)
                }
            }
            else -> throw IllegalArgumentException("Unsupported command ${command.value}")
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
        class Borrow : Commands
        class Return : Commands
        class AddRequest : Commands
    }
}
