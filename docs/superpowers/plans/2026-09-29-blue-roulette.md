# Blue Roulette Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking. The user initially selected GPT-6 Sol High and on 2026-09-30 changed implementation to the currently selected model. No commits without user permission; preserve a file ledger instead of commit-based bookkeeping. The parent coordinates one final independent review.

**Goal:** Add a reliable 50:50 decision roulette, a muted white/blue visual theme, and a tested Android v1.3.0 APK.

**Architecture:** Preserve Compose → ListViewModel → Repository → Room. Pure Kotlin roulette rules choose one result and matching angle; the ViewModel owns its session; a focused dialog renders it; a transactional repository method conditionally applies it and returns existing undo tokens.

**Tech Stack:** Existing Kotlin 2.0.20, Compose BOM 2024.06.00/Material 3, Room 2.6.1, Android API 26+, JDK 17 and existing Gradle 8.9 installation. No production dependency upgrades.

**Spec:** `docs/superpowers/specs/2026-09-29-blue-roulette-design.md`; palette and visual reference: `design/BLUE_ROULETTE_DESIGN.md`, `design/blue-roulette-mockup.html`.

## Global Constraints

- User says plan then start implementation; continue through all tasks without additional stage approval. Never commit/push/merge without permission.
- Implementation and fixes: the currently selected model, per the user’s 2026-09-30 update. Preserve completed work.
- Work in the attached isolated worktree; original checkout was clean before design documents. Do not delete existing APKs.
- Keep applicationId `com.minwoo.jangbogi`, minSdk=26, DB schema version=3. Bump versionCode to 6 and versionName to 1.3.0.
- Use existing debug-key signing arrangement for release and verify certificate against original v1.2.0 before claiming update compatibility.
- Light primary #37658C/background #F7F9FC/white cards; dark primary #A6C8E8/background #121922. Apply the designer's complete token table to all Material slots.
- Exact actions: `살래요`, `결정할래요`, `돌리기`, `확인`, `실행 취소`; roulette sectors `살래요` and `안 살래요`.
- Never reroll on recomposition/recreation/retry; never apply twice; don't swallow CancellationException; no auto-dismiss of the result before it can be read.

## Review Focus

1. Duplicate completion or rapid clicks must not write twice or delete an item restored with Undo; cover in Tasks 1–2.
2. An item edited/deleted/moved while roulette is open must not receive a stale decision; cover transactional snapshot conflict tests in Task 1.
3. Rotation, background/foreground and zero animation scale must not hang, reroll or replay a committed mutation; cover Task 2 UI tests/manual scenarios.
4. 320dp width, 200% font and a long Korean item name must leave both actions, result and close controls reachable; inspect in Task 3.
5. The release must update v1.2.0 with the same signature and preserve current items, purchase intent and plans; verify in Task 4.

## Task 1: Deterministic roulette rules and transactional mutations

**Files:** Create `domain/DecisionRoulette.kt` and `domain/DecisionSession.kt` under `app/src/main/java/com/minwoo/jangbogi`; modify `data/JangbogiRepository.kt`; add `app/src/test/java/com/minwoo/jangbogi/DecisionRouletteTest.kt` and `DecisionSessionTest.kt`, plus `app/src/androidTest/java/com/minwoo/jangbogi/DecisionRepositoryTest.kt`.

**Interfaces:** `DecisionOutcome { BUY, SKIP }`; `RouletteSpin(outcome: DecisionOutcome, targetRotation: Float)`; `DecisionRoulette.spin(random: kotlin.random.Random = Random.Default): RouletteSpin`; `DecisionRoulette.outcomeAtRotation(rotation: Float): DecisionOutcome`; `JangbogiRepository.applyDecision(expected: ShoppingItem, outcome: DecisionOutcome): ItemMutationResult`. Existing `ItemMutationResult.Applied(UndoToken)`/`Missing`/`NoChange` are sufficient; UI interprets NoChange as stale/unavailable for this call. Domain session model owns explicit ready/spinning/applying/result/error transitions (exact representation may be chosen idiomatically and documented).

- [x] Write rules/session tests first: injected deterministic Random drives true and false, choosing the outcome exactly once; all generated angles point to the chosen sector including jitter endpoints; repeated start/complete signals are rejected; error retry retains original spin; completing a dismissed ready session is ignored. Run the focused tests and observe RED before implementation.
- [x] Implement two equal semicircles and fixed top pointer geometry. Canvas local BUY sector starts at -90° and sweeps 180°, SKIP is the other half; target is five full turns + 270° for BUY or +90° for SKIP, with optional ±60° interior jitter. Test the geometry rather than statistically demanding an exact 50/50 sample.
- [x] Write Room tests: BUY preserves all fields except intent/check state; SKIP removes only expected item; both UndoTokens restore exact original once; other list remains untouched; wrong/currently changed snapshot returns NoChange; missing item returns Missing; replay does not mutate. Run test compilation/connected tests for RED as available.
- [x] Implement `applyDecision` in a single `db.withTransaction`: read expected.id, require equality and CONSIDER intent, then perform move or deletion using existing policies and emit one UndoToken. Do not call asynchronous UI code inside transaction.
- [x] Run focused JVM and repository tests to GREEN. Record commands/outcomes in `docs/superpowers/verification/2026-09-29-v1.3.0.md`; do not commit.

## Task 2: ViewModel session and mini roulette dialog

**Files:** Modify `ui/viewmodel/ListViewModel.kt`, `ui/screens/ListScreen.kt`, `ui/components/ShoppingContent.kt`; add `ui/components/DecisionRouletteDialog.kt`. Update draft `ShoppingHomeScreen.kt` call only if callback signature requires it. Add UI/session integration tests under `app/src/androidTest/java/com/minwoo/jangbogi/DecisionRouletteUiTest.kt` (test-only Compose dependencies if necessary).

**Interfaces:** `ShoppingContent(..., onDecide: (ShoppingItem) -> Unit = {}, ...)`. ViewModel exposes immutable `StateFlow<DecisionSession?>`, `openDecision(item)`, `startDecision()`, `finishDecisionAnimation(sessionId)`, `dismissDecision()` returning/handing off the successful UndoToken once. Dialog consumes state and start/animation-finished/dismiss callbacks; it does not roll or access DB. Session IDs are unique per VM.

- [x] Add failing tests for one live session, duplicate start and completion, result retained across activity recreation, successful result dismissed only once, and failed save not reported as success. Use focused pure state tests when Android classes are unnecessary; integration tests verify actual VM/repository wiring.
- [x] Wire ViewModel with frozen snapshot, spin and unique session ID. Change to applying synchronously before launching repository write; reject repeated callbacks. Catch ordinary persistence exceptions to an error state while propagating cancellation. Keep outcome and snapshot on retry. Close ready sessions with no mutation; block close while spinning/applying.
- [x] Add compact Dialog with two equal labeled sectors, top pointer, approximately 2800ms eased spin, state/result semantics and 48dp controls. Use scrollable content with max width 340dp; animations disabled still call completion; recreation uses existing spin. Show result until user dismisses; no reroll button.
- [x] Add `결정할래요` beside direct `살래요` in a second action row on considering items, retain item editing/menu/quantity, clear focus/hide keyboard on opening. Use FlowRow/wrapping or responsive layout for large fonts.
- [x] In ListScreen display the VM-owned dialog. Only after successful result is closed, dismiss any stale Snackbar and show outcome-specific action with the captured token. Undo must restore CONSIDER. Result closing/back/outside dismiss follows the same one-time path. Use accessible duration; keep existing list tab unless user changes it.
- [x] Run JVM + Android UI/repository tests. Verify both outcomes, cancel-ready, double tap, stale item, rotation during spin/result, animation scale 0, and undo. Record evidence and any limitation; do not commit.

## Task 3: Apply designer palette and inspect screens

**Files:** `ui/theme/Color.kt`; `app/src/main/res/values/themes.xml`, `values-night/themes.xml`; existing `drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`; narrowly scoped UI adjustments only if visual QA requires them.

**Interfaces:** Preserve `LightColors`, `DarkColors`, `ColorScheme.surfaceCard` and `JangbogiTheme` contracts. Use the designer's complete table, including tertiary slate-blue and all surface container/inverse slots. Icon geometry unchanged.

- [x] Apply blue palette and matching light/dark launch/window colors; recolor existing green launcher vector fills/strokes/gradient. Do not replace vectors with raster art.
- [x] Compile and inspect actual Android screens: home, considering, list switcher, item editor, household plan, roulette ready/spinning/results, launch icon. Check light/dark, 320/360dp and font_scale=2 with a long Korean name. Capture representative screenshots in `design/qa/v1.3.0-*.png`.
- [x] Fix overflow/contrast based on evidence. Main text/actions target >=4.5:1 contrast; boundaries and pointer stay distinguishable. No low-value tests that only assert hardcoded color constants. Run affected tests after behavioral fixes; record visual results.

## Task 4: Release verification and APK handoff

**Files:** `app/build.gradle.kts`; `README.md`, `INSTALL.md`; verification report; root `장보기_v1.3.0.apk`.

- [x] Set versionCode=6/versionName=1.3.0. Update install/feature documentation and latest APK link; preserve historical docs/APKs.
- [x] Run full `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease` with local JDK17 and Gradle8.9; inspect test XML and lint report since lint currently does not abort on errors. Run `:app:connectedDebugAndroidTest` on dedicated QA emulator. Separate preexisting warnings from new errors.
- [x] Install original v1.2.0 on QA emulator, create sample BUY/CONSIDER data and plans, update with release v1.3.0 using `adb install -r`, and verify preservation and roulette. Use a dedicated emulator, never clear a user's attached phone data.
- [x] Verify APK using SDK `apksigner verify --verbose --print-certs` and `aapt dump badging`; compare signer digest to v1.2.0, confirm package/min SDK/version, copy release as `장보기_v1.3.0.apk`, compute SHA256 and size.
- [x] Request parent-coordinated independent review of uncommitted changes and newly created files against spec/plan, fix important findings using the currently selected model, rerun affected validation and regenerate final release if product code changed.
- [x] Final report states actual passed checks and unavailable checks, artifact path and signature result. Deliver APK also at original user checkout for easy access. Leave changes uncommitted.

## Execution environment notes

Original root: `C:\Users\Minu\Documents\GitHub\Jangbogi`. Worktree: `C:\Users\Minu\.codex\worktrees\blue-roulette\Jangbogi`.

JDK: original root `tools\jdk17`; Gradle: original root `tools\gradle-8.9\bin\gradle.bat`; SDK: original root `tools\android-sdk`. `local.properties` in worktree points there. Wrapper pins Gradle8.4; use installed 8.9 as previous project setup did. Set JAVA_HOME per command. Outside-sandbox Gradle/adb calls need require_escalated due to user cache/AVD access. Do not change HOME/USERPROFILE or other common system variables. Store verbose logs under ignored build directories or original `tools\dl`.

Parent has started a baseline unit-test run; consult its result before repeating setup. Design-only files are prepared in original root and copied into worktree before handoff. Windows helper processes must use hidden windows. No cleanup that deletes worktrees; no user-data deletion on physical devices.

## Completion — 2026-09-30

All four tasks are complete. Final evidence and QA limits are recorded in `docs/superpowers/verification/2026-09-29-v1.3.0.md`. Product source, design files, tests, screenshots and APK are copied back to the original checkout, without commits.
