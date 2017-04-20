package org.mockito.plugin.codegen;

import org.junit.Test;

/**
 * Created by przemek on 8/9/15.
 */
public class GenMockitoCodeActionTest extends MockitoPluginPsiTestCase {

    @Test
    public void testSanityCheck() throws Exception {
        testFile("codegen/beforeClassWithAllMocksTest.java", "codegen/afterClassWithAllMocksTest.java");
    }
}