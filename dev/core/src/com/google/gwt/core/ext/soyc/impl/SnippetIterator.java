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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.linker.CompilationAnalysis.Snippet;
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.core.ext.soyc.Story;

import java.util.Iterator;

/**
 * Uses a list of StoryImpls present a sequence of Snippets by synthesizing
 * Range objects based on the length of the StoryImpls.
 */
public class SnippetIterator implements Iterator<Snippet> {
  /**
   * An Iterator over the backing object.
   */
  private final Iterator<StoryImpl> iter;

  /**
   * The starting position for the next Range object generated.
   */
  private int start = 0;

  public SnippetIterator(Iterable<StoryImpl> stories) {
    iter = stories.iterator();
  }

  public boolean hasNext() {
    return iter.hasNext();
  }

  public Snippet next() {
    final StoryImpl story = iter.next();
    final Range range = new Range(start, start + story.getLength());
    start += story.getLength();

    return new Snippet() {
      public Range getRange() {
        return range;
      }

      public Story getStory() {
        return story;
      }
    };
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}