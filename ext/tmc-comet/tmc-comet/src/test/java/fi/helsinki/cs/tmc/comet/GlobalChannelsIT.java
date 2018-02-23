package fi.helsinki.cs.tmc.comet;

import org.junit.Test;

public class GlobalChannelsIT extends AbstractTmcCometTest {
    @Test
    public void testAdminMessageNotifications() throws Exception {
        testSimplePubSub("/broadcast/tmc/global/admin-msg");
    }
    
    @Test
    public void testCoursesUpdatedNotification() throws Exception {
        testSimplePubSub("/broadcast/tmc/global/course-updated");
    }
    
    @Test
    public void testCannotCreateBogusChannel() throws Exception {
        testCannotCreateAsFrontend("/broadcast/tmc/global/foobar");
        testCannotCreateAsBackend("/broadcast/tmc/global/foobar");
    }
}
