#!/usr/bin/env python3
"""
Rules & Skills Activation Synchronizer
Reads rules_config.json and updates YAML frontmatter in all Android & Flutter rule and skill files.
Supports:
  python3 sync_rules_config.py             # One-time sync
  python3 sync_rules_config.py --watch     # Live watch mode (auto-syncs on file save)
  python3 sync_rules_config.py --interactive # Interactive terminal selector
"""

import sys
import os
import json
import time
import re
from pathlib import Path

WORKSPACE_ROOT = Path(__file__).resolve().parent
CONFIG_FILE = WORKSPACE_ROOT / "rules_config.json"

PLATFORM_DIRS = {
    "android": WORKSPACE_ROOT / "android_rules_skills" / ".agents",
    "flutter": WORKSPACE_ROOT / "flutter_rules_skills" / ".agents",
}

def parse_frontmatter(content: str):
    """Splits markdown content into frontmatter dict and markdown body."""
    pattern = r"^---\s*\n(.*?)\n---\s*\n(.*)$"
    match = re.match(pattern, content, re.DOTALL)
    if match:
        raw_fm = match.group(1)
        body = match.group(2)
        return raw_fm, body
    return "", content

def build_frontmatter(name: str, description: str, trigger: str, globs: list) -> str:
    """Builds YAML frontmatter string conforming to Antigravity & Cursor standards."""
    always_apply = trigger == "always_on"
    effective_globs = globs if trigger in ("glob", "always_on") else []
    
    lines = ["---"]
    lines.append(f"name: {name}")
    if description:
        # Format description safely
        clean_desc = description.replace('"', '\\"').strip()
        lines.append(f'description: "{clean_desc}"')
    lines.append(f"trigger: {trigger}")
    lines.append(f"alwaysApply: {'true' if always_apply else 'false'}")
    
    if effective_globs:
        lines.append("globs:")
        for g in effective_globs:
            lines.append(f'  - "{g}"')
    else:
        lines.append("globs: []")
        
    lines.append("---")
    return "\n".join(lines)

def sync_rule(rule_file: Path, name: str, entry: dict) -> bool:
    """Updates the YAML frontmatter of a rule markdown file."""
    if not rule_file.exists():
        print(f"  ⚠️  Rule file not found: {rule_file}")
        return False
        
    content = rule_file.read_text(encoding="utf-8")
    _, body = parse_frontmatter(content)
    
    trigger = entry.get("trigger", "always_on")
    globs = entry.get("globs", ["*"])
    description = entry.get("description", "")
    
    new_fm = build_frontmatter(name, description, trigger, globs)
    new_content = f"{new_fm}\n\n{body.lstrip()}"
    
    if new_content != content:
        rule_file.write_text(new_content, encoding="utf-8")
        return True
    return False

def sync_skill(skill_file: Path, name: str, entry: dict) -> bool:
    """Updates the YAML frontmatter of a skill SKILL.md file."""
    if not skill_file.exists():
        print(f"  ⚠️  Skill file not found: {skill_file}")
        return False
        
    content = skill_file.read_text(encoding="utf-8")
    _, body = parse_frontmatter(content)
    
    trigger = entry.get("trigger", "always_on")
    globs = entry.get("globs", ["*"])
    description = entry.get("description", "")
    
    new_fm = build_frontmatter(name, description, trigger, globs)
    new_content = f"{new_fm}\n\n{body.lstrip()}"
    
    if new_content != content:
        skill_file.write_text(new_content, encoding="utf-8")
        return True
    return False

def sync_all(verbose=True):
    """Executes full synchronization from rules_config.json."""
    if not CONFIG_FILE.exists():
        print(f"❌ Error: Config file not found at {CONFIG_FILE}")
        return False
        
    try:
        with open(CONFIG_FILE, "r", encoding="utf-8") as f:
            config = json.load(f)
    except Exception as e:
        print(f"❌ JSON Parse Error in {CONFIG_FILE}: {e}")
        return False

    updated_count = 0
    total_count = 0

    for platform, base_dir in PLATFORM_DIRS.items():
        if platform not in config:
            continue
            
        p_config = config[platform]
        rules_dir = base_dir / "rules"
        skills_dir = base_dir / "skills"
        
        # 1. Sync Rules
        rules = p_config.get("rules", {})
        for rule_name, rule_entry in rules.items():
            rule_path = rules_dir / f"{rule_name}.md"
            total_count += 1
            if sync_rule(rule_path, rule_name, rule_entry):
                updated_count += 1
                if verbose:
                    print(f"  [UPDATED] {platform} rule: {rule_name} -> {rule_entry.get('trigger')}")
                    
        # 2. Sync Skills
        skills = p_config.get("skills", {})
        for skill_name, skill_entry in skills.items():
            skill_path = skills_dir / skill_name / "SKILL.md"
            total_count += 1
            if sync_skill(skill_path, skill_name, skill_entry):
                updated_count += 1
                if verbose:
                    print(f"  [UPDATED] {platform} skill: {skill_name} -> {skill_entry.get('trigger')}")

    if verbose:
        print(f"✅ Sync complete: {updated_count}/{total_count} files updated from rules_config.json.")
    return True

def run_watch():
    """Live watch mode: automatically syncs whenever rules_config.json is saved."""
    print(f"👀 Watching for changes in {CONFIG_FILE.name}...")
    print("   Edit any 'trigger' option ('always_on', 'manual', 'glob', 'disabled') and save.")
    print("   Press Ctrl+C to exit.\n")
    
    last_mtime = CONFIG_FILE.stat().st_mtime if CONFIG_FILE.exists() else 0
    sync_all(verbose=True)
    
    try:
        while True:
            time.sleep(1.0)
            if CONFIG_FILE.exists():
                current_mtime = CONFIG_FILE.stat().st_mtime
                if current_mtime > last_mtime:
                    last_mtime = current_mtime
                    print(f"\n🔄 Change detected in {CONFIG_FILE.name}! Applying updates...")
                    sync_all(verbose=True)
    except KeyboardInterrupt:
        print("\n👋 Stopped watching.")

def run_interactive():
    """Terminal interactive menu to toggle options."""
    if not CONFIG_FILE.exists():
        print(f"❌ Error: Config file not found at {CONFIG_FILE}")
        return
        
    with open(CONFIG_FILE, "r", encoding="utf-8") as f:
        config = json.load(f)
        
    print("\n🎛️  Rules & Skills Interactive Configurator")
    print("1) Android Rules")
    print("2) Android Skills")
    print("3) Flutter Rules")
    print("4) Flutter Skills")
    choice = input("Select category (1-4) or 'q' to quit: ").strip()
    
    platform, kind = None, None
    if choice == "1": platform, kind = "android", "rules"
    elif choice == "2": platform, kind = "android", "skills"
    elif choice == "3": platform, kind = "flutter", "rules"
    elif choice == "4": platform, kind = "flutter", "skills"
    else: return
    
    items = list(config[platform][kind].items())
    print(f"\nSelect {platform} {kind} to configure:")
    for idx, (name, data) in enumerate(items, 1):
        print(f"  {idx:2d}) {name:40s} [{data.get('trigger')}]")
        
    item_choice = input(f"\nSelect item (1-{len(items)}): ").strip()
    try:
        idx = int(item_choice) - 1
        if 0 <= idx < len(items):
            target_name, target_data = items[idx]
        else:
            return
    except ValueError:
        return
        
    print(f"\nSelected: {target_name} (Current: {target_data.get('trigger')})")
    print("Choose activation trigger:")
    print("  1) always_on  (Always loaded in context)")
    print("  2) manual     (On-demand / @mentioned only)")
    print("  3) glob       (Loaded when matching files open)")
    print("  4) disabled   (Deactivated)")
    
    trig_choice = input("Select option (1-4): ").strip()
    modes = {"1": "always_on", "2": "manual", "3": "glob", "4": "disabled"}
    new_trigger = modes.get(trig_choice)
    if not new_trigger:
        print("Invalid choice.")
        return
        
    config[platform][kind][target_name]["trigger"] = new_trigger
    with open(CONFIG_FILE, "w", encoding="utf-8") as f:
        json.dump(config, f, indent=2)
        f.write("\n")
        
    print(f"✅ Updated {target_name} -> {new_trigger}")
    sync_all(verbose=True)

if __name__ == "__main__":
    if "--watch" in sys.argv:
        run_watch()
    elif "--interactive" in sys.argv or "-i" in sys.argv:
        run_interactive()
    else:
        sync_all(verbose=True)
