package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.TextArea;

public class FixedTextArea extends TextArea
{
   
   public interface Style extends CssResource
   {
      String notResizable();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("FixedTextArea.css")
      Style style();
   }
   
   public FixedTextArea(int numVisibleLines, int charWidth) {
      super();
      style_ = ((Resources)GWT.create(Resources.class)).style();
      style_.ensureInjected();
      setVisibleLines(numVisibleLines);
      setCharacterWidth(charWidth);
      addStyleName(style_.notResizable());
   }
   
   private Style style_;
   
}
