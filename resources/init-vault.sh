#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Check if VAULT_TOKEN is set
if [ -z "$VAULT_TOKEN" ]; then
    echo "Error: VAULT_TOKEN environment variable is not set."
    exit 1
fi

# Wait for Vault to be ready
until vault status > /dev/null 2>&1; do
    echo "Waiting for Vault to start..."
    sleep 1
done

# Login to Vault
vault login $VAULT_TOKEN

# Check if OpenSSL is installed, if not, install it
if ! command -v openssl &> /dev/null; then
    echo "OpenSSL is not installed. Attempting to install..."
    if command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y openssl
    elif command -v apk &> /dev/null; then
        apk add --no-cache openssl
    else
        echo "Error: Unable to install OpenSSL. Please install it manually."
        exit 1
    fi
fi

# Generate required files
echo "Generating required files..."

# Generate private key and certificate for transfer proxy
openssl genpkey -algorithm RSA -out private-key.pem
openssl req -new -x509 -key private-key.pem -out cert.pem -days 365 -subj "/CN=transfer-proxy"

# Generate AES key
openssl rand -base64 32 > aes.key

# Check if P12_PASSWORD and P12_CONTENT are set
if [ -z "$P12_PASSWORD" ] || [ -z "$P12_CONTENT" ]; then
    echo "Error: P12_PASSWORD or P12_CONTENT environment variable is not set."
    exit 1
fi

# Extract private key and certificate from P12_CONTENT for DAPS
echo "Extracting private key and certificate from P12_CONTENT for DAPS..."
echo "$P12_CONTENT" | base64 -d > temp.p12
openssl pkcs12 -in temp.p12 -nocerts -out daps.key -nodes -passin env:P12_PASSWORD
openssl pkcs12 -in temp.p12 -clcerts -nokeys -out daps.cert -passin env:P12_PASSWORD
rm temp.p12

echo "Required files generated and extracted."

# Function to create JSON file for a secret
create_json_secret() {
    local file=$1
    local json_file=$2
    local content=$(base64 -w 0 < "$file")
    echo "{\"content\":\"$content\"}" > "$json_file"
}

# Create JSON files for secrets
echo "Creating JSON files for secrets..."
create_json_secret "private-key.pem" "transfer-proxy-token-signer-private-key.json"
create_json_secret "cert.pem" "transfer-proxy-token-signer-public-key.json"
create_json_secret "aes.key" "transfer-proxy-token-encryption-aes-key.json"
create_json_secret "daps.key" "daps-private-key.json"
create_json_secret "daps.cert" "daps-public-key.json"

# Function to safely add a secret from a JSON file
add_secret() {
    local path=$1
    local file=$2

    if [ ! -f "$file" ]; then
        echo "Error: File $file not found."
        return 1
    fi

    vault kv put "$path" @"$file"
}

# Add secrets from JSON files
echo "Adding secrets to Vault..."
add_secret "secret/transfer-proxy-token-signer-private-key" "transfer-proxy-token-signer-private-key.json"
add_secret "secret/transfer-proxy-token-signer-public-key" "transfer-proxy-token-signer-public-key.json"
add_secret "secret/transfer-proxy-token-encryption-aes-key" "transfer-proxy-token-encryption-aes-key.json"
add_secret "secret/daps-private-key" "daps-private-key.json"
add_secret "secret/daps-public-key" "daps-public-key.json"

echo "Vault initialization complete."
