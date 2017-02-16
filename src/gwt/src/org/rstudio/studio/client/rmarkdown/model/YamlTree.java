/*
 * YamlTree.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.Debug;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class YamlTree
{
   private class YamlTreeNode
   {
      public YamlTreeNode(String line)
      {
         // the YAML package outputs tildes to represent null values; these 
         // are valid YAML but "null" is easier to read for R users 
         if (line.endsWith(": ~"))
         {
            line = line.substring(0, line.length() - 1) + "null";
         }
         yamlLine = line;
         key = getKey(line);
         indentLevel = getIndentLevel();
      }
      
      // add a child node; returns true iff the node was added (we may choose
      // to treat the child as a continuation)
      public boolean addChild(YamlTreeNode child)
      {
         // if this node has a key, add it as a tree node; otherwise, add its
         // line data to this node (as a multi-line continuation)
         if (child.key.length() > 0)
         {
            children.add(child);
            child.parent = this;
            return true;
         }

         yamlLine += ("\n" + child.yamlLine);
         return false;
      }
      
      public void adopt(YamlTree tree)
      {
         children.clear();
         adoptNode(tree.root_, true);
      }
      
      public void adoptNode(YamlTreeNode node, boolean isRoot)
      {
         for (YamlTreeNode child: node.children)
         {
            // increase the indentation level of the children to match our own
            child.indentLevel += indentLevel + 2;
            child.yamlLine = getIndent() + "  " + child.yamlLine;
            
            // if this is the root node, adopt is children as our own
            if (isRoot)
            {
               children.add(child);
            }
            adoptNode(child, false);
         }
      }
      
      public String getIndent()
      {
         // consider the list element indicator (-) to be part of the node's
         // indentation, to prevent list continuations from being treated as
         // sibling nodes
         RegExp whitespace = RegExp.compile("^\\s*-?\\s*");
         MatchResult result = whitespace.exec(yamlLine);
         if (result == null)
            return "";
         else
            return result.getGroup(0);
      }
      
      public String getValue()
      {
         int idx = yamlLine.indexOf(":");
         if (idx < 0)
            return "";
         return yamlLine.substring(idx + 2).trim();
      }
      
      public void setValue(String value)
      {
         int idx = yamlLine.indexOf(":");
         if (idx < 0)
            return;
         yamlLine = yamlLine.substring(0, idx + 2) + value;
      }
      
      public String yamlLine;
      public String key;
      public int indentLevel = 0;
      public YamlTreeNode parent = null;
      public List<YamlTreeNode> children = new ArrayList<YamlTreeNode>();
      
      private String getKey(String line)
      {
         RegExp keyReg = RegExp.compile("^\\s*([^:]+(?:::[^:]+)?):");
         MatchResult result = keyReg.exec(line);
         if (result == null)
            return "";
          else
            return result.getGroup(1).trim();
      }

      private int getIndentLevel()
      {
         return getIndent().length();
      }
   }
   
   public YamlTree(String yaml)
   {
      root_ = createYamlTree(yaml);
      keyMap_ = new HashMap<String, YamlTreeNode>();
      createKeyMap(root_, keyMap_);
   }
   
   public String toString()
   {
      return yamlFromTree(root_);
   }
   
   public void reorder(List<String> orderedKeys)
   {
      // Sort the YAML lines into a tree
      if (!keyMap_.containsKey(orderedKeys.get(0)))
         return;
      YamlTreeNode parent = keyMap_.get(orderedKeys.get(0)).parent;
      
      // Move around subtrees to match the given list of ordered keys 
      // (swap subtrees into a position matching that specified)
      for (int i = 0; i < orderedKeys.size(); i++)
      {
         for (int j = i; j < parent.children.size(); j++)
         {
            YamlTreeNode child = parent.children.get(j);
            if (orderedKeys.get(i) == child.key)
            {
               YamlTreeNode previousChild = parent.children.get(i);
               parent.children.set(i, child);
               parent.children.set(j, previousChild);
            }
         }
      }
   }

   public List<String> getChildKeys(String parentKey)
   {
      try
      {
         if (!keyMap_.containsKey(parentKey))
            return null;
         YamlTreeNode parent = keyMap_.get(parentKey);
         ArrayList<String> result = new ArrayList<String>();
         for (YamlTreeNode child: parent.children)
         {
            result.add(child.key);
         }
         return result;
      }
      catch (Exception e)
      {
         // case 3826: it's not clear why but we've seen an exception thrown
         // from the above in non-reproducible circumstances; handle gracefully
         Debug.log("YamlTree: Couldn't get child keys for " + parentKey + 
                   " (" + e.getMessage() + ")");
      }
      return null;
   }
   
   // add a simple YAML value to the tree under the given parent, or under
   // the root if parentKey is null.
   public void addYamlValue(String parentKey, String key, String value)
   {
      // if a key was specified but doesn't exist in the flattened map, abort
      if (parentKey != null && !keyMap_.containsKey(parentKey))
         return;
      
      String line = key + ": " + value;
      YamlTreeNode parent = parentKey == null ? root_ : keyMap_.get(parentKey);
      
      // check to see if we should be replacing a value rather than adding
      YamlTreeNode child = null;
      for (YamlTreeNode childNode: parent.children)
      {
         if (childNode.key == key)
         {
            // found an existing child node
            child = childNode;
            
            // set its value to the one supplied by the caller and return
            child.setValue(value);
            return;
         }
      }
      
      // no existing value, create a wholly new node
      child = parentKey == null ? new YamlTreeNode(line) : 
            new YamlTreeNode(parent.getIndent() + "  " + line);
      keyMap_.put(key, child);
      parent.addChild(child);
   }
   
   public String getKeyValue(String key)
   {
      if (keyMap_.containsKey(key))
         return keyMap_.get(key).getValue();
      return "";
   }
   
   public String getChildValue(String parentKey, String childKey)
   {
      for (YamlTreeNode parent: root_.children)
      {
         if (parent.key == parentKey)
         {
            for (YamlTreeNode child: parent.children)
            {
               if (child.key == childKey)
               {
                  return child.getValue();
               }
            }
         }
      }
      return "";
   }
   
   public boolean containsKey(String key)
   {
      return keyMap_.containsKey(key);
   }

   public void setKeyValue(String key, String value)
   {
      if (keyMap_.containsKey(key))
         keyMap_.get(key).setValue(value);
   }
   
   public void setKeyValue(String key, YamlTree subtree)
   {
      if (keyMap_.containsKey(key))
      {
         YamlTreeNode node = keyMap_.get(key);
         node.adopt(subtree);
         node.setValue("");
         keyMap_.clear();
         createKeyMap(root_, keyMap_);
      }
   }
   
   public void clearChildren(String key)
   {
      if (keyMap_.containsKey(key))
      {
         keyMap_.get(key).children.clear();
         keyMap_.clear();
         createKeyMap(root_, keyMap_);
      }
   }
   
   private YamlTreeNode createYamlTree(String yaml)
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
            while (currentParent != null &&
                   currentParent.indentLevel >= child.indentLevel);
            
            // if we unwound all the way to the top, use the root as the parent
            if (currentParent == null)
               currentParent = root;
         }

         currentIndentLevel = child.indentLevel;
         if (currentParent.addChild(child))
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
   
   private static void createKeyMap(YamlTreeNode root, 
                                    Map<String, YamlTreeNode> output)
   {
      for (YamlTreeNode child: root.children)
      {
         String key = child.key;
         // add this key if it doesn't already exist, or if it does exist and
         // this key is closer to the root than the existing key
         if (key.length() > 0 &&
             (!output.containsKey(key) ||
              child.indentLevel < output.get(key).indentLevel))
         {
            output.put(child.key, child);
         }
         createKeyMap(child, output);
      }
   }
   
   // A flattened version of the tree as a list of key-value pairs. In the 
   // case where multiple keys exist, the key closest to the root is stored.
   private final Map<String, YamlTreeNode> keyMap_;
   private final YamlTreeNode root_;
}
