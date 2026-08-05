#!/usr/bin/env bash
set -e
./mvnw clean verify --batch-mode --no-transfer-progress -T 1C
