import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

import java.util.List;
import java.util.Random;

import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(JUnit4.class)
class FooTest {
    @Mock
    private Random mRandom;
    @Mock
    private List mList;

    private Foo mUnderTest;

    @Before
    public void setUp() {
        initMocks(this);

        mUnderTest = new Foo(mRandom, mList);
    }
}