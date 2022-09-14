/*
 * FindInFilesDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.FormListBox;
import org.rstudio.core.client.widget.LabeledTextBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.output.OutputConstants;

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
                                             boolean wholeWord,
                                             JsArrayString filePatterns,
                                             boolean useGitGrep, 
                                             boolean excludeGitIgnore,
                                             JsArrayString excludeFilePatterns) /*-{
         return {
            query: query,
            path: path,
            regex: regex,
            caseSensitive: caseSensitive,
            wholeWord: wholeWord,
            filePatterns: filePatterns,
            useGitGrep: useGitGrep,
            excludeGitIgnore: excludeGitIgnore,
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

      public native final boolean isWholeWord() /*-{
          return this.wholeWord;
      }-*/;

      public final String[] getFilePatterns()
      {
         return JsUtil.toStringArray(getFilePatternsNative());
      }

      private native JsArrayString getFilePatternsNative() /*-{
         return this.filePatterns;
      }-*/;

      public native final boolean getExcludeGitIgnore() /*-{
         return this.excludeGitIgnore;
      }-*/;

      public native final boolean getUseGitGrep() /*-{
         return this.useGitGrep;
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

   public enum Include
   {
      AllFiles,
      CommonRSourceFiles,
      RScripts,
      PackageSource,
      PackageTests,
      CustomFilter
   }

   public FindInFilesDialog(OperationWithInput<State> operation)
   {
      super(constants_.findInFilesCaption(), Roles.getDialogRole(), operation);

      dirChooser_ = new DirectoryChooserTextBox(constants_.searchInLabel(),
         ElementIds.TextBoxButtonId.FIND_IN,
         null);
      dirChooser_.setText("");
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      labelFilePatterns_.setFor(listPresetFilePatterns_);
      setOkButtonCaption(constants_.findButtonCaption());

      setExampleIdAndAriaProperties(spanPatternExample_, txtFilePattern_);
      setExampleIdAndAriaProperties(spanExcludePatternExample_, txtExcludeFilePattern_);

      checkboxRegex_.addValueChangeHandler(event ->
      {
         // Disable "Whole Word" checkbox when regex is selected
         if (event.getValue())
            checkboxWholeWord_.setValue(false);
      });

      checkboxWholeWord_.addValueChangeHandler(event ->
      {
         if (event.getValue())
            checkboxRegex_.setValue(false);
      });

      listPresetFilePatterns_.addChangeHandler(event ->
      {
         manageFilePattern();
      });       
      manageFilePattern();

      checkboxExcludeCustom_.addValueChangeHandler(event ->
      {
         manageExcludeFilePattern();
      });

      txtSearchPattern_.addKeyUpHandler(event -> 
      {
         updateOkButtonEnabled();
      });
   }

   public String getDirectory()
   {
      return dirChooser_.getText();
   }

   public void setDirectory(FileSystemItem directory)
   {
      dirChooser_.setText(directory.getPath());
   }

   public DirectoryChooserTextBox getDirectoryChooser()
   {
      return dirChooser_;
   }

   public void setGitStatus(boolean status)
   {
      gitStatus_ = status;
      manageExcludeFilePattern();
   }

   public void setPackageStatus(boolean status)
   {
      packageStatus_ = status;
      manageFilePattern();
   }

   private void manageFilePattern()
   {
      // disable custom filter text box when 'Custom Filter' is not selected
      showDivIf(divCustomFilter_, listPresetFilePatterns_.getSelectedIndex() == Include.CustomFilter.ordinal());
      
      // disable 'Package' option when chosen directory is not a package
      if (!packageStatus_)
      {
         ((Element) listPresetFilePatterns_.getElement().getChild(
               Include.PackageSource.ordinal()))
            .setAttribute("disabled", "disabled");
         ((Element) listPresetFilePatterns_.getElement().getChild(
               Include.PackageTests.ordinal()))
            .setAttribute("disabled", "disabled");

         if (listPresetFilePatterns_.getSelectedIndex() ==
             Include.PackageSource.ordinal() ||
             listPresetFilePatterns_.getSelectedIndex() ==
             Include.PackageTests.ordinal())
            listPresetFilePatterns_.setSelectedIndex(Include.AllFiles.ordinal());
      }
      else
      {
         ((Element) listPresetFilePatterns_.getElement().getChild(
            Include.PackageSource.ordinal())).removeAttribute("disabled");
         ((Element) listPresetFilePatterns_.getElement().getChild(
            Include.PackageTests.ordinal())).removeAttribute("disabled");
      }
   }

   private void manageExcludeFilePattern()
   {
      // disable custom exclude text box when 'Exclude these files:' is unchecked
      showDivIf(divExcludeCustomFilter_, checkboxExcludeCustom_.getValue());
      
      // disable 'Files matched by .gitignore' when directory is not a git repository or git is not installed
      showDivIf(divExcludeGitIgnore_, gitStatus_);
   }

   private void showDivIf(DivElement div, boolean visible) 
   {
      div.getStyle().setDisplay(visible ? Style.Display.BLOCK : Style.Display.NONE);
   }

   @Override
   protected State collectInput()
   {
      String includeFilePatterns =
            listPresetFilePatterns_.getValue(
                  listPresetFilePatterns_.getSelectedIndex());
      if (StringUtil.equals(includeFilePatterns, "custom"))
         includeFilePatterns = txtFilePattern_.getText();

      ArrayList<String> list = new ArrayList<>();
      for (String pattern : includeFilePatterns.split(","))
      {
         String trimmedPattern = pattern.trim();
         if (trimmedPattern.length() > 0)
            list.add(trimmedPattern);
      }

      String excludeFilePatterns = checkboxExcludeCustom_.getValue() ? txtExcludeFilePattern_.getText() : "";
      ArrayList<String> excludeList = new ArrayList<>();
      for (String pattern : excludeFilePatterns.split(","))
      {
         String trimmedPattern = pattern.trim();
         if (trimmedPattern.length() > 0)
            excludeList.add(trimmedPattern);
      }
      
      return State.createState(txtSearchPattern_.getText(),
                               getEffectivePath(),
                               checkboxRegex_.getValue(),
                               checkboxCaseSensitive_.getValue(),
                               checkboxWholeWord_.getValue(),
                               JsUtil.toJsArrayString(list),
                               gitStatus_,
                               checkboxExcludeGitIgnore_.getValue(),
                               JsUtil.toJsArrayString(excludeList));
   }

   private String getEffectivePath()
   {
      if (StringUtil.notNull(dirChooser_.getText()).trim().length() == 0)
         return null;
      return FileSystemItem.createDir(dirChooser_.getText()).getPath();
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
               constants_.errorCaption(), constants_.errorMessage());

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
      checkboxWholeWord_.setValue(dialogState.isWholeWord());
      checkboxRegex_.setValue(dialogState.isRegex());
      dirChooser_.setText(dialogState.getPath());

      String includeFilePatterns = StringUtil.join(
            Arrays.asList(dialogState.getFilePatterns()), ", ");
      int index = listPresetFilePatterns_.getIndexFromValue(includeFilePatterns);
      if (index >= 0)
         listPresetFilePatterns_.setSelectedIndex(index);
      else
         listPresetFilePatterns_.setSelectedIndex(Include.CustomFilter.ordinal());
      if (index == Include.PackageSource.ordinal() ||
          index == Include.PackageTests.ordinal())
         packageStatus_ = true;
      txtFilePattern_.setText(includeFilePatterns);
      manageFilePattern();
      String excludeFilePatterns = StringUtil.join(
         Arrays.asList(dialogState.getExcludeFilePatterns()), ",");

      checkboxExcludeGitIgnore_.setValue(dialogState.getExcludeGitIgnore());
      txtExcludeFilePattern_.setText(excludeFilePatterns);
      checkboxExcludeCustom_.setValue(!StringUtil.equals(excludeFilePatterns, ""));
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
      Roles.getTextboxRole().setAriaLabelProperty(textbox.getElement(), constants_.customFilterPatterValue());
      Roles.getTextboxRole().setAriaDescribedbyProperty(textbox.getElement(),
            ElementIds.getAriaElementId(ElementIds.FIND_FILES_PATTERN_EXAMPLE));
   }

   @UiField
   LabeledTextBox txtSearchPattern_;
   @UiField
   CheckBox checkboxCaseSensitive_;
   @UiField
   CheckBox checkboxWholeWord_;
   @UiField
   CheckBox checkboxRegex_;
   @UiField(provided = true)
   DirectoryChooserTextBox dirChooser_;
   @UiField
   TextBox txtFilePattern_;
   @UiField
   FormLabel labelFilePatterns_;
   @UiField
   FormListBox listPresetFilePatterns_;
   @UiField
   DivElement divCustomFilter_;
   @UiField
   TextBox txtExcludeFilePattern_;
   @UiField
   DivElement divExcludeCustomFilter_;
   @UiField 
   DivElement divExcludeGitIgnore_;
   @UiField
   SpanElement spanPatternExample_;
   @UiField
   SpanElement spanExcludePatternExample_;
   @UiField
   CheckBox checkboxExcludeGitIgnore_;
   @UiField
   CheckBox checkboxExcludeCustom_;

   private boolean gitStatus_;
   private boolean packageStatus_;
   private final Widget mainWidget_;
   private final GlobalDisplay globalDisplay_ = RStudioGinjector.INSTANCE.getGlobalDisplay();
   private final Session session_ = RStudioGinjector.INSTANCE.getSession();
   private static final OutputConstants constants_ = GWT.create(OutputConstants.class);
}
