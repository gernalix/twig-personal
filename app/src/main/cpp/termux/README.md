# Vendored: Termux terminal JNI

`termux.c` is an **unmodified upstream copy** of

    termux-app / terminal-emulator/src/main/jni/termux.c   at tag v0.118.3
    https://github.com/termux/termux-app

which is the exact version of the `terminal-emulator` artifact this app depends
on. It is GPL-3.0-only, the same licence as Twig, and it was already linked into
the APK before — as a prebuilt `libtermux.so` inside that AAR.

## Why it is built here instead of taken from the AAR

Android 15 requires native libraries to be laid out for 16 KB pages. A 4 KB
library is not slower on such a device, the loader refuses to map it at all.
Every published Termux build is still 4 KB-aligned — v0.118.0, v0.118.3 and
v0.119.0-beta.3 were each downloaded and checked — so waiting for upstream or
bumping the dependency does not solve it, and the AAR ships a binary we cannot
relink.

Compiling the same source ourselves with `-Wl,-z,max-page-size=16384` produces a
byte-compatible replacement: the four native methods `com.termux.terminal.JNI`
declares, plus the unused `setPtyUTF8Mode` the prebuilt also exported.

The failure this avoids is not graceful. `JNI` loads the library from a static
initialiser, so a missing library raises `UnsatisfiedLinkError` — an `Error`,
which the `catch (e: Exception)` blocks in `TerminalActivity` do not stop. On a
16 KB device the terminal would crash the activity rather than report itself
unavailable.

## Keeping it in step

The Java side still comes from the AAR, so **this file must match the version in
`app/build.gradle.kts`**. If the Termux dependency is bumped, re-copy
`termux.c` from the matching tag. Generate a JNI header from the native method
declarations in the new artifact and check that its prototypes match the C
definitions. Check the actual ELF alignment in each packaged APK.

Drop this directory, the `termux` target in `../CMakeLists.txt` and the
`libtermux.so` pick-first rule in `app/build.gradle.kts` once the AAR ships a
16 KB-aligned build.
