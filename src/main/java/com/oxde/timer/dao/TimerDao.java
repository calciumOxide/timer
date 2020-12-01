package com.oxde.timer.dao;

import com.oxde.timer.entity.Timer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static com.oxde.timer.entity.Timer.STATUS.*;


@RepositoryRestResource
@Transactional
public interface TimerDao extends JpaRepository<Timer, Long> {


    List<Timer> findByStatusInAndExpireLessThanEqual(List<Integer> status, Long expire, Pageable pageable);

    @Modifying
    @Query(value="update timer set status = 3 where status = 1 and id in :ids", nativeQuery = true)
    int cache(@Param(value = "ids") List<Long> list);

    @Modifying
    @Query(value="update timer set status = 1 where status = 3 and id in :ids", nativeQuery = true)
    int readuce(@Param(value = "ids") List<Long> list);

    @Modifying
    @Query(value="update timer set status = 5 where status in (1, 3) and id = :id", nativeQuery = true)
    int runing(@Param(value = "id") Long id);

    @Modifying
    @Query(value="update timer set status = 13 where status = 5 and id = :id", nativeQuery = true)
    int success(@Param(value = "id") Long id);

    @Modifying
    @Query(value="update timer set status = 11 where status = 5 and id = :id", nativeQuery = true)
    int fail(@Param(value = "id") Long id);

}
