package net.corda.shared_library.example.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.shared_library.example.flows.BorrowBookFlow.Acceptor
import net.corda.shared_library.example.flows.BorrowBookFlow.Initiator
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.shared_library.example.contracts.BookContract
import net.corda.shared_library.example.states.BookState
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria


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
object BorrowBookFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val bookId: UniqueIdentifier ) : FlowLogic<SignedTransaction>() {
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
            //val inputCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bookId))
            //val inputStateAndRef = serviceHub.vaultService.queryBy<ProposalState>(inputCriteria).states.single()
            //val input = inputStateAndRef.state.data
            val BooksStateAndRefs = serviceHub.vaultService.queryBy(BookState::class.java).states
            val inputStateAndRef = landTitleStateAndRefs.stream().filter{ it.state.data.linearId == bookId }
                .findAny().orElseThrow{IllegalArgumentException("Book Not Found")}
            val inputState = inputStateAndRef.state.data
   
            // Creating the output.
            // val myschool = serviceHub.myInfo.legalIdentities.first()
            val output = input.copy(title = input.title, author=input.title, ISBN=input.ISBN owner=outIdentity, status=1, holder = borrowParty)

            // Creating the command.
            val requiredSigners = listOf(ourIdentity.owningKey,borrowParty.owningKey)
            val txCommand = Command(BorrowBookContract.Commands.Borrow(), listOf(inputState.issuer.owningKey,inputState.owner.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(bookState, BookContract.ID)
                    .addCommand(txCommand)
        val builder = TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState)
                .addCommand(LandTitleContract.Commands.Transfer(), listOf(inputState.issuer.owningKey,inputState.owner.owningKey))

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
            val otherPartySession = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
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
                    val iou = output as BookState
                    "I won't accept Books with a value over 100." using (iou.value <= 100)
                }
            }
            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}
