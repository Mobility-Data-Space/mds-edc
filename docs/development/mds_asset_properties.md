
# MDS Asset Properties and Semantic Validations

## General Asset Properties
| Property JSONLD | Attribute| Cardinality | Type | Explanation | Example |
|-----------|-----------|---------|------|------------|--------------|
| @id | Asset ID |  1  |  String  | Dataset Id (in the MDS Frontend will be generated automatically) | traffic-situation-in-hamburg-1.0 |
| http://purl.org/dc/terms/title | Title |  1  |  String  | Dataset Name | Traffic situation in Hamburg|
| http://www.w3.org/2002/07/owl#versionInfo | Version |  0..1  |  String  | Dataset Version | 1.0.0 |
| http://purl.org/dc/terms/description | Description |  0..1  |  String with Markdown | Dataset Description | The dataset contains the traffic situation in real time on the Hamburg road network. The traffic situation is divided into 4 condition classes: **flowing traffic** (green), **heavy traffic** (orange), **slow-moving traffic** (red), **queued traffic** (dark red).|
| http://www.w3.org/ns/dcat#keywords | Keywords |  0..*  |  String | Keywords describing the dataset | traffic; hamburg; traffic jams |
| http://purl.org/dc/terms/language | Language |  0..*  |  Enumeration | Language of the dataset (use "Multilingual" if more than one language is used) | German |
| http://www.w3.org/ns/dcat#mediaType | Content type |  0..*  |  String | Content type of the dataset | application/json |
| http://www.w3.org/ns/dcat#landingPage | Endpoint Documentation |  0..1  | URL | Documentation describing the dataset, its parameters and values | https://api.hamburg.de/datasets/v1/verkehrslage//api?f=html |
| http://purl.org/dc/terms/publisher | Publisher |  0..1  | URL | Homepage of the participant who makes the dataset available within the MDS | https://mobility-dataspace.eu/ |
| http://www.w3.org/ns/dcat#organization | Organization |  1  | String | Legal name of the participant who makes the dataset available within the MDS | DRM GmbH |
| http://purl.org/dc/terms/license | Standard licence |  0..1  | URL | License under which is the dataset available | https://www.govdata.de/dl-de/by-2-0 |

## Mobility Asset Properties
| Property JSONLD | Attribute| Cardinality | Type | Explanation | Example |
|-----------|-----------|---------|------|------------|--------------|
| https://w3id.org/mobilitydcat-ap#mobilityTheme | Data Category |  1  |  Enumeration | Vordefined [MDS Data Category](https://github.com/Mobility-Data-Space/mobility-data-space/wiki/MDS-Ontology#data-category) | Traffic Flow Information |
| https://w3id.org/mobilitydcat-ap#mobilityTheme | Data Subcategory |  0..1  |  Enumeration | Vordefined [MDS Data Subcategory](https://github.com/Mobility-Data-Space/mobility-data-space/wiki/MDS-Ontology#data-subcategory) | Realtime Traffic Flow Data |
| https://w3id.org/mobilitydcat-ap#mobilityDataStandard | Data Model |  0..1  |  String | Mobility Data Standard (e.g. DATEX II, TPEG) | Proprietary |
| https://w3id.org/mobilitydcat-ap#transportMode | Transport Mode |  0..1  |  Enumeration | Vordefined [Transport Mode](https://github.com/Mobility-Data-Space/mobility-data-space/wiki/MDS-Ontology#transport-mode) | Road |
| https://w3id.org/mobilitydcat-ap#georeferencingMethod | Geo Reference Model |  0..1  |  String | Geo Referencing Method (e.g. OpenLR) | GeoJSON |
| http://purl.org/dc/terms/rightsHolder | Sovereign |  0..1  |  String | Legal name of the owner of the dataset | LGV Hamburg |
| http://purl.org/dc/terms/accrualPeriodicity | Data update frequency |  0..1  |  String | How often is the dataset updated. | Every 5 min. |
| http://purl.org/dc/terms/spatial | Geo location |  0..1  |  String | Simple description of the relevant geolocation. | Hamburg and vicinity |
| http://purl.org/dc/terms/spatial | NUTS location |  0..*  |  String | NUTS code(s) for the relevant geolocation. | DE60 |
| http://www.w3.org/ns/adms#sample | Data samples |  0..*  |  URL | Dataset samples if available | - |
| http://purl.org/dc/terms/isReferencedBy | Reference files |  0..*  |  URL | Dataset schemas or other references | - |
| http://purl.org/dc/terms/temporal | Temporal coverage |  0..2  |  Date | Start and/or end date for the dataset | 14.05.2024 - 14.05.2024 |
| http://purl.org/dc/terms/accessRights | Condition for use |  0..1  |  String | Additional condiotions for use, source reference, copyright etc. | Source reference: Freie und Hansestadt Hamburg, Behörde für Verkehr und Mobilitätswende  |

## Semantic Validation during asset creation

Only property names in the list above are accepted to be part of the asset properties. Those properties are added by the connector to the DCAT Datasets.

This list maps the asset attribues of the Mobility Data Space to a recommended MobilityDCAT-AP property.