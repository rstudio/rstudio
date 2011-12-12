package org.rstudio.studio.client.common.shell;

import com.google.gwt.core.client.JavaScriptObject;

public class ShellInput extends JavaScriptObject
{
   protected ShellInput()
   {
   }
   
   public native static ShellInput create(String text, boolean echoInput) /*-{
      return {
         interrupt: false,
         text: text,
         echo_input: echoInput
      };
   }-*/;
   
   public native static ShellInput createInterrupt() /*-{
      return {
         interrupt: true,
         text: "",
         echo_input: true
      };
   }-*/;
   
   
   
   public native final boolean getInterrupt() /*-{
      return this.interrupt;
   }-*/;

   public native final String getText() /*-{
      return this.text;
   }-*/;
   
   public native final boolean getEchoInput() /*-{
      return this.echo_input;
   }-*/;
   
}
