package com.oxde.timer.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Data @Accessors(chain = true)
@ToString(callSuper = true)
@Entity
@Table(name = "timer")
public class Timer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "app")
    private Integer app;
    @Column(name = "code")
    private String code;
    @Column(name = "expire")
    private Long expire;
    @Column(name = "status", insertable = false)
    private Integer status;
    @Column(name = "url")
    private String url;
    @Column(name = "executed_at")
    private Date executedAt;
    @Column(name = "created_at", insertable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public static class STATUS {
        public final static Integer WAIT = 1;
        public final static Integer CACHE = 3;
        public final static Integer RUNING = 5;
        public final static Integer CANCLE = 7;
        public final static Integer FAIL = 11;
        public final static Integer SUCCESS = 13;
    }
}
