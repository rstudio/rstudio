/*
 * TextEditingTargetPresentation2Helper.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocation;
import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocationItem;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.YamlTree;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.core.client.JsArray;

public class TextEditingTargetPresentation2Helper
{
   
   public TextEditingTargetPresentation2Helper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
   }
   
   public PresentationEditorLocation getPresentationEditorLocation()
   {
      JsArray<PresentationEditorLocationItem> items = JsArray.createArray().cast();
      
      boolean foundCursor = false;
      Token lastRowToken = null;
      for (int i=0; i<docDisplay_.getRowCount(); i++)
      { 
         // determine the current token and type
         Token rowToken = docDisplay_.getTokenAt(i, 0);
         if (rowToken == null)
            continue;
         String type = rowToken.getType();
         if (type == null)
            continue;
         
         // horizontal rule
         if (type.equals("constant.hr"))
         {
            items.push(PresentationEditorLocationItem.hr(i));
         }
         
         // headings can be atx style or can actually be an 
         // hr without a caption
         else if (type.startsWith("markup.heading."))
         {
            int level = StringUtil.parseInt(
               type.substring(type.length()-1, type.length()),
               0
            );
            // atx style
            if (rowToken.getValue().trim().startsWith("#"))
            {
               items.push(PresentationEditorLocationItem.heading(level, i));
            }
            else if (rowToken.getValue().startsWith("---") &&
                     lastRowToken != null &&
                     !lastRowToken.getType().startsWith("markup.heading"))
            {
               items.push(PresentationEditorLocationItem.hr(i));
            }
         }
         
         // if the cursor is on this line then add the cursor token
         Position linePos = Position.create(i, 0);
         if (!foundCursor && linePos.isAfterOrEqualTo(docDisplay_.getCursorPosition()))
         {
            foundCursor = true;
            items.push(PresentationEditorLocationItem.cursor(i));
         }
         
         //  note last row token
         lastRowToken = docDisplay_.getTokenAt(i, 0);
      }
        
      // add title at the beginning if we have one
      String yaml = YamlFrontMatter.getFrontMatter(docDisplay_);
      YamlTree tree = new YamlTree(yaml);
      String title = tree.getKeyValue("title");
      if (!StringUtil.isNullOrEmpty(title))
         items.unshift(PresentationEditorLocationItem.title(0));
          
      return PresentationEditorLocation.create(items);
   }
   
   public void navigateToPresentationEditorLocation(PresentationEditorLocation location)
   {
      // get the current set of locations in the editor (filtering out the cursor)
      JsArray<PresentationEditorLocationItem> editorItemsWithCursor = 
         getPresentationEditorLocation().getItems();
      JsArray<PresentationEditorLocationItem> editorItems = JsArray.createArray().cast();
      for (int i=0; i<editorItemsWithCursor.length(); i++) {
         PresentationEditorLocationItem item = editorItemsWithCursor.get(i);
         if (!item.getType().equals(PresentationEditorLocationItem.CURSOR)) {
            editorItems.push(item);
         }
      }
      
      // find the index of the cursor in the passed location
      int cursorIdx = -1;
      JsArray<PresentationEditorLocationItem> locationItems = location.getItems();
      for (int i=0; i<locationItems.length(); i++)
      {
         if (locationItems.get(i).getType().equals(PresentationEditorLocationItem.CURSOR))
         {
            cursorIdx = i;
            break;
         }
      }
      
      // navigate to the location right above the cursor
      if (cursorIdx > 0)
      {
         PresentationEditorLocationItem item = editorItems.get(cursorIdx - 1);
         if (item != null)
         {
            docDisplay_.navigateToPosition(SourcePosition.create(item.getRow(), 0), true);
         }
      }
      
      
   }
  
   private final DocDisplay docDisplay_;
}
