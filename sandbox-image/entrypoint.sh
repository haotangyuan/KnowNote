#!/bin/sh
set -e
cd /workspace

if [ ! -f package.json ]; then
  echo "Seeding template..."
  cp -r /workspace-template/. /workspace/
fi

echo "Installing dependencies..."
npm install --prefer-offline 2>&1

echo "Starting dev servers..."
exec npm run dev
