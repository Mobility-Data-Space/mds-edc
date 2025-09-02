package eu.dataspace.connector.dataplane.kafka.spi;

import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

public interface IdentityProvider {

    ServiceResult<Credentials> grantAccess(DataAddress dataAddress);

}
