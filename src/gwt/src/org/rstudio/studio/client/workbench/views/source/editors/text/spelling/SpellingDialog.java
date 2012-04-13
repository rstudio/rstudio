package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;

import java.util.ArrayList;

public class SpellingDialog extends ModalDialogBase implements CheckSpelling.Display
{
   interface Binder extends UiBinder<Widget, SpellingDialog>
   {}

   public SpellingDialog()
   {
      setText("Check Spelling");

      btnAdd_ = new ThemedButton("Add");
      btnAdd_.setTitle("Add word to user dictionary");
      btnSkip_ = new ThemedButton("Ignore");
      btnIgnoreAll_ = new ThemedButton("Ignore All");
      btnChange_ = new ThemedButton("Change");
      btnChangeAll_ = new ThemedButton("Change All");
      prepareButtons(btnAdd_, btnSkip_, btnIgnoreAll_, btnChange_, btnChangeAll_);

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);

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
}
