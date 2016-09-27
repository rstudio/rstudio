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

load("//third_party/java_src/gwt:build-macros.bzl", "AugmentedJar")

# The codeserver jar, built the open source way.
# For GWT internal use only.
# It must be jarjar-ed and dependencies stripped before exposing to google3.
java_library(
    name = "codeserver-bare",
    srcs = glob(["java/**/*.java"]),
    resources = glob([
        "java/**/*.html",
        "java/**/*.ico",
        "java/**/*.js",
    ]),
    deps = [
        # dependencies for open source code (should match ant)
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-bare",
        "//third_party/java_src/gwt/svn/tools:dev_deps",
        "//third_party/java_src/gwt/svn/tools:servlet_api_3",
    ],
)

java_library(
    name = "testlib",
    testonly = 1,
    srcs = glob(["javatests/**/*.java"]),
    deps = [
        ":codeserver-bare",
        "//java/com/google/common/collect",
        "//third_party/java/junit",
        "//third_party/java_src/gwt:gwt-testing",
        "//third_party/java_src/gwt/svn/tools:dev_deps",
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-only",
        "//third_party/java_src/gwt/svn/trunk/user:user-test-code",
    ],
)

test_suite(
    name = "tests",
    tests = [
        ":RecompilerTest",
        ":SourceHandlerTest",
    ],
)

java_test(
    name = "SourceHandlerTest",
    test_class = "com.google.gwt.dev.codeserver.SourceHandlerTest",
    runtime_deps = [
        ":testlib",
    ],
)

java_test(
    name = "RecompilerTest",
    test_class = "com.google.gwt.dev.codeserver.RecompilerTest",
    runtime_deps = [
        ":testlib",
    ],
)

# Repackaged codeserver for google3.
AugmentedJar(
    name = "codeserver",
    srcs = [],
    added_jars = [
        ":libcodeserver-bare.jar",
    ],
    constraints = [],
    dojarjar = 1,
    export_deps = [
        # Dependencies for google3 (after jarjar).
        "//third_party/java_src/gwt:gwt-dev",
        "//third_party/java_src/gwt:gwt-dev-private",
        # The codeserver needs servlet_api_3. Be careful about adding dependencies.
        "//third_party/java_src/gwt/svn/tools:servlet_api_3",
    ],
    visibility = ["//java/com/google/gwt/dev/codeserver:__pkg__"],
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
