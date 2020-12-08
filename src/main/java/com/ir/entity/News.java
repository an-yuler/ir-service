package com.ir.entity;

import com.ir.service.IndexServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.lucene.document.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

/**
 * @author yuler
 */
@Data
@AllArgsConstructor
public class News {
    private int id;
    private String category;
    private String url;
    private String title;
    private String articleAbstract;
    private Date date;
    private String articleSource;
    private String articleSourceUrl;
    private String content;
    private String editor;
    private int heat;
    /**
     * highlight title
     */
    private String highlightTitle;
    private String highlightContent;

    public News(Document doc, String highlightTitle, String highlightContent){
        this(doc);
        this.highlightTitle = highlightTitle;
        this.highlightContent = highlightContent;
    }

    News(Document doc){
        String t;
        this.id = (t = doc.get(IndexServiceImpl.NEWS_ID)) == null? -1: Integer.parseInt(t);
        this.category = doc.get(IndexServiceImpl.NEWS_CATEGORY);
        this.url = doc.get(IndexServiceImpl.NEWS_URL);
        this.title = doc.get(IndexServiceImpl.NEWS_TITLE);
        this.articleAbstract = doc.get(IndexServiceImpl.NEWS_ABSTRACT);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        try{
            this.date = (t = doc.get(IndexServiceImpl.NEWS_DATE)) == null? Date.from(Instant.ofEpochSecond(0)) :sdf.parse(t);
        }catch (ParseException e){
            e.printStackTrace();
        }

        this.articleSource = doc.get(IndexServiceImpl.NEWS_SOURCE);
        this.articleSourceUrl = doc.get(IndexServiceImpl.NEWS_SOURCE_URL);
        this.content = doc.get(IndexServiceImpl.NEWS_CONTENT);
        this.editor = doc.get(IndexServiceImpl.NEWS_EDITOR);
        this.heat = ((t = doc.get(IndexServiceImpl.NEWS_HEAT)) == null)? 0 : Integer.parseInt(t);
    }
}

