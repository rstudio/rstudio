/*
 * EditingPreferencesPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public class EditingPreferencesPane extends PreferencesPane
{
   @Inject
   public EditingPreferencesPane(SourceServerOperations server,
                                 UIPrefs prefs,
                                 PreferencesDialogResources res)
   {
      server_ = server;
      prefs_ = prefs;
      res_ = res;

      add(checkboxPref("Highlight selected word", prefs.highlightSelectedWord()));
      add(checkboxPref("Highlight selected line", prefs.highlightSelectedLine()));
      add(checkboxPref("Show line numbers", prefs.showLineNumbers()));
      add(tight(spacesForTab_ = checkboxPref("Insert spaces for tab", prefs.useSpacesForTab())));
      add(indent(tabWidth_ = numericPref("Tab width", prefs.numSpacesForTab())));
      add(tight(showMargin_ = checkboxPref("Show margin", prefs.showMargin())));
      add(indent(marginCol_ = numericPref("Margin column", prefs.printMarginColumn())));
      add(checkboxPref("Show whitespace characters", prefs_.showInvisibles()));
      add(checkboxPref("Show indent guides", prefs_.showIndentGuides()));
      add(checkboxPref("Enable vim editing mode", prefs_.useVimMode()));
      add(checkboxPref("Insert matching parens/quotes", prefs_.insertMatching()));
      add(checkboxPref("Soft-wrap R source files", prefs_.softWrapRFiles()));
      add(checkboxPref("Show syntax highlighting in console input", prefs_.syntaxColorConsole()));
      add(checkboxPref("Save all files before build", prefs_.saveAllBeforeBuild()));
      add(checkboxPref("Automatically navigate to build errors", prefs_.navigateToBuildError()));


      encodingValue_ = prefs_.defaultEncoding().getGlobalValue();
      add(encoding_ = new TextBoxWithButton(
            "Default text encoding:",
            "Change...",
            new ClickHandler()
            {
               public void onClick(ClickEvent event)
               {
                  server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
                  {
                     @Override
                     public void onResponseReceived(IconvListResult response)
                     {
                        new ChooseEncodingDialog(
                              response.getCommon(),
                              response.getAll(),
                              encodingValue_,
                              true,
                              false,
                              new OperationWithInput<String>()
                              {
                                 public void execute(String encoding)
                                 {
                                    if (encoding == null)
                                       return;

                                    setEncoding(encoding);
                                 }
                              }).showModal();
                     }
                  });

               }
            }));
      encoding_.setWidth("250px");
      encoding_.addStyleName(res_.styles().encodingChooser());
      nudgeRight(encoding_);
      setEncoding(prefs.defaultEncoding().getGlobalValue());
   }

   private void setEncoding(String encoding)
   {
      encodingValue_ = encoding;
      if (StringUtil.isNullOrEmpty(encoding))
         encoding_.setText(ChooseEncodingDialog.ASK_LABEL);
      else
         encoding_.setText(encoding);
   }


   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconCodeEditing();
   }

   @Override
   public boolean validate()
   {
      return (!spacesForTab_.getValue() || tabWidth_.validatePositive("Tab width")) &&
             (!showMargin_.getValue() || marginCol_.validate("Margin column"));
   }

   @Override
   public String getName()
   {
      return "Code Editing";
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
   }
   
   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);
      prefs_.defaultEncoding().setGlobalValue(encodingValue_);
      return restartRequired;
   }

   private final SourceServerOperations server_;
   private final UIPrefs prefs_;
   private final PreferencesDialogResources res_;
   private final NumericValueWidget tabWidth_;
   private final NumericValueWidget marginCol_;
   private final TextBoxWithButton encoding_;
   private String encodingValue_;
   private final CheckBox spacesForTab_;
   private final CheckBox showMargin_;
   
}
