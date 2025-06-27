package eu.dataspace.connector.validator.semantic;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static eu.dataspace.connector.validator.semantic.Vocabulary.Enum.enumProperty;
import static eu.dataspace.connector.validator.semantic.Vocabulary.Property.property;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class VocabularyProviderTest {

    @Test
    void shouldProvideVocabulary() {
        var provider = new VocabularyProvider();

        var vocabulary = provider.provide();

        assertThat(vocabulary).isNotNull();
        assertThat(vocabulary.required()).contains(property("http://purl.org/dc/terms/title", null));
        assertThat(vocabulary.required()).contains(property("https://w3id.org/mobilitydcat-ap/mobilityTheme", property("https://w3id.org/mobilitydcat-ap/mobility-theme/data-content-category", null)));
        assertThat(vocabulary.allowed()).contains(property("http://purl.org/dc/terms/accrualPeriodicity", null));
        assertThat(vocabulary.enums()).contains(entry("https://w3id.org/mobilitydcat-ap/transportMode",
                Set.of(enumProperty("ROAD"), enumProperty("RAIL"), enumProperty("WATER"), enumProperty("AIR"))));
        assertThat(vocabulary.enums()).hasEntrySatisfying("https://w3id.org/mobilitydcat-ap/mobility-theme/data-content-category", enums -> {
                assertThat(enums).hasSizeGreaterThan(0).anySatisfy(enumItem -> {
                    assertThat(enumItem.id()).isEqualTo("TRAFFIC_FLOW_INFORMATION");
                    assertThat(enumItem.sub()).hasEntrySatisfying("https://w3id.org/mobilitydcat-ap/mobility-theme/data-content-sub-category", subs -> {
                        assertThat(subs).hasSize(2).extracting(Vocabulary.Enum::id).containsExactly("FORECAST_TRAFFIC_FLOW_DATA", "REALTIME_TRAFFIC_FLOW_DATA");
                    });
                });
            });
    }

}
