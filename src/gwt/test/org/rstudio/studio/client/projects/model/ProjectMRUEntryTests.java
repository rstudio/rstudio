/*
 * ProjectMRUEntryTests.java
 *
 * Copyright (C) 2023 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.model;

import com.google.gwt.junit.client.GWTTestCase;

public class ProjectMRUEntryTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testConstructorNullPaths()
   {
      String projectFilePath = null;
      String projectName = null;
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals("", entry.getProjectFilePath());
      assertEquals("", entry.getProjectName());
   }

   public void testConstructorEmptyPaths()
   {
      String projectFilePath = "";
      String projectName = "";
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals(projectFilePath, entry.getProjectFilePath());
      assertEquals(projectName, entry.getProjectName());
   }

   public void testConstructorNonEmptyPaths()
   {
      String projectFilePath = "foo";
      String projectName = "bar";
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals(projectFilePath, entry.getProjectFilePath());
      assertEquals(projectName, entry.getProjectName());
   }

   public void testConstructorNullMRU()
   {
      String mruEntry = null;
      ProjectMRUEntry entry = new ProjectMRUEntry(mruEntry);
      assertEquals("", entry.getProjectFilePath());
      assertEquals("", entry.getProjectName());
   }

   public void testConstructorEmptyMRU()
   {
      String mruEntry = "";
      ProjectMRUEntry entry = new ProjectMRUEntry(mruEntry);
      assertEquals("", entry.getProjectFilePath());
      assertEquals("", entry.getProjectName());
   }

   public void testConstructorNoTab()
   {
      String mruEntry = "foo";
      ProjectMRUEntry entry = new ProjectMRUEntry(mruEntry);
      assertEquals(mruEntry, entry.getProjectFilePath());
      assertEquals("", entry.getProjectName());
   }

   public void testConstructorEmptyProjectName()
   {
      String mruEntry = "foo\t";
      ProjectMRUEntry entry = new ProjectMRUEntry(mruEntry);
      assertEquals("foo", entry.getProjectFilePath());
      assertEquals("", entry.getProjectName());
   }

   public void testConstructorNonEmptyProjectName()
   {
      String mruEntry = "foo\tbar";
      ProjectMRUEntry entry = new ProjectMRUEntry(mruEntry);
      assertEquals("foo", entry.getProjectFilePath());
      assertEquals("bar", entry.getProjectName());
   }

   public void testGetMRUValueNullProjectFilePath()
   {
      String projectFilePath = null;
      String projectName = "bar";
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals("", entry.getMRUValue());
   }

   public void testGetMRUValueEmptyProjectFilePath()
   {
      String projectFilePath = "";
      String projectName = "bar";
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals("", entry.getMRUValue());
   }

   public void testGetMRUValueNullProjectName()
   {
      String projectFilePath = "foo";
      String projectName = null;
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals("foo", entry.getMRUValue());
   }

   public void testGetMRUValueEmptyProjectName()
   {
      String projectFilePath = "foo";
      String projectName = "";
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals("foo", entry.getMRUValue());
   }

   public void testGetMRUValueNonEmptyProjectName()
   {
      String projectFilePath = "foo";
      String projectName = "bar";
      ProjectMRUEntry entry = new ProjectMRUEntry(projectFilePath, projectName);
      assertEquals("foo\tbar", entry.getMRUValue());
   }

   public void testGetMRUValueEmptyMRU()
   {
      String mruEntry = "";
      ProjectMRUEntry entry = new ProjectMRUEntry(mruEntry);
      assertEquals("", entry.getMRUValue());
   }
}
