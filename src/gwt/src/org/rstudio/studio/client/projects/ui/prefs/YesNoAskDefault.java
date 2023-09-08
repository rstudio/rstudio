package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.studio.client.projects.StudioClientProjectConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ListBox;

public class YesNoAskDefault extends ListBox
{
   public YesNoAskDefault(boolean includeAsk)
   {
      super();
      setMultipleSelect(false);

      String[] items = includeAsk ? new String[] { USE_DEFAULT, YES, NO, ASK }:
                                    new String[] { USE_DEFAULT, YES, NO };
      
      for (String item : items)
         addItem(item);
   }

   @Override
   public void setSelectedIndex(int value)
   {
      if (value < getItemCount())
         super.setSelectedIndex(value);
      else
         super.setSelectedIndex(0);
   }
   
   public int getValue()
   {
      return getSelectedIndex();
   }
   
   public void setValue(int value)
   {
      setSelectedIndex(value);
   }
   
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
   
   public static final String USE_DEFAULT = constants_.projectTypeDefault();
   public static final String YES         = constants_.yesLabel();
   public static final String NO          = constants_.noLabel();
   public static final String ASK         = constants_.askLabel();
   
   public static final int USE_DEFAULT_VALUE = 0;
   public static final int YES_VALUE         = 1;
   public static final int NO_VALUE          = 2;
   public static final int ASK_VALUE         = 3;
}