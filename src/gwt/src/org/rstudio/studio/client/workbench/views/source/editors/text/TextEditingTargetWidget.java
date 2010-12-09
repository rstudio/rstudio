/*
 * TextEditingTargetWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import org.rstudio.codemirror.client.CodeMirrorEditor;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.core.client.widget.WarningBar;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.Display;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

public class TextEditingTargetWidget extends ResizeComposite implements Display
{
   public TextEditingTargetWidget(Commands commands,
                                  DocDisplay editor,
                                  TextFileType fileType)
   {
      commands_ = commands;
      editor_ = editor;
      sourceOnSave_ = new CheckBox("Source on Save");
      sourceOnSave_.getElement().getStyle().setMarginLeft(-6, Unit.PX);
      panel_ = new PanelWithToolbar(createToolbar(),
                                    editor.toWidget());
      adaptToFileType(fileType);

      initWidget(panel_);
   }

   private Toolbar createToolbar()
   {
      Toolbar toolbar = new Toolbar();

      toolbar.addLeftWidget(commands_.saveSourceDoc().createToolbarButton());
      toolbar.addLeftWidget(sourceOnSave_);

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(createFindReplaceButton());
      toolbar.addLeftWidget(createCodeTransformMenuButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.printSourceDoc().createToolbarButton());
      toolbar.addLeftWidget(commands_.compilePDF().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.publishPDF().createToolbarButton());

      toolbar.addRightWidget(commands_.executeCode().createToolbarButton());
      toolbar.addRightWidget(commands_.executeAllCode().createToolbarButton());

      return toolbar;
   }

   private Widget createFindReplaceButton()
   {
      if (findReplaceBar_ == null)
      {
         findReplaceButton_ = new ToolbarButton(
               FindReplaceBar.getFindIcon(),
               new ClickHandler() {
                  public void onClick(ClickEvent event)
                  {
                     if (findReplaceBar_ == null)
                        showFindReplace();
                     else
                        hideFindReplace();
                  }
               });
         findReplaceButton_.setTitle("Find/Replace");
      }
      return findReplaceButton_;
   }

   private Widget createCodeTransformMenuButton()
   {
      if (codeTransform_ == null)
      {
         ImageResource icon = ThemeResources.INSTANCE.codeTransform();

         ToolbarPopupMenu menu = new ToolbarPopupMenu();
         menu.addItem(commands_.extractFunction().createMenuItem(false));
         menu.addItem(commands_.commentUncomment().createMenuItem(false));
         codeTransform_ = new ToolbarButton("", icon, menu);
         codeTransform_.setTitle("Code Tools");
      }
      return codeTransform_;
   }

   public void adaptToFileType(TextFileType fileType)
   {
      editor_.setFileType(fileType);
      editor_.setTextWrapping(fileType.getWordWrap());
      sourceOnSave_.setVisible(fileType.canSourceOnSave());
      codeTransform_.setVisible(fileType.canExecuteCode());
   }

   public HasValue<Boolean> getSourceOnSave()
   {
      return sourceOnSave_;
   }

   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   public void showWarningBar(String warning)
   {
      if (warningBar_ == null)
      {
         warningBar_ = new WarningBar();
         panel_.insertNorth(warningBar_, warningBar_.getHeight(), null);
      }
      warningBar_.setText(warning);
   }

   public void hideWarningBar()
   {
      if (warningBar_ != null)
      {
         panel_.remove(warningBar_);
      }
   }

   public void showFindReplace()
   {
      if (findReplaceBar_ == null)
      {
         findReplaceBar_ = new FindReplaceBar();
         new FindReplace((CodeMirrorEditor)editor_,
                         findReplaceBar_,
                         RStudioGinjector.INSTANCE.getGlobalDisplay());
         panel_.insertNorth(findReplaceBar_,
                            findReplaceBar_.getHeight(),
                            warningBar_);
         findReplaceBar_.getCloseButton().addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               hideFindReplace();
            }
         });

         findReplaceButton_.setLeftImage(FindReplaceBar.getFindLatchedIcon());
      }
      findReplaceBar_.focusFindField(true);
   }

   private void hideFindReplace()
   {
      if (findReplaceBar_ != null)
      {
         panel_.remove(findReplaceBar_);
         findReplaceBar_ = null;
         findReplaceButton_.setLeftImage(FindReplaceBar.getFindIcon());
      }
      editor_.focus();
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   private final Commands commands_;
   private final DocDisplay editor_;
   private CheckBox sourceOnSave_;
   private PanelWithToolbar panel_;
   private WarningBar warningBar_;
   private FindReplaceBar findReplaceBar_;
   private ToolbarButton findReplaceButton_;
   private ToolbarButton codeTransform_;
}
