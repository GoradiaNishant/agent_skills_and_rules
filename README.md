# Agent Skills & Rules (Android & Flutter)

A standardized, production-ready suite of architectural rules, progressive skills, and production templates for AI coding agents (Google Antigravity / Gemini) across **Android (Kotlin + Jetpack Compose)** and **Flutter (Dart)**.

---

## Architecture Blueprint: Clean Architecture & Modern App Architecture (MAD)

Both Android and Flutter repositories follow Google's official **Modern App Architecture (MAD)** combined with **Clean Architecture principles**:

- **UI Layer (Presentation)**: Pure composables / widgets, ViewModels with Unidirectional Data Flow (`UiState`, `UiEvent`), and decoupled navigation.
- **Domain Layer (Optional)**: Business use cases, domain entities, and repository contracts with zero framework dependencies.
- **Data Layer (Single Source of Truth)**: Repositories coordinating remote APIs (Retrofit / Dio) and local persistence (Room / Drift / DataStore).

```text
├── core/
│   ├── di/                 # Dependency injection (Hilt / Injectable)
│   ├── network/            # HTTP clients (Retrofit / Dio), Interceptors, NetworkResult
│   ├── session/            # SessionManager, TokenProvider, TokenAuthenticator
│   ├── monitoring/         # AppLogger with execution timers, CrashReporter, AnalyticsTracker
│   └── errors/             # GlobalErrorHandler, failure definitions
│
├── shared/
│   ├── data/models/        # Cross-feature DTO models (UserModel, AddressModel, PaginationModel)
│   ├── domain/entities/    # Cross-feature domain entities (User, Address, PaginatedResult)
│   ├── ui/                 # Reusable design system components, buttons, text fields, theme tokens
│   └── utils/              # Common extensions, formatters, and helpers
│
└── features/
    ├── auth/               # data/, domain/, presentation/
    ├── profile/            # data/, domain/, presentation/
    ├── customers/          # data/, domain/, presentation/
    └── orders/             # data/, domain/, presentation/
```

---

## Central Rule & Skill Manager (`rules_config.json`)

All 47 rules and skills across Android and Flutter are configured and monitored from a single central file:
[`rules_config.json`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/rules_config.json).

### Interactive Dropdown in your IDE
Thanks to [`rules_config.schema.json`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/rules_config.schema.json), opening `rules_config.json` in VS Code, Antigravity IDE, Cursor, or JetBrains provides a native **dropdown menu** for the `trigger` property of every rule and skill:

- `always_on` *(Default)*: Rule/skill is always loaded in full context (`alwaysApply: true`, `globs: ["*"]`).
- `glob`: Loaded selectively when working on matching file patterns (`globs: ["**/*.kt", ...]`).
- `manual`: Skill or rule loaded on-demand only (`alwaysApply: false`).
- `disabled`: Rule or skill ignored (`alwaysApply: false`).

### Synchronizing Rules & Skills
Whenever you select an option from the dropdown or change glob patterns, run the sync engine to automatically update frontmatter across all `.md` and `SKILL.md` files:

```bash
# One-time sync
./sync_rules.sh

# Live watch mode (automatically syncs whenever you edit rules_config.json)
./sync_rules.sh --watch

# Interactive terminal wizard
./sync_rules.sh --interactive
```

---

## Repository Structure

```text
agent_skills_and_rules/
├── rules_config.json              # Central control file for all 47 rules & skills (default: always_on)
├── rules_config.schema.json       # JSON Schema enabling IDE dropdown menus for trigger options
├── sync_rules.sh                  # Runner script for syncing (one-time, --watch, --interactive)
├── sync_rules_config.py           # Core Python synchronization engine
├── install.sh                     # Automated one-line installer script
├── android_rules_skills/
│   └── .agents/
│       ├── rules/                 # 14 Android development rules (00 to 90)
│       └── skills/                # 10 Android skills with Kotlin templates
└── flutter_rules_skills/
    └── .agents/
        ├── rules/                 # 14 Flutter development rules (00 to 90)
        └── skills/                # 9 Flutter skills with Dart templates
```

---

## How to Integrate Into Every New Project

### Method 1: One-Line Terminal Command (Recommended)

When you create a new Android or Flutter project, open the terminal in that project's root directory and run:

#### For an Android Project:
```bash
curl -sSL https://raw.githubusercontent.com/Nishant-commits/agent_skills_and_rules/main/install.sh | bash -s -- android
```

#### For a Flutter Project:
```bash
curl -sSL https://raw.githubusercontent.com/Nishant-commits/agent_skills_and_rules/main/install.sh | bash -s -- flutter
```

> **Local Symlink Option (Live Updates on your Mac):**
> If you have this repository cloned locally on your machine, you can symlink the rules so edits in this repo instantly apply to your project:
> ```bash
> /Users/firdos/StudioProjects/agent_skills_and_rules/install.sh android --symlink
> /Users/firdos/StudioProjects/agent_skills_and_rules/install.sh flutter --symlink
> ```

---

### Method 2: Global Configuration (`~/.gemini/config/`)

If you want these skills active on your machine across **all projects automatically without installing anything per project**:

```bash
# To make Android rules globally available:
ln -s /Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/* ~/.gemini/config/skills/
ln -s /Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/rules/* ~/.gemini/config/rules/

# Or for Flutter:
ln -s /Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/skills/* ~/.gemini/config/skills/
ln -s /Users/firdos/StudioProjects/agent_skills_and_rules/flutter_rules_skills/.agents/rules/* ~/.gemini/config/rules/
```

---

### Method 3: Git Submodule (Recommended for Team Version Control)

If you want the `.agents/` folder committed to your project's repository so teammates and CI/CD share the rules:

```bash
# In your project root:
git submodule add https://github.com/Nishant-commits/agent_skills_and_rules.git .agents-repo

# Link the appropriate platform:
ln -s .agents-repo/android_rules_skills/.agents .agents   # For Android
# or
ln -s .agents-repo/flutter_rules_skills/.agents .agents   # For Flutter

git add .gitmodules .agents-repo .agents
git commit -m "chore: add agent skills and rules submodule"
```

---

## How to Push This Repository to GitHub

1. Create a new empty repository on GitHub named `agent_skills_and_rules` (do not initialize with README or .gitignore).
2. Run the following commands in this directory:

```bash
git remote add origin https://github.com/Nishant-commits/agent_skills_and_rules.git
git branch -M main
git push -u origin main
```
