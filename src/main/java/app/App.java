package app;

import java.util.List;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.ConfigurationTypes.BulkAlgo;
import app.ConfigurationTypes.Extension;
import app.ConfigurationTypes.HashAlgo;
import app.ConfigurationTypes.KeyExchange;
import app.ConfigurationTypes.KeyExchangeGroup;
import app.ConfigurationTypes.ServerAuth;
import app.ConfigurationTypes.TlsVersion;
import app.HandshakeStepping.HandshakeType;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.connection.OutboundConnection;
import de.rub.nds.tlsattacker.core.exceptions.WorkflowExecutionException;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutorFactory;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;

public class App {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        // TODOs
        // with TLS1.2 ECDHE OCSP => decryption failed or bad Record MAC
        Config myConfig =
            ConfigFactory.getConfig(
                TlsVersion.TLS13,
                KeyExchange.ECDHE,
                KeyExchangeGroup.SECP256R1,
                ServerAuth.ECDSA,
                HashAlgo.SHA256,
                BulkAlgo.AES_128_GCM,
                new Vector<Extension>(){{add(Extension.SESSION_RESUMPTION);}});
                //new Vector<>());

        OutboundConnection outboundCon = new OutboundConnection();
        outboundCon.setHostname("localhost");
        outboundCon.setPort(4433);
        myConfig.setDefaultClientConnection(outboundCon);
        
        List<WorkflowTrace> segmentedHandshake = HandshakeStepping.getSegmentedHandshake(HandshakeType.TLS13_WITHOUT_CLIENTAUTH, myConfig, outboundCon);
        Long[][] resultsMeasurement = TimeMeasurement.startTimeMeasurement(2, myConfig, segmentedHandshake, false, 1);
        //System.out.println(resultsMeasurement);

        System.out.println("Reached End");
    }

    public static long startTlsClient(Config config, WorkflowTrace trace) {
        State state = new State(config, trace);
        WorkflowExecutor workflowExecutor =
                WorkflowExecutorFactory.createWorkflowExecutor(
                        config.getWorkflowExecutorType(), state);

        long timeElapsed = 0;
        try {
            long start = System.nanoTime();
            workflowExecutor.executeWorkflow();
            long finish = System.nanoTime();
            timeElapsed = finish - start;
        } catch (WorkflowExecutionException ex) {
            System.out.println(
                    "The TLS protocol flow was not executed completely, follow the debug messages for more information.");
            LOGGER.warn(
                    "The TLS protocol flow was not executed completely, follow the debug messages for more information.");
        }
        return timeElapsed;
    }
}
