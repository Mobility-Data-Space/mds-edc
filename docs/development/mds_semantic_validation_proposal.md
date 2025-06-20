# MDS Asset Properties and Semantic Validations

## @context

```json
{
  "@context": {
    "@vocabulary": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "owl": "http://www.w3.org/2002/07/owl#",
    "mobilitydcatap": "https://w3id.org/mobilitydcat-ap/",
    "mobilitydcatap-theme": "https://w3id.org/mobilitydcat-ap/mobility-theme/",
    "adms": "http://www.w3.org/ns/adms#",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "skos": "http://www.w3.org/2004/02/skos/core#",
    "rdf": "http://www.w3.org/2000/01/rdf-schema#"
  }
}
```

## General Asset Properties

| Prefix | Attribute name | Type | Required | Examples |
|--------|----------------|------|----------|----------|
| dct | title | String | Yes | My Asset |
| dcat | keywords | Array[String] | No | ["some", "keywords"] |
| dcat | mediaType | String | No | application/json |
| dcat | landingPage | URL | No | https://data-source.my-org/docs |
| dct | description | String | No | Lorem Ipsum ... |
| dct | language | String | No | code/EN |
| dct | publisher | URL | No | https://data-source.my-org/about |
| dct | license | URL | No | https://data-source.my-org/license |
| dct | rightsHolder | String | No | my-sovereign-legal-name |
| dct | accessRights | String | No | usage policies and rights |
| dct | spatial | Object ```{ "skos:prefLabel": String, "dct:identifier": Array[String] }``` | No | {"skos:prefLabel": "my-geo-location", "dct:identifier": ["DE", "DE636"]} |
| dct | isReferencedBy | URL | No | https://data-source.my-org/references |
| dct | temporal | Object ```{ "dcat:startDate": String, "dcat:endDate": String }``` | No | {"dcat:startDate": "2024-02-01", "dcat:endDate": "2024-02-10"} |
| dct | accrualPeriodicity | String | No | every month |
| owl | versionInfo | String | No | 1.1 |
| edc | additionalProperties | Custom Object ``` { } ``` | No | See [how to use](./mds_data_offer_types_in_catalog_proposal.md) |

## Mobility Asset Properties

| Prefix | Attribute name | Type | Required | Examples |
|--------|----------------|------|----------|----------|
| mobilitydcatap | mobilityTheme | Object ```{ "mobilitydcatap-theme:data-content-category": DataCategory, "mobilitydcatap-theme:data-content-sub-category": DataSubcategory }``` | Partially (data-content-category is required) | {"mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS", "mobilitydcatap-theme:data-content-sub-category": "GENERAL_INFORMATION_ABOUT_PLANNING_OF_ROUTES"} |
| adms | sample | Array[String] | No | ["https://teamabc.departmentxyz.sample/a", "https://teamabc.departmentxyz.sample/b"] |
| mobilitydcatap | mobilityDataStandard | Object ```{ "@id": String, "mobilitydcatap:schema": { "dcat:downloadURL": Array[URL], "rdf:Literal": String } }``` | No | {"@id": "my-data-model-001", "mobilitydcatap:schema": {"dcat:downloadURL": ["https://teamabc.departmentxyz.schema/a", "https://teamabc.departmentxyz.schema/b"], "rdf:Literal": "These reference files are important"}} |
| mobilitydcatap | transportMode | TransportMode | No | ROAD |
| mobilitydcatap | georeferencingMethod | String | No | my-geo-reference-method |

## Proposal for v1.0.0: Semantic Validation during asset creation

The semantic validation process for MDS asset creation consists of three main steps:

1. Required field validation:
   - Check presence of all properties marked as "Required" in the above tables.
    - Title as a valid non-empty string
    - Data-content-category within mobilitydcatap:mobilityTheme is provided.
        - Verify data-content-category matches a value in the DataCategory enumeration.

2. Enumeration validation:
   - If present, verify mobilitydcatap:mobilityTheme.data-content-sub-category matches a value in the DataSubcategory enumeration and belongs to the specified data-content-category.
   - If present, verify mobilitydcatap:transportMode matches a value in the TransportMode enumeration.

3. Attribute validation:
   - Ensure all asset properties are listed in the General Asset Properties or Mobility Asset Properties tables.

Validation failures result in asset creation rejection with specific error messages. Successful validation allows the connector to add the properties to the DCAT Datasets.

## MDS Data Category and Subcategory Enumerations

```java
public enum DataCategory {
    TRAFFIC_INFORMATION("Traffic Information"),
    ROADWORKS_AND_ROAD_CONDITIONS("Roadworks and Road Conditions"),
    TRAFFIC_FLOW_INFORMATION("Traffic Flow Information"),
    PARKING_INFORMATION("Parking Information"),
    ELECTROMOBILITY("Electromobility"),
    TRAFFIC_SIGNS_AND_SPEED_INFORMATION("Traffic Signs and Speed Information"),
    WEATHER_INFORMATION("Weather Information"),
    PUBLIC_TRANSPORT_INFORMATION("Public Transport Information"),
    SHARED_AND_ON_DEMAND_MOBILITY("Shared and On-Demand Mobility"),
    INFRASTRUCTURE_AND_LOGISTICS("Infrastructure and Logistics"),
    VARIOUS("Various");

    private final String name;

    DataCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<DataSubcategory> getSubcategories() {
        return DataSubcategory.getSubcategoriesForCategory(this);
    }
}

public enum DataSubcategory {
    // Traffic Information
    ACCIDENTS("Accidents", DataCategory.TRAFFIC_INFORMATION),
    HAZARD_WARNINGS("Hazard Warnings", DataCategory.TRAFFIC_INFORMATION),
    
    // Roadworks and Road Conditions
    ROADWORKS("Roadworks", DataCategory.ROADWORKS_AND_ROAD_CONDITIONS),
    ROAD_CONDITIONS("Road Conditions", DataCategory.ROADWORKS_AND_ROAD_CONDITIONS),
    
    // Traffic Flow Information
    REALTIME_TRAFFIC_FLOW_DATA("Realtime Traffic Flow Data", DataCategory.TRAFFIC_FLOW_INFORMATION),
    FORECAST_TRAFFIC_FLOW_DATA("Forecast Traffic Flow Data", DataCategory.TRAFFIC_FLOW_INFORMATION),
    
    // Parking Information
    AVAILABILITY_AND_FORECAST("Availability and Forecast", DataCategory.PARKING_INFORMATION),
    PRICES("Prices", DataCategory.PARKING_INFORMATION),
    
    // Electromobility
    AVAILABILITY_OF_CHARGING_STATION("Availability of Charging Station", DataCategory.ELECTROMOBILITY),
    LOCATION_OF_CHARGING_STATION("Location of Charging Station", DataCategory.ELECTROMOBILITY),
    PRICES_OF_CHARGING_STATION("Prices of Charging Station", DataCategory.ELECTROMOBILITY),
    
    // Traffic Signs and Speed Information
    DYNAMIC_SPEED_INFORMATION("Dynamic Speed Information", DataCategory.TRAFFIC_SIGNS_AND_SPEED_INFORMATION),
    DYNAMIC_TRAFFIC_SIGNS("Dynamic Traffic Signs", DataCategory.TRAFFIC_SIGNS_AND_SPEED_INFORMATION),
    STATIC_TRAFFIC_SIGNS("Static Traffic Signs", DataCategory.TRAFFIC_SIGNS_AND_SPEED_INFORMATION),
    
    // Weather Information
    CURRENT_WEATHER_CONDITIONS("Current Weather Conditions", DataCategory.WEATHER_INFORMATION),
    WEATHER_FORECAST("Weather Forecast", DataCategory.WEATHER_INFORMATION),
    SPECIAL_EVENTS_OR_DISRUPTIONS("Special Events or Disruptions", DataCategory.WEATHER_INFORMATION),
    
    // Public Transport Information
    TIMETABLES("Timetables", DataCategory.PUBLIC_TRANSPORT_INFORMATION),
    FARE("Fare", DataCategory.PUBLIC_TRANSPORT_INFORMATION),
    LOCATION_INFORMATION("Location Information", DataCategory.PUBLIC_TRANSPORT_INFORMATION),
    
    // Shared and On-Demand Mobility
    VEHICLE_INFORMATION("Vehicle Information", DataCategory.SHARED_AND_ON_DEMAND_MOBILITY),
    AVAILABILITY("Availability", DataCategory.SHARED_AND_ON_DEMAND_MOBILITY),
    LOCATION("Location", DataCategory.SHARED_AND_ON_DEMAND_MOBILITY),
    RANGE("Range", DataCategory.SHARED_AND_ON_DEMAND_MOBILITY),
    
    // Infrastructure and Logistics
    GENERAL_INFORMATION_ABOUT_PLANNING_OF_ROUTES("General Information About Planning Of Routes", DataCategory.INFRASTRUCTURE_AND_LOGISTICS),
    PEDESTRIAN_NETWORKS("Pedestrian Networks", DataCategory.INFRASTRUCTURE_AND_LOGISTICS),
    CYCLING_NETWORKS("Cycling Networks", DataCategory.INFRASTRUCTURE_AND_LOGISTICS),
    ROAD_NETWORK("Road Network", DataCategory.INFRASTRUCTURE_AND_LOGISTICS),
    WATER_ROUTES("Water Routes", DataCategory.INFRASTRUCTURE_AND_LOGISTICS),
    CARGO_AND_LOGISTICS("Cargo and Logistics", DataCategory.INFRASTRUCTURE_AND_LOGISTICS),
    TOLL_INFORMATION("Toll Information", DataCategory.INFRASTRUCTURE_AND_LOGISTICS);

    private final String name;
    private final DataCategory category;

    DataSubcategory(String name, DataCategory category) {
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public DataCategory getCategory() {
        return category;
    }

    public static List<DataSubcategory> getSubcategoriesForCategory(DataCategory category) {
        return Arrays.stream(values())
                .filter(subcategory -> subcategory.getCategory() == category)
                .collect(Collectors.toList());
    }
}

public enum TransportMode {
    ROAD("Road"),
    RAIL("Rail"),
    WATER("Water"),
    AIR("Air");

    private final String name;

    TransportMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
