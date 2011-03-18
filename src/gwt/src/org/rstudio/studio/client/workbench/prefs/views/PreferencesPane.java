package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;

import java.util.ArrayList;

public abstract class PreferencesPane extends VerticalPanel
   implements HasEnsureVisibleHandlers
{
   public abstract ImageResource getIcon();

   public boolean validate()
   {
      return true;
   }

   public void onApply()
   {
      for (Command cmd : onApplyCommands_)
         cmd.execute();
   }
   
   public abstract String getName();

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void registerEnsureVisibleHandler(HasEnsureVisibleHandlers widget)
   {
      widget.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            fireEvent(new EnsureVisibleEvent());
         }
      });
   }

   protected CheckBox checkboxPref(String label,
                                   final HasValue<Boolean> prefValue)
   {
      final CheckBox checkBox = new CheckBox(label, false);
      checkBox.setValue(prefValue.getValue());
      onApplyCommands_.add(new Command()
      {
         public void execute()
         {
            prefValue.setValue(checkBox.getValue());
         }
      });
      return checkBox;
   }

   protected Widget indent(Widget widget)
   {
      widget.addStyleName(res_.styles().indent());
      return widget;
   }

   protected Widget tight(Widget widget)
   {
      widget.addStyleName(res_.styles().tight());
      return widget;
   }

   protected Widget spaced(Widget widget)
   {
      widget.addStyleName(res_.styles().spaced());
      return widget;
   }

   protected NumericValueWidget numericPref(String label,
                                            final HasValue<Integer> prefValue)
   {
      final NumericValueWidget widget = new NumericValueWidget(label);
      registerEnsureVisibleHandler(widget);
      widget.setValue(prefValue.getValue() + "");
      onApplyCommands_.add(new Command()
      {
         public void execute()
         {
            try
            {
               prefValue.setValue(Integer.parseInt(widget.getValue()));
            }
            catch (Exception e)
            {
               // It's OK for this to be invalid if we got past validation--
               // that means the associated checkbox wasn't checked
            }
         }
      });
      return widget;
   }

   protected final ArrayList<Command> onApplyCommands_ = new ArrayList<Command>();
   private final PreferencesDialogResources res_ =
         GWT.create(PreferencesDialogResources.class);
}
