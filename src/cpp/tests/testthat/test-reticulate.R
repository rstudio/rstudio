#
# test-reticulate.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("reticulate")

test_that("RETICULATE_PYTHON environment variable is respected", {
   # save old value and set a dummy value
   oldPython <- Sys.getenv("RETICULATE_PYTHON")
   Sys.setenv(RETICULATE_PYTHON = "/opt/testthat/python")
   on.exit({
      # restore old value
      Sys.setenv(RETICULATE_PYTHON = oldPython)
   }, add = TRUE)

   # perform autodiscovery
   python <- .rs.inferReticulatePython()

   # we expect that since we set a custom value it'll be reflected
   expect_equal(python, Sys.getenv("RETICULATE_PYTHON"))
})

test_that("Python help topics accept only plain dotted names", {

   # legitimate help topics resolve to valid qualified names
   expect_true(.rs.python.isHelpTopicValid("numpy"))
   expect_true(.rs.python.isHelpTopicValid("os.path"))
   expect_true(.rs.python.isHelpTopicValid("pandas.DataFrame"))
   expect_true(.rs.python.isHelpTopicValid("_private.member"))

})

test_that("Python help topics reject code injection payloads", {

   # these would otherwise be evaluated as Python expressions
   expect_false(.rs.python.isHelpTopicValid("open('pwned.txt','w').write('RCE')"))
   expect_false(.rs.python.isHelpTopicValid("__import__('os').system('id')"))
   expect_false(.rs.python.isHelpTopicValid("os.system('id')"))
   expect_false(.rs.python.isHelpTopicValid("numpy.array(1)"))
   expect_false(.rs.python.isHelpTopicValid("a;b"))
   expect_false(.rs.python.isHelpTopicValid("a b"))
   expect_false(.rs.python.isHelpTopicValid("1abc"))
   expect_false(.rs.python.isHelpTopicValid(".leadingdot"))
   expect_false(.rs.python.isHelpTopicValid(""))

})

# initializing Python with no interpreter configured can make reticulate
# provision one over the network, so only run when one was requested
skipIfPythonUnavailable <- function()
{
   skip_if_not_installed("reticulate")
   skip_if(!nzchar(Sys.getenv("RETICULATE_PYTHON")), "RETICULATE_PYTHON is not set")

   available <- tryCatch(
      reticulate::py_available(initialize = TRUE),
      error = function(e) FALSE
   )

   skip_if_not(available, "Python is not available")
}

test_that("Python function arguments are discovered with inspect.signature()", {

   skipIfPythonUnavailable()

   reticulate::py_run_string('
def _rs_test_function(a, b: int, c=1, *args, d, e=2, **kwargs):
    pass

class _rs_test_class:
    def __init__(self, x, y=1, *, z=2):
        pass
')
   on.exit(reticulate::py_run_string("del _rs_test_function, _rs_test_class"), add = TRUE)

   # variadic parameters can't be supplied as 'name=value'
   object <- reticulate::py_eval("_rs_test_function", convert = FALSE)
   expect_equal(.rs.python.getFunctionArguments(object), c("a", "b", "c", "d", "e"))

   # classes report their constructor's arguments, without 'self'
   object <- reticulate::py_eval("_rs_test_class", convert = FALSE)
   expect_equal(.rs.python.getFunctionArguments(object), c("x", "y", "z"))

})

test_that("Python positional-only arguments are not offered", {

   skipIfPythonUnavailable()
   skip_if(reticulate::py_version() < "3.8", "positional-only syntax requires Python 3.8")

   reticulate::py_run_string('
def _rs_test_function(a, /, b):
    pass
')
   on.exit(reticulate::py_run_string("del _rs_test_function"), add = TRUE)

   object <- reticulate::py_eval("_rs_test_function", convert = FALSE)
   expect_equal(.rs.python.getFunctionArguments(object), "b")

})

test_that("Python function arguments fall back to the docstring for opaque signatures", {

   skipIfPythonUnavailable()

   reticulate::py_run_string('
def _rs_test_function(*args, **kwargs):
    """_rs_test_function(x, y=1)"""
    pass
')
   on.exit(reticulate::py_run_string("del _rs_test_function"), add = TRUE)

   object <- reticulate::py_eval("_rs_test_function", convert = FALSE)
   expect_equal(.rs.python.getFunctionArguments(object), c("x", "y"))

})

test_that("Python parameter help includes each parameter's description", {

   skipIfPythonUnavailable()

   # the docstring ends with the last parameter's description
   reticulate::py_run_string('
def _rs_test_function(alpha, *, beta=1):
    """Test function.

    Parameters
    ----------
    alpha : int
        The first parameter.
    beta : int
        The second parameter."""
    pass
')
   on.exit(reticulate::py_run_string("del _rs_test_function"), add = TRUE)

   help <- .rs.python.getParameterHelp("_rs_test_function")
   expect_equal(help$args, c("alpha", "beta"))
   expect_equal(
      help$arg_descriptions,
      c("int\nThe first parameter.", "int\nThe second parameter.")
   )

})
