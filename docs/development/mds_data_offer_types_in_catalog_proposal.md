# Support for the MDS Data offer type On Request

Within MDS, it is possible for participants to publish an asset without having an existing data source, sharing only metadata. 

The purpose of such an asset is to show readiness to provide data in case of interest from consumers.

The participant inputs both an email and a preferred subject that can be used to request the data.

## Proposed solution

We propose that participants, in case they want to publish assets on request, use the `additionalProperties` asset property with the following object:
```json
{
    "edc:additionalProperties": [{
        "onRequest": "true",
        "email": "",
        "preferred_subject": ""
    }]
}
```