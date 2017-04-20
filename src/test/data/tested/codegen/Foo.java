import javax.inject.Inject;
import java.util.List;
import java.util.Random;

public class Foo {
    private Math baz;

    @Inject
    public Foo(Random random, List list) {

    }
}