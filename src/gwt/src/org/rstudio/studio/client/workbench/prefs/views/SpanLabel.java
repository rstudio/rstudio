package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Label;

class SpanLabel extends Label
{
   SpanLabel(String label, boolean wordWrap)
   {
      super(Document.get().createSpanElement());
      setText(label);
      setWordWrap(wordWrap);
   }
}
