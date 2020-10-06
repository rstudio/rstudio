/*
 * SpellingDialog.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.Rectangle.FailureMode;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;

public class SpellingDialog extends ModalDialogBase implements CheckSpelling.Display
{
   interface Binder extends UiBinder<Widget, SpellingDialog>
   {}

   public SpellingDialog()
   {
      super(Roles.getDialogRole());
      setText("Check Spelling");

      btnAdd_ = new ThemedButton("Add");
      btnAdd_.setTitle("Add word to user dictionary");
      btnSkip_ = new ThemedButton("Skip");
      btnIgnoreAll_ = new ThemedButton("Ignore All");
      btnChange_ = new ThemedButton("Change");
      btnChangeAll_ = new ThemedButton("Change All");
      prepareButtons(btnAdd_, btnSkip_, btnIgnoreAll_, btnChange_, btnChangeAll_);

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);

      buttons_ = new ThemedButton[] {
            btnAdd_, btnIgnoreAll_, btnSkip_, btnChange_, btnChangeAll_
      };

      Roles.getListboxRole().setAriaLabelProperty(lstSuggestions_.getElement(), "Suggestions");
      addCancelButton();
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   private void prepareButtons(ThemedButton... buttons)
   {
      for (ThemedButton button : buttons)
      {
         button.setTight(true);
      }
   }

   @Override
   public HasClickHandlers getAddButton()
   {
      return btnAdd_;
   }

   @Override
   public HasClickHandlers getIgnoreAllButton()
   {
      return btnIgnoreAll_;
   }

   @Override
   public HasClickHandlers getSkipButton()
   {
      return btnSkip_;
   }

   @Override
   public HasClickHandlers getChangeButton()
   {
      return btnChange_;
   }

   @Override
   public HasClickHandlers getChangeAllButton()
   {
      return btnChangeAll_;
   }

   @Override
   public HasText getMisspelledWord()
   {
      return txtDisplay_;
   }

   @Override
   public HasText getReplacement()
   {
      return txtReplacement_;
   }

   @Override
   public void setSuggestions(String[] values)
   {
      lstSuggestions_.clear();
      for (String value : values)
         lstSuggestions_.addItem(value);

      if (values.length > 0)
         lstSuggestions_.setSelectedIndex(0);
   }

   @Override
   public void clearSuggestions()
   {
      lstSuggestions_.clear();
   }

   @Override
   public HasChangeHandlers getSuggestionList()
   {
      return lstSuggestions_;
   }

   @Override
   public String getSelectedSuggestion()
   {
      int index = lstSuggestions_.getSelectedIndex();
      if (index < 0)
         return null;
      return lstSuggestions_.getItemText(index);
   }

   @Override
   public void focusReplacement()
   {
      txtReplacement_.setFocus(true);
      txtReplacement_.selectAll();
   }

   @Override
   public void closeDialog()
   {
      super.closeDialog();
   }

   @Override
   public void showProgress()
   {
      txtDisplay_.setText("Checking...");
      txtReplacement_.setText("");

      txtReplacement_.setEnabled(false);
      lstSuggestions_.setEnabled(false);
      clearSuggestions();
      setButtonsEnabled(false);
   }

   @Override
   public void hideProgress()
   {
      txtReplacement_.setEnabled(true);
      lstSuggestions_.setEnabled(true);
      setButtonsEnabled(true);
   }

   @Override
   public void setEditorSelectionBounds(Rectangle selectionBounds)
   {
      // Inflate the bounds by 10 pixels to add a little air
      boundsToAvoid_ = selectionBounds.inflate(10);
      if (isShowing())
      {
         Rectangle screen = new Rectangle(0, 0,
                                          Window.getClientWidth(),
                                          Window.getClientHeight());

         Rectangle bounds = new Rectangle(getPopupLeft(),
                                          getPopupTop(),
                                          getOffsetWidth(),
                                          getOffsetHeight());

         // In case user moved the dialog off the screen
         bounds = bounds.attemptToMoveInto(screen, FailureMode.NO_CHANGE);

         // Now avoid the selected word
         move(bounds.avoidBounds(boundsToAvoid_, screen),
               !RStudioGinjector.INSTANCE.getUserPrefs().reducedMotion().getValue());
      }
   }

   @Override
   protected void positionAndShowDialog(final Command onCompleted)
   {
      setPopupPositionAndShow(new PositionCallback()
      {
         @Override
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            Rectangle screen = new Rectangle(0, 0,
                                             Window.getClientWidth(),
                                             Window.getClientHeight());

            Rectangle bounds = screen.createCenteredRect(offsetWidth,
                                                         offsetHeight);

            move(bounds.avoidBounds(boundsToAvoid_, screen), false);

            onCompleted.execute();
         }
      });
   }

   private void setButtonsEnabled(boolean enabled)
   {
      for (ThemedButton button : buttons_)
         button.setEnabled(enabled);
   }

   private ThemedButton[] buttons_;

   @UiField(provided = true)
   ThemedButton btnAdd_;
   @UiField(provided = true)
   ThemedButton btnIgnoreAll_;
   @UiField(provided = true)
   ThemedButton btnSkip_;
   @UiField(provided = true)
   ThemedButton btnChange_;
   @UiField(provided = true)
   ThemedButton btnChangeAll_;
   @UiField
   TextBox txtReplacement_;
   @UiField
   ListBox lstSuggestions_;
   @UiField
   TextBox txtDisplay_;
   private final Widget mainWidget_;

   private Rectangle boundsToAvoid_;

}
