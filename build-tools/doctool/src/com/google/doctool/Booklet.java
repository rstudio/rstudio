/*
 * Copyright 2006 Google Inc.
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
package com.google.doctool;

import com.google.doctool.LinkResolver.ExtraClassResolver;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

/**
 * Generates XML from Javadoc source, with particular idioms to make it possible
 * to translate into either expository doc or API doc.
 */
public class Booklet {

  private static final String OPT_BKCODE = "-bkcode";
  private static final String OPT_BKDOCPKG = "-bkdocpkg";
  private static final String OPT_BKOUT = "-bkout";

  private static Booklet sBooklet;

  public static void main(String[] args) {
    // Strip off our arguments at the beginning.
    //
    com.sun.tools.javadoc.Main.execute(args);
  }

  public static int optionLength(String option) {
    if (option.equals(OPT_BKOUT)) {
      return 2;
    } else if (option.equals(OPT_BKDOCPKG)) {
      return 2;
    } else if (option.equals(OPT_BKCODE)) {
      return 1;
    }
    return 0;
  }

  public static String slurpSource(SourcePosition position) {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(position.file()));
      for (int i = 0, n = position.line() - 1; i < n; ++i) {
        br.readLine();
      }

      StringBuffer lines = new StringBuffer();
      String line = br.readLine();
      int braceDepth = 0;
      int indent = -1;
      boolean seenSemiColonOrBrace = false;
      while (line != null) {
        if (indent == -1) {
          for (indent = 0; Character.isWhitespace(line.charAt(indent)); ++indent) {
            // just accumulate
          }
        }

        if (line.length() >= indent) {
          line = line.substring(indent);
        }

        lines.append(line + "\n");
        for (int i = 0, n = line.length(); i < n; ++i) {
          char c = line.charAt(i);
          if (c == '{') {
            seenSemiColonOrBrace = true;
            ++braceDepth;
          } else if (c == '}') {
            --braceDepth;
          } else if (c == ';') {
            seenSemiColonOrBrace = true;
          }
        }

        if (braceDepth > 0 || !seenSemiColonOrBrace) {
          line = br.readLine();
        } else {
          break;
        }
      }

      String code = lines.toString();
      return code;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (br != null) {
          br.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return "";
  }

  public static boolean start(RootDoc rootDoc) {
    getBooklet().process(rootDoc);
    return true;
  }

  public static boolean validOptions(String[][] options,
      DocErrorReporter reporter) {
    return getBooklet().analyzeOptions(options, reporter);
  }

  private static Booklet getBooklet() {
    if (sBooklet == null) {
      sBooklet = new Booklet();
    }
    return sBooklet;
  }

  private String outputPath;

  private HashSet packagesToGenerate;

  private RootDoc initialRootDoc;

  private String rootDocId;

  private boolean showCode;

  private HashSet standardTagKinds = new HashSet();

  private Stack tagStack = new Stack();

  private PrintWriter pw;

  public Booklet() {
    // Set up standard tags (to ignore during tag processing)
    //
    standardTagKinds.add("@see");
    standardTagKinds.add("@serial");
    standardTagKinds.add("@throws");
    standardTagKinds.add("@param");
    standardTagKinds.add("@id");
  }

  private boolean analyzeOptions(String[][] options, DocErrorReporter reporter) {
    for (int i = 0, n = options.length; i < n; ++i) {
      if (options[i][0].equals(OPT_BKOUT)) {
        outputPath = options[i][1];
      } else if (options[i][0].equals(OPT_BKDOCPKG)) {
        String[] packages = options[i][1].split(";");
        packagesToGenerate = new HashSet();
        for (int packageIndex = 0; packageIndex < packages.length; ++packageIndex) {
          packagesToGenerate.add(packages[packageIndex]);
        }
      } else if (options[i][0].equals(OPT_BKCODE)) {
        showCode = true;
      }
    }

    if (outputPath == null) {
      reporter.printError("You must specify an output directory with "
          + OPT_BKOUT);
      return false;
    }

    return true;
  }

  private void begin(String tag) {
    pw.print("<" + tag + ">");
    tagStack.push(tag);
  }

  private void begin(String tag, String attr, String value) {
    pw.print("<" + tag + " " + attr + "='" + value + "'>");
    tagStack.push(tag);
  }

  private void beginCDATA() {
    pw.print("<![CDATA[");
  }

  private void beginEndln(String tag) {
    pw.println("<" + tag + "/>");
  }

  private void beginln(String tag) {
    pw.println();
    begin(tag);
  }

  private void beginln(String tag, String attr, String value) {
    pw.println();
    begin(tag, attr, value);
  }

  private void emitDescription(ClassDoc enclosing, Doc forWhat,
      Tag[] leadInline, Tag[] descInline) {
    emitJRELink(enclosing, forWhat);

    beginln("lead");
    processTags(leadInline);
    endln();

    beginln("description");
    processTags(descInline);
    endln();
  }

  private void emitIdentity(String id, String name) {
    beginln("id");
    text(id);
    endln();

    beginln("name");
    text(name);
    endln();
  }

  private void emitJRELink(ClassDoc enclosing, Doc doc) {
    String jreLink = "http://java.sun.com/j2se/1.5.0/docs/api/";
    if (doc instanceof ClassDoc) {
      ClassDoc classDoc = (ClassDoc) doc;
      String pkg = classDoc.containingPackage().name();
      if (!pkg.startsWith("java.")) {
        return;
      }
      String clazz = classDoc.name();

      jreLink += pkg.replace('.', '/') + "/";
      jreLink += clazz;
      jreLink += ".html";
    } else if (doc instanceof ExecutableMemberDoc) {
      ExecutableMemberDoc execMemberDoc = (ExecutableMemberDoc) doc;
      String pkg = enclosing.containingPackage().name();
      if (!pkg.startsWith("java.")) {
        return;
      }
      String clazz = enclosing.name();
      String method = execMemberDoc.name();
      String sig = execMemberDoc.signature();

      jreLink += pkg.replace('.', '/') + "/";
      jreLink += clazz;
      jreLink += ".html";
      jreLink += "#";
      jreLink += method;
      jreLink += sig;
    } else if (doc instanceof PackageDoc) {
      String pkg = doc.name();
      if (!pkg.startsWith("java.")) {
        return;
      }
      jreLink += pkg.replace('.', '/') + "/package-summary.html";
    } else if (doc instanceof FieldDoc) {
      FieldDoc fieldDoc = (FieldDoc) doc;
      String pkg = enclosing.containingPackage().name();
      if (!pkg.startsWith("java.")) {
        return;
      }
      String clazz = fieldDoc.containingClass().name();
      String field = fieldDoc.name();

      jreLink += pkg.replace('.', '/') + "/";
      jreLink += clazz;
      jreLink += ".html";
      jreLink += "#";
      jreLink += field;
    }

    // Add the link.
    //
    beginln("jre");
    text(jreLink);
    endln();
  }

  private void emitLocation(Doc doc) {
    Doc parent = getParentDoc(doc);
    if (parent != null) {
      beginln("location");
      emitLocationLink(parent);
      endln();
    }
  }

  private void emitLocationLink(Doc doc) {
    // Intentionally reverses the order.
    //
    String myId;
    String myTitle;
    if (doc instanceof MemberDoc) {
      MemberDoc memberDoc = (MemberDoc) doc;
      myId = getId(memberDoc);
      myTitle = memberDoc.name();
    } else if (doc instanceof ClassDoc) {
      ClassDoc classDoc = (ClassDoc) doc;
      myId = getId(classDoc);
      myTitle = classDoc.name();
    } else if (doc instanceof PackageDoc) {
      PackageDoc pkgDoc = (PackageDoc) doc;
      myId = getId(pkgDoc);
      myTitle = pkgDoc.name();
    } else if (doc instanceof RootDoc) {
      myId = rootDocId;
      myTitle = initialRootDoc.name();
    } else {
      throw new IllegalStateException(
          "Expected only a member, type, or package");
    }

    Doc parent = getParentDoc(doc);
    if (parent != null) {
      emitLocationLink(parent);
    }

    beginln("link", "ref", myId);

    Tag[] titleTag = doc.tags("@title");
    if (titleTag.length > 0) {
      myTitle = titleTag[0].text().trim();
    }

    if (myTitle == null || myTitle.length() == 0) {
      myTitle = "[NO TITLE]";
    }

    text(myTitle);

    endln();
  }

  private void emitModifiers(ProgramElementDoc doc) {
    if (doc.isPrivate()) {
      beginEndln("isPrivate");
    } else if (doc.isProtected()) {
      beginEndln("isProtected");
    } else if (doc.isPublic()) {
      beginEndln("isPublic");
    } else if (doc.isPackagePrivate()) {
      beginEndln("isPackagePrivate");
    }

    if (doc.isStatic()) {
      beginEndln("isStatic");
    }

    if (doc.isFinal()) {
      beginEndln("isFinal");
    }

    if (doc instanceof MethodDoc) {
      MethodDoc methodDoc = (MethodDoc) doc;

      if (methodDoc.isAbstract()) {
        beginEndln("isAbstract");
      }

      if (methodDoc.isSynchronized()) {
        beginEndln("isSynchronized");
      }
    }
  }

  private void emitOutOfLineTags(Tag[] tags) {
    beginln("tags");
    processTags(tags);
    endln();
  }

  private void emitType(Type type) {
    ClassDoc typeAsClass = type.asClassDoc();

    if (typeAsClass != null) {
      begin("type", "ref", getId(typeAsClass));
    } else {
      begin("type");
    }

    String typeName = type.typeName();
    String dims = type.dimension();

    text(typeName + dims);

    end();
  }

  private void end() {
    pw.print("</" + tagStack.pop() + ">");
  }

  private void endCDATA() {
    pw.print("]]>");
  }

  private void endln() {
    end();
    pw.println();
  }

  private MethodDoc findMatchingInterfaceMethodDoc(ClassDoc[] interfaces,
      MethodDoc methodDoc) {
    if (interfaces != null) {
      // Look through the methods on superInterface for a matching methodDoc.
      //
      for (int intfIndex = 0; intfIndex < interfaces.length; ++intfIndex) {
        ClassDoc currentIntfDoc = interfaces[intfIndex];
        MethodDoc[] intfMethodDocs = currentIntfDoc.methods();
        for (int methodIndex = 0; methodIndex < intfMethodDocs.length; ++methodIndex) {
          MethodDoc intfMethodDoc = intfMethodDocs[methodIndex];
          String methodDocName = methodDoc.name();
          String intfMethodDocName = intfMethodDoc.name();
          if (methodDocName.equals(intfMethodDocName)) {
            if (methodDoc.signature().equals(intfMethodDoc.signature())) {
              // It's a match!
              //
              return intfMethodDoc;
            }
          }
        }

        // Try the superinterfaces of this interface.
        //
        MethodDoc foundMethodDoc = findMatchingInterfaceMethodDoc(
            currentIntfDoc.interfaces(), methodDoc);
        if (foundMethodDoc != null) {
          return foundMethodDoc;
        }
      }
    }

    // Just didn't find it anywhere. Must not be based on an implemented
    // interface.
    //
    return null;
  }

  private ExtraClassResolver getExtraClassResolver(Tag tag) {

    if (tag.holder() instanceof PackageDoc) {
      return new ExtraClassResolver() {
        public ClassDoc findClass(String className) {
          return initialRootDoc.classNamed(className);
        }
      };
    }

    return null;
  }

  private String getId(ClassDoc classDoc) {
    return classDoc.qualifiedName();
  }

  private String getId(ExecutableMemberDoc memberDoc) {
    // Use the mangled name to look up a unique id (based on its hashCode).
    //
    String clazz = memberDoc.containingClass().qualifiedName();
    String id = clazz + "#" + memberDoc.name() + memberDoc.signature();
    return id;
  }

  private String getId(FieldDoc fieldDoc) {
    String clazz = fieldDoc.containingClass().qualifiedName();
    String id = clazz + "#" + fieldDoc.name();
    return id;
  }

  private String getId(MemberDoc memberDoc) {
    if (memberDoc.isMethod()) {
      return getId((MethodDoc) memberDoc);
    } else if (memberDoc.isConstructor()) {
      return getId((ConstructorDoc) memberDoc);
    } else if (memberDoc.isField()) {
      return getId((FieldDoc) memberDoc);
    } else {
      throw new RuntimeException("Unknown member type");
    }
  }

  private String getId(PackageDoc packageDoc) {
    return packageDoc.name();
  }

  private Doc getParentDoc(Doc doc) {
    if (doc instanceof MemberDoc) {
      MemberDoc memberDoc = (MemberDoc) doc;
      return memberDoc.containingClass();
    } else if (doc instanceof ClassDoc) {
      ClassDoc classDoc = (ClassDoc) doc;
      Doc enclosingClass = classDoc.containingClass();
      if (enclosingClass != null) {
        return enclosingClass;
      } else {
        return classDoc.containingPackage();
      }
    } else if (doc instanceof PackageDoc) {
      return initialRootDoc;
    } else if (doc instanceof RootDoc) {
      return null;
    } else {
      throw new IllegalStateException(
          "Expected only a member, type, or package");
    }
  }

  private boolean looksSynthesized(ExecutableMemberDoc memberDoc) {
    SourcePosition memberPos = memberDoc.position();
    int memberLine = memberPos.line();

    SourcePosition classPos = memberDoc.containingClass().position();
    int classLine = classPos.line();

    if (memberLine == classLine) {
      return true;
    } else {
      return false;
    }
  }

  private void process(ClassDoc enclosing, ClassDoc classDoc) {
    // Make sure it isn't a @skip-ped topic.
    //
    if (classDoc.tags("@skip").length > 0) {
      // This one is explicitly skipped right now.
      //
      return;
    }

    if (classDoc.isInterface()) {
      beginln("interface");
    } else {
      beginln("class");
    }

    emitIdentity(getId(classDoc), classDoc.name());
    emitLocation(classDoc);
    emitDescription(enclosing, classDoc, classDoc.firstSentenceTags(),
        classDoc.inlineTags());
    emitOutOfLineTags(classDoc.tags());
    emitModifiers(classDoc);

    ClassDoc superclassDoc = classDoc.superclass();
    if (superclassDoc != null) {
      beginln("superclass", "ref", getId(superclassDoc));
      text(superclassDoc.name());
      endln();
    }

    ClassDoc[] superinterfacesDoc = classDoc.interfaces();
    for (int i = 0; i < superinterfacesDoc.length; i++) {
      ClassDoc superinterfaceDoc = superinterfacesDoc[i];
      beginln("superinterface", "ref", getId(superinterfaceDoc));
      text(superinterfaceDoc.name());
      endln();
    }

    ClassDoc[] cda = classDoc.innerClasses();
    for (int i = 0; i < cda.length; i++) {
      process(classDoc, cda[i]);
    }

    FieldDoc[] fda = classDoc.fields();
    for (int i = 0; i < fda.length; i++) {
      process(classDoc, fda[i]);
    }

    ConstructorDoc[] ctorDocs = classDoc.constructors();
    for (int i = 0; i < ctorDocs.length; i++) {
      process(classDoc, ctorDocs[i]);
    }

    MethodDoc[] methods = classDoc.methods();
    for (int i = 0; i < methods.length; i++) {
      process(classDoc, methods[i]);
    }

    endln();
  }

  private void process(ClassDoc enclosing, ExecutableMemberDoc memberDoc) {
    if (looksSynthesized(memberDoc)) {
      // Skip it.
      //
      return;
    }

    // Make sure it isn't a @skip-ped member.
    //
    if (memberDoc.tags("@skip").length > 0) {
      // This one is explicitly skipped right now.
      //
      return;
    }

    if (memberDoc instanceof MethodDoc) {
      beginln("method");
      emitIdentity(getId(memberDoc), memberDoc.name());
      emitLocation(memberDoc);

      // If this method is not explicitly documented, use the best inherited
      // one.
      //
      String rawComment = memberDoc.getRawCommentText();
      if (rawComment.length() == 0) {
        // Switch out the member doc being used.
        //
        MethodDoc methodDoc = (MethodDoc) memberDoc;
        MethodDoc superMethodDoc = methodDoc.overriddenMethod();

        if (superMethodDoc == null) {

          ClassDoc classDocToTry = methodDoc.containingClass();
          while (classDocToTry != null) {
            // See if this is a method from an interface.
            // If so, borrow its description.
            //
            superMethodDoc = findMatchingInterfaceMethodDoc(
                classDocToTry.interfaces(), methodDoc);

            if (superMethodDoc != null) {
              break;
            }

            classDocToTry = classDocToTry.superclass();
          }
        }

        if (superMethodDoc != null) {
          // Borrow the description from the superclass/superinterface.
          //
          memberDoc = superMethodDoc;
        }
      }
    } else if (memberDoc instanceof ConstructorDoc) {
      beginln("constructor");
      emitIdentity(getId(memberDoc), memberDoc.containingClass().name());
      emitLocation(memberDoc);
    } else {
      throw new IllegalStateException("What kind of executable member is this?");
    }

    emitDescription(enclosing, memberDoc, memberDoc.firstSentenceTags(),
        memberDoc.inlineTags());
    emitOutOfLineTags(memberDoc.tags());
    emitModifiers(memberDoc);

    begin("flatSignature");
    text(memberDoc.flatSignature());
    end();

    // Return type if it's a method
    //
    if (memberDoc instanceof MethodDoc) {
      emitType(((MethodDoc) memberDoc).returnType());
    }

    // Parameters
    //
    beginln("params");
    Parameter[] pda = memberDoc.parameters();
    for (int i = 0; i < pda.length; i++) {
      Parameter pd = pda[i];

      begin("param");
      emitType(pd.type());
      begin("name");
      text(pd.name());
      end();
      end();
    }
    endln();

    // Exceptions thrown
    //
    ClassDoc[] tea = memberDoc.thrownExceptions();
    if (tea.length > 0) {
      beginln("throws");
      for (int i = 0; i < tea.length; ++i) {
        ClassDoc te = tea[i];
        beginln("throw", "ref", getId(te));
        text(te.name());
        endln();
      }
      endln();
    }

    // Maybe show code
    //
    if (showCode) {
      SourcePosition pos = memberDoc.position();
      if (pos != null) {
        beginln("code");
        String source = slurpSource(pos);
        begin("pre", "class", "code");
        beginCDATA();
        text(source);
        endCDATA();
        endln();
        endln();
      }
    }

    endln();
  }

  private void process(ClassDoc enclosing, FieldDoc fieldDoc) {
    // Make sure it isn't @skip-ped.
    //
    if (fieldDoc.tags("@skip").length > 0) {
      // This one is explicitly skipped right now.
      //
      return;
    }

    String commentText = fieldDoc.commentText();
    if (fieldDoc.isPrivate()
        && (commentText == null || commentText.length() == 0)) {
      return;
    }

    beginln("field");
    emitIdentity(fieldDoc.qualifiedName(), fieldDoc.name());
    emitLocation(fieldDoc);
    emitDescription(enclosing, fieldDoc, fieldDoc.firstSentenceTags(),
        fieldDoc.inlineTags());
    emitOutOfLineTags(fieldDoc.tags());
    emitModifiers(fieldDoc);
    emitType(fieldDoc.type());
    endln();
  }

  private void process(PackageDoc packageDoc) {
    beginln("package");

    emitIdentity(packageDoc.name(), packageDoc.name());
    emitLocation(packageDoc);
    emitDescription(null, packageDoc, packageDoc.firstSentenceTags(),
        packageDoc.inlineTags());
    emitOutOfLineTags(packageDoc.tags());

    // Top-level classes
    //
    ClassDoc[] cda = packageDoc.allClasses();
    for (int i = 0; i < cda.length; i++) {
      ClassDoc cd = cda[i];

      // Make sure we have source.
      //
      SourcePosition p = cd.position();
      if (p == null || p.line() == 0) {
        // Skip this since it isn't ours (otherwise we would have source).
        //
        continue;
      }

      if (cd.containingClass() == null) {
        process(cd, cda[i]);
      } else {
        // Not a top-level class.
        //
        cd = cda[i];
      }
    }

    endln();
  }

  private void process(RootDoc rootDoc) {
    try {
      initialRootDoc = rootDoc;
      File outputFile = new File(outputPath);
      // Ignore result since the next line will fail if the directory doesn't
      // exist.
      outputFile.getParentFile().mkdirs();
      FileWriter fw = new FileWriter(outputFile);
      pw = new PrintWriter(fw, true);

      beginln("booklet");

      rootDocId = "";
      String title = "";
      Tag[] idTags = rootDoc.tags("@id");
      if (idTags.length > 0) {
        rootDocId = idTags[0].text();
      } else {
        initialRootDoc.printWarning("Expecting @id in an overview html doc; see -overview");
      }

      Tag[] titleTags = rootDoc.tags("@title");
      if (titleTags.length > 0) {
        title = titleTags[0].text();
      } else {
        initialRootDoc.printWarning("Expecting @title in an overview html doc; see -overview");
      }

      emitIdentity(rootDocId, title);
      emitLocation(rootDoc);
      emitDescription(null, rootDoc, rootDoc.firstSentenceTags(),
          rootDoc.inlineTags());
      emitOutOfLineTags(rootDoc.tags());

      // Create a list of the packages to iterate over.
      //
      HashSet packageNames = new HashSet();
      ClassDoc[] cda = initialRootDoc.classes();
      for (int i = 0; i < cda.length; i++) {
        ClassDoc cd = cda[i];
        // Only top-level classes matter.
        //
        if (cd.containingClass() == null) {
          packageNames.add(cd.containingPackage().name());
        }
      }

      // Packages
      //
      for (Iterator iter = packageNames.iterator(); iter.hasNext();) {
        String packageName = (String) iter.next();

        // Only process this package if either no "docpkg" is set, or it is
        // included.
        //
        if (packagesToGenerate == null
            || packagesToGenerate.contains(packageName)) {
          PackageDoc pd = initialRootDoc.packageNamed(packageName);
          process(pd);
        }
      }

      endln();
    } catch (Exception e) {
      e.printStackTrace();
      initialRootDoc.printError("Caught exception: " + e.toString());
    }
  }

  private void processSeeTag(SeeTag seeTag) {
    String ref = null;
    ClassDoc cd = null;
    PackageDoc pd = null;
    MemberDoc md = null;
    String title = null;

    // Check for HTML links
    if (seeTag.text().startsWith("<")) {
      // TODO: ignore for now
      return;
    }
    // Ordered: most-specific to least-specific
    if (null != (md = seeTag.referencedMember())) {
      ref = getId(md);
    } else if (null != (cd = seeTag.referencedClass())) {
      ref = getId(cd);

      // See if the target has a title.
      //
      Tag[] titleTag = cd.tags("@title");
      if (titleTag.length > 0) {
        title = titleTag[0].text().trim();
        if (title.length() == 0) {
          title = null;
        }
      }
    } else if (null != (pd = seeTag.referencedPackage())) {
      ref = getId(pd);
    }

    String label = seeTag.label();

    // If there is a label, use it.
    if (label == null || label.trim().length() == 0) {

      // If there isn't a label, see if the @see target has a @title.
      //
      if (title != null) {
        label = title;
      } else {
        label = seeTag.text();

        if (label.endsWith(".")) {
          label = label.substring(0, label.length() - 1);
        }

        // Rip off all but the last interesting part to prevent fully-qualified
        // names everywhere.
        //
        int last1 = label.lastIndexOf('.');
        int last2 = label.lastIndexOf('#');

        if (last2 > last1) {
          // Use the class name plus the member name.
          //
          label = label.substring(last1 + 1).replace('#', '.');
        } else if (last1 != -1) {
          label = label.substring(last1 + 1);
        }

        if (label.charAt(0) == '.') {
          // Started with "#" so remove the dot.
          //
          label = label.substring(1);
        }
      }
    }

    if (ref != null) {
      begin("link", "ref", ref);
      text(label != null ? label.trim() : "");
      end();
    } else {
      initialRootDoc.printWarning(seeTag.position(),
          "Unverifiable cross-reference to '" + seeTag.text() + "'");
      // The link probably won't work, but emit it anyway.
      begin("link");
      text(label != null ? label.trim() : "");
      end();
    }
  }

  private void processTags(Tag[] tags) {
    for (int i = 0; i < tags.length; i++) {
      Tag tag = tags[i];
      String tagKind = tag.kind();
      if (tagKind.equals("Text")) {
        text(tag.text());
      } else if (tagKind.equals("@see")) {
        processSeeTag((SeeTag) tag);
      } else if (tagKind.equals("@param")) {
        ParamTag paramTag = (ParamTag) tag;
        beginln("param");
        begin("name");
        text(paramTag.parameterName());
        end();
        begin("description");
        processTags(paramTag.inlineTags());
        end();
        endln();
      } else if (tagKind.equals("@example")) {
        ExtraClassResolver extraClassResolver = getExtraClassResolver(tag);
        SourcePosition pos = LinkResolver.resolveLink(tag, extraClassResolver);
        String source = slurpSource(pos);
        begin("pre", "class", "code");
        beginCDATA();
        text(source);
        endCDATA();
        endln();
      } else if (tagKind.equals("@gwt.include")) {
        String contents = ResourceIncluder.getResourceFromClasspathScrubbedForHTML(tag);
        begin("pre", "class", "code");
        text(contents);
        endln();
      } else if (!standardTagKinds.contains(tag.name())) {
        // Custom tag; pass it along other tag.
        //
        String tagName = tag.name().substring(1);
        begin(tagName);
        processTags(tag.inlineTags());
        end();
      }
    }
  }

  private void text(String s) {
    pw.print(s);
  }
}
