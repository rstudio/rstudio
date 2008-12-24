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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.CompilationAnalysis;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.soyc.ClassMember;
import com.google.gwt.core.ext.soyc.FunctionMember;
import com.google.gwt.core.ext.soyc.Member;
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.core.ext.soyc.Story;
import com.google.gwt.core.ext.soyc.Story.Origin;
import com.google.gwt.core.ext.soyc.impl.AbstractMemberWithDependencies;
import com.google.gwt.core.ext.soyc.impl.MemberFactory;
import com.google.gwt.core.ext.soyc.impl.OriginImpl;
import com.google.gwt.core.ext.soyc.impl.SnippetIterator;
import com.google.gwt.core.ext.soyc.impl.StandardClassMember;
import com.google.gwt.core.ext.soyc.impl.StandardFieldMember;
import com.google.gwt.core.ext.soyc.impl.StandardFunctionMember;
import com.google.gwt.core.ext.soyc.impl.StandardMethodMember;
import com.google.gwt.core.ext.soyc.impl.StoryImpl;
import com.google.gwt.dev.jjs.Correlation;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.js.ast.JsFunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An implementation of CompilationAnalysis. This class transforms SourceInfos
 * and related data into an API suitable for public consumption via the Linker
 * API.
 */
public class StandardCompilationAnalysis extends CompilationAnalysis {
  /**
   * A roll-up struct for all the data produced by the analysis to make
   * serialization simpler.
   */
  private static class Data implements Serializable {
    SortedSet<ClassMember> classes;
    SortedSet<FunctionMember> functions;

    /**
     * These are the Stories in the order in which they should be presented to
     * the user via {@link CompilationAnalysis#getSnippets()}.
     */
    Map<Integer, List<StoryImpl>> orderedStories = new HashMap<Integer, List<StoryImpl>>();

    SortedSet<Story> stories;
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

  private Data data;

  /**
   * Used by {@link #popAndRecord(Stack)} to determine start and end ranges.
   */
  private int lastEnd = 0;

  private CompilationResult result;

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

  /**
   * Map from split point numbers to the method where they were set.
   */
  private Map<Integer, String> splitPointMap = new TreeMap<Integer, String>();
  
  /**
   * Constructed by PermutationCompiler.
   */
  public StandardCompilationAnalysis(TreeLogger logger,
      List<Map<Range, SourceInfo>> sourceInfoMaps,
      Map<Integer, String> splitPointMap)
      throws UnableToCompleteException {
    super(StandardLinkerContext.class);
    logger = logger.branch(TreeLogger.INFO,
        "Creating CompilationAnalysis (this may take some time)");

    data = new Data();
    
    this.splitPointMap = splitPointMap;
    
    /*
     * Don't retain beyond the constructor to avoid lingering references to AST
     * nodes.
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

    data.classes = Collections.unmodifiableSortedSet(classesMutable);
    data.functions = Collections.unmodifiableSortedSet(functionsMutable);

    // Deduplicate the ordered stories into an ordered set
    SortedSet<Story> mutableStories = new TreeSet<Story>(
        StoryImpl.ID_COMPARATOR);
    for (List<StoryImpl> stories : data.orderedStories.values()) {
      mutableStories.addAll(stories);
    }
    data.stories = Collections.unmodifiableSortedSet(mutableStories);

    /*
     * Clear the member fields that we don't need anymore to allow GC of the
     * SourceInfo objects
     */
    membersByCorrelation = null;
    storyCache = null;

    logger.log(TreeLogger.INFO, "Done");
  }

  @Override
  public SortedSet<ClassMember> getClasses() {
    return data.classes;
  }

  @Override
  public CompilationResult getCompilationResult() {
    return result;
  }

  @Override
  public SortedSet<FunctionMember> getFunctions() {
    return data.functions;
  }

  @Override
  public Iterable<Snippet> getSnippets(int fragment) {
    final List<StoryImpl> stories = data.orderedStories.get(fragment);
    if (stories == null) {
      throw new IllegalArgumentException("Unknown fragment id " + fragment);
    }

    return new Iterable<Snippet>() {
      public Iterator<Snippet> iterator() {
        return new SnippetIterator(stories);
      }
    };
  }

  @Override
  public Map<Integer, String> getSplitPointMap() {
    return splitPointMap;
  }
  
  @Override
  public SortedSet<Story> getStories() {
    return data.stories;
  }

  /**
   * Back-channel setter used by PermutationCompiler.
   */
  public void setCompilationResult(CompilationResult result) {
    this.result = result;
  }

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

        for (Correlation outerCorrelation : outerInfo.getPrimaryCorrelations()) {
          Member outerMember = membersByCorrelation.get(outerCorrelation);

          if (outerMember instanceof AbstractMemberWithDependencies) {
            for (Correlation innerCorrelation : info.getAllCorrelations()) {
              /*
               * This check prevents an inlined method from depending on the
               * method or function into which is was inlined.
               */
              if (correlationsInScope.contains(innerCorrelation)) {
                continue;
              }

              Member innerMember = membersByCorrelation.get(innerCorrelation);

              /*
               * The null check is because we may not create Members for all
               * types of Correlations.
               */
              if (innerMember != null) {
                if (((AbstractMemberWithDependencies) outerMember).addDependency(innerMember)) {
                  // System.out.println(outerMember + " -> " + innerMember);
                }
              }
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
      recordStory(gapInfo, fragment, newRange.length());
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
      recordStory(rangeInfo.info, fragment, newRange.length());
      lastEnd += newRange.length();
    }
  }

  private void recordStory(SourceInfo info, int fragment, int length) {
    assert storyCache != null;

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

      theStory = new StoryImpl(storyCache.size(), members, info.getMutations(),
          origins, literalType, fragment, length);
      storyCache.put(info, theStory);
    } else {
      // Use a copy-constructed instance
      theStory = new StoryImpl(storyCache.get(info), length);
    }

    List<StoryImpl> stories = data.orderedStories.get(fragment);
    if (stories == null) {
      stories = new ArrayList<StoryImpl>();
      data.orderedStories.put(fragment, stories);
    }
    stories.add(theStory);
  }
}
