package dev.portalmod.assets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceMaterialTest {
    @Test void opaqueShaderMasksDoNotRemoveGeometry() {
        var m=SourceMaterial.parse("VertexLitGeneric { \"$baseTexture\" \"models/example\" $phong 1 }");
        assertEquals("models/example",m.texture()); assertEquals(0xff123456,m.pixel(0x00123456));
    }
    @Test void irisLoadsAndShaderMaskBecomesOpaque() {
        var m=SourceMaterial.parse("EyeRefract { $Iris \"models/eyes\" }");
        assertTrue(m.eye()); assertEquals("models/eyes",m.texture()); assertEquals(0xff123456,m.pixel(0x20123456));
    }
    @Test void translucentAndCutoutKeepActualOpacity() {
        assertEquals(0x20123456,SourceMaterial.parse("$translucent 1").pixel(0x20123456));
        assertEquals(0x20123456,SourceMaterial.parse("$alphatest \"1\"").pixel(0x20123456));
        assertFalse(SourceMaterial.parse("// $translucent 1\n$basetexture example").translucent());
    }
}
