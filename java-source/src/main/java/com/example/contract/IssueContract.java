package com.example.contract;

import com.example.state.IOUState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * All contracts must sub-class the [Contract] interface.
 */
public class IssueContract implements Contract {
    public static final String IOU_CONTRACT_ID = "com.example.contract.IssueContract";


    /**
     * Override verify() function.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an IOU.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final IOUState out = tx.outputsOfType(IOUState.class).get(0);
            require.using("The hospital and the patient cannot be the same entity.",
                    out.getHospital() != out.getPatient());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

             //IOU-specific constraints.
            require.using("The IOU's name must not be null.",
                    out.getName() != null);
            require.using("The IOU's age must be non-negative.",
                    out.getAge() > 0);
            require.using("The IOU's gender must not null.",
                    out.getGender() != null);
            require.using("The IOU's height must be non-negative.",
                    out.getHeight() > 0);
            require.using("The IOU's weight must be non-negative.",
                    out.getWeight() > 0);
            require.using("The IOU's bloodGroup must not be null.",
                    out.getBloodGroup() != null);
            require.using("The IOU's diagnosis must not be null.",
                    out.getDiagnosis() != null);
            require.using("The IOU's medicine must not be null.",
                    out.getMedicine() != null);

            return null;
        });
    }

    /**
     * This contract only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}