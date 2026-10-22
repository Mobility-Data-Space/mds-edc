package eu.dataspace.connector.datasource.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * A named, reusable bag of properties describing one backend, used as a creation-time template for assets: its
 * {@code properties} are copied (client-side) into an asset's {@code dataplaneMetadata}. There is no persistent link to
 * assets: editing or deleting a data source never affects existing assets or in-flight transfers.
 * <p>
 * Only {@code id}, {@code name} and {@code type} are queryable; the {@code properties} are opaque and vault-stored.
 */
public class DataSource {

    public static final String DATA_SOURCE_TYPE_TERM = "DataSource";
    public static final String DATA_SOURCE_TYPE = EDC_NAMESPACE + DATA_SOURCE_TYPE_TERM;

    public static final String DATA_SOURCE_NAME = EDC_NAMESPACE + "name";
    public static final String DATA_SOURCE_TYPE_PROPERTY = EDC_NAMESPACE + "type";
    public static final String DATA_SOURCE_PROPERTIES = EDC_NAMESPACE + "properties";
    public static final String DATA_SOURCE_CREATED_AT = EDC_NAMESPACE + "createdAt";
    public static final String DATA_SOURCE_UPDATED_AT = EDC_NAMESPACE + "updatedAt";

    private String id;
    private String name;
    private String type;
    private final Map<String, Object> properties = new HashMap<>();
    private long createdAt;
    private long updatedAt;

    public DataSource() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class Builder {

        private final DataSource dataSource;

        private Builder(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new DataSource());
        }

        public Builder id(String id) {
            dataSource.id = id;
            return this;
        }

        public Builder name(String name) {
            dataSource.name = name;
            return this;
        }

        public Builder type(String type) {
            dataSource.type = type;
            return this;
        }

        public Builder property(String key, Object value) {
            dataSource.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            if (properties != null) {
                dataSource.properties.putAll(properties);
            }
            return this;
        }

        public Builder createdAt(long createdAt) {
            dataSource.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            dataSource.updatedAt = updatedAt;
            return this;
        }

        public DataSource build() {
            requireNonNull(dataSource.name, DATA_SOURCE_NAME);
            requireNonNull(dataSource.type, DATA_SOURCE_TYPE_PROPERTY);
            return dataSource;
        }
    }
}
