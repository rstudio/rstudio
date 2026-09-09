# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.

"""Exercise the configured Unix test launcher without starting RStudio or R.

Run with: python3 src/cpp/tests/python/test-test-runner-isolation.py
"""

import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import unittest


@unittest.skipIf(os.name == "nt", "Exercises the Unix test launcher")
class TestRunnerIsolation(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="rstudio-runner-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        for name in ("bin", "core", "session", "conf", "tmp", "user-data", "user-config"):
            (self.root / name).mkdir()

        self.record = self.root / "session-environment"
        self.sentinel = self.root / "user-data" / "unsaved-document"
        self.sentinel.write_text("keep this document")
        self.write_executable("bin/uname", "#!/bin/sh\necho Linux\n")
        # Select the ordinary executable layout, rather than Xcode's Debug/.
        self.write_executable("core/rstudio-core-tests", "#!/bin/sh\nexit 0\n")
        self.write_executable(
            "session/rsession",
            "#!/bin/sh\n"
            "printf '%s\\n' \"$RSTUDIO_DATA_HOME\" \"$RSTUDIO_CONFIG_HOME\" >> "
            + shlex.quote(str(self.record))
            + "\nprintf '0\\n' > "
            + shlex.quote(str(self.root / "testthat-failures.log"))
            + "\n",
        )

        source = Path(__file__).resolve().parents[2]
        template = (source / "rstudio-tests.in").read_text()
        values = {
            "CMAKE_CURRENT_BINARY_DIR": str(self.root),
            "CMAKE_CURRENT_SOURCE_DIR": str(source),
            "LIBR_HOME": str(self.root),
            "LIBR_DOC_DIR": str(self.root),
            "LIBR_LIB_DIR": str(self.root),
            "LIBR_BIN_DIR": str(self.root / "bin"),
        }
        for name, value in values.items():
            template = template.replace("@" + name + "@", value)
        self.write_executable("rstudio-tests", template)

        self.environment = dict(os.environ)
        self.environment.update(
            TMPDIR=str(self.root / "tmp"),
            PATH=str(self.root / "bin") + os.pathsep + os.environ["PATH"],
            RSTUDIO_DATA_HOME=str(self.root / "user-data"),
            RSTUDIO_CONFIG_HOME=str(self.root / "user-config"),
            RSTUDIO_TESTS_UNDER_SCRIPT="1",
        )

    def write_executable(self, name, contents):
        path = self.root / name
        path.write_text(contents)
        path.chmod(0o755)

    def run_launcher(self, scope):
        return subprocess.run(
            ["bash", str(self.root / "rstudio-tests"), "--scope", scope],
            cwd=self.root,
            env=self.environment,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=30,
        )

    def test_cpp_and_r_sessions_each_receive_fresh_state(self):
        for scope in ("rsession", "r"):
            result = self.run_launcher(scope)
            self.assertEqual(result.returncode, 0, result.stdout)
        paths = self.record.read_text().splitlines()
        self.assertEqual(len(paths), 4)
        self.assertEqual(len(set(paths)), 4)
        for path in paths:
            self.assertTrue(Path(path).parent.is_dir())
            self.assertTrue(Path(path).is_relative_to(self.root / "tmp"))
        self.assertEqual(self.sentinel.read_text(), "keep this document")

    def test_temporary_directory_failure_prevents_session_launch(self):
        self.write_executable("bin/mktemp", "#!/bin/sh\nexit 1\n")
        result = self.run_launcher("rsession")
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertFalse(self.record.exists())
        self.assertEqual(self.sentinel.read_text(), "keep this document")


if __name__ == "__main__":
    unittest.main()
