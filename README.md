# Agent Skills & Rules (Android & Flutter)

A standardized, production-ready suite of architectural rules, progressive skills, and production templates for AI coding agents (Google Antigravity / Gemini) across **Android (Kotlin + Jetpack Compose)** and **Flutter (Dart)**.

---

## Architecture Blueprint: The Three-Tier Standard

Both Android and Flutter repositories follow an identical three-tier architectural standard:

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

## Repository Structure

```text
agent_skills_and_rules/
├── install.sh                     # Automated one-line installer script
├── android_rules_skills/
│   └── .agents/
│       ├── rules/                 # 14 Android development rules (00 to 90)
│       └── skills/                # 9 Android skills with Kotlin templates
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
