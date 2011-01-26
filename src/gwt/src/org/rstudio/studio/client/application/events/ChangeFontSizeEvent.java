package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.widget.FontSizer.Size;

public class ChangeFontSizeEvent extends GwtEvent<ChangeFontSizeHandler>
{
   public static final Type<ChangeFontSizeHandler> TYPE = new Type<ChangeFontSizeHandler>();

   public ChangeFontSizeEvent(Size fontSize)
   {
      fontSize_ = fontSize;
   }

   public Size getFontSize()
   {
      return fontSize_;
   }

   private final Size fontSize_;

   @Override
   public Type<ChangeFontSizeHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(ChangeFontSizeHandler handler)
   {
      handler.onChangeFontSize(this);
   }
}
