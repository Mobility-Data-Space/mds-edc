package eu.dataspace.connector.patch;

import jakarta.annotation.Priority;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import org.eclipse.edc.connector.controlplane.api.management.asset.v4.AssetApiV4Controller;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.io.IOException;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

/**
 * Patch that automatically maps incoming Asset dataAddress into dataplaneMetadata.properties, to fully support DPS capabilities.
 */
public class AssetDataAddressPatch implements ServiceExtension {
    @Inject
    private WebService webService;
    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerDynamicResource(ApiContext.MANAGEMENT, AssetApiV4Controller.class, new DataAddressToDataplaneMetadata(monitor));
    }

    @Provider
    @Priority(10_000)
    public static class DataAddressToDataplaneMetadata implements ReaderInterceptor {

        private final Monitor monitor;

        public DataAddressToDataplaneMetadata(Monitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            var result = context.proceed();
            if (!(result instanceof JsonObject expanded)) {
                return result;
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
