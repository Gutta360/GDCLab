#!/bin/bash

set -e

SERVER="${SERVER:-gutta@100.65.174.126}"
REMOTE_DIR="${REMOTE_DIR:-/var/www/gdclab}"

echo "========================================"
echo " GDCLab FRONTEND DEPLOYMENT"
echo "========================================"

npm ci
npm run build -- --configuration production --base-href /gdclab/

rsync -av --delete dist/gdclab-ui/browser/ "$SERVER:$REMOTE_DIR/"

echo ""
echo "GDCLab frontend deployed to $REMOTE_DIR"
