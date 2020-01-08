import { WorkbenchState } from 'workbench/store/store';

export function prefsShowOutline(state: WorkbenchState) {
  return state.prefs.showOutline;
}

export function prefsShowMarkdown(state: WorkbenchState) {
  return state.prefs.showMarkdown;
}
