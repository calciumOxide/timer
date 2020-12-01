package com.oxde.timer.dao;

import com.oxde.timer.entity.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.oxde.timer.constant.TimerConstant.*;

@Component
public class TimerCacheDao {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private TimerDao timerDao;

    public String getTimerCacheKey(String key) {
        Double score = null;
        if (key != null) {
            score = redisTemplate.opsForZSet().score(TIMER_CACHE_KEY, key);
        }
        if (score == null || score < System.currentTimeMillis() - KEY_OFFSET) {
            key = UUID.randomUUID().toString();
        }
        redisTemplate.opsForZSet().add(TIMER_CACHE_KEY, key, System.currentTimeMillis());
        return key;
    }

    public List<String> listTimerCacheKey() {
        reduce();
        Set<String> keys = redisTemplate.opsForZSet().rangeByScore(TIMER_CACHE_KEY, System.currentTimeMillis() - KEY_OFFSET, System.currentTimeMillis() + KEY_OFFSET);
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(keys);
    }

    public List<Timer> pop(final String key) {
        Set<String> ids = redisTemplate.opsForZSet().rangeByScore(queueName(key), 0, System.currentTimeMillis() + TIMER_OFFSET);
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>(0);
        }
        redisTemplate.opsForZSet().remove(queueName(key), ids.toArray());
        return timerDao.findAllById(ids.stream().map(Long::new).collect(Collectors.toList()));
    }

    public void push(final String key, List<Timer> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        list.forEach(t -> redisTemplate.opsForZSet().add(queueName(key), t.getId().toString(), t.getExpire().doubleValue()));

    }

    private void reduce() {
        double limiter = System.currentTimeMillis() - KEY_OFFSET - REDUNDANT_OFFSET;
        Set<String> expireKeys = redisTemplate.opsForZSet().rangeByScore(TIMER_CACHE_KEY, 0, limiter);
        if (CollectionUtils.isEmpty(expireKeys)) {
            return;
        }
        Set<String> keys = redisTemplate.opsForZSet().rangeByScore(TIMER_CACHE_KEY, limiter + REDUNDANT_OFFSET, System.currentTimeMillis());
        if (CollectionUtils.isEmpty(keys)) {
            for (String expireKey : expireKeys) {
                Set<String> ids = redisTemplate.opsForZSet().range(queueName(expireKey), 0, -1);
                if (CollectionUtils.isEmpty(ids)) {
                    continue;
                }
                timerDao.readuce(ids.stream().map(Long::new).collect(Collectors.toList()));
            }
        } else {
            List<String> keyList = new ArrayList<>(keys);
            List<String> expireKeyList = new ArrayList<>(expireKeys);
            for (int i = 0; i < expireKeyList.size(); i++) {
                String expireKey = expireKeyList.get(i);
                Set<String> ids = redisTemplate.opsForZSet().range(queueName(expireKey), 0, -1);
                if (CollectionUtils.isEmpty(ids)) {
                    continue;
                }
                push(keyList.get(i % keyList.size()), ids.stream().map(id -> new Timer().setId(Long.valueOf(id))).collect(Collectors.toList()));
            }
        }
        redisTemplate.opsForZSet().removeRangeByScore(TIMER_CACHE_KEY, 0, limiter);
    }

    private String queueName(String key) {
        return TIMER_CACHE_KEY + "::" + key;
    }
}
