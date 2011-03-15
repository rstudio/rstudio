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
      res_ = res;

      add(checkboxPref("Highlight selected line", prefs.highlightSelectedLine()));
      add(checkboxPref("Show line numbers", prefs.showLineNumbers()));
      add(tight(spacesForTab_ = checkboxPref("Insert spaces for tab", prefs.useSpacesForTab())));
      add(indent(tabWidth_ = numericPref("Tab width", prefs.numSpacesForTab())));
      add(tight(showMargin_ = checkboxPref("Show margin", prefs.showMargin())));
      add(indent(marginCol_ = numericPref("Margin column", prefs.printMarginColumn())));
//      add(checkboxPref("Automatically insert matching parens/quotes", prefs_.insertMatching()));
//      add(checkboxPref("Soft-wrap R files", prefs_.softWrapRFiles()));
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
   public String getName()
   {
      return "Editing";
   }

   private final PreferencesDialogResources res_;
   private final NumericValueWidget tabWidth_;
   private final NumericValueWidget marginCol_;
   private CheckBox spacesForTab_;
   private CheckBox showMargin_;
}
