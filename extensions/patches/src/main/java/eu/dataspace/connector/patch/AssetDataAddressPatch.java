package eu.dataspace.connector.patch;

import jakarta.annotation.Priority;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JsonObjectMessageBodyReader;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Patch that automatically maps incoming Asset dataAddress into dataplaneMetadata.properties, to fully support DPS capabilities.
 */
public class AssetDataAddressPatch implements ServiceExtension {
    @Inject
    private WebService webService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Monitor monitor;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.MANAGEMENT, new DataAddressToDataplaneMetadataBodyReader(jsonLd, typeManager, JSON_LD, validatorRegistry, monitor));
    }

    @Provider
    @Priority(10_000)
    public static class DataAddressToDataplaneMetadataBodyReader extends JsonObjectMessageBodyReader {

        private final Monitor monitor;

        public DataAddressToDataplaneMetadataBodyReader(JsonLd jsonLd, TypeManager typeManager, String typeContext,
                                                        JsonObjectValidatorRegistry validatorRegistry, Monitor monitor) {
            super(jsonLd, typeManager, typeContext, validatorRegistry);
            this.monitor = monitor;
        }

        @Override
        public JsonObject readFrom(Class<JsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            var expanded = super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
            if (expanded == null) {
                return null;
            }

            var jsonType = expanded.getJsonArray(TYPE);
            if (jsonType == null || !jsonType.contains(Json.createValue(Asset.EDC_ASSET_TYPE)) || !expanded.containsKey(Asset.EDC_ASSET_DATA_ADDRESS)) {
                return expanded;
            }

            monitor.warning("`dataAddress` attribute has been deprecated, `dataplaneMetadata` should be used instead");

            var dataAddress = expanded.get(Asset.EDC_ASSET_DATA_ADDRESS);
            var properties = withoutType(dataAddress);

            return Json.createObjectBuilder(expanded)
                    .remove(Asset.EDC_ASSET_DATA_ADDRESS)
                    .add(Asset.EDC_ASSET_DATAPLANE_METADATA, Json.createArrayBuilder().add(Json.createObjectBuilder()
                            .add(DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES, properties)
                    ))
                    .build();
        }

        /**
         * Removes the JSON-LD {@code @type} from a (possibly expanded) dataAddress so it is not carried
         * into dataplaneMetadata.properties. Handles both a bare node object and the expanded array of nodes.
         */
        private static JsonValue withoutType(JsonValue dataAddress) {
            if (dataAddress instanceof JsonArray array) {
                var builder = Json.createArrayBuilder();
                array.forEach(it -> builder.add(withoutType(it)));
                return builder.build();
            }
            if (dataAddress instanceof JsonObject object) {
                return Json.createObjectBuilder(object).remove(TYPE).build();
            }
            return dataAddress;
        }
    }
}
