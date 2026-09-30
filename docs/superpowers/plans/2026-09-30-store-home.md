# Store Home Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans inline. The user requested immediate implementation and subsequently authorized committing the finished release on main with `feat: 장보기_v1.4.0 Release`.

**Goal:** 마트 선택 홈에서 마트를 추가/선택하고 해당 마트의 기존 장보기로 진입하며 설치 가능한 APK를 제공한다.

**Architecture:** Existing ShoppingList backs each store. HomeViewModel owns store CRUD and selection, ShoppingHomeViewModel is scoped to shopping/{listId}, and existing ListScreen keeps shopping behavior. Compose components provide store cards, a vector illustration, and a name sheet.

**Tech Stack:** Kotlin 2.0.20, Compose Material 3, Navigation 2.7.7, Room 2.6.1, JDK 17.

**Spec:** docs/superpowers/specs/2026-09-30-store-home-design.md

## Global Constraints
- com.minwoo.jangbogi, Room schema 3, minSdk 26, targetSdk 34.
- versionName 1.4.0, versionCode 7, same signing certificate.
- Commit after validation on main with `feat: 장보기_v1.4.0 Release`; no changes to user phone data.
- Existing lists become store cards without dropping data.

## Review Focus
- First launch with zero stores must offer an accessible add action.
- Cancel/blank/double submission must not create unwanted stores.
- Selecting one store must never show another store's items or typed draft.
- Recreating home, sheet, or shopping must preserve position and typed input.
- Long names, 320dp width, 200% font, dark theme and animation scale 0 remain usable.

### Task 1: Define and prove the new entry flow
- [x] Add StoreHomeFlowTest using real Room data and ActivityScenario: home shown before shopping, select two stores with distinct items, empty/blank/cancel/create, rename/delete/Undo, recreation.
- [x] Run new first-home test against old implementation; expected failure because centered home title does not exist.
- [x] Adapt DecisionAppFlowTest setup to explicitly select its store before exercising existing behavior.

### Task 2: Store CRUD and reusable UI
Files: HomeViewModel.kt, StoreCard.kt, StoreIllustration.kt, StoreNameSheet.kt, HomeScreen.kt.
Interfaces: HomeViewModel.saveStore(name, id=null), selectStore(id), operation StateFlow<StoreOperation?> + consumeOperation(result), existing deleteList/undoDeleteList; HomeScreen(vm, onOpenList, onOpenPlan, createRequested=false).
- [x] Add validation/loading/error handling and select only persisted store IDs.
- [x] Build vector storefront illustration, responsive progress cards, and keyboard-safe name sheet (store-name-input/save-store tags).
- [x] Replace list-management HomeScreen with store-selection home; preserve rename/delete confirmation and Undo.

### Task 3: Navigation and shopping context
Files: AppNavHost.kt, ShoppingHomeViewModel.kt, ShoppingHomeScreen.kt, ListScreen.kt, ShoppingContent.kt, ListSwitcherSheet.kt.
- [x] Start at home; typed route shopping/{listId}; initialize VM from route ID and persist draft state per store.
- [x] Add dedicated back-to-stores icon; switcher names reference marts and selecting replaces the current shopping destination above home.
- [x] Add forward/pop slide+fade transitions of at most 300ms; preserve plan return behavior.
- [x] Run StoreHomeFlowTest and existing DecisionAppFlowTest; expected all passing.

### Task 4: QA and APK
- [x] Run JVM suite, complete Android suite, lintDebug, assembleDebug, assembleRelease.
- [x] Capture and inspect home, empty home, create sheet, shopping, long-name/large-font/dark variants; animation scale 0 flow.
- [x] Verify previous APK overwrite install and certificate compatibility using dedicated emulator.
- [x] Copy release APK to 장보기_v1.4.0.apk; update README/INSTALL and record exact evidence.
- [x] Review final diff; commit all release changes on main using the authorized message.
