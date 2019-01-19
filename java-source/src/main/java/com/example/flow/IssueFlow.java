package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.IssueContract;
import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;

import static com.example.contract.IssueContract.IOU_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class IssueFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String iouName;
        private final int iouAge;
        private final String iouGender;
        private final int iouHeight;
        private final int iouWeight;
        private final String iouBloodGroup;
        private final String iouDiagnosis;
        private final String iouMedicine;
        private final Party otherParty;

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new IOU.");
        private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
        private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
        private final Step GATHERING_SIGS = new Step("Gathering the counterparty's signature.") {

            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public Initiator(String iouName, int iouAge, String iouGender, int iouHeight, int iouWeight, String iouBloodGroup, String iouDiagnosis, String iouMedicine, Party otherParty) {
            this.iouName = iouName;
            this.iouAge = iouAge;
            this.iouGender = iouGender;
            this.iouHeight = iouHeight;
            this.iouWeight = iouWeight;
            this.iouBloodGroup = iouBloodGroup;
            this.iouDiagnosis = iouDiagnosis;
            this.iouMedicine = iouMedicine;
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party me = getOurIdentity();
            IOUState iouState = new IOUState(me, otherParty, iouName, iouAge, iouGender, iouHeight, iouWeight, iouBloodGroup, iouDiagnosis, iouMedicine, new UniqueIdentifier());
            final Command<IssueContract.Commands.Create> txCommand = new Command<>(
                    new IssueContract.Commands.Create(),
                    ImmutableList.of(iouState.getHospital().getOwningKey(), iouState.getPatient().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(iouState, IOU_CONTRACT_ID)
                    .addCommand(txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(otherParty);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an IOU transaction.", output instanceof IOUState);
                        IOUState iou = (IOUState) output;
                            require.using("I won't accept patients with a name that is missing or has wrong format.", iou.getName() != null);
                            require.using("I won't accept patients with age that is negtive.", iou.getAge() <= 150);
                            require.using("I won't accept patients with a wrong gender information.", iou.getGender().equals("Male") || iou.getGender().equals("Female") || iou.getGender().equals("Other"));
                            require.using("I won't accept patients with a wrong blood group information.", iou.getBloodGroup().equals("A(positive)") || iou.getBloodGroup().equals("A(negative)")
                                || iou.getBloodGroup().equals("B(positive)") || iou.getBloodGroup().equals("B(negative)")
                                || iou.getBloodGroup().equals("AB(positive)") || iou.getBloodGroup().equals("AB(negative)")
                                || iou.getBloodGroup().equals("O(positive)") || iou.getBloodGroup().equals("O(negative)"));
                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
