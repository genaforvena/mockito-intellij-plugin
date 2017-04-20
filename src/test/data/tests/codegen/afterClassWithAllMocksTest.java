import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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