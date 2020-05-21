package com.github.fmjsjx.libnetty.fastcgi;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * A {@code FCGI_GET_VALUES} record.
 * 
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiGetValues extends AbstractFcgiRecord {

    /**
     * {@code FCGI_MAX_CONNS}
     */
    public static final String FCGI_MAX_CONNS = "FCGI_MAX_CONNS";
    /**
     * {@code FCGI_MAX_REQS}
     */
    public static final String FCGI_MAX_REQS = "FCGI_MAX_REQS";
    /**
     * {@code FCGI_MPXS_CONNS}
     */
    public static final String FCGI_MPXS_CONNS = "FCGI_MPXS_CONNS";

    private final LinkedHashSet<String> names = new LinkedHashSet<>();

    /**
     * Constructs a new {@link FcgiGetValues} instance.
     * 
     * @param protocolVersion the {@code FcgiVersion}
     */
    public FcgiGetValues(FcgiVersion protocolVersion) {
        super(protocolVersion, 0);
    }

    @Override
    public FcgiRecordType type() {
        return FcgiRecordType.GET_VALUES;
    }

    /**
     * Put a name of the variable to be queried.
     * 
     * @param name the name of the variable
     * @return this instance
     */
    public FcgiGetValues put(String name) {
        names.add(name);
        return this;
    }

    /**
     * Remove the specified name in this query.
     * 
     * @param name the name of the variable
     */
    public void remove(String name) {
        names.remove(name);
    }

    /**
     * Returns an unmodifiable collection of the names of the variables in this
     * query.
     * 
     * @return an {@code unmodifiable} collection
     */
    public Collection<String> names() {
        return Collections.unmodifiableCollection(names);
    }

    /**
     * Returns the number of the names.
     * 
     * @return the number of the names
     */
    public int size() {
        return names.size();
    }

    @Override
    protected void bodyToString(StringBuilder builder) {
        builder.append('{');
        boolean hasValue = false;
        for (String name : names) {
            if (hasValue) {
                builder.append(", ");
            } else {
                hasValue = true;
            }
            builder.append("\"").append(name.replace("\"", "\\\"")).append("\"");
        }
        builder.append('}');
    }

}
