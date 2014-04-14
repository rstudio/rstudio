/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.soyc.ClassMember;
import com.google.gwt.core.ext.soyc.Member;
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.jjs.Correlation;
import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

/**
 * Records {@link Story}s to a file for Compile Reports.
 */
public class StoryRecorder {

  /**
   * Associates a SourceInfo with a Range.
   */
  private static class RangeInfo {
    public final SourceInfo info;
    public final Range range;

    public RangeInfo(Range range, SourceInfo info) {
      assert range != null;
      assert info != null;
      this.range = range;
      this.info = info;
    }
  }

  private static final int MAX_STRING_BUILDER_SIZE = 65536;

  /**
   * Used to record dependencies of a program.
   */
  public static void recordStories(TreeLogger logger, OutputStream out,
      List<Map<Range, SourceInfo>> sourceInfoMaps, String[] js) {
    new StoryRecorder().recordStoriesImpl(logger, out, sourceInfoMaps, js);
  }

  private StringBuilder builder;

  private int curHighestFragment = 0;

  private OutputStream gzipStream;

  private String[] js;

  /**
   * Used by {@link #popAndRecord(Stack)} to determine start and end ranges.
   */
  private int lastEnd = 0;

  /**
   * This is a class field for convenience, but it should be deleted at the end
   * of the constructor.
   */
  private transient Map<Correlation, Member> membersByCorrelation =
      new IdentityHashMap<Correlation, Member>();

  /**
   * This is a class field for convenience, but it should be deleted at the end
   * of the constructor.
   */
  private transient Map<SourceInfo, StoryImpl> storyCache =
      new IdentityHashMap<SourceInfo, StoryImpl>();

  private StoryRecorder() {
  }

  protected void recordStoriesImpl(TreeLogger logger, OutputStream out,
      List<Map<Range, SourceInfo>> sourceInfoMaps, String[] js) {

    logger = logger.branch(TreeLogger.INFO, "Creating Stories file for the compile report");

    this.js = js;

    try {
      builder = new StringBuilder(MAX_STRING_BUILDER_SIZE * 2);
      gzipStream = new GZIPOutputStream(out);

      /*
       * Don't retain beyond the constructor to avoid lingering references to
       * AST nodes.
       */
      MemberFactory memberFactory = new MemberFactory();

      // Record what we've seen so far
      TreeSet<ClassMember> classesMutable = new TreeSet<ClassMember>(Member.SOURCE_NAME_COMPARATOR);
      Set<SourceInfo> sourceInfoSeen = new HashSet<SourceInfo>();

      builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<soyc>\n<stories>\n");

      int fragment = 0;
      for (Map<Range, SourceInfo> sourceInfoMap : sourceInfoMaps) {
        lastEnd = 0;
        analyzeFragment(memberFactory, classesMutable, sourceInfoMap, sourceInfoSeen, fragment++);

        // Flush output to improve memory locality
        flushOutput();
      }

      builder.append("</stories>\n</soyc>\n");

      /*
       * Clear the member fields that we don't need anymore to allow GC of the
       * SourceInfo objects
       */
      membersByCorrelation = null;
      storyCache = null;

      Util.writeUtf8(builder, gzipStream);
      Utility.close(gzipStream);

      logger.log(TreeLogger.INFO, "Done");
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Could not write dependency file.", e);
    }
  }

  private void analyzeFragment(MemberFactory memberFactory, TreeSet<ClassMember> classesMutable,
      Map<Range, SourceInfo> sourceInfoMap, Set<SourceInfo> sourceInfoSeen, int fragment)
      throws IOException {
    /*
     * We want to iterate over the Ranges so that enclosing Ranges come before
     * their enclosed Ranges...
     */
    Range[] dependencyOrder = sourceInfoMap.keySet().toArray(new Range[sourceInfoMap.size()]);
    Arrays.sort(dependencyOrder, Range.DEPENDENCY_ORDER_COMPARATOR);

    Stack<RangeInfo> dependencyScope = new Stack<RangeInfo>();
    for (Range range : dependencyOrder) {
      SourceInfo info = sourceInfoMap.get(range);
      assert info != null;

      // Infer dependency information
      if (!dependencyScope.isEmpty()) {

        /*
         * Pop frames until we get back to a container, using this as a chance
         * to build up our list of non-overlapping Ranges to report back to the
         * user.
         */
        while (!dependencyScope.peek().range.contains(range)) {
          popAndRecord(dependencyScope, fragment);
        }
      }

      // Possibly create and record Members
      if (!sourceInfoSeen.contains(info)) {
        sourceInfoSeen.add(info);
        for (Correlation c : info.getCorrelations()) {
          if (c == null) {
            continue;
          }
          if (membersByCorrelation.containsKey(c)) {
            continue;
          }

          switch (c.getAxis()) {
            case CLASS: {
              JDeclaredType type = c.getType();
              StandardClassMember member = memberFactory.get(type);
              membersByCorrelation.put(c, member);
              classesMutable.add(member);
              break;
            }
            case FIELD: {
              JField field = c.getField();
              JDeclaredType type = c.getType();
              StandardFieldMember member = memberFactory.get(field);
              memberFactory.get(type).addField(member);
              membersByCorrelation.put(c, member);
              break;
            }
            case METHOD: {
              JMethod method = c.getMethod();
              JDeclaredType type = c.getType();
              StandardMethodMember member = memberFactory.get(method);
              memberFactory.get(type).addMethod(member);
              membersByCorrelation.put(c, member);
              break;
            }
          }
        }
      }

      dependencyScope.push(new RangeInfo(range, info));
    }

    // Unwind the rest of the stack to finish out the ranges
    while (!dependencyScope.isEmpty()) {
      popAndRecord(dependencyScope, fragment);
    }

    /*
     * Because the first Range corresponds to the SourceInfo of the whole
     * program, we'll know that we got all of the data if the ends match up. If
     * this assert passes, we know that we've correctly generated a sequence of
     * non-overlapping Ranges that encompass the whole program.
     */
    assert dependencyOrder[0].getEnd() == lastEnd;
  }

  private void emitStory(StoryImpl story, Range range) throws IOException {
    builder.append("<story id=\"story");
    builder.append(story.getId());
    if (story.getLiteralTypeName() != null) {
      builder.append("\" literal=\"");
      builder.append(story.getLiteralTypeName());
    }
    builder.append("\">\n");

    Set<Member> correlations = story.getMembers();
    if (correlations.size() > 0) {
      builder.append("<correlations>\n");
      for (Member correlation : correlations) {
        builder.append("<by idref=\"");
        builder.append(correlation.getSourceName());
        builder.append("\"/>\n");

        flushOutput();
      }
      builder.append("</correlations>\n");
    }

    builder.append("<js fragment=\"");
    builder.append(curHighestFragment);
    builder.append("\"/>\n<storyref idref=\"story");
    builder.append(story.getId());

    int start = range.getStart();
    int end = range.getEnd();
    String jsCode = js[curHighestFragment];
    if ((start == end) || ((end == start + 1) && jsCode.charAt(start) == '\n')) {
      builder.append("\"/>\n</story>\n");
    } else {
      builder.append("\">");
      SizeMapRecorder.escapeXml(jsCode, start, end, false, builder);
      builder.append("</storyref>\n</story>\n");
    }
  }

  private void flushOutput() throws IOException {
    // Flush output to improve memory locality
    if (builder.length() > MAX_STRING_BUILDER_SIZE) {
      Util.writeUtf8(builder, gzipStream);
      builder.setLength(0);
    }
  }

  /**
   * Remove an element from the RangeInfo stack and stare a new StoryImpl with
   * the right length, possibly sub-dividing the super-enclosing Range in the
   * process.
   */
  private void popAndRecord(Stack<RangeInfo> dependencyScope, int fragment) throws IOException {
    RangeInfo rangeInfo = dependencyScope.pop();
    Range toStore = rangeInfo.range;

    /*
     * Make a new Range for the gap between the popped Range and whatever we
     * last stored.
     */
    if (lastEnd < toStore.getStart()) {
      Range newRange = new Range(lastEnd, toStore.getStart());
      assert !dependencyScope.isEmpty();

      SourceInfo gapInfo = dependencyScope.peek().info;
      recordStory(gapInfo, fragment, newRange.length(), newRange);

      lastEnd += newRange.length();
    }

    /*
     * Store as much of the current Range as we haven't previously stored. The
     * Max.max() is there to take care of the tail end of Ranges that have had a
     * sub-range previously stored.
     */
    if (lastEnd < toStore.getEnd()) {
      Range newRange = new Range(Math.max(lastEnd, toStore.getStart()), toStore.getEnd());
      recordStory(rangeInfo.info, fragment, newRange.length(), newRange);
      lastEnd += newRange.length();
    }
  }

  private void recordStory(SourceInfo info, int fragment, int length, Range range)
      throws IOException {
    assert info != null;
    assert storyCache != null;

    if (fragment > curHighestFragment) {
      curHighestFragment = fragment;
    }

    StoryImpl theStory;
    if (!storyCache.containsKey(info)) {
      SortedSet<Member> members = new TreeSet<Member>(Member.TYPE_AND_SOURCE_NAME_COMPARATOR);
      for (Correlation c : info.getCorrelations()) {
        Member m = membersByCorrelation.get(c);
        if (m != null) {
          members.add(m);
        }
      }

      String literalType = null;
      Correlation literalCorrelation = info.getCorrelation(Axis.LITERAL);
      if (literalCorrelation != null) {
        literalType = literalCorrelation.getLiteral().getDescription();
      }

      theStory = new StoryImpl(storyCache.size(), members, literalType, fragment, length);
      storyCache.put(info, theStory);
    } else {
      // Use a copy-constructed instance
      theStory = new StoryImpl(storyCache.get(info), length);
    }

    emitStory(theStory, range);
  }

}
