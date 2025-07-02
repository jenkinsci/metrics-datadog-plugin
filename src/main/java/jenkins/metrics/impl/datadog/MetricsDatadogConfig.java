package jenkins.metrics.impl.datadog;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.init.TermMilestone;
import hudson.init.Terminator;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@Extension
public class MetricsDatadogConfig extends GlobalConfiguration {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MetricsDatadogConfig.class.getName());

    private DescribableList<DataDogEndpoint, Descriptor<DataDogEndpoint>> endpointsList;

    private transient DatadogReportersRegistry registry;

    public MetricsDatadogConfig() {
        load();
        if (endpointsList == null) {
            endpointsList = new DescribableList<DataDogEndpoint, Descriptor<DataDogEndpoint>>(this);
        }
        registry = new DatadogReportersRegistry();
        registry.updateReporters(endpointsList.toList());
    }

    @Terminator(after= TermMilestone.STARTED)
    @Restricted(NoExternalUse.class)
    public static void shutdown() {
        MetricsDatadogConfig config = instanceOrDie();
        config.getRegistry().stopReporters();
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
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

    public DatadogReportersRegistry getRegistry() {
        return registry;
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        setEndpointsList(req.bindJSONToList(DataDogEndpoint.class, json.get("endpointsList")));
        save();
        getRegistry().updateReporters(endpointsList.toList());
        return true;
    }

    public static abstract class DataDogEndpoint extends AbstractDescribableImpl<DataDogEndpoint> {

        private final List<Tag> tags;
        private final List<PrefixFilter> prefixFilters;

        public DataDogEndpoint(List<Tag> tags, List<PrefixFilter> prefixFilters) {
            this.tags = Util.fixNull(tags);
            this.prefixFilters = Util.fixNull(prefixFilters);
        }

        @NonNull
        public List<Tag> getTags() {
            return new ArrayList<Tag>(tags);
        }

        public List<String> getMergedTags() {
            List<String> mergedTags = new ArrayList<>();
            getTags().forEach(t -> mergedTags.add(t.getKey()+ ":" + t.getValue()));
            return mergedTags;
        }

        @NonNull
        public List<PrefixFilter> getPrefixFilters() {
            return new ArrayList<PrefixFilter>(prefixFilters);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataDogEndpoint that = (DataDogEndpoint) o;
            return Objects.equals(tags, that.tags) &&
                   Objects.equals(prefixFilters, that.prefixFilters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                Stream.concat(tags.stream(), prefixFilters.stream())
                      .collect(Collectors.toList())
            );
        }

        abstract boolean isValid();
    }

    public static class DatadogUdpEndpoint extends DataDogEndpoint {

        private final String statsdHost;
        private final int port;

        @DataBoundConstructor
        public DatadogUdpEndpoint(List<PrefixFilter> prefixFilters, List<Tag> tags, String statsdHost, int port) {
            super(tags, prefixFilters);
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
                return Messages.DatadogUdpEndpoint_DescriptorImpl_displayName();
            }

            public FormValidation doTestUdpEndpoint(@QueryParameter("statsdHost") final String formStatsdHost, @QueryParameter("port") final int formPort) {
                try{
                    Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                    if (formStatsdHost == null || formStatsdHost.isEmpty()) return FormValidation.error("");
                    if (formPort == 0) return FormValidation.error(Messages.DatadogUdpEndpoint_DescriptorImpl_errors_validation_invalidPort());
                    new DatadogUdpEndpoint(null, null, formStatsdHost, formPort).checkResolvable();
                } catch (UnknownHostException e) {
                    return FormValidation.error(Messages.DatadogUdpEndpoint_DescriptorImpl_errors_validation_invalidHost());
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
                throw new IllegalArgumentException(Messages.DatadogUdpEndpoint_DescriptorImpl_errors_validation_invalidPort());
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
                return Messages.Tag_DescriptorImpl_displayName();
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

    public static class PrefixFilter extends AbstractDescribableImpl<PrefixFilter> {
        private final String prefix;

        @DataBoundConstructor
        public PrefixFilter(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
        @Extension
        public static class DescriptorImpl extends Descriptor<PrefixFilter> {
            @Override
            public String getDisplayName() {
                return Messages.PrefixFilter_DescriptorImpl_displayName();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrefixFilter i = (PrefixFilter) o;
            return Objects.equals(prefix, i.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix);
        }
    }


    @NonNull
    public static MetricsDatadogConfig instanceOrDie() {
        MetricsDatadogConfig config = ExtensionList.lookup(GlobalConfiguration.class).get(MetricsDatadogConfig.class);
        if (config == null) {
            throw new IllegalStateException("MetricsDatadogConfig is not in the list of extensions");
        }
        return config;
    }

}
