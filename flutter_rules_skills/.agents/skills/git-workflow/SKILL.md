---
name: flutter-git-workflow
description: >-
  Git branching, commit, and contribution workflow for Flutter/Dart projects.
  Use this skill when the user asks to create a branch, write a commit message,
  push changes, create a Pull Request, review a git diff or status, merge a branch,
  or when starting any new development task. This skill enforces consistent
  branch naming, commit format, PR structure, and engineering Git discipline.
---

# Flutter Git & Contribution Workflow

You are an experienced Flutter engineer working in a professional team environment.

Your responsibility is to ensure that every change follows the project's Git
branching, commit, and contribution standards defined in this skill.

When helping with **any development task**, do not only solve the code problem.
Also consider: which branch the work belongs to, change type, ticket number,
commit scope, testing (`flutter test`), git diff, commit message, push command, and PR preparation.

---

## 1. Branch Naming Standard

Every branch MUST follow this exact format:

```
<type>/<PROJECT_INITIALS>-flutter-<TICKET_NO>-<short-description>
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
feature/myapp-flutter-12-user-authentication
bugfix/myapp-flutter-31-login-validation-fix
hotfix/myapp-flutter-47-crash-on-startup
refactor/myapp-flutter-55-extract-domain-layer
chore/myapp-flutter-60-update-flutter-dependencies
```

**Branch name rules:**
- Lowercase only
- Hyphens only (no underscores, no spaces)
- Descriptive — clearly reflects the ticket
- Must include the ticket number
- No unnecessary filler words ("fix-for-", "my-", "temp-", "wip-")

If project initials, ticket number, or branch type are missing — **ask, do not invent**.

---

## 2. Commit Message Standard

Every commit message MUST follow this exact format:

```
<type>(<scope>): <TICKET_NO> <short description in present tense>

[optional body: explain WHAT changed and WHY, not HOW]

[optional footer: breaking changes, co-authors, issue references]
```

**Commit types:**

| Type | When to use |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `refactor` | Code restructuring — no behavior change |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests |
| `chore` | Build runner, tooling, pubspec updates |
| `docs` | Documentation only changes |
| `style` | Formatting, missing semi-colons (no code change) |

**Examples:**
```
feat(auth): FLUTTER-12 add phone number verification screen

Implemented phone number OTP input widget using PinCodeTextField.
Integrated SMS autofill service with error fallback handling.
```

---

## 3. Pre-Commit Verification Workflow

Before staging or committing any code, run:

1. **Format Code**: `dart format .`
2. **Static Analysis**: `flutter analyze`
3. **Run Tests**: `flutter test`
4. **Code Generation** (if Freezed/Injectable models changed): `dart run build_runner build --delete-conflicting-outputs`
5. **Review Git Diff**: `git diff` to ensure no accidental changes or secrets staged.
