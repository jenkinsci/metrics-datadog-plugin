package jenkins.metrics.impl.datadog;

import hudson.model.Descriptor;
import hudson.util.DescribableList;
import jenkins.metrics.impl.datadog.MetricsDatadogConfig.DataDogEndpoint;
import jenkins.metrics.impl.datadog.MetricsDatadogConfig.DatadogUdpEndpoint;
import com.codahale.metrics.MetricRegistry;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import hudson.Plugin;
import jenkins.metrics.api.Metrics;
import org.coursera.metrics.datadog.transport.Transport;
import org.coursera.metrics.datadog.transport.UdpTransport;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginImpl extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    private transient Map<DataDogEndpoint, DatadogReporter> reporters;

    public PluginImpl() {
        this.reporters = new LinkedHashMap<>();
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public synchronized void stop() throws Exception {
        if (reporters != null) {
            reporters.values().forEach(r -> r.stop());
            reporters.clear();
        }
    }

    @Override
    public void postInitialize() throws Exception {
        updateReporters();
    }

    public synchronized void updateReporters() {

        if (reporters == null) {
            this.reporters = new LinkedHashMap<>();
        }

        MetricRegistry registry = Metrics.metricRegistry();

        MetricsDatadogConfig config = MetricsDatadogConfig.instance();
        if (config == null) {
            LOGGER.warning("No configuration found for MetricsDatadogConfig.");
            return;
        }

        List<DataDogEndpoint> endpoints = config.getEndpointsList().toList();

        Set<DataDogEndpoint> toStop = new HashSet<DataDogEndpoint>(reporters.keySet());

        for (DataDogEndpoint endpoint: endpoints) {

            toStop.remove(endpoint);
            if (reporters.containsKey(endpoint)) continue;

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
            EnumSet<Expansion> expansions = EnumSet.of(Expansion.COUNT);
            return new UdpTransport.Builder()
                    .withStatsdHost(udpEndpoint.getStatsdHost())
                    .withPort(udpEndpoint.getPort())
                    .build();
        }

        return null;
    }
}
