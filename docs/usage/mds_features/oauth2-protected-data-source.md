# OAuth2 Protected Data Sources

This guide explains how to use OAuth2-protected APIs as data sources in the MDS EDC Connector.

The MDS EDC Connector supports OAuth2 authentication for HTTP data sources (provider side). This allows secure integration with OAuth2-protected APIs without exposing credentials in the dataspace.

OAuth2 support enables **Providers** to share data from OAuth2-protected APIs.

## Security Notice

> **⚠️ IMPORTANT - Consumer Side Limitation**
>
> While the EDC framework technically supports OAuth2 for both data sources and data sinks, there was a security vulnerability (CVE-2024-4536) in the consumer-side implementation that was removed in [eclipse-edc/Connector#4152](https://github.com/eclipse-edc/Connector/pull/4152).
> The EDC committer group recommends using OAuth2 primarily on the **provider side**. If consumer-side OAuth2 is required, conduct a thorough security assessment considering the potential security flaws.

## Configuration Properties

The following properties configure OAuth2 authentication for data addresses:

| Property | Description | Required |
|----------|-------------|----------|
| `oauth2:tokenUrl` | OAuth2 token endpoint URL where access tokens are obtained | Yes |
| `oauth2:clientId` | OAuth2 client ID for authentication | Yes |
| `oauth2:clientSecretKey` | **Vault key** containing the client secret (not the secret itself) | Yes |

### Important Notes

- **Client Secret Storage**: The `oauth2:clientSecretKey` property references a **key in HashiCorp Vault**, not the actual secret value
- **Manual Vault Setup**: You must manually add the client secret to Vault before using it
- **Token Management**: The connector automatically obtains and refreshes access tokens as needed.

## OAuth2 Protected Data Sources (Provider)

Use this when you want to **provide data** from an OAuth2-protected API through your EDC connector.

Before creating the asset, add the client secret to your Vault. The vault key you'll reference in the asset is: `my-api-secret`

### Option 1: Create Asset via EDC UI

If using the EDC UI to create an asset:

1. Navigate to the asset creation page
2. Select `Custom Datasource Config (JSON)`
3. Provide the following JSON:

```json
{
  "https://w3id.org/edc/v0.0.1/ns/type": "HttpData",
  "https://w3id.org/edc/v0.0.1/ns/baseUrl": "https://api.example.com/protected-endpoint",
  "oauth2:tokenUrl": "https://auth.example.com/oauth/token",
  "oauth2:clientId": "your-client-id",
  "oauth2:clientSecretKey": "my-api-secret"
}
```

**Replace:**

- `https://api.example.com/protected-endpoint` - The OAuth2-protected API endpoint
- `https://auth.example.com/oauth/token` - OAuth2 token endpoint
- `your-client-id` - Your OAuth2 client ID
- `my-api-secret` - Vault key containing the client secret

### Option 2: Create Asset via Management API

To create an OAuth2-protected asset programmatically:

**Endpoint:** `POST /api/management/v3/assets`

**Headers:**

```
X-Api-Key: YourApiKey
Content-Type: application/json
```

**Request Body:**

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
  "@type": "Asset",
  "@id": "oauth2-protected-asset-1",
  "properties": {
    "dct:title": "My OAuth2 Asset",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS"
    }
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://api.example.com/protected-endpoint",
    "oauth2:tokenUrl": "https://auth.example.com/oauth/token",
    "oauth2:clientId": "your-client-id",
    "oauth2:clientSecretKey": "my-api-secret"
  }
}
```

### How It Works

When a consumer initiates a transfer:

1. Consumer negotiates contract and starts transfer
2. Provider's data plane needs to fetch data from the OAuth2-protected source
3. Data plane checks Vault for the client secret using `oauth2:clientSecretKey`
4. Data plane requests access token from the `oauth2:tokenUrl` using client credentials
5. Data plane uses the access token to authenticate requests to `baseUrl`
6. Data is fetched and transferred to the consumer

## Vault Configuration

Before creating OAuth2-protected assets, you must add the client secret to your HashiCorp Vault instance.

### Critical: Vault Secret Format

**⚠️ IMPORTANT**: All secrets stored in HashiCorp Vault for EDC must follow a specific structure. The secret value must be wrapped in a `content` field.

**Correct Vault Secret Structure:**

```json
{
  "content": "your-actual-client-secret-value"
}
```

**Common Mistake - Do NOT store the secret like this:**

```json
{
  "clientSecret": "your-secret-value"
}
```

or just:

```
your-secret-value
```

The secret must be stored as a value inside the `content` field.

### Adding OAuth2 Client Secrets to Vault

**Using Vault CLI:**

```bash
# Add the client secret
vault kv put secret/my-api-secret \
  content='your-actual-client-secret-value'
```

**Using Vault UI:**

1. Navigate to `secret/` path
2. Create new secret with key: `my-api-secret`
3. Add a field named `content`
4. Set the value to your client secret: `your-actual-client-secret-value`

**Using curl:**

```bash
curl -X POST \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"your-actual-client-secret-value"}' \
  http://vault:8200/v1/secret/my-api-secret
```

**Verification:**

Verify the secret is stored correctly:

```bash
# Retrieve the secret
vault kv get -format=json secret/my-api-secret

# Should return:
{
  "data": {
    "content": "your-actual-client-secret-value"
  }
}
```

### Example: Weather API Secret

If your OAuth2 client secret is `super-secret-weather-key-12345`, store it in Vault like this:

```bash
vault kv put secret/weather-api-secret \
  content='super-secret-weather-key-12345'
```

Then reference it in your asset configuration:

```json
{
  "oauth2:clientSecretKey": "weather-api-secret"
}
```

## Example: Public Weather API

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
  "@type": "Asset",
  "@id": "weather-data-munich",
  "properties": {
    "dct:title": "Munich Weather Data",
    "mobilitydcatap:mobilityTheme": {
      "mobilitydcatap-theme:data-content-category": "INFRASTRUCTURE_AND_LOGISTICS"
    }
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://api.weather-service.com/v1/current?city=Munich",
    "oauth2:tokenUrl": "https://auth.weather-service.com/oauth/token",
    "oauth2:clientId": "mds-connector-client",
    "oauth2:clientSecretKey": "weather-api-secret"
  }
}
```
