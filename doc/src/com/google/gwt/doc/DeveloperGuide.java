/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.doc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The GWT Developer Guide contains a variety of topics that explain the various
 * moving parts of the toolkit.
 * 
 * @title Developer Guide
 * @index Developer Guide
 * @index Google Web Toolkit, developer guide
 * @index GWT, developer's guide
 * @childIntro Topic Guide
 * @tip For a list of JRE classes that GWT can translate out of the box, see the
 *      <a href="jre.html">documentation for the GWT JRE emulation library</a>.
 */
public class DeveloperGuide {

  static class BogusImports {
    RootPanel rp;
    CellPanel cp;
    GWTTestCase gtc;
    JavaScriptObject jso;
    JavaScriptException jse;
  }

  /**
   * @title Fundamentals
   * 
   * @synopsis Core GWT concepts such as
   *           {@link Fundamentals.JavaToJavaScriptCompiler compiling Java source into JavaScript},
   *           {@link Fundamentals.HostedMode debugging},
   *           {@link Fundamentals.CrossBrowserSupport cross-browser support},
   *           and {@link Fundamentals.Modules defining modules}.
   * @childIntro
   */
  public static class Fundamentals {

    /**
     * The heart of GWT is a compiler that converts Java source into JavaScript,
     * transforming your working Java application into an equivalent JavaScript
     * application. Generally speaking,
     * <ol>
     * <li>If your GWT application compiles and runs in
     * {@link DeveloperGuide.Fundamentals.HostedMode hosted mode} as you expect</li>
     * <li>And GWT compiles your application into JavaScript output without
     * complaint,</li>
     * <li>Then your application will work the same way in a web browser as it
     * did in hosted mode.</li>
     * </ol>
     * The GWT compiler supports the vast majority of the Java language itself.
     * The GWT runtime library emulates a relevant subset of the Java runtime
     * library.
     * 
     * @title GWT Compiler
     * @synopsis A compiler that transforms your working Java application into
     *           an equivalent JavaScript application.
     * @childIntro Specifics
     */
    public static class JavaToJavaScriptCompiler {

      /**
       * GWT compiles Java source that is compatible with J2SE 1.4.2 or earlier.
       * 
       * <ul class="featurelist">
       * 
       * <li><div class="heading">Intrinsic types</div>
       * 
       * <div><code>byte</code>, <code>char</code>, <code>short</code>,
       * <code>int</code>, <code>long</code>, <code>float</code>,
       * <code>double</code>, <code>Object</code>, <code>String</code>,
       * and arrays are supported. However, there is no 64-bit integral type in
       * JavaScript, so variables of type <code>long</code> are mapped onto
       * JavaScript double-precision floating point values. To ensure maximum
       * consistency between hosted mode and web mode, we recommend that you use
       * <code>int</code> variables.</div> </li>
       * 
       * <li><div class="heading">Exceptions</div>
       * 
       * <div><code>try</code>, <code>catch</code>, <code>finally</code>
       * and user-defined exceptions are supported as normal, although
       * <code>Throwable.getStackTrace()</code> is not supported for web mode.
       * See {@link Throwable} for additional details.</div></li>
       * 
       * <li><div class="heading">Assertions</div>
       * 
       * <div>The GWT compiler parses Java <code>assert</code> statements, but
       * it does not emit code JavaScript code for them.</div></li>
       * 
       * <li><div class="heading">Multithreading and Synchronization</div>
       * 
       * <div> JavaScript interpreters are single-threaded, so while GWT
       * silently accepts the <code>synchronized</code> keyword, it has no
       * real effect. Synchronization-related library methods are not available,
       * including <code>Object.wait()</code>, <code>Object.notify()</code>,
       * and <code>Object.notifyAll()</code> </div> </li>
       * 
       * <li><div class="heading">Reflection</div>
       * 
       * <div>For maximum efficiency, GWT compiles your Java source into a
       * monolithic script, and does not support subsequent dynamic loading of
       * classes. This and other optimizations preclude general support for
       * reflection. It is possible to query an object for its class name using
       * {@link GWT#getTypeName(Object)}. </div> </li>
       * 
       * <li><div class="heading">Finalization</div>
       * 
       * <div>JavaScript does not support object finalization during garbage
       * collection, so GWT isn't able to be honor Java finalizers in web mode.
       * </div> </li>
       * 
       * <li><div class="heading">Strict Floating-Point</div>
       * 
       * <div>The Java language specification precisely defines floating-point
       * support, including single-precision and double-precision numbers as
       * well as the <code>strictfp</code> keyword. GWT does not support the
       * <code>strictfp</code> keyword and can't ensure any particular degree
       * of floating-point precision in translated code, so you may want to
       * avoid calculations in client-side code that require a guaranteed level
       * of floating-point precision.</div> </li>
       * 
       * </ul>
       * 
       * @title Language Support
       * @synopsis GWT supports most core Java 1.4 language semantics, but there
       *           are a few differences you'll want to be aware of.
       * @index synchronization support
       * @index Java language support
       * @index floating-point support
       * @index multithreading support
       * @see DeveloperGuide.Fundamentals.JavaToJavaScriptCompiler.JavaRuntimeSupport
       */
      public static class LanguageSupport {
      }

      /**
       * GWT supports only a small subset of the classes available in the Java 2
       * Standard and Enterprise Edition libraries, as these libraries are quite
       * large and rely on functionality that is unavailable within web
       * browsers. To find out exactly which classes and methods are supported
       * for core Java runtime packages, see the API reference for
       * <code>{@link java.lang java.lang}</code> and
       * <code>{@link java.util java.util}</code>, which lists supported
       * classes and contains notes on behavioral differences from the standard
       * Java runtime.
       * 
       * <p>
       * Some specific areas in which GWT emulation differs from the standard
       * Java runtime:
       * <ul class="featurelist">
       * <li><div class="heading">Regular Expressions</div> <div> The syntax
       * of <a
       * href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html">Java
       * regular expressions</a> is similar, but not identical, to <a
       * href="http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Guide:Regular_Expressions">JavaScript
       * regular expressions</a>. For example, the
       * {@link String#replaceAll(java.lang.String, java.lang.String) replaceAll}
       * and {@link String#split(java.lang.String) split} methods use regular
       * expressions. So, you'll probably want to be careful to only use Java
       * regular expressions that have the same meaning in JavaScript.</div>
       * </li>
       * 
       * <li><div class="heading">Serialization</div> <div>Java serialization
       * relies on a few mechanisms that are not available in compiled
       * JavaScript, such as dynamic class loading and reflection. As a result,
       * GWT does not support standard Java serialization. Instead, GWT has an
       * {@link DeveloperGuide.RemoteProcedureCalls RPC} facility that provides
       * automatic object serialization to and from the server for the purpose
       * of invoking remote methods. </div> </li>
       * </ul>
       * </p>
       * 
       * @title Runtime Library Support
       * @synopsis GWT emulates a basic set of the standard Java library
       *           classes.
       * @tip You'll save yourself a lot of frustration if you make sure that
       *      you use only translatable classes in your
       *      {@link DeveloperGuide.Fundamentals.ClientSide client-side code}
       *      from the very beginning. To help you identify problems early, your
       *      code is checked against the JRE emulation library whenever you run
       *      in {@link DeveloperGuide.Fundamentals.HostedMode hosted mode}. As
       *      a result, most uses of unsupported libraries will be caught the
       *      first time you attempt to run your application. So, <i>run early
       *      and often</i>.
       * 
       * @index regular expression support
       * @index Java runtime support
       * @see java.lang java.lang
       * @see java.util java.util
       */
      public static class JavaRuntimeSupport {
      }
    }

    /**
     * GWT shields you from worrying too much about cross-browser
     * incompatibilities. If you stick to built-in
     * {@link DeveloperGuide.UserInterface.WidgetsAndPanels widgets} and
     * {@link DeveloperGuide.UserInterface.CreatingCustomWidgets composites},
     * your applications will work similarly on the most recent versions of
     * Internet Explorer, Firefox, and Safari. (Opera, too, most of the time.)
     * DHTML user interfaces are remarkably quirky, though, so make sure to test
     * your applications thoroughly on every browser.
     * 
     * <p>
     * Whenever possible, GWT defers to browsers' native user interface
     * elements. For example, GWT's {@link Button} widget is a true HTML
     * <code>&lt;button&gt;</code> rather than a synthetic button-like widget
     * built, say, from a <code>&lt;div&gt;</code>. That means that GWT
     * buttons render appropriately in different browsers and on different
     * client operating systems. We like the native browser controls because
     * they're fast, accessible, and most familiar to users.
     * </p>
     * 
     * <p>
     * When it comes to styling web applications, <a
     * href="http://www.w3.org/Style/CSS/">CSS</a> is ideal. So, instead of
     * attempting to encapsulate UI styling behind a wall of
     * least-common-denominator APIs, GWT provides very few methods directly
     * related to style. Rather, developers are encouraged to define styles in
     * stylesheets that are linked to application code using
     * {@link com.google.gwt.user.client.ui.UIObject#setStyleName(String) style names}.
     * In addition to cleanly separating style from application logic, this
     * division of labor helps applications load and render more quickly,
     * consume less memory, and even makes them easier to tweak during
     * edit/debug cycles since there's no need to recompile for style tweaks.
     * </p>
     * 
     * @title Cross-browser Support
     * @synopsis The architecture of GWT makes it easy to support multiple
     *           browsers with a single code base.
     * @see DeveloperGuide.UserInterface.StyleSheets
     */
    public static class CrossBrowserSupport {
    }

    /**
     * You will spend most of your development time working in <i>hosted mode</i>,
     * which means that you are interacting with your GWT application without it
     * having been translated into JavaScript. Anytime you edit, run, and debug
     * applications from a Java integrated development environment (IDE), you
     * are working in hosted mode. When running in hosted mode, the Java Virtual
     * Machine (JVM) is actually executing your application code as compiled
     * Java bytecode, using GWT plumbing to automate an embedded browser window.
     * By remaining in this traditional "code-test-debug" cycle, hosted mode is
     * by far the most productive way to develop your application quickly.
     * 
     * <p>
     * To launch a hosted mode session, your startup class should be
     * <code>com.google.gwt.dev.GWTShell</code>, found in
     * <code>gwt-dev-windows.jar</code> (or <code>gwt-dev-linux.jar</code>).
     * </p>
     * 
     * @title Debugging in Hosted Mode
     * @synopsis An embedded DHTML browser lets you run and debug applications
     *           directly in any Java development environment before being
     *           translated into JavaScript.
     * @tip In hosted mode, the GWT development shell looks for
     *      {@link DeveloperGuide.Fundamentals.Modules modules} (and therefore
     *      {@link DeveloperGuide.Fundamentals.ClientSide client-side} source)
     *      using the JVM's classpath. Make sure to add your source directories
     *      first in your classpath when running the development shell.
     */
    public static class HostedMode {
    }

    /**
     * As you move from development into end-to-end testing and production, you
     * will begin to interact with your application in web mode more often.
     * <i>Web mode</i> refers to accessing your application from a normal
     * browser -- where it runs as pure JavaScript -- as it is ultimately
     * intended to be deployed.
     * 
     * <p>
     * To create a web mode version of your module, you compile it using either
     * the "Compile/Browse" button available in the hosted browser window or the
     * command-line compiler <code>com.google.gwt.dev.GWTCompiler</code>.
     * </p>
     * 
     * <p>
     * Web mode demonstrates what makes GWT unusual: when your application is
     * launched in web mode, it runs completely as JavaScript and does not
     * require any browser plug-ins or JVM.
     * </p>
     * 
     * @title Deployment in Web Mode
     * @synopsis Compile your application into JavaScript for easy deployment.
     */
    public static class WebMode {
    }

    /**
     * Any HTML page containing the proper incantation can include code created
     * with GWT, referred to as a <i>host page</i>. A typical HTML host page
     * looks like this:
     * 
     * <pre class="code">
     * &lt;html&gt;
     *  &lt;head&gt;
     *  
     *    &lt;!-- Properties can be specified to influence deferred binding --&gt;
     *    &lt;meta name='gwt:property' content='locale=en_UK'&gt;
     *    
     *    &lt;!-- Stylesheets are optional, but useful --&gt;
     *    &lt;link rel="stylesheet" href="Calendar.css"&gt;
     *    
     *    &lt;!-- Titles are optional, but useful --&gt;
     *    &lt;title&gt;Calendar App&lt;/title&gt;
     *    
     *  &lt;/head&gt;
     *  &lt;body&gt;
     *   
     *    &lt;!-- The fully-qualified module name, followed by 'nocache.js' --&gt;
     *    &lt;script language="javascript" src="com.example.cal.Calendar.nocache.js"&gt;&lt;/script&gt;
     *    
     *    &lt;!-- Include a history iframe to enable full GWT history support --&gt;
     *    &lt;!-- (the id must be exactly as shown)                           --&gt;
     *    &lt;iframe id="__gwt_historyFrame" style="width:0;height:0;border:0"&gt;&lt;/iframe&gt;
     *    
     *  &lt;/body&gt;
     * &lt;/html&gt;
     * </pre>
     * 
     * The structure was designed to make it easy to add GWT functionality to
     * existing web applications with only minor changes.
     * 
     * @title HTML Host Pages
     * @synopsis A host page is an HTML document that includes a GWT module.
     */
    public static class HostPage {
    }

    /**
     * Your application is sent across a network to a user, where it runs as
     * JavaScript inside his or her web browser. Everything that happens within
     * your user's web browser is referred to as <i>client-side</i> processing.
     * When you write client-side code that is intended to run in the web
     * browser, remember that it ultimately becomes JavaScript. Thus, it is
     * important to use only libraries and Java language constructs that can be
     * {@link DeveloperGuide.Fundamentals.JavaToJavaScriptCompiler translated}.
     * 
     * @title Client-side Code
     * @index client-side
     * @synopsis "Client-side" refers to source code that is intended to be
     *           translated and run in a web browser as JavaScript.
     */
    public static class ClientSide {
    }

    /**
     * Everything that happens within your server computer is referred to as
     * <i>server-side</i> processing. When your application needs to interact
     * with your server (for example, to load or save data), it makes a
     * client-side request (from the browser) across the network using a
     * {@link DeveloperGuide.RemoteProcedureCalls remote procedure call (RPC)}.
     * While processing an RPC, your server is executing server-side code.
     * 
     * @title Server-side Code
     * @index server-side
     * @synopsis "Server-side" refers to source code that is not intended to be
     *           translated and will only run on a server as bytecode.
     * @tip GWT doesn't meddle with your ability to run Java bytecode on your
     *      server whatsoever. Server-side code doesn't need to be translatable,
     *      so you're free to use any Java library you find useful.
     */
    public static class ServerSide {
    }

    /**
     * GWT projects are overlaid onto Java packages such that most of the
     * configuration can be inferred from the classpath and your
     * {@link DeveloperGuide.Fundamentals.Modules module definitions}.
     * 
     * <p>
     * If you are starting a GWT project from scratch, you should use the
     * standard GWT package layout, which makes it easy to differentiate
     * {@link DeveloperGuide.Fundamentals.ClientSide client-side code} from
     * {@link DeveloperGuide.Fundamentals.ServerSide server-side code}. For
     * example, suppose your new project is called "Calendar". The standard
     * package layout would look like this:
     * </p>
     * 
     * <table width="80%" align="center">
     * <tr>
     * <th width="50%" align="left"> Package </th>
     * <th width="50%" align="left"> Purpose </th>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/</code></td>
     * <td>The project root package contains
     * {@link DeveloperGuide.Fundamentals.Modules.ModuleXml module XML files}</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/client/</code></td>
     * <td>Client-side source files and subpackages</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/server/</code></td>
     * <td>Server-side code and subpackages</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/public/</code></td>
     * <td>Static resources that can be served publicly</td>
     * </tr>
     * </table>
     * 
     * <p>
     * and examples files would be arranged like so:
     * </p>
     * 
     * <table width="80%" align="center">
     * <tr>
     * <th width="50%" align="left">File</th>
     * <th width="50%" align="left">Purpose</th>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/Calendar.gwt.xml</code></td>
     * <td>A common base {@link DeveloperGuide.Fundamentals.Modules module} for
     * your project that inherits <code>com.google.gwt.user.User</code> module</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/CalendarApp.gwt.xml</code></td>
     * <td>Inherits the <code>com.example.cal.Calendar</code> module (above)
     * and adds an entry point class</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/CalendarTest.gwt.xml</code></td>
     * <td>A {@link DeveloperGuide.Fundamentals.Modules module} defined by your
     * project</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/client/CalendarApp.java</code></td>
     * <td>Client-side Java source for the entry-point class</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/client/spelling/SpellingService.java</code></td>
     * <td>An RPC service interface defined in a subpackage</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/server/spelling/SpellingServiceImpl.java</code></td>
     * <td>Server-side Java source that implements the logic of the spelling
     * service</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/public/Calendar.html</code></td>
     * <td>An HTML page that loads the calendar app</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/public/Calendar.css</code></td>
     * <td>A stylesheet that styles the calendar app</td>
     * </tr>
     * <tr>
     * <td><code>com/example/cal/public/images/logo.gif</code></td>
     * <td>A logo</td>
     * </tr>
     * </table>
     * 
     * @title Project Structure
     * @synopsis GWT projects are built from a recommended package layout.
     * @tip The easiest way to create a GWT project from scratch is to use the
     *      {@link DeveloperGuide.Fundamentals.CommandLineTools.projectCreator projectCreator script}.
     */
    public static class ProjectStructure {
    }

    /**
     * Individual units of GWT configuration are XML files called <i>modules</i>.
     * A module bundles together all the configuration settings that your GWT
     * project needs, namely
     * 
     * <ul>
     * <li>Inherited modules</li>
     * <li>An entry point application class name; these are optional, although
     * any module referred to in HTML must have at least one entry-point class
     * specified</li>
     * <li>Source path entries</li>
     * <li>Public path entries</li>
     * <li>Deferred binding rules, including property providers and class
     * generators</li>
     * </ul>
     * 
     * Modules may appear in any package in your classpath, although it is
     * strongly recommended that they appear in the root package of a
     * {@link DeveloperGuide.Fundamentals.ProjectStructure standard project layout}.
     * 
     * <h2>Entry-Point Classes</h2>
     * A module entry-point is any class that is assignable to
     * {@link com.google.gwt.core.client.EntryPoint} and that can be constructed
     * without parameters. When a module is loaded, every entry point class is
     * instantiated and its
     * {@link com.google.gwt.core.client.EntryPoint#onModuleLoad()} method gets
     * called.
     * 
     * <h2>Source Path</h2>
     * Modules can specify which subpackages contain translatable <i>source</i>,
     * causing the named package and its subpackages to be added to the
     * <i>source path</i>. Only files found on the source path are candidates
     * to be translated into JavaScript, making it possible to mix
     * {@link DeveloperGuide.Fundamentals.ClientSide client-side} and
     * {@link DeveloperGuide.Fundamentals.ServerSide server-side} code together
     * in the same classpath without conflict.
     * 
     * <p>
     * When module inherit other modules, their source paths are combined so
     * that each module will have access to the translatable source it requires.
     * </p>
     * 
     * <h2>Public Path</h2>
     * Modules can specify which subpackages are <i>public</i>, causing the
     * named package and its subpackages to be added to the <i>public path</i>.
     * When you compile your application into JavaScript, all the files that can
     * be found on your public path are copied to the module's output directory.
     * The net effect is that user-visible URLs need not include a full package
     * name.
     * 
     * <p>
     * When module inherit other modules, their public paths are combined so
     * that each module will have access to the static resources it expects.
     * </p>
     * 
     * @title Modules
     * @synopsis Modules are XML files that contain settings related to your
     *           application or library.
     * @childIntro Specifics
     */
    public static class Modules {

      /**
       * Modules are defined in XML files whose file extension is
       * <code>.gwt.xml</code>. Module XML files should reside in your
       * project's root package.
       * 
       * <p>
       * If you are using the
       * {@link DeveloperGuide.Fundamentals.ProjectStructure standard project structure},
       * your module XML can be as simple as this:
       * 
       * <pre class="code">
       * &lt;module&gt;
       *    &lt;inherits name="com.google.gwt.user.User"/&gt;
       *    &lt;entry-point class="com.example.cal.client.CalendarApp"/&gt;
       * &lt;/module&gt;</pre>
       * 
       * </p>
       * 
       * <h2>Loading Modules</h2>
       * Module XML files are found on the Java classpath, referenced by their
       * logical module names from
       * {@link DeveloperGuide.Fundamentals.HostPage host pages} and by being
       * inherited by other modules.
       * 
       * <p>
       * Modules are always referred to by their logical names. The logical name
       * of a module is of the form <code>pkg1.pkg2.ModuleName</code>
       * (although any number of packages may be present) and includes neither
       * the actual file system path nor the file extension. For example, the
       * logical name of a module XML file located at
       * 
       * <pre>~/src/com/example/cal/Calendar.gwt.xml</pre>
       * 
       * is
       * 
       * <pre>com.example.cal.Calendar</pre>
       * 
       * </p>
       * 
       * <h2>Available Elements</h2>
       * <dl class="fixed">
       * 
       * <dt>&lt;inherits name="<i>logical-module-name</i>"/&gt;</dt>
       * <dd>Inherits all the settings from the specified module as if the
       * contents of the inherited module's XML were copied verbatim. Any number
       * of modules can be inherited in this manner.</dd>
       * 
       * <dt>&lt;entry-point class="<i>classname</i>"/&gt;</dt>
       * <dd>Specifies an
       * {@link com.google.gwt.core.client.EntryPoint entry point} class. Any
       * number of entry-point classes can be added, including those from
       * inherited modules.</dd>
       * 
       * <dt>&lt;source path="<i>path</i>"/&gt;</dt>
       * <dd>Adds packages to the
       * {@link DeveloperGuide.Fundamentals.Modules source path} by combining
       * the package in which the module XML is found with the specified path to
       * a subpackage. Any Java source file appearing in this subpackage or any
       * of its subpackages is assumed to be translatable.
       * <p>
       * If no <code>&lt;source&gt;</code> element is defined in a module XML
       * file, the <code>client</code> subpackage is implicitly added to the
       * source path as if <code>&lt;source path="client"&gt;</code> had been
       * found in the XML. This default helps keep module XML compact for
       * standard project layouts.
       * </p>
       * </dd>
       * 
       * <dt>&lt;public path="<i>path</i>"/&gt;</dt>
       * <dd>Adds packages to the
       * {@link DeveloperGuide.Fundamentals.Modules public path} by combining
       * the package in which the module XML is found with the specified path to
       * identify the root of a public path entry. Any file appearing in this
       * package or any of its subpackages will be treated as a
       * publicly-accessible resource. The <code>&lt;public&gt;</code> element
       * supports
       * {@link DeveloperGuide.Fundamentals.Modules.PublicPackageFiltering pattern-based filtering}
       * to allow fine-grained control over which resources get copied into the
       * output directory during a GWT compile.
       * 
       * <p>
       * If no <code>&lt;public&gt;</code> element is defined in a module XML
       * file, the <code>public</code> subpackage is implicitly added to the
       * public path as if <code>&lt;public path="public"&gt;</code> had been
       * found in the XML. This default helps keep module XML compact for
       * standard project layouts.
       * </p>
       * </dd>
       * 
       * <dt>&lt;servlet path="<i>url-path</i>" class="<i>classname</i>"/&gt;</dt>
       * <dd>For convenient RPC testing, this element loads a servlet class
       * mounted at the specified URL path. The URL path should be absolute and
       * have the form of a directory (for example, <code>/spellcheck</code>).
       * Your client code then specifies this URL mapping in a call to
       * {@link ServiceDefTarget#setServiceEntryPoint(String)}. Any number of
       * servlets may be loaded in this manner, including those from inherited
       * modules.</dd>
       * 
       * <dt>&lt;script src="<i>js-url</i>"/&gt;</dt>
       * <dd>Automatically injects the external JavaScript file located at the
       * location specified by <i>src</i>. See
       * {@link DeveloperGuide.Fundamentals.Modules.AutomaticResourceInjection automatic resource inclusion}
       * for details.</dd>
       * 
       * <dt>&lt;stylesheet src="<i>css-url</i>"/&gt;</dt>
       * <dd>Automatically injects the external CSS file located at the
       * location specified by <i>src</i>. See
       * {@link DeveloperGuide.Fundamentals.Modules.AutomaticResourceInjection automatic resource inclusion}
       * for details.</dd>
       * 
       * <dt>&lt;extend-property name="<i>client-property-name</i>" values="<i>comma-separated-values</i>"/&gt;</dt>
       * <dd>Extends the set of values for an existing client property. Any
       * number of values may be added in this manner, and client property
       * values accumulate through inherited modules. You will likely only find
       * this useful for
       * {@link com.google.gwt.doc.DeveloperGuide.Internationalization.SpecifyingLocale specifying locales in internationalization}.</dd>
       * 
       * </dl>
       * 
       * @title Module XML Format
       * @synopsis Modules are defined in XML and placed into your
       *           {@link DeveloperGuide.Fundamentals.ProjectStructure project's package hierarchy}.
       */
      public static class ModuleXml {
      }

      /**
       * Modules can contain references to external JavaScript and CSS files,
       * causing them to be automatically loaded when the module itself is
       * loaded.
       * 
       * <h2>Including External JavaScript</h2>
       * Script inclusion is a convenient way to automatically associate
       * external JavaScript files with your module. Use the following syntax to
       * cause an external JavaScript file to be loaded into the
       * {@link HostPage host page} before your module entry point is called.
       * 
       * <pre class="code">&lt;script src="<i>js-url</i>"/&gt;</pre>
       * 
       * The script is loaded into the namespace of the
       * {@link DeveloperGuide.Fundamentals.HostPage host page} as if you had
       * included it explicitly using the HTML <code>&lt;script&gt;</code>
       * element. The script will be loaded before your
       * {@link com.google.gwt.core.client.EntryPoint#onModuleLoad() onModuleLoad()}
       * is called.
       * 
       * <h2>Including External Stylesheets</h2>
       * Stylesheet inclusion is a convenient way to automatically associate
       * external CSS files with your module. Use the following syntax to cause
       * a CSS file to be automatically attached to the
       * {@link HostPage host page}.
       * 
       * <pre class="code">&lt;stylesheet src="<i>css-url</i>"/&gt;</pre>
       * 
       * You can add any number of stylesheets this way, and the order of
       * inclusion into the page reflects the order in which the elements appear
       * in your module XML.
       * 
       * <h2>Inclusion and Module Inheritance</h2>
       * Module inheritance makes resource inclusion particularly convenient. If
       * you wish to create a reusable library that relies upon particular
       * stylesheets or JavaScript files, you can be sure that clients of your
       * library have everything they need automatically by inheriting from your
       * module.
       * 
       * @title Automatic Resource Inclusion
       * @synopsis Modules can contain references to external JavaScript and CSS
       *           files, causing them to be automatically loaded when the
       *           module itself is loaded.
       * @see DeveloperGuide.Fundamentals.Modules.ModuleXml
       * @tip Versions of GWT prior to 1.4 required a script-ready function to
       *      determine when an included script was loaded. This is no longer
       *      required; all included scripts will be loaded when your
       *      application starts, in the order in which they are declared.
       */
      public static class AutomaticResourceInjection {
      }

      /**
       * The <code>&lt;public&gt;</code> element supports certain attributes
       * and nested elements to allow pattern-based inclusion and exclusion. It
       * follows the same rules as <a href="http://ant.apache.org/">Ant</a>'s
       * <code>FileSet</code> element. Please see the <a
       * href="http://ant.apache.org/manual/CoreTypes/fileset.html">documentation</a>
       * for <code>FileSet</code> for a general overview.
       * 
       * <p>
       * The <code>&lt;public&gt;</code> element does not support the full
       * <code>FileSet</code> semantics. Only the following attributes and
       * nested elements are currently supported:
       * <ul>
       * <li>The <code>includes</code> attribute</li>
       * <li>The <code>excludes</code> attribute</li>
       * <li>The <code>defaultexcludes</code> attribute</li>
       * <li>The <code>casesensitive</code> attribute</li>
       * <li>Nested <code>include</code> tags</li>
       * <li>Nested <code>exclude</code> tags</li>
       * </ul>
       * Other attributes and nested elements are not supported.
       * </p>
       * 
       * <h3>Important</h3>
       * The default value of <code>defaultexcludes</code> is
       * <code>true</code>. By default, the patterns listed <a
       * href="http://ant.apache.org/manual/dirtasks.html#defaultexcludes">here</a>
       * are excluded.
       * 
       * @title Filtering Public Packages
       * @synopsis Filter files into and out of your public path to avoid
       *           publishing files unintentionally.
       */
      public static class PublicPackageFiltering {
      }
    }

    /**
     * GWT comes with a few handy command-line tools to get you up and running
     * quickly.
     * 
     * They are also useful for adding new things to existing projects. For
     * example, <code>projectCreator</code> could be used to make an Eclipse
     * project for one of the samples that comes with GWT.
     * 
     * @title Command-line Tools
     * @synopsis Useful command-line tools for getting started.
     * @childIntro
     */
    public static class CommandLineTools {

      /**
       * Generates an <a href="http://ant.apache.org/">Ant</a> buildfile or <a
       * href="http://www.eclipse.org">Eclipse</a> project.
       * 
       * <p>
       * <code>projectCreator [-ant projectName] [-eclipse projectName] [-out dir] [-overwrite] [-ignore]</code>
       * </p>
       * 
       * <table width="80%" align="center">
       * <tr>
       * <td><code>-ant</code></td>
       * <td>Generate an Ant buildfile to compile source (<code>.ant.xml</code>
       * will be appended)</td>
       * </tr>
       * <tr>
       * <td><code>-eclipse</code></td>
       * <td>Generate an eclipse project</td>
       * </tr>
       * <tr>
       * <td><code>-out</code></td>
       * <td>The directory to write output files into (defaults to current)</td>
       * </tr>
       * <tr>
       * <td><code>-overwrite</code></td>
       * <td>Overwrite any existing files</td>
       * </tr>
       * <tr>
       * <td><code>-ignore</code></td>
       * <td>Ignore any existing files; do not overwrite</td>
       * </tr>
       * </table>
       * 
       * <h2>Example</h2>
       * 
       * <pre class="code">
       * ~/Foo> projectCreator -ant Foo -eclipse Foo
       * Created directory src
       * Created directory test
       * Created file Foo.ant.xml
       * Created file .project
       * Created file .classpath</pre>
       * 
       * <p>
       * Running <code>ant -f Foo.ant.xml</code> will compile <code>src</code>
       * into <code>bin</code>. The buildfile also contains a
       * <code>package</code> target for bundling the project into a jar.
       * </p>
       * 
       * <p>
       * <code>.project</code> can be imported into an Eclipse workspace.
       * </p>
       * 
       * @title projectCreator
       * @synopsis Generates a basic project skeleton and an optional Ant
       *           buildfile and Eclipse project.
       */
      public static class projectCreator {
      }

      /**
       * Generates a starter application and scripts for launching
       * {@link DeveloperGuide.Fundamentals.HostedMode hosted mode} and
       * {@link DeveloperGuide.Fundamentals.JavaToJavaScriptCompiler compiling to JavaScript}.
       * 
       * <p>
       * <code>applicationCreator [-eclipse projectName] [-out dir] [-overwrite] [-ignore] className</code>
       * </p>
       * 
       * <table width="80%" align="center">
       * <tr>
       * <td><code>-eclipse</code></td>
       * <td>Creates a debug launch configuration for the named eclipse project</td>
       * </tr>
       * <tr>
       * <td><code>-out</code></td>
       * <td>The directory to write output files into (defaults to current)</td>
       * </tr>
       * <tr>
       * <td><code>-overwrite</code></td>
       * <td>Overwrite any existing files</td>
       * </tr>
       * <tr>
       * <td><code>-ignore</code></td>
       * <td>Ignore any existing files; do not overwrite</td>
       * </tr>
       * <tr>
       * <td><code>className</code></td>
       * <td>The fully-qualified name of the application class to create</td>
       * </tr>
       * </table>
       * 
       * <h2>Example</h2>
       * 
       * <pre class="code">
       * ~/Foo> applicationCreator -eclipse Foo com.example.foo.client.Foo
       * Created directory src/com/example/foo/client
       * Created directory src/com/example/foo/public
       * Created file src/com/example/foo/Foo.gwt.xml
       * Created file src/com/example/foo/public/Foo.html
       * Created file src/com/example/foo/client/Foo.java
       * Created file Foo.launch
       * Created file Foo-shell
       * Created file Foo-compile</pre>
       * 
       * Running <code>Foo-shell</code> brings up the new app in hosted mode.
       * <code>Foo-compile</code> translates the Java app to JavaScript,
       * creating a web folder under <code>www</code>.
       * <code>Foo.launch</code> is a launch configuration for Eclipse.
       * 
       * @title applicationCreator
       * @synopsis Generate a starter application.
       */
      public static class applicationCreator {
      }

      /**
       * Generates a {@link DeveloperGuide.JUnitIntegration JUnit test} and
       * scripts for testing in both
       * {@link DeveloperGuide.Fundamentals.HostedMode hosted mode} and
       * {@link DeveloperGuide.Fundamentals.WebMode web mode}.
       * 
       * <p>
       * <code>junitCreator -junit pathToJUnitJar [-eclipse projectName] [-out dir] [-overwrite] [-ignore] className</code>
       * </p>
       * 
       * <table width="80%" align="center">
       * <tr>
       * <td><code>-junit</code></td>
       * <td>Specify the path to your junit.jar (required)</td>
       * </tr>
       * <tr>
       * <td><code>-module</code></td>
       * <td>Specify name of the application module to use (required)</td>
       * </tr>
       * <tr>
       * <td><code>-eclipse</code></td>
       * <td>Creates a debug launch configuration for the named eclipse project</td>
       * </tr>
       * <tr>
       * <td><code>-out</code></td>
       * <td>The directory to write output files into (defaults to current)</td>
       * </tr>
       * <tr>
       * <td><code>-overwrite</code></td>
       * <td>Overwrite any existing files</td>
       * </tr>
       * <tr>
       * <td><code>-ignore</code></td>
       * <td>Ignore any existing files; do not overwrite</td>
       * </tr>
       * <tr>
       * <td><code>className</code></td>
       * <td>The fully-qualified name of the test class to create</td>
       * </tr>
       * </table>
       * 
       * <h2>Example</h2>
       * 
       * <pre class="code">
       * ~/Foo> junitCreator -junit /opt/eclipse/plugins/org.junit_3.8.1/junit.jar
       *        -module com.example.foo.Foo
       *        -eclipse Foo com.example.foo.client.FooTest
       * Created directory test/com/example/foo/test
       * Created file test/com/example/foo/client/FooTest.java
       * Created file FooTest-hosted.launch
       * Created file FooTest-web.launch
       * Created file FooTest-hosted
       * Created file FooTest-web</pre>
       * 
       * Running <code>FooTest-hosted</code> tests as Java bytecode in a JVM.
       * <code>FooTest-web</code> tests as compiled JavaScript. The launch
       * configurations do the same thing in Eclipse.
       * 
       * @title junitCreator
       * @synopsis Generate a JUnit test.
       */
      public static class junitCreator {
      }

      /**
       * Generates
       * {@link DeveloperGuide.Internationalization internationalization}
       * scripts for
       * {@link DeveloperGuide.Internationalization.StaticStringInternationalization static
       * internationalization}, along with sample
       * {@link DeveloperGuide.Internationalization.PropertiesFiles properties files}.
       * 
       * <p>
       * <code>i18nCreator [-eclipse projectName] [-out dir] [-overwrite] [-ignore] [-createMessages] interfaceName</code>
       * </p>
       * 
       * <table width="80%" align="center">
       * <tr>
       * <td><code>-eclipse</code></td>
       * <td>Creates a debug launch config for the named Eclipse project</td>
       * </tr>
       * <tr>
       * <td><code>-out</code></td>
       * <td>The directory to write output files into (defaults to current)</td>
       * </tr>
       * <tr>
       * <td><code>-overwrite</code></td>
       * <td>Overwrite any existing files</td>
       * </tr>
       * <tr>
       * <td><code>-ignore</code></td>
       * <td>Ignore any existing files; do not overwrite</td>
       * </tr>
       * <tr>
       * <td><code>-createMessages</code></td>
       * <td>Generate scripts for a Messages interface rather than a Constants
       * one</td>
       * </tr>
       * <tr>
       * <td><code>interfaceName</code></td>
       * <td>The fully-qualified name of the interface to create</td>
       * </tr>
       * </table>
       * 
       * <h2>Example</h2>
       * 
       * <pre class="code">
       * ~/Foo> i18nCreator -eclipse Foo -createMessages com.example.foo.client.FooMessages
       * Created file src/com/example/foo/client/FooMessages.properties
       * Created file FooMessages-i18n.launch
       * Created file FooMessages-i18n
       * 
       * ~/Foo> i18nCreator -eclipse Foo com.example.foo.client.FooConstants
       * Created file src/com/example/foo/client/FooConstants.properties
       * Created file FooConstants-i18n.launch
       * Created file FooConstants-i18n</pre>
       * 
       * <p>
       * Running <code>FooMessages-i18n</code> will generate an interface from
       * <code>FooMessages.properties</code> that extends
       * <code>Messages</code> (The messages will take parameters,
       * substituting <code>{n}</code> with the nth parameter).
       * </p>
       * <p>
       * Running <code>FooConstants-i18n</code> will generate an interface
       * from <code>FooConstants.properties</code> that extends
       * <code>Constants</code> (The constants will not take parameters).
       * </p>
       * The launch configurations do the same thing as the scripts, from within
       * Eclipse.
       * 
       * @title i18nCreator
       * @synopsis Generate an i18n properties file and synchronization script.
       */
      public static class i18nCreator {
      }

    }
  }

  /**
   * GWT user interface classes are similar to those in existing UI frameworks
   * such as <a href =
   * "http://java.sun.com/j2se/1.4.2/docs/api/javax/swing/package-summary.html">
   * Swing</a> and <a href="http://www.eclipse.org/swt/">SWT</a> except that
   * the widgets are rendered using dynamically-created HTML rather than
   * pixel-oriented graphics.
   * 
   * <p>
   * While it is possible to manipulate the browser's DOM directly using the
   * {@link com.google.gwt.user.client.DOM} interface, it is far easier to use
   * classes from the {@link com.google.gwt.user.client.ui.Widget} hierarchy.
   * You should rarely, if ever, need to access the DOM directly. Using widgets
   * makes it much easier to quickly build interfaces that will work correctly
   * on all browsers.
   * </p>
   * 
   * @title Building User Interfaces
   * @synopsis As shown in
   *           {@link DeveloperGuide.UserInterface.WidgetGallery the gallery},
   *           GWT includes a variety of pre-built Java
   *           {@link DeveloperGuide.UserInterface.WidgetsAndPanels widgets and panels}
   *           that serve as cross-browser building blocks for your application.
   *           GWT also includes unique and powerful optimization facilities
   *           such as {@link ImageBundles image bundles}.
   * @childIntro Specifics
   */
  public static class UserInterface {

    /**
     * GWT applications construct user interfaces using {@link Widget widgets}
     * that are contained within
     * {@link com.google.gwt.user.client.ui.Panel panels}. Examples of widgets
     * include {@link Button}, {@link TextBox}, and
     * {@link com.google.gwt.user.client.ui.Tree}.
     * 
     * <p>
     * Widgets and panels work the same way on all browsers; by using them, you
     * eliminate the need to write specialized code for each browser. But you
     * are not limited to the set of widgets provided by the toolkit. There are
     * {@link DeveloperGuide.UserInterface.CreatingCustomWidgets a number of ways}
     * to create custom widgets yourself.
     * </p>
     * 
     * <h2>Panels</h2>
     * <p>
     * Panels, such as {@link com.google.gwt.user.client.ui.DockPanel},
     * {@link com.google.gwt.user.client.ui.HorizontalPanel}, and
     * {@link com.google.gwt.user.client.ui.RootPanel}, contain widgets and are
     * used to define
     * {@link DeveloperGuide.UserInterface.UnderstandingLayout how they are laid out}
     * in the browser.
     * </p>
     * 
     * <h2>Styles</h2>
     * <p>
     * Visual styles are applied to widgets using Cascading Style Sheets (CSS).
     * {@link DeveloperGuide.UserInterface.StyleSheets This section} describes
     * in detail how to use this feature.
     * </p>
     * 
     * @title Widgets and Panels
     * @synopsis Widgets and panels are
     *           {@link DeveloperGuide.Fundamentals.ClientSide client-side} Java
     *           classes used to build user interfaces.
     */
    public static class WidgetsAndPanels {
    }

    /**
     * The following are {@link Widget widgets} and
     * {@link com.google.gwt.user.client.ui.Panel panels} available in the GWT
     * user-interface library. <br/>
     * 
     * <table style='text-align:center'>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.Button.html'>Button</a></td>
     * <td><a href='com.google.gwt.user.client.ui.RadioButton.html'>RadioButton</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='Button.png'/> </td>
     * <td><img class='gallery' src='RadioButton.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.CheckBox.html'>CheckBox</a></td>
     * <td><a href='com.google.gwt.user.client.ui.TextBox.html'>TextBox</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='CheckBox.png'/> </td>
     * <td><img class='gallery' src='TextBox.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a
     * href='com.google.gwt.user.client.ui.PasswordTextBox.html'>PasswordTextBox</a></td>
     * <td><a href='com.google.gwt.user.client.ui.TextArea.html'>TextArea</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='PasswordTextBox.png'/> </td>
     * <td><img class='gallery' src='TextArea.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.Hyperlink.html'>Hyperlink</a></td>
     * <td><a href='com.google.gwt.user.client.ui.ListBox.html'>ListBox</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='Hyperlink.png'/> </td>
     * <td><img class='gallery' src='ListBox.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.MenuBar.html'>MenuBar</a></td>
     * <td><a href='com.google.gwt.user.client.ui.Tree.html'>Tree</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='MenuBar.png'/> </td>
     * <td><img class='gallery' src='Tree.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.HTMLTable.html'>Table</a></td>
     * <td><a href='com.google.gwt.user.client.ui.TabBar.html'>TabBar</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='Table.png'/> </td>
     * <td><img class='gallery' src='TabBar.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.DialogBox.html'>DialogBox</a></td>
     * <td><a href='com.google.gwt.user.client.ui.PopupPanel.html'>PopupPanel</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='DialogBox.png'/> </td>
     * <td><img class='gallery' src='PopupPanel.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.StackPanel.html'>StackPanel</a></td>
     * <td><a
     * href='com.google.gwt.user.client.ui.HorizontalPanel.html'>HorizontalPanel</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='StackPanel.png'/> </td>
     * <td><img class='gallery' src='HorizontalPanel.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a
     * href='com.google.gwt.user.client.ui.VerticalPanel.html'>VerticalPanel</a></td>
     * <td><a href='com.google.gwt.user.client.ui.FlowPanel.html'>FlowPanel</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='VerticalPanel.png'/> </td>
     * <td><img class='gallery' src='FlowPanel.png'/> </td>
     * </tr>
     * 
     * <tr class='gallery-link'>
     * <td><a href='com.google.gwt.user.client.ui.DockPanel.html'>DockPanel</a></td>
     * <td><a href='com.google.gwt.user.client.ui.TabPanel.html'>TabPanel</a></td>
     * </tr>
     * 
     * <tr class='gallery'>
     * <td><img class='gallery' src='DockPanel.png'/> </td>
     * <td><img class='gallery' src='TabPanel.png'/> </td>
     * </tr>
     * 
     * </table>
     * 
     * @title Widgets Gallery
     * @synopsis A gallery of widgets and panels.
     */
    public static class WidgetGallery {
    }

    /**
     * Events in GWT use the "listener interface" model similar to other user
     * interface frameworks. A listener interface defines one or more methods
     * that the widget calls to announce an event. A class wishing to receive
     * events of a particular type implements the associated listener interface
     * and then passes a reference to itself to the widget to "subscribe" to a
     * set of events.
     * 
     * <p>
     * The {@link com.google.gwt.user.client.ui.Button} class, for example,
     * publishes click events. The associated listener interface is
     * {@link com.google.gwt.user.client.ui.ClickListener}.
     * </p>
     * 
     * {@example #anonClickListenerExample()}
     * 
     * Using anonymous inner classes as in the above example can be inefficient
     * for a large number of widgets, since it could result in the creation of
     * many listener objects. Widgets supply their <code>this</code> pointer
     * as the <code>sender</code> parameter when they invoke a listener
     * method, allowing a single listener to distinguish between multiple event
     * publishers. This makes better use of memory but requires slightly more
     * code, as shown in the following example:
     * 
     * {@example ListenerExample}
     * 
     * Some event interfaces specify more than one event. If you are only
     * interested in a subset of these events, subclass one of the event
     * "adapters". Adapters are simply empty concrete implementations of a
     * particular event interface, from which you can derive a listener class
     * without having to implement every method.
     * 
     * {@example #adapterExample()}
     * 
     * @title Events and Listeners
     * @synopsis Widgets publish events using the well-known listener pattern.
     */
    public static class EventsAndListeners {

      /**
       * @skip
       */
      public class ListenerExample extends Composite implements ClickListener {
        private FlowPanel fp = new FlowPanel();
        private Button b1 = new Button("Button 1");
        private Button b2 = new Button("Button 2");

        public ListenerExample() {
          initWidget(fp);
          fp.add(b1);
          fp.add(b2);
          b1.addClickListener(this);
          b2.addClickListener(this);
        }

        public void onClick(Widget sender) {
          if (sender == b1) {
            // handle b1 being clicked
          } else if (sender == b2) {
            // handle b2 being clicked
          }
        }
      }

      public void anonClickListenerExample() {
        Button b = new Button("Click Me");
        b.addClickListener(new ClickListener() {
          public void onClick(Widget sender) {
            // handle the click event
          }
        });
      }

      public void adapterExample() {
        TextBox t = new TextBox();
        t.addKeyboardListener(new KeyboardListenerAdapter() {
          public void onKeyPress(Widget sender, char keyCode, int modifiers) {
            // handle only this one event
          }
        });
      }
    }

    /**
     * GWT makes it easy to create custom widgets entirely in the Java language.
     * 
     * <h2>Composites</h2>
     * Composites are by far the most effective way to create new widgets. You
     * can easily combine groups of existing widgets into a composite that is
     * itself a reusable widget. {@link Composite} is a specialized widget that
     * can contain another component (typically, a
     * {@link com.google.gwt.user.client.ui.Panel panel}) but behaves as if it
     * were its contained widget. Using {@link Composite} is preferable to
     * attempting to create complex widgets by subclassing
     * {@link com.google.gwt.user.client.ui.Panel} because a composite usually
     * wants to control which methods are publicly accessible without exposing
     * those methods that it would inherit from its panel superclass.
     * {@link com.google.gwt.examples.CompositeExample This} is an example of
     * how to create a composite.
     * 
     * <h2>From Scratch in Java code</h2>
     * It is also possible to create a widget from scratch, although it is
     * trickier since you have to write code at a lower level. Many of the basic
     * widgets are written this way, such as {@link Button} and {@link TextBox}.
     * Please refer to the implementations of these widgets to understand how to
     * create your own.
     * 
     * <h2>Using JavaScript</h2>
     * When implementing a custom widget that derives directly from the
     * {@link Widget} base class, you may also write some of the widget's
     * methods using JavaScript. This should generally be done only as a last
     * resort, as it becomes necessary to consider the cross-browser
     * implications of the native methods that you write, and also becomes more
     * difficult to debug. For an example of this pattern in practice, see the
     * {@link TextBox} widget and its underlying
     * {@link com.google.gwt.user.client.ui.impl.TextBoxImpl implementation}.
     * 
     * @title Creating Custom Widgets
     * @synopsis Create your own widgets completely in Java code.
     * @index widgets
     */
    public static class CreatingCustomWidgets {
    }

    /**
     * Panels in GWT are much like their counterparts in other user interface
     * libraries. The main difference lies in the fact that they use HTML
     * elements such as DIV and TABLE to layout their child widgets.
     * 
     * <h2>RootPanel</h2>
     * The first panel you're likely to encounter is the
     * {@link com.google.gwt.user.client.ui.RootPanel}. This panel is always at
     * the top of the containment hierarchy. The default RootPanel wraps the
     * HTML document's body, and is obtained by calling
     * {@link com.google.gwt.user.client.ui.RootPanel#get()}. If you need to
     * get a root panel wrapping another element in the HTML document, you can
     * do so using {@link RootPanel#get(String)}.
     * 
     * <h2>CellPanel</h2>
     * {@link com.google.gwt.user.client.ui.CellPanel} is the abstract base
     * class for {@link com.google.gwt.user.client.ui.DockPanel},
     * {@link com.google.gwt.user.client.ui.HorizontalPanel}, and
     * {@link com.google.gwt.user.client.ui.VerticalPanel}. What these panels
     * all have in common is that they position their child widgets within
     * logical "cells". Thus, a child widget can be aligned within the cell that
     * contains it, using
     * {@link com.google.gwt.user.client.ui.CellPanel#setCellHorizontalAlignment setCellHorizontalAlignment()}
     * and
     * {@link com.google.gwt.user.client.ui.CellPanel#setCellVerticalAlignment setCellVerticalAlignment()}.
     * CellPanels also allow you to set the size of the cells themselves
     * (relative to the panel as a whole) using
     * {@link com.google.gwt.user.client.ui.CellPanel#setCellWidth} and
     * {@link com.google.gwt.user.client.ui.CellPanel#setCellHeight}.
     * 
     * <h2>Other Panels</h2>
     * Other panels include {@link com.google.gwt.user.client.ui.DeckPanel},
     * {@link com.google.gwt.user.client.ui.TabPanel}, {@link FlowPanel},
     * {@link com.google.gwt.user.client.ui.HTMLPanel}, and
     * {@link com.google.gwt.user.client.ui.StackPanel}.
     * 
     * <h2>Sizes and Measures</h2>
     * It is possible to set the size of a widget explicitly using
     * {@link com.google.gwt.user.client.ui.UIObject#setWidth setWidth()},
     * {@link com.google.gwt.user.client.ui.UIObject#setHeight setHeight()},
     * and {@link com.google.gwt.user.client.ui.UIObject#setSize setSize()}.
     * The arguments to these methods are strings, rather than integers, because
     * they accept any valid CSS measurements, such as pixels (128px),
     * centimeters (3cm), and percentage (100%).
     * 
     * @title Understanding Layout
     * @synopsis Understanding how widgets are laid out within panels.
     * @index layout
     */
    public static class UnderstandingLayout {
    }

    /**
     * GWT widgets rely on cascading style sheets (CSS) for visual styling. Each
     * widget has an associated style name that binds it to a CSS rule. A
     * widget's style name is set using
     * {@link com.google.gwt.user.client.ui.UIObject#setStyleName(String) setStyleName()}.
     * For example, the {@link Button} has a default style of
     * <code>gwt-Button</code>. In order to give all buttons a larger font,
     * you could put the following rule in your application's CSS file:
     * 
     * <pre class='code'>.gwt-Button { font-size: 150%; }</pre>
     * 
     * <h2>Complex Styles</h2>
     * <p>
     * Some widgets have somewhat more complex styles associated with them.
     * {@link com.google.gwt.user.client.ui.MenuBar}, for example, has the
     * following styles:
     * </p>
     * 
     * <pre class='code'>
     *   .gwt-MenuBar { the menu bar itself }
     *   .gwt-MenuBar .gwt-MenuItem { menu items }
     *   .gwt-MenuBar .gwt-MenuItem-selected { selected menu items }</pre>
     * 
     * <p>
     * In this example, there are two styles rules that apply to menu items. The
     * first applies to all menu items (both selected and unselected), while the
     * second (with the -selected suffix) applies only to selected menu items. A
     * selected menu item's style name will be set to
     * <code>"gwt-MenuItem gwt-MenuItem-selected"</code>, specifying that
     * both style rules will be applied. The most common way of doing this is to
     * use
     * {@link com.google.gwt.user.client.ui.UIObject#setStyleName(String) setStyleName}
     * to set the base style name, then
     * {@link com.google.gwt.user.client.ui.UIObject#addStyleName(String) addStyleName()}
     * and
     * {@link com.google.gwt.user.client.ui.UIObject#removeStyleName(String) removeStyleName()}
     * to add and remove the second style name.
     * </p>
     * 
     * <h2>CSS Files</h2>
     * <p>
     * Typically, stylesheets are placed in a package that is part of your
     * module's {@link DeveloperGuide.Fundamentals.Modules public path}. Then
     * simply include a reference to the stylesheet in your
     * {@link DeveloperGuide.Fundamentals.HostPage host page}, such as
     * 
     * <pre>&lt;link rel="stylesheet" href="mystyles.css" type="text/css"&gt;</pre>
     * 
     * </p>
     * 
     * <h2>Documentation</h2>
     * <p>
     * It is standard practice to document the relevant CSS style names for each
     * widget class as part of its doc comment. For a simple example, see
     * {@link com.google.gwt.user.client.ui.Button}. For a more complex
     * example, see {@link com.google.gwt.user.client.ui.MenuBar}.
     * </p>
     * 
     * @title Style Sheets
     * @synopsis Widgets are most easily styled using cascading style sheets
     *           (CSS).
     * @index style
     * @index CSS
     */
    public static class StyleSheets {
    }

    /**
     * Typically, an application uses many small images for icons. An HTTP
     * request has to be sent to the server for each of these images, and in
     * some cases, the size of the image is smaller than the HTTP response
     * header that is sent back with the image data. These round trips to the
     * server for small pieces of data are wasteful. Even when the images have
     * been cached by the client, a 304 ("Not Modified") request is still sent
     * to check and see if the image has changed. Since images change
     * infrequently, these freshness checks are also wasteful.
     * 
     * <p>
     * Sending out requests and freshness checks for many images will slow down
     * your application. HTTP 1.1 requires browsers to limit the number of
     * outgoing HTTP connections to two per domain/port. A multitude of image
     * requests will tie up the browser's available connections, which blocks
     * the application's RPC requests. RPC requests are the real work that the
     * application needs to do.
     * </p>
     * 
     * <p>
     * To solve this problem, GWT introduces the concept of an <i>image bundle</i>.
     * An image bundle is a composition of many images into a single image,
     * along with an interface for accessing the individual images from within
     * the composite. Users can define an image bundle that contains the images
     * used by their application, and GWT will automatically create the
     * composite image and provide an implementation of the interface for
     * accessing each individual image. Instead of a round trip to the server
     * for each image, only one round trip to the server for the composite image
     * is needed.
     * </p>
     * 
     * <p>
     * Since the filename of the composite image is based on a hash of the
     * file's contents, the filename will change only if the composite image is
     * changed. This means that it is safe for clients to cache the composite
     * image permanently, which avoids the unnecessary freshness checks for
     * unchanged images. To make this work, the server configuration needs to
     * specify that composite images never expire.
     * </p>
     * 
     * <p>
     * In addition to speeding up startup, image bundles prevent the 'bouncy'
     * effect of image loading in browsers. While images are loading, browsers
     * put a standard placeholder for each image in the UI. The placeholder is a
     * standard size because the browser does not know what the size of an image
     * is until it has been fully downloaded from the server. The result is a
     * 'bouncy' effect, where images 'pop' into the UI once they are downloaded.
     * With image bundles, the size of each individual image within the bundle
     * is discovered when the bundle is created, so the size of the image can be
     * explicitly set whenever images from a bundle are used in an application.
     * </p>
     * 
     * @title Image Bundles
     * @childIntro Specifics
     * @synopsis Optimize the performance of your application by reducing the
     *           number of HTTP requests for images.
     * @see com.google.gwt.user.client.ImageBundle
     * @tip To make all image bundle files permanently cacheable, set up a rule
     *      in your web server to emit the <code>Expires</code> response
     *      header for any files ending with "<code>.cache.*</code>". Such a
     *      rule would automatically match generated image bundle filenames
     *      (e.g. <code>320ADF600D31858000C612E939F0AD1A.cache.png</code>).
     *      The <a
     *      href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">HTTP/1.1
     *      specification</a> recommends specifying date of approximately one
     *      year in the future for the <code>Expires</code> header to indicate
     *      that the resource is permanently cacheable.
     */
    public static class ImageBundles {

      /**
       * To define an image bundle, the user needs to extend the
       * {@link com.google.gwt.user.client.ImageBundle ImageBundle} interface.
       * The ImageBundle interface is a tag interface that can be extended to
       * define new image bundles.
       * 
       * <p>
       * The derived interface can have zero or more methods, where each method
       * <ul>
       * <li>takes no parameters,</li>
       * <li>has a return type of
       * {@link com.google.gwt.user.client.ui.AbstractImagePrototype}, and</li>
       * <li>may have an optional <code>gwt.resource</code> metadata tag
       * which specifies the name of the image file in the module's classpath
       * </li>
       * </ul>
       * </p>
       * 
       * <p>
       * Valid image file types are <code>png</code>, <code>gif</code>,
       * and <code>jpg</code>. If the image name contains '<code>/</code>'
       * characters, it is assumed to be the name of a resource on the
       * classpath, formatted as would be expected by
       * <code>ClassLoader.getResource(String)</code>. Otherwise, the image
       * must be located in the same package as the user-defined image bundle.
       * </p>
       * 
       * <p>
       * If the <code>gwt.resource</code> metadata tag is not specified, then
       * </p>
       * 
       * <ul>
       * <li>the image filename is assumed to match the method name,</li>
       * <li>the extension is assumed to be either <code>.png</code>,
       * <code>.gif</code>, or <code>.jpg</code>, and </li>
       * <li>the file is assumed to be in the same package as the derived
       * interface</li>
       * </ul>
       * 
       * <p>
       * In the event that there are multiple image files with different
       * extensions, the order of extension precedence is (1) <code>png</code>,
       * (2) <code>gif</code>, then (3) <code>jpg</code>.
       * </p>
       * 
       * <p>
       * An image bundle for icons in a word processor application could be
       * defined as follows:
       * 
       * <pre class="code">
       * public interface WordProcessorImageBundle extends ImageBundle {
       * 
       *   /**
       *    * Would match the file 'new_file_icon.png', 'new_file_icon.gif', or
       *    * 'new_file_icon.png' located in the same package as this type.
       *    *&#47;
       *   public AbstractimagePrototype new_file_icon();
       * 
       *   /**
       *    * Would match the file 'open_file_icon.gif' located in the same package
       *    * as this type.
       *    * @gwt.resource open_file_icon.gif
       *    *&#47;
       *   public AbstractImagePrototpe openFileIcon();
       * 
       *   /**
       *    * Would match the file 'savefile.gif' located in the package
       *    * 'com.mycompany.mygwtapp.icons', provided that this package
       *    * is part of the module's classpath.
       *    * @gwt.resource com/mycompany/mygwtapp/icons/savefile.gif
       *    *&#47;
       *   public AbstractImagePrototype saveFileIcon();
       * }
       * </pre>
       * 
       * </p>
       * 
       * <p>
       * Methods in an image bundle return <code>AbstractImagePrototype</code>
       * objects (rather than <code>Image</code> objects, as you might have
       * expected) because <code>AbstractImagePrototype</code> objects provide
       * additional lightweight representations of an image. For example, the
       * {@link com.google.gwt.user.client.ui.AbstractImagePrototype#getHTML()}
       * method provides an HTML fragment representing an image without having
       * to create an actual instance of the {@link Image} widget. In some
       * cases, it can be more efficient to manage images using these HTML
       * fragments.
       * </p>
       * 
       * <p>
       * Another use of <code>AbstractImagePrototype</code> is to use
       * {@link com.google.gwt.user.client.ui.AbstractImagePrototype#applyTo(Image)}
       * to transform an existing <code>Image</code> into one that matches the
       * prototype without having to instantiate another <code>Image</code>
       * object. This can be useful if your application has an image that needs
       * to be swapped depending on some user-initiated action. Of course, if an
       * <code>Image</code> is exactly what you need, the
       * {@link com.google.gwt.user.client.ui.AbstractImagePrototype#createImage()}
       * method can be used to generate new <code>Image</code> instances.
       * </p>
       * 
       * <p>
       * The following example shows how to use the image bundle that we just
       * defined in your application:
       * 
       * <pre class="code">
       * WordProcessorImageBundle wpImageBundle = (WordProcessorImageBundle) GWT
       *        .create(WordProcessorImageBundle.class);
       * HorizontalPanel tbPanel = new HorizontalPanel();
       * tbPanel.add(wpImageBundle.new_file_icon().createImage());
       * tbPanel.add(wpImageBundle.openFileIcon().createImage());
       * tbPanel.add(wpImageBundle.saveFileIcon().createImage());
       * </pre>
       * 
       * </p>
       * 
       * @title Creating and Using an Image Bundle
       * @synopsis Define an image bundle and use it in your application.
       * @see com.google.gwt.user.client.ImageBundle
       * @see com.google.gwt.user.client.ui.AbstractImagePrototype
       * @tip Image bundles are immutable, so you can keep a reference to a
       *      singleton instance of an image bundle instead of creating a new
       *      instance every time the image bundle is needed.
       */
      public static class DefiningAndUsingImageBundle {
      }

      /**
       * Sometimes applications need different images depending on the locale
       * that the user is in. When using image bundles, this means that we need
       * different image bundles for different locales. Although image bundles
       * and localization are orthognal concepts, they can work together by
       * having locale-specific factories create instances of image bundles.
       * 
       * <p>
       * The best way to explain this technique is with an example. Suppose that
       * we define the following <code>ImageBundle</code> for use by a mail
       * application:
       * 
       * <pre class="code">
       * public interface MailImageBundle extends ImageBundle {
       * 
       *  /**
       *   * The default 'Help' icon if no locale-specific image is specified.
       *   * Will match 'help_icon.png', 'help_icon.gif', or 'help_icon.jpg' in
       *   * the same package as this type.
       *   *&#47;
       *  public AbstractImagePrototype help_icon();
       * 
       *  /**
       *   * The default 'Compose New Message' icon if no locale-specific 
       *   * image is specified.
       *   * @gwt.resource compose_new_message_icon.gif
       *   *&#47;
       *  public AbstractImagePrototype composeNewMessageIcon();
       * }
       * </pre>
       * 
       * </p>
       * Suppose the application has to handle both English and French users. We
       * define English and French variations of each image in
       * <code>MailImageBundle</code> by creating locale-specific image
       * bundles that extend <code>MailImageBundle</code>:
       * 
       * <pre class="code">
       * public interface MailImageBundle_en extends MailImageBundle {
       * 
       *  /**
       *   * The English version of the 'Compose New Message' icon.
       *   * Since we are not overriding the help_icon() method, this bundle
       *   * uses the inherited method from MailImageBundle.
       *   * @gwt.resource compose_new_message_icon_en.gif
       *   *&#47;
       *  public AbstractImagePrototype composeNewMessageIcon();
       * }
       * 
       * public interface MailimageBundle_fr extends MailImageBundle {
       * 
       *  /**
       *   * The French version of the 'Help' icon.
       *   * @gwt.resource help_icon_fr.gif
       *   *&#47;
       *  public AbstractImagePrototype help_icon();
       * 
       *  /**
       *   * The French version of the 'Compose New Message' icon.
       *   * @gwt.resource compose_new_message_icon_fr.gif
       *   *&#47;
       *  public AbstractImagePrototype composeNewMessageIcon();
       * }
       * </pre>
       * 
       * The final step is to create a mechanism for choosing the correct image
       * bundle based on the user's locale. By extending
       * {@link com.google.gwt.i18n.client.Localizable Localizable}, we can
       * create a locale-sensitive factory that will return new instances of
       * <code>MailImageBundle</code> that match the factory's locale:
       * 
       * <pre class="code">
       * public interface MailImageBundleFactory extends Localizable {
       * 
       *    public MailImageBundle createImageBundle();
       * }
       * 
       * public class MailImageBundleFactory_en extends MailImageBundleFactory {
       * 
       *    public MailImageBundle createImageBundle() {
       *        return (MailImageBundle) GWT.create(MailImageBundle_en.class);
       *    }
       * }
       * 
       * public class MailImageBundleFactory_fr extends MailImageBundleFactory {
       * 
       *    public MailImageBundle createImageBundle() {
       *        return (MailImageBundle) GWT.create(MailImageBundle_fr.class);
       *    }
       * }
       * </pre>
       * 
       * The application code that utilizes a locale-sensitive image bundle
       * would look something like this:
       * 
       * <pre class="code">
       * // Create a locale-sensitive MailImageBundleFactory
       * MailImageBundleFactory mailImageBundleFactory = (MailImageBundleFactory) GWT
       *        .create(MailImageBundleFactory.class);
       * 
       * // This will return a locale-sensitive MailImageBundle, since we are using
       * // a locale-sensitive factory to create it.
       * MailImageBundle mailImageBundle = mailImageBundleFactory.createImageBundle();
       * 
       * // Get the image prototype for the icon that we are interested in.
       * AbstractImagePrototype helpIconProto = mailImageBundle.help_icon();
       * 
       * // Create an Image object from the prototype and add it to a panel.
       * panel.add(helpIconProto.createImage());
       * </pre>
       * 
       * @title Image Bundles and Localization
       * @synopsis Create locale-sensitive image bundles by using GWT's
       *           localization capabilities.
       * @see com.google.gwt.user.client.ImageBundle
       * @see com.google.gwt.i18n.client.Localizable
       * 
       */
      public static class InteractionWithLocalization {
      }
    }
  }

  /**
   * A fundamental difference between GWT applications and traditional HTML web
   * applications is that GWT applications do not need to fetch new HTML pages
   * while they execute. Because GWT-enhanced pages actually run more like
   * applications within the browser, there is no need to request new HTML from
   * the server to make user interface updates. However, like all client/server
   * applications, GWT applications usually <i>do</i> need to fetch data from
   * the server as they execute. The mechanism for interacting with a server
   * across a network is called making a remote procedure call (RPC), also
   * sometimes referred to as a <i>server call</i>. GWT RPC makes it easy for
   * the client and server to pass Java objects back and forth over HTTP.
   * 
   * <p>
   * When used properly, RPCs give you the opportunity to move all of your UI
   * logic to the client, resulting in greatly improved performance, reduced
   * bandwidth, reduced web server load, and a pleasantly fluid user experience.
   * </p>
   * 
   * <p>
   * The {@link DeveloperGuide.Fundamentals.ServerSide server-side} code that
   * gets invoked from the client is often referred to as a <i>service</i>, so
   * the act of making a remote procedure call is sometimes referred to as
   * invoking a service. To be clear, though, the term <i>service</i> in this
   * context isn't the same as the more general "web service" concept. In
   * particular, GWT services are not related to the Simple Object Access
   * Protocol (SOAP).
   * </p>
   * 
   * @title Remote Procedure Calls
   * @childIntro Specifics
   * @index RPC
   * @index remote procedure calls
   * @index servlet
   * @index services
   * @synopsis An easy-to-use RPC mechanism for passing Java objects to and from
   *           a server over standard HTTP.
   */
  public static class RemoteProcedureCalls {

    /**
     * This section outlines the moving parts required to invoke a service. Each
     * service has a small family of helper interfaces and classes. Some of
     * these classes, such as the service proxy, are automatically generated
     * behind the scenes and you generally will never realize they exist. The
     * pattern for helper classes is identical for every service that you
     * implement, so it is a good idea to spend a few moments to familiarize
     * yourself with the terminology and purpose of each layer in server call
     * processing. If you are familiar with traditional remote procedure call
     * (RPC) mechanisms, you will recognize most of this terminology already.
     * 
     * <p align="center">
     * <img src="AnatomyOfServices.gif"/>
     * </p>
     * 
     * @title RPC Plumbing Diagram
     * @synopsis Diagram of the RPC plumbing.
     */
    public static class PlumbingDiagram {
    }

    /**
     * To develop a new service interface, begin by creating a
     * {@link DeveloperGuide.Fundamentals.ClientSide client-side} Java interface
     * that extends the {@link com.google.gwt.user.client.rpc.RemoteService} tag
     * interface.
     * 
     * {@example MyService}
     * 
     * This synchronous interface is the definitive version of your service's
     * specification. Any implementation of this service on the
     * {@link DeveloperGuide.Fundamentals.ServerSide server-side} must extend
     * {@link RemoteServiceServlet} and implement this service interface.
     * 
     * {@example MyServiceImpl}
     * 
     * <h2>Asynchronous Interfaces</h2>
     * 
     * Before you can actually attempt to make a remote call from the client,
     * you must create another interface, an asynchronous one, based on your
     * original service interface. Continuing with the example above...
     * 
     * {@example MyServiceAsync}
     * 
     * <p>
     * The nature of asynchronous method calls requires the caller to pass in a
     * callback object that can be notified when an asynchronous call completes,
     * since by definition the caller cannot be blocked until the call
     * completes. For the same reason, asynchronous methods do not have return
     * types; they must always return void. After an asynchronous call is made,
     * all communication back to the caller is via the passed-in callback
     * object.
     * </p>
     * 
     * The relationship between a service interface and its asynchronous
     * counterpart is straightforward:
     * <ul>
     * <li>If a service interface is called
     * <code>com.example.cal.client.SpellingService</code>, then the
     * asynchronous interface must be called
     * <code>com.example.cal.client.SpellingServiceAsync</code>. The
     * asynchronous interface must be in the same package and have the same
     * name, but with the suffix <code>Async</code>.</li>
     * 
     * <li>For each method in your service interface,
     * 
     * {@example #methodName(ParamType1, ParamType2)}
     * 
     * an asynchronous sibling method should be defined that looks like this:
     * 
     * {@example #methodName(ParamType1, ParamType2, AsyncCallback)}
     * 
     * </li>
     * </ul>
     * 
     * See {@link AsyncCallback} for additional details on how to implement an
     * asynchronous callback.
     * 
     * @title Creating Services
     * @synopsis How to build a service interface from scratch.
     */
    public static interface CreatingServices {

      /**
       * @skip
       */
      public class ReturnType {
      }

      /**
       * @skip
       */
      public class ParamType1 {
      }

      /**
       * @skip
       */
      public class ParamType2 {
      }

      public ReturnType methodName(ParamType1 param1, ParamType2 param2);

      public void methodName(ParamType1 param1, ParamType2 param2,
          AsyncCallback callback);

      /**
       * @skip
       */
      public interface MyService extends RemoteService {
        public String myMethod(String s);
      }

      /**
       * @skip
       */
      public class MyServiceImpl extends RemoteServiceServlet implements
          MyService {

        public String myMethod(String s) {
          // Do something interesting with 's' here on the server.
          return s;
        }

      }

      /**
       * @skip
       */
      interface MyServiceAsync {
        public void myMethod(String s, AsyncCallback callback);
      }
    }

    /**
     * Every service ultimately needs to perform some processing to order to
     * respond to client requests. Such
     * {@link DeveloperGuide.Fundamentals.ServerSide server-side} processing
     * occurs in the <i>service implementation</i>, which is based on the
     * well-known <a href="http://java.sun.com/products/servlet/">servlet</a>
     * architecture.
     * 
     * <p>
     * A service implementation must extend {@link RemoteServiceServlet} and
     * must implement the associated service interface. Note that the service
     * implementation does <i>not</i> implement the asynchronous version of the
     * service interface.
     * </p>
     * 
     * <p>
     * Every service implementation is ultimately a servlet, but rather than
     * extending <a
     * href="http://java.sun.com/j2ee/sdk_1.3/techdocs/api/javax/servlet/http/HttpServlet.html"><code>HttpServlet</code></a>,
     * it extends {@link RemoteServiceServlet} instead.
     * <code>RemoteServiceServlet</code> automatically handles serialization
     * and invoking the intended method in your service implementation.
     * </p>
     * 
     * <h2>Testing Services During Development</h2>
     * To automatically load your service implementation, use the
     * <code>&lt;servlet&gt;</code> tag within your
     * {@link DeveloperGuide.Fundamentals.Modules.ModuleXml module XML}. The
     * GWT development shell includes an embedded version of Tomcat which acts
     * as a development-time servlet container for testing.
     * 
     * <h2>Deploying Services Into Production</h2>
     * In production, you can use any servlet container that is appropriate for
     * your application. You need only to ensure that the client code is
     * configured to invoke the service using the URL to which your servlet is
     * mapped by the <code>web.xml</code> configuration. See
     * {@link com.google.gwt.user.client.rpc.ServiceDefTarget} for more
     * information.
     * 
     * @title Implementing Services
     * @synopsis Implement your service interface as a servlet.
     */
    public static class ImplementingServices {
    }

    /**
     * The process of making an RPC from the client always involves the exact
     * same steps.
     * <ol>
     * <li>Instantiate the service interface using
     * <code>{@link GWT#create(Class) GWT.create()}</code>.</li>
     * <li>Specify a service entry point URL for the service proxy using
     * {@link com.google.gwt.user.client.rpc.ServiceDefTarget}.</li>
     * <li>Create an asynchronous callback object to be notified when the RPC
     * has completed.</li>
     * <li>Make the call.</li>
     * </ol>
     * 
     * <h2>Example</h2>
     * Suppose you want to call a method on a service interface defined as
     * follows:
     * 
     * {@example RemoteProcedureCalls.MakingACall.MyEmailService}
     * 
     * Its corresponding asynchronous interface will look like this:
     * 
     * {@example RemoteProcedureCalls.MakingACall.MyEmailServiceAsync}
     * 
     * The client-side call will look like this:
     * 
     * {@example #menuCommandEmptyInbox()}
     * 
     * It is safe to cache the instantiated service proxy to avoid creating it
     * for subsequent calls.
     * 
     * @title Actually Making a Call
     * @see RemoteService
     * @see com.google.gwt.user.client.rpc.ServiceDefTarget
     * @see GWT#create(Class)
     * @synopsis How to actually make a remote procedure call from the client
     */
    public static class MakingACall {

      /**
       * @skip
       */
      public interface MyEmailService extends RemoteService {
        void emptyMyInbox(String username, String password);
      }

      /**
       * @skip
       */
      public interface MyEmailServiceAsync {
        void emptyMyInbox(String username, String password,
            AsyncCallback callback);
      }

      private String fUsername;
      private String fPassword;

      /**
       * @skip
       */
      public void menuCommandEmptyInbox() {
        // (1) Create the client proxy. Note that although you are creating the
        // service interface proper, you cast the result to the asynchronous
        // version of
        // the interface. The cast is always safe because the generated proxy
        // implements the asynchronous interface automatically.
        //
        MyEmailServiceAsync emailService = (MyEmailServiceAsync) GWT.create(MyEmailService.class);

        // (2) Specify the URL at which our service implementation is running.
        // Note that the target URL must reside on the same domain and port from
        // which the host page was served.
        //
        ServiceDefTarget endpoint = (ServiceDefTarget) emailService;
        String moduleRelativeURL = GWT.getModuleBaseURL() + "email";
        endpoint.setServiceEntryPoint(moduleRelativeURL);

        // (3) Create an asynchronous callback to handle the result.
        //
        AsyncCallback callback = new AsyncCallback() {
          public void onSuccess(Object result) {
            // do some UI stuff to show success
          }

          public void onFailure(Throwable caught) {
            // do some UI stuff to show failure
          }
        };

        // (4) Make the call. Control flow will continue immediately and later
        // 'callback' will be invoked when the RPC completes.
        //
        emailService.emptyMyInbox(fUsername, fPassword, callback);
      }
    }

    /**
     * Method parameters and return types must be <i>serializable</i>, which
     * means they must conform to certain restrictions. GWT tries really hard to
     * make serialization as painless as possible, so while the rules regarding
     * serialization are subtle, in practice the behavior becomes intuitive very
     * quickly.
     * 
     * <p>
     * A type is serializable and can be used in a service interface if it
     * <ul>
     * <li>is primitive, such as <code>char</code>, <code>byte</code>,
     * <code>short</code>, <code>int</code>, <code>long</code>,
     * <code>boolean</code>, <code>float</code>, or <code>double</code>;</li>
     * <li>is <code>String</code>, <code>Date</code>, or a primitive
     * wrapper such as <code>Character</code>, <code>Byte</code>,
     * <code>Short</code>, <code>Integer</code>, <code>Long</code>,
     * <code>Boolean</code>, <code>Float</code>, or <code>Double</code>;</li>
     * <li>is an array of serializable types (including other serializable
     * arrays);</li>
     * <li>is a serializable user-defined class; or</li>
     * <li>has at least one serializable subclass</li>
     * </ul>
     * </p>
     * 
     * <h2>Serializable User-defined Classes</h2>
     * A user-defined class is serializable if
     * <ol>
     * <li>it is assignable to
     * {@link com.google.gwt.user.client.rpc.IsSerializable}, either because it
     * directly implements the interface or because it derives from a superclass
     * that does</li>
     * <li>all non-<code>transient</code> fields are themselves
     * serializable, and</li>
     * <li>it explicitly defines a default constructor, which is a constructor
     * that is declared to be public and takes no arguments</li>
     * </ol>
     * 
     * The <code>transient</code> keyword is honored, so values in transient
     * fields are not exchanged during RPCs. Fields that are declared
     * <code>final</code> are also not exchanged during RPCs, so they should
     * generally be marked <code>transient</code> as well.
     * 
     * <h2>Polymorphism</h2>
     * GWT RPC supports polymorphic parameters and return types. To make the
     * best use of polymorphism, however, you should still try to be as specific
     * as your design allows when defining service interfaces. Increased
     * specificity allows the
     * {@link DeveloperGuide.Fundamentals.JavaToJavaScriptCompiler compiler} to
     * do a better job of removing unnecessary code when it optimizes your
     * application for size reduction.
     * 
     * <h2>Type Arguments</h2>
     * Collection classes such as <code>java.util.Set</code> and
     * <code>java.util.List</code> are tricky because they operate in terms of
     * <code>Object</code> instances. To make collections serializable, you
     * should specify the particular type of objects they are expected to
     * contain. This requires you to use the special Javadoc annotation
     * <code>&#64;gwt.typeArgs</code>. Defining an <i>item type</i> for a
     * collection means that you will ensure that the collection only ever
     * contains objects of that item type or a subclass thereof. This hint is
     * necessary so that the GWT proxy generator can create efficient code.
     * Adding an object to a collection that violates its asserted item type
     * will lead to undefined behavior.
     * 
     * <p>
     * To annotate fields of collection type in a serializable user-defined
     * class:
     * 
     * {@example RemoteProcedureCalls.SerializableTypes.MyClass}
     * 
     * Note that there is no need to specify the name of the field in the
     * <code>&#64;gwt.typeArgs</code> declaration since it can be inferred.
     * </p>
     * 
     * <p>
     * Similarly, to annotate parameters and return types:
     * 
     * {@example RemoteProcedureCalls.SerializableTypes.MyService}
     * 
     * Note that parameter annotations must include the name of the parameter
     * they are annotating in addition to the collection item type, while return
     * type annotations do not.
     * </p>
     * 
     * @title Serializable Types
     * @synopsis Using GWT's automatic serialization well.
     * @tip Although the terminology is very similar, GWT's concept of
     *      "serializable" is different than serialization based on the standard
     *      Java interface
     *      <code><a href="http://java.sun.com/j2se/1.4.2/docs/api/java/io/Serializable.html">Serializable</a></code>.
     *      All references to serialization are referring to the GWT concept as
     *      defined below.
     */
    public static class SerializableTypes {

      /**
       * @skip
       */
      public class MyClass implements IsSerializable {
        /**
         * This field is a Set that must always contain Strings.
         * 
         * @gwt.typeArgs <java.lang.String>
         */
        public Set setOfStrings;

        /**
         * This field is a Map that must always contain Strings as its keys and
         * values.
         * 
         * @gwt.typeArgs <java.lang.String,java.lang.String>
         */
        public Map mapOfStringToString;

        /**
         * Default Constructor. The Default Constructor's explicit declaration
         * is required for a serializable class.
         */
        public MyClass() {
        }
      }

      /**
       * @skip
       */
      public interface MyService extends RemoteService {
        /**
         * The first annotation indicates that the parameter named 'c' is a List
         * that will only contain Integer objects. The second annotation
         * indicates that the returned List will only contain String objects
         * (notice there is no need for a name, since it is a return value).
         * 
         * @gwt.typeArgs c <java.lang.Integer>
         * @gwt.typeArgs <java.lang.String>
         */
        List reverseListAndConvertToStrings(List c);
      }

    }

    /**
     * Making RPCs opens up the possibility of a variety of errors. Networks
     * fail, servers crash, and problems occur while processing a server call.
     * GWT lets you handle these conditions in terms of Java exceptions.
     * RPC-related exceptions fall into two categories.
     * 
     * <h2>Checked Exceptions</h2>
     * {@link DeveloperGuide.RemoteProcedureCalls.CreatingServices Service interface}
     * methods support <code>throws</code> declarations to indicate which
     * exceptions may be thrown back to the client from a service
     * implementation. Callers should implement
     * {@link AsyncCallback#onFailure(Throwable)} to check for any exceptions
     * specified in the service interface.
     * 
     * <h2>Unexpected Exceptions</h2>
     * An RPC may not reach the
     * {@link DeveloperGuide.RemoteProcedureCalls.ImplementingServices service implementation}
     * at all. This can happen for many reasons: the network may be
     * disconnected, a DNS server might not be available, the HTTP server might
     * not be listening, and so on. In this case, an
     * {@link com.google.gwt.user.client.rpc.InvocationException} is passed to
     * your implementation of {@link AsyncCallback#onFailure(Throwable)}. The
     * class is called <code><i>Invocation</i>Exception</code> because the
     * problem was with the invocation attempt itself rather than with the
     * service implementation itself.
     * 
     * <p>
     * An RPC can also fail with an invocation exception if the call does reach
     * the server, but an undeclared exception occurs during normal processing
     * of the call. There are many reasons such a situation could arise: a
     * necessary server resource, such as a database, might be unavailable, a
     * <code>NullPointerException</code> could be thrown due to a bug in the
     * service implementation, and so on. In these cases, a
     * {@link com.google.gwt.user.client.rpc.InvocationException} is thrown in
     * application code.
     * </p>
     * 
     * @title Handling Exceptions
     * @synopsis Handle exceptions due to failed calls or thrown from the
     *           server.
     */
    public static class HandlingExceptions {
    }

    /**
     * Asynchronous RPC isn't the simplest thing in the world, but it does allow
     * you to achieve true parallelism in your application, even without
     * multi-threading.
     * 
     * <p>
     * For example, suppose your application displays a large
     * {@link com.google.gwt.user.client.ui.HTMLTable table} containing many
     * widgets. Constructing and laying out all those widgets can be time
     * consuming. At the same time, you need to fetch data from the server to
     * display inside the table. This is a perfect reason to use asynchronous
     * calls. Initiate an asynchronous call to request the data immediately
     * before you begin constructing your table and its widgets. While the
     * server is fetching the required data, the browser is executing your user
     * interface code. When the client finally receives the data from the
     * server, the table has been constructed and laid out, and the data is
     * ready to be displayed.
     * </p>
     * 
     * <p>
     * To give you an idea of how effective this technique can be, suppose that
     * building the table takes 1 second and fetching the data takes 1 second.
     * If you make the server call synchronously, the whole process will require
     * at least 2 seconds. But if you fetch the data asynchronously, the whole
     * process still takes just 1 second, even though you are doing 2 seconds'
     * worth of work.
     * </p>
     * 
     * <p>
     * The hardest thing to get used to about asynchronous calls is that the
     * calls are non-blocking. However, Java inner classes go a long way toward
     * making this manageable.
     * </p>
     * 
     * @title Getting Used to Asynchronous Calls
     * @synopsis Asynchronous calls are tricky at first, but ultimately your
     *           users will thank you.
     * @tip The {@link AsyncCallback} interface is the key interface you'll
     *      extend to handle RPC responses.
     */
    public static class GettingUsedToAsyncCalls {
    }

    /**
     * There are various ways to approach services within your application
     * architecture. Understand first of all that GWT services are not intended
     * to replace J2EE servers, nor are they intended to provide a public web
     * services (e.g. SOAP) layer for your application. GWT RPCs, fundamentally,
     * are simply a method of "getting from the client to the server." In other
     * words, you use RPCs to accomplish tasks that are part of your application
     * but that cannot be done on the client computer.
     * 
     * <p>
     * Architecturally, you can make use of RPC two alternative ways. The
     * difference is a matter of taste and of the architectural needs of your
     * application.
     * </p>
     * 
     * <p>
     * The first and most straightforward way to think of service definitions is
     * to treat them as your application's entire back end. From this
     * perspective,
     * {@link DeveloperGuide.Fundamentals.ClientSide client-side code} is your
     * "front end" and all service code that runs on the server is "back end."
     * If you take this approach, your service implementations would tend to be
     * more general-purpose APIs that are not tightly coupled to one specific
     * application. Your service definitions would likely directly access
     * databases through JDBC or Hibernate or even files in the server's file
     * system. For many applications, this view is appropriate, and it can be
     * very efficient because it reduces the number of tiers.
     * </p>
     * 
     * <p>
     * In more complex, multi-tiered architectures, your GWT service definitions
     * could simply be lightweight gateways that call through to back-end server
     * environments such as J2EE servers. From this perspective, your services
     * can be viewed of as the "server half" of your application's user
     * interface. Instead of being general-purpose, services are created for the
     * specific needs of your user interface. Your services become the "front
     * end" to the "back end" classes that are written by stitching together
     * calls to a more general-purpose back-end layer of services, implemented,
     * for example, as a cluster of J2EE servers. This kind of architecture is
     * appropriate if you require your back-end services to run on a physically
     * separate computer from your HTTP server.
     * </p>
     * 
     * @title Architectural Perspectives
     * @synopsis Contrasting a couple of approaches to implementing services.
     */
    public static class ArchitecturalPerspectives {
    }
  }

  /**
   * 
   * @skip
   * @title Managing Application History
   * @synopsis Manage history state in your AJAX applications programmatically.
   */
  public static class History {
  }

  /**
   * GWT includes a special {@link GWTTestCase} base class that provides <a
   * href="http://www.junit.org">JUnit</a> integration. Running a compiled
   * {@link GWTTestCase} subclass under JUnit launches an invisible GWT browser.
   * 
   * <p>
   * By default, tests run in
   * {@link DeveloperGuide.Fundamentals.HostedMode hosted mode} as normal Java
   * bytecode in a JVM. Overriding this default behavior requires passing
   * arguments to the GWT shell. Arguments cannot be passed directly through the
   * command line, because normal command-line arguments go directly to the
   * JUnit runner. Instead, define the system property <code>gwt.args</code>
   * to pass arguments to GWT. For example, to run in
   * {@link DeveloperGuide.Fundamentals.WebMode web mode}, declare
   * <code>-Dgwt.args="-web"</code> as a JVM argument when invoking JUnit. To
   * get a full list of supported options, declare
   * <code>-Dgwt.args="-help"</code> (instead of running the test, help is
   * printed to the console).
   * </p>
   * 
   * <h2>Creating a Test Case</h2>
   * <p>
   * GWT includes a handy
   * {@link DeveloperGuide.Fundamentals.CommandLineTools.junitCreator} tool that
   * will generate a starter test case for you, plus scripts for testing in both
   * hosted mode and web mode. But here are the steps if you want to set it up
   * by hand:
   * </p>
   * <ol>
   * <li>Define a class that extends {@link GWTTestCase}.</li>
   * <li>Create a {@link DeveloperGuide.Fundamentals.Modules module} that
   * causes the source for your test case to be included. If you are adding a
   * test case to an existing GWT app, you can usually just use the existing
   * module.</li>
   * <li>Implement the method {@link GWTTestCase#getModuleName()} to return the
   * fully-qualified name of the module. </li>
   * <li>Compile your test case class to bytecode (using <a
   * href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javac.html">javac</a>
   * or a Java IDE).</li>
   * <li>When running the test case, make sure your classpath includes:
   * 
   * <ul>
   * <li>your project's <code>src</code> directory</li>
   * <li>your project's <code>bin</code> directory</li>
   * <li><code>gwt-user.jar</code></li>
   * <li><code>gwt-dev-windows.jar</code> (or <code>gwt-dev-linux.jar</code>)</li>
   * <li><code>junit.jar</code></li>
   * </ul>
   * 
   * </li>
   * </ol>
   * 
   * <p>
   * <h2>Example</h2>
   * Write the <code>com.example.foo.client.FooTest</code> test case.
   * 
   * {@example com.example.foo.client.FooTest}
   * 
   * Create the <code>com.example.foo.Foo</code> module.
   * 
   * {@gwt.include com/example/foo/Foo.gwt.xml}
   * </p>
   * 
   * @title JUnit Integration
   * @childIntro Advanced Topics
   * @synopsis Integration with JUnit lets you test your AJAX code almost as
   *           easily as any other Java code.
   * @tip You don't need to create a separate module for every test case. In the
   *      example above, any test cases in <code>com.example.foo.client</code>
   *      (or any subpackage) can share the <code>com.example.foo.Foo</code>
   *      module.
   * @index JUnit
   */
  public static class JUnitIntegration {

    /**
     * GWT's <a href="http://www.junit.org">JUnit</a> integration provides
     * special support for testing functionality that cannot execute in
     * straight-line code. For example, you might want to make an
     * {@link DeveloperGuide.RemoteProcedureCalls RPC} call to a server and then
     * validate the response. However, in a normal JUnit test run, the test
     * stops as soon as the test method returns control to the caller, and GWT
     * does not support multiple threads or blocking. To support this use case,
     * {@link GWTTestCase} has extended the <code>TestCase</code> API.
     * 
     * <p>
     * The two key methods are {@link GWTTestCase#delayTestFinish(int)} and
     * {@link GWTTestCase#finishTest()}. Calling <code>delayTestFinish()</code>
     * during a test method's execution puts that test in asynchronous mode,
     * which means the test will not finish when the test method returns control
     * to the caller. Instead, a <i>delay period</i> begins, which lasts the
     * amount of time specified in the call to <code>delayTestFinish()</code>.
     * During the delay period, the test system will wait for one of three
     * things to happen:
     * 
     * <ol>
     * <li> If <code>finishTest()</code> is called before the delay period
     * expires, the test will succeed.</li>
     * <li> If any exception escapes from an event handler during the delay
     * period, the test will error with the thrown exception.</li>
     * <li> If the delay period expires and neither of the above has happened,
     * the test will error with a
     * {@link com.google.gwt.junit.client.TimeoutException}. </li>
     * </ol>
     * </p>
     * 
     * <p>
     * The normal use pattern is to setup an event in the test method and call
     * <code>delayTestFinish()</code> with a timeout significantly longer than
     * the event is expected to take. The event handler validates the event and
     * then calls <code>finishTest()</code>.
     * </p>
     * 
     * <p>
     * <h3>Example</h3>
     * {@example com.google.gwt.examples.AsyncJUnitExample#testTimer()}
     * </p>
     * 
     * @title Asynchronous Testing
     * @synopsis How to test event-driven features such as server calls or
     *           timers.
     * @tip The recommended pattern is to test one asynchronous event per test
     *      method. If you need to test multiple events in the same method, here
     *      are a couple of techniques:
     * 
     * <ul>
     * <li> "Chain" the events together. Trigger the first event during the test
     * method's execution; when that event fires, call
     * <code>delayTestFinish()</code> again with a new timeout and trigger the
     * next event. When the last event fires, call <code>finishTest()</code>
     * as normal. </li>
     * <li> Set a counter containing the number of events to wait for. As each
     * event comes in, decrement the counter. Call <code>finishTest()</code>
     * when the counter reaches <code>0.</code> </li>
     * </ul>
     * 
     * @see GWTTestCase#delayTestFinish(int)
     * @see GWTTestCase#finishTest()
     */
    public static class JUnitAsync {
    }

    /**
     * GWT's <a href="http://www.junit.org">JUnit</a> integration provides
     * special support for creating and reporting on benchmarks. Specifically,
     * GWT has introduced a new {@link com.google.gwt.junit.client.Benchmark}
     * class which provides built-in facilities for common benchmarking needs.
     * 
     * To take advantage of benchmarking support, take the following steps:
     * <ol>
     * <li>Review the documentation on
     * {@link com.google.gwt.junit.client.Benchmark}. Take a look at the
     * example benchmark code.</li>
     * <li>Create your own benchmark by subclassing
     * {@link com.google.gwt.junit.client.Benchmark}. Execute your benchmark
     * like you would any normal JUnit test. By default, the test results are
     * written to a report XML file in your working directory.</li>
     * <li>Run <code>benchmarkViewer</code> to browse visualizations
     * (graphs/charts) of your report data. The <code>benchmarkViewer</code>
     * is a GWT tool in the root of your GWT installation directory that
     * displays benchmark reports.</li>
     * </ol>
     * 
     * @title Benchmarking
     * @synopsis How to use GWT's JUnit support to create and report on
     *           benchmarks to help you optimize your code.
     */
    public static class JUnitBenchmarking {
    }
  }

  /**
   * GWT includes a flexible set of tools to help you internationalize your
   * applications and libraries. GWT internationalization support provides a
   * variety of techniques to internationalize strings, typed values, and
   * classes.
   * 
   * <h2>Getting Started</h2>
   * Since GWT supports a variety of ways of internationalizing your code, begin
   * by researching which approach best matches your development requirements.
   * 
   * <p>
   * <b>Are you writing code from scratch?</b> <br/>If so, you'll probably want
   * to read up on GWT's
   * {@link com.google.gwt.doc.DeveloperGuide.Internationalization.StaticStringInternationalization static string internationalization}
   * techniques.
   * </p>
   * 
   * <p>
   * <b>Do you want to internationalize mostly settings or end-user messages?</b>
   * <br/> If you have mostly settings (the kind of thing for which you'd
   * normally use simple properties files), consider
   * {@link com.google.gwt.i18n.client.Constants}. If you have a lot a of
   * end-user messages, then {@link com.google.gwt.i18n.client.Messages} is
   * probably what you want.
   * </p>
   * 
   * <p>
   * <b>Do you have existing localized properties files you'd like to reuse?</b>
   * <br/> The
   * {@link com.google.gwt.doc.DeveloperGuide.Fundamentals.CommandLineTools.i18nCreator i18nCreator tool}
   * can automatically generate interfaces that extend either
   * {@link com.google.gwt.i18n.client.Constants} or
   * {@link com.google.gwt.i18n.client.Messages}.
   * </p>
   * 
   * <p>
   * <b>Are you adding GWT functionality to an existing web application that
   * already has a localization process defined?</b> <br/>
   * {@link com.google.gwt.i18n.client.Dictionary} will help you interoperate
   * with existing pages without requiring you to use
   * {@link com.google.gwt.doc.DeveloperGuide.Internationalization.SpecifyingLocale GWT's concept of locale}.
   * </p>
   * 
   * <p>
   * <b>Do you really just want a simple way to get properties files down to the
   * client regardless of localization?</b> <br/> You can do that, too. Try
   * using {@link com.google.gwt.i18n.client.Constants} without
   * {@link com.google.gwt.doc.DeveloperGuide.Internationalization.SpecifyingLocale specifying a locale}.
   * </p>
   * 
   * <h2>Internationalization Techniques</h2>
   * GWT offers multiple internationalization techniques to afford maximum
   * flexibility to GWT developers and to make it possible to design for
   * efficiency, maintainability, flexibility, and interoperability in whichever
   * combinations are most useful.
   * 
   * <p>
   * {@link com.google.gwt.doc.DeveloperGuide.Internationalization.StaticStringInternationalization Static string internationalization}
   * refers to a family of efficient and type-safe techniques that rely on
   * strongly-typed Java interfaces,
   * {@link com.google.gwt.doc.DeveloperGuide.Internationalization.PropertiesFiles properties files},
   * and code generation to provide locale-aware messages and configuration
   * settings. These techniques depend on the interfaces
   * {@link com.google.gwt.i18n.client.Constants} and
   * {@link com.google.gwt.i18n.client.Messages}.
   * </p>
   * 
   * <p>
   * At the other end of the spectrum,
   * {@link com.google.gwt.doc.DeveloperGuide.Internationalization.DynamicStringInternationalization dynamic string internationalization}
   * is a simplistic and flexible technique for looking up localized values
   * defined in a module's
   * {@link com.google.gwt.doc.DeveloperGuide.Fundamentals.HostPage host page}
   * without needing to recompile your application. This technique is supported
   * by the class {@link com.google.gwt.i18n.client.Dictionary}.
   * </p>
   * 
   * <p>
   * Using an approach similar to static string internationalization, GWT also
   * supports internationalizing sets of algorithms using locale-sensitive type
   * substitution. This is an advanced technique that you probably will not need
   * to use directly, although it is useful for implementing complex
   * internationalized libraries. For details on this technique, see
   * {@link com.google.gwt.i18n.client.Localizable}.
   * </p>
   * 
   * <h2>The I18N Module</h2>
   * The core types related to internationalization reside in the
   * <code>com.google.gwt.i18n</code> package:
   * 
   * <ul>
   * <li>{@link com.google.gwt.i18n.client.Constants} <div>Useful for
   * localizing typed constant values</div></li>
   * <li>{@link com.google.gwt.i18n.client.Messages} <div>Useful for localizing
   * messages requiring arguments</div></li>
   * <li>{@link com.google.gwt.i18n.client.ConstantsWithLookup} <div>Like
   * {@link com.google.gwt.i18n.client.Constants} but with extra lookup
   * flexibility for highly data-driven applications</div></li>
   * <li>{@link com.google.gwt.i18n.client.Dictionary} <div>Useful when adding
   * a GWT module to existing localized web pages</div> </li>
   * <li>{@link com.google.gwt.i18n.client.Localizable} <div>Useful for
   * localizing algorithms encapsulated in a class</div> </li>
   * </ul>
   * 
   * <p>
   * The GWT internationalization types are included in the module
   * <code>com.google.gwt.i18n.I18N</code>. To use any of these types, your
   * module must inherit from it:
   * 
   * {@gwt.include com/google/gwt/examples/i18n/InheritsExample.gwt.xml}
   * </p>
   * 
   * @title Internationalization
   * @synopsis Easily support multiple locales with a single code base.
   * @childIntro Specifics
   */
  public static class Internationalization {

    /**
     * Static string localization relies on code generation from
     * {@link DeveloperGuide.Internationalization.PropertiesFiles properties files}.
     * GWT supports static string localization through two tag interfaces (that
     * is, interfaces having no methods that represent a functionality contract)
     * and a code generation library to generate implementations of those
     * interfaces.
     * 
     * <p>
     * For example, if you wanted to localize the constant strings "hello,
     * world" and "goodbye, world" in your GWT application, you could define an
     * interface that abstracts those strings by extending the built-in
     * <code>Constants</code> interface:
     * 
     * {@example com.google.gwt.examples.i18n.MyConstants}
     * 
     * Now create an associated default properties file called
     * <code>MyConstants.properties</code> in the same package:
     * 
     * {@gwt.include com/google/gwt/examples/i18n/MyConstants.properties}
     * 
     * You can also create a localized translation for each supported locale in
     * separate properties file. In this case, we localize for Spanish:
     * 
     * {@gwt.include com/google/gwt/examples/i18n/MyConstants_es.properties}
     * 
     * To use the internationalized constants, you create an implementation of
     * <code>MyConstants</code> using {@link GWT#create(Class)}:
     * 
     * {@example com.google.gwt.examples.i18n.MyConstantsExample#useMyConstants()}
     * </p>
     * 
     * <h2>The Benefits of Static String Internationalization</h2>
     * As you can see from the example above, static internationalization relies
     * on a very tight binding between internationalized code and its localized
     * resources. Using explicit method calls in this way has a number of
     * advantages. The GWT compiler can optimize deeply, removing uncalled
     * methods and inlining localized strings -- making generated code as
     * efficient as if the strings had been hard-coded.
     * 
     * <p>
     * The value of compile-time checking becomes even more apparent when
     * applied to messages that take multiple arguments. Creating a Java method
     * for each message allows the compiler to check both the number and types
     * of arguments supplied by the calling code against the message template
     * defined in a properties file. For example, attempting to use this
     * interface:
     * 
     * {@example com.google.gwt.examples.i18n.ErrorMessages}
     * 
     * with this properties file:
     * 
     * {@gwt.include com/google/gwt/examples/i18n/ErrorMessages.properties}
     * 
     * results in a compile-time error because the message template in the
     * properties file expects three arguments, while the
     * <code>permissionDenied</code> method can only supply two.
     * </p>
     * 
     * <h2>Which Interface to Use?</h2>
     * Extend <code>{@link com.google.gwt.i18n.client.Constants}</code> to
     * create a collection of constant values of a variety of types that can be
     * accessed by calling methods (called <i>constant accessors</i>) on an
     * interface. Constant accessors may return a variety of types, including
     * strings, numbers, booleans, and even maps. A compile-time check is done
     * to ensure that the value in a properties file matches the return type
     * declared by its corresponding constant accessor. In other words, if a
     * constant accessor is declared to return an <code>int</code>, its
     * associated property is guaranteed to be a valid <code>int</code> value --
     * avoiding a potential source of runtime errors.
     * 
     * <p>
     * <code>{@link com.google.gwt.i18n.client.ConstantsWithLookup}</code> is
     * identical to <code>Constants</code> except that the interface also
     * includes a method to look up strings by property name, which facilitates
     * dynamic binding to constants by name at runtime.
     * <code>ConstantsWithLookup</code> can sometimes be useful in highly
     * data-driven applications. One caveat: <code>ConstantsWithLookup</code>
     * is less efficient than <code>Constants</code> because the compiler
     * cannot discard unused constant methods, resulting in larger applications.
     * </p>
     * 
     * <p>
     * Extend <code>{@link com.google.gwt.i18n.client.Messages}</code> to
     * create a collection of formatted messages that can accept parameters. You
     * might think of the <code>Messages</code> interface as a statically
     * verifiable equivalent of the traditional Java combination of
     * <code>Properties</code>, <code>ResourceBundle</code>, and
     * <code>MessageFormat</code> rolled into a single mechanism.
     * </p>
     * 
     * <h2>Properties Files</h2>
     * All of the types above use properties files based on the traditional <a
     * href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/Properties.html#load(java.io.InputStream)">Java
     * properties file format</a>, although GWT uses
     * {@link DeveloperGuide.Internationalization.PropertiesFiles an enhanced properties file format}
     * that are encoded as UTF-8 and can therefore contain Unicode characters
     * directly.
     * 
     * @title Static String Internationalization
     * @synopsis A type-safe and optimized approach to internationalizing
     *           strings.
     * @see com.google.gwt.i18n.client.Constants
     * @see com.google.gwt.i18n.client.ConstantsWithLookup
     * @see com.google.gwt.i18n.client.Messages
     * @see DeveloperGuide.Internationalization.PropertiesFiles
     * @see DeveloperGuide.Internationalization.DynamicStringInternationalization
     * @see com.google.gwt.i18n.client.Dictionary
     */
    public static class StaticStringInternationalization {
    }

    /**
     * The {@link com.google.gwt.i18n.client.Dictionary} class lets your GWT
     * application consume strings supplied by the
     * {@link DeveloperGuide.Fundamentals.HostPage host HTML page}. This
     * approach is convenient if your existing web server has a localization
     * system that you do not wish to integrate with the
     * {@link DeveloperGuide.Internationalization.StaticStringInternationalization static string methods}.
     * Instead, simply print your strings within the body of your HTML page as a
     * JavaScript structure, and your GWT application can reference and display
     * them to end users.
     * 
     * <p>
     * Since it binds directly to the key/value pairs in the host HTML, whatever
     * they may be, the {@link com.google.gwt.i18n.client.Dictionary} class is
     * not sensitive to the
     * {@link DeveloperGuide.Internationalization.SpecifyingLocale the GWT locale setting}.
     * Thus, the burden of generating localized strings is on your web server.
     * </p>
     * 
     * Dynamic string localization allows you to look up localized strings
     * defined in a {@link DeveloperGuide.Fundamentals.HostPage host HTML} page
     * at runtime using string-based keys.
     * 
     * <p>
     * This approach is typically slower and larger than the static string
     * approach, but does not require application code to be recompiled when
     * messages are altered or the set of locales changes.
     * </p>
     * 
     * @title Dynamic String Internationalization
     * @synopsis A flexible and simplistic method of internationalizing strings
     *           that easily integrates with existing web applications that do
     *           not support the GWT <code>locale</code> client property.
     * @tip The <code>Dictionary</code> class is completely dynamic, so it
     *      provides no static type checking, and invalid keys cannot be checked
     *      by the compiler. This is another reason we recommend using
     *      {@link DeveloperGuide.Internationalization.StaticStringInternationalization static string internationalization}
     *      where possible.
     */
    public static class DynamicStringInternationalization {
    }

    /**
     * GWT represents <code>locale</code> as a client property whose value can
     * be set either using a meta tag embedded in the
     * {@link com.google.gwt.doc.DeveloperGuide.Fundamentals.HostPage host page}
     * or in the query string of the host page's URL. Rather than being supplied
     * by GWT, the set of possible values for the <code>locale</code> client
     * property is entirely a function of your
     * {@link com.google.gwt.doc.DeveloperGuide.Fundamentals.Modules module configuration}.
     * 
     * <p>
     * If that sounded like gibberish (and it probably did), a quick digression
     * into the purpose of client properties is in order...
     * </p>
     * 
     * <h2>Client Properties and the GWT Compilation Process</h2>
     * <i>Client properties</i> are key/value pairs that can be used to
     * configure GWT modules. User agent, for example, is represented by a
     * client property. Each client property can have any number of values, but
     * all of the values must be enumerable when the GWT compiler runs.
     * 
     * <p>
     * GWT modules can define and extend the set of available client properties
     * along with the potential values each property might assume when loaded in
     * an end user's browser. At compile time, the GWT compiler determines all
     * the possible permutations of a module's client properties, from which it
     * produces multiple <i>compilations</i>. Each compilation is optimized for
     * a different set of client properties and is recorded into a file ending
     * with the suffix <code>.cache.html</code>.
     * </p>
     * 
     * <p>
     * In deployment, the end-user's browser only needs one particular
     * compilation, which is determined by mapping the end user's client
     * properties onto the available compiled permutations. Thus, only the exact
     * code required by the end user is downloaded, no more. By making locale a
     * client property, the standard startup process in <code>gwt.js</code>
     * chooses the appropriate localized version of an application, providing
     * ease of use (it's easier than it might sound!), optimized performance,
     * and minimum script size.
     * </p>
     * 
     * <h2>The Default Locale</h2>
     * The <code>com.google.gwt.i18n.I18N</code> module defines only one
     * locale by default, called <code>default</code>. This default locale is
     * used when the <code>locale</code> client property goes unspecified in
     * deployment. The default locale is used internally as a last-resort match
     * between a {@link com.google.gwt.i18n.client.Localizable} interface and a
     * localized resource or class.
     * 
     * <h2>Adding Locale Choices to a Module</h2>
     * In any real-world application, you will define at least one locale in
     * addition to the default locale. "Adding a locale" means extending the set
     * of values of the <code>locale</code> client property using the
     * <code>&lt;extend-property&gt;</code> element in your
     * {@link com.google.gwt.doc.DeveloperGuide.Fundamentals.Modules.ModuleXml module XML}.
     * 
     * <p>
     * For example, the following module adds multiple locale values:
     * 
     * {@gwt.include com/google/gwt/examples/i18n/MyAppWithLocales.gwt.xml}
     * </p>
     * 
     * 
     * <h2>Choosing a Locale at Runtime</h2>
     * The locale client property can be specified using either a meta tag or as
     * part of the query string in the host page's URL. If both are specified,
     * the query string takes precedence.
     * 
     * <p>
     * To specify the <code>locale</code> client property using a meta tag in
     * the
     * {@link com.google.gwt.doc.DeveloperGuide.Fundamentals.HostPage host page},
     * embed a meta tag for <code>gwt:property</code> as follows:
     * 
     * <pre>&lt;meta name="gwt:property" content="locale=x_Y"&gt;</pre>
     * 
     * For example, the following host HTML page sets the locale to "ja_JP":
     * 
     * {@gwt.include com/google/gwt/examples/i18n/ColorNameLookupExample_ja_JP.html}
     * </p>
     * 
     * <p>
     * To specify the <code>locale</code> client property using a query
     * string, specify a value for the name <code>locale</code>. For example,
     * 
     * <pre>http://www.example.org/myapp.html?locale=fr_CA</pre>
     * 
     * </p>
     * 
     * @title Specifying a Locale
     * @synopsis How to add locales and specify the <code>locale</code> client
     *           property during deployment.
     */
    public static class SpecifyingLocale {
    }

    /**
     * Both {@link com.google.gwt.i18n.client.Constants} and
     * {@link com.google.gwt.i18n.client.Messages} use traditional Java
     * properties files, with one notable difference: properties files used with
     * GWT should be encoded as UTF-8 and may contain Unicode characters
     * directly, avoiding the need for <code>native2ascii</code>. See the API
     * documentation for the above interfaces for examples and formatting
     * details.
     * 
     * <p>
     * Many thanks to the <a href="http://tapestry.apache.org/">Tapestry</a>
     * project for solving the problem of reading UTF-8 properties files in
     * Tapestry's <code>LocalizedProperties</code> class.
     * </p>
     * 
     * @title Localized Properties Files
     * @synopsis How to create localized properties files for use with
     *           {@link com.google.gwt.i18n.client.Constants} or
     *           {@link com.google.gwt.i18n.client.Messages}
     * @see com.google.gwt.i18n.client.Constants
     * @see com.google.gwt.i18n.client.Messages
     */
    public static class PropertiesFiles {
    }
  }

  /**
   * The
   * {@link DeveloperGuide.Fundamentals.JavaToJavaScriptCompiler GWT compiler}
   * translates Java source into JavaScript. Sometimes it's very useful to mix
   * handwritten JavaScript into your Java source code. For example, the
   * lowest-level functionality of certain core GWT classes are handwritten in
   * JavaScript. GWT borrows from the Java Native Interface (JNI) concept to
   * implement Java<i>Script</i> Native Interface (JSNI).
   * 
   * <p>
   * Writing JSNI methods is a powerful technique, but should be used sparingly.
   * JSNI code is less portable across browsers, more likely to leak memory,
   * less amenable to Java tools, and hard for the compiler to optimize.
   * </p>
   * 
   * <p>
   * We think of JSNI as the web equivalent of inline assembly code. You can:
   * <ul class="featurelist">
   * <li>Implement a Java method directly in JavaScript</li>
   * <li>Wrap type-safe Java method signatures around existing JavaScript</li>
   * <li>Call from JavaScript into Java code and vice-versa</li>
   * <li>Throw exceptions across Java/JavaScript boundaries</li>
   * <li>Read and write Java fields from JavaScript</li>
   * <li>Use hosted mode to debug both Java source (with a Java debugger) and
   * JavaScript (with a script debugger, only in Windows right now)</li>
   * </ul>
   * </p>
   * 
   * @title JavaScript Native Interface (JSNI)
   * @childIntro Specifics
   * @tip When accessing the browser's window and document objects from JSNI,
   *      you must reference them as <code>$wnd</code> and <code>$doc</code>,
   *      respectively. Your compiled script runs in a nested frame, and
   *      <code>$wnd</code> and <code>$doc</code> are automatically
   *      initialized to correctly refer to the host page's window and document.
   * @synopsis Mix handwritten JavaScript into your Java classes to access
   *           low-level browser functionality.
   */
  public static class JavaScriptNativeInterface {

    /**
     * JSNI methods are declared <code>native</code> and contain JavaScript
     * code in a specially formatted comment block between the end of the
     * parameter list and the trailing semicolon. A JSNI comment block begins
     * with the exact token <code>/&#42;-{</code> and ends with the exact
     * token <code>}-&#42;/</code>. JSNI methods are be called just like any
     * normal Java method. They can be static or instance methods.
     * 
     * <h2>Example</h2>
     * {@example com.google.gwt.user.client.Window#alert(String)}
     * 
     * @title Writing Native JavaScript Methods
     * @synopsis Declare a <code>native</code> method and write the body into
     *           a specially formatted comment.
     * @tip In {@link DeveloperGuide.Fundamentals.HostedMode hosted mode}, you
     *      can set a breakpoint on the source line containing the opening brace
     *      of a JSNI method, allowing you to see invocation arguments.
     */
    public static class JavaScriptFromJava {
    }

    /**
     * It can be very useful to manipulate Java objects from within the
     * JavaScript implementation of a JSNI method. There is a special syntax for
     * this.
     * 
     * <h2>Invoking Java methods from JavaScript</h2>
     * Calling Java methods from JavaScript is somewhat similar to calling Java
     * methods from C code in <a
     * href="http://java.sun.com/j2se/1.4.2/docs/guide/jni/index.html">JNI</a>.
     * In particular, JSNI borrows the JNI mangled method signature approach to
     * distinguish among overloaded methods.
     * 
     * <p>
     * JavaScript calls into Java methods are of the form
     * 
     * <pre class="code">[instance-expr.]@class-name::method-name(param-signature)(arguments)</pre>
     * 
     * where
     * <dl class="fixed">
     * <dt>[instance-expr.]</dt>
     * <dd> must be present when calling an instance method and must be absent
     * when calling a static method</dd>
     * <dt>class-name</dt>
     * <dd> is the fully-qualified name of the class in which the method is
     * declared (or a subclass thereof)</dd>
     * <dt>param-signature</dt>
     * <dd>is the internal Java method signature as specified <a
     * href="http://java.sun.com/j2se/1.4.2/docs/guide/jni/spec/types.html#wp16432">here</a>
     * but without the trailing signature of the method return type since it
     * isn't needed to choose the overload</dd>
     * <dt>arguments</dt>
     * <dd>the actual argument list to pass to the called method</dd>
     * </dl>
     * </p>
     * 
     * <h2>Accessing Java fields from JavaScript</h2>
     * Static and instance fields can be accessed from handwritten JavaScript.
     * Field references are of the form
     * 
     * <pre class="code">[instance-expr.]@class-name::field-name</pre>
     * 
     * <h2>Example</h2>
     * {@example com.google.gwt.examples.JSNIExample}
     * 
     * @title Accessing Java Methods and Fields from JavaScript
     * @synopsis Handwritten JavaScript can invoke methods and access fields on
     *           Java objects.
     * @tip When writing JSNI code, it's helpful to occasionally run in
     *      {@link DeveloperGuide.Fundamentals.WebMode web mode}. The
     *      {@link DeveloperGuide.Fundamentals.JavaToJavaScriptCompiler JavaScript compiler}
     *      checks your JSNI code and can flag errors at compile time that you
     *      wouldn't catch until runtime in
     *      {@link DeveloperGuide.Fundamentals.HostedMode hosted mode}.
     */
    public static class JavaFromJavaScript {
    }

    /**
     * Parameters and return types in JSNI methods are declared as Java types.
     * There are very specific rules for how values passing in and out of
     * JavaScript code must be treated. These rules must be followed whether the
     * values enter and leave through normal method call semantics, or through
     * the
     * {@link DeveloperGuide.JavaScriptNativeInterface.JavaFromJavaScript special syntax}.
     * 
     * <p>
     * <h2>Passing Java values into JavaScript</h2>
     * </p>
     * 
     * <table width="80%" align="center">
     * <tr valign="top">
     * <th width="30%" align="left">Incoming Java type</th>
     * <th width="70%" align="left">How it appears to JavaScript code</th>
     * </tr>
     * <tr valign="top">
     * <td><nobr>a Java numeric primitive</nobr></td>
     * <td>a JavaScript numeric value, as in <nobr><code>var x = 42;</code></nobr></td>
     * </tr>
     * <tr valign="top">
     * <td><code>String</code></td>
     * <td> a JavaScript string, as in <nobr><code>var s = "my string";</code></nobr>
     * </td>
     * </tr>
     * <tr valign="top">
     * <td><code>boolean</code></td>
     * <td>a JavaScript boolean value, as in <nobr><code>var b = true;</code></nobr>
     * </td>
     * </tr>
     * <tr valign="top">
     * <td><nobr>{@link com.google.gwt.core.client.JavaScriptObject} (see
     * notes)</nobr></td>
     * <td>a <code>JavaScriptObject</code> that must have originated from
     * JavaScript code, typically as the return value of some other JSNI method
     * </td>
     * </tr>
     * <tr valign="top">
     * <td><nobr>Java array</nobr></td>
     * <td> an opaque value that can only be passed back into Java code</td>
     * </tr>
     * <tr valign="top">
     * <td><nobr>any other Java <code>Object</code></nobr></td>
     * <td> an opaque value accessible through
     * {@link DeveloperGuide.JavaScriptNativeInterface.JavaFromJavaScript special syntax}</td>
     * </tr>
     * </table>
     * 
     * <p>
     * <h2>Passing JavaScript values into Java code</h2>
     * </p>
     * 
     * <table width="80%" align="center">
     * <tr valign="top">
     * <th width="30%" align="left">Outgoing Java type</th>
     * <th width="70%" align="left">What must be passed</th>
     * </tr>
     * <tr valign="top">
     * <td><nobr>a Java numeric primitive</nobr></td>
     * <td>a JavaScript numeric value, as in <nobr><code>return 19;</code></nobr></td>
     * </tr>
     * <tr valign="top">
     * <td><code>String</code></td>
     * <td>a JavaScript string, as in <nobr><code>return "boo";</code></nobr></td>
     * </tr>
     * <tr valign="top">
     * <td><code>boolean</code></td>
     * <td> a JavaScript boolean value, as in <nobr><code>return false;</code></nobr>
     * </td>
     * </tr>
     * <tr valign="top">
     * <td><nobr>{@link JavaScriptObject} (see notes)</nobr></td>
     * <td> a native JavaScript object, as in <nobr><code>return
     * document.createElement("div")</code></nobr></td>
     * </tr>
     * <tr valign="top">
     * <td><nobr>any other Java <code>Object</code> (including arrays)</nobr></td>
     * <td>a Java <code>Object</code> of the correct type that must have
     * originated in Java code; Java objects cannot be constructed from "thin
     * air" in JavaScript</td>
     * </tr>
     * </table>
     * 
     * <h2>Important Notes</h2>
     * <ul>
     * <li> A Java numeric primitive is one of <code>byte</code>,
     * <code>short</code>, <code>char</code>, <code>int</code>,
     * <code>long</code>, <code>float</code>, or <code>double</code>.
     * You must ensure the value is appropriate for the declared type. Returning
     * <code>3.7</code> when the declared type is <code>int</code> will
     * cause unpredictable behavior. </li>
     * 
     * <li> Java <code>null</code> and JavaScript <code>null</code> are
     * identical and always legal values for any non-primitive Java type.
     * JavaScript <code>undefined</code> is <i>not</i> identical to
     * <code>null</code>; never return <code>undefined</code> from a JSNI
     * method or unpredictable behavior will occur. </li>
     * 
     * <li> Violating any of these marshaling rules in
     * {@link DeveloperGuide.Fundamentals.HostedMode hosted mode} will generate
     * a <code>com.google.gwt.dev.shell.HostedModeException</code> detailing
     * the problem. This exception is not
     * {@link DeveloperGuide.Fundamentals.ClientSide translatable} and never
     * thrown in {@link DeveloperGuide.Fundamentals.WebMode web mode}. </li>
     * 
     * <li> {@link JavaScriptObject} is a magical type that gets special
     * treatment from the GWT compiler and hosted browser. Its purpose is to
     * provide an opaque representation of native JavaScript objects to Java
     * code.</li>
     * </ul>
     * 
     * @title Sharing objects between Java source and JavaScript
     * @synopsis How Java objects appear to JavaScript code and vice-versa.
     * @tip When returning a possibly undefined value from a JSNI method, we
     *      suggest using the idiom <blockquote><code>return (value == null) ? null : value;</code></blockquote>
     *      to avoid returning <code>undefined</code>.
     */
    public static class Marshaling {
    }

    /**
     * Exceptions can originate both in Java code and in handwritten JavaScript
     * code.
     * 
     * <p>
     * An exception that originates in a JSNI method and escapes into Java code
     * can be caught as a {@link JavaScriptException}. Relying on this behavior
     * is discouraged because JavaScript exceptions are not usefully typed. The
     * recommended practice is to handle JavaScript exceptions in JavaScript
     * code and Java exceptions in Java code.
     * </p>
     * 
     * <p>
     * When a JSNI method invokes a Java method, a more complex call chain
     * results. An exception thrown from the inner Java method can safely pass
     * through the sandwiched JSNI method back to the original Java call site,
     * retaining type fidelity. It can be caught as expected. For example,
     * 
     * <ol>
     * <li>Java method <code>foo()</code> calls JSNI method
     * <code>bar()</code></li>
     * <li>JavaScript method <code>bar()</code> calls Java method
     * <code>baz()</code></li>
     * <li>Java method <code>baz()</code> throws an exception</li>
     * </ol>
     * 
     * The exception thrown out of <code>baz()</code> will propagate through
     * <code>bar()</code> and can be caught in <code>foo()</code>.
     * </p>
     * 
     * @title Exceptions and JSNI
     * @synopsis How JavaScript exceptions interact with Java exceptions and
     *           vice-versa.
     */
    public static class JsniExceptions {
    }

  }

  /**
   * Deferred binding story.
   * 
   * @title Deferred Binding
   * @synopsis Deferred binding is a general-purpose class substitution and code
   *           generation subsystem that makes GWT modules highly-configurable
   *           without sacrificing performance.
   * @skip
   */
  public static class DeferredBinding {

    /**
     * Client properties are name/value pairs that are available to
     * {@link Concepts.ClientServer client-side} code. They are similar to Java
     * system properties and can be used for a variety of purposes.
     * 
     * <h2>Setting Client Properties</h2>
     * Client properties are accumulated from various sources, but they are all
     * accessed using
     * {@link com.google.amp.client.Application#getProperty(String)}. The
     * following list describes the various ways client properties can be
     * defined. When a particular name is defined more than once, the method
     * corresponding to the higher number below will override the method
     * corresponding to a lower number. For example, suppose a client property
     * called "language" has been set in the configuration of <i>MyApplication</i>
     * to "en". But if <i>MyApplication</i> is then requested like this:
     * 
     * <pre>
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                             http://localhost/MyApplication.ui?language=fr
     * </pre>
     * 
     * Then calling <code>Application.get().getProperty("language")</code>
     * will return "fr" instead of "en".
     * 
     * <dl>
     * <dt>Method 1: Formerly Saved as Cookies</dt>
     * <dd> Client-side properties serve as a convenient mechanism for accessing
     * <i>client-side cookies</i>. If a client property has previously been set
     * as a cookie when running inside the same client browser, its value is
     * available upon startup. </dd>
     * 
     * <dt>Method 2: Pre-configured</dt>
     * <dd> Applications can define client properties that are always set when
     * the application starts up. To define client properties this way, you can
     * either use the DCS Admin Application or by editing an
     * {@link Concepts.Applications application config file} directly. </dd>
     * 
     * <dt>Method 3: Specified by the Request</dt>
     * <dd> An application request can also specify client properties that will
     * be available when the application starts up. In hosted mode, the
     * application request is specified by setting the Java system property
     * called
     * <code>{@link Concepts.HostedModeAndWebMode.LaunchingHostedMode dcs.args}</code>.
     * In web mode, the application request is the
     * {@link Concepts.HostedModeAndWebMode.RunningApplicationsInWebMode URL entered into the web browser},
     * whose query string contains name/value pairs that become client
     * properties. </dd>
     * 
     * <dt>Method 4: Programmatically</dt>
     * <dd> A running application can change its set of properties by calling
     * {@link com.google.amp.client.Application#setProperty(String, String, boolean)}
     * any time during execution. </dd>
     * </dl>
     * 
     * <h2>Getting Client Properties</h2>
     * Client-side code can get the value of a client property using
     * {@link com.google.amp.client.Application#getProperty(String)}. Remember
     * the order of precedence described above if you are surprised by the
     * result when retrieving a client property.
     * 
     * <h2>Client Properties are Available to Deferred Binding Contexts</h2>
     * All client properties defined using method 2 or 3 above are available to
     * {@link Concepts.DeferredBinding.Contexts deferred binding contexts}
     * during the application request process. This allows URLs, for example, to
     * influence deferred binding decisions. Deferred binding contexts are the
     * only server-side code that actually has any knowledge of client
     * properties.
     * 
     * @skip
     * @title Client Properties
     * @childIntro Read about my children
     * @index client properties
     * @index cookies, exposed as client properties
     * @see Concepts.ServerProperties
     * @see Concepts.DeferredBinding
     */
    public static class ClientProperties {
    }
  }

  /**
   * All content is Copyright Google Inc. Most web page content is licensed
   * under the Creative Commons Attribution 2.5 License, as noted on those
   * pages. The Google Web Toolkit source code is licensed under terms available
   * <a href="ttp://code.google.com/webtoolkit/terms.html">here</a>.
   * 
   * <p>
   * <i>Java</i> is a trademark of Sun Microsystems, Inc. in the United States
   * and other countries.
   * </p>
   * 
   * <p>
   * <i>Microsoft</i>, <i>Windows</i>, and <i>ActiveX</i> are either
   * registered trademarks or trademarks of Microsoft Corporation in the United
   * States and/or other countries.
   * </p>
   * 
   * <p>
   * Other trademarks are the property of their respective owners.
   * </p>
   * 
   * @title Legal Notices
   * @order -1
   */
  public class LegalNotices {
  }
}
