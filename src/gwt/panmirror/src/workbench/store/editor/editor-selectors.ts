import { WorkbenchState } from 'workbench/store/store';

export function editorTitle(state: WorkbenchState) {
  return state.editor.title;
}

export function editorSelection(state: WorkbenchState) {
  return state.editor.selection;
}

export function editorOutline(state: WorkbenchState) {
  return state.editor.outline;
}
