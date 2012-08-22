/*
 * FindReplaceBar.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.findreplace;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CheckboxLabel;
import org.rstudio.core.client.widget.FindTextBox;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.workbench.views.history.view.Shelf;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplace.Display;

public class FindReplaceBar extends Composite implements Display, RequiresResize
{
   interface Resources extends ClientBundle
   {
      @Source("FindReplaceBar.css")
      Styles styles();

      ImageResource findReplace();
      ImageResource findReplaceLatched();
   }

   interface Styles extends CssResource
   {
      String findReplaceBar();
      String replaceTextBox();
      String findPanelWithReplace();
      String optionsPanel();
      String optionsPanelWithReplace();
      String checkboxLabel();
      String closeButton();
   }

   public FindReplaceBar(boolean showReplace, final boolean defaultForward)
   {
      showReplace_ = showReplace;
      defaultForward_ = defaultForward;
      
      Shelf shelf = new Shelf(showReplace);
      shelf.setWidth("100%");

      CellPanel panel = showReplace ? new VerticalPanel() : new HorizontalPanel();
     
      HorizontalPanel findReplacePanel = new HorizontalPanel();
      if (showReplace)
         findReplacePanel.addStyleName(RES.styles().findPanelWithReplace());
      findReplacePanel.add(txtFind_ = new FindTextBox("Find"));
      txtFind_.setIconVisible(true);
      findReplacePanel.add(btnFindNext_ = new SmallButton("Next", true));
      findReplacePanel.add(btnFindPrev_ = new SmallButton("Prev", true));
      findReplacePanel.add(txtReplace_ = new FindTextBox("Replace"));
      txtReplace_.addStyleName(RES.styles().replaceTextBox());
      findReplacePanel.add(btnReplace_ = new SmallButton("Replace"));
      findReplacePanel.add(btnReplaceAll_ = new SmallButton("All"));
      
      panel.add(findReplacePanel);
      
      HorizontalPanel optionsPanel = new HorizontalPanel();
      if (showReplace)
         optionsPanel.addStyleName(RES.styles().optionsPanelWithReplace());
      else
         optionsPanel.addStyleName(RES.styles().optionsPanel());
      
          
      optionsPanel.add(chkCaseSensitive_ = new CheckBox());
      Label matchCaseLabel =
                  new CheckboxLabel(chkCaseSensitive_, "Match case").getLabel();
      matchCaseLabel.addStyleName(RES.styles().checkboxLabel());
      optionsPanel.add(matchCaseLabel);
      
      optionsPanel.add(chkWholeWord_ = new CheckBox());
      Label wholeWordLabel = 
             new CheckboxLabel(chkWholeWord_, "Whole word").getLabel();
      wholeWordLabel.addStyleName(RES.styles().checkboxLabel());
      optionsPanel.add(wholeWordLabel);
      
      optionsPanel.add(chkRegEx_ = new CheckBox());
      Label regexLabel = new CheckboxLabel(chkRegEx_, "Regex").getLabel();
      regexLabel.addStyleName(RES.styles().checkboxLabel());
      
      optionsPanel.add(regexLabel);
      
      panel.add(optionsPanel);
      
      shelf.addLeftWidget(panel);
      
      // fixup tab indexes of controls
      txtFind_.setTabIndex(100);
      txtReplace_.setTabIndex(101);
      chkCaseSensitive_.setTabIndex(102);
      chkWholeWord_.setTabIndex(103);
      chkRegEx_.setTabIndex(104);
      
      // remove SmallButton instances from tab order since (a) they aren't
      // capable of showing a focused state; and (b) enter is already a
      // keyboard shortcut for both find and replace
      btnFindNext_.setTabIndex(-1);
      btnFindPrev_.setTabIndex(-1);
      btnReplace_.setTabIndex(-1);
      btnReplaceAll_.setTabIndex(-1);
     
      if (showReplace)
         shelf.setRightVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      shelf.addRightWidget(btnClose_ = new Button());
      btnClose_.setStyleName(RES.styles().closeButton());
      btnClose_.addStyleName(ThemeStyles.INSTANCE.closeTabButton());
      btnClose_.getElement().appendChild(
            new Image(ThemeResources.INSTANCE.closeTab()).getElement());

      txtFind_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.preventDefault();
               event.stopPropagation();
               if (defaultForward_)
                  btnFindNext_.click();
               else
                  btnFindPrev_.click();
               focusFindField(false);
            }
         }
      });

      txtReplace_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.preventDefault();
               event.stopPropagation();
               btnReplace_.click();
               WindowEx.get().focus();
               txtReplace_.focus();
            }
         }
      });

      shelf.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE
                && !event.isAnyModifierKeyDown())
            {
               event.stopPropagation();
               event.preventDefault();

               btnClose_.click();
            }
         }
      });
      
      if (!showReplace)
      {
         txtReplace_.setVisible(false);
         btnReplace_.setVisible(false);
         btnReplaceAll_.setVisible(false);
      }

      initWidget(shelf);

      addStyleName(RES.styles().findReplaceBar());
   }

   public HasValue<String> getFindValue()
   {
      return txtFind_;
   }

   public HasValue<String> getReplaceValue()
   {
      return txtReplace_;
   }

   public HasValue<Boolean> getCaseSensitive()
   {
      return chkCaseSensitive_;
   }

   public HasValue<Boolean> getWholeWord()
   {
      return chkWholeWord_;
   }
   
   public HasValue<Boolean> getRegex()
   {
      return chkRegEx_;
   }

   public HasClickHandlers getFindNextButton()
   {
      return btnFindNext_;
   }

   public HasClickHandlers getFindPrevButton()
   {
      return btnFindPrev_;
   }

   public HasClickHandlers getReplace()
   {
      return btnReplace_;
   }

   public HasClickHandlers getReplaceAll()
   {
      return btnReplaceAll_;
   }


   public HasClickHandlers getCloseButton()
   {
      return btnClose_;
   }

   public double getHeight()
   {
      if (showReplace_)
         return 56;
      else
         return 23;
   }
   
   public void activate(String searchText, boolean defaultForward)
   {
      defaultForward_ = defaultForward;
      
      focusFindField(false);
      
      txtFind_.setValue(searchText);
   }

   private void focusFindField(boolean selectAll)
   {
      WindowEx.get().focus();
      txtFind_.focus();
      if (selectAll)
         txtFind_.selectAll();
   }
   

   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   public void onResize()
   {
      int width = getOffsetWidth();
      setNarrowMode(width > 0 && width < 520);
   }

   private void setNarrowMode(boolean narrow)
   {
      if (narrow)
      {
         txtFind_.setOverrideWidth(100);
         txtReplace_.setOverrideWidth(100);
      }
      else
      {
         txtFind_.setOverrideWidth(160);
         txtReplace_.setOverrideWidth(160);
      }
   }

   public static ImageResource getFindIcon()
   {
      return RES.findReplace();
   }
   
   public static ImageResource getFindLatchedIcon()
   {
      return RES.findReplaceLatched();
   }

   private FindTextBox txtFind_;
   private FindTextBox txtReplace_;
   private SmallButton btnFindNext_;
   private SmallButton btnFindPrev_;
   private SmallButton btnReplace_;
   private SmallButton btnReplaceAll_;
   private CheckBox chkWholeWord_;
   private CheckBox chkCaseSensitive_;
   private CheckBox chkRegEx_;
   private Button btnClose_;
   private static Resources RES = GWT.create(Resources.class);
   private final boolean showReplace_;
   private boolean defaultForward_;
}
