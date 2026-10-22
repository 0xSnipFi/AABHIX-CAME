# Contributing

Open an issue with device/Android version, exact steps, mode, format/FPS, active effects/profile, and expected versus actual behavior. Supply only redacted diagnostics or media you intend to make public.

Use JDK17 and the Gradle wrapper. Run `testDebugUnitTest`, `lintDebug`, `assembleDebug` and `assembleDebugAndroidTest` before proposing changes. Physical-device instrumentation creates media and must be explicitly operated on an unlocked foreground device. Preserve user preferences after tests; document intentional exceptions.

Keep controls connected to real processing and actual capability checks. Do not add active placeholder switches or imply computational modes from color filters. Keep recording state/lifecycle ownership correct across main/camera/GL/export threads. Add meaningful tests for behavior changes, including saved-media checks for camera work.

Do not commit signing keys, local SDK paths, credentials, captures, user profiles/LUTs, private reports or proprietary comparison assets. No additional code license has been selected; discuss licensing separately before incorporating third-party implementation or models.
