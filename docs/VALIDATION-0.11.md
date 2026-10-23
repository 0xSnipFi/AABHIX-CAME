# Validation summary — 0.11.0

Device: Vivo T2 Pro (V2321), Android 15. This development increment improves profile workflows and photo export; it does not implement all requested computational modes.

## Host checks

- 59 JVM tests passed, including randomized LUT interpolation parity and profile-value validation.
- Android lint: zero errors, 40 remaining warnings.
- Application and instrumentation debug APKs built successfully with Java 17.
- Device-test watermark flows now restore the user's initial scope preferences instead of retaining test-selected scopes.

## Device evidence

- Configuration workflow passed: apply an installed imported LUT with strength 0.37 and original-copy OFF; reject a missing LUT before changing JPEG quality; save and reparse the selected look, strength and original-copy preference; open the searchable full-page library. Screenshot inspected for layout.
- Production GPU color comparisons passed: 111 cases, 5,328 channels, maximum channel error 1. Composition geometry: 8 cases; producer geometry: 64 cases, both with zero measured error.
- Photo raster comparisons: 96 cases, zero maximum channel error.
- Three styled, watermarked captures saved at 3456 × 4608 with original-copy preservation. Shutter-to-save times: 2,410 / 2,206 / 1,805 ms. Export stage times: 1,686 / 1,703 / 1,248 ms. Saved JPEG watermark masks passed the pixel-presence check. These were the existing Mint look, not full-frame imported LUT captures.
- Short recording retry passed: two consecutive microphone clips of 6,150 and 6,082 ms had encoded audio; injected nonterminal/stale callbacks did not stop recording. Rapid mode changes stayed synchronized with advancing preview timestamps. Turning both watermark switches OFF without pressing Save updated preferences and preview immediately. Original user preferences were restored after testing.
- Independent full A/V decode of the second clip passed: H.264 720 × 1280, approximately 29.92 FPS; video duration 6.082 s, AAC duration 6.059 s. This short clip is not a long-run synchronization test.
- Synthetic 12 MP JPEG export compared the former byte-array path with direct streaming: 720 → 666 ms and 642 → 636 ms. Output dimensions and sampled decoded pixels matched; camera make/GPS tags were absent. Direct streaming removes full compressed-JPEG copies; this is not evidence of an equally large end-to-end speedup.
- One million 64³ LUT samples: former sampler 329.39 ms, optimized sampler 179.31 ms, aggregate absolute difference zero. This microbenchmark excludes JPEG decoding, full-image processing and encoding; randomized JVM tests additionally check per-sample parity.

## Limits

The first configuration test was interrupted by a locked phone; the subsequent unlocked run passed. A longer video regression was interrupted when another application came to the foreground. Those interrupted runs are not counted as successful recording tests.

Batch import is implemented with 100-file / 2 MB-per-file limits, but the system document-picker batch-selection flow has not been manually exercised in this pass. Saved profile XML references installed LUT assets; it does not embed the assets. Private GCam/LMC HDR, noise-model and libpatcher fields remain retained-only. Actual Night fusion, depth/segmentation Portrait, HEVC/Log and other items in FEATURES.md remain unfinished.

No all-device, all-settings, calibrated image-quality, sustained 4K frame-rate or long thermal/A-V certification is implied. User captures, private profiles, raw logs, screenshots and signing keys are excluded from the public repository.
