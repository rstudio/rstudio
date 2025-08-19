
# Description
This folder contains files related to unit testing the core library with gtest and gmock.

# Unit Testing Framework Changes
There is an ongoing effort to convert from using TestThat and Catch2 to using gtest and gmock.
As we convert, unit test source files will no longer appear alongside regular source files, but rather be contained within a seperate `unittests` directory.

This is to cut down on clutter in the source file directories. As we expand to using mocks, fakes, data driven tests, etc. There will no longer necessarily be a one to one
correlation between a regular source file and one test file source file. Multiple files may be required to unit test one class, module, or "unit" of code.



