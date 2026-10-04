package dev.portalmod.assets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalRenderBudgetTest {
    @Test void sustainedOverloadReducesDepthAndQuietFramesRestoreIt() {
        var b=new PortalRenderBudget();
        assertEquals(2,b.update(2,true,25,6,20,4,2,3));
        assertEquals(1,b.update(2,true,25,6,20,4,2,3));
        for(int i=0;i<2;i++) assertEquals(1,b.update(2,true,10,1,20,4,2,3));
        assertEquals(2,b.update(2,true,10,1,20,4,2,3));
        assertEquals(1,b.update(1,false,100,50,20,4,2,3));
    }
    @Test void pauseAndSingleSpikeDoNotCollapseRecursion() {
        var b=new PortalRenderBudget(); assertEquals(1,b.update(1,true,1000,1,20,4,2,3));
        assertEquals(1,b.update(1,true,25,1,20,4,2,3)); assertEquals(1,b.update(1,true,10,1,20,4,2,3));
        for(int i=0;i<6;i++) b.update(1,true,100,10,20,4,2,3);
        assertEquals(1,b.update(1,true,100,10,20,4,2,3));
    }
}
