package eu.dataspace.dataplane.dfrs;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(DependencyInjectionExtension.class)
class DfrsObserverExtensionTest {

    private final Monitor monitor = mock();

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(Monitor.class, monitor);
    }

    @Test
    void shouldLogWarning_whenObserverConfigNotSet(ObjectFactory objectFactory, TestExtensionContext context) {
        context.setConfig(ConfigFactory.empty());

        var extension = objectFactory.constructInstance(DfrsObserverExtension.class);
        extension.initialize(context);

        verify(monitor).warning(anyString());
    }

    @Test
    void shouldNotLogWarning_whenObserverConfigured(ObjectFactory objectFactory, TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.dfrs.observer.url", "http://any-url",
                "edc.dfrs.observer.dataset.id", "dataset-id"
        )));

        var extension = objectFactory.constructInstance(DfrsObserverExtension.class);
        extension.initialize(context);

        verifyNoInteractions(monitor);
    }

}