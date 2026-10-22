# Feature status and roadmap

This is the current development snapshot, not a completed combined stock/Blackmagic/GCam/LMC camera.

| Area | Implemented | Remaining work |
|---|---|---|
| Capture | Photo, Video, Pro, camera/lens selection, output framing | Expanded capability-qualified device coverage |
| Exposure | Lens-bounded ISO/shutter, EV with Auto, supported locks, anti-flicker | Calibrated Kelvin/tint, clearer conflicts and per-mode recall |
| Focus | Continuous photo/video AF, tap focus, supported manual focus | Tracking, visible AF confidence/reticle, moving-scene validation |
| Quality | Native sensor JPEG, full-resolution color/frame/watermark export | Measured enhancement pipeline, robust HDR/night fusion |
| Portrait | No computational engine | Segmentation/depth, refined masks, realistic blur |
| Night | No computational engine; tested phone exposed no public OEM extensions | Alignment/merge/denoise/tone-map capture engine |
| Video | AVC SDR, supported formats/FPS, optional AAC microphone, timestamp repair | HEVC/10-bit/Log, long thermal/A-V tests, richer audio controls |
| Monitoring | Grid, sampled RGB histogram, storage/battery, Pro readout | Zebra, peaking, false color, waveform, level/safe-area guides |
| Looks | 36 original styles, intensity, CUBE/tiled PNG/native JSON, local library | GPU full-resolution photo LUT export, separate display/record LUT scopes |
| Profiles | Native setup save/import, limited GCam/LMC subset, review and receipt | Per-lens diffs/rollback and richer application readback |
| Watermark | Unicode text, photo/video scopes, position/size/opacity, live sample | Date/model/logo/font/layout templates |
| Creative modes | Pro Video, bounded long exposure controls, Document Mono treatment | True document scan/PDF, panorama, time-lapse, slow motion, QR, live photo |
| Media | Last-capture viewer handoff, original preservation, scoped export/recovery files | Media browser, visible recovery/retry, exhaustive storage-failure tests |
| Integrations | Offline local application | Optional remote/cloud/accessories/concurrent-camera workflows |

Priority order: correctness and capture/recording reliability; measured latency and quality; genuine Night/Portrait/HDR engines; professional monitoring/audio; creative output; optional integrations.

Acceptance must include actual request/result state, saved-media inspection and realistic scenes. UI labels, advertised capabilities, color presets or a green build alone are not evidence that a camera mode exists.
