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
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.core.ext.soyc.Story;
import com.google.gwt.core.ext.soyc.StoryRecorder;
import com.google.gwt.dev.jjs.Correlation;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.util.HtmlTextOutput;
import com.google.gwt.util.tools.Utility;
import com.google.gwt.core.ext.soyc.ClassMember;
import com.google.gwt.core.ext.soyc.FunctionMember;
import com.google.gwt.core.ext.soyc.Member;
import com.google.gwt.core.ext.soyc.Story.Origin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

public class StoryRecorderImpl implements StoryRecorder {

  private FileOutputStream stream;
  private OutputStreamWriter writer;
  private PrintWriter pw;
  private HtmlTextOutput htmlOut;

  private Map<Story, Integer> storyIds = new HashMap<Story, Integer>();
  private String[] js;
  private int curHighestFragment = 0;

  /**
   * Used to record dependencies of a program
   * 
   * @param jprogram
   * @param workDir
   * @param permutationId
   * @param logger
   * @return The file that the dependencies are recorded in
   */
  public File recordStories(JProgram jprogram, File workDir, int permutationId,
      TreeLogger logger, List<Map<Range, SourceInfo>> sourceInfoMaps,
      String[] js) {

    logger = logger.branch(TreeLogger.INFO, "Creating Stories file for SOYC");

    this.js = js;

    File storiesFile = new File(workDir, "stories"
        + Integer.toString(permutationId) + ".xml.gz");
    try {
      stream = new FileOutputStream(storiesFile, true);
      writer = new OutputStreamWriter(new GZIPOutputStream(stream), "UTF-8");
      storiesFile.getParentFile().mkdirs();
      pw = new PrintWriter(writer);
      htmlOut = new HtmlTextOutput(pw, false);

      String curLine = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      curLine = "<soyc>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();
      htmlOut.indentIn();
      htmlOut.indentIn();

      curLine = "<stories>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();
      htmlOut.indentIn();
      htmlOut.indentIn();

      /*
       * Don't retain beyond the constructor to avoid lingering references to
       * AST nodes.
       */
      MemberFactory memberFactory = new MemberFactory();

      // Record what we've seen so far
      TreeSet<ClassMember> classesMutable = new TreeSet<ClassMember>(
          Member.SOURCE_NAME_COMPARATOR);
      TreeSet<FunctionMember> functionsMutable = new TreeSet<FunctionMember>(
          Member.SOURCE_NAME_COMPARATOR);
      Set<SourceInfo> sourceInfoSeen = new HashSet<SourceInfo>();

      int fragment = 0;
      for (Map<Range, SourceInfo> sourceInfoMap : sourceInfoMaps) {
        lastEnd = 0;
        analyzeFragment(memberFactory, classesMutable, functionsMutable,
            sourceInfoMap, sourceInfoSeen, fragment++);
      }

      /*
       * Clear the member fields that we don't need anymore to allow GC of the
       * SourceInfo objects
       */
      membersByCorrelation = null;
      storyCache = null;

      htmlOut.indentOut();
      htmlOut.indentOut();
      curLine = "</stories>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      htmlOut.indentOut();
      htmlOut.indentOut();
      curLine = "</soyc>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      Utility.close(writer);
      pw.close();
     
      logger.log(TreeLogger.INFO, "Done");
      
    } catch (Throwable e) {
      logger.log(TreeLogger.ERROR, "Could not open dependency file.", e);
    }

    return storiesFile;
  }

  /**
   * Associates a SourceInfo with a Range.
   */
  private static class RangeInfo {
    public final SourceInfo info;
    public final Range range;

    public RangeInfo(Range range, SourceInfo info) {
      this.range = range;
      this.info = info;
    }
  }

  /**
   * Used by {@link #popAndRecord(Stack)} to determine start and end ranges.
   */
  private int lastEnd = 0;

  /**
   * This is a class field for convenience, but it should be deleted at the end
   * of the constructor.
   */
  private transient Map<SourceInfo, StoryImpl> storyCache = new IdentityHashMap<SourceInfo, StoryImpl>();

  /**
   * This is a class field for convenience, but it should be deleted at the end
   * of the constructor.
   */
  private transient Map<Correlation, Member> membersByCorrelation = new IdentityHashMap<Correlation, Member>();

  private void analyzeFragment(MemberFactory memberFactory,
      TreeSet<ClassMember> classesMutable,
      TreeSet<FunctionMember> functionsMutable,
      Map<Range, SourceInfo> sourceInfoMap, Set<SourceInfo> sourceInfoSeen,
      int fragment) {
    /*
     * We want to iterate over the Ranges so that enclosing Ranges come before
     * their enclosed Ranges...
     */
    Range[] dependencyOrder = sourceInfoMap.keySet().toArray(
        new Range[sourceInfoMap.size()]);
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
        for (Correlation c : info.getPrimaryCorrelations()) {
          if (membersByCorrelation.containsKey(c)) {
            continue;
          }

          switch (c.getAxis()) {
            case CLASS: {
              JReferenceType type = c.getType();
              StandardClassMember member = memberFactory.get(type);
              membersByCorrelation.put(c, member);
              classesMutable.add(member);
              break;
            }
            case FIELD: {
              JField field = c.getField();
              JReferenceType type = c.getType();
              StandardFieldMember member = memberFactory.get(field);
              memberFactory.get(type).addField(member);
              membersByCorrelation.put(c, member);
              break;
            }
            case FUNCTION: {
              JsFunction function = c.getFunction();
              StandardFunctionMember member = memberFactory.get(function);
              membersByCorrelation.put(c, member);
              functionsMutable.add(member);
              break;
            }
            case METHOD: {
              JMethod method = c.getMethod();
              JReferenceType type = c.getType();
              StandardMethodMember member = memberFactory.get(method);
              memberFactory.get(type).addMethod(member);
              membersByCorrelation.put(c, member);
              break;
            }
          }
        }
      }

      /*
       * Record dependencies as observed in the structure of the JS output. This
       * an an ad-hoc approach that just looks at which SourceInfos are used
       * within the Range of another SourceInfo.
       */
      Set<Correlation> correlationsInScope = new HashSet<Correlation>();
      for (RangeInfo outer : dependencyScope) {
        SourceInfo outerInfo = outer.info;
        correlationsInScope.addAll(outerInfo.getPrimaryCorrelations());
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

  /**
   * Remove an element from the RangeInfo stack and stare a new StoryImpl with
   * the right length, possibly sub-dividing the super-enclosing Range in the
   * process.
   */
  private void popAndRecord(Stack<RangeInfo> dependencyScope, int fragment) {
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
      Range newRange = new Range(Math.max(lastEnd, toStore.getStart()),
          toStore.getEnd());
      recordStory(rangeInfo.info, fragment, newRange.length(), newRange);
      lastEnd += newRange.length();
    }
  }

  private void recordStory(SourceInfo info, int fragment, int length,
      Range range) {
    assert storyCache != null;

    if (fragment > curHighestFragment) {
      curHighestFragment = fragment;
    }

    StoryImpl theStory;
    if (!storyCache.containsKey(info)) {

      SortedSet<Member> members = new TreeSet<Member>(
          Member.TYPE_AND_SOURCE_NAME_COMPARATOR);

      if (info != null) {
        for (Correlation c : info.getAllCorrelations()) {
          Member m = membersByCorrelation.get(c);
          if (m != null) {
            members.add(m);
          }
        }
      }

      SortedSet<Origin> origins = new TreeSet<Origin>();
      for (Correlation c : info.getAllCorrelations(Axis.ORIGIN)) {
        origins.add(new OriginImpl(c.getOrigin()));
      }

      String literalType = null;
      Correlation literalCorrelation = info.getPrimaryCorrelation(Axis.LITERAL);
      if (literalCorrelation != null) {
        literalType = literalCorrelation.getLiteral().getDescription();
      }

      theStory = new StoryImpl(storyCache.size(), members, origins,
          literalType, fragment, length);
      storyCache.put(info, theStory);
    } else {
      // Use a copy-constructed instance
      theStory = new StoryImpl(storyCache.get(info), length);
    }

    emitStory(theStory, range);
  }

  private void emitStory(StoryImpl story, Range range) {
    
    int storyNum;
    if (storyIds.containsKey(story)) {
      storyNum = storyIds.get(story);
    } else {
      storyNum = storyIds.size();
      storyIds.put(story, storyNum);
    }

    String curLine = "<story id=\"story" + Integer.toString(storyNum) + "\"";
    if (story.getLiteralTypeName() != null) {
      curLine = curLine + " literal=\"" + story.getLiteralTypeName() + "\"";
    }
    curLine = curLine + ">";
    htmlOut.printRaw(curLine);
    htmlOut.newline();

    Set<Origin> origins = story.getSourceOrigin();
    if (origins.size() > 0) {
      htmlOut.indentIn();
      htmlOut.indentIn();

      curLine = "<origins>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      htmlOut.indentIn();
      htmlOut.indentIn();
    }
    for (Origin origin : origins) {
      curLine = "<origin lineNumber=\""
          + Integer.toString(origin.getLineNumber()) + "\" location=\""
          + origin.getLocation() + "\"/>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();
    }
    if (origins.size() > 0) {
      htmlOut.indentOut();
      htmlOut.indentOut();

      curLine = "</origins>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      htmlOut.indentOut();
      htmlOut.indentOut();
    }

    Set<Member> correlations = story.getMembers();
    if (correlations.size() > 0) {
      htmlOut.indentIn();
      htmlOut.indentIn();

      curLine = "<correlations>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      htmlOut.indentIn();
      htmlOut.indentIn();
    }
    for (Member correlation : correlations) {
      curLine = "<by idref=\"" + correlation.getSourceName() + "\"/>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();
    }
    if (correlations.size() > 0) {
      htmlOut.indentOut();
      htmlOut.indentOut();

      curLine = "</correlations>";
      htmlOut.printRaw(curLine);
      htmlOut.newline();

      htmlOut.indentOut();
      htmlOut.indentOut();
    }

    htmlOut.indentIn();
    htmlOut.indentIn();

    curLine = "<js fragment=\"" + curHighestFragment + "\"/>";
    htmlOut.printRaw(curLine);
    htmlOut.newline();

    String jsCode = js[curHighestFragment].substring(range.getStart(),
        range.getEnd());
    jsCode = escapeXml(jsCode);
    if ((jsCode.length() == 0) || (jsCode.compareTo("\n") == 0)) {
      curLine = "<storyref idref=\"story" + Integer.toString(storyNum) + "\"/>";
    } else {
      curLine = "<storyref idref=\"story" + Integer.toString(storyNum) + "\">"
          + jsCode + "</storyref>";
    }

    htmlOut.printRaw(curLine);
    htmlOut.newline();

    htmlOut.indentOut();
    htmlOut.indentOut();

    curLine = "</story>";
    htmlOut.printRaw(curLine);
    htmlOut.newline();
  }

  private String escapeXml(String unescaped) {
    String escaped = unescaped.replaceAll("\\&", "&amp;");
    escaped = escaped.replaceAll("\\<", "&lt;");
    escaped = escaped.replaceAll("\\>", "&gt;");
    escaped = escaped.replaceAll("\\\"", "&quot;");
    // escaped = escaped.replaceAll("\\'", "&apos;");
    return escaped;
  }
}
