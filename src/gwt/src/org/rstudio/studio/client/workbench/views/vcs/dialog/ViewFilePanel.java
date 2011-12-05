package org.rstudio.studio.client.workbench.views.vcs.dialog;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FullscreenPopupPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetFindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ViewFilePanel extends Composite 
                           implements ViewFilePresenter.Display
{
   @Inject
   public ViewFilePanel(DocDisplay docDisplay,
                        FileTypeRegistry fileTypeRegistry,
                        Commands commands)
   {
      fileTypeRegistry_ = fileTypeRegistry;
      commands_ = commands;
      docDisplay_ = docDisplay; 
      docDisplay_.setReadOnly(true);
      
      findReplace_ = new TextEditingTargetFindReplace(
            new TextEditingTargetFindReplace.Container() {

               @Override
               public AceEditor getEditor()
               {
                  return (AceEditor)docDisplay_;
               }

               @Override
               public void insertFindReplace(FindReplaceBar findReplaceBar)
               {
                  panel_.insertNorth(findReplaceBar,
                                     findReplaceBar.getHeight(),
                                     null);
               }

               @Override
               public void removeFindReplace(FindReplaceBar findReplaceBar)
               {
                  panel_.remove(findReplaceBar);
               }
              
            },
            false); // don't show replace UI
      
      panel_ = new PanelWithToolbars(createToolbar(),
                                     null,
                                     docDisplay_.asWidget(),
                                     null);
      panel_.setSize("100%", "100%");
      
      initWidget(panel_);
   }
   
   @Override
   public void showFile(FileSystemItem file, String commitId, String contents)
   {
      docDisplay_.setCode(contents, false);  
      
      adaptToFileType(fileTypeRegistry_.getTextTypeForFile(file));
      
      // header widget has icon + label
      HorizontalPanel panel = new HorizontalPanel();
     
      Image imgFile = new Image(fileTypeRegistry_.getIconForFile(file));
      imgFile.addStyleName(RES.styles().captionIcon());
      panel.add(imgFile);
      
      Label lblCaption = new Label(file.getPath() + " @ " + commitId);
      lblCaption.addStyleName(RES.styles().captionLabel());
      panel.add(lblCaption);
      
      new FullscreenPopupPanel(panel,asWidget()).center();
   }
    
   private Toolbar createToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      toolbar.addLeftWidget(new ToolbarButton(
         "Save As...", 
         commands_.saveSourceDoc().getImageResource(),
         new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
              
               
            }
            
         }));
      toolbar.addLeftSeparator();
      
      toolbar.addLeftWidget(new ToolbarButton(
         null,
         commands_.printSourceDoc().getImageResource(),
         new ClickHandler() {

            @Override
            public void onClick(ClickEvent event)
            {
               docDisplay_.print();
            }
            
         }));
      toolbar.addLeftSeparator();
      
      toolbar.addLeftWidget(findReplace_.createFindReplaceButton());
      
      return toolbar;
   }  
   
   @Override
   public void onActivate()
   {
      docDisplay_.onActivate();
      
   }

   @Override
   public void adaptToFileType(TextFileType fileType)
   {
      docDisplay_.setFileType(fileType, true);
   }

   @Override
   public void setFontSize(double size)
   {
      docDisplay_.setFontSize(size);
   }

   @Override
   public Widget asWidget()
   {
      return this;
   }

   @Override
   public DocDisplay getDocDisplay()
   {
      return docDisplay_;
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("ViewFilePanel.css")
      Styles styles();
   }
   
   public interface Styles extends CssResource
   {
      String captionIcon();
      String captionLabel();
   }
   
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
  
   private final FileTypeRegistry fileTypeRegistry_;
   private final Commands commands_;
   private final DocDisplay docDisplay_;
   
   private final PanelWithToolbars panel_;
   private final TextEditingTargetFindReplace findReplace_;
   
   private static final Resources RES = GWT.<Resources>create(Resources.class);
}
