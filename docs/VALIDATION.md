# Validation summary — 0.10.0

Recorded locally on 2026-10-22 against the development source, on Vivo T2 Pro (Android model identifier V2321), Android 15. Raw media, identifiers, device paths and personal logs are intentionally not published.

## Evidence

- Java/JVM: 54 tests passed.
- Android lint: zero errors, 40 warnings; warnings remain and code coverage percentage was not measured.
- Build: app and instrumentation debug APKs built successfully.
- Production shader tests cover color transforms, composition geometry and producer transforms.
- 0.9 fused photo path: 96 orientation/crop/color comparisons exactly matched the prior separate paths on the tested chart.
- 0.10 reliability: rapid successive mode changes retained matching UI/camera mode and advancing frame timestamps.
- Two consecutive microphone recordings saved with encoded audio, approximately 15.149 and 15.019 seconds; nonterminal/stale callback injections did not terminate them.
- Independent inspection of one clip: H.264 720×1280, approximately 29.92 FPS; video start 0.0108 s / duration 15.005978 s, AAC start 0 / duration 15.018667 s. Full audio/video decode passed.
- Watermark OFF without Save updated preference and preview state; final scopes remained OFF while text was preserved.
- Core device regression: 19 passed. Orientation regression: 16 passed.

## Limits

These are focused engineering checks, not a claim that every setting combination works. No calibrated lip-sync, color/optical-quality, long thermal-run, all-device or sustained 4K30 certification. A prior short 4K sample averaged about 24.31 FPS despite a 30 FPS request. Photo export latency varies with JPEG decode, grading/LUT workload, encoding, memory pressure and original preservation.

The public project uses standard local debug signing instead of the owner's private development key. The published Java/resources match the milestone; public build signatures/bytes differ from the privately signed device artifact. GitHub CI verifies build/lint/JVM tests only. CI results must be checked independently of this historical device report.
