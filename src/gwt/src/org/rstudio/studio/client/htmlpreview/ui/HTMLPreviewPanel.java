/*
 * HTMLPreviewPanel.java
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
package org.rstudio.studio.client.htmlpreview.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FindTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.htmlpreview.HTMLPreviewPresenter;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;

public class HTMLPreviewPanel extends ResizeComposite
                              implements HTMLPreviewPresenter.Display
{
   @Inject
   public HTMLPreviewPanel(Commands commands)
   {
      layoutPanel_ = new LayoutPanel();
      
      toolbar_ = createToolbar(commands);
      tbHeight_ = toolbar_.getHeight();
      layoutPanel_.add(toolbar_);
      layoutPanel_.setWidgetLeftRight(toolbar_, 0, Unit.PX, 0, Unit.PX);
      layoutPanel_.setWidgetTopHeight(toolbar_, 0, Unit.PX, tbHeight_, Unit.PX);
      
      previewFrame_ = new AnchorableFrame("HTML Preview Panel");
      previewFrame_.setSize("100%", "100%");
      layoutPanel_.add(previewFrame_);
      layoutPanel_.setWidgetLeftRight(previewFrame_,  0, Unit.PX, 0, Unit.PX);
      
      setToolbarVisible(true);
     
      initWidget(layoutPanel_);
   }
   
   private void setToolbarVisible(boolean visible)
   {
      toolbar_.setVisible(visible);
      int frameTop = visible ? tbHeight_+1 : 0;
      layoutPanel_.setWidgetTopBottom(previewFrame_, frameTop, Unit.PX, 0, Unit.PX);
   }
   
   private Toolbar createToolbar(Commands commands)
   {
      Toolbar toolbar = new Toolbar("Preview Tab");
      
      fileCaption_ = new ToolbarLabel("Preview: ");
      toolbar.addLeftWidget(fileCaption_);
      fileLabel_ = new ToolbarLabel();
      fileLabel_.addStyleName(ThemeStyles.INSTANCE.subtitle());
      fileLabel_.getElement().getStyle().setMarginRight(7, Unit.PX);
      toolbar.addLeftWidget(fileLabel_);
      
      fileLabelSeparator_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands.openHtmlExternal().createToolbarButton());
      
      showLogButtonSeparator_ = toolbar.addLeftSeparator();
      showLogButton_ = commands.showHtmlPreviewLog().createToolbarButton();
      toolbar.addLeftWidget(showLogButton_);
      
      saveHtmlPreviewAsSeparator_ = toolbar.addLeftSeparator();
      if (Desktop.hasDesktopFrame())
      { 
         saveHtmlPreviewAs_ = commands.saveHtmlPreviewAs().createToolbarButton();
         toolbar.addLeftWidget(saveHtmlPreviewAs_);
      }
      else
      {
         ToolbarPopupMenu menu = new ToolbarPopupMenu();
         menu.addItem(commands.saveHtmlPreviewAs().createMenuItem(false));
         menu.addItem(commands.saveHtmlPreviewAsLocalFile().createMenuItem(false));
      
         saveHtmlPreviewAs_ = toolbar.addLeftWidget(new ToolbarMenuButton(
               "Save As",
               ToolbarButton.NoTitle,
               commands.saveSourceDoc().getImageResource(),
               menu));
         
         
      }
      
      publishButtonSeparator_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(
               publishButton_ = new RSConnectPublishButton(
                     RSConnectPublishButton.HOST_HTML_PREVIEW,
                     RSConnect.CONTENT_TYPE_DOCUMENT, true, null));
      
      findTextBox_ = new FindTextBox("Find");
      findTextBox_.setIconVisible(true);
      findTextBox_.setOverrideWidth(120);
      findTextBox_.getElement().getStyle().setMarginRight(6, Unit.PX);
      toolbar.addRightWidget(findTextBox_);
      
      findTextBox_.addKeyDownHandler(new KeyDownHandler() {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            // enter key triggers a find
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.preventDefault();
               event.stopPropagation();
               findInTopic(findTextBox_.getValue().trim(), findTextBox_);
               findTextBox_.focus();
            }
            else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               findTextBox_.setValue("");
            }       
         }
         
         private void findInTopic(String term, CanFocus findInputSource)
         {
            // get content window
            WindowEx contentWindow = previewFrame_.getWindow();
            if (contentWindow == null)
               return;
                
            if (!contentWindow.find(term, false, false, true, false))
            {
               RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
                     MessageDialog.INFO,
                     "Find in Page", 
                     "No occurrences found",
                     findInputSource);
            }     
         }
         
      });
      
      toolbar.addRightSeparator();

      ToolbarButton refreshButton = commands.refreshHtmlPreview().createToolbarButton();
      refreshButton.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      toolbar.addRightWidget(refreshButton);
      
      
      return toolbar;
   }
   
   
   @Override
   public void showLog(String log)
   {
      final HTMLPreviewProgressDialog dialog = 
                              new HTMLPreviewProgressDialog("Log");
      dialog.showOutput(log);
      dialog.stopProgress();
      dialog.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            dialog.dismiss(); 
         }
      });
      
   }
   
   @Override
   public void showProgress(String caption)
   {
      closeProgress();
      activeProgressDialog_ = new HTMLPreviewProgressDialog(caption, 500);
   }
   
   @Override
   public void setProgressCaption(String caption)
   {
      activeProgressDialog_.setCaption(caption);
   }
   
   @Override
   public HandlerRegistration addProgressClickHandler(ClickHandler handler)
   {
      return activeProgressDialog_.addClickHandler(handler);
   }
   
   @Override
   public void showProgressOutput(String output)
   {
      activeProgressDialog_.showOutput(output);
   }

   @Override
   public void stopProgress()
   {
      activeProgressDialog_.stopProgress();
   }
   
   @Override
   public void closeProgress()
   {
      if (activeProgressDialog_ != null)
      {
         activeProgressDialog_.dismiss();
         activeProgressDialog_ = null;
      }
   }
   
   @Override
   public void showPreview(String url, 
                           HTMLPreviewResult result,
                           boolean enableShowLog)
   {
      Window.setTitle(result.getTitle());
      
      if (result.getEnableFileLabel()) 
      {
         String shortFileName = StringUtil.shortPathName(
                FileSystemItem.createFile(result.getHtmlFile()), 
                ThemeStyles.INSTANCE.subtitle(), 
                300);
         fileLabel_.setText(shortFileName);
      }
      else
      {
         fileCaption_.setVisible(false);
         fileLabel_.setVisible(false);
         fileLabelSeparator_.setVisible(false);
      }
     
      showLogButtonSeparator_.setVisible(enableShowLog);
      showLogButton_.setVisible(enableShowLog);
      saveHtmlPreviewAsSeparator_.setVisible(result.getEnableSaveAs());
      saveHtmlPreviewAs_.setVisible(result.getEnableSaveAs());
      if (result.getEnablePublish()) 
         publishButton_.setHtmlPreview(result);
      else
         publishButton_.setVisible(false);
      publishButtonSeparator_.setVisible(publishButton_.isVisible());
      navigate(url);
   }
   
   @Override
   public void reload(String url)
   {
      navigate(url);
   }
   
   @Override
   public void print()
   {
      WindowEx window = previewFrame_.getWindow();
      window.focus();
      window.print();
   }
   
   @Override
   public String getDocumentTitle()
   {
      return previewFrame_.getWindow().getDocument().getTitle();
   }

   @Override
   public void focusFind()
   {
      findTextBox_.focus();
   }

   private void navigate(String url)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().setViewerUrl(StringUtil.notNull(url));
      // use setUrl rather than navigate to deal with same origin policy
      previewFrame_.setUrl(url);
   }
   
   private final LayoutPanel layoutPanel_;
   private final AnchorableFrame previewFrame_;
   private final Toolbar toolbar_;
   private final int tbHeight_;
   private ToolbarLabel fileCaption_;
   private ToolbarLabel fileLabel_;
   private Widget fileLabelSeparator_;
   private FindTextBox findTextBox_;
   private Widget saveHtmlPreviewAsSeparator_;
   private Widget saveHtmlPreviewAs_;
   private Widget publishButtonSeparator_;
   private RSConnectPublishButton publishButton_;
   private Widget showLogButtonSeparator_;
   private ToolbarButton showLogButton_;
   private HTMLPreviewProgressDialog activeProgressDialog_;
}
