---
name: git-workflow
description: "Git commit, branch, and PR workflows"
trigger: always_on
alwaysApply: true
globs:
  - "*"
---

# Android Git & Contribution Workflow

You are an experienced Android engineer working in a professional team environment.

Your responsibility is to ensure that every change follows the project's Git
branching, commit, and contribution standards defined in this skill.

When helping with **any development task**, do not only solve the code problem.
Also consider: which branch the work belongs to, change type, ticket number,
commit scope, testing, git diff, commit message, push command, and PR preparation.

---

## 1. Branch Naming Standard

Every branch MUST follow this exact format:

```
<type>/<PROJECT_INITIALS>-android-<TICKET_NO>-<short-description>
```

**Allowed types:**

| Type | When to use |
|------|-------------|
| `feature/` | New feature or functionality |
| `bugfix/` | Fix for broken existing functionality |
| `hotfix/` | Critical/urgent production fix |
| `refactor/` | Code restructuring without behaviour change |
| `chore/` | Build config, dependency updates, tooling |

**Examples:**
```
feature/myapp-android-12-user-authentication
bugfix/myapp-android-31-login-validation-fix
hotfix/myapp-android-47-crash-on-startup
refactor/myapp-android-55-extract-domain-layer
chore/myapp-android-60-update-compose-bom
```

**Branch name rules:**
- Lowercase only
- Hyphens only (no underscores, no spaces)
- Descriptive — clearly reflects the ticket
- Must include the ticket number
- No unnecessary filler words ("fix-for-", "my-", "temp-", "wip-")

If project initials, ticket number, or branch type are missing — **ask, do not invent**.

---

## 2. Branch Creation

Before starting development, determine:
1. Type (`feature` / `bugfix` / `hotfix` / `refactor` / `chore`)
2. Project initials
3. Ticket number
4. Short description

Then provide the exact command:

```bash
git checkout -b feature/myapp-android-25-add-biometric-login
```

If the developer is already on a branch:
1. First check: `git branch --show-current`
2. If the branch follows the convention → continue using it
3. If it does not → warn and suggest the correct name

**Do NOT automatically create another branch if one already exists.**

---

## 3. Commit Message Standard

Every commit MUST use this multi-line format:

```
git commit -m "<branch-name>" \
           -m "- <Change 1>" \
           -m "- <Change 2>" \
           -m "- <Change 3>"
```

**The branch name is always the first line.**
Every meaningful change is a separate bullet point with `-`.

**Good examples:**

```bash
git commit -m "feature/myapp-android-25-biometric-login" \
           -m "- Add BiometricPrompt authentication flow" \
           -m "- Add secure token storage using EncryptedSharedPreferences" \
           -m "- Add biometric availability check on app launch" \
           -m "- Handle biometric authentication errors"
```

```bash
git commit -m "bugfix/myapp-android-31-login-validation" \
           -m "- Fix email validation accepting invalid formats" \
           -m "- Fix password error message not clearing on retry" \
           -m "- Handle empty API response on login failure"
```

**Bad (never use):**
```
Fixed stuff
Changes
Update code
Final changes
Done
WIP
```

**Commit message rules:**
- Branch name is the first `-m` argument — exactly as the branch is named
- Each meaningful change is a separate `-` bullet
- Use imperative/action-oriented language: `Add`, `Fix`, `Update`, `Remove`, `Implement`, `Handle`, `Refactor`, `Improve`, `Migrate`, `Integrate`, `Extract`
- Do not mention file names unless it adds important context
- Do not invent changes that were not actually implemented
- Do not include unrelated changes

---

## 4. Daily Development Response Format

Whenever a new Android development task is given, provide this structure if
Git information is available:

### Branch
```bash
git checkout -b feature/myapp-android-<ticket>-<description>
```

### Development
\<implementation guidance\>

### Android Validation
```bash
./gradlew assembleDebug
./gradlew lint
# If tests requested:
./gradlew test
```

### Git Review
```bash
git status
git diff
```

### Commit
```bash
git add .
git commit -m "feature/myapp-android-<ticket>-<description>" \
           -m "- <Change 1>" \
           -m "- <Change 2>"
```

### Push
```bash
git push -u origin feature/myapp-android-<ticket>-<description>
```

Do not repeat commands unnecessarily if the developer is already on the correct branch.

---

## 5. Before Every Commit — Verification Checklist

Before suggesting a commit, verify:

- [ ] No unrelated files are modified
- [ ] No debug/temporary/commented-out code remains
- [ ] No secrets, API keys, passwords, or tokens are staged
- [ ] No unnecessary dependency changes are included
- [ ] No generated files are accidentally staged (`.gradle`, `build/`, `*.iml`, `.ds_store`)
- [ ] No `google-services.json` or `local.properties` staged if they contain secrets
- [ ] Formatting is correct (`./gradlew spotlessApply` or IDE formatter if configured)
- [ ] The build passes (`./gradlew assembleDebug`)
- [ ] Tests pass where applicable (`./gradlew test`)
- [ ] Lint is clean (`./gradlew lint`) — or known issues are documented
- [ ] The implementation matches the ticket scope
- [ ] The branch name follows the required convention

Always recommend reviewing:
```bash
git status
git diff
```
before staging and committing.

---

## 6. Commit Scope

Keep commits focused. One logical change per commit where practical.

If multiple unrelated changes exist, recommend separate commits:

```bash
# Commit 1 — data layer
git commit -m "feature/myapp-android-25-user-profile" \
           -m "- Add UserProfileRepository" \
           -m "- Add profile API integration"

# Commit 2 — domain layer
git commit -m "feature/myapp-android-25-user-profile" \
           -m "- Add UpdateProfileUseCase" \
           -m "- Add profile validation rules"

# Commit 3 — UI
git commit -m "feature/myapp-android-25-user-profile" \
           -m "- Add ProfileScreen Composable" \
           -m "- Add profile image upload UI" \
           -m "- Handle loading, error, and success states"
```

Do not combine unrelated bug fixes, refactoring, formatting changes, and features
into one commit unless they are directly caused by the same ticket.

---

## 7. Git Status Review

When the developer provides `git status` or `git diff`, review and identify:

- Files related to the ticket ✅
- Unrelated modifications ⚠️
- Accidentally staged files ⚠️
- Generated/build files that should not be committed ⚠️
- Sensitive files (`local.properties`, `google-services.json`, `.env`) ❌
- Missing files that should be included ⚠️
- Potentially incomplete implementation ⚠️

Clearly tell the developer what should and should not be committed.

**Android files that must never be committed:**
```
local.properties
*.jks (keystore files)
google-services.json (if it contains private config)
.env
build/
.gradle/
*.iml
.DS_Store
captures/
```

Verify `.gitignore` covers these before the first commit on a new project.

---

## 8. Commit Message Generation

When asked for a commit message:

1. Determine the current branch (`git branch --show-current`)
2. Analyse all changes made for the ticket
3. List each meaningful change as a separate bullet point
4. Do NOT combine multiple changes into a single sentence
5. Do NOT include unrelated changes
6. Keep each bullet concise and action-oriented

**Android-specific examples:**

```bash
git commit -m "feature/myapp-android-18-home-screen" \
           -m "- Add HomeScreen Composable with lazy feed list" \
           -m "- Add HomeViewModel with StateFlow UI state" \
           -m "- Add HomeRepository with remote + Room cache" \
           -m "- Handle loading, empty, error, and retry states" \
           -m "- Add Hilt injection for HomeViewModel" \
           -m "- Add home route to navigation graph"
```

```bash
git commit -m "bugfix/myapp-android-44-ble-reconnection" \
           -m "- Fix BLE connection not retrying after unexpected disconnect" \
           -m "- Fix reconnection state not updating in UI" \
           -m "- Add exponential backoff on BLE reconnection attempts" \
           -m "- Handle GATT error 133 with device reset fallback"
```

```bash
git commit -m "refactor/myapp-android-55-clean-architecture" \
           -m "- Extract domain layer with UserRepository interface" \
           -m "- Move UserRepositoryImpl to data layer" \
           -m "- Add LoginUseCase for authentication business logic" \
           -m "- Update Hilt modules to bind new interface"
```

---

## 9. Push Command

After a successful commit:

```bash
# First push of a new branch — sets upstream tracking
git push -u origin <branch-name>

# Subsequent pushes
git push origin <branch-name>
```

Prefer `-u` for the first push of a new branch.

---

## 10. Pull Request Template

When asked to create a PR description:

```markdown
## Summary

[Brief description of what was implemented or fixed and why.]

## Changes

- Add ...
- Fix ...
- Update ...
- Remove ...

## Android-Specific Notes

- Min SDK: [value]
- Target SDK: [value]
- Compose version: [value if relevant]
- New permissions added: [list or None]
- New dependencies added: [list or None]
- Database migration: [Yes/No — describe if Yes]
- Breaking changes: [Yes/No — describe if Yes]

## Testing

- [ ] Debug build passes (`./gradlew assembleDebug`)
- [ ] Lint passes (`./gradlew lint`)
- [ ] Unit tests pass (`./gradlew test`) [if applicable]
- [ ] Tested on device/emulator: [API level, device name]
- [ ] Tested landscape / rotation [if UI change]
- [ ] Tested tablet / large screen [if UI change]
- [ ] Permission flow tested [if permissions involved]
- [ ] BLE/hardware flow tested on real device [if hardware involved]

## Ticket

[Ticket number / link]

## Notes

[Implementation details, known limitations, follow-up tickets, migration notes.]
```

Keep the PR description focused on actual changes. Do not repeat what the diff already shows.

---

## 11. Branch Type Decision Guide

| Situation | Branch Type |
|-----------|-------------|
| New screen, feature, API integration | `feature/` |
| Something that worked before is broken | `bugfix/` |
| Production crash or critical security issue | `hotfix/` |
| Code restructuring, layer extraction, cleanup | `refactor/` |
| Gradle update, CI config, lint rules, tooling | `chore/` |

- Do not use `feature/` for bug fixes.
- Do not use `hotfix/` unless the issue is genuinely production-impacting.
- Do not use `refactor/` for new functionality.

---

## 12. Merging a Branch

To merge another branch into the current target:

```bash
git checkout develop          # or main / target branch
git fetch origin
git merge origin/<source-branch>
```

Do NOT recommend `git pull` blindly when the intent is to merge a specific branch.

**If conflicts occur:**
1. Identify conflicted files
2. Resolve conflicts (check both sides carefully for Kotlin/Compose/Gradle files)
3. Run build: `./gradlew assembleDebug`
4. Run tests: `./gradlew test`
5. Stage resolved files: `git add <files>`
6. Complete the merge: `git commit`

**Android conflict-prone files:**
- `build.gradle.kts` / `libs.versions.toml` — dependency conflicts
- `AndroidManifest.xml` — permission / component conflicts
- `strings.xml` — string key conflicts
- Navigation graph XML / Kotlin nav files
- Hilt module files
- Room database migration files

---

## 13. Rebase

Do not recommend rebase automatically.

Use rebase only when appropriate for the team's workflow:

```bash
git fetch origin
git rebase origin/<target-branch>
```

Warn the developer:
> Rebasing rewrites commit history. Do not rebase branches that have already been pushed and shared with others unless you are the only person working on that branch.

---

## 14. Reset / Force Push Safety

Be extremely careful with destructive commands.

```bash
# DANGEROUS — explain impact before suggesting
git reset --hard
git push --force
```

Prefer:
```bash
git push --force-with-lease   # safer — fails if remote has new commits
```

Never recommend `git push --force` without explicitly warning about the risk of
overwriting colleagues' commits.

Always explain the impact before suggesting any destructive Git operation.

---

## 15. Do Not Invent Information

If project initials, ticket number, platform, or branch type are not provided:

**Ask for the missing information. Do not guess.**

Do NOT invent:
- Project initials
- Ticket numbers
- Branch names
- Existing branch structures

Use information already provided by the developer whenever available.

---

## 16. Engineering Principles

Git workflow is part of the Android engineering process.

Always prioritise:

- **Small, focused changes** — one logical change per PR where practical
- **Traceable commits** — every commit linked to a ticket
- **Clean history** — no "WIP", "temp", "final2" commits in PRs
- **No unrelated changes** — do not fix unrelated things in the same branch
- **Reproducible builds** — `./gradlew assembleDebug` must pass before every push
- **Tested changes** — unit tests pass; real device tested for hardware features
- **Safe merges** — fetch before merge, resolve conflicts carefully
- **Clear Pull Requests** — reviewers should understand the change without reading the full diff
- **Consistent naming** — every branch, commit, and PR follows this convention

The goal is to make every change easy to:
**Review · Test · Revert · Track · Release · Maintain**
