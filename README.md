# Codacy CPPCheck

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3bd4fdde0b9b43dd9aead3130d370c5d)](https://www.codacy.com/gh/codacy/codacy-cppcheck?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-cppcheck&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/codacy/codacy-cppcheck.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-cppcheck)

This is the docker engine we use at Codacy to have [CPPCheck](http://cppcheck.sourceforge.net) support.
You can also create a docker to integrate the tool and language of your choice!
Check the **Docs** section for more information.

## Usage

You can create the docker by doing:

```bash
docker build --no-cache -f Dockerfile.base -t codacy-cppcheck-base:latest .
docker build -t codacy-cppcheck .
```

The docker is ran with the following command:

```bash
docker run --rm -v $srcDir:/src -v $configFile:/.codacyrc codacy-cppcheck
```

### Generate Docs

1. Update the `ARG toolVersion` in `Dockerfile`
2. Run the documentation generator:

```bash
docker build --no-cache -f Dockerfile.base -t codacy-cppcheck-base:latest .
sbt doc-generator/run
```

## Docs

[Tool Developer Guide](https://support.codacy.com/hc/en-us/articles/207994725-Tool-Developer-Guide)

[Tool Developer Guide - Using Scala](https://support.codacy.com/hc/en-us/articles/207280379-Tool-Developer-Guide-Using-Scala)

## Test

For a faster development loop you can create a Docker image based on the JVM instead of creating a native-image:

```bash
sbt stage
docker build --no-cache -f Dockerfile.base -t codacy-cppcheck-base:latest .
docker build -t codacy-cppcheck --target dev .
```

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped [Cppcheck](http://cppcheck.sourceforge.net) version, but also base image / orb / sbt-plugin bumps. Follow it top to bottom; it tells you what to change, how to regenerate derived files, how to test locally, and how to interpret CI so you can iterate on failures without guessing.

### 1. What this repository is

This is a **Codacy engine**: a thin Scala wrapper (`src/main/scala/codacy/Engine.scala` and `src/main/scala/codacy/cppcheck/CPPCheck.scala`, built on `codacy-engine-scala-seed`) that packages [Cppcheck](https://github.com/danmar/cppcheck) as a Docker image Codacy's platform can run against a customer's C/C++ source code. Cppcheck itself is **not** a Scala/JVM dependency — it is a native C++ tool compiled from source inside `Dockerfile.base` via `apk`/`make`. The Scala code only wraps its CLI (builds arguments, parses XML output, applies MISRA/addon rules).

`docs/` is machine-consumed configuration, not just documentation:

- `docs/patterns.json` — the full list of Cppcheck rules ("patterns") Codacy knows about (including MISRA C:2012 and addon patterns), their category/level, and which are enabled by default. Generated file, do not hand-edit.
- `docs/description/description.json` (+ per-file docs under `docs/description/`) — human-readable titles/descriptions per pattern, used in the Codacy UI. Generated file, do not hand-edit.
- `docs/tests/*` and `docs/multiple-tests/*` — fixtures used by `codacy-plugins-test` to validate the engine actually produces the results it claims to for real code samples.
- `docs/tool-description.md` — short blurb about the tool, hand-maintained.

Both generated artifacts above come from **`DocGenerator`** (`doc-generator/src/main/scala/codacy/cppcheck/DocGenerator.scala`, a separate sbt sub-project). It runs `docker run -i --entrypoint cppcheck codacy-cppcheck-base:latest --errorlist` against the just-built base image to enumerate all rules, merges in hand-maintained MISRA titles (in the same file) and addon patterns/descriptions from `addons/patterns.json` / `addons/description/description.json`, and writes `docs/patterns.json` + `docs/description/description.json`. This means the generator needs **Docker** (the `codacy-cppcheck-base:latest` image must already be built) — no network/git access beyond that.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `Dockerfile.base` → `ARG toolVersion` | Which Cppcheck release is downloaded (`https://github.com/danmar/cppcheck/archive/<version>.tar.gz`), compiled, and installed; also the value written into `docs/patterns.json`'s `"version"` field via `build.sbt`'s `cppcheckVersion` | Bump to the target version. Confirm a matching tag/release exists in [danmar/cppcheck](https://github.com/danmar/cppcheck/tags). |
| `Dockerfile.base` → `FROM alpine:<version>` | Base OS for the compiled Cppcheck binary and runtime packages (`bash python3 pcre z3`) | Bump opportunistically alongside a Cppcheck bump if the prior version bumps did (see history below), or when asked explicitly. |
| `build.sbt` → `com.codacy %% codacy-engine-scala-seed` | Codacy's engine SDK/base library | Check for newer versions if asked to update it; not tied to Cppcheck bumps. |
| `build.sbt` → `doc-generator` sub-project dependencies (`scala-xml`, `better-files`, `play-json`) | Libraries used only by the doc generator | Bump opportunistically; low risk. |
| `.circleci/config.yml` → `codacy/base` orb | Shared CircleCI steps (checkout, versioning, sbt build, docker publish, tagging) | Check the latest published version; `git log -p .circleci/config.yml` shows prior bump history as a fallback reference. |
| `.circleci/config.yml` → `codacy/plugins-test` orb | Runs `codacy-plugins-test` in CI after the image is built | Same as above. |
| `project/build.properties` / `project/plugins.sbt` | sbt version, `codacy-sbt-plugin`, `sbt-native-packager`, `sbt-native-image` | Bumped in the most recent real version-bump PR alongside the Cppcheck bump — check for newer versions of each. |
| `Dockerfile` → `sbtscala/scala-sbt:graalvm-ce-...` builder image and `nativeImageVersion`/`nativeImageJvm` in `build.sbt` | GraalVM/JDK used to build and run the native image | Only bump if asked explicitly or if raising the JDK is required — don't bump opportunistically. |

### 3. Step-by-step update procedure

1. **Bump the version(s)** — at minimum `ARG toolVersion` in `Dockerfile.base`, plus any CI orbs / sbt plugin versions in scope for the task.
2. **Build the base image** (required before regenerating docs, since `DocGenerator` runs the compiled binary inside it): `docker build --no-cache -f Dockerfile.base -t codacy-cppcheck-base:latest .`
3. **Regenerate the docs**: `sbt doc-generator/run`. Review the diff for new/removed/renamed patterns (`docs/patterns.json`, `docs/description/description.json`) and stale fixture references.
4. **Compile, format, and stage**: `sbt test:scalafmt scalafmt scalafmtSbt` then `sbt stage`.
5. **Build the full Docker image**: `docker build --no-cache -t codacy-cppcheck .` (or `docker build -t codacy-cppcheck --target dev .` for a faster JVM-based dev loop, per the Test section above).
6. **Run `codacy-plugins-test` locally** before pushing — clone https://github.com/codacy/codacy-plugins-test and run the DockerTest commands (this repo's CI runs `run_multiple_tests: true`, i.e. both single and multiple/`without-config-file` test modes) against your local image tag.
7. **Iterate on failures**, re-running only the relevant DockerTest command after each fix.
8. **Commit** the version bump(s) together with the regenerated `docs/` files in one change.
9. **Push and open a PR.** CI (`.circleci/config.yml`) runs `checkout_and_version` → `checkfmt_and_compile` (`sbt test:scalafmt scalafmt scalafmtSbt`) → `publish_docker_local` (rebuilds the base image, regenerates docs, stages, builds and saves the full image) → `plugins_test` → `publish_docker` (master only) → `tag_version`.
10. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (CircleCI API/UI for the failing job — don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. The CI environment's toolchain can differ from your local one, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human — in which case explain it in the PR rather than guessing.

### 4. Common failure modes and fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| `scalafmt`/`scalafmtSbt` check fails in CI/locally | Generated or hand-edited Scala file not formatted | Run `sbt test:scalafmt scalafmt scalafmtSbt` and re-run the check |
| `doc-generator/run` fails or produces an empty pattern list | `codacy-cppcheck-base:latest` image wasn't (re)built after bumping `toolVersion`, or the tarball URL/tag for the new version doesn't exist upstream | Rebuild the base image first; verify the release tag on danmar/cppcheck |
| `pattern`/`json`/`multiple` DockerTest fails | Rule renamed/removed/added upstream between Cppcheck versions, or a fixture in `docs/tests`/`docs/multiple-tests` is stale | Re-run `sbt doc-generator/run`; confirm the change matches Cppcheck's changelog and update fixtures accordingly |
| Compile fails only in `doc-generator` sub-project after a `build.sbt` edit | The `cppcheckVersion` `val` in `build.sbt` reads `ARG toolVersion=` directly from `Dockerfile.base` at build time — a malformed or missing line breaks the sbt build entirely | Keep the exact `ARG toolVersion=<version>` line format in `Dockerfile.base` |

### 5. Definition of done

- `ARG toolVersion` (and any other in-scope versions: Alpine base, CI orbs, sbt plugins) bumped in every file that encodes them.
- Base image rebuilt and `docs/patterns.json` + `docs/description/description.json` regenerated via `sbt doc-generator/run`, with any fixture inconsistencies resolved.
- `sbt test:scalafmt scalafmt scalafmtSbt` and `sbt stage` pass locally.
- Full Docker image builds successfully.
- `codacy-plugins-test` (single and multiple-tests modes) pass locally against the freshly built image.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure (fetch the real CI log, fix, push, re-poll) until all pass — a passing local build is not sufficient, because the CI toolchain can differ from your local one (see step 10 above).

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
