# On-Request Data Offers

The On-Request Data Offers feature allows MDS participants to publish asset metadata without having an immediately available data source. This enables providers to advertise their willingness and capability to provide specific types of data upon consumer request, facilitating demand-driven data sharing in the dataspace.

## Purpose

This feature addresses scenarios where:

- Data is expensive or resource-intensive to prepare
- Data needs to be generated dynamically based on specific requirements
- Providers want to gauge demand before investing in data preparation
- Data availability depends on external factors or approvals
- Providers want to advertise data capabilities without committing resources upfront

## How It Works

### Provider Perspective

Providers can publish "on-request" assets by including the `additionalProperties` object in the asset properties with specific attributes indicating the asset is available upon request.

#### Required Attributes

The `additionalProperties` object must include:

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `onRequest` | Boolean | Yes | Indicates the asset is available on request (set to `true`) |
| `email` | String | Yes | Contact email address for data requests |
| `preferred_subject` | String | No | Suggested email subject line for requests |

### Creating an On-Request Asset

#### Example Asset Configuration

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "dct": "http://purl.org/dc/terms/",
    "dcat": "http://www.w3.org/ns/dcat#",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "Asset",
  "@id": "traffic-data-on-request-001",
  "properties": {
    "dct:title": "Historical Traffic Data - Custom Timeframes",
    "dct:description": "Historical traffic flow data available upon request for custom time periods and geographic areas",
    "dcat:keywords": ["traffic", "historical", "on-request", "custom"],
    "dcat:mediaType": "application/json",
    "edc:additionalProperties": {
      "onRequest": true,
      "email": "data-requests@example.com",
      "preferred_subject": "Request for Historical Traffic Data"
    }
  },
  "dataAddress": {
    "@type": "MDSOnRequest",
    "email": "data-requests@example.com",
    "preferred_subject": "Request for Historical Traffic Data"
  }
}
```

### Consumer Perspective

Consumers can discover on-request assets through the provider's catalog and identify them by checking the `additionalProperties` field in the DCAT dataset.

#### Catalog Response Example

When browsing the catalog, consumers will see:

```json
{
  "dcat:dataset": [{
    "@id": "traffic-data-on-request-001",
    "@type": "dcat:Dataset",
    "odrl:hasPolicy": {
      "@id": "policy-id",
      "@type": "odrl:Offer",
      "odrl:permission": {
        "odrl:action": {
          "@id": "use"
        }
      }
    },
    "dcat:distribution": [],
    "dct:title": "Historical Traffic Data - Custom Timeframes",
    "dct:description": "Historical traffic flow data available upon request for custom time periods and geographic areas",
    "dcat:keywords": ["traffic", "historical", "on-request", "custom"],
    "dcat:mediaType": "application/json",
    "additionalProperties": {
      "onRequest": true,
      "email": "data-requests@example.com",
      "preferred_subject": "Request for Historical Traffic Data"
    }
  }]
}
```
