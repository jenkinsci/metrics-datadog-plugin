package jenkins.metrics.impl.datadog;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import jenkins.metrics.impl.datadog.MetricsDatadogConfig.PrefixFilter;

public class SimpleMetricFilter implements MetricFilter {
    private static final Logger LOGGER = Logger.getLogger(SimpleMetricFilter.class.getName());
    private List<PrefixFilter> prefixes;

    public SimpleMetricFilter(List<PrefixFilter> prefixes) {
        this.prefixes =
            prefixes.stream()
                    .distinct()
                    .filter(p -> p != null && !p.getPrefix().isEmpty())
                    .collect(Collectors.toList());
    }

    public boolean matches(String name, Metric metric) {
        boolean match =
        this.prefixes.stream()
                     .anyMatch(p -> name.startsWith(p.getPrefix()));
        if (match) {
            LOGGER.log(Level.FINE, "Metric {0} should be included", new Object[]{name});
        } else {
            LOGGER.log(Level.FINE, "Metric {0} does not match the filter", new Object[]{name});
        }
        return match;
    }
}