package eu.dataspace.dataplane.observer;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ObserverExtensionTest {

    private final Monitor monitor = mock();
    private final ObserverManager manager = mock();

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(Monitor.class, monitor);
        context.registerService(ObserverManager.class, manager);
    }

    @Nested
    class Initialize {
        @Test
        void shouldLogWarning_whenObserverConfigNotSet(ObjectFactory objectFactory, TestExtensionContext context) {
            context.setConfig(ConfigFactory.empty());

            var extension = objectFactory.constructInstance(ObserverExtension.class);
            extension.initialize(context);

            verify(monitor).warning(anyString());
        }

        @Test
        void shouldNotLogWarning_whenObserverConfigured(ObjectFactory objectFactory, TestExtensionContext context) {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "edc.mds.observer.id", "observer-participant-id",
                    "edc.mds.observer.url", "http://any-url",
                    "edc.mds.observer.dataset.id", "dataset-id",
                    "edc.mds.observer.profile", "dataspace-protocol-http:2025-1",
                    "edc.mds.observer.transfer.profile", "HttpData-PULL"
            )));

            var extension = objectFactory.constructInstance(ObserverExtension.class);
            extension.initialize(context);

            verifyNoInteractions(monitor);
        }
    }

    @Nested
    class Start {
        @Test
        void shouldRequestObserverNegotiation_whenNoOneAvailable(ObjectFactory objectFactory, TestExtensionContext context) {
            when(manager.activate(any())).thenReturn(Result.success());
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "edc.mds.observer.id", "observer-participant-id",
                    "edc.mds.observer.url", "http://any-url",
                    "edc.mds.observer.dataset.id", "dataset-id",
                    "edc.mds.observer.profile", "dataspace-protocol-http:2025-1",
                    "edc.mds.observer.transfer.profile", "HttpData-PULL"
            )));

            var extension = objectFactory.constructInstance(ObserverExtension.class);
            extension.initialize(context);
            extension.prepare();
            extension.start();

            verify(manager).activate(any());
        }
    }

}