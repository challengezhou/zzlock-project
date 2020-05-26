package top.zzspace.zzlock.zzlockspringbootstartertestapp.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.zzspace.zzlock.ZzLock;

import java.util.concurrent.CountDownLatch;

/**
 * @author zujool  At 2018/9/21 17:25
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ZzLockTestApplication.class)
public class ZzLockTest {

    private static int testConcurrent = 0;

    private CountDownLatch countDownLatch = null;

    @Autowired
    private ZzLock zzLock;

    @Test
    public void testReentrant() {
        if (zzLock.acquire("l1")){
            if (zzLock.acquire("l2")){
                if (zzLock.acquire("l3")){
                    System.out.println("all lock acquired");
                }
            }
        }
        zzLock.release();
        zzLock.release();
        zzLock.release();
//        zzLock.releaseAll();
    }


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
                    System.out.println(end - begin);
                    return null;
                });

            }).start();
        }
        countDownLatch.countDown();
        Thread.sleep(5000);
    }

    @Test
    public void tesMultiThreadWithLockWrapExplicit() throws InterruptedException {
        System.out.println("With zzlock thinWrap");
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(zzLock.thinWrap("test", () -> {
                    System.out.print("In thread value " + testConcurrent);
                    testConcurrent = ++testConcurrent;
                    System.out.println(" || Out thread value " + testConcurrent);
                    long end = System.currentTimeMillis();
                    System.out.println(end - begin);
                    return "";
                }));
            }).start();
        }
        countDownLatch.countDown();
        Thread.sleep(7000);
    }

}
