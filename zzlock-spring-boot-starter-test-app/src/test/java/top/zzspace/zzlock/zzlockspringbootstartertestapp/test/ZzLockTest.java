package top.zzspace.zzlock.zzlockspringbootstartertestapp.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private CountDownLatch countDownLatch = null;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ZzLock zzLock;

    @Before
    public void before() {
        testConcurrent = 0;
        countDownLatch = new CountDownLatch(1);
    }

    @Test
    public void tesMultiThreadWithoutLock() throws InterruptedException {
        System.out.println("Without lock");
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print("In thread value " + testConcurrent);
                testConcurrent = ++testConcurrent;
                System.out.println(" || Out thread value " + testConcurrent);
            }).start();
        }
        countDownLatch.countDown();
        Thread.sleep(1000);
    }

    @Test
    public void tesMultiThreadWithLock() throws InterruptedException {
        System.out.println("With zzlock");
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                zzLock.wrap("test", () -> {
                    System.out.print("In thread value " + testConcurrent);
                    testConcurrent = ++testConcurrent;
                    System.out.println(" || Out thread value " + testConcurrent);
                    long end = System.currentTimeMillis();
                    System.out.println(end-begin);
                    return null;
                });

            }).start();
        }
        countDownLatch.countDown();
        Thread.sleep(5000);
    }

}
