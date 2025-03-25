#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

#EDPS_CONTRACT_ID="<EDPS_CONTRACT_ID>"
if [[ -z "$EDPS_CONTRACT_ID" ]]; then
    echo "Error: EDPS_CONTRACT_ID is not defined. Please set it before running the script."
    exit 1
fi

#DASEEN_CONTRACT_ID="<DASEEN_CONTRACT_ID>"
if [[ -z "$DASEEN_CONTRACT_ID" ]]; then
    echo "Error: DASEEN_CONTRACT_ID is not defined. Please set it before running the script."
    exit 1
fi

echo -e "${GREEN}Creating source asset for EDPS...${NC}\n"
curl -d @resources/requests/create-asset.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/assets \
  -s | jq


echo -e "${GREEN}Creating EDPS job...${NC}\n"
echo "EDPS_CONTRACT_ID is: $EDPS_CONTRACT_ID"
JOB_RESPONSE=$(curl -d "{\"contractId\": \"$EDPS_CONTRACT_ID\"}" \
   -H 'content-type: application/json' http://localhost:19191/api/edp/edps/assetId1/jobs \
   -s | jq
)
echo "$JOB_RESPONSE" | jq

# Todo: Check extraction of job ID
JOB_ID=$(echo "$JOB_RESPONSE" | jq -r '.jobId')

echo -e "${GREEN}Creating result asset for job $JOB_ID...${NC}\n"

curl -X POST http://localhost:19191/api/edp/edps/assetId1/jobs/$JOB_ID/result \
 -H 'content-type: application/json' \
 -d @resources/requests/fetch-edps-result.json

curl -d @resources/requests/create-result-asset.json \
  -H 'content-type: application/json' http://localhost:19193/management/v3/assets \
  -s | jq

echo -e "${GREEN}Publishing to Daseen...${NC}\n"
curl -d "{\"contractId\": \"$DASEEN_CONTRACT_ID\"}" \
  -H 'content-type: application/json' http://localhost:19191/api/edp/daseen/resultAssetId1 \
  -s | jq


echo -e "\n${GREEN}Done!${NC}"

exit 0
