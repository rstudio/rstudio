/*
 * YamlUtils.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rmarkdown.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class YamlUtils
{
   private static class YamlTreeNode
   {
      public YamlTreeNode(String line)
      {
         yamlLine = line;
         indentLevel = getIndentLevel(line);
      }
      
      public void addChild(YamlTreeNode child)
      {
         children.add(child);
         child.parent = this;
      }
      
      public String getKey()
      {
         RegExp key = RegExp.compile("^\\s*([^:]+):");
         MatchResult result = key.exec(yamlLine);
         return result.getGroup(1);
      }

      public String yamlLine;
      public int indentLevel = 0;
      public YamlTreeNode parent = null;
      public List<YamlTreeNode> children = new ArrayList<YamlTreeNode>();
      
      private static int getIndentLevel(String line)
      {
         RegExp whitespace = RegExp.compile("^\\s*");
         MatchResult result = whitespace.exec(line);
         return result.getGroup(0).length();
      }
   }
   
   public static String reorderYaml(String yaml, List<String> orderedKeys)
   {
      // Sort the YAML lines into a tree
      YamlTreeNode tree = createYamlTree(yaml);
      YamlTreeNode parent = findParentNodeOfKey(tree, orderedKeys.get(0));
      
      // Move around subtrees to match the given list of ordered keys 
      // (swap subtrees into a position matching that specified)
      for (int i = 0; i < orderedKeys.size(); i++)
      {
         for (int j = i; j < parent.children.size(); j++)
         {
            YamlTreeNode child = parent.children.get(j);
            if (orderedKeys.get(i) == child.getKey())
            {
               YamlTreeNode previousChild = parent.children.get(i);
               parent.children.set(i, child);
               parent.children.set(j, previousChild);
            }
         }
      }

      // Re-emit the lines in the new order
      return yamlFromTree(tree);
   }

   private static YamlTreeNode createYamlTree(String yaml)
   {
      String[] lines = yaml.split("\n");
      YamlTreeNode root = new YamlTreeNode("");
      root.indentLevel = -2;
      YamlTreeNode currentParent = root;
      YamlTreeNode lastNode = root;
      int currentIndentLevel = 0;
      for (String line: lines)
      {
         YamlTreeNode child = new YamlTreeNode(line);
         if (child.indentLevel > currentIndentLevel)
         {
            // Descending: we're recording children of the previous line
            currentParent = lastNode;
         }
         else if (child.indentLevel < currentIndentLevel)
         {
            // Ascending: find the first parent node in the hierarchy that has
            // an indent level less than this node's
            do
            {
               currentParent = currentParent.parent;
            }
            while (currentParent.indentLevel >= child.indentLevel);
         }

         currentIndentLevel = child.indentLevel;
         currentParent.addChild(child);
         lastNode = child;
      }
      return root;
   }
   
   private static String yamlFromTree(YamlTreeNode root)
   {
      String yaml = "";
      for (YamlTreeNode child: root.children)
      {
         yaml += (child.yamlLine + '\n');
         yaml += yamlFromTree(child);
      }
      return yaml;
   }
   
   private static YamlTreeNode findParentNodeOfKey(YamlTreeNode root, 
                                                   String key)
   {
      for (YamlTreeNode child: root.children)
      {
         if (child.getKey() == key)
            return root;
         YamlTreeNode node = findParentNodeOfKey(child, key);
         if (node != null)
            return node;
      }
      return null;
   }
}
