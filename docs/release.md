# Version, Artifact, and Release Governance

> Semantic source: English. Chinese counterpart: [release.zh-CN.md](release.zh-CN.md).
>
> This document is the authoritative operational contract for application versions, completed version records, APK artifacts, signing continuity, Git tags, and GitHub Releases. It does not authorize a particular version, tag, GitHub Release, public distribution, or implementation scope.

## Project version model

Launcher4Max uses a project-defined versioning profile with the numeric form `MAJOR.MINOR.PATCH`. It is based on the shape of Semantic Versioning but is governed by this document rather than by the official Semantic Versioning specification.

- The first formal application version is `1.0.0`. The project does not use `0.x.y` versions.
- The project has no prerelease-version concept. Identifiers such as `alpha`, `beta`, and `rc`, and SemVer prerelease or build-metadata suffixes, are not part of the version format.
- Repository commits, pushed source states, local builds, and other work before `1.0.0` are development evidence, not formal application versions.
- A formal version identifies an installable and verified Android APK. It is not created by documentation work alone.
- Every formal version contains one or more completed iterations. The applicable version delivery contract selects the subset of the current product contract delivered by that version.
- `1.0.0` is the first minimum usable version. It is not required to implement every behavior described by the current product contract; its exact delivery scope must be established through the version and iteration contracts.
- The current product contract may continue to change while versions are planned, delivered, or completed. A completed version record preserves the scope and evidence accepted at its own delivery boundary and may therefore differ from the later current product contract without being retroactively incorrect.

## Delivery levels

Every version delivery contract must select exactly one of the following delivery levels. The levels increase evidence and operational obligations; they are not separate version-number systems and do not imply public distribution.

### Development build

A development build supports implementation, investigation, or focused validation before a version boundary is accepted. It may be local, temporary, incompletely validated, and signed by a development identity. Record only the evidence needed by the active task or iteration. A development build is not a completed application version, daily-use baseline, formal release artifact, milestone, or release.

### Author daily-use baseline

An author daily-use baseline is an installable version intended for the project author's ongoing use. Completion requires:

- the selected product journey is implemented and accepted;
- the exact application identifiers and source commit represented by the installed APK are recorded;
- the APK installs and the selected core journey succeeds on at least one author-designated primary physical device;
- no known included-path failure makes that validated daily use unsafe or unusable;
- known validation gaps, limitations, recovery implications, and follow-up ownership are recorded; and
- the implementation and delivery documentation are committed and synchronized to the author-designated shared Git history.

Recommended automated checks, the complete compatibility matrix, performance distributions, formal release signing and backup, release-wide artifact digest and archive evidence, specialist license conclusions, tags, publication, and distribution do not block this level unless the applicable version contract explicitly promotes one to a gate. Missing evidence remains unknown, not passed. Retain sufficient source and APK identity information to reinstall the accepted baseline; this does not promise Android data rollback or downgrade support.

### Formal release artifact

A formal release artifact is the highest-evidence delivery level. In addition to the author daily-use baseline requirements, its version contract defines and satisfies the required compatibility environments, automated and manual validation, performance or reliability thresholds, dependency and license disposition, merged-manifest and security/privacy review, stable release-signing custody and recovery, artifact digest, external retention, and complete source-to-artifact traceability. Tagging, GitHub Release creation, store action, upload, and public distribution remain separately authorized even when this level is complete.

The author may promote a completed lower-level version through a later, separately authorized delivery contract or version. Do not retroactively claim evidence that did not exist at the original completion boundary.

## Numeric progression

Every changed numeric component increments by exactly one. Skipping numbers is prohibited unless the project author explicitly replaces this contract.

- `PATCH` increments by one for a compatible implementation correction within the current product and version contract. When `PATCH` increments, `MAJOR` and `MINOR` remain unchanged.
- `MINOR` increments by one for a user-visible capability, an intentional behavior change, or a materially improved usable experience within the current capability layer. When `MINOR` increments, `PATCH` resets to zero.
- `MAJOR` increments by one only when the project enters the next authorized capability layer. When `MAJOR` increments, `MINOR` and `PATCH` reset to zero.
- An increment to an earlier component always resets every later component to zero.

The `MAJOR` number is reserved for the product capability layers:

| Version family | Capability layer |
| --- | --- |
| `1.x.x` | V1: fixed presentation |
| `2.x.x` | V2: basic adaptation |
| `3.x.x` | V3: AI capability |
| `4.x.x` | V4: agent capability |

Until the contract for entering V2 is explicitly authorized, every formal version remains in `1.x.x`. A major project achievement or an incompatible change does not independently permit a `MAJOR` increment. Within one capability layer, an incompatible change uses the next `MINOR` version and must document its impact and migration requirements.

Pure documentation changes never change the application version. A source, resource, manifest, dependency, or build-configuration change that affects the APK may justify a new formal version, but an ordinary code change does not become a formal version until the author declares the version boundary and its required evidence is complete.

## Android version identifiers

- Android `versionName` is the formal `MAJOR.MINOR.PATCH` value defined above.
- Android `versionCode` starts at `1` for `1.0.0`.
- Every APK that becomes a traceable artifact through distribution for installation, delivery validation, or observability-platform upload must use the next unused `versionCode`, incremented by exactly one.
- Repeated local builds that are not retained, distributed, used as delivery evidence, or uploaded to an observability platform may reuse their configured `versionCode`.
- A completed formal version uses the next unused `versionCode` available at its accepted artifact boundary. A planned `versionCode` is provisional until that boundary.
- `versionCode` is never calculated from `versionName`; the allocation sequence may not skip a value, and an allocated value may not be reclaimed, reused for a different traceable APK, or decreased.
- Two different formal `versionName` values may not share a `versionCode`.
- The product rename to Launcher4Max changed the `applicationId` from `com.avenor.launcher` to `com.maxarchm.launcher`. Android therefore treats the renamed application as a fresh install: an existing Avenor installation does not upgrade in place, and its local favorites and Drawer display settings are not visible to the renamed application. The supported migration path is: back up favorites and display settings through the previous application's Settings Data export, install the Launcher4Max build, restore that backup file, re-select the default-Launcher role, re-enable the accessibility service, and only then remove the previous application. The restore path accepts the previous `avenor-backup-` file name because restore parses backup content rather than file names.
- Downgrade is not supported.

## Application identity migration

The current `applicationId` is `com.maxarcher.launcher`, replacing `com.maxarchm.launcher`.
Android installs it as a separate application, even with the same signing certificate and version.
It cannot upgrade the previous package in place or automatically access its private data.
The earlier Avenor rename recorded above remains a historical migration.

1. Export a backup through Settings Data in the previous application before removing it.
2. Install the build with the new application identity and restore the exported backup manually.
3. Select the new application as the default Home app and enable its accessibility service again
   if using accessibility-based lock.
4. Verify restored settings and favorites before removing the previous application.

This identity change does not change the backup schema or add automatic data migration.
Backup component identities are not rewritten from the old Launcher package to the new package.
Old Launcher self-references continue through the existing restore and availability handling;
check those entries after restoring.
Android roles and service grants are not restored by the backup.
Build, installation, and device migration validation remain required before release acceptance.
This source change does not declare a completed formal version or allocate a new artifact version.

## Build identity and observability

Product version, APK build sequence, build stage, source revision, and Git milestone identity are separate dimensions:

- `versionName` expresses the product version and follows the project version model. It does not encode development stage, monitoring state, or tag status.
- `versionCode` distinguishes ordered, traceable APK builds. Development and release artifacts are not distinguished through odd/even allocation, fixed `+2` release jumps, or another reserved numeric pattern.
- A traceable development or internal APK may carry the planned target `versionName` without declaring that product version complete. It does not add a suffix or create a prerelease version.
- Every traceable APK record identifies its build stage as `development`, `internal`, or `release`, together with its exact source Git commit. These stage labels are artifact metadata and do not introduce a prerelease-version format.
- Record each allocated `versionCode` and its APK identity in the applicable iteration or delivery evidence even when that build is later rejected or superseded, so the next allocation remains unambiguous.
- When an observability or crash-monitoring platform is adopted, its uploaded build identity must be traceable to `applicationId`, `versionName`, `versionCode`, build stage, and source commit. Platform-specific release markers must not replace the project delivery record.
- A build does not become a completed formal version merely because it is uploaded to a monitoring platform or marked as a release there.
- Privacy, security, data collection, consent or disclosure, dependency and license impact, symbol or mapping-file custody, access control, retention, and upload authority must be reviewed before a monitoring integration is authorized.

Within one `MAJOR` family and the same application identity, any older formal version must have a
supported direct upgrade path to any newer formal version. The application identity migration above
requires a separate installation and manual restore, regardless of the configured version number.
For a cross-major upgrade, the applicable major-version delivery contract must define supported
source versions, migrations, validation, and limitations before release.

## Iterations and completed version records

Each version uses one stable `docs/delivery/<version>/` directory from planning through completion. Its `delivery.md` records the selected delivery level, completion criteria, and factual result. Completing a version does not rename or move the directory; it makes the version and its completed iteration records protected delivery history. Record the following when applicable:

- `versionName` and `versionCode`
- The exact Git commit represented by the APK
- The included iterations and the reason for declaring the version boundary
- Important product and implementation changes
- Migration or compatibility impact
- Completed validation evidence and affected devices or environments
- Known limitations, unresolved defects, and other legacy issues
- Available APK or build identity
- APK SHA-256 digest when required by the selected level
- Signing-certificate SHA-256 fingerprint when a stable signing identity is part of the selected level
- Related Git tag and GitHub Release when either exists

The completed version record must never contain a private signing key, keystore password, key password, or another signing secret.

## APK artifact contract

When a completed version APK is retained, store it outside this product repository.
The project author keeps the exact directory and may change it.
This repository does not record that directory.
The author accepted on 2026-09-23 that the chosen directory may also hold other private files.
Author daily-use baseline retention is optional unless that version contract requires it.
Retention is mandatory for a formal release artifact.

- APK files must not be committed to the `launcher-4-max` Git repository.
- Whether another repository tracks the retained APK files is not decided here.
- Until that decision, the location is external storage, not authorization to commit the APK.
- No synchronization or backup schedule is defined here.
- For consistent external archiving, use the filename pattern
  `<product>-v<versionName>-vc<versionCode>-<stage>-<shortCommit>.apk`, for example
  `launcher4max-v1.7.0-vc8-release-8523e0f.apk`.
- The filename is an identification and archival aid only. It does not replace the
  APK's `applicationId`, `versionName`, `versionCode`, full source commit, SHA-256
  digest, or applicable signing evidence.
- The recorded SHA-256 digest must be computed from the exact archived APK and verified after copying it to the external location.
- The artifact record must also identify the build time and the environment used to produce and validate the APK when that evidence becomes available.

An APK represents a completed version only after it is installable, has passed the validation required by the selected delivery level, and matches the recorded source commit and identifiers. Digest and signing-fingerprint evidence is required only when the selected level or version contract requires it.

## Signing continuity and custody

Every formal release artifact must use one stable release-signing identity so supported updates can be recognized as updates of the same Android application. An author daily-use baseline may use an existing development or author-controlled signing identity when its version contract records the resulting installation and update limitations.

- The project author creates or explicitly authorizes creation of the release keystore and retains ownership and final control of it.
- The release keystore, private key, passwords, and signing-property files must remain outside Git and outside authoritative project documentation.
- The project author retains the release keystore, its passwords, and its backups outside Git.
- Those materials stay outside Git and outside authoritative documentation.
- On 2026-09-23 the author chose to perform that custody and backup personally.
- This repository records no locations, format, parameters, or procedure.
- The two-independent-encrypted-backup requirement still applies before the first formal artifact.
- Satisfying it is an author action, not a recorded procedure.
- Each applicable completed version record stores only the release certificate's SHA-256 fingerprint, never private signing material.
- Before the first formal release artifact, the project must establish secure storage and at least two independent, encrypted, author-controlled backups of the release keystore and required recovery information.
- Loss, compromise, rotation, or platform-managed migration of the signing identity requires explicit author approval, an impact assessment, and a documented migration decision before another formal version is declared.

A future decision to distribute through Google Play or another store must separately define platform-managed signing, upload keys, channel requirements, and migration consequences. No distribution platform is currently authorized.

## Git tags and GitHub Releases

Application versions, Git tags, and GitHub Releases are separate records:

- Every formal version retains a completed delivery record, but not every formal version receives a Git tag.
- A tag is appropriate when accumulated related functionality or experience improvements establish an important, stable implementation baseline. For example, `1.0.0` may remain untagged while a later `1.3.0` becomes a tagged baseline.
- Tag creation and the exact target commit require explicit project-author approval. Agents may recommend whether a version is worth tagging.
- When a tag is created, its name is `v<MAJOR>.<MINOR>.<PATCH>`.
- The name matches the accepted `versionName`, for example `v1.7.0`.
- Prerelease suffixes are not used.
- The tag is an annotated tag with the message `release: <tag>`.
- Each released version has one tag, and it is never force-moved or recreated.
- It points to the exact source commit represented by the accepted APK and is created on `main`.
- Creating it does not change `versionName` or `versionCode`.
- Creating the tag and pushing it with `git push origin <tag>` remain separately authorized.
- If no prior release tag exists, the author chooses the release-notes comparison range.
- That range is not assumed to be the whole history.
- Creating a tag does not increment or otherwise change the accepted APK's `versionName` or `versionCode`. The tag must point to the exact source commit represented by that artifact.
- Creating a tag does not require creating a GitHub Release.
- A GitHub Release must reference an existing approved tag and requires separate explicit author approval.
- No GitHub Release is required for version completion.
- The current distribution channel is the public GitHub repository only.
- Attaching a verified APK to a GitHub Release requires separate author approval.
- Using any other channel also requires separate author approval.
- That approval also covers the applicable security and validation gates.
- Neither this document nor a completed version record authorizes a tag, remote push, GitHub Release, or APK upload.

For this project, a milestone exists only when the project author explicitly declares an important baseline and its approved Git tag exists. A GitHub Release is optional and requires separate approval when the author chooses outward-facing publication. A version or an approved tag not declared as a milestone is not a milestone automatically.

## Migration-cost discipline

Product definition, technical research, architecture, and implementation must consider likely future capability-layer migration cost. Where evidence supports it, preserve replaceable boundaries, data migration paths, and appropriate extension or API seams so later capability layers do not require avoidable rewrites. This requirement does not authorize speculative frameworks, unused abstractions, or implementation details in product documentation; the chosen preparation must remain proportional to current evidence and must not silently expand current scope.

## Remaining implementation decisions

Decided on 2026-09-23:

- Retained APKs stay outside this product repository, at a directory kept by the project author.
- Tracking them elsewhere, and any synchronization or backup schedule, remain undecided.
- Release-keystore custody and backup stay with the project author and are not recorded here.
- A created tag uses `v<MAJOR>.<MINOR>.<PATCH>` as an annotated tag.
- See Git tags and GitHub Releases.
- This distribution stage uses no observability or crash-monitoring platform.
- The distribution channel is the public GitHub repository only.
- A GitHub Release, tag, or APK upload remains separately authorized.
- The authoritative commands are: build and sign with `./gradlew assembleRelease`, which writes `app/build/outputs/apk/release/app-release.apk` and takes its signing configuration from the local `keystore.properties`; verify the artifact identity with `aapt2 dump badging <apk>`; verify the signing certificate with `apksigner verify --print-certs <apk>`; compute the digest with `shasum -a 256 <apk>` and recompute it after the copy to the retained location; and validate with `./gradlew lintRelease`, `./gradlew testDebugUnitTest`, and `./gradlew connectedDebugAndroidTest`. Device installation and upgrade are performed on the device by opening the APK, so no host-side install command is recorded.
