/*
 * FindInFilesDialog.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LabeledTextBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;

import java.util.ArrayList;
import java.util.Arrays;

public class FindInFilesDialog extends ModalDialog<FindInFilesDialog.State>
{
   public interface Binder extends UiBinder<Widget, FindInFilesDialog>
   {
   }

   public static class State extends JavaScriptObject
   {
      public static native State createState(String query,
                                             String path,
                                             boolean regex,
                                             boolean caseSensitive,
                                             JsArrayString filePatterns,
                                             JsArrayString excludeFilePatterns) /*-{
         return {
            query: query,
            path: path,
            regex: regex,
            caseSensitive: caseSensitive,
            filePatterns: filePatterns,
            excludeFilePatterns: excludeFilePatterns,
            resultsCount: 0,
            errorCount: 0,
            replaceErrors: ""
         };
      }-*/;

      protected State() {}

      public native final String getQuery() /*-{
         return this.query;
      }-*/;

      public native final String getPath() /*-{
         return this.path;
      }-*/;

      public native final boolean isRegex() /*-{
         return this.regex;
      }-*/;

      public native final boolean isCaseSensitive() /*-{
         return this.caseSensitive;
      }-*/;

      public final String[] getFilePatterns()
      {
         return JsUtil.toStringArray(getFilePatternsNative());
      }

      private native JsArrayString getFilePatternsNative() /*-{
         return this.filePatterns;
      }-*/;

      public final String[] getExcludeFilePatterns()
      {
         return JsUtil.toStringArray(getExcludeFilePatternsNative());
      }

      private native JsArrayString getExcludeFilePatternsNative() /*-{

         if (!this.excludeFilePatterns)
            this.excludeFilePatterns = [];
         return this.excludeFilePatterns;
      }-*/;

      public native final void updateResultsCount(int count) /*-{
         this.resultsCount += count;
      }-*/;

      public native final int getResultsCount() /*-{
         return this.resultsCount;
      }-*/;

      public native final void clearResultsCount() /*-{
         this.resultsCount = 0;
      }-*/;

      public native final void updateErrorCount(int count) /*-{
         this.errorCount += count;
      }-*/;

      public native final int getErrorCount() /*-{
         return this.errorCount;
      }-*/;

      public native final void updateReplaceErrors(String errors) /*-{
         if (this.replaceErrors)
            this.replaceErrors = this.replaceErrors.concat(errors);
         else
            this.replaceErrors = errors;
      }-*/;

      public native final int getReplaceErrors() /*-{
         return this.replaceErrors;
      }-*/;
   }

   public FindInFilesDialog(OperationWithInput<State> operation)
   {
      super("Find in Files", Roles.getDialogRole(), operation);

      dirChooser_ = new DirectoryChooserTextBox("Search in:", null);
      dirChooser_.setText("");
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      labelFilePatterns_.setFor(listPresetFilePatterns_);
      labelExcludeFilePatterns_.setFor(listPresetExcludeFilePatterns_);
      setOkButtonCaption("Find");

      setExampleIdAndAriaProperties(spanPatternExample_, txtFilePattern_);
      setExampleIdAndAriaProperties(spanExcludePatternExample_, txtExcludeFilePattern_);

      listPresetFilePatterns_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            manageFilePattern();
         }
      });
      manageFilePattern();

      listPresetExcludeFilePatterns_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            manageExcludeFilePattern();
         }
      });

      txtSearchPattern_.addKeyUpHandler(new KeyUpHandler()
      {
         @Override
         public void onKeyUp(KeyUpEvent event)
         {
            updateOkButtonEnabled();
         }
      });
   }

   public void setDirectory(FileSystemItem directory)
   {
      dirChooser_.setText(directory.getPath());
   }

   private void manageFilePattern()
   {
      divCustomFilter_.getStyle().setDisplay(
            (listPresetFilePatterns_.getSelectedIndex() == 4 &&
             listPresetExcludeFilePatterns_.getSelectedIndex() != 1)
            ? Style.Display.BLOCK
            : Style.Display.NONE);
   }

   private void manageExcludeFilePattern()
   {
      divExcludeCustomFilter_.getStyle().setDisplay(
            listPresetExcludeFilePatterns_.getSelectedIndex() == 2
            ? Style.Display.BLOCK
            : Style.Display.NONE);
      if (listPresetExcludeFilePatterns_.getSelectedIndex() != 1)
         listPresetFilePatterns_.setEnabled(true);
      else
      {
         listPresetFilePatterns_.setEnabled(false);
         listPresetFilePatterns_.setSelectedIndex(0);
         manageFilePattern();
      }
   }

   @Override
   protected State collectInput()
   {
      String filePatterns =
            listPresetFilePatterns_.getValue(
                  listPresetFilePatterns_.getSelectedIndex());
      if (filePatterns == "custom")
         filePatterns = txtFilePattern_.getText();

      ArrayList<String> list = new ArrayList<String>();
      for (String pattern : filePatterns.split(","))
      {
         String trimmedPattern = pattern.trim();
         if (trimmedPattern.length() > 0)
            list.add(trimmedPattern);
      }

      String excludeFilePatterns =
         listPresetExcludeFilePatterns_.getValue(
               listPresetExcludeFilePatterns_.getSelectedIndex());
      if (StringUtil.equals(excludeFilePatterns, "custom"))
         excludeFilePatterns = txtExcludeFilePattern_.getText();

      ArrayList<String> excludeList = new ArrayList<String>();
      for (String pattern : excludeFilePatterns.split(","))
      {
         String trimmedPattern = pattern.trim();
         if (trimmedPattern.length() > 0)
            excludeList.add(trimmedPattern);
      }

      return State.createState(txtSearchPattern_.getText(),
                               getEffectivePath().getPath(),
                               checkboxRegex_.getValue(),
                               checkboxCaseSensitive_.getValue(),
                               JsUtil.toJsArrayString(list),
                               JsUtil.toJsArrayString(excludeList));
   }

   private FileSystemItem getEffectivePath()
   {
      if (StringUtil.notNull(dirChooser_.getText()).trim().length() == 0)
         return null;
      return FileSystemItem.createDir(dirChooser_.getText());
   }

   @Override
   protected boolean validate(State input)
   {
      if (StringUtil.isNullOrEmpty(input.getQuery().trim()))
      {
         // TODO: Show an error message here? Or disable Find button until there
         // is something to search for?
         return false;
      }

      if (StringUtil.isNullOrEmpty(input.getPath().trim()))
      {
         globalDisplay_.showErrorMessage(
               "Error", "You must specify a directory to search.");

         return false;
      }

      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   protected void focusInitialControl()
   {
      txtSearchPattern_.setFocus(true);
      txtSearchPattern_.selectAll();
      updateOkButtonEnabled();
   }
   
   public void setSearchPattern(String searchPattern)
   {
      txtSearchPattern_.setText(searchPattern);
   }

   public void setState(State dialogState)
   {
      if (txtSearchPattern_.getText().isEmpty())
         txtSearchPattern_.setText(dialogState.getQuery());
      checkboxCaseSensitive_.setValue(dialogState.isCaseSensitive());
      checkboxRegex_.setValue(dialogState.isRegex());
      dirChooser_.setText(dialogState.getPath());

      String filePatterns = StringUtil.join(
            Arrays.asList(dialogState.getFilePatterns()), ", ");
      if (listPresetFilePatterns_.getValue(0) == filePatterns)
         listPresetFilePatterns_.setSelectedIndex(0);
      else if (listPresetFilePatterns_.getValue(1) == filePatterns)
         listPresetFilePatterns_.setSelectedIndex(1);
      else if (listPresetFilePatterns_.getValue(2) == filePatterns)
         listPresetFilePatterns_.setSelectedIndex(2);
      else if (listPresetFilePatterns_.getValue(3) == filePatterns)
         listPresetFilePatterns_.setSelectedIndex(3);
      else
         listPresetFilePatterns_.setSelectedIndex(4);
      txtFilePattern_.setText(filePatterns);
      manageFilePattern();

      String excludeFilePatterns = StringUtil.join(
         Arrays.asList(dialogState.getExcludeFilePatterns()), ",");
      if (listPresetExcludeFilePatterns_.getValue(0) == excludeFilePatterns)
         listPresetExcludeFilePatterns_.setSelectedIndex(0);
      else if (listPresetExcludeFilePatterns_.getValue(1) == excludeFilePatterns)
         listPresetExcludeFilePatterns_.setSelectedIndex(1);
      else
         listPresetExcludeFilePatterns_.setSelectedIndex(2);
      txtExcludeFilePattern_.setText(excludeFilePatterns);
      manageExcludeFilePattern();
   }
   
   private void updateOkButtonEnabled()
   {
      enableOkButton(txtSearchPattern_.getText().trim().length() > 0);
   }

   private void setExampleIdAndAriaProperties(SpanElement span, TextBox textbox)
   {
      // give custom pattern textbox a label and extended description using the visible
      // example shown below it
      span.setId(ElementIds.getElementId(ElementIds.FIND_FILES_PATTERN_EXAMPLE));
      Roles.getTextboxRole().setAriaLabelProperty(textbox.getElement(), "Custom Filter Pattern");
      Roles.getTextboxRole().setAriaDescribedbyProperty(textbox.getElement(),
            ElementIds.getAriaElementId(ElementIds.FIND_FILES_PATTERN_EXAMPLE));
   }

   @UiField
   LabeledTextBox txtSearchPattern_;
   @UiField
   CheckBox checkboxRegex_;
   @UiField
   CheckBox checkboxCaseSensitive_;
   @UiField(provided = true)
   DirectoryChooserTextBox dirChooser_;
   @UiField
   TextBox txtFilePattern_;
   @UiField
   FormLabel labelFilePatterns_;
   @UiField
   ListBox listPresetFilePatterns_;
   @UiField
   DivElement divCustomFilter_;
   @UiField
   TextBox txtExcludeFilePattern_;
   @UiField
   FormLabel labelExcludeFilePatterns_;
   @UiField
   ListBox listPresetExcludeFilePatterns_;
   @UiField
   DivElement divExcludeCustomFilter_;
   @UiField
   SpanElement spanPatternExample_;
   @UiField
   SpanElement spanExcludePatternExample_;

   private Widget mainWidget_;
   private GlobalDisplay globalDisplay_ = RStudioGinjector.INSTANCE.getGlobalDisplay();
}
