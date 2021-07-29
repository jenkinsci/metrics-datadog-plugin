package jenkins.metrics.impl.datadog;

import hudson.util.FormValidation;
import jenkins.metrics.impl.datadog.MetricsDatadogConfig.DatadogUdpEndpoint;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsDatadogConfigTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testDoTestUdpEndpoint() throws Exception {
        DatadogUdpEndpoint.DescriptorImpl udpDesc = j.getInstance().getDescriptorByType(DatadogUdpEndpoint.DescriptorImpl.class);
        assertThat(udpDesc.doTestUdpEndpoint("localhost", 8125).kind).isEqualTo(FormValidation.Kind.OK);
        assertThat(udpDesc.doTestUdpEndpoint("localhost", 10280).kind).isEqualTo(FormValidation.Kind.OK);
        assertThat(udpDesc.doTestUdpEndpoint("UNRESOLVABLE", 8125).kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(udpDesc.doTestUdpEndpoint("localhost", -1).kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(udpDesc.doTestUdpEndpoint("localhost", 65536).kind).isEqualTo(FormValidation.Kind.ERROR);
    }

    @Test
    public void testConfigRoundTrip() throws Exception {
        MetricsDatadogConfig config = MetricsDatadogConfig.instanceOrDie();
        List<MetricsDatadogConfig.DataDogEndpoint> list = Arrays.asList(
            new DatadogUdpEndpoint(null, null, "localhost", 8125),
            new DatadogUdpEndpoint(null, null, "invalid", 999999)
        );
        config.setEndpointsList(list);
        j.configRoundtrip();
        // reload list from disk:
        List<MetricsDatadogConfig.DataDogEndpoint> reloadedList = new MetricsDatadogConfig().getEndpointsList().toList();
        assertThat(reloadedList).isEqualTo(list);
    }

}
