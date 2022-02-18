package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.user.client.ui.ListBox;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;

class YesNoAskDefault extends ListBox
{
   private static final StudioClientProjectConstants constants_ = com.google.gwt.core.client.GWT.create(StudioClientProjectConstants.class);
   static final String USE_DEFAULT = constants_.projectTypeDefault();
   static final String YES = constants_.yesLabel();
   static final int YES_VALUE = 1;
   static final String NO = constants_.noLabel();
   static final int NO_VALUE = 2;
   static final String ASK =constants_.askLabel();
   
   public YesNoAskDefault(boolean includeAsk)
   {
      super();
      setMultipleSelect(false);

      String[] items = includeAsk ? new String[] {USE_DEFAULT, YES, NO, ASK}:
                                    new String[] {USE_DEFAULT, YES, NO};

      for (int i=0; i<items.length; i++)
         addItem(items[i]);
   }

   @Override
   public void setSelectedIndex(int value)
   {
      if (value < getItemCount())
         super.setSelectedIndex(value);
      else
         super.setSelectedIndex(0);
   }
}