# RStudio Automation: Logic Deep Dive

## Why These Patterns Work

### Console is an Ace Editor

The RStudio console is **not a simple `<textarea>` element**. It's wrapped in the Ace code editor framework.

**Structure:**
```
#rstudio_console_input (container)
  └─ .ace_text-input (hidden textarea)
```

This means:
- You can't just `page.fill()` directly into the console
- You must target `.ace_text-input` inside the container
- `page.keyboard.type()` works reliably because it simulates actual typing into the hidden textarea

### Selector Strategy: Why Stable IDs Matter

RStudio generates dynamic IDs like `gwt-uid-XXXX` on every restart. These are **unreliable**.

**Solution:** Derive stable selectors from RStudio source code:
- File: `Commands.cmd.xml` (command definitions)
- Pattern: Command IDs map to stable element IDs
- Example: `r_script_command` ID becomes `#rstudio_label_r_script_command` selector

**How to find them:**
1. Look up the command ID in `Commands.cmd.xml`
2. Apply the pattern: `#rstudio_label_{sanitized_id}`
3. Test in browser DevTools before using

### Process Reliability: Why Cleanup Matters

**Port 9222 Conflict:**
- Chrome Remote Debugging Protocol uses port 9222
- If a previous RStudio process didn't fully terminate, it still occupies port 9222
- New process can't bind to the port → "Target closed" error

**5-Second Minimum Wait:**
- RStudio needs time to initialize its UI after startup
- The CDP connection is established quickly, but the UI takes time
- Less than 5 seconds = elements not ready yet = timing failures

**Process Lifecycle:**
```
1. Kill any lingering rstudio/rstudio.exe processes
   ↓
2. Start fresh RStudio with --remote-debugging-port=9222
   ↓
3. Wait 5 seconds (UI initialization)
   ↓
4. Connect via CDP
   ↓
5. Interact with UI
   ↓
6. Gracefully quit via q()
   ↓
7. Let RStudio save state and exit cleanly
```

### Typing vs. Keyboard Shortcuts

**Keyboard shortcuts work fine via CDP** — the codebase uses them extensively (`ControlOrMeta+;`, `ControlOrMeta+s`, `Control+l`, `Control+Enter`, etc.).

The pitfalls are specific, not general:
- **`Control` vs `ControlOrMeta`** — using the wrong one causes macOS failures (see SKILL.md Rule #7 for the full table)
- **Platform-branched keys** — some Ace navigation keys differ on macOS (e.g., `Control+End` vs `Meta+ArrowDown`); use helpers like `sourceActions.goToEnd()`
- **Menu clicks vs shortcuts** — prefer GUI interactions when *testing a UI workflow* (clicking a button tests more of the real path). But for setup/teardown and navigation, shortcuts are fine and often faster.

### Graceful Shutdown

**Why `q()` instead of force termination:**

- `process.terminate()` = immediately kills RStudio = may lose state
- `q()` in R console = clean shutdown = saves workspace, history, state

**Trade-off:**
- Takes 2 extra seconds
- Ensures data consistency
- Better for automation reliability

---

## UI Structure Understanding

### File Menu Access

The toolbar File menu works reliably:
```
button[aria-label*='File']  ← Use this
```

The main menu bar File menu (`#rstudio_label_file_menu`) doesn't reliably appear in recent RStudio versions. Always use the toolbar version.

### Tab Structure

Tabs use GWT (Google Web Toolkit) classes:
```
.gwt-TabLayoutPanelTab          ← Individual tab
.gwt-TabLayoutPanelTab-selected ← Currently active tab
.dirtyTab                        ← Unsaved changes indicator
.docTabLabel                     ← Tab label text
```

This allows you to:
1. Click tabs to switch files
2. Detect which tab is active
3. Check if file has unsaved changes
4. Find tabs by text content (e.g., "Untitled")

---

## RStudio Source Code References

Key files for understanding selectors and structure:

- **Menu definitions**: `src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml`
- **Menu rendering**: `src/gwt/src/org/rstudio/core/client/command/AppMenuBar.java`
- **ID generation**: `src/gwt/src/org/rstudio/core/client/ElementIds.java`
- **Console component**: `src/gwt/src/org/rstudio/studio/client/workbench/views/console/ConsolePane.java`
- **Tab structure**: `src/gwt/src/org/rstudio/core/client/theme/DocTabLayoutPanel.java`

When selectors break or you need new ones, check these files first.

---

## Timing Philosophy

**Fixed waits are suboptimal but reliable.**

Ideally, we'd poll for readiness (e.g., "wait until console is ready"). Instead, we use fixed waits:

```typescript
await new Promise(resolve => setTimeout(resolve, 5000));  // Always wait 5 seconds, even if ready sooner
```

**Why?**
- Polling adds complexity
- Fixed waits are predictable and reliable
- Once RStudio is ready, the script continues immediately after the wait
- The script doesn't know if it's waiting 0.1s or 4.9s—it just waits the full time

**Future optimization:** Implement readiness polling (check for specific DOM elements, API responses, etc.) but this would add complexity.

---

## Common Patterns and Their Purpose

### Pattern 1: Console Test
**Purpose:** Interactive R commands in the console
**Use case:** Testing R commands, exploring data, quick calculations

### Pattern 2: File Creation and Edit
**Purpose:** Create new R scripts and write code to them
**Use case:** Automation that generates R scripts, testing RStudio's file handling

### Pattern 3: Startup/Shutdown
**Purpose:** Test RStudio initialization and graceful exit
**Use case:** Integration tests, ensuring RStudio state management works

All three share the same core process management and cleanup logic.

