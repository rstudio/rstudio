package org.rstudio.studio.client.workbench.prefs.events;

import com.google.gwt.event.shared.GwtEvent;

public class PreferenceChangedEvent extends GwtEvent<PreferenceChangedHandler>
{
   public static final Type<PreferenceChangedHandler> TYPE = new Type<PreferenceChangedHandler>();

   public PreferenceChangedEvent(String prefName)
   {
      prefName_ = prefName;
   }

   public String getName()
   {
      return prefName_;
   }

   @Override
   public Type<PreferenceChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(PreferenceChangedHandler handler)
   {
      handler.onPreferenceChanged(this);
   }

   private final String prefName_;
}
