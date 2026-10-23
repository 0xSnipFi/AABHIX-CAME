package com.aabhix.camera.domain;
import org.junit.Test;import static org.junit.Assert.*;
public class PortableConfigTest {
    @Test public void retainsAndroidStringSets()throws Exception{PortableConfig config=PortableConfig.parse("<map><set name='private'><string>one</string><string>two</string></set><int name='pref_qjpg_key' value='97'/></map>");assertEquals(1,config.unmapped);assertEquals("97",config.values.get("jpegQuality"));}
    @Test public void mapsKnownKeysOnly()throws Exception{PortableConfig config=PortableConfig.parse("<map><string name='pref_qjpg_key'>100</string><string name='private_hex'>AABB</string></map>");assertEquals("100",config.values.get("jpegQuality"));assertEquals(1,config.unmapped);}
    @Test public void nativeExposureHasExplicitUnits()throws Exception{PortableConfig config=PortableConfig.parse("<aabhix-config><string name='aabhix.exposureNs'>8000000</string><string name='aabhix.iso'>400</string></aabhix-config>");assertEquals(2,config.values.size());}
    @Test(expected=IllegalArgumentException.class)public void rejectsDoctype()throws Exception{PortableConfig.parse("<!DOCTYPE map SYSTEM 'file:///x'><map/>");}
    @Test(expected=IllegalArgumentException.class)public void rejectsDuplicate()throws Exception{PortableConfig.parse("<map><string name='pref_qjpg_key'>90</string><string name='pref_qjpg_key'>99</string></map>");}
    @Test(expected=IllegalArgumentException.class)public void rejectsBadQuality()throws Exception{PortableConfig.parse("<map><int name='pref_qjpg_key' value='500'/></map>");}
    @Test(expected=IllegalArgumentException.class)public void rejectsDeepInputBeforeDomRecursion()throws Exception{PortableConfig.parse("<map><string name='private'>"+"<x>".repeat(20000)+"x"+"</x>".repeat(20000)+"</string></map>");}
    @Test public void supportsSavedImportedLookAndStrength()throws Exception{PortableConfig c=PortableConfig.parse("<aabhix-config><string name='aabhix.styleId'>user-123.cube</string><string name='aabhix.styleIntensity'>0.65</string><boolean name='aabhix.original' value='false'/></aabhix-config>");assertEquals(3,c.values.size());assertEquals("user-123.cube",c.values.get("styleId"));assertEquals("false",c.values.get("original"));}
    @Test(expected=IllegalArgumentException.class) public void rejectsNonFiniteIntensity()throws Exception{PortableConfig.parse("<map><string name='aabhix.styleIntensity'>NaN</string></map>");}
    @Test(expected=IllegalArgumentException.class) public void rejectsExcessiveIntensity()throws Exception{PortableConfig.parse("<map><string name='aabhix.styleIntensity'>1.1</string></map>");}
    @Test(expected=IllegalArgumentException.class) public void rejectsStylePath()throws Exception{PortableConfig.parse("<map><string name='aabhix.styleId'>../private</string></map>");}
}
