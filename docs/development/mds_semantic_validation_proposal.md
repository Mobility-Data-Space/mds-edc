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

## MDS Data Category and Subcategory Representation

```json
{
  "categories": [
    {
      "name": "Traffic Information",
      "id": "TRAFFIC_INFORMATION",
      "subcategories": [
        { "name": "Accidents", "id": "ACCIDENTS" },
        { "name": "Hazard Warnings", "id": "HAZARD_WARNINGS" }
      ]
    },
    {
      "name": "Roadworks and Road Conditions",
      "id": "ROADWORKS_AND_ROAD_CONDITIONS",
      "subcategories": [
        { "name": "Roadworks", "id": "ROADWORKS" },
        { "name": "Road Conditions", "id": "ROAD_CONDITIONS" }
      ]
    },
    {
      "name": "Traffic Flow Information",
      "id": "TRAFFIC_FLOW_INFORMATION",
      "subcategories": [
        { "name": "Realtime Traffic Flow Data", "id": "REALTIME_TRAFFIC_FLOW_DATA" },
        { "name": "Forecast Traffic Flow Data", "id": "FORECAST_TRAFFIC_FLOW_DATA" }
      ]
    },
    {
      "name": "Parking Information",
      "id": "PARKING_INFORMATION",
      "subcategories": [
        { "name": "Availability and Forecast", "id": "AVAILABILITY_AND_FORECAST" },
        { "name": "Prices", "id": "PRICES" }
      ]
    },
    {
      "name": "Electromobility",
      "id": "ELECTROMOBILITY",
      "subcategories": [
        { "name": "Availability of Charging Station", "id": "AVAILABILITY_OF_CHARGING_STATION" },
        { "name": "Location of Charging Station", "id": "LOCATION_OF_CHARGING_STATION" },
        { "name": "Prices of Charging Station", "id": "PRICES_OF_CHARGING_STATION" }
      ]
    },
    {
      "name": "Traffic Signs and Speed Information",
      "id": "TRAFFIC_SIGNS_AND_SPEED_INFORMATION",
      "subcategories": [
        { "name": "Dynamic Speed Information", "id": "DYNAMIC_SPEED_INFORMATION" },
        { "name": "Dynamic Traffic Signs", "id": "DYNAMIC_TRAFFIC_SIGNS" },
        { "name": "Static Traffic Signs", "id": "STATIC_TRAFFIC_SIGNS" }
      ]
    },
    {
      "name": "Weather Information",
      "id": "WEATHER_INFORMATION",
      "subcategories": [
        { "name": "Current Weather Conditions", "id": "CURRENT_WEATHER_CONDITIONS" },
        { "name": "Weather Forecast", "id": "WEATHER_FORECAST" },
        { "name": "Special Events or Disruptions", "id": "SPECIAL_EVENTS_OR_DISRUPTIONS" }
      ]
    },
    {
      "name": "Public Transport Information",
      "id": "PUBLIC_TRANSPORT_INFORMATION",
      "subcategories": [
        { "name": "Timetables", "id": "TIMETABLES" },
        { "name": "Fare", "id": "FARE" },
        { "name": "Location Information", "id": "LOCATION_INFORMATION" }
      ]
    },
    {
      "name": "Shared and On-Demand Mobility",
      "id": "SHARED_AND_ON_DEMAND_MOBILITY",
      "subcategories": [
        { "name": "Vehicle Information", "id": "VEHICLE_INFORMATION" },
        { "name": "Availability", "id": "AVAILABILITY" },
        { "name": "Location", "id": "LOCATION" },
        { "name": "Range", "id": "RANGE" }
      ]
    },
    {
      "name": "Infrastructure and Logistics",
      "id": "INFRASTRUCTURE_AND_LOGISTICS",
      "subcategories": [
        { "name": "General Information About Planning Of Routes", "id": "GENERAL_INFORMATION_ABOUT_PLANNING_OF_ROUTES" },
        { "name": "Pedestrian Networks", "id": "PEDESTRIAN_NETWORKS" },
        { "name": "Cycling Networks", "id": "CYCLING_NETWORKS" },
        { "name": "Road Network", "id": "ROAD_NETWORK" },
        { "name": "Water Routes", "id": "WATER_ROUTES" },
        { "name": "Cargo and Logistics", "id": "CARGO_AND_LOGISTICS" },
        { "name": "Toll Information", "id": "TOLL_INFORMATION" }
      ]
    },
    {
      "name": "Various",
      "id": "VARIOUS",
      "subcategories": []
    }
  ]
}
