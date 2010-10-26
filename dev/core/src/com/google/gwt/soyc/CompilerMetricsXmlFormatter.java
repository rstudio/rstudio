/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.soyc;

import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationMetricsArtifact;
import com.google.gwt.core.ext.linker.ModuleMetricsArtifact;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Sets;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Exports the compiler metrics gathered by the precompile and
 * compilePermutation steps into an XML file to be read by external tools.
 * 
 * See compilerMetrics.xsd for an XML schema describing the output.
 * 
 * <pre>
 * &lt;metrics version="1">
 * 
 *   &lt;module elapsed="1234"> 
 *    &lt;sources count="1">
 *      &lt;source name="foo.java" />
 *    &lt;/sources>
 *    &lt;types count="1" kind="initial">
 *      &lt;type name="com.google.foo.Foo" />
 *    &lt;/types>
 *   &lt;/module>
 * 
 *   &lt;precompilations>
 *     &lt;precompilation base="0" ids="0,1,2" elapsed="1">
 *       &lt;types count="1" kind="generated">
 *         &lt;type name="com.google.foo.Bar" />
 *       &lt;/types>
 *       &lt;types count="2" kind="ast">
 *         &lt;type name="com.google.foo.Foo" />
 *         &lt;type name="com.google.foo.Bar" />
 *       &lt;/types>
 *     &lt;/precompilation>
 *   &lt;/precompilations>
 *
 *   &lt;compilations>
 *     &lt;compilation id="0" elapsed="1" totalElapsed="2" description="foo">
 *       &lt;javascript fragments="1" size="123">
 *         &lt;fragment size="123" initial="true" />
 *       &lt;/javascript>
 *     &lt;/compilation>
 *   &lt;/compilations>
 * 
 * &lt;/metrics>
 *
 *</pre>
 * 
 */
public class CompilerMetricsXmlFormatter {
    public static final int XML_FORMAT_VERSION = 1;
    
    public static byte[] writeMetricsAsXml(ArtifactSet artifacts,
        ModuleMetricsArtifact moduleMetrics) {

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintWriter pw = new PrintWriter(out);
      pw.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      pw.append("<metrics version=\"" + XML_FORMAT_VERSION + "\" >\n");

      writeModuleMetricsAsXml(moduleMetrics, pw);

      Set<PrecompilationMetricsArtifact> precompilationMetrics = artifacts.find(PrecompilationMetricsArtifact.class);
      if (!precompilationMetrics.isEmpty()) {
        pw.append(" <precompilations>\n");
        for (PrecompilationMetricsArtifact metrics : precompilationMetrics) {
          writePrecompilationMetricsAsXml(moduleMetrics, metrics, pw);
        }
        pw.append(" </precompilations>\n");
      }

      Set<CompilationMetricsArtifact> compilationMetrics = artifacts.find(CompilationMetricsArtifact.class);
      if (!compilationMetrics.isEmpty()) {
        pw.append(" <compilations>\n");
        for (CompilationMetricsArtifact metrics : compilationMetrics) {
          writeCompilationMetricsAsXml(metrics, pw);
        }
        pw.append(" </compilations>\n");
      }

      pw.append("</metrics>\n");
      pw.close();

      return out.toByteArray();
    }

    private static String escapeXmlAttributeContent(String content) {
      String result = content.replaceAll("\'", "&apos;");
      return result.replaceAll("\"", "&quot;");
    }
    
    private static void writeCompilationMetricsAsXml(
        CompilationMetricsArtifact metrics, PrintWriter pw) {
      pw.append("  <compilation  id=\"" + metrics.getPermuationId() + "\" ");
      pw.append("elapsed=\"" + metrics.getCompileElapsedMilliseconds() + "\" ");
      pw.append("totalElapsed=\"" + metrics.getElapsedMilliseconds() + "\" ");
      // TODO(zundel): Print out captured GC and heap memory analysis if it is
      // available.
      
      String description = metrics.getPermutationDescription();
      if (description != null) {
        pw.append(" description=\"" + escapeXmlAttributeContent(description) + "\"");
      }
      pw.append(">\n");

      int[] jsSizes = metrics.getJsSize();
      if (jsSizes != null) {
        int totalSize = 0, numFragments = 0;
        for (int size : jsSizes) {
          totalSize += size;
          numFragments++;
        }
        pw.append("   <javascript size=\"" + totalSize + "\" fragments=\""
            + numFragments + "\">\n");
        boolean initialFragment = true;
        for (int size : jsSizes) {
          pw.append("    <fragment ");
          if (initialFragment) {
            pw.append("initial=\"true\" ");
            initialFragment = false;
          }
          pw.append("size=\"" + size + "\" />\n");
        }
        pw.append("   </javascript>\n");
      }
      pw.append("  </compilation>\n");
    }

    private static void writeModuleMetricsAsXml(ModuleMetricsArtifact metrics,
        PrintWriter pw) {
      pw.append(" <module elapsed=\"" + metrics.getElapsedMilliseconds()
          + "\" ");
      pw.append(">\n");
      String[] sourceFiles = metrics.getSourceFiles();
      if (sourceFiles != null) {
        Arrays.sort(sourceFiles);
        pw.append("  <sources count=\"" + sourceFiles.length + "\">\n");
        for (String sourceFile : sourceFiles) {
          pw.append("   <source name=\"" + sourceFile + "\" />\n");
        }
        pw.append("  </sources>\n");
      }
      String[] initialTypes = metrics.getInitialTypes();
      if (initialTypes != null) {
        Arrays.sort(initialTypes);
        pw.append("  <types kind=\"initial\" count=\"" + initialTypes.length
            + "\">\n");
        for (String typeName : initialTypes) {
          pw.append("   <type name=\"" + typeName + "\" />\n");
        }
        pw.append("  </types>\n");
      }
      pw.append(" </module>\n");
    }

    private static void writePrecompilationMetricsAsXml(
        ModuleMetricsArtifact moduleMetrics,
        PrecompilationMetricsArtifact metrics, PrintWriter pw) {
      pw.append("  <precompilation ");
      pw.append("base=\"" + metrics.getPermuationBase() + "\" ");
      int[] permutationIds = metrics.getPermutationIds();
      if (permutationIds != null) {
        StringBuilder builder = new StringBuilder();
        for (int perm : permutationIds) {
          builder.append("" + perm + ",");
        }
        String idList = builder.substring(0, builder.length() - 1);
        pw.append("ids=\"" + idList + "\" ");
      }
      pw.append("elapsed=\"" + metrics.getElapsedMilliseconds() + "\" ");

      // TODO(zundel): Print out captured GC and heap memory analysis if it is
      // available.
      pw.append(">\n");
      String[] astTypes = metrics.getAstTypes();
      if (astTypes != null) {
        Arrays.sort(astTypes);
        pw.append("   <types kind=\"ast\" count=\"" + astTypes.length + "\">\n");
        for (String typeName : astTypes) {
          pw.append("    <type name=\"" + typeName + "\" />\n");
        }
        pw.append("   </types>\n");
      }
      String[] finalTypeOracleTypes = metrics.getFinalTypeOracleTypes();
      if (finalTypeOracleTypes != null) {
        assert finalTypeOracleTypes.length > 0;
        List<String> initialTypes = Lists.create(moduleMetrics.getInitialTypes());
        Set<String> generatedTypesList = Sets.create(finalTypeOracleTypes);
        generatedTypesList.removeAll(initialTypes);
        String[] generatedTypes = generatedTypesList.toArray(new String[generatedTypesList.size()]);
        Arrays.sort(generatedTypes);
        pw.append("   <types kind=\"generated\" count=\"" + generatedTypes.length
            + "\">\n");
        for (String typeName : generatedTypes) {
          pw.append("    <type name=\"" + typeName + "\" />\n");
        }
        pw.append("   </types>\n");
      }
      pw.append(" </precompilation>\n");
    }
    
    private CompilerMetricsXmlFormatter() {
      // do not instantiate
    }
}
