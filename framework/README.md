# Script-Driven Android Instrumentation Framework

A data-driven Android test framework that executes UI tests defined in **YAML scripts**,
while producing **standard JUnit results** compatible with any CI/CD pipeline.

---

## Architecture

```
/sdcard/test-scripts/
  ├── login.yaml
  └── checkout.yaml
        │
        │  parsed at runtime
        ▼
  ScriptLoader  ──►  YamlScriptParser  ──►  TestSuite / TestScenario / TestStep
                                                        │
                                                        │  fed into
                                                        ▼
                                           ScriptDrivenRunner  (JUnit4 ParentRunner)
                                             │  one child per scenario
                                             ▼
                                           StepExecutor  ──►  Espresso actions
                                             │
                                             ▼
                                           JUnit RunNotifier
                                           (standard pass/fail/skip reporting)
```

---

## File Structure

```
app/src/androidTest/
├── com/framework/script/
│   ├── models/
│   │   └── TestModels.kt           # TestSuite, TestScenario, TestStep, enums
│   ├── parser/
│   │   └── YamlScriptParser.kt     # YAML → model objects
│   ├── loader/
│   │   └── ScriptLoader.kt         # reads files from external storage
│   ├── executor/
│   │   └── StepExecutor.kt         # maps steps to Espresso calls
│   ├── runner/
│   │   └── ScriptDrivenRunner.kt   # JUnit4 ParentRunner (the core)
│   └── rules/
│       └── GrantExternalStorageRule.kt
└── com/example/app/test/
    └── ScriptDrivenTest.kt         # @RunWith marker class (no logic here)
```

---

## YAML Script Schema

```yaml
name: "Suite display name"           # required
description: "Optional summary"      # optional

scenarios:
  - id: "unique_scenario_id"         # required — used for filtering
    name: "Human readable test name" # required — shown in JUnit results
    description: "Optional"          # optional

    steps:
      - action: CLICK                # see actions table below
        target: "view_id"            # resource id name (no package prefix)
        targetType: ID               # ID | TEXT | CONTENT_DESC  (default: ID)
        value: "some text"           # required for TYPE_TEXT and ASSERT_TEXT
        timeout: 5000                # ms to wait for the view (default: 5000)
        optional: false              # if true, step failure won't fail test
```

### Supported Actions

| Action             | target required | value required | Description                              |
|--------------------|-----------------|----------------|------------------------------------------|
| `CLICK`            | ✅              | ❌             | Tap the target view                      |
| `TYPE_TEXT`        | ✅              | ✅             | Clear field then type text               |
| `ASSERT_VISIBLE`   | ✅              | ❌             | Assert view is displayed on screen       |
| `ASSERT_NOT_VISIBLE`| ✅             | ❌             | Assert view is gone / does not exist     |
| `ASSERT_TEXT`      | ✅              | ✅             | Assert view displays the expected text   |
| `SCROLL_DOWN`      | ❌              | ❌             | Swipe up (scroll content downward)       |
| `SCROLL_UP`        | ❌              | ❌             | Swipe down (scroll content upward)       |
| `SCROLL_TO_VIEW`   | ✅              | ❌             | Scroll until target view is visible      |

### Target Types

| targetType     | Matches by                  | Example target value  |
|----------------|-----------------------------|-----------------------|
| `ID` (default) | `R.id.<name>` in the app    | `"btn_login"`         |
| `TEXT`         | Visible text content        | `"Sign In"`           |
| `CONTENT_DESC` | Accessibility description   | `"Close dialog"`      |

---

## Setup

### 1. build.gradle

```groovy
android {
    defaultConfig {
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.espresso:espresso-contrib:3.5.1'
    androidTestImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'org.yaml:snakeyaml:2.2'
}
```

### 2. AndroidManifest.xml (test module)

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
```

---

## Running Tests

### Push scripts and run all

```bash
adb push login.yaml    /sdcard/test-scripts/
adb push checkout.yaml /sdcard/test-scripts/

# Grant permission (Android 11+)
adb shell appops set --uid com.example.app.test MANAGE_EXTERNAL_STORAGE allow

# Run
adb shell am instrument \
  -w \
  com.example.app.test/androidx.test.runner.AndroidJUnitRunner
```

### Run a single file

```bash
adb shell am instrument \
  -w \
  -e scriptFile login.yaml \
  com.example.app.test/androidx.test.runner.AndroidJUnitRunner
```

### Run a single scenario

```bash
adb shell am instrument \
  -w \
  -e scriptFile login.yaml \
  -e scenarioId login_success \
  com.example.app.test/androidx.test.runner.AndroidJUnitRunner
```

### Use a custom script directory

```bash
adb shell am instrument \
  -w \
  -e scriptDir /sdcard/automation/regression \
  com.example.app.test/androidx.test.runner.AndroidJUnitRunner
```

---

## JUnit Output

Results appear exactly like native Kotlin tests:

```
Login Flow Suite > Successful login with valid credentials        PASSED  (3.2s)
Login Flow Suite > Error shown when submitting empty credentials  PASSED  (1.1s)
Login Flow Suite > Error shown for wrong password                 FAILED  (5.0s)
  AssertionError: Step 5/6 [ASSERT_TEXT] target='tv_error_message' value='Invalid email or password'
  ↳ 'View with id: tv_error_message' check 'with text: is "Invalid email or password"' didn't match
```

Standard JUnit XML is also generated and picked up by CI tools (Jenkins, GitHub Actions, etc.).

---

## Extending with Custom Actions

Add a new value to `StepAction`, then handle it in `StepExecutor.execute()`:

```kotlin
StepAction.LONG_CLICK -> viewFor(step).perform(longClick())
StepAction.SWIPE_LEFT -> viewFor(step).perform(swipeLeft())
```

No changes needed to the runner or parser.
