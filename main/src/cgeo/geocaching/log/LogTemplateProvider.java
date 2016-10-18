package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Formatter;

import org.apache.commons.lang3.StringUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides all the available templates for logging.
 *
 */
public final class LogTemplateProvider {

    private LogTemplateProvider() {
        // utility class
    }

    /**
     * Context aware data container for log templates.
     * <p>
     * Some log templates need additional information. To provide that information, it can be encapsulated in this log
     * context.
     * </p>
     *
     */
    public static class LogContext {
        private Geocache cache;
        private Trackable trackable;
        private boolean offline = false;
        private final LogEntry logEntry;

        public LogContext(final Geocache cache, final LogEntry logEntry) {
            this(cache, logEntry, false);
        }

        public LogContext(final Trackable trackable, final LogEntry logEntry) {
            this.trackable = trackable;
            this.logEntry = logEntry;
        }

        public LogContext(final Geocache cache, final LogEntry logEntry, final boolean offline) {
            this.cache = cache;
            this.offline = offline;
            this.logEntry = logEntry;
        }

        public final Geocache getCache() {
            return cache;
        }

        public final Trackable getTrackable() {
            return trackable;
        }

        public final boolean isOffline() {
            return offline;
        }

        public final LogEntry getLogEntry() {
            return logEntry;
        }
    }

    public abstract static class LogTemplate {
        private final String template;
        @StringRes
        private final int resourceId;

        protected LogTemplate(final String template, @StringRes final int resourceId) {
            this.template = template;
            this.resourceId = resourceId;
        }

        public abstract String getValue(LogContext context);

        @StringRes
        public final int getResourceId() {
            return resourceId;
        }

        public final int getItemId() {
            return template.hashCode();
        }

        public final String getTemplateString() {
            return template;
        }

        @NonNull
        private String apply(@NonNull final String input, final LogContext context) {
            final String bracketedTemplate = "[" + template + "]";

            // check containment first to not unconditionally call the getValue(...) method
            if (input.contains(bracketedTemplate)) {
                return StringUtils.replace(input, bracketedTemplate, getValue(context));
            }
            return input;
        }
    }

    /**
     * @return all user-facing templates, but not the signature template itself
     */
    @NonNull
    public static List<LogTemplate> getTemplatesWithoutSignature() {
        final List<LogTemplate> templates = new ArrayList<>();
        templates.add(new LogTemplate("DATE", R.string.init_signature_template_date) {

            @Override
            public String getValue(final LogContext context) {
                return Formatter.formatFullDate(System.currentTimeMillis());
            }
        });
        templates.add(new LogTemplate("TIME", R.string.init_signature_template_time) {

            @Override
            public String getValue(final LogContext context) {
                return Formatter.formatTime(System.currentTimeMillis());
            }
        });
        templates.add(new LogTemplate("DATETIME", R.string.init_signature_template_datetime) {

            @Override
            public String getValue(final LogContext context) {
                final long currentTime = System.currentTimeMillis();
                return Formatter.formatFullDate(currentTime) + " " + Formatter.formatTime(currentTime);
            }
        });
        templates.add(new LogTemplate("USER", R.string.init_signature_template_user) {

            @Override
            public String getValue(final LogContext context) {
                final Geocache cache = context.getCache();
                if (cache != null) {
                    final IConnector connector = ConnectorFactory.getConnector(cache);
                    if (connector instanceof ILogin) {
                        return ((ILogin) connector).getUserName();
                    }
                }
                return Settings.getUserName();
            }
        });
        templates.add(new LogTemplate("NUMBER", R.string.init_signature_template_number) {

            @Override
            public String getValue(final LogContext context) {
                return getCounter(context, true);
            }
        });
        templates.add(new LogTemplate("OWNER", R.string.init_signature_template_owner) {

            @Override
            public String getValue(final LogContext context) {
                final Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getOwner();
                }
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getOwnerDisplayName();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("NAME", R.string.init_signature_template_name) {
            @Override
            public String getValue(final LogContext context) {
                final Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getName();
                }
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getName();
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("URL", R.string.init_signature_template_url) {

            @Override
            public String getValue(final LogContext context) {
                final Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getUrl();
                }
                final Geocache cache = context.getCache();
                if (cache != null) {
                    return StringUtils.defaultString(cache.getUrl());
                }
                return StringUtils.EMPTY;
            }
        });
        templates.add(new LogTemplate("LOG", R.string.init_signature_template_log) {
            @Override
            public String getValue(final LogContext context) {
                final LogEntry logEntry = context.getLogEntry();
                if (logEntry != null) {
                    return logEntry.getDisplayText();
                }
                return StringUtils.EMPTY;
            }
        });
        return templates;
    }

    @NonNull
    private static String getCounter(final LogContext context, final boolean incrementCounter) {
        final Geocache cache = context.getCache();
        if (cache == null) {
            return StringUtils.EMPTY;
        }

        int current = 0;
        final IConnector connector = ConnectorFactory.getConnector(cache);
        if (connector instanceof ILogin) {
            current = ((ILogin) connector).getCachesFound();
        }

        // try updating the login information, if the counter is zero
        if (current == 0) {
            if (context.isOffline()) {
                return StringUtils.EMPTY;
            }
            if (connector instanceof ILogin) {
                ((ILogin) connector).login(null, null);
                current = ((ILogin) connector).getCachesFound();
            }
        }

        if (current >= 0) {
            return String.valueOf(incrementCounter ? current + 1 : current);
        }
        return StringUtils.EMPTY;
    }

    /**
     * @return all user-facing templates, including the signature template
     */
    @NonNull
    public static List<LogTemplate> getTemplatesWithSignature() {
        final List<LogTemplate> templates = getTemplatesWithoutSignature();
        templates.add(new LogTemplate("SIGNATURE", R.string.init_signature) {
            @Override
            public String getValue(final LogContext context) {
                final String nestedTemplate = Settings.getSignature();
                if (StringUtils.contains(nestedTemplate, "SIGNATURE")) {
                    return "invalid signature template";
                }
                return applyTemplates(nestedTemplate, context);
            }
        });
        return templates;
    }

    @NonNull
    private static List<LogTemplate> getAllTemplates() {
        final List<LogTemplate> templates = getTemplatesWithSignature();
        templates.add(new LogTemplate("NUMBER$NOINC", -1 /* Never user facing */) {
            @Override
            public String getValue(final LogContext context) {
                return getCounter(context, false);
            }
        });
        return templates;
    }

    @Nullable
    public static LogTemplate getTemplate(final int itemId) {
        for (final LogTemplate template : getAllTemplates()) {
            if (template.getItemId() == itemId) {
                return template;
            }
        }
        return null;
    }

    public static String applyTemplates(@NonNull final String signature, final LogContext context) {
        String result = signature;
        for (final LogTemplate template : getAllTemplates()) {
            result = template.apply(result, context);
        }
        return result;
    }

    public static String applyTemplatesNoIncrement(@NonNull final String signature, final LogContext context) {
        return applyTemplates(signature.replace("[NUMBER]", "[NUMBER$NOINC]"), context);
    }
}