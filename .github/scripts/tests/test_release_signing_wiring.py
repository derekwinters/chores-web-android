"""Wiring tests for stable release signing (issue #52, ADR-0008).

The gate in `verify_release_signature.py` only protects a release if the workflows run it, and
the release key is only safe if no pull request can reach it. These tests read the workflow
files and `app/build.gradle.kts` as text (standard library only, no YAML parser on the runner)
and pin:

- no `pull_request`-triggered workflow references the keystore secrets;
- every release-producing job checks the secrets first, decodes the keystore, builds, removes
  the keystore in an `if: always()` step, and runs the gate (from a `main` checkout) before it
  uploads or attaches anything;
- `release-candidate.yml` runs on `workflow_run` / `workflow_dispatch`, never `pull_request`;
- `pr.yml` runs these tests;
- every `android-actions/setup-android` step passes an explicit `packages:` list that matches
  `compileSdk`;
- the Gradle release build type signs from environment variables only, with v1/v2/v3 stated,
  and never falls back to the debug key.
"""

import os
import re
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.abspath(__file__)))))
WORKFLOWS = os.path.join(ROOT, ".github", "workflows")
APP_BUILD = os.path.join(ROOT, "app", "build.gradle.kts")

SECRETS = (
    "ANDROID_KEYSTORE_BASE64",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEYSTORE_SHA256",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_ALIAS_PASSWORD",
)
KEYSTORE_NAME = re.compile(r"ANDROID_KEYSTORE_\w*|ANDROID_KEY_ALIAS\w*")

GATE_CHECKOUT_PATH = "verification-gate"
GATE_SCRIPT = GATE_CHECKOUT_PATH + "/.github/scripts/verify_release_signature.py"

# (workflow file, job id, name of the step that publishes the APK)
RELEASE_JOBS = (
    ("release-please.yml", "build-and-attach", "Attach APK to GitHub Release"),
    ("release.yml", "build-and-attach-release", "Attach APK to GitHub Release"),
    ("release-candidate.yml", "build-release-candidate", "Upload release candidate artifact"),
)


def _read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def _indent(line):
    return len(line) - len(line.lstrip())


def _workflow_files():
    return sorted(
        name for name in os.listdir(WORKFLOWS) if name.endswith((".yml", ".yaml")))


def _on_block(text):
    """The text of the top-level `on:` block."""
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if re.match(r"^on:\s*$", line):
            body = []
            for following in lines[index + 1:]:
                if following.strip() and _indent(following) == 0:
                    break
                body.append(following)
            return "\n".join(body)
    raise AssertionError("no top-level `on:` block")


def _is_pull_request_triggered(text):
    return re.search(r"^\s+pull_request(_target)?\s*:", _on_block(text), re.MULTILINE) is not None


def _job_body(text, job_id):
    lines = text.splitlines()
    in_jobs = False
    for index, line in enumerate(lines):
        if line.rstrip() == "jobs:":
            in_jobs = True
            continue
        if in_jobs and line.strip() == job_id + ":" and _indent(line) == 2:
            body = []
            for following in lines[index + 1:]:
                if following.strip() and _indent(following) <= 2:
                    break
                body.append(following)
            return "\n".join(body)
    raise AssertionError("no job {0!r}".format(job_id))


def _steps(job_body):
    """The job's steps as a list of text blocks, in order."""
    lines = job_body.splitlines()
    steps = []
    current = None
    step_indent = None
    for line in lines:
        stripped = line.strip()
        if stripped.startswith("- ") and (step_indent is None or _indent(line) == step_indent):
            if step_indent is None:
                step_indent = _indent(line)
            current = [line]
            steps.append(current)
        elif current is not None:
            current.append(line)
    return ["\n".join(step) for step in steps]


def _step_named(steps, name):
    for index, step in enumerate(steps):
        if re.search(r"name:\s*{0}\s*$".format(re.escape(name)), step.splitlines()[0]):
            return index, step
    raise AssertionError("no step named {0!r}".format(name))


class PullRequestNeverReachesTheKeyTests(unittest.TestCase):

    def test_no_pull_request_triggered_workflow_references_the_keystore(self):
        checked = 0
        for name in _workflow_files():
            text = _read(os.path.join(WORKFLOWS, name))
            if not _is_pull_request_triggered(text):
                continue
            checked += 1
            with self.subTest(workflow=name):
                self.assertEqual(
                    KEYSTORE_NAME.findall(text), [],
                    "{0} is pull_request-triggered and must never reference the release "
                    "keystore".format(name))
        self.assertGreater(checked, 0, "expected pr.yml to be pull_request-triggered")

    def test_the_release_candidate_is_not_pull_request_triggered(self):
        text = _read(os.path.join(WORKFLOWS, "release-candidate.yml"))
        self.assertFalse(_is_pull_request_triggered(text))

    def test_the_release_candidate_runs_after_pr_checks_and_on_dispatch(self):
        text = _read(os.path.join(WORKFLOWS, "release-candidate.yml"))
        on_block = _on_block(text)
        self.assertRegex(on_block, r"(?m)^\s+workflow_dispatch\s*:")
        self.assertRegex(on_block, r"(?m)^\s+workflow_run\s*:")
        workflows = re.search(r"workflows:\s*\[([^\]]*)\]", on_block)
        self.assertIsNotNone(workflows, "workflow_run needs a `workflows: [...]` list")
        pr_name = re.search(
            r"(?m)^name:\s*(.+?)\s*$", _read(os.path.join(WORKFLOWS, "pr.yml"))).group(1)
        self.assertEqual(
            [w.strip().strip("\"'") for w in workflows.group(1).split(",")], [pr_name],
            "workflow_run matches by the other workflow's `name:`, not its filename")
        self.assertRegex(on_block, r"types:\s*\[\s*completed\s*\]")

    def test_the_release_candidate_only_builds_successful_same_repo_release_branches(self):
        job = _job_body(
            _read(os.path.join(WORKFLOWS, "release-candidate.yml")), "build-release-candidate")
        condition = job.split("steps:")[0]
        self.assertIn("github.event_name == 'workflow_dispatch'", condition)
        self.assertIn("github.event.workflow_run.conclusion == 'success'", condition)
        self.assertIn(
            "startsWith(github.event.workflow_run.head_branch, 'release-please--')", condition)
        # A fork can name its branch release-please--anything; its code must never be built
        # with the release key.
        self.assertIn(
            "github.event.workflow_run.head_repository.full_name == github.repository",
            condition)
        self.assertIn("github.event.workflow_run.head_sha", job)


class ReleaseJobTests(unittest.TestCase):

    def _job(self, workflow, job_id):
        return _job_body(_read(os.path.join(WORKFLOWS, workflow)), job_id)

    def test_each_release_job_references_every_secret(self):
        for workflow, job_id, _ in RELEASE_JOBS:
            job = self._job(workflow, job_id)
            for secret in SECRETS:
                with self.subTest(workflow=workflow, secret=secret):
                    self.assertIn("secrets.{0}".format(secret), job)

    def test_each_release_job_checks_the_secrets_first(self):
        for workflow, job_id, _ in RELEASE_JOBS:
            with self.subTest(workflow=workflow):
                steps = _steps(self._job(workflow, job_id))
                index, step = _step_named(steps, "Check the release signing secrets")
                self.assertEqual(index, 0, "the secret check must be the job's first step")
                for secret in SECRETS:
                    self.assertIn("secrets.{0}".format(secret), step)
                self.assertIn("exit 1", step)

    def test_each_release_job_signs_gates_then_publishes_in_order(self):
        for workflow, job_id, publish_step in RELEASE_JOBS:
            with self.subTest(workflow=workflow):
                steps = _steps(self._job(workflow, job_id))
                decode, decode_step = _step_named(steps, "Decode the release keystore")
                build, build_step = _step_named(steps, "Build the signed release APK")
                remove, remove_step = _step_named(steps, "Remove the decoded keystore")
                verify, _ = _step_named(steps, "Verify the release signature")
                publish, _ = _step_named(steps, publish_step)
                self.assertLess(decode, build)
                self.assertLess(build, remove)
                self.assertLess(build, verify)
                self.assertLess(verify, publish, "the gate must run before upload/attach")
                self.assertIn("$RUNNER_TEMP", decode_step)
                self.assertIn("ANDROID_KEYSTORE_PATH", decode_step)
                self.assertIn("assembleRelease", build_step)
                self.assertRegex(remove_step, r"if:\s*always\(\)")

    def test_each_release_job_runs_the_gate_from_a_main_checkout(self):
        for workflow, job_id, _ in RELEASE_JOBS:
            with self.subTest(workflow=workflow):
                steps = _steps(self._job(workflow, job_id))
                _, checkout = _step_named(steps, "Check out main for the release-signature gate")
                self.assertRegex(checkout, r"uses:\s*actions/checkout@[0-9a-f]{40}")
                self.assertRegex(checkout, r"(?m)^\s+ref:\s*main\s*$")
                self.assertRegex(
                    checkout, r"(?m)^\s+path:\s*{0}\s*$".format(GATE_CHECKOUT_PATH))
                _, verify = _step_named(steps, "Verify the release signature")
                self.assertIn(GATE_SCRIPT, verify)
                self.assertIn("ANDROID_KEYSTORE_SHA256: ${{ secrets.ANDROID_KEYSTORE_SHA256 }}",
                              verify)
                self.assertNotRegex(
                    verify, r"(?<![\w/-])\.github/scripts/verify_release_signature\.py",
                    "the gate must not run from the checkout being built")


class PullRequestWorkflowTests(unittest.TestCase):

    def test_pr_checks_runs_the_gate_and_wiring_tests(self):
        steps = _steps(_job_body(_read(os.path.join(WORKFLOWS, "pr.yml")), "build-and-test"))
        _, step = _step_named(steps, "Run the release-signature gate's unit tests")
        self.assertIn("python3 -m unittest discover -s .github/scripts/tests", step)
        self.assertNotIn("uses:", step)


def _platform_package(compile_sdk):
    """The sdkmanager package id for a compileSdk platform.

    From API 37 the platform package id carries a minor version (`platforms;android-37.0`);
    `platforms;android-37` fails with "Failed to find package".
    """
    level = int(compile_sdk)
    return "platforms;android-{0}{1}".format(level, ".0" if level >= 37 else "")


class SetupAndroidPackagesTests(unittest.TestCase):

    def test_every_setup_android_step_passes_explicit_packages(self):
        compile_sdk = re.search(r"compileSdk\s*=\s*(\d+)", _read(APP_BUILD)).group(1)
        found = 0
        for name in _workflow_files():
            text = _read(os.path.join(WORKFLOWS, name))
            for match in re.finditer(r"uses:\s*android-actions/setup-android@", text):
                found += 1
                block = text[match.start():].split("\n      - ")[0]
                with self.subTest(workflow=name):
                    packages = re.search(r"packages:\s*['\"]([^'\"]+)['\"]", block)
                    self.assertIsNotNone(packages, "setup-android needs an explicit packages:")
                    listed = packages.group(1).split()
                    self.assertIn("platform-tools", listed)
                    self.assertIn(_platform_package(compile_sdk), listed)
                    self.assertTrue(
                        any(re.fullmatch(r"build-tools;\d+\.\d+\.\d+", p) for p in listed),
                        "build-tools (which provides apksigner) must be listed")
        self.assertGreater(found, 0)


class GradleReleaseSigningTests(unittest.TestCase):

    def setUp(self):
        self.text = _read(APP_BUILD)

    def test_signing_inputs_come_from_the_environment(self):
        for name in ("ANDROID_KEYSTORE_PATH", "ANDROID_KEYSTORE_PASSWORD",
                     "ANDROID_KEY_ALIAS", "ANDROID_KEY_ALIAS_PASSWORD"):
            with self.subTest(env=name):
                self.assertIn('System.getenv("{0}")'.format(name), self.text)

    def test_release_never_falls_back_to_the_debug_key(self):
        self.assertNotIn('signingConfigs.getByName("debug")', self.text)
        self.assertNotRegex(self.text, r"signingConfigs\.debug\b")
        self.assertRegex(
            self.text,
            r"if \(hasReleaseSigningConfig\) \{\s*signingConfig = "
            r"signingConfigs\.getByName\(\"release\"\)\s*\}")

    def test_all_three_signature_schemes_are_stated(self):
        for scheme in ("enableV1Signing", "enableV2Signing", "enableV3Signing"):
            with self.subTest(scheme=scheme):
                self.assertRegex(self.text, r"{0}\s*=\s*true".format(scheme))


if __name__ == "__main__":
    unittest.main()
