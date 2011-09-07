package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AskPassEvent extends GwtEvent<AskPassEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAskPass(AskPassEvent e);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final String getHandle() /*-{
         return this.handle;
      }-*/;

      public native final String getPrompt() /*-{
         return this.prompt;
      }-*/;
   }

   public AskPassEvent(String handle, String prompt)
   {
      handle_ = handle;
      prompt_ = prompt;
   }

   public String getHandle()
   {
      return handle_;
   }

   public String getPrompt()
   {
      return prompt_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAskPass(this);
   }

   private final String handle_;
   private final String prompt_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
