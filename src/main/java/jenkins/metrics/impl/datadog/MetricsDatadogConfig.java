package jenkins.metrics.impl.datadog;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
        Jenkins.get().getPlugin(PluginImpl.class).updateReporters();
        return true;
    }

    public static abstract class DataDogEndpoint extends AbstractDescribableImpl<DataDogEndpoint> {

        private final List<Tag> tags;

        public DataDogEndpoint(List<Tag> tags) {
            this.tags = tags;
        }

        @NonNull
        public List<Tag> getTags() {
            return Util.fixNull(tags);
        }

        public List<String> getMergedTags() {
            List<String> mergedTags = new ArrayList<>();
            getTags().forEach(t -> mergedTags.add(t.getKey()+ ":" + t.getValue()));
            return mergedTags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataDogEndpoint that = (DataDogEndpoint) o;
            return Objects.equals(tags, that.tags);
        }

        abstract boolean isValid();
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

            public FormValidation doTestUdpEndpoint(@QueryParameter("statsdHost") final String formStatsdHost, @QueryParameter("port") final int formPort) {
                try{
                    if (formStatsdHost == null || formStatsdHost.isEmpty()) return FormValidation.error("Invalid statsd host");
                    if (formPort == 0) return FormValidation.error("Invalid port");
                    new DatadogUdpEndpoint(null, formStatsdHost, formPort).checkResolvable();
                } catch (UnknownHostException e) {
                    return FormValidation.error("Invalid statsd host: unresolvable");
                } catch (IllegalArgumentException e) {
                    return FormValidation.error(e.getMessage());
                }
                return FormValidation.ok("OK");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DatadogUdpEndpoint)) return false;
            DatadogUdpEndpoint that = (DatadogUdpEndpoint) o;
            return port == that.port && Objects.equals(statsdHost, that.statsdHost) && super.equals(o);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statsdHost, port, getTags());
        }

        @Override
        public String toString() {
            return "DatadogUdpEndpoint{" +
                    "statsdHost='" + statsdHost + '\'' +
                    ", port=" + port +
                    '}';
        }

        @Override
        public boolean isValid() {
            try {
                if (this.statsdHost == null || this.statsdHost.isEmpty()) return false;
                checkResolvable();
            } catch (UnknownHostException|IllegalArgumentException e) {
                return false;
            }
            return true;
        }

        private void checkResolvable() throws UnknownHostException, IllegalArgumentException {
            InetAddress.getByName(this.statsdHost); //  throw UnknownHostException if host unresolvable
            if (this.port <= 0 || this.port > 65535) {
                throw new IllegalArgumentException("Invalid port");
            }
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tag tag = (Tag) o;
            return Objects.equals(key, tag.key) &&
                    Objects.equals(value, tag.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }

    public static MetricsDatadogConfig instance() {
        return ExtensionList.lookup(GlobalConfiguration.class).get(MetricsDatadogConfig.class);
    }

}
