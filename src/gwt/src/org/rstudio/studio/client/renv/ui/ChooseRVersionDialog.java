/*
 * ChooseRVersionDialog.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.renv.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.renv.RVersionChooser;
import org.rstudio.studio.client.renv.RenvConstants;
import org.rstudio.studio.client.renv.model.RVersionInstall;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Chooses the installation of R that RStudio uses: the default R, an
 * installation found on this machine, or one the user browses to.
 */
public class ChooseRVersionDialog extends ModalDialog<RVersionChooser.Choice>
{
   /** Lets the user pick an R executable; `onChosen` receives its installation. */
   public interface Browser
   {
      void browse(CommandWithArg<RVersionInstall> onChosen);
   }

   /**
    * @param caption The dialog's title.
    * @param okCaption The caption of the button that accepts the choice.
    * @param defaultR The default R as an installation (null when there is
    *    none); its 'current' says whether it is the R in use.
    * @param installed The installations to offer, newest first. One whose
    *    executable is the default R's is offered only as the default R.
    * @param selected The choice to start from.
    */
   public ChooseRVersionDialog(String caption,
                               String okCaption,
                               RVersionInstall defaultR,
                               List<RVersionInstall> installed,
                               RVersionChooser.Choice selected,
                               Browser browser,
                               OperationWithInput<RVersionChooser.Choice> operation)
   {
      super(caption, Roles.getDialogRole(), operation);
      setThemeAware(true);
      setOkButtonCaption(okCaption);

      list_ = new ListBox();
      list_.setMultipleSelect(false);
      list_.setVisibleItemCount(8);
      list_.setWidth("100%");
      ElementIds.assignElementId(list_, ElementIds.CHOOSE_R_VERSION_LIST);
      list_.addChangeHandler(event -> updateOkButton());

      // an installation that is the default R (Debian's /usr/lib/R, whose
      // executable is /usr/bin/R, say) would be followed as the default R if
      // chosen, whatever its entry says
      defaultR_ = defaultR;
      if (defaultR != null)
         add(new RVersionChooser.Choice(defaultR, true));
      for (RVersionInstall installation : installed)
      {
         if (!isDefaultR(installation))
            add(new RVersionChooser.Choice(installation, false));
      }

      select(selected);

      ThemedButton browse = new ThemedButton(constants_.browseForR(), event ->
      {
         browser.browse(installation ->
         {
            RVersionChooser.Choice choice = new RVersionChooser.Choice(installation, false);
            int index = isDefaultR(installation) ? indexOfDefault() : indexOf(choice);
            if (index < 0)
            {
               add(choice);
               index = choices_.size() - 1;
            }
            list_.setSelectedIndex(index);
            updateOkButton();
            list_.setFocus(true);
         });
      });
      addLeftButton(browse, ElementIds.CHOOSE_R_VERSION_BROWSE);

      contents_ = new VerticalPanel();
      contents_.setWidth("520px");
      contents_.add(new FormLabel(constants_.chooseRVersionLabel(), list_));
      contents_.add(new VerticalSpacer("4px"));
      contents_.add(list_);
      updateOkButton();
   }

   @Override
   protected RVersionChooser.Choice collectInput()
   {
      int index = list_.getSelectedIndex();
      return index < 0 ? null : choices_.get(index);
   }

   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }

   @Override
   public void focusFirstControl()
   {
      list_.setFocus(true);
   }

   private void add(RVersionChooser.Choice choice)
   {
      choices_.add(choice);
      list_.addItem(RVersionChooser.describe(choice, true));
   }

   // the given choice, or else the default R when nothing was chosen, or
   // else the R in use
   private void select(RVersionChooser.Choice selected)
   {
      int index = selected == null ? -1 : indexOf(selected);
      if (index < 0 && selected != null && selected.useDefault)
         index = indexOfDefault();
      if (index < 0)
         index = indexOfCurrent();
      if (index < 0 && !choices_.isEmpty())
         index = 0;

      if (index >= 0)
         list_.setSelectedIndex(index);
   }

   private int indexOf(RVersionChooser.Choice choice)
   {
      for (int i = 0; i < choices_.size(); i++)
         if (choices_.get(i).sameAs(choice))
            return i;
      return -1;
   }

   private boolean isDefaultR(RVersionInstall installation)
   {
      return defaultR_ != null && RVersionChooser.sameExecutable(installation.binary, defaultR_.binary);
   }

   private int indexOfDefault()
   {
      for (int i = 0; i < choices_.size(); i++)
         if (choices_.get(i).useDefault)
            return i;
      return -1;
   }

   private int indexOfCurrent()
   {
      for (int i = 0; i < choices_.size(); i++)
         if (choices_.get(i).installation.current)
            return i;
      return -1;
   }

   private void updateOkButton()
   {
      enableOkButton(list_.getSelectedIndex() >= 0);
   }

   private final ListBox list_;
   private final RVersionInstall defaultR_;
   private final List<RVersionChooser.Choice> choices_ = new ArrayList<>();
   private final VerticalPanel contents_;

   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
