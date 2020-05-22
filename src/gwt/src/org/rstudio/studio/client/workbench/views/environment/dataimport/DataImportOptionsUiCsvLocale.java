/*
 * DataImportOptionsUiCsvLocale.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataImportOptionsUiCsvLocale extends ModalDialog<DataImportOptionsCsvLocale>
{
   interface Binder extends UiBinder<Widget, DataImportOptionsUiCsvLocale> {}
   
   public interface DataImportOptionsUiCsvLocaleStyle extends CssResource
   {
      String dialog();
   }

   @Inject
   private void initialize(GlobalDisplay globalDisplay)
   {
      globalDisplay_ = globalDisplay;
   }
   
   public DataImportOptionsUiCsvLocale(
      OperationWithInput<DataImportOptionsCsvLocale> operation,
      DataImportOptionsCsvLocale locale)
   {
      super("Configure Locale", Roles.getDialogRole(), operation);
      widget_ = GWT.<Binder> create(Binder.class).createAndBindUi(this);
      initialLocale_ = locale;

      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   private void assignLocale(DataImportOptionsCsvLocale locale)
   {
      if (locale == null) return;
      
      int fixedEncodings = encoding_.getItemCount();
      for (int idxEncoding = 0; idxEncoding < fixedEncodings; idxEncoding ++) {
         if (encoding_.getValue(idxEncoding) == locale.getEncoding()) {
            encoding_.setSelectedIndex(idxEncoding);
            break;
         }
         
         if (idxEncoding == fixedEncodings - 1) {
            addOtherEncodingItem(idxEncoding, locale.getEncoding());
         }
      }
      
      dateName_.setText(locale.getDateName());
      dateFormat_.setText(locale.getDateFormat());
      timeFormat_.setText(locale.getTimeFormat());
      decimalMark_.setText(locale.getDecimalMark());
      groupingMark_.setText(locale.getGroupingMark());
      timeZone_.setText(locale.getTZ());
      asciify_.setValue(locale.getAsciify());
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();

      initializeDefaults();
      initializeEvents();
      
      assignLocale(initialLocale_);

      setOkButtonCaption("Configure");

      HelpLink helpLink = new HelpLink(
         "Locales in readr",
         "readr_locales",
         false);
      addLeftWidget(helpLink);   
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }
   
   @Override
   protected DataImportOptionsCsvLocale collectInput()
   {
      return DataImportOptionsCsvLocale.createLocale(
        dateName_.getText(),
        dateFormat_.getText(),
        timeFormat_.getText(),
        decimalMark_.getText(),
        groupingMark_.getText(),
        timeZone_.getText(),
        encoding_.getSelectedValue(),
        asciify_.getValue()
      );
   }

   private void initializeDefaults()
   {
      // load .rs.iconvcommon()
      encoding_.addItem("ASCII", "ASCII");
      encoding_.addItem("UTF-8", "UTF-8");
      encoding_.addItem("ISO-8859-1", "ISO-8859-1");
      encoding_.addItem("WINDOWS-1252", "WINDOWS-1252");
      encoding_.addItem("SHIFT-JIS", "SHIFT-JIS");
      encoding_.addItem("ISO-2022-JP", "ISO-2022-JP");
      encoding_.addItem("BIG5", "BIG5");
      encoding_.addItem("ISO-2022-KR", "ISO-2022-KR");
      encoding_.addItem("ISO-8859-7", "ISO-8859-7");
      encoding_.addItem("GB2312", "GB2312");
      encoding_.addItem("GB18030", "GB18030");
      encoding_.addItem("ISO-8859-2", "ISO-8859-2");

      encoding_.addItem(otherLabel, "");

      encoding_.setSelectedIndex(1);
   }
   
   private void addOtherEncodingItem(int selectedIndex, String otherEncoding) {
      encoding_.insertItem(otherEncoding, otherEncoding, selectedIndex);
      encoding_.setSelectedIndex(selectedIndex);
   }

   private void initializeEvents()
   {
      ChangeHandler encodingChangeHandler = new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent arg0)
         {
            if (encoding_.getSelectedItemText() == otherLabel)
            {
               globalDisplay_.promptForText(
                  "Encoding Identifier",
                  "Please enter an encoding identifier. For a list of valid encodings run iconvlist().",
                  encoding_.getSelectedValue(),
                  new OperationWithInput<String>()
                  {
                     @Override
                     public void execute(final String otherEncoding)
                     {
                        for (int idxEncoding = 0; idxEncoding < encoding_.getItemCount(); idxEncoding++) {
                           if (encoding_.getValue(idxEncoding) == otherEncoding) {
                              encoding_.setSelectedIndex(idxEncoding);
                              return;
                           }
                        }

                        int selectedIndex = encoding_.getSelectedIndex();
                        addOtherEncodingItem(selectedIndex - 1, otherEncoding);
                     }
                  }
               );
            }
         }
      };

      encoding_.addChangeHandler(encodingChangeHandler);
   }
   
   @UiField
   TextBox dateName_;

   @UiField
   ListBox encoding_;
   
   @UiField
   TextBox dateFormat_;
   
   @UiField
   TextBox timeFormat_;
   
   @UiField
   TextBox decimalMark_;
   
   @UiField
   TextBox groupingMark_;
   
   @UiField
   TextBox timeZone_;
   
   @UiField
   CheckBox asciify_;
   
   private Widget widget_;
   private DataImportOptionsCsvLocale initialLocale_;
   private GlobalDisplay globalDisplay_;

   private final String otherLabel = "Other...";
}
