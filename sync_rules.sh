#!/usr/bin/env bash
# ==============================================================================
# Rules & Skills Synchronizer Runner
# Usage:
#   ./sync_rules.sh              # One-time sync
#   ./sync_rules.sh --watch      # Watch mode (syncs automatically on file save)
#   ./sync_rules.sh --interactive# Interactive terminal configurator
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "${SCRIPT_DIR}/sync_rules_config.py" "$@"
