#!/bin/bash
# Mock script for Account Vending Machine

PR=""
PROVIDER="aws"
ACTION="create"
FORCE=false

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --pr) PR="$2"; shift ;;
        --provider) PROVIDER="$2"; shift ;;
        --action) ACTION="$2"; shift ;;
        --force) FORCE=true ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

echo "======================================"
echo " Vending Machine Execution "
echo "======================================"
echo "PR: $PR"
echo "Provider: $PROVIDER"
echo "Action: $ACTION"
echo "Force: $FORCE"

if [ "$ACTION" == "create" ]; then
    echo "Creating ephemeral account for PR-$PR on $PROVIDER..."
    # Simulate API call to cloud provider
    sleep 2
    echo "Account created successfully. Account ID: 123456789012"
elif [ "$ACTION" == "destroy" ]; then
    echo "Destroying ephemeral account for PR-$PR on $PROVIDER..."
    if [ "$FORCE" = true ]; then
        echo "Force flag is set. Bypassing safety checks."
    fi
    # Simulate API call to cloud provider
    sleep 2
    echo "Account destroyed/quarantined successfully."
else
    echo "Unknown action: $ACTION"
    exit 1
fi
echo "======================================"
