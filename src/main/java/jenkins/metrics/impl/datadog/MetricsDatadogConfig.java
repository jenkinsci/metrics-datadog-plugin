package jenkins.metrics.impl.datadog;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.DescribableList;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class MetricsDatadogConfig extends GlobalConfiguration {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MetricsDatadogConfig.class.getName());

    private DescribableList<DataDogEndpoint, Descriptor<DataDogEndpoint>> endpointsList;

    public MetricsDatadogConfig() {
        load();
        if (endpointsList == null) {
            endpointsList = new DescribableList<DataDogEndpoint, Descriptor<DataDogEndpoint>>(this);
        }
    }

    public DescribableList<DataDogEndpoint, Descriptor<DataDogEndpoint>> getEndpointsList() {
        return endpointsList;
    }

    public void setEndpointsList(List<DataDogEndpoint> endpointsList) {
        try {
            this.endpointsList.replaceBy(Util.fixNull(endpointsList));
        } catch(IOException e) {
            LOGGER.log(Level.WARNING, "Cannot save DataDog endpoints", e);
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        setEndpointsList(req.bindJSONToList(DataDogEndpoint.class, json.get("endpointsList")));
        save();
        Jenkins.getActiveInstance().getPlugin(PluginImpl.class).updateReporters();
        return true;
    }

    public static abstract class DataDogEndpoint extends AbstractDescribableImpl<DataDogEndpoint> {

        private final List<Tag> tags;

        public DataDogEndpoint(List<Tag> tags) {
            this.tags = tags;
        }

        public List<Tag> getTags() {
            return tags;
        }

        public List<String> getMergedTags() {
            List<String> mergedTags = new ArrayList<>();
            if (tags != null) {
                tags.forEach(t -> mergedTags.add(t.getKey()+ ":" + t.getValue()));
            }
            return mergedTags;
        }

    }

    public static class DatadogUdpEndpoint extends DataDogEndpoint {

        private final String statsdHost;
        private final int port;

        @DataBoundConstructor
        public DatadogUdpEndpoint(List<Tag> tags, String statsdHost, int port) {
            super(tags);
            this.statsdHost = statsdHost;
            this.port = port;
        }

        public String getStatsdHost() {
            return statsdHost;
        }

        public int getPort() {
            return port;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<DataDogEndpoint> {
            @Override
            public String getDisplayName() {
                return "Dogstatsd (UDP)";
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DatadogUdpEndpoint that = (DatadogUdpEndpoint) o;
            return port == that.port &&
                    Objects.equals(statsdHost, that.statsdHost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statsdHost, port);
        }

        @Override
        public String toString() {
            return "DatadogUdpEndpoint{" +
                    "statsdHost='" + statsdHost + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    public static class Tag extends AbstractDescribableImpl<Tag> {

        private final String key;
        private final String value;

        @DataBoundConstructor
        public Tag(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<Tag> {
            @Override
            public String getDisplayName() {
                return "Key/Value";
            }
        }
    }

    public static MetricsDatadogConfig instance() {
        return ExtensionList.lookup(GlobalConfiguration.class).get(MetricsDatadogConfig.class);
    }

}
