package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Tokenizer extends JavaScriptObject
{
   protected Tokenizer()
   {
   }
   
   public final native Token[] getLineTokens(String line) /*-{
      return this.getLineTokens(line, "start").tokens;
   }-*/;
}
