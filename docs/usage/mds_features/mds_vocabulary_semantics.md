# MDS Vocabulary and Semantic Validation

The MDS EDC Connector supports Mobility DCAT-AP vocabulary for asset metadata, enabling semantic interoperability within the Mobility Data Space. This feature provides standardized property definitions and validation for mobility-specific data assets.

The rules described here are defined in the
[MDS asset JSON Schema](../../../extensions/semantic-validator/src/main/resources/mds-asset-schema.json), which is the
single source of truth. The lists of [data categories](#data-categories-and-sub-categories) and
[transport modes](#transport-modes) in this page are generated from it.

## JSON-LD Context

Asset requests should use the MDS management context, which defines all the prefixes used in this page:

```json
{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1"
}
```

### Vocabulary Prefixes

| Prefix                 | Namespace                                          | Purpose                                                  |
|------------------------|----------------------------------------------------|----------------------------------------------------------|
| `dct`                  | `http://purl.org/dc/terms/`                        | General metadata (title, description, publisher)         |
| `dcat`                 | `http://www.w3.org/ns/dcat#`                       | Dataset properties (keywords, media type, organization)  |
| `mobilitydcatap`       | `https://w3id.org/mobilitydcat-ap/`                | Mobility-specific properties, including `transportMode`  |
| `mobilitydcatap-theme` | `https://w3id.org/mobilitydcat-ap/mobility-theme/` | Data category and sub-category inside `mobilityTheme`    |
| `adms`                 | `http://www.w3.org/ns/adms#`                       | Sample data references                                   |
| `owl`                  | `http://www.w3.org/2002/07/owl#`                   | Version information                                      |
| `skos`                 | `http://www.w3.org/2004/02/skos/core#`             | Labels                                                   |
| `rdf`                  | `http://www.w3.org/2000/01/rdf-schema#`            | Literal descriptions in `mobilityDataStandard`           |
| `edc`                  | `https://w3id.org/edc/v0.0.1/ns/`                  | EDC-specific properties (default vocabulary)             |

## Asset Properties

The vocabulary is **closed**: only the properties listed in this section are accepted in the asset `properties`, both
at the top level and inside nested objects (`dct:spatial`, `dct:temporal`, `mobilitydcatap:mobilityTheme`,
`mobilitydcatap:mobilityDataStandard`, `edc:additionalProperties`). Any other property makes the asset creation fail.
Custom metadata that is not part of the vocabulary can be stored in `privateProperties`, which is not validated.

Unless stated otherwise, a property accepts a string or an array of strings. The validator checks the structure and the
enumerated values, not the format: URLs, dates and language codes are not checked.

### General Asset Properties

| Prefix | Attribute              | Type          | Required | Description                 | Example                                  |
|--------|------------------------|---------------|----------|-----------------------------|------------------------------------------|
| `dct`  | `title`                | String        | **Yes**  | Asset title                 | "Traffic Flow Data Munich"               |
| `dct`  | `description`          | String        | No       | Detailed description        | "Real-time traffic flow data..."         |
| `dct`  | `language`             | String        | No       | Language code               | "code/EN"                                |
| `dct`  | `publisher`            | URL           | No       | Publisher URL               | `https://example.com/about`              |
| `dct`  | `license`              | URL           | No       | License URL                 | `https://example.com/license`            |
| `dct`  | `rightsHolder`         | String        | No       | Legal rights holder         | "Example Mobility GmbH"                  |
| `dct`  | `accessRights`         | String        | No       | Access rights description   | "Restricted to MDS participants"         |
| `dct`  | `spatial`              | Object        | No       | Geographic coverage         | See below                                |
| `dct`  | `isReferencedBy`       | URL           | No       | Reference URL               | `https://example.com/references`         |
| `dct`  | `temporal`             | Object        | No       | Temporal coverage           | See below                                |
| `dct`  | `accrualPeriodicity`   | String        | No       | Update frequency            | "daily", "hourly"                        |
| `dcat` | `organization`         | String        | No       | Organization name           | "Example Mobility GmbH"                  |
| `dcat` | `keywords`             | Array[String] | No       | Search keywords             | ["traffic", "real-time", "munich"]       |
| `dcat` | `mediaType`            | String        | No       | Content media type          | "application/json"                       |
| `dcat` | `landingPage`          | URL           | No       | Dataset landing page        | `https://data.example.com/docs`          |
| `owl`  | `versionInfo`          | String        | No       | Version number              | "2.1.0"                                  |
| `edc`  | `additionalProperties` | Object        | No       | MDS-specific asset settings | See below                                |

`dct:title` must be present; its value is not checked, so an empty string is accepted.

#### Spatial Property Structure

`dct:spatial` accepts only `skos:prefLabel` and `dct:identifier`:

```json
{
  "dct:spatial": {
    "skos:prefLabel": "Munich Metropolitan Area",
    "dct:identifier": ["DE", "DE212"]
  }
}
```

#### Temporal Property Structure

`dct:temporal` accepts only `dcat:startDate` and `dcat:endDate`:

```json
{
  "dct:temporal": {
    "dcat:startDate": "2024-01-01",
    "dcat:endDate": "2024-12-31"
  }
}
```

#### Additional Properties Structure

`edc:additionalProperties` is not free-form: it accepts only the following properties.

| Attribute           | Description                                                                                       |
|---------------------|---------------------------------------------------------------------------------------------------|
| `onrequest`         | Marks the asset as an [on-request data offer](on-request-data-offers.md)                          |
| `email`             | Contact email for requests about the asset                                                        |
| `preferred_subject` | Suggested subject for request emails                                                              |
| `manual_approval`   | Informational flag: it is not evaluated by the connector, which holds negotiations for [manual approval](contract_negotiation_manual_approval_rejection.md) based on the `ManualApproval` contract policy constraint |

```json
{
  "edc:additionalProperties": {
    "edc:onrequest": "true",
    "edc:email": "data@example.com",
    "edc:preferred_subject": "Access request"
  }
}
```

With the MDS management context `edc` is the default vocabulary, so `additionalProperties` and its nested properties can
also be written without prefix.

### Mobility Asset Properties

| Prefix           | Attribute              | Type        | Required | Description                   | Example                          |
|------------------|------------------------|-------------|----------|-------------------------------|----------------------------------|
| `mobilitydcatap` | `mobilityTheme`        | Object      | **Yes**  | Data category and subcategory | See below                        |
| `mobilitydcatap` | `mobilityDataStandard` | Object      | No       | Data model/schema references  | See below                        |
| `mobilitydcatap` | `transportMode`        | Enum        | No       | Transport mode (single value) | "CAR"                            |
| `mobilitydcatap` | `georeferencingMethod` | String      | No       | Geo-referencing method        | "WGS84", "ETRS89"                |
| `adms`           | `sample`               | Array[URL]  | No       | Sample data URLs              | ["https://example.com/sample"]   |

`mobilitydcatap:transportMode` must be one of the [transport modes](#transport-modes).

#### Mobility Theme Structure

`mobilitydcatap:mobilityTheme` is always required, and it must contain a `mobilitydcatap-theme:data-content-category`.

- **Required**: `data-content-category`, one of the [data categories](#data-categories-and-sub-categories)
- **Optional**: `data-content-sub-category`, which must be one of the sub-categories of the chosen category. It is
  rejected for categories that have no sub-categories.

Both accept a single value.

```json
{
  "mobilitydcatap:mobilityTheme": {
    "mobilitydcatap-theme:data-content-category": "REAL_TIME_TRAFFIC_DATA",
    "mobilitydcatap-theme:data-content-sub-category": "CURRENT_TRAVEL_TIME"
  }
}
```

#### Mobility Data Standard Structure

`mobilitydcatap:mobilityDataStandard` accepts only `@id` and `mobilitydcatap:schema`, which in turn accepts only
`dcat:downloadURL` and `rdf:Literal`:

```json
{
  "mobilitydcatap:mobilityDataStandard": {
    "@id": "datex2-v3",
    "mobilitydcatap:schema": {
      "dcat:downloadURL": [
        "https://example.com/schema/datex2-v3.xsd"
      ],
      "rdf:Literal": "DATEX II version 3 schema"
    }
  }
}
```

## Data Categories and Sub-categories

<!-- generated:categories:start - do not edit, run `./gradlew :extensions:semantic-validator:test -PupdateDocs` -->

- `ROAD_EVENTS_AND_CONDITIONS`
  - `ACCIDENTS_AND_INCIDENTS`
  - `POOR_ROAD_CONDITIONS`
  - `ROAD_WEATHER_CONDITIONS`
  - `HAZARD_WARNINGS`
- `ROAD_WORK_INFORMATION`
  - `LONG_TERM_ROAD_WORKS`
  - `SHORT_TERM_ROAD_WORKS`
- `REAL_TIME_TRAFFIC_DATA`
  - `CURRENT_TRAVEL_TIME`
  - `EXPECTED_DELAYS`
  - `LOCATION_AND_LENGTH_OF_QUEUES`
  - `PREDICTED_TRAVEL_TIME`
  - `SPEED`
  - `TRAFFIC_DATA_AT_BORDER_CROSSINGS_TO_THIRD_COUNTRIES`
  - `TRAFFIC_VOLUME`
  - `WAITING_TIME_AT_BORDER_CROSSINGS_TO_NON-EU_MEMBER_STATES`
- `PARKING_SERVICE_AND_REST_AREA_INFORMATION`
  - `BIKE_PARKING_LOCATIONS`
  - `CAR_PARKING_AVAILABILITY`
  - `CAR_PARKING_LOCATIONS_AND_CONDITIONS`
  - `PARK_AND_RIDE_STOPS`
  - `SERVICE_AND_REST_AREA_AVAILABILITY`
  - `SERVICE_AND_REST_AREA_LOCATIONS_AND_CONDITIONS`
  - `TRUCK_PARKING_AVAILABILITY`
  - `TRUCK_PARKING_LOCATIONS_AND_CONDITIONS`
- `FILLING_AND_CHARGING_STATIONS`
  - `AVAILABILITY_OF_CHARGING_POINTS_FOR_ELECTRIC_VEHICLES`
  - `LOCATION_AND_CONDITIONS_OF_CHARGING_POINTS`
  - `AVAILABILITY_OF_FILLING_STATIONS`
  - `LOCATION_AND_CONDITIONS_OF_FILLING_STATIONS`
- `DYNAMIC_TRAFFIC_SIGNS_AND_REGULATIONS`
  - `BRIDGE_CLOSURES_AND_ACCESS_CONDITIONS`
  - `DIRECTION_OF_TRAVEL_ON_REVERSIBLE_LANES`
  - `DYNAMIC_OVERTAKING_BANS_ON_HEAVY_GOODS_VEHICLES`
  - `DYNAMIC_SPEED_LIMITS`
  - `LANE_CLOSURES_AND_ACCESS_CONDITIONS`
  - `OTHER_ACCESS_RESTRICTIONS_AND_TRAFFIC_REGULATIONS`
  - `OTHER_TEMPORARY_TRAFFIC_MANAGEMENT_MEASURES_OR_PLANS`
  - `ROAD_CLOSURES_AND_ACCESS_CONDITIONS`
  - `TUNNEL_CLOSURES_AND_ACCESS_CONDITIONS`
- `STATIC_TRAFFIC_SIGNS_AND_REGULATIONS`
  - `BRIDGE_ACCESS_CONDITIONS`
  - `OTHER_STATIC_TRAFFIC_SIGNS`
  - `OTHER_TRAFFIC_REGULATIONS`
  - `PERMANENT_ACCESS_RESTRICTIONS`
  - `SPEED_LIMITS`
  - `TRAFFIC_CIRCULATION_PLANS`
  - `TUNNEL_ACCESS_CONDITIONS`
- `PUBLIC_TRANSPORT_SCHEDULED_TRANSPORT`
  - `BASIC_COMMERCIAL_CONDITIONS`
  - `BASIC_COMMON_STANDARD_FARES`
  - `COMMON_FARE_PRODUCTS`
  - `CONNECTION_LINKS`
  - `DISRUPTIONS_DELAYS_CANCELLATIONS`
  - `ENVIRONMENTAL_STANDARDS_FOR_VEHICLES`
  - `HOURS_OF_OPERATION`
  - `NETWORK_TOPOLOGY_AND_ROUTES_LINES`
  - `OPERATIONAL_CALENDAR`
  - `PASSENGER_CLASSES`
  - `PLANNED_INTERCHANGES_BETWEEN_SCHEDULED_SERVICES`
  - `PURCHASE_INFORMATION`
  - `REAL_TIME_ESTIMATED_DEPARTURE_AND_ARRIVAL_TIMES`
  - `SPECIAL_FARE_PRODUCTS`
  - `STOP_FACILITIES_ACCESSIBILITY_AND_PATHS_WITHIN_FACILITY`
  - `STOP_FACILITIES_GEOMETRY_AND_MAP_LAYOUT`
  - `STOP_FACILITIES_LOCATION_AND_FEATURES`
  - `STOP_FACILITIES_STATUS_OF_FEATURES`
  - `TIMETABLES_STATIC`
  - `TRANSPORT_OPERATORS`
  - `VEHICLE_DETAILS`
- `PUBLIC_TRANSPORT_NON_SCHEDULED_TRANSPORT`
  - `ACCESIBILITY_INFORMATION_FOR_VEHICLES`
  - `ENVIRONMENTAL_STANDARDS_FOR_VEHICLES`
  - `FARES`
  - `LOCATIONS_AND_STATIONS`
  - `PROVIDER_DATA`
  - `RESERVATION_AND_PURCHASE_OPTIONS`
  - `SERVICE_AREAS_AND_SERVICE_TIMES`
- `SHARING_AND_HIRING_SERVICES`
  - `BIKE_HIRING_AVAILABILITY`
  - `BIKE_HIRING_STATIONS`
  - `BIKE_SHARING_AVAILABILITY`
  - `BIKE_SHARING_LOCATIONS_AND_STATIONS`
  - `CAR_HIRING_STATIONS`
  - `CAR_HIRING_AVAILABILITY`
  - `CAR_SHARING_AVAILABILITY`
  - `CAR_SHARING_LOCATIONS_AND_STATIONS`
  - `ENVIRONMENTAL_STANDARDS_FOR_VEHICLES`
  - `E_SCOOTER_SHARING_AVAILABILITY`
  - `E_SCOOTER_SHARING_LOCATIONS_AND_STATIONS`
  - `PAYMENT_METHODS`
- `CYCLE_NETWORK_DATA`
  - `NETWORK_CLOSURES_DIVERSIONS`
  - `NETWORK_DETAILED_ATTRIBUTES`
  - `NETWORK_GEOMETRY_AND_LANE_CHARACTER`
- `GENERAL_INFORMATION_FOR_TRIP-PLANNING`
  - `ADDRESS_IDENTIFIERS`
  - `PARAMETERS_NEEDED_TO_CALCULATE_COSTS`
  - `PARAMETERS_NEEDED_TO_CALCULATE_ENVIRONMENTAL_FACTORS`
  - `POINTS_OF_INTERESTS`
  - `TOPOGRAPHIC_PLACES`
- `STATIC_ROAD_NETWORK_DATA`
  - `GEOMETRY`
  - `GRADIENTS`
  - `JUNCTIONS`
  - `NUMBER_OF_LANES`
  - `ROAD_CLASSIFICATION`
  - `ROAD_WIDTH`
- `PEDESTRIAN_NETWORK_DATA`
  - `PEDESTRIAN_ACCESSIBILITY_FACILITIES`
  - `PEDESTRIAN_NETWORK_GEOMETRY`
- `TOLL_INFORMATION`
  - `APPLICABLE_ROAD_USER_CHARGES_AND_PAYMENT_METHODS`
  - `IDENTIFICATION_OF_TOLLED_ROADS`
  - `LOCATION_OF_TOLLING_STATIONS`
  - `PAYMENT_METHODS_FOR_TOLLS`
- `FREIGHT_AND_LOGISTICS`
  - `AVAILABILITY_OF_DELIVERY_AREAS`
  - `FREIGHT_DELIVERY_REGULATIONS`
  - `LOCATION_OF_DELIVERY_AREAS`
- `AIR_AND_SPACE_TRAVEL` (no sub-categories)
- `WATERWAYS_AND_WATER_BODIES` (no sub-categories)
- `OTHER` (no sub-categories)

<!-- generated:categories:end -->

## Transport Modes

<!-- generated:transport-modes:start - do not edit, run `./gradlew :extensions:semantic-validator:test -PupdateDocs` -->

- `AIR`
- `BICYCLE`
- `BIKE_HIRE`
- `BIKE_SHARING`
- `BUS`
- `CAR`
- `CAR_HIRE`
- `CAR_POOLING`
- `CAR_SHARING`
- `E_SCOOTER`
- `LONG_DISTANCE_COACH`
- `LONG_DISTANCE_RAIL`
- `MARITIME`
- `METRO_SUBWAY_TRAIN`
- `MOTORCYCLE`
- `OTHER`
- `PEDESTRIAN`
- `REGIONAL_AND_LOCAL_RAIL`
- `RIDE_POOLING`
- `SHUTTLE_BUS`
- `SHUTTLE_FERRY`
- `TAXI`
- `TRAM_LIGHT_RAIL`
- `TRUCK`

<!-- generated:transport-modes:end -->

## Semantic Validation

The MDS connector validates the asset `properties` when an asset is created or updated. The JSON Schema is applied to
the **expanded** JSON-LD form of the asset, so property names are full IRIs and literal values are JSON-LD value
objects (`[{"@value": "..."}]`), independently of the prefixes used in the request.

### Validation Rules

1. **Required properties**
   - `dct:title` is present
   - `mobilitydcatap:mobilityTheme` is present and contains a `data-content-category`

2. **Allowed properties**
   - Only the properties listed in [Asset Properties](#asset-properties) are accepted, at the top level and inside
     nested objects

3. **Enumerations**
   - `data-content-category` is one of the [data categories](#data-categories-and-sub-categories)
   - `data-content-sub-category` (if present) belongs to the specified category
   - `transportMode` (if present) is one of the [transport modes](#transport-modes)

4. **Structure**
   - Literal properties contain values, not objects
   - `data-content-category`, `data-content-sub-category` and `transportMode` contain a single value

### Validation Failures

If validation fails, the request is rejected with `400 Bad Request` and a list of violations, each one indicating the
path of the offending property and why it failed (missing, not allowed, invalid value, wrong type).

## Complete Asset Example

```json
{
  "@context": "https://w3id.org/mobility-dataspace/connector/management/v1",
  "@type": "Asset",
  "@id": "asset-id",
  "properties": {
    "dct:title": "My Asset",
    "dct:description": "Lorem Ipsum ...",
    "dct:language": "code/EN",
    "dct:publisher": "https://data-source.my-org/about",
    "dct:license": "https://data-source.my-org/license",
    "dct:rightsHolder": "my-sovereign-legal-name",
    "dct:accessRights": "usage policies and rights",
    "dct:spatial": {
      "skos:prefLabel": "my-geo-location",
      "dct:identifier": ["DE", "DE636"]
    },
    "dct:isReferencedBy": "https://data-source.my-org/references",
    "dct:temporal": {
      "dcat:startDate": "2024-02-01",
      "dcat:endDate": "2024-02-10"
    },
    "dct:accrualPeriodicity": "every month",
    "dcat:organization": "Company Name",
    "dcat:keywords": ["some", "keywords"],
    "dcat:mediaType": "application/json",
    "dcat:landingPage": "https://data-source.my-org/docs",
    "owl:versionInfo": "1.1",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "GENERAL_INFORMATION_FOR_TRIP-PLANNING",
      "mobilitydcatap-theme:data-content-sub-category": "ADDRESS_IDENTIFIERS"
    },
    "mobilitydcatap:mobilityDataStandard": {
      "@id": "my-data-model-001",
      "mobilitydcatap:schema": {
        "dcat:downloadURL": [
          "https://teamabc.departmentxyz.schema/a",
          "https://teamabc.departmentxyz.schema/b"
        ],
        "rdf:Literal": "These reference files are important"
      }
    },
    "mobilitydcatap:transportMode": "CAR",
    "mobilitydcatap:georeferencingMethod": "my-geo-reference-method",
    "adms:sample": ["https://teamabc.departmentxyz.sample/a", "https://teamabc.departmentxyz.sample/b"],
    "additionalProperties": {}
  },
  "dataAddress": {
    "type": "HttpData",
    "name": "Example",
    "baseUrl": "https://example.com/"
  }
}
```
