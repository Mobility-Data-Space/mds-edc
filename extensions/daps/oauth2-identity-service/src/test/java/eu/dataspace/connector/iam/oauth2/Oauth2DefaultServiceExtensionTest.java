package eu.dataspace.connector.iam.oauth2;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class Oauth2DefaultServiceExtensionTest {


    @Test
    void defaultAudienceResolver(Oauth2ServiceDefaultServicesExtension extension) {
        var address = "http://address";
        var remoteMessage = mock(RemoteMessage.class);
        when(remoteMessage.getCounterPartyAddress()).thenReturn(address);
        assertThat(extension.defaultAudienceResolver().resolve(remoteMessage))
                .extracting(Result::getContent)
                .isEqualTo(address);
    }

}
