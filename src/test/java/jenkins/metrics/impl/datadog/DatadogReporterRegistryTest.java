package jenkins.metrics.impl.datadog;

import jenkins.metrics.impl.datadog.MetricsDatadogConfig.DatadogUdpEndpoint;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DatadogReporterRegistryTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test
    public void testInvalidUdpEndpoint() throws Exception {
        rr.then(r -> {
            MetricsDatadogConfig config = MetricsDatadogConfig.instanceOrDie();
            List<MetricsDatadogConfig.DataDogEndpoint> list = Arrays.asList(
                    new DatadogUdpEndpoint(null, "localhost", 8125),
                    new DatadogUdpEndpoint(null, "localhost", 18125)
            );

            config.setEndpointsList(list);
            r.configRoundtrip();
            assertThat(config.getRegistry().getReporters().size()).isEqualTo(2);
        });
        rr.then(r -> {
            MetricsDatadogConfig config = MetricsDatadogConfig.instanceOrDie();
            List<MetricsDatadogConfig.DataDogEndpoint> list = Arrays.asList(
                    new DatadogUdpEndpoint(null, "localhost", 8125),
                    new DatadogUdpEndpoint(null, "invalid", 999999)
            );
            config.setEndpointsList(list);
            r.configRoundtrip();
            // only one reporter here as one is invalid
            assertThat(config.getRegistry().getReporters().size()).isEqualTo(1);
        });
    }

}
