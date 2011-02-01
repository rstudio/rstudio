package org.rstudio.studio.client.workbench.model;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

public class EventBasedChangeTracker<T> implements ChangeTracker
{
   public EventBasedChangeTracker(HasValueChangeHandlers<T> source)
   {
      source_ = source;
      source.addValueChangeHandler(new ValueChangeHandler<T>()
      {
         public void onValueChange(ValueChangeEvent<T> valueChangeEvent)
         {
            changed_ = true;
         }
      });
   }

   public boolean hasChanged()
   {
      return changed_;
   }

   public void reset()
   {
      changed_ = false;
   }

   public ChangeTracker fork()
   {
      EventBasedChangeTracker<T> ebct = new EventBasedChangeTracker<T>(source_);
      ebct.changed_ = changed_;
      return ebct;
   }

   private boolean changed_ = false;
   private final HasValueChangeHandlers<T> source_;
}
