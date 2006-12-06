// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.util.tools.Utility;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Somewhat general-purpose SAX-style XML parser that uses reflection and calls
 * into your "schema" classes. For example, the element
 * <code>&lt;server-name&gt;</code> maps to the method
 * <code>server_name</code>. Note that the mapping is one-way, hyphens become
 * underscores, but then you don't really want to use underscores in XML tag
 * names anyway, do you? Also, all mixed content text (that is, text inside
 * elements) is ignored, so think attributes.
 */
public final class ReflectiveParser {

  private static final class Impl extends DefaultHandler {

    public void characters(char[] ch, int start, int length)
        throws SAXException {
      int lineNumber = fLocator.getLineNumber();

      // Get the active schema level.
      //
      Schema schemaLevel = getTopSchemaLevel();

      // Find the precomputed handler class info.
      //
      Class slc = schemaLevel.getClass();
      HandlerClassInfo classInfo = HandlerClassInfo.getClassInfo(slc);
      assert (classInfo != null); // would've thrown if unregistered
      HandlerMethod method = classInfo.getTextMethod();
      if (method == null) {
        // This is okay. Nothing special to do.
        //
        return;
      }

      // Call the handler.
      //
      try {
        final String text = String.valueOf(ch, start, length);
        method.invokeText(lineNumber, text, schemaLevel);
      } catch (UnableToCompleteException e) {
        throw new SAXException(e);
      }
    }

    public void endElement(String namespaceURI, String localName, String elem)
        throws SAXException {
      int lineNumber = fLocator.getLineNumber();

      // Get the active schema level.
      //
      Schema schemaLevel = popLevel();
      if (schemaLevel == null) {
        // This was an unexpected child, but we already informed the schema
        // about it during startElement(), so we can just return.
        //
        return;
      }

      // Find the precomputed handler class info.
      //
      Class slc = schemaLevel.getClass();
      HandlerClassInfo classInfo = HandlerClassInfo.getClassInfo(slc);
      assert (classInfo != null); // would've thrown if unregistered
      HandlerMethod method = classInfo.getEndMethod(elem);
      if (method == null) {
        // This is okay. Nothing special to do.
        //
        return;
      }

      Object[] args = getCurrentArgs();
      if (args != null) {
        // Call the handler using the same arguments we send to the "begin"
        // handler.
        //
        try {
          method.invokeEnd(lineNumber, elem, schemaLevel, args);
        } catch (UnableToCompleteException e) {
          throw new SAXException(e);
        }
      }
    }

    public void setDocumentLocator(Locator locator) {
      fLocator = locator;
    }

    public void startElement(String namespaceURI, String localName,
        String elemName, Attributes atts) throws SAXException {
      int lineNumber = fLocator.getLineNumber();

      // Get the active schema level.
      //
      Schema schemaLevel = getTopSchemaLevel();
      if (schemaLevel == null) {
        // This means that children should not appear at this level.
        //
        Schema nextToTop = getNextToTopSchemaLevel();

        // Push another null since this child shouldn't have children either.
        //
        setArgsAndPushLevel(null, null);

        // Inform the next-to-top schema level about this.
        //
        try {
          nextToTop.onUnexpectedChild(lineNumber, elemName);
        } catch (UnableToCompleteException e) {
          throw new SAXException(e);
        }

        return;
      }

      // Find the precomputed handler class info.
      //
      Class slc = schemaLevel.getClass();
      HandlerClassInfo classInfo = HandlerClassInfo.getClassInfo(slc);
      HandlerMethod method = classInfo.getStartMethod(elemName);

      if (method == null) {
        // This is not okay. The schema has to at least have a stub
        // to indicate that a particular tag is allowed.
        //
        try {
          schemaLevel.onUnexpectedElement(lineNumber, elemName);
        } catch (UnableToCompleteException e) {
          throw new SAXException(e);
        }

        // Since we don't know about this element, assume it should not have
        // children either.
        //
        setArgsAndPushLevel(null, null);

        return;
      }

      HandlerArgs args = method.createArgs(schemaLevel, lineNumber, elemName);

      // For each attribute found, try to match it up to a parameter.
      //
      for (int i = 0, n = atts.getLength(); i < n; ++i) {
        String attrName = atts.getQName(i);
        String attrValue = atts.getValue(i);

        if (!args.setArg(attrName, attrValue)) {
          // Inform the handler that the attribute was unknown.
          //
          try {
            schemaLevel.onUnexpectedAttribute(lineNumber, elemName, attrName,
              attrValue);
          } catch (UnableToCompleteException e) {
            throw new SAXException(e);
          }
        }
      }

      // Check for unset parameters.
      //
      int missingCount = 0;
      for (int i = 0, n = args.getArgCount(); i < n; ++i) {
        if (!args.isArgSet(i)) {
          // Inform the handler that the required attribute was not set.
          // It might throw, but it also might not.
          //
          try {
            schemaLevel.onMissingAttribute(lineNumber, elemName, args
              .getArgName(i));
          } catch (UnableToCompleteException e) {
            throw new SAXException(e);
          }

          ++missingCount;
        }
      }

      if (missingCount > 0) {
        // Do not invoke the handler.
        //

        // Assume that children shouldn't be recognized either since the
        // handler wasn't invoked.
        //
        setArgsAndPushLevel(null, null);

        return;
      }

      // Invoke the handler method, which will internally
      // convert all the args to their respective parameter types
      // (or warn if there is a problem doing so).
      //
      Object[] invokeArgs = new Object[method.getParamCount()];
      Schema childSchemaLevel;
      try {
        childSchemaLevel = method.invokeBegin(lineNumber, elemName,
          schemaLevel, args, invokeArgs);
      } catch (UnableToCompleteException e) {
        throw new SAXException(e);
      }

      // childSchemaLevel can be null and that's okay -- it means no children
      // are expected. Same for invokeArgs[0] -- it means that the "begin"
      // handler was not called, so neither will we call the "end" handler.
      //
      setArgsAndPushLevel(invokeArgs, childSchemaLevel);
    }

    private Schema getNextToTopSchemaLevel() {
      return (Schema) fSchemaLevels.get(fSchemaLevels.size() - 2);
    }

    private Schema getTopSchemaLevel() {
      return (Schema) fSchemaLevels.peek();
    }

    private Object[] getCurrentArgs() {
      return (Object[]) fArgStack.peek();
    }

    private void parse(TreeLogger logger, Schema topSchema, Reader reader)
        throws UnableToCompleteException {
      // Set up the parentmost schema which is used to find default converters
      // and handlers (but isn't actually on the schema stack.)
      //
      fDefaultSchema = new DefaultSchema(logger);

      // Tell this schema level about the default schema, which is initialized
      // with
      // converters for basic types.
      //
      topSchema.setParent(fDefaultSchema);

      // Make a slot for the document element's args.
      //
      fArgStack.push(null);

      // Push the first schema.
      //
      setArgsAndPushLevel(null, topSchema);

      Throwable caught = null;
      try {
        fReader = reader;
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        InputSource inputSource = new InputSource(fReader);
        XMLReader xmlReader = parser.getXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.parse(inputSource);
      } catch (SAXException e) {
        // If it's an exception wrapped in a SAXException, rip off the outer SAX
        // exception.
        //
        caught = e;

        Exception inner = e.getException();
        if (inner instanceof RuntimeException)
          throw (RuntimeException) inner;
        else if (inner != null)
          caught = inner;

      } catch (ParserConfigurationException e) {
        caught = e;
      } catch (IOException e) {
        caught = e;
      } finally {
        Utility.close(reader);
      }

      if (caught != null) {
        Messages.XML_PARSE_FAILED.log(logger, caught);
        throw new UnableToCompleteException();
      }
    }

    private Schema popLevel() {
      fArgStack.pop();
      fSchemaLevels.pop();
      return getTopSchemaLevel();
    }

    private void setArgsAndPushLevel(Object[] handlerArgs, Schema schemaLevel) {
      // Set the args on the current schema level.
      fArgStack.set(fArgStack.size() - 1, handlerArgs);
      // A slot for the args at the childrens' depth.
      fArgStack.push(null);
      if (!fSchemaLevels.isEmpty()) {
        // Tell this schema level about its parent.
        //
        Schema maybeParent = null;
        for (int i = fSchemaLevels.size() - 1; i >= 0; --i) {
          maybeParent = (Schema) fSchemaLevels.get(i);
          if (maybeParent != null)
            break;
        }
        if (maybeParent == null)
          throw new IllegalStateException("Cannot find any parent schema");
        if (schemaLevel != null)
          schemaLevel.setParent(maybeParent);
      }
      // The schema for children.
      fSchemaLevels.push(schemaLevel);
    }

    private Locator fLocator;
    private Reader fReader;
    private Stack fSchemaLevels = new Stack();
    private Stack fArgStack = new Stack();
    private Schema fDefaultSchema;
  }

  public static void parse(TreeLogger logger, Schema schema, Reader reader)
      throws UnableToCompleteException {

    // Register the schema level.
    //
    registerSchemaLevel(schema.getClass());

    // Do the parse.
    //
    Impl impl = new Impl();
    impl.parse(logger, schema, reader);
  }

  /**
   * Can safely register the same class recursively.
   */
  public static void registerSchemaLevel(Class schemaLevelClass) {
    HandlerClassInfo.registerClass(schemaLevelClass);

    // Try to register nested classes.
    //
    Class[] nested = schemaLevelClass.getDeclaredClasses();
    for (int i = 0, n = nested.length; i < n; ++i) {
      Class nestedClass = nested[i];
      if (Schema.class.isAssignableFrom(nestedClass))
        registerSchemaLevel(nestedClass);
    }
  }
}
