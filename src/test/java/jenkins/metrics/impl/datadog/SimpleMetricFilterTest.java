package jenkins.metrics.impl.datadog;

import jenkins.metrics.impl.datadog.MetricsDatadogConfig.DatadogUdpEndpoint;
import jenkins.metrics.impl.datadog.MetricsDatadogConfig.PrefixFilter;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleMetricFilterTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testMatches() throws Exception {
        List<PrefixFilter> prefixFilters = Arrays.asList(
            new PrefixFilter("vm"),
            new PrefixFilter("http")
        );
        SimpleMetricFilter mf = new SimpleMetricFilter(prefixFilters);
        assertThat(mf.matches("vm.memory.total", null)).isTrue();
        assertThat(mf.matches("http.requests.total", null)).isTrue();
        assertThat(mf.matches("jenkins.job.finished", null)).isFalse();
    }

    public void testConfigRoundTrip() throws Exception {
        MetricsDatadogConfig config = MetricsDatadogConfig.instanceOrDie();
        List<PrefixFilter> prefixFilters = Arrays.asList(
            new PrefixFilter("vm"),
            new PrefixFilter("http")
        );
        List<MetricsDatadogConfig.DataDogEndpoint> list = Arrays.asList(
            new DatadogUdpEndpoint(prefixFilters, null, "localhost", 8125),
            new DatadogUdpEndpoint(prefixFilters, null, "invalid", 999999)
        );


        config.setEndpointsList(list);
        j.configRoundtrip();
        // reload list from disk:
        List<MetricsDatadogConfig.DataDogEndpoint> reloadedList = new MetricsDatadogConfig().getEndpointsList().toList();
        assertThat(reloadedList).isEqualTo(list);
    }

}
