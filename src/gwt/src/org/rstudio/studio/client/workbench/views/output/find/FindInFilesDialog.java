/*
 * FindInFilesDialog.java
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
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
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.workbench.model.Session;

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

   public enum Include
   {
      AllFiles,
      CommonRSourceFiles,
      RScripts,
      PackageSource,
      PackageTests,
      CustomFilter
   }

   public enum Exclude
   {
      None,
      StandardGit,
      CustomFilter
   }

   public FindInFilesDialog(OperationWithInput<State> operation)
   {
      super("Find in Files", Roles.getDialogRole(), operation);

      dirChooser_ = new DirectoryChooserTextBox("Search in:",
         ElementIds.TextBoxButtonId.FIND_IN,
         null);
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
      divCustomFilter_.getStyle().setDisplay(
            (listPresetFilePatterns_.getSelectedIndex() ==
             Include.CustomFilter.ordinal() &&
             listPresetExcludeFilePatterns_.getSelectedIndex() != 
             Exclude.StandardGit.ordinal())
            ? Style.Display.BLOCK
            : Style.Display.NONE);

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
      // disable 'Standard Git exclusions' when directory is not a git repository
      // or git is not installed
      // this should come first as it may change the value of listPresetExcludeFilePatterns
      if (!gitStatus_ ||
          !session_.getSessionInfo().isVcsAvailable(VCSConstants.GIT_ID))
      {
         DomUtils.setOptionDisabled(
            listPresetExcludeFilePatterns_.getElement(),
            Exclude.StandardGit.ordinal(),
            true);
         if (listPresetExcludeFilePatterns_.getSelectedIndex() ==
             Exclude.StandardGit.ordinal())
            listPresetExcludeFilePatterns_.setSelectedIndex(Exclude.None.ordinal());
      }
      else
         DomUtils.setOptionDisabled(
            listPresetExcludeFilePatterns_.getElement(),
            Exclude.StandardGit.ordinal(),
            false);

      // disable custom filter text box when 'Custom Filter' is not selected
      divExcludeCustomFilter_.getStyle().setDisplay(
            listPresetExcludeFilePatterns_.getSelectedIndex() == 
            Exclude.CustomFilter.ordinal()
            ? Style.Display.BLOCK
            : Style.Display.NONE);

      // user cannot specify include patterns when using git grep
      boolean disableIncludes =
         listPresetExcludeFilePatterns_.getSelectedIndex() == Exclude.StandardGit.ordinal();
      DomUtils.setOptionDisabled(
         listPresetFilePatterns_.getElement(),
         Include.CommonRSourceFiles.ordinal(),
         disableIncludes);
      DomUtils.setOptionDisabled(
         listPresetFilePatterns_.getElement(),
         Include.RScripts.ordinal(),
         disableIncludes);
      DomUtils.setOptionDisabled(
         listPresetFilePatterns_.getElement(),
         Include.CustomFilter.ordinal(),
         disableIncludes);

      // if a disabled index is selected, change selection to All Files
      if (disableIncludes &&
          (listPresetFilePatterns_.getSelectedIndex() ==
              Include.CommonRSourceFiles.ordinal() || 
           listPresetFilePatterns_.getSelectedIndex() ==
              Include.RScripts.ordinal() || 
           listPresetFilePatterns_.getSelectedIndex() ==
             Include.CustomFilter.ordinal()))
      {
         listPresetFilePatterns_.setSelectedIndex(Include.AllFiles.ordinal());
         manageFilePattern();
      }
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

      String excludeFilePatterns =
         listPresetExcludeFilePatterns_.getValue(
               listPresetExcludeFilePatterns_.getSelectedIndex());
      if (StringUtil.equals(excludeFilePatterns, "custom"))
         excludeFilePatterns = txtExcludeFilePattern_.getText();

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
                               JsUtil.toJsArrayString(list),
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
      index = listPresetExcludeFilePatterns_.getIndexFromValue(excludeFilePatterns);
      if (index >= 0)
         listPresetExcludeFilePatterns_.setSelectedIndex(index);
      else
         listPresetExcludeFilePatterns_.setSelectedIndex(Exclude.CustomFilter.ordinal());
      if (index == Exclude.StandardGit.ordinal())
         gitStatus_ = true;
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
   FormListBox listPresetFilePatterns_;
   @UiField
   DivElement divCustomFilter_;
   @UiField
   TextBox txtExcludeFilePattern_;
   @UiField
   FormLabel labelExcludeFilePatterns_;
   @UiField
   FormListBox listPresetExcludeFilePatterns_;
   @UiField
   DivElement divExcludeCustomFilter_;
   @UiField
   SpanElement spanPatternExample_;
   @UiField
   SpanElement spanExcludePatternExample_;

   private boolean gitStatus_;
   private boolean packageStatus_;
   private final Widget mainWidget_;
   private final GlobalDisplay globalDisplay_ = RStudioGinjector.INSTANCE.getGlobalDisplay();
   private final Session session_ = RStudioGinjector.INSTANCE.getSession();
}
