package com.example.api;

import com.example.flow.IssueFlow;
import com.example.schema.IOUSchemaV1;
import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("resources")
public class Api {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(Api.class);

    public Api(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<IOUState>> getIOUs() {
        return rpcOps.vaultQuery(IOUState.class).getStates();
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the hospital and the patient will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("create-iou")
    public Response createIOU(@QueryParam("iouName") String iouName, @QueryParam("iouAge") int iouAge,
                              @QueryParam("iouGender") String iouGender, @QueryParam("iouHeight") int iouHeight,
                              @QueryParam("iouWeight") int iouWeight, @QueryParam("iouBloodGroup") String iouBloodGroup,
                              @QueryParam("iouDiagnosis") String iouDiagnosis, @QueryParam("iouMedicine") String iouMedicine,
                              @QueryParam("partyName") CordaX500Name partyName) throws InterruptedException, ExecutionException {

        if (iouName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouName' missing or has wrong format.\n").build();
        }
        if (iouAge <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouAge' is negative.\n").build();
        }
        if (iouGender == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouGender' missing or has wrong format.\n").build();
        }
        if (iouHeight <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouHeight' is negative.\n").build();
        }
        if (iouWeight <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouWeight' is negative.\n").build();
        }
        if (iouBloodGroup == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouBloodGroup' missing or has wrong format.\n").build();
        }
        if (iouDiagnosis == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouDiagnosis' missing or has wrong format.\n").build();
        }
        if (iouMedicine == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouMedicine' missing or has wrong format.\n").build();
        }
        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(IssueFlow.Initiator.class, iouName, iouAge, iouGender, iouHeight, iouWeight, iouBloodGroup, iouDiagnosis, iouMedicine, otherParty)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }
	
	/**
     * Displays all IOU states that are created by Party.
     */
    @GET
    @Path("my-ious")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyIOUs() throws NoSuchFieldException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        Field hospital = IOUSchemaV1.PersistentIOU.class.getDeclaredField("hospital");
        CriteriaExpression hospitalIndex = Builder.equal(hospital, myLegalName.toString());
        QueryCriteria hospitalCriteria = new QueryCriteria.VaultCustomQueryCriteria(hospitalIndex);
        QueryCriteria criteria = generalCriteria.and(hospitalCriteria);
        List<StateAndRef<IOUState>> results = rpcOps.vaultQueryByCriteria(criteria,IOUState.class).getStates();
        return Response.status(OK).entity(results).build();
    }
}
