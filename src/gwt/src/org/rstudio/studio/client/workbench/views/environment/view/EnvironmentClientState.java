package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.JavaScriptObject;

public class EnvironmentClientState extends JavaScriptObject
{
   protected EnvironmentClientState()
   {
   }

   public static final native EnvironmentClientState create(
           int scrollPosition) /*-{
       var options = new Object();
       options.scrollPosition = scrollPosition;
       return options ;
   }-*/;


   public final native int getScrollPosition() /*-{
       return this.scrollPosition;
   }-*/;

   public static native boolean areEqual(EnvironmentClientState a,
                                         EnvironmentClientState b) /*-{
       if (a === null ^ b === null)
           return false;
       if (a === null)
           return true;
       return a.scrollPosition === b.scrollPosition;
   }-*/;


}
