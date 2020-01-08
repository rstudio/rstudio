import React from 'react';

export interface EditorActions {
  focus(): void;
  navigate(id: string): void;
}

export const EditorActionsContext = React.createContext<EditorActions>({
  focus() {
    /* */
  },
  navigate(_id: string) {
    /* */
  },
});

export function withEditorActions<P extends WithEditorActionsProps>(Component: React.ComponentType<P>) {
  return function CommandsComponent(props: Pick<P, Exclude<keyof P, keyof WithEditorActionsProps>>) {
    return (
      <EditorActionsContext.Consumer>
        {(editorActions: EditorActions) => <Component {...(props as P)} editorActions={editorActions} />}
      </EditorActionsContext.Consumer>
    );
  };
}

interface WithEditorActionsProps {
  editorActions: EditorActions;
}
