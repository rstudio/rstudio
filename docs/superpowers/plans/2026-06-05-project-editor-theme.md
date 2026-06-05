# Project-level editor theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a project override the editor (Ace) color theme, stored in `.Rproj`, defaulting to `(Default)` (use the global theme), applied live on save and on project open, with a safe fallback when the theme isn't installed.

**Architecture:** A new `EditorTheme` field in the `.Rproj` file (via the forward-compatible *sorted-fields* path) is mirrored into the `editor_theme` project preference layer. Theme application reuses the existing `userState.theme` → `AceThemes` binding: the backend `syncThemePrefs()` resolves the effective theme on project open, and the Project Options dialog resolves and applies it live on save. All three apply paths use one shared resolution rule (effective-if-installed, else global, else built-in default) and never mutate the stored project value.

**Tech Stack:** C++ (Google Test) for backend core/session; GWT/Java for the frontend; Playwright/TypeScript for e2e.

**Spec:** `docs/superpowers/specs/2026-06-05-project-editor-theme-design.md` (issue [#2350](https://github.com/rstudio/rstudio/issues/2350))

**Conventions used below:**
- C++ build (per project memory): from `build/`, run `cmake ..` (only needed after *adding* a file, because tests are GLOB-ed) then `ninja` (build everything, don't scope to a subtarget).
- C++ tests: `./rstudio-tests --scope core --filter "<Pattern>"` (or `--scope rsession`).
- GWT compile check: `cd src/gwt && ant javac`.
- Commit after each task. Branch is `feature/project-editor-theme`. Each commit triggers a roborev review — wait for it and address findings before the next task.

---

### Task 1: Store editor theme in the `.Rproj` file (core C++)

**Files:**
- Modify: `src/cpp/core/include/core/r_util/RProjectFile.hpp` (RProjectConfig struct + ctor)
- Modify: `src/cpp/core/r_util/RProjectFile.cpp` (read ~line 1088, write ~line 1414)
- Test: `src/cpp/core/r_util/RProjectFileTests.cpp` (new)

- [ ] **Step 1: Write the failing test**

Create `src/cpp/core/r_util/RProjectFileTests.cpp`:

```cpp
/*
 * RProjectFileTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/r_util/RProjectFile.hpp>

#include <shared_core/FilePath.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace r_util {
namespace {

TEST(RProjectFileTests, EditorThemeRoundTrips)
{
   FilePath projPath;
   ASSERT_FALSE(FilePath::tempFilePath(".Rproj", projPath));

   RProjectConfig config;
   config.editorTheme = "Cobalt";

   ASSERT_FALSE(writeProjectFile(projPath, RProjectBuildDefaults(), config));

   RProjectConfig readConfig;
   std::string errMsg;
   ASSERT_FALSE(readProjectFile(projPath, &readConfig, &errMsg));

   EXPECT_EQ(readConfig.editorTheme, "Cobalt");

   projPath.removeIfExists();
}

TEST(RProjectFileTests, EmptyEditorThemeOmitted)
{
   FilePath projPath;
   ASSERT_FALSE(FilePath::tempFilePath(".Rproj", projPath));

   RProjectConfig config; // editorTheme defaults to ""
   ASSERT_FALSE(writeProjectFile(projPath, RProjectBuildDefaults(), config));

   std::string contents;
   ASSERT_FALSE(readStringFromFile(projPath, &contents));
   EXPECT_EQ(contents.find("EditorTheme"), std::string::npos);

   RProjectConfig readConfig;
   std::string errMsg;
   ASSERT_FALSE(readProjectFile(projPath, &readConfig, &errMsg));
   EXPECT_EQ(readConfig.editorTheme, "");

   projPath.removeIfExists();
}

TEST(RProjectFileTests, UnknownSortedFieldPreservedAlongsideEditorTheme)
{
   FilePath projPath;
   ASSERT_FALSE(FilePath::tempFilePath(".Rproj", projPath));

   RProjectConfig config;
   config.editorTheme = "Cobalt";
   config.sortedFields["FutureUnknownField"] = "keepme";

   ASSERT_FALSE(writeProjectFile(projPath, RProjectBuildDefaults(), config));

   RProjectConfig readConfig;
   std::string errMsg;
   ASSERT_FALSE(readProjectFile(projPath, &readConfig, &errMsg));

   EXPECT_EQ(readConfig.editorTheme, "Cobalt");
   ASSERT_TRUE(readConfig.sortedFields.count("FutureUnknownField") == 1);
   EXPECT_EQ(readConfig.sortedFields["FutureUnknownField"], "keepme");

   projPath.removeIfExists();
}

} // anonymous namespace
} // namespace r_util
} // namespace core
} // namespace rstudio
```

Add the include for `readStringFromFile` near the top if not pulled in transitively:

```cpp
#include <core/FileSerializer.hpp>
```

- [ ] **Step 2: Build and run to verify it fails**

Run:
```bash
cd build && cmake .. >/dev/null && ninja
```
Expected: FAIL — compile error `'editorTheme' is not a member of 'rstudio::core::r_util::RProjectConfig'`.

- [ ] **Step 3: Add the `editorTheme` field**

In `src/cpp/core/include/core/r_util/RProjectFile.hpp`, in the `RProjectConfig` constructor initializer list, add `editorTheme()` in the sorted-fields area (replace the `// firstSortedExample(),` comment line at ~line 132):

```cpp
        projectName(),

        // new fields following the convention of being stored in the final section of the file in 
        // sorted order start here

        editorTheme(),

        // internal storage for sorted fields
        sortedFields()
```

And add the member in the sorted section (replace the `// std::string firstSortedExample;` example block at ~line 186-190):

```cpp
   std::string projectName;

   // fields living in the sorted section at end of file start here

   std::string editorTheme;

   std::map<std::string, std::string> sortedFields;
```

- [ ] **Step 4: Implement read and write of `EditorTheme`**

In `src/cpp/core/r_util/RProjectFile.cpp`, in `readProjectFile` where the sorted-field skeleton lives (~line 1088, just before `return Success();`):

```cpp
   // editor theme (sorted field; preserved by older RStudio versions)
   it = dcfFields.find("EditorTheme");
   if (it != dcfFields.end())
      pConfig->editorTheme = it->second;
   else
      pConfig->editorTheme = defaultConfig.editorTheme;
```

In `writeProjectFile`, in the sorted-fields section (~line 1414, after `auto sortedFields = config.sortedFields;` and before "add the sorted fields"):

```cpp
   if (!config.editorTheme.empty())
      sortedFields["EditorTheme"] = config.editorTheme;
   else
      sortedFields.erase("EditorTheme");
```

- [ ] **Step 5: Build and run the tests to verify they pass**

Run:
```bash
cd build && ninja && ./rstudio-tests --scope core --filter "RProjectFileTests*"
```
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add src/cpp/core/include/core/r_util/RProjectFile.hpp src/cpp/core/r_util/RProjectFile.cpp src/cpp/core/r_util/RProjectFileTests.cpp
git commit -m "Store project editor theme in .Rproj as a sorted field"
```

Then wait for the roborev review of this commit and address findings before Task 2.

---

### Task 2: Theme resolution helpers + missing-theme fallback (session C++)

**Files:**
- Modify: `src/cpp/session/modules/SessionThemes.hpp` (declare two helpers)
- Modify: `src/cpp/session/modules/SessionThemes.cpp` (implement helpers; rework `syncThemePrefs()`)
- Test: `src/cpp/session/modules/SessionThemesTests.cpp` (new)

- [ ] **Step 1: Write the failing tests**

Create `src/cpp/session/modules/SessionThemesTests.cpp`:

```cpp
/*
 * SessionThemesTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionThemes.hpp"

#include <set>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace modules {
namespace themes {
namespace {

TEST(SessionThemesTests, ChooseAppliedThemeNamePrefersEffectiveWhenInstalled)
{
   std::set<std::string> installed = { "Cobalt", "Textmate (default)" };
   EXPECT_EQ(chooseAppliedThemeName("Cobalt", "Textmate (default)", installed,
                                    "Textmate (default)"),
             "Cobalt");
}

TEST(SessionThemesTests, ChooseAppliedThemeNameFallsBackToGlobalWhenEffectiveMissing)
{
   std::set<std::string> installed = { "Textmate (default)" };
   EXPECT_EQ(chooseAppliedThemeName("Cobalt", "Textmate (default)", installed,
                                    "Textmate (default)"),
             "Textmate (default)");
}

TEST(SessionThemesTests, ChooseAppliedThemeNameFallsBackToDefaultWhenBothMissing)
{
   std::set<std::string> installed = { "Textmate (default)" };
   EXPECT_EQ(chooseAppliedThemeName("Cobalt", "AlsoMissing", installed,
                                    "Textmate (default)"),
             "Textmate (default)");
}

TEST(SessionThemesTests, ResolveGlobalThemeNameChecksLayersBeyondUser)
{
   // user layer empty, system layer set -> must return the system value
   auto readLayer = [](const std::string& layer) -> boost::optional<std::string> {
      if (layer == "system") return std::string("Ambiance");
      return boost::none;
   };
   EXPECT_EQ(resolveGlobalThemeName(readLayer), "Ambiance");
}

TEST(SessionThemesTests, ResolveGlobalThemeNamePrefersUserOverSystem)
{
   auto readLayer = [](const std::string& layer) -> boost::optional<std::string> {
      if (layer == "user") return std::string("Cobalt");
      if (layer == "system") return std::string("Ambiance");
      return boost::none;
   };
   EXPECT_EQ(resolveGlobalThemeName(readLayer), "Cobalt");
}

TEST(SessionThemesTests, ResolveGlobalThemeNameEmptyWhenNonePresent)
{
   auto readLayer = [](const std::string&) -> boost::optional<std::string> {
      return boost::none;
   };
   EXPECT_EQ(resolveGlobalThemeName(readLayer), "");
}

} // anonymous namespace
} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
```

- [ ] **Step 2: Build to verify it fails**

Run:
```bash
cd build && cmake .. >/dev/null && ninja
```
Expected: FAIL — `'chooseAppliedThemeName' was not declared` / `'resolveGlobalThemeName' was not declared`.

- [ ] **Step 3: Declare the helpers**

In `src/cpp/session/modules/SessionThemes.hpp`, add includes and declarations inside `namespace themes`:

```cpp
#include <functional>
#include <set>
#include <string>

#include <boost/optional.hpp>
```

```cpp
// Resolves the global (non-project) editor theme name by consulting the layers in
// precedence order: user, system, computed, default. Returns the first present
// value, or "" if none. readLayer is injected for testability.
std::string resolveGlobalThemeName(
   const std::function<boost::optional<std::string>(const std::string& layer)>& readLayer);

// Returns the theme name to apply: effectiveName if installed, else globalName if
// installed, else defaultName.
std::string chooseAppliedThemeName(const std::string& effectiveName,
                                   const std::string& globalName,
                                   const std::set<std::string>& availableThemes,
                                   const std::string& defaultName);
```

- [ ] **Step 4: Implement the helpers**

In `src/cpp/session/modules/SessionThemes.cpp` (anonymous namespace or themes namespace, matching the file's style; the declarations are in the `themes` namespace so define them there):

```cpp
std::string resolveGlobalThemeName(
   const std::function<boost::optional<std::string>(const std::string& layer)>& readLayer)
{
   for (const char* layer : { kUserPrefsUserLayer, kUserPrefsSystemLayer,
                              kUserPrefsComputedLayer, kUserPrefsDefaultLayer })
   {
      boost::optional<std::string> value = readLayer(layer);
      if (value && !value->empty())
         return *value;
   }
   return std::string();
}

std::string chooseAppliedThemeName(const std::string& effectiveName,
                                   const std::string& globalName,
                                   const std::set<std::string>& availableThemes,
                                   const std::string& defaultName)
{
   if (availableThemes.count(effectiveName) == 1)
      return effectiveName;
   if (availableThemes.count(globalName) == 1)
      return globalName;
   return defaultName;
}
```

Add the include for the layer-name constants near the top of the .cpp if not present:

```cpp
#include <session/prefs/UserPrefs.hpp>
```

- [ ] **Step 5: Build and run the helper tests**

Run:
```bash
cd build && ninja && ./rstudio-tests --scope rsession --filter "SessionThemesTests*"
```
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add src/cpp/session/modules/SessionThemes.hpp src/cpp/session/modules/SessionThemes.cpp src/cpp/session/modules/SessionThemesTests.cpp
git commit -m "Add editor-theme resolution helpers with missing-theme fallback"
```

Wait for roborev, address findings before continuing.

---

### Task 3: Wire the fallback into `syncThemePrefs()` (session C++)

**Files:**
- Modify: `src/cpp/session/modules/SessionThemes.cpp` (`syncThemePrefs`, ~line 632)

- [ ] **Step 1: Rework `syncThemePrefs()` to resolve + apply via the helpers**

Replace the body of `syncThemePrefs()` so it computes the applied theme by name (effective → global → default) and writes the resolved object to user state. It must **not** modify the `editor_theme` pref or the `.Rproj` value.

```cpp
Error syncThemePrefs()
{
   Error err;

   // Effective editor theme (project layer wins if set).
   std::string effectiveName = prefs::userPrefs().editorTheme();

   // Global editor theme: the effective value with the project layer excluded.
   std::string globalName = resolveGlobalThemeName(
      [](const std::string& layer) -> boost::optional<std::string> {
         boost::optional<json::Value> v =
            prefs::userPrefs().readValue(layer, kEditorTheme);
         if (v && v->isString())
            return v->getString();
         return boost::none;
      });

   // Installed themes and the built-in default name.
   ThemeMap themes = getAllThemes();
   std::set<std::string> available;
   std::map<std::string, std::tuple<std::string, std::string, bool>> byName;
   for (const auto& theme : themes)
   {
      const std::string& name = std::get<0>(theme.second);
      available.insert(name);
      byName[name] = theme.second;
   }

   std::string defaultName = "Textmate (default)";
   std::string appliedName =
      chooseAppliedThemeName(effectiveName, globalName, available, defaultName);

   // Only update user state when the applied theme differs from what is stored.
   json::Object stateTheme = prefs::userState().theme();
   auto storedName = stateTheme.find(kThemeName);
   bool needsUpdate =
      storedName == stateTheme.end() ||
      (*storedName).getValue().getString() != appliedName;

   if (needsUpdate)
   {
      auto found = byName.find(appliedName);
      if (found != byName.end())
      {
         json::Object jsonTheme;
         jsonTheme["name"] = std::get<0>(found->second);
         jsonTheme["url"] = std::get<1>(found->second);
         jsonTheme["isDark"] = std::get<2>(found->second);
         err = prefs::userState().setTheme(jsonTheme);
      }
      else
      {
         LOG_WARNING_MESSAGE("No installed theme resolved for editor_theme '" +
                             effectiveName + "' (global '" + globalName + "').");
      }
   }

   return err;
}
```

(Adjust `kThemeName`, `ThemeMap`, `getAllThemes`, `json` usage to match what already exists in the file — these are all already used by the original `syncThemePrefs`.)

- [ ] **Step 2: Build**

Run:
```bash
cd build && ninja
```
Expected: PASS (compiles).

- [ ] **Step 3: Re-run the theme tests (no regressions)**

Run:
```bash
./rstudio-tests --scope rsession --filter "SessionThemesTests*"
```
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/cpp/session/modules/SessionThemes.cpp
git commit -m "Apply effective editor theme with fallback on session start"
```

Wait for roborev, address findings.

---

### Task 4: Expose `editor_theme` through project options + project pref layer (session C++)

**Files:**
- Modify: `src/cpp/session/projects/SessionProjects.cpp` (`projectConfigJson` ~line 504; `writeProjectConfig` readObject ~line 825)
- Modify: `src/cpp/session/projects/SessionProjectContext.cpp` (`uiPrefs()` ~line 1066; `defaultConfig()`)

- [ ] **Step 1: Serialize `editor_theme` to/from JSON**

In `src/cpp/session/projects/SessionProjects.cpp`, in `projectConfigJson()` (after the markdown entries, ~line 507):

```cpp
   configJson["editor_theme"] = config.editorTheme;
```

In `writeProjectConfig()`, add `editor_theme` to the **existing markdown `readObject`
group** (~line 825-829). The frontend always sends `editor_theme` (Task 4 Step 1 adds it
to `projectConfigJson()`, and `RProjectConfig.java.getEditorTheme()` returns `""` rather
than `undefined`), so reading it in the required group is correct and avoids a spurious
`LOG_ERROR`:

```cpp
   error = json::readObject(configJson,
                            "markdown_wrap", config.markdownWrap,
                            "markdown_wrap_at_column", config.markdownWrapAtColumn,
                            "markdown_references", config.markdownReferences,
                            "markdown_canonical", config.markdownCanonical,
                            "editor_theme", config.editorTheme);
```

- [ ] **Step 2: Mirror into the project pref layer**

In `src/cpp/session/projects/SessionProjectContext.cpp`, in `ProjectContext::uiPrefs()` (near the markdown block, ~line 1071):

```cpp
   // editor theme -- only set the project value when overridden
   if (!config_.editorTheme.empty())
      uiPrefs[kEditorTheme] = config_.editorTheme;
```

Ensure `kEditorTheme` is available (it is defined in `session/prefs/UserPrefs.hpp`, already included transitively where the other `k*` pref keys are used in this file).

In `ProjectContext::defaultConfig()`, leave `editorTheme` at its `""` default (the `RProjectConfig` constructor already does this; no change unless the function sets fields explicitly — if it does, add `defaultConfig.editorTheme = "";`).

- [ ] **Step 3: Build**

Run:
```bash
cd build && ninja
```
Expected: PASS (compiles).

- [ ] **Step 4: Commit**

```bash
git add src/cpp/session/projects/SessionProjects.cpp src/cpp/session/projects/SessionProjectContext.cpp
git commit -m "Expose project editor_theme via options RPC and pref layer"
```

Wait for roborev, address findings.

---

### Task 5: Frontend config accessor (GWT)

**Files:**
- Modify: `src/gwt/src/org/rstudio/studio/client/projects/model/RProjectConfig.java`

- [ ] **Step 1: Add the `editor_theme` accessor**

Mirror the `markdownWrap` accessor pattern. Add to `RProjectConfig.java`:

```java
   public native final String getEditorTheme() /*-{
      return this.editor_theme || "";
   }-*/;

   public native final void setEditorTheme(String editorTheme) /*-{
      this.editor_theme = editorTheme;
   }-*/;
```

- [ ] **Step 2: Compile check**

Run:
```bash
cd src/gwt && ant javac
```
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/gwt/src/org/rstudio/studio/client/projects/model/RProjectConfig.java
git commit -m "Add editor_theme accessor to RProjectConfig (GWT)"
```

Wait for roborev, address findings.

---

### Task 6: i18n strings + icon resource (GWT)

**Files:**
- Modify: `src/gwt/src/org/rstudio/studio/client/projects/StudioClientProjectConstants.java`
- Modify: `src/gwt/src/org/rstudio/studio/client/projects/StudioClientProjectConstants_en.properties`
- Modify: `src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/ProjectPreferencesDialogResources.java`
- Create: `src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/iconAppearance_2x.png` (copied)

- [ ] **Step 1: Add constant methods**

In `StudioClientProjectConstants.java`, add:

```java
    String appearanceText();
    String editorThemeFormLabel();
```

- [ ] **Step 2: Add English strings**

In `StudioClientProjectConstants_en.properties`, add:

```properties
appearanceText=Appearance
editorThemeFormLabel=Editor theme:
```

- [ ] **Step 3: Add the icon resource and copy the PNG**

Copy the existing global appearance icon into the project resources bundle directory:

```bash
cp src/gwt/src/org/rstudio/studio/client/workbench/prefs/views/iconAppearance_2x.png \
   src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/iconAppearance_2x.png
```

In `ProjectPreferencesDialogResources.java`, add (next to `iconRMarkdown2x`):

```java
   @Source("iconAppearance_2x.png")
   ImageResource iconAppearance2x();
```

- [ ] **Step 4: Compile check**

Run:
```bash
cd src/gwt && ant javac
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/gwt/src/org/rstudio/studio/client/projects/StudioClientProjectConstants.java \
        src/gwt/src/org/rstudio/studio/client/projects/StudioClientProjectConstants_en.properties \
        src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/ProjectPreferencesDialogResources.java \
        src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/iconAppearance_2x.png
git commit -m "Add Appearance pane strings and icon resource (GWT)"
```

Wait for roborev, address findings.

---

### Task 7: New Project Appearance preferences pane (GWT)

**Files:**
- Create: `src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/ProjectAppearancePreferencesPane.java`

- [ ] **Step 1: Create the pane**

```java
/*
 * ProjectAppearancePreferencesPane.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class ProjectAppearancePreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectAppearancePreferencesPane(AceThemes themes)
   {
      themes_ = themes;

      addHeader(constants_.appearanceText());

      theme_ = new ListBox();
      theme_.addItem(constants_.projectTypeDefault(), DEFAULT_VALUE);

      VerticalPanel panel = new VerticalPanel();
      panel.add(new FormLabel(constants_.editorThemeFormLabel(), theme_));
      panel.add(theme_);
      add(panel);

      wrapWithPanel("project_appearance_prefs");
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(ProjectPreferencesDialogResources.INSTANCE.iconAppearance2x());
   }

   @Override
   public String getName()
   {
      return constants_.appearanceText();
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      final String storedTheme = options.getConfig().getEditorTheme();
      initialEditorTheme_ = storedTheme;

      themes_.getThemes(themeList ->
      {
         themeList_ = themeList;

         theme_.clear();
         theme_.addItem(constants_.projectTypeDefault(), DEFAULT_VALUE);

         ArrayList<String> names = new ArrayList<>(themeList.keySet());
         Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
         for (String name : names)
            theme_.addItem(name, name);

         // Preserve a stored-but-uninstalled theme so OK does not erase it.
         if (!StringUtil.isNullOrEmpty(storedTheme) && !themeList.containsKey(storedTheme))
            theme_.addItem(storedTheme, storedTheme);

         selectValue(StringUtil.isNullOrEmpty(storedTheme) ? DEFAULT_VALUE : storedTheme);
      },
      null);
   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      // If the theme list has not loaded yet, the selector only holds (Default);
      // preserve the stored value rather than erasing an existing override.
      String value = (themeList_ == null) ? initialEditorTheme_ : theme_.getSelectedValue();
      options.getConfig().setEditorTheme(value);
      return new RestartRequirement();
   }

   // Applied-theme resolution rule: effective if installed, else global, else the
   // built-in default. Returns null only when the theme list has not loaded.
   public AceTheme resolveAppliedTheme(UserPrefs uiPrefs)
   {
      if (themeList_ == null || themeList_.isEmpty())
         return null;

      AceTheme theme = themeList_.get(uiPrefs.editorTheme().getValue());
      if (theme == null)
         theme = themeList_.get(uiPrefs.editorTheme().getGlobalValue());
      if (theme == null)
         theme = themeList_.get(AceTheme.createDefault().getName());
      return theme;
   }

   private void selectValue(String value)
   {
      for (int i = 0; i < theme_.getItemCount(); i++)
      {
         if (StringUtil.equals(theme_.getValue(i), value))
         {
            theme_.setSelectedIndex(i);
            return;
         }
      }
      theme_.setSelectedIndex(0);
   }

   private static final String DEFAULT_VALUE = "";

   private String initialEditorTheme_ = "";
   private final AceThemes themes_;
   private final ListBox theme_;
   private HashMap<String, AceTheme> themeList_;
   private static final StudioClientProjectConstants constants_ =
      GWT.create(StudioClientProjectConstants.class);
}
```

(If `AceThemes.getThemes(...)` requires a `ProgressIndicator` rather than accepting `null`, pass `new ProgressIndicator()`/the global indicator used by `AppearancePreferencesPane.setThemes` — match that call site's signature exactly.)

- [ ] **Step 2: Compile check**

Run:
```bash
cd src/gwt && ant javac
```
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/ProjectAppearancePreferencesPane.java
git commit -m "Add Project Appearance preferences pane with editor theme selector"
```

Wait for roborev, address findings.

---

### Task 8: Register the pane and apply the theme on save (GWT)

**Files:**
- Modify: `src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/ProjectPreferencesDialog.java`

- [ ] **Step 1: Add the pane index and renumber**

Replace the index constants (lines 53-62) so `APPEARANCE` sits between `EDITING` and `R_MARKDOWN`:

```java
   public static final int GENERAL    = 0;
   public static final int EDITING    = 1;
   public static final int APPEARANCE = 2;
   public static final int R_MARKDOWN = 3;
   public static final int PYTHON     = 4;
   public static final int SWEAVE     = 5;
   public static final int SPELLING   = 6;
   public static final int BUILD      = 7;
   public static final int VCS        = 8;
   public static final int RENV       = 9;
   public static final int SHARING    = 10;
```

- [ ] **Step 2: Inject the pane and add it to the pane list**

Add the constructor parameter `ProjectAppearancePreferencesPane appearance` (after `editing`), store it, and insert it into the `panes(...)` call between `editing` and `rMarkdown`:

```java
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectAppearancePreferencesPane appearance,
                                   ProjectRMarkdownPreferencesPane rMarkdown,
```

```java
            panes(
                  general,
                  editing,
                  appearance,
                  rMarkdown,
                  python,
                  compilePdf,
                  spelling,
                  build,
                  source,
                  renv,
                  sharing,
                  assistant));
```

Store the needed references (after the existing assignments ~line 106):

```java
      appearance_ = appearance;
      pUserState_ = pUserState;
```

Add the fields (near the other private fields):

```java
   private final ProjectAppearancePreferencesPane appearance_;
   private Provider<UserState> pUserState_;
```

(The constructor already receives `Provider<UserState> pUserState`; it just was not stored.)

- [ ] **Step 3: Sync + live-apply the editor theme in `doSaveChanges`**

In the success callback of `doSaveChanges()` (after the markdown block, ~line 201), add:

```java
                // editor theme: drop the project override when (Default), else set it
                String projectTheme = config.getEditorTheme();
                if (StringUtil.isNullOrEmpty(projectTheme))
                   uiPrefs.editorTheme().removeProjectValue(true);
                else
                   uiPrefs.editorTheme().setProjectValue(projectTheme);

                // apply the effective theme live (handles (Default) and uninstalled
                // project themes); resolveAppliedTheme is null only if themes have not
                // loaded, in which case syncThemePrefs applies on next project open
                AceTheme appliedTheme = appearance_.resolveAppliedTheme(uiPrefs);
                if (appliedTheme != null)
                   pUserState_.get().theme().setGlobalValue(appliedTheme);
```

Add imports as needed:

```java
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;
```

- [ ] **Step 4: Compile check**

Run:
```bash
cd src/gwt && ant javac
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/gwt/src/org/rstudio/studio/client/projects/ui/prefs/ProjectPreferencesDialog.java
git commit -m "Register Appearance pane and apply project editor theme on save"
```

Wait for roborev, address findings.

---

### Task 9: Global Appearance pane companion fix (GWT)

**Files:**
- Modify: `src/gwt/src/org/rstudio/studio/client/workbench/prefs/views/AppearancePreferencesPane.java`

- [ ] **Step 1: Seed the selector from the global pref, not user state**

In `setThemes(...)` (~lines 565-581), replace the use of `userState_.theme().getGlobalValue()` for the *current selection* with the global editor-theme pref name. Replace:

```java
            AceTheme currentTheme = userState_.theme().getGlobalValue().cast();
            if (!themeList_.containsKey(currentTheme.getName()))
            {
               // ... existing not-found warning / default selection ...
            }

            theme_.setChoices(themeList_.keySet().toArray(new String[0]));
            theme_.setValue(currentTheme.getName());
            removeThemeButton_.setEnabled(!currentTheme.isDefaultTheme());
```

with:

```java
            String globalThemeName = userPrefs_.editorTheme().getGlobalValue();
            if (!themeList_.containsKey(globalThemeName))
            {
               // ... keep the existing not-found warning / default selection,
               // substituting globalThemeName for currentTheme.getName() ...
               globalThemeName = AceTheme.createDefault().getName();
            }

            theme_.setChoices(themeList_.keySet().toArray(new String[0]));
            theme_.setValue(globalThemeName);
            AceTheme globalTheme = themeList_.get(globalThemeName);
            // keep the preview in sync with the selected (global) theme -- otherwise,
            // with a project override active, the preview would still show the override
            if (globalTheme != null)
               preview_.setTheme(globalTheme.getUrl());
            removeThemeButton_.setEnabled(globalTheme != null && !globalTheme.isDefaultTheme());
```

(Preserve the existing warning-dialog logic for a missing theme; only change the *source* of the name from `userState_.theme()` to `userPrefs_.editorTheme().getGlobalValue()`.)

- [ ] **Step 2: Apply the effective theme in `onApply`, never clobbering an override**

Replace the theme block in `onApply` (~lines 760-765):

```java
      if (themeList_ != null &&
          !StringUtil.equals(theme_.getValue(), userPrefs_.editorTheme().getGlobalValue()))
      {
         userState_.theme().setGlobalValue(themeList_.get(theme_.getValue()));
         userPrefs_.editorTheme().setGlobalValue(theme_.getValue(), false);
      }
```

with:

```java
      if (themeList_ != null &&
          !StringUtil.equals(theme_.getValue(), userPrefs_.editorTheme().getGlobalValue()))
      {
         // persist the user's global editor theme
         userPrefs_.editorTheme().setGlobalValue(theme_.getValue(), false);

         // apply the *effective* theme: a project override (if active and installed)
         // must win, so changing the global theme does not replace it
         String effectiveName = userPrefs_.editorTheme().getValue();
         AceTheme applied = themeList_.get(effectiveName);
         if (applied == null)
            applied = themeList_.get(userPrefs_.editorTheme().getGlobalValue());
         if (applied == null)
            applied = themeList_.get(AceTheme.createDefault().getName());
         if (applied != null)
            userState_.theme().setGlobalValue(applied);
      }
```

- [ ] **Step 3: Compile check**

Run:
```bash
cd src/gwt && ant javac
```
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/gwt/src/org/rstudio/studio/client/workbench/prefs/views/AppearancePreferencesPane.java
git commit -m "Keep global Appearance pane from clobbering a project theme override"
```

Wait for roborev, address findings.

---

### Task 10: NEWS.md entry

**Files:**
- Modify: `NEWS.md`

- [ ] **Step 1: Add an entry under `### New`**

Add under the appropriate `### New` section:

```markdown
- ([#2350](https://github.com/rstudio/rstudio/issues/2350)): Project Options now includes an Appearance pane for setting a project-specific editor theme; leaving it at "(Default)" uses the global theme.
```

- [ ] **Step 2: Commit**

```bash
git add NEWS.md
git commit -m "Add NEWS entry for project-level editor theme (#2350)"
```

Wait for roborev, address findings.

---

### Task 11: End-to-end test (Playwright)

**Files:**
- Create: `e2e/rstudio/tests/<area>/project-editor-theme.spec.ts` (follow the layout the `rstudio-create-playwright-tests` skill prescribes)

**Before writing:** read the `rstudio-create-playwright-tests` skill for the exact helpers, fixtures, and project-fixture conventions. The bridge (`window.rstudio`) cannot easily drive arbitrary dialog widgets, so the automatable path is **project open applies the stored theme**.

- [ ] **Step 1: Write the test (project-open applies stored theme)**

The test should, using the project/automation fixtures:
1. Create a temporary project directory containing a `.Rproj` whose body includes a line `EditorTheme: Cobalt` (use a theme known to be installed by default — verify the exact name via `window.rstudio` theme listing or a default install; "Cobalt" ships by default).
2. Open it via `window.rstudio.project.open(path)` and wait for `window.rstudio.ready`.
3. Assert the active editor theme reflects Cobalt — e.g. read the injected stylesheet link:
   ```ts
   const href = await page.evaluate(() =>
     document.getElementById('rstudio-acethemes-linkelement')?.getAttribute('href') ?? '');
   expect(href.toLowerCase()).toContain('cobalt');
   ```
4. Open a second project with **no** `EditorTheme` line and assert the theme reverts to the user's global theme (capture the global theme href before opening the override project, and assert it matches after).

- [ ] **Step 2: Run the test**

Per the `rstudio-run-playwright-tests` skill (Desktop or Server). Example (Desktop):
```bash
cd e2e/rstudio && npx playwright test tests/<area>/project-editor-theme.spec.ts
```
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add e2e/rstudio/tests/<area>/project-editor-theme.spec.ts
git commit -m "Add e2e test for project-level editor theme on project open"
```

Wait for roborev, address findings.

---

## Manual verification (cannot be automated reliably)

- **Uninstalled project theme:** Put `EditorTheme: <a custom theme you then remove>` in a `.Rproj`, open the project on a machine without that theme. Confirm the editor shows the user's global theme and that editing/saving other Project Options leaves the `.Rproj` `EditorTheme` line intact.
- **Global edit while override active:** With a project that overrides the theme open, change the theme in Global Options → Appearance. Confirm the editor keeps showing the project's theme (override wins) and the user's global theme is updated (reopen a non-override project to confirm).
- **Live save:** In a project, set the Appearance pane theme to a non-default installed theme and click OK; confirm the editor re-themes immediately. Set it back to `(Default)`; confirm it reverts to the global theme.

## Pre-PR cleanup (tracked separately)

Before opening the PR: restore the `docs/superpowers` line in `.gitignore` and delete the `docs/superpowers/` directory (spec + this plan). These planning artifacts must not ship.
