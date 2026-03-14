#!/usr/bin/env bash
# Erstellt einen Matrix Bot-User über die Synapse Admin API (Shared Secret Registration).
# Voraussetzung: registration_shared_secret in der homeserver.yaml gesetzt.
#
# Verwendung:
#   ./create-bot-user.sh <homeserver-url> <username> <password> [shared-secret]
#
# Beispiel:
#   ./create-bot-user.sh https://matrix.example.org lea meinPasswort meinSharedSecret
#
# Falls kein Shared Secret angegeben wird, wird nach REGISTRATION_SHARED_SECRET
# in der Umgebung gesucht.

set -euo pipefail

HOMESERVER="${1:?Homeserver-URL fehlt (z.B. https://matrix.example.org)}"
USERNAME="${2:?Username fehlt (z.B. lea)}"
PASSWORD="${3:?Passwort fehlt}"
SHARED_SECRET="${4:-${REGISTRATION_SHARED_SECRET:-}}"

if [[ -z "$SHARED_SECRET" ]]; then
  echo "Fehler: Shared Secret fehlt. Als 4. Argument oder via REGISTRATION_SHARED_SECRET übergeben."
  exit 1
fi

REGISTER_URL="${HOMESERVER}/_synapse/admin/v1/register"

echo "→ Hole Nonce von ${REGISTER_URL}..."
NONCE=$(curl -fsSL "${REGISTER_URL}" | grep -o '"nonce":"[^"]*"' | cut -d'"' -f4)

if [[ -z "$NONCE" ]]; then
  echo "Fehler: Konnte keine Nonce abrufen. Ist der Homeserver erreichbar?"
  exit 1
fi

echo "→ Nonce: ${NONCE}"

# HMAC-SHA1: nonce\0username\0password\0notadmin
HMAC_INPUT="${NONCE}\0${USERNAME}\0${PASSWORD}\0notadmin"
MAC=$(printf "%b" "$HMAC_INPUT" | openssl dgst -sha1 -hmac "$SHARED_SECRET" | awk '{print $2}')

echo "→ Registriere User @${USERNAME}..."
RESPONSE=$(curl -fsSL -X POST "${REGISTER_URL}" \
  -H "Content-Type: application/json" \
  -d "{
    \"nonce\": \"${NONCE}\",
    \"username\": \"${USERNAME}\",
    \"password\": \"${PASSWORD}\",
    \"admin\": false,
    \"mac\": \"${MAC}\"
  }")

echo ""
echo "✓ User erstellt!"
echo ""
echo "Matrix User ID: $(echo "$RESPONSE" | grep -o '"user_id":"[^"]*"' | cut -d'"' -f4)"
echo "Access Token:   $(echo "$RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)"
echo ""
echo "→ Diese Werte in config/lea.yml eintragen:"
echo "   userId:      $(echo "$RESPONSE" | grep -o '"user_id":"[^"]*"' | cut -d'"' -f4)"
echo "   accessToken: $(echo "$RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)"
