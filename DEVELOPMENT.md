# DatStation Development

### Build and test

```
gradlew.bat assembleDebug          # debug APK in app\build\outputs\apk\debug
gradlew.bat testDebugUnitTest      # JVM unit tests
gradlew.bat lintDebug              # lint; errors fail the build, report in app\build\reports
```

`build_apk.bat` does the debug build with Android Studio's bundled JDK.
Every push to `master` runs the `CI` workflow: unit tests, lint and a debug build.

### Game database

`app/src/main/assets/datstation.db` is a plain SQLite file with one table per
platform (PS1, PS2, PS3, PS4, PS5, PS6, PSP, PSV). It is **not in git** (63 MB,
and every update would add another 63 MB of history). The build refuses to run
without it and tells you where to put it.

- Locally: copy the file into `app/src/main/assets/`.
- GitHub Actions: set the repository variable `DATSTATION_DB_URL` to a direct
  download link (for example a release asset of the database repository). If
  the link needs a token, add it as the secret `DATSTATION_DB_TOKEN`; it is sent
  as a bearer token.

On the device the app remembers the size of the asset it last copied and
re-copies when a new APK ships a different file. User data (favorites,
collection, scores, trophies, profile) lives in a separate encrypted
`user_data.db` and survives database updates.

### Quiz questions

Questions ship as plain JSON in `app/src/main/assets/questions.json`: an array
of objects with `id`, `text`, `type` (`MC` or `TF`), `category`, `options`,
`correct_answer`, `points` and an optional `explanation` that is shown after a
true/false answer. `correct_answer` must be one of the `options` exactly.

After editing the file, bump `currentQuestionVersion` in `PlayStationQuiz.kt`
so installed apps reload the questions.

### Releasing

A release is cut by pushing a version tag; nothing is committed back:

```
git tag v1.0.3
git push origin v1.0.3
```

The `Release` workflow runs tests and lint, builds a signed release APK with
`versionName` 1.0.3 and `versionCode` 10003 (major × 10000 + minor × 100 +
patch), and publishes a GitHub release with the APK attached. It needs these
repository secrets: `SIGNING_KEY` (keystore, base64), `ALIAS`,
`KEY_STORE_PASSWORD`, `KEY_PASSWORD`, and the database variable above.

To build a signed APK locally, export the same values as environment
variables before running Gradle: `DATSTATION_KEYSTORE` (path),
`DATSTATION_KEYSTORE_PASSWORD`, `DATSTATION_KEY_ALIAS`, `DATSTATION_KEY_PASSWORD`.

### ADB

```
adb uninstall com.robertotorino.datstation
adb install app\build\outputs\apk\debug\datstation-debug.apk
adb shell pm clear com.robertotorino.datstation
```

### GitHub

```powershell
git pull origin master

git remote add origin git@github.com:RobertoTorino/DatStation.git
git push -u origin master
git fetch --all
git remote -v
```

## Refactor I

Overall: the app works and its structure is readable, but there are a few real defects in the database bootstrap and the translations, the main screen holds all state in a way that rotation wipes, and CI builds without running tests or lint. Below is what I found, ranked, then the decisions I need before building anything.

Likely defects
Database re-copy on cold start. DatabaseManager.kt:90-103 decides whether to refresh the local copy by comparing file sizes. But Room adds its own two tables to the copied file, and the asset has no free pages to absorb them. Once SQLite checkpoints, the local file grows, the size check fails, and every launch deletes and re-copies the 63 MB asset on the main thread. Worth confirming on a device by listing the databases folder after a few launches. Fix: compare a version string or hash stored in preferences instead of sizes.
Fragile Room version. The asset's user_version is 0 while the code declares version 87 with destructive migration that drops all tables. Any future bump of that number wipes the PS1 to PSV tables on existing installs. Room is used here only for createFromAsset, so a plain copy-and-open would remove the risk and the dummy entity.
Translations break format strings. random_results and welcome_title have a placeholder in English but none in all six translations, so non-English users never see the count or their name. Lint reports this as an error.
Rotation wipes state. No ViewModel, no rememberSaveable, and orientation is not locked, so rotating the phone clears the search, filters and a running Neon Disc or Neon Racer game. Only the quiz survives.
Cold-start freeze. The database is opened synchronously in onCreate, and the window theme is light, so the first launch shows a white screen for the whole 63 MB copy. A splash theme plus opening the database off the main thread fixes both.
Build, CI and repo hygiene
Lint fails, CI skips it. Lint reports 124 errors and 184 warnings, and the workflow only runs assembleRelease. Tests do pass (10 of 10) but are not run either. Adding test lint to the workflow with a baseline file gets you a green gate that catches regressions.
Release workflow quirks. Every push to master, including a README edit, bumps the version and pushes a tag, yet no GitHub Release is created and the artifact is named gallery-release from another project. If the signing secrets are missing the APK is uploaded unsigned without a warning.
Dependencies. ui-graphics is pinned to 1.11.0 beside the BOM-managed one, roomKtx duplicates the room version, and android-database-sqlcipher 4.5.4 is the deprecated artifact superseded by sqlcipher-android. Lint lists ten newer versions. Java target is 11 while AGP 9 and Kotlin 2.3 are happy with 17.
Repo weight. The 63 MB database lives in git, so every data update adds 63 MB of history. Git LFS, or fetching it from its source repo at build time, keeps the repo small. The compiled EncryptQuestions.class is also committed.
Cosmetic encryption. The quiz questions are AES-encrypted with a key that is in the APK and in the repo, and the quiz Room database uses a hardcoded passphrase. That is obfuscation, not protection. Keep it knowingly or drop it and simplify.
Code quality
Main screen size. GameFinderApp is a 1400-line composable with around thirty state variables. The search call is duplicated between the voice path and the button, and the favorite and collection handlers are near-identical copies. A ViewModel plus one runSearch function would halve it.
Unbounded search. A query across all tables has no LIMIT, so a one-letter search loads every match into memory. A limit with a "show more" or paging keeps it snappy.
Hardcoded English. 23 user-facing strings bypass resources (Hall of Fame, NEXT STAGE, PlayStation Updates, Search apps) while the app ships six languages, and lint counts over a hundred missing translations, mostly in the non-Italian locales.
Tests test the wrong thing. GameFilterTest exercises Kotlin's filter, not the repository SQL. A Robolectric test against the real asset database would have caught the size-check and format-string issues.
Dead code. DataStationTheme and the purple palette are used only by previews, one Moonshie home image is unused, and lint lists 58 unused resources.
Game loop. Neon Racer ticks on a fixed 16 ms delay, so speed depends on the device. Delta time from withFrameNanos fixes it.
Decisions I need from you

Scope: a quick fix pass (bootstrap, format strings, lint gate, dependency cleanup, workflow fixes) or also the structural work (ViewModel and saved state, split the main screen, repository tests)?
Keep the questions encryption as-is, or drop it?
Database distribution: stay in git with LFS, or fetch at build time from the source repo?
Releases: keep bump-on-every-push, or switch to tag-driven releases that publish a GitHub Release with the signed APK?
Estimate: the quick fix pass is about one day. The ViewModel and state refactor with tests is one to two days more. Translation cleanup is half a day. The workflow rework is half a day. I will wait for your go-ahead before changing anything.

## What changed
Translations dropped. The six language folders are deleted, along with the locale preview composables. The app is English only, which also removed the broken format strings.

Quiz data bug fixed. 56 true/false questions, ids qz-0612 to qz-0667, had the explanation glued onto the answer, for example False: Bloodborne was developed by FromSoftware.... The app compares the exact string, so those questions were always marked wrong. The answers are now plain False with the explanation moved to a new explanation field, 30 questions using TRUE/FALSE are normalised to True/False, the asset is re-encrypted, and the question version is bumped so installed apps reload. The answer check is also case-insensitive now. The plain JSON sits in tools/questions.json, untracked; keep or delete it as you prefer.

Database bootstrap. DatabaseManager.kt no longer goes through Room for the catalogue. It copies the asset once, records the asset size it copied, and only re-copies when a new APK ships a different file. The Room version-87 trap and the dummy entity are gone.

Startup and navigation. MainActivity.kt opens the database off the main thread behind a loading screen, shows an error screen instead of crashing, remembers the current screen across rotation, and handles Back so a game returns to the finder instead of closing the app. The window theme is dark now, so no white flash at launch and status bar icons stay readable.

Finder screen. Search state lives in the new GameFinderViewModel.kt and survives rotation. The duplicated search and favorite/collection code is folded into two helpers. Text searches are capped at 500 rows with a toast telling you to narrow the search. The SQL lives in SearchQueryBuilder.kt and has seven unit tests. The activity is locked to portrait so a running game is not lost on rotation.

Build and repo. Java 17 target, dependencies bumped (Compose BOM, Room, lifecycle, core, browser, sqlite), duplicate dependency entries removed, lint set to fail on errors, 48 unused strings and three unused drawables removed, dead commented code stripped. The compiled class file is out of the repo and the encrypt tool moved to tools/. The workflow now runs tests and lint before building, uploads reports on failure, warns when signing secrets are missing, and names the artifact after the app.

Quiz scan, first pass
The automated pass over all 1461 questions found no duplicate ids or texts, every question has the right field set, and after the fix every answer is one of its options. Worth a look by eye:

Eleven multiple-choice questions have no question mark, mostly "Guess the game from the dialogue:" ones. Cosmetic.
One question, qz-1055, has a space before its question mark.
Non-ASCII characters are all legitimate (yen sign, ö in Ragnarök, Ō, é, curly apostrophes), no mojibake.
The manual read-through for odd sentences is still to do. Say the word and I will go through the full list.

Left open
The SQLCipher library is end-of-life; the migration to its successor needs an on-device check of existing encrypted user data, so I left it.
Neon Racer still runs on a fixed 16 ms tick.
The 63 MB database stays in git, and the workflow still bumps the version on every push; both were your call and remain undecided.

## Prompt
Fix any cosmetics you find, then do the read-through, drop the encryption, Neon Racer: can you explain? Database is needed for the app, can I leave it out of git then? Workflow should be refactored and based on tags.

10:51 AM
Plan for this round: fix the cosmetic quiz issues and read all 1461 questions, remove the quiz encryption (questions become a plain JSON asset, quiz database opens without a passphrase), take the catalogue out of git with a download step, and split the workflow into a CI run on master and a tag-driven release. I'll explain the Neon Racer tick issue in the write-up.

## Refactor II

Quiz read-through
I read all 1461 questions. The fixed file is questions.json, now a plain JSON asset.

Cosmetic fixes, about 100 in total: stray "s for 's (HORI"s, Insomniac"s, Capcom"s), trailing apostrophes on 12 sets of dialogue options, curly quotes, doubled words ("PlayStation PlayStation Vita"), "populair", "Astros Playroom", "discontinued" for "discontinue", "Who is main the character", a space before a question mark, and eight multiple-choice questions with no question mark.
Rewordings, about 70: questions whose text did not match their options, such as "What term does the main rival use to refer to living things?" with character names as options, the ten PlayStation Plus tier questions that described "cloud storage accounts" and "PlayStation stores", and "Which franchise features Ratchet and Clank?" answered with "Insomniac Games". Each now asks for exactly what the answer is.
Factual corrections, 6: "A hunter is a hunter, even in a dream" is Gehrman, not Micolash (two questions); MJ is voiced by Laura Bailey, not Tara Platt; the SIE CEO is Hideaki Nishino; "You exist because we allow it" is from the first Mass Effect, so the answer is the Legendary Edition; Rise of the Ronin is from 2024, not 2020. Ambiguous option sets were also fixed, for example Final Fantasy VIII also has four discs, so it no longer sits next to The Legend of Dragoon.
Removed, 25: questions I could not repair, like "What Pokémon game is associated with..." answered "Kingdom Hearts", a Samba de Amigo question for a console it never shipped on, and unverifiable claims. They are in questions-removed.json with a reason each, so you can restore any you disagree with.
The true/false explanations I split off last round are now shown under the answer in the quiz.

Encryption dropped
The quiz asset is plain JSON and the quiz cache database opens without a passphrase. The old encrypted cache file is deleted on first launch, and the encrypt tool is gone. The user_data.db with favorites and scores keeps its device-bound SQLCipher key, because changing it would wipe existing users' data.

Database out of git: yes
The catalogue is now gitignored and untracked. Two things make that work:

Gradle fails before compiling with a clear message if the file is missing, so a fresh clone cannot silently build a broken APK. I verified that message fires.
Both workflows download it first from the repository variable DATSTATION_DB_URL, with an optional DATSTATION_DB_TOKEN secret sent as a bearer token. You need to set that variable in GitHub before the next workflow run, pointing at a direct download link such as a release asset of the database repo.
The 63 MB already in history stays there unless you rewrite history. It just stops growing.

Workflow
Two workflows replace the old one. ci.yml runs on every push and pull request to master: tests, lint, debug build, debug APK as artifact. release.yml runs only when you push a tag like v1.0.3. It refuses to run without the signing secrets, derives the version name from the tag and the version code as major×10000 + minor×100 + patch, builds a signed APK, and publishes a GitHub release with the APK attached. Nothing gets committed back. Signing moved into Gradle via environment variables, so the same secrets work locally. The steps are in DEVELOPMENT.md.

Neon Racer
The game loop in NeonRacerGame.kt:368 waits 16 milliseconds, then moves every car by a fixed fraction of a grid cell. The wait is a minimum, not an exact interval. On a loaded or slower phone each tick arrives late, so fewer ticks happen per second and the whole game runs slower, including the enemy. On a 120 Hz phone it can run faster. Difficulty therefore depends on the device, not on the player. The fix is to measure the real time between ticks and scale each movement by it, or to drive the loop from the display's frame clock. It touches every per-tick movement constant in the file, which I would want to tune on a real device, so I left it for when one is attached.

Still to do on your side
Set DATSTATION_DB_URL in the repository settings, then push to master to see CI go green.
Install the debug APK once before tagging a release. The database bootstrap, the startup flow and the quiz reload are compile-checked only.
Two deletions are staged in git, the untracked database and the old encrypted asset, because that is how untracking works. Everything else is unstaged.
