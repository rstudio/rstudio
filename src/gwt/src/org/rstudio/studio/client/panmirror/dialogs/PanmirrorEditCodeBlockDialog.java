/*
 * PanmirrorEditCodeBlockDialog.java
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


package org.rstudio.studio.client.panmirror.dialogs;


import com.google.gwt.aria.client.Roles;

import java.util.ArrayList;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorCodeBlockProps;

import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditCodeBlockDialog extends ModalDialog<PanmirrorCodeBlockProps>
{ 
   public PanmirrorEditCodeBlockDialog(
               PanmirrorCodeBlockProps codeBlock,
               String[] languages,
               OperationWithInput<PanmirrorCodeBlockProps> operation)
   {
      super("Code Block", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      
      
      VerticalTabPanel langTab = new VerticalTabPanel(ElementIds.VISUAL_MD_CODE_BLOCK_TAB_LANGUAGE);
      langTab.addStyleName(RES.styles().dialog());
      langTab.add(new FormLabel("Language:"));
      lang_ = new SuggestBox(languagesSuggestOracle(languages));
      lang_.getElement().setId(ElementIds.VISUAL_MD_CODE_BLOCK_LANG);
      lang_.setText(codeBlock.lang);
      PanmirrorDialogsUtil.setFullWidthStyles(lang_);
      langTab.add(lang_);
      
      
      VerticalTabPanel attributesTab = new VerticalTabPanel(ElementIds.VISUAL_MD_CODE_BLOCK_TAB_ATTRIBUTES);
      attributesTab.addStyleName(RES.styles().dialog());
      editAttr_ =  new PanmirrorEditAttrWidget();   
      editAttr_.setAttr(codeBlock);
      attributesTab.add(editAttr_);
     
      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("Image");
      tabPanel.addStyleName(RES.styles().linkDialogTabs());
      tabPanel.add(langTab, "Language", langTab.getBasePanelId());
      tabPanel.add(attributesTab, "Attributes", attributesTab.getBasePanelId());
      tabPanel.selectTab(0);
      
      
      mainWidget_ = tabPanel;
      
      
      
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   public void focusFirstControl()
   {
      lang_.setFocus(true);
   }
   
   @Override
   protected PanmirrorCodeBlockProps collectInput()
   {
      PanmirrorCodeBlockProps result = new PanmirrorCodeBlockProps();
      PanmirrorAttrProps attr = editAttr_.getAttr();
      result.id = attr.id;
      result.classes = attr.classes;
      result.keyvalue = attr.keyvalue;
      result.lang = lang_.getText().trim();
      return result;
   }


   @Override
   protected boolean validate(PanmirrorCodeBlockProps input)
   {
      if (StringUtil.isNullOrEmpty(input.lang))
      {
         GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
         globalDisplay.showErrorMessage(
            "Language Required", 
            "You must specify a language for the code block."
         );
         lang_.setFocus(true);
         return false;
      } 
      else 
      {
         return true;
      }
   }
   
   private SuggestOracle languagesSuggestOracle(String[] languages) 
   {
      return new SuggestOracle() 
      {

         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            String query = request.getQuery();
            
            ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();
            for (int i=0; i<languages.length; i++)
            {
               String language = languages[i];
               if (language.startsWith(query))
                  suggestions.add(new LanguageSuggestion(language));
               
               if (suggestions.size() > request.getLimit())
                  break;
            }
            
            callback.onSuggestionsReady(request, new Response(suggestions));
         }
         
         class LanguageSuggestion implements Suggestion
         {
            public LanguageSuggestion(String language)
            {
               language_ = language;
            }

            @Override
            public String getDisplayString()
            {
               return language_;
            }

            @Override
            public String getReplacementString()
            {
               return language_;
            }
            
            private final String language_;
            
         }
         
      };
   }
   

   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

   
   private Widget mainWidget_; 
   
   private SuggestBox lang_;
   private PanmirrorEditAttrWidget editAttr_;
  
}
