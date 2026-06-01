# ⚠️ IMPORTANT — Read Before Opening in IntelliJ

## The Gradle error you saw is now fixed

The original error (`NoSuchMethodError: LenientConfiguration.getArtifacts`) happened because
the `io.spring.dependency-management` plugin version was incompatible with Gradle 8.6.

**Fix applied:** Removed that plugin entirely. The project now uses Gradle's native
`platform()` BOM import, which is fully compatible with Gradle 8.6+.

## How to open in IntelliJ (do this exactly)

1. **File → Open** → select the `hotelos` folder → OK
2. When IntelliJ asks about Gradle, choose:
   - **Gradle JVM:** Java 17 (or 21 — both work, toolchain handles it)
   - **Use Gradle from:** `'gradle-wrapper.properties' file`  ← important
3. Wait for Gradle sync to finish (downloads dependencies — needs internet first time)
4. If sync still complains, do: **File → Invalidate Caches → Invalidate and Restart**

## If `./gradlew` is missing the wrapper jar

This zip does not include the binary `gradle-wrapper.jar`. Two options:

**Option A (easiest):** Let IntelliJ handle it — it has its own bundled Gradle.
Just open the folder and IntelliJ generates the wrapper automatically.

**Option B (command line):** If you have Gradle installed, run once in the project root:
```
gradle wrapper --gradle-version 8.6
```
This generates the wrapper jar. After that `./gradlew build` works.

## Then follow README.md for running the services
