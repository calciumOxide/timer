package com.oxde.timer.listener;

import com.oxde.timer.service.TimerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.oxde.timer.constant.TimerConstant.*;


@Slf4j
@Component
public class ApplicationEventListener implements ApplicationListener<ApplicationEvent> {

    @Autowired
    private TimerService timerService;

    private ThreadPoolExecutor threadPoolExecutor;

    @PostConstruct
    public void init() {
        ArrayBlockingQueue queue = new ArrayBlockingQueue(10);
        threadPoolExecutor = new ThreadPoolExecutor(8, 10, 6, TimeUnit.MINUTES, queue);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationReadyEvent) {
            int keySize = 5;
            List<String> keys = timerService.init(keySize);
            for (int i=0; i<keySize; i++) {
                final String key = keys.get(i);
                threadPoolExecutor.execute(() -> {
                    while (true) {
                        try {
                            timerService.exec(key);
                        } catch (Exception e) {
                            log.error("run exec err. except={}", ExceptionUtils.getStackTrace(e));
                        }
                        try {
                            Thread.sleep(EXEC_SLEEP);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            threadPoolExecutor.execute(() -> {
                while (true) {
                    try {
                        timerService.cache();
                    } catch (Exception e) {
                        log.error("run cache err. except={}", ExceptionUtils.getStackTrace(e));
                    }
                    try {
                        Thread.sleep(CACHE_SLEEP);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        if (applicationEvent instanceof ApplicationFailedEvent) {
            threadPoolExecutor.shutdown();
        }
    }
}
