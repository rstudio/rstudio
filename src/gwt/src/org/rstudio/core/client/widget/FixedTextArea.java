package org.rstudio.core.client.widget;

import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.user.client.ui.TextArea;

public class FixedTextArea extends TextArea
{
   
   public FixedTextArea(int numVisibleLines, int charWidth) {
      super();
      setVisibleLines(numVisibleLines);
      setCharacterWidth(charWidth);
      addStyleName(ThemeStyles.INSTANCE.notResizable());
   }
   
}
