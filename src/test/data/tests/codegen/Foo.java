package org.mockito.plugin.codegen.te;

import javax.inject.Inject;
import java.sql.Array;
import java.util.List;

public class Foo {
    private Math baz;

    @Inject
    public Foo(Array array, List list) {

    }
}