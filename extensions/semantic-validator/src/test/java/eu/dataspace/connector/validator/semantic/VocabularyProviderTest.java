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
        assertThat(vocabulary.required())
                .contains(property("http://purl.org/dc/terms/title", null))
                .contains(property("https://w3id.org/mobilitydcat-ap/mobilityTheme", property("https://w3id.org/mobilitydcat-ap/mobility-theme/data-content-category")));
        assertThat(vocabulary.allowed())
                .contains(property("http://purl.org/dc/terms/accrualPeriodicity", null))
                .contains(property("https://w3id.org/mobilitydcat-ap/mobilityDataStandard", property("@id", null)))
                .contains(property("https://w3id.org/mobilitydcat-ap/mobilityTheme", property("https://w3id.org/mobilitydcat-ap/mobility-theme/data-content-category")))
                .contains(property("https://w3id.org/edc/v0.0.1/ns/additionalProperties", property("https://w3id.org/edc/v0.0.1/ns/onrequest")));
        assertThat(vocabulary.enums()).hasEntrySatisfying("https://w3id.org/mobilitydcat-ap/transportMode", enums -> {
                assertThat(enums).hasSizeGreaterThan(0).containsAll(Set.of(enumProperty("BICYCLE"), enumProperty("BIKE_SHARING"), enumProperty("CAR_HIRE"), enumProperty("AIR")));
            });
        assertThat(vocabulary.enums()).hasEntrySatisfying("https://w3id.org/mobilitydcat-ap/mobility-theme/data-content-category", enums -> {
                assertThat(enums).hasSizeGreaterThan(0).anySatisfy(enumItem -> {
                    assertThat(enumItem.id()).isEqualTo("ROAD_WORK_INFORMATION");
                    assertThat(enumItem.sub()).hasEntrySatisfying("https://w3id.org/mobilitydcat-ap/mobility-theme/data-content-sub-category", subs -> {
                        assertThat(subs).hasSize(2).extracting(Vocabulary.Enum::id).containsExactlyInAnyOrder("LONG_TERM_ROAD_WORKS", "SHORT_TERM_ROAD_WORKS");
                    });
                });
            });
    }

}
