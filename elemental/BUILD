# Copyright 2012 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

# Description:
#   Elemental bindings for GWT # released under various licenses, derived from Dart project
licenses(["notice"])  # BSD (with Chrome/WebKit IDL files under LGPL)

package(default_visibility = [
    "//visibility:public",
])

ELEMENTAL_EXTRA_XML = """
  <inherits name='com.google.gwt.resources.Resources' />
  <set-configuration-property name='user.agent.runtimeWarning' value='false' />
  <set-property name='user.agent' value='safari' />
  <source path='client' />
  <source path='js' />
  <source path='canvas'/>
  <source path='css'/>
  <source path='dom'/>
  <source path='events'/>
  <source path='html'/>
  <source path='json'/>
  <source path='ranges'/>
  <source path='stylesheets'/>
  <source path='svg'/>
  <source path='traversal'/>
  <source path='xpath'/>
  <source path='xml'/>
  <source path='util'/>
  <super-source path='super'/>
"""

genrule(
    name = "elemental_extra_xml",
    outs = ["ElementalExtra.gwt.xml"],
    cmd = "echo \"" + ELEMENTAL_EXTRA_XML + "\" > $@",
)

gwt_module(
    name = "elemental",
    srcs = [":buildsrcjar"] + glob(
        ["src/**/*.java"],
        exclude = ["**/super/**"],
    ),
    base_inherits = ["com.google.gwt.core.Core"],
    extra_module_contents = [":ElementalExtra.gwt.xml"],
    resources = [
        "src/elemental/super/elemental/json/Json.java",
        "src/elemental/super/elemental/json/impl/JreJsonValue.java",
    ],
    deps = ["//third_party/java/gwt"],
)

genrule(
    name = "buildsrcjar",
    srcs = glob(["idl/**/*"]),
    outs = ["elementaldom.srcjar"],
    cmd = " && ".join([
        "mkdir -p $@.tmp",
        "export CC=$(CC)",
        "export JAR=$(JAR)",
        "idl/buildsrcjar $@.tmp $@",
    ]),
    tools = [
        "//third_party/crosstool",
        "//tools/defaults:jdk",
        "//tools/defaults:python_runtime",
    ],
)
