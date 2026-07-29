# Assets

An asset represents a resource that can be shared within the Dataspace, such as a file or a service endpoint.

## Create Asset

```http
POST /v3/assets
Content-Type: application/json

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
      "mobilitydcatap-theme:data-content-category": "STATIC_ROAD_NETWORK_DATA",
      "mobilitydcatap-theme:data-content-sub-category": "ROAD_CLASSIFICATION"
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

## Create Asset with Minimum Required Properties

```http
POST /v3/assets
Content-Type: application/json

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
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "STATIC_ROAD_NETWORK_DATA"
    }
  },
  "dataAddress": {
    "type": "HttpData",
    "name": "Example",
    "baseUrl": "https://example.com/"
  }
}
```

## Create Kafka Asset

```http
POST /v3/assets
Content-Type: application/json

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
  "@id": "kafka-asset-id",
  "properties": {
    "dct:title": "Real-time Traffic Events Stream",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "DYNAMIC_TRAFFIC_DATA"
    }
  },
  "dataAddress": {
    "type": "Kafka",
    "topic": "traffic-events",
    "kafka.bootstrap.servers": "kafka.example.com:9092",
    "kafka.security.protocol": "SASL_PLAINTEXT",
    "kafka.sasl.mechanism": "OAUTHBEARER",
    "oidcDiscoveryUrl": "https://auth.example.com/.well-known/openid-configuration",
    "oidcRegisterClientTokenKey": "oidc-initial-access-token",
    "kafkaAdminPropertiesKey": "kafka-admin-properties"
  }
}
```

For Confluent Cloud, add `kafka.sasl.oauthbearer.extensions` and use `SASL_SSL`:

```http
POST /v3/assets
Content-Type: application/json

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
  "@id": "kafka-confluent-asset-id",
  "properties": {
    "dct:title": "Real-time Traffic Events Stream (Confluent Cloud)",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "DYNAMIC_TRAFFIC_DATA"
    }
  },
  "dataAddress": {
    "type": "Kafka",
    "topic": "traffic-events",
    "kafka.bootstrap.servers": "pkc-abc123.eu-central-1.aws.confluent.cloud:9092",
    "kafka.security.protocol": "SASL_SSL",
    "kafka.sasl.mechanism": "OAUTHBEARER",
    "kafka.sasl.oauthbearer.extensions": "logicalCluster=lkc-abc123,identityPoolId=pool-xyz",
    "oidcDiscoveryUrl": "https://auth.example.com/.well-known/openid-configuration",
    "oidcRegisterClientTokenKey": "oidc-initial-access-token",
    "kafkaAdminPropertiesKey": "kafka-admin-properties"
  }
}
```

## Create On Request Asset

```http
POST /v3/assets
Content-Type: application/json

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
  "@id": "on-request-asset-id",
  "properties": {
    "dct:title": "On Request Asset",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "STATIC_ROAD_NETWORK_DATA"
    },
    "additionalProperties": {
      "onrequest": "true",
      "email": "contact@example.com",
      "preferred_subject": "Data Access Request"
    }
  },
  "dataAddress": {
    "type": "MDSOnRequest",
    "email": "contact@example.com",
    "preferred_subject": "Data Access Request"
  }
}
```
