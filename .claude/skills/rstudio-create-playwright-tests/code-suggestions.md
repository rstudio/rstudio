# Code suggestions: Copilot and NES

Read this when working on tests in `tests/panes/editor/code_suggestions.test.ts`,
`tests/panes/editor/edit_suggestions.test.ts`, or anything that exercises ghost
text, inline suggestions, or next-edit suggestions.

Any test that exercises Copilot or Posit AI must gate on
`requireAiCredentials(test, provider)` (`@utils/ai-credentials`) at the top of
its `describe` block. Without it, a sandbox with no credentials for that
provider hits the feature's own timeout instead of skipping cleanly.

## Copilot ghost text

Ghost text renders as multiple DOM elements. Use `.first()` for visibility and
`.allTextContents()` to assemble the full text:

```typescript
const ghostText = page.locator('[class*=ace_ghost_text]');
await expect(ghostText.first()).toBeVisible({ timeout: 30000 });
const fullText = (await ghostText.allTextContents()).join('');

// After acceptance:
await expect(ghostText).toHaveCount(0, { timeout: 5000 });
```

Accept the suggestion with `ControlOrMeta+;`.

## NES (Next Edit Suggestions)

NES does **not** use `ace_ghost_text` and has a different UI. NES triggers
only from keyboard-driven edits; programmatic edits via
`rstudioapi::modifyRange()` do **not** trigger NES.

**Don't shortcut the trigger.** APIs like `.rs.api.showEditSuggestion` can
inject a NES widget directly, which makes the test pass mechanical
assertions but only exercises widget rendering -- not the actual trigger
flow the feature is supposed to test. Use a real keyboard-driven edit and
let NES surface organically.

| Element                            | Selector                                |
|------------------------------------|------------------------------------------|
| Suggestion highlight               | `.ace_next-edit-suggestion-highlight`    |
| Suggestion preview (read-only Ace) | `.ace_lineWidgetContainer .ace_content`  |
| Apply link                         | XPath: `//*[text()='Apply']`             |
| Discard link                       | XPath: `//*[text()='Discard']`           |

## Reaching into Ace from page.evaluate

Use `element.env.editor` to get the existing editor instance. **Never** call
`ace.edit(element)` -- it reinitializes the editor and breaks Copilot/NES.

## Scoping to the active editor tab

With multiple files open, generic selectors match all tabs. Scope to the
visible tabpanel:

```
//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//textarea[contains(@class,'ace_text-input')]
```
