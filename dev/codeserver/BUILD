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
#   Open Source Code for Super Dev Mode.
#   http://go/superdevmode
#   (Within Google, depend on //java/com/google/gwt/dev/codeserver.)

# released under various licenses, but Google owns the copyright
licenses(["unencumbered"])

package_group(
    name = "friends",
    packages = [
        "//java/com/google/gwt/dev/codeserver",
    ],
)

package(
    default_visibility = [
        ":friends",
    ],
)

java_library(
    name = "codeserver",
    srcs = glob(["java/**/*.java"]),
    resources = glob([
        "java/**/*.html",
        "java/**/*.ico",
        "java/**/*.js",
    ]),
    deps = [
        # approved dependencies (for open source code)
        "//third_party/java_src/gwt:gwt-dev",
        # needed for guava
        "//third_party/java_src/gwt/svn/tools:compiler_deps",
        # no other dependencies allowed
        "//third_party/java/json",  # undeclared dependency of SourceMapConsumer. TODO: fix
    ],
)

# allow the top-level ant build to call us
filegroup(
    name = "ant",
    srcs = glob([
        "java/**/*.java",
        "java/**/*.html",
        "java/**/*.ico",
        "java/**/*.js",
    ]) + ["build.xml"],
    visibility = ["//third_party/java_src/gwt:__pkg__"],
)
