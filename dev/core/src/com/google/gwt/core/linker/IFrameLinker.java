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
package com.google.gwt.core.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.impl.HostedModeLinker;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.SortedSet;

/**
 * Implements the canonical GWT bootstrap sequence that loads the GWT module in
 * a separate iframe.
 */
@LinkerOrder(Order.PRIMARY)
public class IFrameLinker extends SelectionScriptLinker {
  /**
   * This string is inserted between script chunks. It is made default access
   * for testing.
   */
  static final String SCRIPT_CHUNK_SEPARATOR = "--></script>\n<script><!--\n";

  /**
   * A configuration property indicating how large each script tag should be.
   */
  private static final String CHUNK_SIZE_PROPERTY = "iframe.linker.script.chunk.size";

  /**
   * Split a JavaScript string into multiple chunks, at statement boundaries.
   * Insert and end-script tag and a start-script tag in between each chunk.
   * This method is made default access for testing.
   * 
   * @param ranges Describes where the statements are located within the
   *          JavaScript code. If <code>null</code>, then return <code>js</code>
   *          unchanged.
   * @param js The JavaScript code to be split up.
   * @param charsPerChunk The number of characters to be put in each script tag
   */
  static String splitPrimaryJavaScript(StatementRanges ranges, String js,
      int charsPerChunk) {
    if (charsPerChunk < 0 || ranges == null) {
      return js;
    }

    StringBuilder sb = new StringBuilder();
    int bytesInCurrentTag = 0;

    for (int i = 0; i < ranges.numStatements(); i++) {
      int start = ranges.start(i);
      int end = ranges.end(i);
      int length = end - start;
      if (bytesInCurrentTag > 0 && bytesInCurrentTag + length > charsPerChunk) {
        if (lastChar(sb) != '\n') {
          sb.append('\n');
        }
        sb.append(SCRIPT_CHUNK_SEPARATOR);
        bytesInCurrentTag = 0;
      }
      if (bytesInCurrentTag > 0) {
        char lastChar = lastChar(sb);
        if (lastChar != '\n' && lastChar != ';' && lastChar != '}') {
          /*
           * Make sure this statement has a separator from the last one.
           */
          sb.append(";");
        }
      }
      sb.append(js, start, end);
      bytesInCurrentTag += length;
    }
    return sb.toString();
  }

  private static char lastChar(StringBuilder sb) {
    return sb.charAt(sb.length() - 1);
  }

  @Override
  public String getDescription() {
    return "Standard";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet toReturn = super.link(logger, context, artifacts);

    try {
      // Add hosted mode iframe contents
      // TODO move this into own impl package if HostedModeLinker goes away
      URL resource = HostedModeLinker.class.getResource("hosted.html");
      if (resource == null) {
        logger.log(TreeLogger.ERROR,
            "Unable to find support resource 'hosted.html'");
        throw new UnableToCompleteException();
      }

      final URLConnection connection = resource.openConnection();
      // TODO: extract URLArtifact class?
      EmittedArtifact hostedHtml = new EmittedArtifact(IFrameLinker.class,
          "hosted.html") {
        @Override
        public InputStream getContents(TreeLogger logger)
            throws UnableToCompleteException {
          try {
            return connection.getInputStream();
          } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "Unable to copy support resource", e);
            throw new UnableToCompleteException();
          }
        }

        @Override
        public long getLastModified() {
          return connection.getLastModified();
        }
      };
      toReturn.add(hostedHtml);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to copy support resource", e);
      throw new UnableToCompleteException();
    }

    return toReturn;
  }

  /**
   * This implementation divides the code of the initial fragment into multiple
   * script tags. These chunked script tags loads faster on Firefox even when
   * the data is cached. Additionally, having the script tags separated means
   * that the early ones can be evaluated before the later ones have finished
   * downloading. As a result of this parallelism, the overall time to get the
   * JavaScript downloaded and evaluated can lower.
   */
  @Override
  protected byte[] generatePrimaryFragment(TreeLogger logger,
      LinkerContext context, CompilationResult result, String[] js)
      throws UnableToCompleteException {
    StringBuffer b = new StringBuffer();
    b.append(getModulePrefix(logger, context, result.getStrongName(), js.length));
    b.append(splitPrimaryJavaScript(result.getStatementRanges()[0], js[0],
        charsPerChunk(context, logger)));
    b.append(getModuleSuffix(logger, context));
    return Util.getBytes(b.toString());
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) {
    return ".cache.html";
  }

  /**
   * Returns the subdirectory name to be used by getModulPrefix when requesting
   * a runAsync module. The default implementation returns the value of
   * FRAGMENT_SUDBIR. This has been factored out for test cases.
   */
  protected String getFragmentSubdir() {
    return FRAGMENT_SUBDIR;
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName) {
    return getModulePrefix(context, strongName, true);
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName, int numFragments) {
    return getModulePrefix(context, strongName, numFragments > 1);
  }

  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context) {
    DefaultTextOutput out = new DefaultTextOutput(context.isOutputCompact());

    out.print("$stats && $stats({moduleName:'" + context.getModuleName()
        + "',sessionId:$sessionId"
        + ",subSystem:'startup',evtGroup:'moduleStartup'"
        + ",millis:(new Date()).getTime(),type:'moduleEvalEnd'});");

    // Generate the call to tell the bootstrap code that we're ready to go.
    out.newlineOpt();
    out.print("if ($wnd." + context.getModuleFunctionName() + ") $wnd."
        + context.getModuleFunctionName() + ".onScriptLoad();");
    out.newline();
    out.print("--></script></body></html>");
    out.newlineOpt();

    return out.toString();
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) {
    return "com/google/gwt/core/linker/IFrameTemplate.js";
  }

  protected String modifyPrimaryJavaScript(String js) {
    return js;
  }

  /**
   * Extract via {@link #CHUNK_SIZE_PROPERTY} the number of characters to be
   * included in each script tag.
   */
  private int charsPerChunk(LinkerContext context, TreeLogger logger)
      throws UnableToCompleteException {
    SortedSet<ConfigurationProperty> configProps = context.getConfigurationProperties();
    for (ConfigurationProperty prop : configProps) {
      if (prop.getName().equals(CHUNK_SIZE_PROPERTY)) {
        return Integer.parseInt(prop.getValues().get(0));
      }
    }

    logger.log(TreeLogger.ERROR, "Unable to find configuration property "
        + CHUNK_SIZE_PROPERTY);
    throw new UnableToCompleteException();
  }

  /**
   * This is the real implementation of <code>getModulePrefix</code> for this
   * linker. The other versions forward to this one.
   */
  private String getModulePrefix(LinkerContext context, String strongName,
      boolean supportRunAsync) {
    DefaultTextOutput out = new DefaultTextOutput(context.isOutputCompact());
    out.print("<html>");
    out.newlineOpt();

    // Setup the well-known variables.
    out.print("<head><script>");
    out.newlineOpt();
    out.print("var $gwt_version = \"" + About.getGwtVersionNum() + "\";");
    out.newlineOpt();
    out.print("var $wnd = parent;");
    out.newlineOpt();
    out.print("var $doc = $wnd.document;");
    out.newlineOpt();
    out.print("var $moduleName, $moduleBase;");
    out.newlineOpt();
    out.print("var $strongName = '" + strongName + "';");
    out.newlineOpt();
    if (supportRunAsync) {
      out.print("function __gwtStartLoadingFragment(frag) {");
      out.indentIn();
      out.newlineOpt();
      out.print("  return $moduleBase + '" + getFragmentSubdir()
          + "/'  + $strongName + '/' + frag + '" + FRAGMENT_EXTENSION + "';");
      out.indentOut();
      out.newlineOpt();
      out.print("};");
      out.newlineOpt();
      out.print("function __gwtInstallCode(code) {");
      /*
       * Use a script tag on all platforms, for simplicity. It would be cleaner
       * to use window.eval, but at the time of writing that only reliably works
       * on Firefox. It would also be cleaner to use window.execScript on
       * platforms that support it (IE and Chrome). However, trying this causes
       * IE 6 (and possibly others) to emit "error 80020101", apparently due to
       * something objectionable in the compiler's output JavaScript.
       */
      out.indentIn();
      out.newlineOpt();
      out.print("var head = document.getElementsByTagName('head').item(0);");
      out.newlineOpt();
      out.print("var script = document.createElement('script');");
      out.newlineOpt();
      out.print("script.type = 'text/javascript';");
      out.newlineOpt();
      out.print("script.text = code;");
      out.newlineOpt();
      out.print("head.appendChild(script);");
      out.indentOut();
      out.newlineOpt();
      out.print("};");
      out.newlineOpt();
    }
    out.print("var $stats = $wnd.__gwtStatsEvent ? function(a) {return $wnd.__gwtStatsEvent(a);} : null,");
    out.newlineOpt();
    out.print("$sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null;");
    out.newlineOpt();
    out.print("$stats && $stats({moduleName:'" + context.getModuleName()
        + "',sessionId:$sessionId"
        + ",subSystem:'startup',evtGroup:'moduleStartup'"
        + ",millis:(new Date()).getTime(),type:'moduleEvalStart'});");
    out.newlineOpt();
    out.print("</script></head>");
    out.newlineOpt();
    out.print("<body>");
    out.newlineOpt();

    // Begin a script block inside the body. It's commented out so that the
    // browser won't mistake strings containing "<script>" for actual script.
    out.print("<script><!--");
    out.newline();
    return out.toString();
  }

}
