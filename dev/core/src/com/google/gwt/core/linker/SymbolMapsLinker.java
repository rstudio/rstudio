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
package com.google.gwt.core.linker;

import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapGeneratorV3;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SoftPermutation;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashMap;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * This Linker exports the symbol maps associated with each compilation result as a private file.
 * The names of the symbol maps files are computed by appending {@value #STRONG_NAME_SUFFIX} to the
 * value returned by {@link CompilationResult#getStrongName()}.
 */
@LinkerOrder(Order.POST)
@Shardable
public class SymbolMapsLinker extends AbstractLinker {

  public static final String MAKE_SYMBOL_MAPS = "compiler.useSymbolMaps";

  /**
   * Artifact to record insertions or deletions made to Javascript fragments.
   */
  public static class ScriptFragmentEditsArtifact extends Artifact<ScriptFragmentEditsArtifact> {

    /**
     * Operation type performed on script.
     */
    public enum Edit {
      PREFIX, INSERT, REMOVE;
    }

    private static class EditOperation {

      public static EditOperation insert(int lineNumber, String data) {
        return new EditOperation(Edit.INSERT, lineNumber, data);
      }

      public static EditOperation prefix(String data) {
        return new EditOperation(Edit.PREFIX, 0, data);
      }

      public static EditOperation remove(int lineNumber) {
        return new EditOperation(Edit.REMOVE, lineNumber, null);
      }

      Edit op;
      int lineNumber;
      int numLines;

      public EditOperation(
          Edit op, int lineNumber, String data) {
        this.op = op;
        this.lineNumber = lineNumber;
        this.numLines = countNewLines(data);
      }

      public int getLineNumber() {
        return lineNumber;
      }

      public int getNumLines() {
        return numLines;
      }

      public Edit getOp() {
        return op;
      }

      private int countNewLines(String chunkJs) {
        int newLineCount = 0;
        for (int j = 0; j < chunkJs.length(); j++) {
          if (chunkJs.charAt(j) == '\n') {
            newLineCount++;
          }
        }
        return newLineCount;
      }
    }

    private List<EditOperation> editOperations = new ArrayList<EditOperation>();

    private String strongName;
    private int fragment;

    public ScriptFragmentEditsArtifact(String strongName,
        int fragment) {
      super(SymbolMapsLinker.class);
      this.strongName = strongName;
      this.fragment = fragment;
    }

    public int getFragment() {
      return fragment;
    }

    public String getStrongName() {
      return strongName;
    }

    @Override
    public int hashCode() {
      return (strongName + fragment).hashCode();
    }

    public void insertLinesBefore(int position, String lines) {
      editOperations.add(EditOperation.insert(position, lines));
    }

    public void prefixLines(String lines) {
      editOperations.add(EditOperation.prefix(lines));
    }

    @Override
    protected int compareToComparableArtifact(SymbolMapsLinker.ScriptFragmentEditsArtifact o) {
      int result = (strongName + fragment).compareTo(strongName + fragment);
      return result;
    }

    @Override
    protected Class<ScriptFragmentEditsArtifact> getComparableArtifactType() {
      return ScriptFragmentEditsArtifact.class;
    }
  }

  /**
   * Artifact to represent a sourcemap file to be processed by SymbolMapsLinker.
   */
  public static class SourceMapArtifact extends SyntheticArtifact {

    private int permutationId;
    private int fragment;
    private byte[] js;

    public SourceMapArtifact(int permutationId, int fragment, byte[] js) {
      super(SymbolMapsLinker.class, permutationId + "/sourceMap" + fragment + ".json", js);
      this.permutationId = permutationId;
      this.fragment = fragment;
      this.js = js;
    }

    public int getFragment() {
      return fragment;
    }

    public int getPermutationId() {
      return permutationId;
    }
  }

  /**
   * This value is appended to the strong name of the CompilationResult to form the symbol map's
   * filename.
   */
  public static final String STRONG_NAME_SUFFIX = ".symbolMap";

  public static String propertyMapToString(
      Map<SelectionProperty, String> propertyMap) {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    printPropertyMap(pw, propertyMap);
    pw.flush();
    return writer.toString();
  }

  private static void printPropertyMap(PrintWriter pw,
      Map<SelectionProperty, String> map) {
    boolean needsComma = false;
    for (Map.Entry<SelectionProperty, String> entry : map.entrySet()) {
      if (needsComma) {
        pw.print(" , ");
      } else {
        needsComma = true;
      }

      pw.print("'");
      pw.print(entry.getKey().getName());
      pw.print("' : '");
      pw.print(entry.getValue());
      pw.print("'");
    }
  }

  @Override
  public String getDescription() {
    return "Export CompilationResult symbol maps";
  }

  /**
   * Included to support legacy non-shardable subclasses.
   */
  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    return link(logger, context, artifacts, true);
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {

    if (onePermutation) {
      artifacts = new ArtifactSet(artifacts);
      Map<Integer, String> permMap = new HashMap<Integer, String>();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      for (CompilationResult result : artifacts.find(CompilationResult.class)) {
        
        boolean makeSymbolMaps = true;

        for (SoftPermutation perm : result.getSoftPermutations()) {
          for (Entry<SelectionProperty, String> propMapEntry : perm.getPropertyMap().entrySet()) {
            if (propMapEntry.getKey().getName().equals(MAKE_SYMBOL_MAPS)) {
              makeSymbolMaps = Boolean.valueOf(propMapEntry.getValue());
            }
          }
        }

        if (makeSymbolMaps) {
          PrintWriter pw = new PrintWriter(out);
          permMap.put(result.getPermutationId(), result.getStrongName());
          doWriteSymbolMap(logger, result, pw);
          pw.close();

          doEmitSymbolMap(logger, artifacts, result, out);
          out.reset();
        }
      }


      for (SourceMapArtifact se : artifacts.find(SourceMapArtifact.class)) {
        // filename is permutation_id/sourceMap<fragmentNumber>.json
        String sourceMapString = Util.readStreamAsString(se.getContents(logger));
        String strongName = permMap.get(se.getPermutationId());
        String partialPath = strongName + "_sourceMap" + se.getFragment() + ".json";

        int fragment = se.getFragment();
        ScriptFragmentEditsArtifact editArtifact = null;
        for (ScriptFragmentEditsArtifact mp : artifacts.find(ScriptFragmentEditsArtifact.class)) {
          if (mp.getStrongName().equals(strongName) && mp.getFragment() == fragment) {
            editArtifact = mp;
            artifacts.remove(editArtifact);
            break;
          }
        }

        SyntheticArtifact emArt = null;
        // no need to adjust source map
        if (editArtifact == null) {
          emArt = emitSourceMapString(logger, sourceMapString, partialPath);
        } else {
          SourceMapGeneratorV3 sourceMapGenerator = new SourceMapGeneratorV3();
          try {
            int totalPrefixLines = 0;
            for (ScriptFragmentEditsArtifact.EditOperation op : editArtifact.editOperations) {
              if (op.getOp() == ScriptFragmentEditsArtifact.Edit.PREFIX) {
                totalPrefixLines += op.getNumLines();
              }
            }
            // TODO(cromwellian): apply insert and remove edits
            sourceMapGenerator.mergeMapSection(totalPrefixLines, 0, sourceMapString);
            StringWriter stringWriter = new StringWriter();
            sourceMapGenerator.appendTo(stringWriter, "sourceMap");
            emArt = emitSourceMapString(logger, stringWriter.toString(), partialPath);
          } catch (Exception e) {
            logger.log(TreeLogger.Type.WARN, "Can't write source map " + partialPath, e);
          }
        }
        artifacts.add(emArt);
        artifacts.remove(se);
      }
    }
    return artifacts;
  }

  /**
   * Override to change the manner in which the symbol map is emitted.
   */
  protected void doEmitSymbolMap(TreeLogger logger, ArtifactSet artifacts,
      CompilationResult result, ByteArrayOutputStream out)
      throws UnableToCompleteException {
    EmittedArtifact symbolMapArtifact = emitBytes(logger, out.toByteArray(),
        result.getStrongName() + STRONG_NAME_SUFFIX);
    // TODO: change to Deploy when possible
    symbolMapArtifact.setVisibility(Visibility.LegacyDeploy);
    artifacts.add(symbolMapArtifact);
  }

  /**
   * Override to change the format of the symbol map.
   *
   * @param logger the logger to write to
   * @param result the compilation result
   * @param pw     the output PrintWriter
   * @throws UnableToCompleteException if an error occurs
   */
  protected void doWriteSymbolMap(TreeLogger logger, CompilationResult result,
      PrintWriter pw) throws UnableToCompleteException {
    pw.println("# { " + result.getPermutationId() + " }");

    for (SortedMap<SelectionProperty, String> map : result.getPropertyMap()) {
      pw.print("# { ");
      printPropertyMap(pw, map);
      pw.println(" }");
    }

    pw.println("# jsName, jsniIdent, className, memberName, sourceUri, sourceLine, fragmentNumber");
    StringBuilder sb = new StringBuilder(1024);
    char[] buf = new char[1024];
    for (SymbolData symbol : result.getSymbolMap()) {
      sb.append(symbol.getSymbolName());

      sb.append(',');
      String jsniIdent = symbol.getJsniIdent();
      if (jsniIdent != null) {
        sb.append(jsniIdent);
      }
      sb.append(',');
      sb.append(symbol.getClassName());
      sb.append(',');
      String memberName = symbol.getMemberName();
      if (memberName != null) {
        sb.append(memberName);
      }
      sb.append(',');
      String sourceUri = symbol.getSourceUri();
      if (sourceUri != null) {
        sb.append(sourceUri);
      }
      sb.append(',');
      sb.append(symbol.getSourceLine());
      sb.append(',');
      sb.append(symbol.getFragmentNumber());
      sb.append('\n');

      int sbLen = sb.length();
      if (buf.length < sbLen) {
        int bufLen = buf.length;
        while (bufLen < sbLen) {
          bufLen <<= 1;
        }
        buf = new char[bufLen];
      }
      sb.getChars(0, sbLen, buf, 0);
      pw.write(buf, 0, sbLen);
      sb.setLength(0);
    }
  }

  protected SyntheticArtifact emitSourceMapString(TreeLogger logger, String contents,
      String partialPath) throws UnableToCompleteException {
    SyntheticArtifact emArt = emitString(logger, contents, partialPath);
    emArt.setVisibility(Visibility.LegacyDeploy);
    return emArt;
  }
}
