/*
 * AccessibilityPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.CheckBoxList;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import java.util.Map;

public class AccessibilityPreferencesPane extends PreferencesPane
{
   @Inject
   public AccessibilityPreferencesPane(UserPrefs prefs,
                                       AriaLiveService ariaLive,
                                       PreferencesDialogResources res)
   {
      res_ = res;
      ariaLive_ = ariaLive;

      VerticalTabPanel generalPanel = new VerticalTabPanel(ElementIds.A11Y_GENERAL_PREFS);
      VerticalTabPanel announcementsPanel = new VerticalTabPanel(ElementIds.A11Y_ANNOUNCEMENTS_PREFS);

      generalPanel.add(headerLabel("Assistive Tools"));
      chkScreenReaderEnabled_ = new CheckBox("Screen reader support (requires restart)");
      generalPanel.add(chkScreenReaderEnabled_);

      typingStatusDelay_ = numericPref("Milliseconds after typing before speaking results",
            1, 9999, prefs.typingStatusDelayMs());
      generalPanel.add(indent(typingStatusDelay_));
      generalPanel.add(indent(maxOutput_ = numericPref("Maximum number of console output lines to read",
            0, UserPrefs.MAX_SCREEN_READER_CONSOLE_OUTPUT, prefs.screenreaderConsoleAnnounceLimit())));

      Label displayLabel = headerLabel("Other");
      generalPanel.add(displayLabel);
      displayLabel.getElement().getStyle().setMarginTop(8, Style.Unit.PX);
      generalPanel.add(checkboxPref("Reduce user interface animations", prefs.reducedMotion()));
      chkTabMovesFocus_ = new CheckBox("Tab key always moves focus");
      generalPanel.add(lessSpaced(chkTabMovesFocus_));
      chkShowFocusRectangles_ = new CheckBox("Always show focus outlines (requires restart)");
      generalPanel.add(lessSpaced(chkShowFocusRectangles_));
      generalPanel.add(checkboxPref("Highlight focused panel", prefs.showPanelFocusRectangle()));

      HelpLink helpLink = new HelpLink("RStudio accessibility help", "rstudio_a11y", false);
      nudgeRight(helpLink);
      helpLink.addStyleName(res_.styles().newSection());
      generalPanel.add(helpLink);

      Label announcementsLabel = headerLabel("Enable / Disable Announcements");
      announcements_ = new CheckBoxList(announcementsLabel);
      announcementsPanel.add(announcementsLabel);
      announcementsLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      announcementsPanel.add(announcements_);
      DomUtils.ensureHasId(announcementsLabel.getElement());
      Roles.getListboxRole().setAriaLabelledbyProperty(announcements_.getElement(),
            Id.of(announcementsLabel.getElement()));
      announcements_.setHeight("380px");
      announcements_.setWidth("390px");
      announcements_.getElement().getStyle().setMarginBottom(15, Unit.PX);
      announcements_.getElement().getStyle().setMarginLeft(3, Unit.PX);

      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("Accessibility");
      tabPanel.setSize("435px", "533px");
      tabPanel.add(generalPanel, "General", generalPanel.getBasePanelId());
      tabPanel.add(announcementsPanel, "Announcements", announcementsPanel.getBasePanelId());
      tabPanel.selectTab(0);
      add(tabPanel);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconAccessibility2x());
   }

   @Override
   public String getName()
   {
      return "Accessibility";
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      initialScreenReaderEnabled_ = prefs.enableScreenReader().getValue();
      initialShowFocusRectangles_ = prefs.showFocusRectangles().getValue();
      chkScreenReaderEnabled_.setValue(initialScreenReaderEnabled_);
      chkShowFocusRectangles_.setValue(initialShowFocusRectangles_);
      chkTabMovesFocus_.setValue(prefs.tabKeyMoveFocus().getValue());
      populateAnnouncementList();
   }

   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement restartRequirement = super.onApply(prefs);

      boolean screenReaderEnabledSetting = chkScreenReaderEnabled_.getValue();
      if (screenReaderEnabledSetting != initialScreenReaderEnabled_)
      {
         initialScreenReaderEnabled_ = screenReaderEnabledSetting;
         prefs.setScreenReaderEnabled(screenReaderEnabledSetting);
         restartRequirement.setRestartRequired();
      }

      if (chkShowFocusRectangles_.getValue() != initialShowFocusRectangles_)
      {
         initialShowFocusRectangles_ = chkShowFocusRectangles_.getValue();
         prefs.showFocusRectangles().setGlobalValue(chkShowFocusRectangles_.getValue());
         restartRequirement.setRestartRequired();
      }

      prefs.tabKeyMoveFocus().setGlobalValue(chkTabMovesFocus_.getValue());
      prefs.syncToggleTabKeyMovesFocusState(chkTabMovesFocus_.getValue());

      if (applyAnnouncementList(prefs))
         restartRequirement.setUiReloadRequired(true);

      return restartRequirement;
   }

   @Override
   public boolean validate()
   {
      return (!chkScreenReaderEnabled_.getValue() ||
            (typingStatusDelay_.validate() && maxOutput_.validate()));
   }

   private void populateAnnouncementList()
   {
      announcements_.clearItems();
      for (Map.Entry<String,String> entry : ariaLive_.getAnnouncements().entrySet())
      {
         CheckBox checkBox = new CheckBox(entry.getValue());
         checkBox.setFormValue(entry.getKey());
         announcements_.addItem(checkBox);

         // The preference tracks disabled announcements, but the UI shows enabled announcements.
         // Having the UI show disabled announcements is counter-intuitive, but tracking
         // disabled items in the preferences causes newly added announcements to be enabled
         // by default.
         checkBox.setValue(!ariaLive_.isDisabled(entry.getKey()));
      }
   }

   private boolean applyAnnouncementList(UserPrefs prefs)
   {
      boolean origConsoleLog = ariaLive_.isDisabled(AriaLiveService.CONSOLE_LOG);
      boolean origConsoleCommand = ariaLive_.isDisabled(AriaLiveService.CONSOLE_COMMAND);
      boolean restartNeeded = false;

      JsArrayString settings = prefs.disabledAriaLiveAnnouncements().getValue();
      settings.setLength(0);
      for (int i = 0; i < announcements_.getItemCount(); i++)
      {
         CheckBox chk = announcements_.getItemAtIdx(i);
         if (!chk.getValue()) // preference tracks disabled, UI tracks enabled
            settings.push(chk.getFormValue());

         if (StringUtil.equals(chk.getFormValue(), AriaLiveService.CONSOLE_LOG) &&
               origConsoleLog == chk.getValue())
         {
            restartNeeded = true;
         }
         else if (StringUtil.equals(chk.getFormValue(), AriaLiveService.CONSOLE_COMMAND) &&
               origConsoleCommand == chk.getValue())
         {
            restartNeeded = true;
         }
      }

      prefs.disabledAriaLiveAnnouncements().setGlobalValue(settings);
      return restartNeeded;
   }

   private final CheckBox chkScreenReaderEnabled_;
   private final CheckBox chkShowFocusRectangles_;
   private final NumericValueWidget typingStatusDelay_;
   private final NumericValueWidget maxOutput_;
   private final CheckBox chkTabMovesFocus_;
   private final CheckBoxList announcements_;

   // initial values of prefs that can trigger reloads (to avoid unnecessary reloads)
   private boolean initialScreenReaderEnabled_;
   private boolean initialShowFocusRectangles_;

   private final PreferencesDialogResources res_;
   private final AriaLiveService ariaLive_;
}
