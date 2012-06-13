#!/usr/bin/python2.4
#
# Copyright 2008 Google Inc. All Rights Reserved.

"""Generates StyleBase.java based on list of styles in styles.txt.

Usage: $0
(No args, just run it, with styles.txt in the same directory as the script)
"""

__author__ = 'danilatos@google.com (Daniel Danilatos)'

import os
import re
import string
import sys

CONST_PREFIX = '  public static final String '

def main(argv):
  if len(argv) != 4:
    print 'Usage: input_file interface.java.snippet implementation.java.snippet'
    sys.exit(-1)
  input_file = argv[1]
  output_intf = argv[2]
  output_impl = argv[3]

  output_intf_stream = open(output_intf, 'w')
  output_impl_stream = open(output_impl, 'w')
  input_stream = open(input_file, 'r')
  GenerateStyleBase(input_stream, output_intf_stream, output_impl_stream)


def GenerateStyleBase(input_stream, output_intf_stream, output_impl_stream):
  """Generate the StyleBase.java file.

  Args:
    input_stream: A stream containing the configuration input (usually
        styles.txt)
    output_stream: A stream to write the output to
  """

  wintf = output_intf_stream.write
  wimpl = output_impl_stream.write

  # Write the generic setProperty() that's not in the IDL :-/
  # Write Unit interface into the interface snippet
  wintf("""
package $PACKAGE;
import elemental.dom.*;
import elemental.html.*;

/**
  * $CLASS_JAVADOC
  */
public interface $ID$EXTENDS {
$!MEMBERS
""")

  wimpl("""package $PACKAGE;
$IMPORTS
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class $ID$EXTENDS $IMPLEMENTS {
  protected $ID() {}
$!MEMBERS
""")

  wintf('public interface Unit {\n')
  wintf('  public static final String PX = "px";\n')
  wintf('  public static final String PCT = "%";\n')
  wintf('  public static final String EM = "em";\n')
  wintf('  public static final String EX = "ex";\n')
  wintf('  public static final String PT = "pt";\n')
  wintf('  public static final String PC = "pc";\n')
  wintf('  public static final String IN = "in";\n')
  wintf('  public static final String CM = "cm";\n')
  wintf('  public static final String MM = "mm";\n')
  wintf('}\n\n')


  for line in input_stream:
    line = re.sub('#.*$', '', line).strip()
    if not line or line.startswith("//"): continue

    # prop is the css property name
    # value_type is the actual high-level type of the property
    # output_mode is whether it is just a simple constant, or a dimension type,
    #   or an enum, etc.
    # params are additional line parameters, currently used for enum values
    (prop, output_mode, value_type, params) = ParseLine(line)

    method_suffix = PropToCapsCase(prop)
    js_prop = method_suffix[0].lower() + method_suffix[1:]  # Camel case
    if js_prop == 'float':
      js_prop = "this['float']"
    else:
      js_prop = "this." + js_prop

    if output_mode == 'enum':
      wintf('\npublic interface %s {\n' % method_suffix)
      for p in params:
        wintf(CONST_PREFIX + PropToConstant(p) + ' = "' + p + '";\n')
      wintf('}\n\n')

    # getter
    wintf('%s get%s();\n' % (value_type, method_suffix))
    wimpl('public final native %s get%s() /*-{ return %s; }-*/;\n' % (value_type, method_suffix, js_prop))

    # setter(s) & clearer(s)
    wintf('void set%s(%s value);\n' % (method_suffix, value_type))
    wintf('void clear%s();\n' % method_suffix)
    wimpl('public final native void set%s(%s value) /*-{ %s = value; }-*/;\n' % (method_suffix, value_type, js_prop))
    wimpl('public final native void clear%s() /*-{ %s = ""; }-*/;\n' % (method_suffix, js_prop))
    if output_mode == 'dim':
      wintf('void set%s(double value, String unit);\n' % (method_suffix))
      wimpl('public final native void set%s(double value, String unit) /*-{ %s = value + unit; }-*/;\n' % (method_suffix, js_prop))
 
  wintf('}')
  wimpl('}')
  output_intf_stream.close()
  output_impl_stream.close()


def ParseLine(line):
  """Parses a line of the input file into useful parameters."""
  bits = re.compile(r'\s+').split(line)
  if len(bits) == 1:
    output_mode = 'simple'
    value_type = 'String'
  elif bits[1] == 'enum':
    output_mode = 'enum'
    value_type = 'String' # PropToCapsCase(bits[0])
  elif bits[1] == 'dim':
    output_mode = 'dim'
    value_type = 'String' # None  # lots of different types for dim properties
  else:
    output_mode = 'simple'
    value_type = bits[1]

  css_prop_name = bits[0]
  additional_params = bits[2:]

  return (css_prop_name, output_mode, value_type, additional_params)


def PropToCapsCase(css_prop):
  """Converts abc-def to AbcDef."""
  return re.sub(' ', '', string.capwords(re.sub('-', ' ', css_prop)))


def PropToConstant(css_prop):
  """Converts abc-def to ABC_DEF."""
  return css_prop.upper().replace('-', '_')


if __name__ == '__main__':
  sys.exit(main(sys.argv))
