package fi.helsinki.cs.tmc.comet;

import org.junit.Test;

public class UserChannelsIT extends AbstractTmcCometTest {
    @Test
    public void testReviewAvailable() throws Exception {
        testSimplePubSub("/broadcast/tmc/user/" + TestUtils.TEST_USER + "/review-available");
    }
    
    @Test
    public void testNotAbleToSubscribeToChannelsOwnedByOthers() throws Exception {
        testCannotSub("/broadcast/tmc/user/a_different_user/review-available");
    }
}
