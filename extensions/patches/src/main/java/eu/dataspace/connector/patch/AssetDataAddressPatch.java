package eu.dataspace.connector.patch;

import jakarta.annotation.Priority;
import jakarta.json.Json;
import jakarta.json.JsonObject;
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
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerDynamicResource(ApiContext.MANAGEMENT, AssetApiV4Controller.class, new MyClientRequestFilter(typeManager, monitor));
    }

    @Provider
    @Priority(10_000)
    public static class MyClientRequestFilter implements ReaderInterceptor {
        private final TypeManager typeManager;
        private final Monitor monitor;

        public MyClientRequestFilter(TypeManager typeManager, Monitor monitor) {
            this.typeManager = typeManager;
            this.monitor = monitor;
        }

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws WebApplicationException, IOException {
            if (!context.getType().equals(JsonObject.class)) {
                return context.proceed();
            }

            var bytes = context.getInputStream().readAllBytes();
            if (bytes.length == 0) {
                return context.proceed();
            }

            var jsonObject = typeManager.getMapper(JSON_LD).readValue(bytes, JsonObject.class);
            if (!jsonObject.getJsonArray(TYPE).contains(Json.createValue(Asset.EDC_ASSET_TYPE))) {
                return context.proceed();
            }

            if (!jsonObject.containsKey(Asset.EDC_ASSET_DATA_ADDRESS)) {
                return context.proceed();
            }

            monitor.warning("`dataAddress` attribute has been deprecated, `dataplaneMetadata` should be used instead");

            var dataAddress = jsonObject.getJsonArray(Asset.EDC_ASSET_DATA_ADDRESS);
            var modified = Json.createObjectBuilder(jsonObject)
                    .remove(Asset.EDC_ASSET_DATA_ADDRESS)
                    .add(Asset.EDC_ASSET_DATAPLANE_METADATA, Json.createArrayBuilder().add(Json.createObjectBuilder()
                            .add(DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES, dataAddress)
                    ))
                    .build();


            var modifiedBytes = typeManager.getMapper(JSON_LD).writeValueAsBytes(modified);
            context.setInputStream(new ByteArrayInputStream(modifiedBytes));

            return context.proceed();
        }
    }
}
