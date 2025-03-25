#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting EDPS and Daseen contract setup...${NC}\n"

# Setup Service Provider side
echo -e "${GREEN}1. Creating EDPS Asset...${NC}"
curl -d @resources/requests/create-edps-asset.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/assets \
  -s | jq

echo -e "${GREEN}1. Creating Daseen Asset...${NC}"
curl -d @resources/requests/create-daseen-asset.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/assets \
  -s | jq

echo -e "\n${GREEN}2. Creating Policy...${NC}"
curl -d @resources/requests/create-policy.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/policydefinitions \
  -s | jq

echo -e "\n${GREEN}3. Creating Contract Definition...${NC}"
curl -d @resources/requests/create-contract-definition.json \
  -H 'content-type: application/json' http://localhost:29193/management/v3/contractdefinitions \
  -s | jq

# Establish contracts
echo -e "\n${GREEN}4. Fetching Catalog...${NC}"
CATALOG_RESPONSE=$(curl -d @resources/requests/fetch-service-provider-catalog.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/catalog/request \
  -s)

echo "$CATALOG_RESPONSE" | jq

# Extract contract offer IDs for both services
EDPS_OFFER_ID=$(echo "$CATALOG_RESPONSE" | jq -r '.["dcat:dataset"][] | select(.id == "edps1") | .["odrl:hasPolicy"]["@id"]')
DASEEN_OFFER_ID=$(echo "$CATALOG_RESPONSE" | jq -r '.["dcat:dataset"][] | select(.id == "daseen1") | .["odrl:hasPolicy"]["@id"]')

echo -e "\n${YELLOW}Found EDPS contract offer ID: $EDPS_OFFER_ID${NC}"
echo -e "${YELLOW}Found Daseen contract offer ID: $DASEEN_OFFER_ID${NC}"

# Process EDPS contract
echo -e "\n${GREEN}5a. Setting up EDPS contract...${NC}"
sed "s/{{contract-offer-id}}/$EDPS_OFFER_ID/" resources/requests/negotiate-edps-contract.json > temp_negotiate_edps.json

EDPS_NEGOTIATION_RESPONSE=$(curl -d @temp_negotiate_edps.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/contractnegotiations \
  -s)

echo "EDPS Negotiation Response:"
echo "$EDPS_NEGOTIATION_RESPONSE" | jq

# Process Daseen contract
echo -e "\n${GREEN}5b. Setting up Daseen contract...${NC}"
sed "s/{{contract-offer-id}}/$DASEEN_OFFER_ID/" resources/requests/negotiate-daseen-contract.json > temp_negotiate_daseen.json

DASEEN_NEGOTIATION_RESPONSE=$(curl -d @temp_negotiate_daseen.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/contractnegotiations \
  -s)

echo "Daseen Negotiation Response:"
echo "$DASEEN_NEGOTIATION_RESPONSE" | jq

echo -e "\n${YELLOW}Waiting for contract negotiations to complete...${NC}"
sleep 2

# Get contract IDs
EDPS_CONTRACT_ID=$(curl -X POST http://localhost:29193/management/v3/contractagreements/request \
  -s | jq -r '.[] | select(.assetId == "edps1") | .["@id"]')

DASEEN_CONTRACT_ID=$(curl -X POST http://localhost:29193/management/v3/contractagreements/request \
  -s | jq -r '.[] | select(.assetId == "daseen1") | .["@id"]')

echo -e "${YELLOW}EDPS Contract Agreement ID: $EDPS_CONTRACT_ID${NC}"
echo -e "${YELLOW}Daseen Contract Agreement ID: $DASEEN_CONTRACT_ID${NC}"

# Start EDPS transfer process
echo -e "\n${GREEN}6a. Initiating EDPS transfer process...${NC}"
sed "s/{{contract-id}}/$EDPS_CONTRACT_ID/" resources/requests/start-transfer.json > temp_transfer_edps.json

EDPS_TRANSFER_RESPONSE=$(curl -d @temp_transfer_edps.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/transferprocesses \
  -s)

echo "EDPS Transfer Response:"
echo "$EDPS_TRANSFER_RESPONSE" | jq

# Start Daseen transfer process
echo -e "\n${GREEN}6b. Initiating Daseen transfer process...${NC}"
sed "s/{{contract-id}}/$DASEEN_CONTRACT_ID/" resources/requests/start-transfer.json > temp_transfer_daseen.json

DASEEN_TRANSFER_RESPONSE=$(curl -d @temp_transfer_daseen.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/transferprocesses \
  -s)

echo "Daseen Transfer Response:"
echo "$DASEEN_TRANSFER_RESPONSE" | jq

# Cleanup temporary files
rm temp_negotiate_edps.json temp_negotiate_daseen.json temp_transfer_edps.json temp_transfer_daseen.json

echo -e "\n${GREEN}Setup complete!${NC}"

echo -e "export EDPS_CONTRACT_ID=${EDPS_CONTRACT_ID}"
echo -e "export DASEEN_CONTRACT_ID=${DASEEN_CONTRACT_ID}"