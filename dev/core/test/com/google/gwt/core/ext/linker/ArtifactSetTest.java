/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.linker.impl.StandardScriptReference;
import com.google.gwt.core.ext.linker.impl.StandardStylesheetReference;

import junit.framework.TestCase;

import java.util.SortedSet;

/**
 * Tests for {@link ArtifactSet}.
 */
public class ArtifactSetTest extends TestCase {

  public void testScriptOrder() {
    StandardScriptReference fooScript = new StandardScriptReference("foo", 0);
    StandardScriptReference barScript = new StandardScriptReference("bar", 1);
    assertTrue(fooScript.compareTo(barScript) < 0);
    assertTrue(barScript.compareTo(fooScript) > 0);
    assertTrue(fooScript.compareTo(fooScript) == 0);
    assertTrue(barScript.compareTo(barScript) == 0);

    {
      ArtifactSet set = new ArtifactSet();
      // Add in order.
      set.add(fooScript);
      set.add(barScript);
      assertEquals(2, set.size());
      assertSame(fooScript, set.first());
      assertSame(barScript, set.last());

      SortedSet<StandardScriptReference> found = set.find(StandardScriptReference.class);
      assertEquals(2, found.size());
      assertSame(fooScript, found.first());
      assertSame(barScript, found.last());
    }
    {
      ArtifactSet set = new ArtifactSet();
      // Reversed add order.
      set.add(barScript);
      set.add(fooScript);
      assertEquals(2, set.size());
      assertSame(fooScript, set.first());
      assertSame(barScript, set.last());

      SortedSet<StandardScriptReference> found = set.find(StandardScriptReference.class);
      assertEquals(2, found.size());
      assertSame(fooScript, found.first());
      assertSame(barScript, found.last());
    }

  }

  public void testStyleOrder() {
    StandardStylesheetReference fooStyle = new StandardStylesheetReference(
        "foo", 0);
    StandardStylesheetReference barStyle = new StandardStylesheetReference(
        "bar", 1);
    assertTrue(fooStyle.compareTo(barStyle) < 0);
    assertTrue(barStyle.compareTo(fooStyle) > 0);
    assertTrue(fooStyle.compareTo(fooStyle) == 0);
    assertTrue(barStyle.compareTo(barStyle) == 0);

    {
      ArtifactSet set = new ArtifactSet();
      // Add in order.
      set.add(fooStyle);
      set.add(barStyle);
      assertEquals(2, set.size());
      assertSame(fooStyle, set.first());
      assertSame(barStyle, set.last());

      SortedSet<StandardStylesheetReference> found = set.find(StandardStylesheetReference.class);
      assertEquals(2, found.size());
      assertSame(fooStyle, found.first());
      assertSame(barStyle, found.last());
    }
    {
      ArtifactSet set = new ArtifactSet();
      // Reversed add order.
      set.add(barStyle);
      set.add(fooStyle);
      assertEquals(2, set.size());
      assertSame(fooStyle, set.first());
      assertSame(barStyle, set.last());

      SortedSet<StandardStylesheetReference> found = set.find(StandardStylesheetReference.class);
      assertEquals(2, found.size());
      assertSame(fooStyle, found.first());
      assertSame(barStyle, found.last());
    }

  }
}
