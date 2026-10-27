---
name: 90-build-optimization-shrinking
description: "Code obfuscation, tree shaking, and split-debug-info"
trigger: always_on
alwaysApply: true
globs:
  - "**/build.gradle*"
  - "pubspec.yaml"
---

# Build Optimization, Obfuscation & Shrinking Rules

@../skills/flutter-architect/references/07-security.md

## Release Build Optimization Governance

1. **Code Obfuscation & Symbol Splitting**:
   - Production release builds must enable obfuscation and split debug symbols:
     ```bash
     flutter build appbundle --release --obfuscate --split-debug-info=./build/symbols
     flutter build ipa --release --obfuscate --split-debug-info=./build/symbols
     ```
   - Archive the generated symbol files alongside the release build for deobfuscating Crashlytics reports.
2. **Android ProGuard / R8 Rules**:
   - Ensure `isMinifyEnabled = true` and `isShrinkResources = true` in `android/app/build.gradle.kts`.
   - Maintain keep rules for reflective plugins in `android/app/proguard-rules.pro`.
3. **Asset Optimization**:
   - Compress all image assets into WebP or vector SVGs (`flutter_svg`).
   - Remove unused font weights from `pubspec.yaml`.
   - Never bundle raw high-resolution photography; provide density-scaled variants (`2.0x`, `3.0x`).
