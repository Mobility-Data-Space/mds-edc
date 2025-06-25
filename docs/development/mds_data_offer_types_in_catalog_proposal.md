# Support for the MDS Data Offer Type On Request

## Introduction

Within MDS, it is possible for participants to publish an asset without having an existing data source, sharing only metadata. The purpose of such an asset is to show readiness to provide data in case of interest from consumers. This proposal aims to define a clear process for both providers and consumers to interact with such assets.

## Provider

Participants who wish to publish assets on request can do so by including the `additionalProperties` object in the asset properties, with the following attributes:
- `onRequest`: A boolean indicating the asset is available on request.
- `email`: The contact email for data requests.
- `preferred_subject`: A suggested subject line for the request email.

### Example:
```json
{
    "edc:additionalProperties": {
        "onRequest": "true",
        "email": "provider@example.com",
        "preferred_subject": "Request for Data Access"
    }
}
```

## Consumer

Consumers can discover assets marked as "on request" when browsing the catalog of the provider, through the DCAT asset attribute `additionalProperties`.

### Example:
```json
{   
    "dcat:dataset": [{
        "@id": "asset-8-id",
        "@type": "dcat:Dataset",
        "odrl:hasPolicy": {
          "@id": "c2VydmljZXMtb2ZmZXI=:jg0ZjUtYjI3ZC00MGM3LTgzY2ItNTc0MDRiMWQ5MWMx",
          "@type": "odrl:Offer",
          "odrl:permission": {
            "odrl:action": {
              "@id": "use"
            }
          },
          "odrl:prohibition": [],
          "odrl:obligation": []
        },
        "dcat:distribution": [],
        "dct:accrualPeriodicity": "every month",
        "dcat:organization": "Company Name",
        "dct:description": "Description for Asset 8",
        "dct:title": "Asset 8",
        "dcat:mediaType": "application/json",
        "id": "asset-8-id",
        "additionalProperties": {
            "onRequest": "true",
            "email": "provider@example.com",
            "preferred_subject": "Request for Data Access"
        }
    }]
}
```

It is possible, **but not useful**, to negotiate a contract with the connector. **However**, it is intended that the consumer proactively reaches out to the provider through the email address.

To request data, they should:
1. Use the provided email address to contact the provider.
2. Include the preferred subject line in their email to ensure clarity and expedite the process.

This approach leverages existing MDS capabilities while introducing minimal changes to the current framework.