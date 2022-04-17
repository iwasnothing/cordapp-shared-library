package net.corda.shared_library.example.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.shared_library.example.flows.AddRequestFlow.Acceptor
import net.corda.shared_library.example.flows.AddRequestFlow.Initiator
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.shared_library.example.contracts.BookContract
import net.corda.shared_library.example.states.BookState
import net.corda.shared_library.example.states.BookRequest
import net.corda.shared_library.example.states.StudentState
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import java.time.format.DateTimeFormatter
import java.time.Instant


/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the Book encapsulated
 * within an [BookState].
 *
 * In our simple example, the [Acceptor] always accepts a valid Book.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object AddRequestFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val bookUUID: UniqueIdentifier, val studentUUID: UniqueIdentifier ) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new Book.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {

            // Obtain a reference from a notary we wish to use.
            /**
             *  METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            val notary = serviceHub.networkMapCache.notaryIdentities.single() // METHOD 1
            // val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            // search the input.
            val studentCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(studentUUID))
            val studentStateAndRef = serviceHub.vaultService.queryBy<StudentState>(studentCriteria).states.single()
            val student = studentStateAndRef.state.data
            requireThat { "Student not found" using (student.linearId == studentUUID)}
            val inputCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bookUUID))
            val inputStateAndRef = serviceHub.vaultService.queryBy<BookState>(inputCriteria).states.single()
            val input = inputStateAndRef.state.data
            requireThat { "requester not entitled" using (ourIdentity in input.entitleParties)}
            //val BooksStateAndRefs = serviceHub.vaultService.queryBy(BookState::class.java).states
            //val inputStateAndRef = landTitleStateAndRefs.stream().filter{ it.state.data.linearId == bookUUID }
                //.findAny().orElseThrow{IllegalArgumentException("Book Not Found")}
            //val inputState = inputStateAndRef.state.data
   
            // Creating the output.
            val myschool = serviceHub.myInfo.legalIdentities.first()
            val ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()
            val req = BookRequest(bookUUID , myschool, studentUUID ,  ts)
            val newList = input.requestQueue + listOf(req)
            val output = input.copy(requestQueue = newList)

            // Creating the command.
            val txCommand = Command(BookContract.Commands.AddRequest(), output.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(output)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            //val otherPartySession = initiateFlow(otherParty)
            val otherPartySessionList = output.participants.filter{it.owningKey != myschool.owningKey}.map { initiateFlow(it) }
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, otherPartySessionList, GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, otherPartySessionList, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Book transaction." using (output is BookState)
                    //val book = output as BookState
                }
            }
            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}
