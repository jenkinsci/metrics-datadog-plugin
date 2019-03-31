package jenkins.metrics.impl.datadog;

import com.codahale.metrics.MetricRegistry;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import hudson.Plugin;
import jenkins.metrics.api.Metrics;
import org.coursera.metrics.datadog.transport.UdpTransport;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class PluginImpl extends Plugin {

    @Override
    public void start() throws Exception {
    }

    @Override
    public void postInitialize() throws Exception {
        updateReporters();
    }

    private void updateReporters() {
        MetricRegistry registry = Metrics.metricRegistry();
        EnumSet<Expansion> expansions = EnumSet.of(Expansion.COUNT);
        UdpTransport transporter = new UdpTransport.Builder().build();
        DatadogReporter reporter = DatadogReporter.forRegistry(registry)
                .withTransport(transporter)
                .withExpansions(expansions)
                .build();
        reporter.start(60, TimeUnit.SECONDS);
    }
}
