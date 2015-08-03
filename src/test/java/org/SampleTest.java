package org;

import com.tradeshift.test.remote.RemoteTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RemoteTestRunner.class)
public class SampleTest {

    @Test 
    public void test1() throws Exception {
        System.out.println("12456");
        Assert.assertEquals(1,1);

    }
}
