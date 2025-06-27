package eu.dataspace.connector.validator.semantic;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Vocabulary(Set<Property> required, Set<Property> optional, Map<String, Set<Enum>> enums) {

    public Set<Property> allowed() {
        return Stream.concat(required.stream(), optional.stream()).collect(Collectors.toSet());
    }

    public record Property(String name, Property child) {

        static Property property(String name) {
            return new Property(name, null);
        }

        static Property property(String name, Property child) {
            return new Property(name, child);
        }

        @Override
        public @NotNull String toString() {
            var base = "[" + name + "]";
            if (child == null) {
                return base;
            }

            return base + "." + child;
        }
    }

    public record Enum(String id, Map<String, Set<Enum>> sub) {

        public static Enum enumProperty(String id) {
            return new Enum(id, Collections.emptyMap());
        }

        public static Enum enumProperty(String id, Map<String, Set<Enum>> sub) {
            return new Enum(id, sub);
        }
    }

}

