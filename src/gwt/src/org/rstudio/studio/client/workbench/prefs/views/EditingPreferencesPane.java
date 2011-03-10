package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import java.util.ArrayList;

public class EditingPreferencesPane extends PreferencesPane
{
   @Inject
   public EditingPreferencesPane(UIPrefs prefs, PreferencesDialogResources res)
   {
      prefs_ = prefs;
      res_ = res;

      add(first(checkboxPref("Highlight selected line", prefs_.highlightSelectedLine())));
      add(checkboxPref("Show line numbers", prefs_.showLineNumbers()));
      add(spacesForTab_ = checkboxPref("Insert spaces for tab", prefs_.useSpacesForTab()));
      add(indent(tabWidth_ = numericPref("Tab width", prefs_.numSpacesForTab())));
      add(showMargin_ = checkboxPref("Show margin", prefs_.showMargin()));
      add(indent(marginCol_ = numericPref("Margin column", prefs_.printMarginColumn())));
//      add(checkboxPref("Automatically insert matching parens/quotes", prefs_.insertMatching()));
//      add(checkboxPref("Soft-wrap R files", prefs_.softWrapRFiles()));
   }

   private Widget first(Widget widget)
   {
      widget.addStyleName(res_.styles().first());
      return widget;
   }

   private Widget indent(Widget widget)
   {
      widget.addStyleName(res_.styles().indent());
      return widget;
   }

   private NumericValueWidget numericPref(String label,
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

   

   @Override
   public ImageResource getIcon()
   {
      return res_.iconEdit();
   }

   @Override
   public boolean validate()
   {
      return (!spacesForTab_.getValue() || tabWidth_.validate("Tab width")) &&
             (!showMargin_.getValue() || marginCol_.validate("Margin column"));
   }

   @Override
   public void onApply()
   {
      for (Command cmd : onApplyCommands_)
         cmd.execute();
   }

   @Override
   public String getName()
   {
      return "Editing";
   }

   private final UIPrefs prefs_;
   private final PreferencesDialogResources res_;
   private final ArrayList<Command> onApplyCommands_ = new ArrayList<Command>();
   private final NumericValueWidget tabWidth_;
   private final NumericValueWidget marginCol_;
   private CheckBox spacesForTab_;
   private CheckBox showMargin_;
}
