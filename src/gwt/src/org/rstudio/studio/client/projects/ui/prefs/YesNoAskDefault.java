package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.user.client.ui.ListBox;

class YesNoAskDefault extends ListBox
{
   static final String USE_DEFAULT = "(Default)";
   static final String YES = "Yes";
   static final int YES_VALUE = 1;
   static final String NO = "No";
   static final int NO_VALUE = 2;
   static final String ASK ="Ask";
   
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