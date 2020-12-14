package com.ir.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @author yuler
 */
@Data
public class NewsBean {
    private int id;
    private String src;
    private String url;
    private String title;
    private String articleAbstract;
    private Date date;
    private String articleSource;
    private String articleSourceUrl;
    private String content;
    private String editor;
    private int heat;
}

