package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Search extends JavaScriptObject
{
   public static native Search create(String needle,
                                      boolean backwards,
                                      boolean wrap,
                                      boolean caseSensitive,
                                      boolean wholeWord,
                                      boolean selectionOnly,
                                      boolean regexpMode) /*-{
      var Search = $wnd.require('ace/search').Search;
      return new Search().set({
         needle: needle,
         backwards: backwards,
         wrap: wrap,
         caseSensitive: caseSensitive,
         wholeWord: wholeWord,
         scope: selectionOnly ? Search.SELECTION : Search.ALL,
         regExp: regexpMode
      })
   }-*/;

   protected Search() {}

   public final native Range find(EditSession session) /*-{
      return this.find(session);
   }-*/;
}
