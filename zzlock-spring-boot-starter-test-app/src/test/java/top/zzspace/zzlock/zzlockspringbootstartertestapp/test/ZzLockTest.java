package top.zzspace.zzlock.zzlockspringbootstartertestapp.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.zzspace.zzlock.ZzLock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author zujool  At 2018/9/21 17:25
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ZzLockTestApplication.class)
public class ZzLockTest {

    private static int testConcurrent = 0;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Autowired
    private ZzLock zzLock;

    @Test
    public void tesMultiThread() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            service.submit(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                zzLock.wrap("testLock", () -> {
                    System.out.print("In thread value " + testConcurrent);
                    testConcurrent = ++testConcurrent;
                    System.out.println(" || Out thread value " + testConcurrent);
                    return null;
                });
            });
        }
        countDownLatch.countDown();
        service.shutdown();
        service.awaitTermination(10,TimeUnit.SECONDS);
    }

}
