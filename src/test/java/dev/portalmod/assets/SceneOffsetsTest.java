package dev.portalmod.assets;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneOffsetsTest {
    @Test void eachVoxelOccursOnceInIncreasingShellOrder() {
        int[] positions=SceneOffsets.create(3); assertEquals(7*7*7*3,positions.length);
        var unique=new HashSet<String>(); int previous=0;
        for(int i=0;i<positions.length;i+=3) {
            int shell=Math.max(Math.abs(positions[i]),Math.max(Math.abs(positions[i+1]),Math.abs(positions[i+2])));
            assertTrue(shell>=previous); previous=shell;
            assertTrue(unique.add(positions[i]+":"+positions[i+1]+":"+positions[i+2]));
        }
        assertEquals(343,unique.size());
    }
}
