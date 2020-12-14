package com.ir.bo;

import com.ir.pojo.NewsBean;
import com.ir.service.IndexServiceImpl;
import lombok.Data;
import org.apache.lucene.document.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author yuler
 */
@Data
public class News {

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
    /**
     * highlight title
     */
    private String highlightTitle;
    private String highlightContent;

    public News(NewsBean newsBean) {
        this.id = newsBean.getId();
        this.src = newsBean.getSrc();
        this.url = newsBean.getUrl();
        this.title = newsBean.getTitle();
        this.articleAbstract = newsBean.getArticleAbstract();
        this.date = newsBean.getDate();
        this.articleSource = newsBean.getArticleSource();
        this.articleSourceUrl = newsBean.getArticleSourceUrl();
        this.content = newsBean.getContent();
        this.editor = newsBean.getEditor();
        this.heat = newsBean.getHeat();
    }

    public News(Document document, String highlightTitle, String highlightContent) {
        this(document);
        this.highlightTitle = highlightTitle;
        this.highlightContent = highlightContent;
    }

    public News(Document doc) {
        String t;
        this.id = (t = doc.get(IndexServiceImpl.NEWS_ID)) == null ? -1 : Integer.parseInt(t);
        this.src = doc.get(IndexServiceImpl.NEWS_SRC);
        this.url = doc.get(IndexServiceImpl.NEWS_URL);
        this.title = doc.get(IndexServiceImpl.NEWS_TITLE);
        this.articleAbstract = doc.get(IndexServiceImpl.NEWS_ABSTRACT);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        try {
            Date date1 = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                    .parse(doc.get(IndexServiceImpl.NEWS_DATE));
            this.date = sdf.parse(sdf.format(date1));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        this.articleSource = doc.get(IndexServiceImpl.NEWS_SOURCE);
        this.articleSourceUrl = doc.get(IndexServiceImpl.NEWS_SOURCE_URL);
        this.content = doc.get(IndexServiceImpl.NEWS_CONTENT);
        this.editor = doc.get(IndexServiceImpl.NEWS_EDITOR);
        this.heat = ((t = doc.get(IndexServiceImpl.NEWS_HEAT)) == null) ? 0 : Integer.parseInt(t);
    }
}
