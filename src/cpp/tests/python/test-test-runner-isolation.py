# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.

"""Test the native test launcher with stub executables, without starting R.

Run with: python3 src/cpp/tests/python/test-test-runner-isolation.py
"""

import json
import os
from pathlib import Path
import shlex
import subprocess
import sys
import tempfile
import unittest


class TestRunnerIsolation(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="rstudio-runner-test-")
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        for name in ("bin", "core", "session", "conf", "tmp", "user-data", "user-config"):
            (self.root / name).mkdir()

        self.record = self.root / "session-environment.jsonl"
        self.sentinel = self.root / "user-data" / "unsaved-document"
        self.sentinel.write_text("keep this document")
        stub = self.root / "session-stub.py"
        stub.write_text(
            r'''
import json
import os
from pathlib import Path

root = Path(__file__).parent
data = Path(os.environ["RSTUDIO_DATA_HOME"])
config = Path(os.environ["RSTUDIO_CONFIG_HOME"])
state = data / "previous-test-scope"
with (root / "session-environment.jsonl").open("a") as output:
    output.write(json.dumps({
        "data": str(data),
        "config": str(config),
        "migrated": (data / "client-state").is_dir(),
        "config_exists": config.is_dir(),
        "previous_scope": state.exists(),
    }) + "\n")
data.mkdir(parents=True, exist_ok=True)
state.write_text("state from this test scope")
(root / "testthat-failures.log").write_text("0\n")
'''
        )

        source = Path(__file__).resolve().parents[2]
        windows = os.name == "nt"
        template_name = "rstudio-tests.bat.in" if windows else "rstudio-tests.in"
        template = (source / template_name).read_text()
        values = {
            "CMAKE_CURRENT_BINARY_DIR": self.root.as_posix(),
            "CMAKE_CURRENT_SOURCE_DIR": source.as_posix(),
            "LIBR_HOME": str(self.root),
            "LIBR_DOC_DIR": str(self.root),
            "LIBR_LIB_DIR": str(self.root),
            "LIBR_BIN_DIR": str(self.root / "bin"),
        }
        for name, value in values.items():
            template = template.replace("@" + name + "@", value)

        if windows:
            # Use a real child executable so cmd returns to the launcher, as
            # it does after rsession.exe; a batch stub would transfer control.
            template = template.replace(
                '"' + self.root.as_posix() + '/session/rsession.exe"',
                subprocess.list2cmdline([sys.executable, str(stub)]),
            )
            self.launcher = self.root / "rstudio-tests.bat"
        else:
            self.write_executable("bin/uname", "#!/bin/sh\necho Linux\n")
            self.write_executable("core/rstudio-core-tests", "#!/bin/sh\nexit 0\n")
            self.write_executable(
                "session/rsession",
                "#!/bin/sh\nexec " + shlex.join([sys.executable, str(stub)]) + ' "$@"\n',
            )
            self.launcher = self.root / "rstudio-tests"
        self.launcher.write_text(template)

        self.environment = dict(os.environ)
        self.environment.update(
            TMPDIR=str(self.root / "tmp"),
            TEMP=str(self.root / "tmp"),
            PATH=str(self.root / "bin") + os.pathsep + os.environ["PATH"],
            RSTUDIO_DATA_HOME=str(self.root / "user-data"),
            RSTUDIO_CONFIG_HOME=str(self.root / "user-config"),
            RSTUDIO_TESTS_UNDER_SCRIPT="1",
        )

    def write_executable(self, name, contents):
        path = self.root / name
        path.write_text(contents)
        path.chmod(0o755)

    def run_launcher(self, scope=None):
        command = ["cmd", "/c"] if os.name == "nt" else ["bash"]
        arguments = ["--scope", scope] if scope else []
        return subprocess.run(
            command + [str(self.launcher)] + arguments,
            cwd=self.root,
            env=self.environment,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=30,
        )

    def assert_isolated(self, expected_count):
        records = [json.loads(line) for line in self.record.read_text().splitlines()]
        self.assertEqual(len(records), expected_count)
        paths = [Path(record[key]).resolve() for record in records for key in ("data", "config")]
        self.assertEqual(len(set(paths)), 2 * expected_count)
        for path in paths:
            self.assertTrue(path.is_dir())
            self.assertIn((self.root / "tmp").resolve(), path.parents)
        for record in records:
            self.assertTrue(record["migrated"])
            self.assertTrue(record["config_exists"])
            self.assertFalse(record["previous_scope"])
        self.assertEqual(self.sentinel.read_text(), "keep this document")

    def test_each_session_scope_overrides_inherited_state(self):
        for scope in ("rsession", "r"):
            result = self.run_launcher(scope)
            self.assertEqual(result.returncode, 0, result.stdout)
        self.assert_isolated(2)

    def test_combined_scopes_use_distinct_state(self):
        # Unix runs all scopes when --scope is omitted; Windows also accepts
        # a comma-separated selection. Use each launcher's existing interface.
        result = self.run_launcher("rsession,r" if os.name == "nt" else None)
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assert_isolated(2)

    def test_repeated_invocations_use_distinct_state(self):
        for _ in range(2):
            result = self.run_launcher("rsession")
            self.assertEqual(result.returncode, 0, result.stdout)
        self.assert_isolated(2)

    def test_temporary_directory_failure_prevents_session_launch(self):
        if os.name == "nt":
            self.environment["TEMP"] = str(self.sentinel)
        else:
            self.write_executable("bin/mktemp", "#!/bin/sh\nexit 1\n")
        for scope in ("rsession", "r"):
            result = self.run_launcher(scope)
            self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertFalse(self.record.exists())
        self.assertEqual(self.sentinel.read_text(), "keep this document")

    @unittest.skipIf(os.name == "nt", "mkdir is a cmd builtin")
    def test_state_initialization_failure_prevents_session_launch(self):
        self.write_executable("bin/mkdir", "#!/bin/sh\nexit 1\n")
        for scope in ("rsession", "r"):
            result = self.run_launcher(scope)
            self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertFalse(self.record.exists())
        self.assertEqual(self.sentinel.read_text(), "keep this document")


if __name__ == "__main__":
    unittest.main()
