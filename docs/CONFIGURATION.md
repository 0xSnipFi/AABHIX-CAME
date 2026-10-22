# Configuration profiles

Open More → Configs, or Settings → XML configuration library. Review mapped values before applying. Save current setup stores a reusable local profile. Settings → Last applied configuration shows the request receipt.

```xml
<aabhix-config>
  <int name="aabhix.iso" value="400"/>
  <string name="aabhix.exposureNs">8000000</string>
  <int name="aabhix.wb" value="1"/>
  <int name="aabhix.evSteps" value="0"/>
  <int name="aabhix.jpegQuality" value="97"/>
  <boolean name="aabhix.stableVideo" value="true"/>
  <string name="aabhix.videoFrame">cinema239</string>
</aabhix-config>
```

ISO0/Auto uses automatic exposure; the stored manual shutter is not active then. Manual ISO/shutter must fit the active lens. Shutter units are nanoseconds; stable-video mode caps it to one frame period. Nonzero EV requires automatic exposure. WB uses supported Camera2 presets, with1=Auto.

Other keys: `grid`, `stabilization`, `photoFrame`; frame values are `none`, `cinema239`, `cinema235`, `cinema185`, `white`, `black`.

The GCam/LMC adapter recognizes `pref_qjpg_key` and `pref_camera_iso_key`. Unknown/private HDR/denoise/libpatcher/ISP fields are retained only. JPEG quality changes compression, not preview color or video processing. Source profiles and user LUTs are not bundled here. Use supported LUT imports for color transforms.
