# Architecture

## Capture and display

Native Android Views (`CameraScreen`, `SettingsSheet`, `ProControlPanel`, `WatermarkEditor`) call `MainActivity` actions. `CameraController` owns a Camera2 handler thread, generation-scoped device/session callbacks and recording state. `CameraPreview` owns the OpenGL thread, external camera texture, preview grading, LUTs, overlays and encoder surface.

The preview and recorder use the same GPU composition. Recorder presentation timestamps are mapped to a monotonic timeline with a stable per-record offset. Camera timestamps are used for frame deltas and deduplication, not passed through as an assumed audio clock. Terminal recorder callbacks validate ownership; nonterminal/stale events cannot stop a new clip.

## Photo export

ImageReader JPEG → recovery capture → metadata minimization → decode → orientation/crop/color → optional frame/watermark → JPEG encode → pending MediaStore row → validation → publication.

`PhotoRasterizer` fuses EXIF orientation, crop and non-LUT affine color into one native Canvas pass. `PhotoColorProcessor` supplies bounded strip processing for LUT/other callers. The original photo can be preserved separately. Capture resolution is not reduced by the color optimization.

## State and imports

`PresetStore` owns styles and local library preferences. `ConfigLibrary` imports bounded XML, deduplicates source content, reviews mapped fields and saves native setups. `PortableConfig` is a strict data-only subset adapter. `CubeLut` accepts bounded finite SDR 3D LUT data. Camera values are validated against the active lens.

Watermark settings are immutable snapshots for captures. Scope switches update preferences and preview immediately; text/layout changes use Save. A configuration receipt describes mapped requests, not independent measured camera state for every field.

## Thread/lifecycle boundaries

- UI changes happen on the main thread.
- Camera requests/session changes happen on the camera handler thread.
- GL/encoder surface operations happen on the renderer thread.
- Full-resolution photo/video exports happen on worker threads.
- Reconfiguration is marked busy before asynchronous work; configured/error/close paths release that guard.
- Recording is finalized before the GL thread is paused. App backgrounding does not continue background capture.

Domain utilities under `domain/` remain Android-independent where feasible and are covered by JVM tests. Device instrumentation additionally exercises the actual production shader, capture results, saved output and lifecycle behavior.
