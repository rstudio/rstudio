package org.rstudio.codemirror.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface EditorBlurHandler extends EventHandler
{
   void onEditorBlur(EditorBlurEvent e);
}
