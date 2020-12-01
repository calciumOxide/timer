package com.oxde.timer.service;


import com.alibaba.fastjson.JSON;
import com.oxde.timer.dao.TimerCacheDao;
import com.oxde.timer.dao.TimerDao;
import com.oxde.timer.entity.Timer;
import com.oxde.timer.util.ListHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static com.oxde.timer.entity.Timer.STATUS.*;

@Slf4j
@Service
public class TimerService {

    private static long CACHE_EXPIRE = 2L * 60 * 60 * 1000;

    @Autowired
    private TimerDao timerDao;
    @Autowired
    private TimerCacheDao timerCacheDao;
    @Autowired
    private RestTemplate restTemplate;

    public List<String> init(int keySize) {
        List<String> keys = initKeys(keySize);
        initCache();
        return keys;
    }
    private List<String> initKeys(int size) {
        List<String> keys = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            keys.add(timerCacheDao.getTimerCacheKey("TIMER_" + i));
        }
        return keys;
    }
    private void initCache() {
        cache(Arrays.asList(WAIT, CACHE, RUNING));
    }

    public void cache() {
        cache(Arrays.asList(WAIT));
    }

    public void exec(String key) {
        List<Timer> list = timerCacheDao.pop(key);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(this::exec);
    }

    private void exec(Timer timer) {
        int runing = timerDao.runing(timer.getId());
        if (runing == 1) {
            String url = timer.getUrl();
            if (url.indexOf("?") > -1) {
                url += "&code=" + timer.getCode();
            } else {
                url += "?code=" + timer.getCode();
            }
            int retry = 5;
            while (true) {
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                    if (response.getStatusCodeValue() == HttpStatus.OK.value()) {
                        timerDao.success(timer.getId());
                        break;
                    }
                } catch (RestClientException e) {
                    if (retry <= 0) {
                        timerDao.fail(timer.getId());
                        log.error("callback err. timer={} except={}", JSON.toJSONString(timer), ExceptionUtils.getStackTrace(e));
                    }
                }
                retry--;
            }
        }
    }

    private void cache(List<Integer> status) {
        List<String> keys = timerCacheDao.listTimerCacheKey();
        if (CollectionUtils.isEmpty(keys)) {
            log.info("cache key list is empty.");
            return;
        }

        List<Timer> list = timerDao.findByStatusInAndExpireLessThanEqual(status, System.currentTimeMillis() + CACHE_EXPIRE, PageRequest.of(0, 200));
        if (CollectionUtils.isEmpty(list)) {
            log.info("timer list is empty.");
            return;
        }
        List<List<Timer>> lists = ListHelper.averageAssign(list, keys.size());
        for (int i = 0; i < lists.size(); i++) {
            timerCacheDao.push(keys.get(i % keys.size()), lists.get(i));
        }
        timerDao.cache(list.stream().map(Timer::getId).collect(Collectors.toList()));
    }
}
