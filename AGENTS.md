# Repository guide

This is an Android Java17 / Camera2 / GLES application. Build with the Gradle wrapper; SDK36 and JDK17 are required. Main implementation: app/src/main/java/com/aabhix/camera. JVM tests: app/src/test. Device instrumentation: app/src/androidTest.

Run host tests, lint and both debug APK builds for changes. Do not claim camera features work from a UI label or advertised capability alone. Device tests capture media and can change settings; run them only with explicit authorization and restore user preferences. Keep keys, local SDK configuration, captures, user profiles and identifying logs out of Git. Preserve main/camera/GL/export thread boundaries and lifecycle generation ownership.
