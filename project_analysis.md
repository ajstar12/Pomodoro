# Pomodoro App — Project Analysis
**Date:** March 21, 2026
**Package:** `com.arjunsinh.pomodoro`
**Version:** 1.0.0 | Min SDK: 24 | Target SDK: 36 | Language: Kotlin

---

## Project Overview

A full-featured Pomodoro timer Android app built with Kotlin. It includes a foreground timer service, task management, session history, statistics, and a gamification system (XP + leveling). The project has been under active development since mid-March 2026 (~10 commits).

**Total source:** ~2,665 lines of Kotlin across 26 source files.

---

## Architecture

**Pattern:** MVVM (Model–View–ViewModel)
**Stack:**

| Layer | Technology |
|-------|-----------|
| UI | Activities + ViewBinding |
| State | Kotlin `StateFlow` / `LiveData` |
| Business Logic | `ViewModel` + `PomodoroForegroundService` |
| Data | Room Database + SharedPreferences |
| Async | Kotlin Coroutines |
| Build | Gradle KTS + `kapt` |

The architecture is clean and well-separated into `data`, `service`, `ui`, `utils`, and `viewmodel` packages. The overall structure is solid for a project of this scale.

---

## Bugs Found

### 🔴 Critical

**1. All active tasks increment on every focus session (not just the selected one)**
In `TaskRepository.incrementAllActiveTasksPomodoros()`, the underlying DAO query runs:
```sql
UPDATE tasks SET completed_pomodoros = completed_pomodoros + 1 ... WHERE is_completed = 0
```
This increments every single active task, regardless of which task (if any) the user selected. The service even has a `currentTaskId` field, yet it's not passed to the increment call. If a user has three in-progress tasks, all three get a pomodoro credit after every focus session.

**2. Focus streak is read but never written**
`StatsViewModel` reads `PREFS_FOCUS_STREAK` from SharedPreferences and displays it on the Statistics screen. However, there is no code anywhere in the project that writes this value — not in the service, not in any ViewModel. The streak will always show 0.

---

### 🟠 High

**3. WakeLock has a 1-hour hard ceiling**
```kotlin
wakeLock?.acquire(60 * 60 * 1000L)  // 1 hour max
```
If the service runs for more than an hour (e.g. auto-start sessions back-to-back), the wake lock expires silently. The timer will continue counting in the foreground notification but the device can sleep, causing the ticker to be delayed. The fix is to release and re-acquire the lock each session start, or use a timed approach per session.

**4. Redundant dual state update in `HomeActivity`**
`HomeActivity` subscribes to the service's `timerState: StateFlow` via service binding (the `ServiceConnection`), *and also* registers a `BroadcastReceiver` for `BROADCAST_TIMER_UPDATE`. Both paths call `viewModel.updateTimerState(...)`. Every tick fires two redundant UI updates. The broadcast path should be removed since the StateFlow binding is already reactive and more robust.

**5. Testing options left in production release**
The duration picker in `SettingsActivity` includes `"10 seconds"` and `"1 minute"` options alongside the standard 25–45 minute range. These are development/debug values that should be removed before shipping.

---

### 🟡 Medium

**6. `TimerManager.currentSessionCount` duplicates the database count**
The service maintains a DB-backed session count (via `sessionRepository.getSessionCount(...)`) and the `TimerManager` also tracks its own `currentSessionCount` internally via `completeSession()`. These can diverge (e.g. after an app restart). Only the DB count is used for the UI and daily limit check; the `TimerManager` count is never used. It and the unused `resetSessionCount()` / `shouldResetCount()` methods are dead code.

**7. DAO mixes enum binding and hardcoded string literals**
Some DAO queries pass the `SessionType` enum as a parameter (converted to string by the `Converters` class):
```kotlin
@Query("... WHERE session_type = :sessionType ...")
suspend fun getSessionCount(sessionType: Constants.SessionType, ...): Int
```
Other queries hardcode the string directly:
```kotlin
@Query("... WHERE session_type = 'FOCUS' ...")
```
This is safe today because `Constants.SessionType.FOCUS.name == "FOCUS"`, but it's fragile. If the enum name ever changes, the hardcoded queries break silently. All queries should use the parameterized form.

**8. `getFormattedTime` is duplicated**
The exact same time-formatting logic (`mm:ss`) exists in both `TimerManager` and `TimerViewModel`. It should live in one utility function.

**9. `StatsViewModel.loadStatistics()` is not reactive**
It runs once in `init` and on manual `loadStatistics()` calls. If new sessions are added while the Statistics screen is open, the display won't update. It should use `sessionDao.getAllSessions()` as a `Flow` and react to changes, similar to how `HistoryViewModel` works.

---

### 🟢 Low / Code Quality

**10. No dependency injection**
Every ViewModel and the foreground service instantiates `PomodoroDatabase.getDatabase(...)` directly. This tightly couples everything and makes unit testing nearly impossible. Hilt (or Koin) would cleanly solve this.

**11. `kapt` instead of `ksp` for Room**
The project uses the older `kapt` annotation processor. Migrating to KSP (Kotlin Symbol Processing) would significantly speed up incremental and full builds.

**12. DataStore dependency declared but never used**
`build.gradle.kts` includes `androidx.datastore:datastore-preferences:1.1.1`, but all settings are stored using `SharedPreferences`. Either the DataStore dependency should be removed, or the settings should be migrated to use it (which is the modern, recommended approach).

**13. `FOREGROUND_SERVICE_SPECIAL_USE` may cause Play Store friction**
The manifest declares `foregroundServiceType="specialUse"` with a timer sub-type. Timer apps typically qualify for `foregroundServiceType="mediaPlayback"` or a dedicated timer type, but `specialUse` requires a justification form when submitting to Google Play. This should be verified against Play policy before release.

**14. No meaningful tests**
The only test files are the default `ExampleUnitTest.kt` and `ExampleInstrumentedTest.kt` stubs. The `GamificationUtils` XP and level formulas are pure functions and would be trivial to unit test. The `TimerManager` state machine is also highly testable.

---

## Missing Features (vs. Classic Pomodoro Technique)

- **No Long Break.** The traditional technique uses a long break (15–30 min) after every 4 focus sessions. Only `FOCUS` and `SHORT_BREAK` session types exist.
- **No "sessions until long break" counter.** Because there is no long break concept, there is no cycle tracking (e.g. "3 of 4 sessions before a long break").
- **Streak is displayed but non-functional** (see Bug #2 above).

---

## What's Working Well

- The foreground service architecture with `ServiceConnection` + `StateFlow` is the right approach for keeping the timer alive.
- `RECEIVER_NOT_EXPORTED` is correctly set on the broadcast receiver — a security best practice for Android 13+.
- Room's `ForeignKey` with `onDelete = ForeignKey.SET_NULL` correctly handles task deletion without orphaning sessions.
- Edge-to-edge UI is properly implemented in all activities.
- The gamification XP formula (linear decay removed, clean arithmetic progression for level cost) is sound.
- `SharingStarted.WhileSubscribed(5000)` in `TaskViewModel` is the correct pattern for StateFlow upstream sharing.
- `START_STICKY` on the service ensures Android restarts it if killed by the OS.

---

## Priority Recommendations

| Priority | Action |
|----------|--------|
| 🔴 Fix now | Scope the task pomodoro increment to only the *selected* task, not all active tasks |
| 🔴 Fix now | Implement focus streak write logic (track last focus date + increment streak on new day) |
| 🟠 Before release | Remove the 10-second and 1-minute debug duration options |
| 🟠 Before release | Fix or replace the 1-hour WakeLock ceiling |
| 🟠 Before release | Remove the redundant BroadcastReceiver path in `HomeActivity` |
| 🟡 Soon | Add Long Break session type and 4-session cycle tracking |
| 🟡 Soon | Make `StatsViewModel` reactive to DB changes |
| 🟡 Soon | Remove or migrate to DataStore; remove unused DataStore dependency |
| 🟢 Ongoing | Add unit tests for `GamificationUtils` and `TimerManager` |
| 🟢 Ongoing | Migrate from `kapt` to `ksp` for faster builds |
| 🟢 Later | Add dependency injection (Hilt) to improve testability |
