/*
 * VisualModeEditingLocation.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingLocation;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocation;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingOutlineLocationItem;
import org.rstudio.studio.client.panmirror.outline.PanmirrorOutlineItemType;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.core.client.JsArray;

// save and restore editing location

public class VisualModeEditingLocation
{
   public static final String RMD_VISUAL_MODE_LOCATION = "rmdVisualModeLocation";   
   
   public VisualModeEditingLocation(DocUpdateSentinel docUpdateSentinel,
                                    DocDisplay docDisplay)
   {
      docUpdateSentinel_ = docUpdateSentinel;
      docDisplay_ = docDisplay;
   }
   
   public void saveEditingLocation(PanmirrorEditingLocation location)
   {
      String locationProp = + location.pos + ":" + location.scrollTop; 
      docUpdateSentinel_.setProperty(RMD_VISUAL_MODE_LOCATION, locationProp);
   }
   
   
   public PanmirrorEditingLocation savedEditingLocation()
   {
      String location = docUpdateSentinel_.getProperty(RMD_VISUAL_MODE_LOCATION, null);
      if (StringUtil.isNullOrEmpty(location))
         return null;
      
      String[] parts = location.split(":");
      if (parts.length != 2)
         return null;
      
      try
      {
         PanmirrorEditingLocation editingLocation = new PanmirrorEditingLocation();
         editingLocation.pos = Integer.parseInt(parts[0]);
         editingLocation.scrollTop = Integer.parseInt(parts[1]);
         return editingLocation;
      }
      catch(Exception ex)
      {
         Debug.logException(ex);
         return null;
      }
      
   }
   
   public PanmirrorEditingOutlineLocation getSourceOutlineLocation()
   {
      // if we are at the very top of the file then this is a not a good 'hint'
      // for where to navigate to, in that case return null
      Position cursorPosition = docDisplay_.getCursorPosition();
      if (cursorPosition.getRow() == 0 && cursorPosition.getColumn() == 0)
         return null;
      
      // if we don't have an outline then return null
      if (docDisplay_.getScopeTree().length() == 0)
         return null;
      
      // build the outline
      ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>> outlineItems = 
         new ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>>();
      buildOutlineLocation(docDisplay_.getScopeTree(), outlineItems);
      
      // return the location, set the active item by scanning backwards until
      // we find an item with a position before the cursor
      boolean foundActive = false;
      Position cursorPos = docDisplay_.getCursorPosition();
      ArrayList<PanmirrorEditingOutlineLocationItem> items = new ArrayList<PanmirrorEditingOutlineLocationItem>();
      for (int i = outlineItems.size() - 1; i >= 0; i--) 
      {
         Pair<PanmirrorEditingOutlineLocationItem, Scope> outlineItem = outlineItems.get(i);
         PanmirrorEditingOutlineLocationItem item = outlineItem.first;
         Scope scope = outlineItem.second;
         if (!foundActive && scope.getPreamble().isBeforeOrEqualTo(cursorPos))
         {
            item.active = true;
            foundActive = true;
         }
         items.add(0, item);
      }
   
      PanmirrorEditingOutlineLocation location = new PanmirrorEditingOutlineLocation();
      location.items = items.toArray(new PanmirrorEditingOutlineLocationItem[] {});
      return location;
   }
   
   public void setSourceOutlineLocation(PanmirrorEditingOutlineLocation location)
   {
      // if we don't have an outline then bail
      if (docDisplay_.getScopeTree().length() == 0)
         return;
      
      // build flattened version of scope tree which includes outline items
      ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>> outlineItems = 
         new ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>>();
      buildOutlineLocation(docDisplay_.getScopeTree(), outlineItems);
      
      // if the lengths differ then bail
      if (outlineItems.size() != location.items.length)
         return;
      
      // iterate over the items until we find the active one (bail if our own
      // outline/scope-tree representation doesn't match the one passed to us)
      for (int i = 0; i<outlineItems.size(); i++)
      {
         // structure check
         Pair<PanmirrorEditingOutlineLocationItem, Scope> outlineItem = outlineItems.get(i);
         if (!outlineLocationsAreSimilar(outlineItem.first, location.items[i]))
            break;
         
         // check for active
         if (location.items[i].active) 
         {
            Scope scope = outlineItem.second;
            SourcePosition position = SourcePosition.create(scope.getPreamble().getRow(), 
                                                            scope.getPreamble().getColumn());
            docDisplay_.navigateToPosition(position, false);
            break;
         }  
      }  
   }
   
   private void buildOutlineLocation(JsArray<Scope> scopes, 
                                     ArrayList<Pair<PanmirrorEditingOutlineLocationItem, Scope>> outlineItems)
   {
      for (int i = 0; i<scopes.length(); i++)
      {
         // get scope
         Scope scope = scopes.get(i);
         
         // create item + default values
         PanmirrorEditingOutlineLocationItem item = new PanmirrorEditingOutlineLocationItem();
         item.level = scope.getDepth();
         item.title = scope.getLabel();
         item.active = false;
         
         // process yaml, headers, and chunks
         if (scope.isYaml())
         {
            item.type = PanmirrorOutlineItemType.YamlMetadata;
            outlineItems.add(new Pair<PanmirrorEditingOutlineLocationItem, Scope>(item, scope));
         }
         else if (scope.isMarkdownHeader())
         {
            item.type = PanmirrorOutlineItemType.Heading;
            outlineItems.add(new Pair<PanmirrorEditingOutlineLocationItem, Scope>(item, scope));
            buildOutlineLocation(scope.getChildren(), outlineItems);
         }
         else if (scope.isChunk())
         {
            item.type = PanmirrorOutlineItemType.RmdChunk;
            item.title = scope.getChunkLabel();
            outlineItems.add(new Pair<PanmirrorEditingOutlineLocationItem, Scope>(item, scope));
         }
      }
   }
   
   private boolean outlineLocationsAreSimilar(PanmirrorEditingOutlineLocationItem a, PanmirrorEditingOutlineLocationItem b)
   {
      return a.type.equals(b.type) && a.level == b.level;
   }
   
   private final DocUpdateSentinel docUpdateSentinel_;
   private final DocDisplay docDisplay_;
   
}
