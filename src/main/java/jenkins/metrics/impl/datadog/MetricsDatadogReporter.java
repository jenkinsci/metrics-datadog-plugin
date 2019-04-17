package jenkins.metrics.impl.datadog;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.metrics.api.Metrics;
import jenkins.metrics.impl.datadog.MetricsDatadogConfig.DataDogEndpoint;
import jenkins.metrics.impl.datadog.MetricsDatadogConfig.DatadogUdpEndpoint;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import org.coursera.metrics.datadog.transport.Transport;
import org.coursera.metrics.datadog.transport.UdpTransport;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class MetricsDatadogReporter {

    private static final Logger LOGGER = Logger.getLogger(MetricsDatadogReporter.class.getName());

    @NonNull
    private transient Map<DataDogEndpoint, DatadogReporter> reporters;

    MetricsDatadogReporter() {
        this.reporters = new LinkedHashMap<>();
    }

    void stopReporters() {
        LOGGER.info("Stopping DataDog reporters.");
        reporters.values().forEach(DatadogReporter::stop);
        reporters.clear();
    }

    synchronized void updateReporters(@NonNull List<DataDogEndpoint> endpoints) {

        MetricRegistry registry = Metrics.metricRegistry();

        Set<DataDogEndpoint> toStop = new HashSet<DataDogEndpoint>(reporters.keySet());

        for (DataDogEndpoint endpoint: endpoints) {

            toStop.remove(endpoint);
            if (reporters.containsKey(endpoint)) continue;

            if (!endpoint.isValid()) {
                LOGGER.log(Level.WARNING, "Ignoring invalid DataDog endpoint {0}", new Object[]{endpoint});
                continue;
            }

            Transport transporter = createTransporter(endpoint);
            if (transporter == null) {
                LOGGER.warning("Unknown DataDog transporter. Skipping DataDog endpoint configuration.");
                continue;
            }

            // TODO - needs to be configurable through Endpoint configuration:
            EnumSet<Expansion> expansions = EnumSet.of(Expansion.COUNT);

            DatadogReporter reporter = DatadogReporter.forRegistry(registry)
                    .withTransport(transporter)
                    .withExpansions(expansions)
                    .withTags(endpoint.getMergedTags())
                    .build();

            reporters.put(endpoint, reporter);

            LOGGER.log(Level.INFO, "Starting DataDog reporter for endpoint {0}", new Object[]{endpoint});
            reporter.start(60, TimeUnit.SECONDS);
        }

        for (DataDogEndpoint endpoint: toStop) {
            DatadogReporter reporter = reporters.get(endpoint);
            reporters.remove(endpoint);
            reporter.stop();
            LOGGER.log(Level.INFO, "Stopping DataDog reporter for endpoint {0}", new Object[]{endpoint});
        }
    }

    private Transport createTransporter(DataDogEndpoint endpoint) {

        if (endpoint instanceof DatadogUdpEndpoint) {
            DatadogUdpEndpoint udpEndpoint = (DatadogUdpEndpoint) endpoint;
            return new UdpTransport.Builder()
                    .withStatsdHost(udpEndpoint.getStatsdHost())
                    .withPort(udpEndpoint.getPort())
                    .build();
        }

        return null;
    }

    @NonNull
    @VisibleForTesting
    Map<DataDogEndpoint, DatadogReporter> getReporters() {
        return reporters;
    }
}
