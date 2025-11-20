# MDS Vocabulary and Semantic Validation

The MDS EDC Connector supports Mobility DCAT-AP vocabulary for asset metadata, enabling semantic interoperability within the Mobility Data Space. This feature provides standardized property definitions and validation for mobility-specific data assets.

## Supported Vocabularies

The MDS connector uses the following vocabularies and namespaces:

### JSON-LD Context

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
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

### Vocabulary Prefixes

| Prefix | Namespace | Purpose |
|--------|-----------|---------|
| `dct` | Dublin Core Terms | General metadata (title, description, publisher) |
| `dcat` | Data Catalog | Dataset properties (keywords, media type) |
| `mobilitydcatap` | Mobility DCAT-AP | Mobility-specific properties |
| `mobilitydcatap-theme` | Mobility Themes | Data categories and transport modes |
| `adms` | Asset Description Metadata | Sample data references |
| `owl` | Web Ontology Language | Version information |
| `skos` | Simple Knowledge Organization | Labels and identifiers |
| `edc` | Eclipse Dataspace Components | EDC-specific properties |

## Asset Properties

### General Asset Properties

Standard metadata properties applicable to all assets:

| Prefix | Attribute | Type | Required | Description | Example |
|--------|-----------|------|----------|-------------|---------|
| `dct` | `title` | String | **Yes** | Asset title | "Traffic Flow Data Munich" |
| `dcat` | `keywords` | Array[String] | No | Search keywords | ["traffic", "real-time", "munich"] |
| `dcat` | `mediaType` | String | No | Content media type | "application/json" |
| `dcat` | `landingPage` | URL | No | Dataset landing page | `https://data.example.com/docs` |
| `dct` | `description` | String | No | Detailed description | "Real-time traffic flow data..." |
| `dct` | `language` | String | No | Language code | "en" |
| `dct` | `publisher` | URL | No | Publisher URL | `https://example.com/about` |
| `dct` | `license` | URL | No | License URL | `https://example.com/license` |
| `dct` | `rightsHolder` | String | No | Legal rights holder | "Example Mobility GmbH" |
| `dct` | `accessRights` | String | No | Access rights description | "Restricted to MDS participants" |
| `dct` | `spatial` | Object | No | Geographic coverage | See below |
| `dct` | `isReferencedBy` | URL | No | Reference URL | `https://example.com/references` |
| `dct` | `temporal` | Object | No | Temporal coverage | See below |
| `dct` | `accrualPeriodicity` | String | No | Update frequency | "daily", "hourly" |
| `owl` | `versionInfo` | String | No | Version number | "2.1.0" |
| `edc` | `additionalProperties` | Object | No | Custom properties | See below |

#### Spatial Property Structure

```json
{
  "dct:spatial": {
    "skos:prefLabel": "Munich Metropolitan Area",
    "dct:identifier": ["DE", "DE212"]
  }
}
```

#### Temporal Property Structure

```json
{
  "dct:temporal": {
    "dcat:startDate": "2024-01-01",
    "dcat:endDate": "2024-12-31"
  }
}
```

### Mobility Asset Properties

Mobility-specific properties for transportation data:

| Prefix | Attribute | Type | Required | Description | Example |
|--------|-----------|------|----------|-------------|---------|
| `mobilitydcatap` | `mobilityTheme` | Object | **Partial** | Data category and subcategory | See below |
| `adms` | `sample` | Array[URL] | No | Sample data URLs | ["https://example.com/sample"] |
| `mobilitydcatap` | `mobilityDataStandard` | Object | No | Data model/schema references | See below |
| `mobilitydcatap` | `transportMode` | Enum | No | Transport mode | "ROAD", "RAIL", "AIR" |
| `mobilitydcatap` | `georeferencingMethod` | String | No | Geo-referencing method | "WGS84", "ETRS89" |

#### Mobility Theme Structure

**Required**: `data-content-category`
**Optional**: `data-content-sub-category`

```json
{
  "mobilitydcatap:mobilityTheme": {
    "mobilitydcatap-theme:data-content-category": "TRAFFIC_FLOW_INFORMATION",
    "mobilitydcatap-theme:data-content-sub-category": "REALTIME_TRAFFIC_FLOW_DATA"
  }
}
```

#### Mobility Data Standard Structure

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

## Data Categories and Subcategories

### Available Data Categories

The following data categories are supported in the MDS:

1. **TRAFFIC_INFORMATION**
   - ACCIDENTS
   - HAZARD_WARNINGS

2. **ROADWORKS_AND_ROAD_CONDITIONS**
   - ROADWORKS
   - ROAD_CONDITIONS

3. **TRAFFIC_FLOW_INFORMATION**
   - REALTIME_TRAFFIC_FLOW_DATA
   - FORECAST_TRAFFIC_FLOW_DATA

4. **PARKING_INFORMATION**
   - AVAILABILITY_AND_FORECAST
   - PRICES

5. **ELECTROMOBILITY**
   - AVAILABILITY_OF_CHARGING_STATION
   - LOCATION_OF_CHARGING_STATION
   - PRICES_OF_CHARGING_STATION

6. **TRAFFIC_SIGNS_AND_SPEED_INFORMATION**
   - DYNAMIC_SPEED_INFORMATION
   - DYNAMIC_TRAFFIC_SIGNS
   - STATIC_TRAFFIC_SIGNS

7. **WEATHER_INFORMATION**
   - CURRENT_WEATHER_CONDITIONS
   - WEATHER_FORECAST
   - SPECIAL_EVENTS_OR_DISRUPTIONS

8. **PUBLIC_TRANSPORT_INFORMATION**
   - TIMETABLES
   - FARE
   - LOCATION_INFORMATION

9. **SHARED_AND_ON_DEMAND_MOBILITY**
   - VEHICLE_INFORMATION
   - AVAILABILITY
   - LOCATION
   - RANGE

10. **INFRASTRUCTURE_AND_LOGISTICS**
    - GENERAL_INFORMATION_ABOUT_PLANNING_OF_ROUTES
    - PEDESTRIAN_NETWORKS
    - CYCLING_NETWORKS
    - ROAD_NETWORK
    - WATER_ROUTES
    - CARGO_AND_LOGISTICS
    - TOLL_INFORMATION

11. **VARIOUS**
    - (No specific subcategories)

### Transport Modes

The following transport modes are supported:

- `ROAD`
- `RAIL`
- `AIR`
- `WATER`
- `BICYCLE`
- `PEDESTRIAN`
- `MULTIMODAL`

## Semantic Validation

The MDS connector performs semantic validation during asset creation:

### Validation Steps

1. **Required Field Validation**
   - Verify `dct:title` is present and non-empty
   - Verify `mobilitydcatap:mobilityTheme.data-content-category` is present (if mobility theme is used)

2. **Enumeration Validation**
   - Verify `data-content-category` matches a valid category from the enumeration
   - Verify `data-content-sub-category` (if present) belongs to the specified category
   - Verify `transportMode` (if present) matches a valid transport mode

3. **Schema Validation**
   - Ensure all properties follow the correct structure

### Validation Failures

If validation fails, asset creation is rejected with a specific error message indicating:

- Which field failed validation
- Why it failed (missing, invalid value, wrong type)

## Complete Asset Example

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/",
    "owl": "http://www.w3.org/2002/07/owl#",
    "mobilitydcatap": "https://w3id.org/mobilitydcat-ap/",
    "mobilitydcatap-theme": "https://w3id.org/mobilitydcat-ap/mobility-theme/",
    "adms": "http://www.w3.org/ns/adms#",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "skos": "http://www.w3.org/2004/02/skos/core#",
    "rdf": "http://www.w3.org/2000/01/rdf-schema#"
  },
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
      "mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS",
      "mobilitydcatap-theme:data-content-sub-category": "GENERAL_INFORMATION_ABOUT_PLANNING_OF_ROUTES"
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
    "mobilitydcatap:transportMode": "ROAD",
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
