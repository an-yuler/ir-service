package com.ir.service;

import com.ir.config.PropertiesFactory;
import com.ir.dao.NewsMapper;
import com.ir.entity.News;
import com.ir.entity.NewsProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yuler
 */
@Slf4j
public class IndexServiceImpl implements IndexService{
    public static String NEWS_ID = "news_id";
    public static String NEWS_CATEGORY = "news_category";
    public static String NEWS_URL = "news_url";
    public static String NEWS_TITLE = "news_title";
    public static String NEWS_ABSTRACT = "news_abstract";
    public static String NEWS_DATE = "news_date";
    public static String NEWS_SOURCE = "news_source";
    public static String NEWS_SOURCE_URL = "news_source_url";
    public static String NEWS_CONTENT = "news_content";
    public static String NEWS_EDITOR = "news_editor";
    public static String NEWS_HEAT = "news_heat";

    private static NewsProperties props = new PropertiesFactory().getProps();
    private NewsMapper newsMapper;

    public void setNewsMapper(NewsMapper newsMapper) {
        this.newsMapper = newsMapper;
    }

    /**
     * 创建索引
     */
    @Override
    public void index(){
        IndexWriter indexWriter = null;

        String dirPath = props.getIndexPath();
        // Analyzer:

        try {
            Directory dir = FSDirectory.open(Paths.get(dirPath));
            Analyzer analyzer = new SmartChineseAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            indexWriter = new IndexWriter(dir, iwc);

            // get all news from dao
            List<News> newss = newsMapper.getAllNews();
            /*         index|store|
             * id:      no  | yes
             * url:     no  | yes
             * title:   yes | yes
             * abstract:yes | yes
             * date:    no  | yes
             * source:  no  | yes
             * source_url: no | yes
             * content: no | yes
             * editor: no | yes
             * heat:   no | yes
             */

            FieldType indexAndStoreType = new FieldType(){{
                setTokenized(true);
                setStored(true);
                setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            }};
            indexAndStoreType.freeze();

            for (News news : newss) {
                Document doc = new Document();
                // set field for document
                doc.add(new StoredField(NEWS_ID, news.getId()));
                doc.add(new Field(NEWS_CATEGORY, news.getCategory(), indexAndStoreType));
                doc.add(new StoredField(NEWS_URL, news.getUrl()));
                doc.add(new Field(NEWS_TITLE, news.getTitle(), indexAndStoreType));
                doc.add(new Field(NEWS_ABSTRACT, news.getArticleAbstract(), indexAndStoreType));
                doc.add(new StoredField(NEWS_DATE, news.getDate().toString()));
                doc.add(new StoredField(NEWS_SOURCE, news.getArticleSource()));
                doc.add(new StoredField(NEWS_SOURCE_URL, news.getArticleSourceUrl()));
                doc.add(new StoredField(NEWS_EDITOR, news.getEditor()));
                doc.add(new Field(NEWS_CONTENT, news.getContent(), indexAndStoreType));
                doc.add(new NumericDocValuesField(NEWS_HEAT, news.getHeat()));
                indexWriter.addDocument(doc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (indexWriter != null){
                try {
                    indexWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param keyword 要检索的关键字
     * @param category 分类 "all", "netease"
     * @param rank 排序方式 1相关度, 2热度, 3时间
     * @param field 检索字段 0标题和全文, 1标题, 2全文
     * @return 返回一个News对象的列表
     */
    @Override
    public List<News> search(String keyword, String category, int rank, int field) {
        List<News> res = new ArrayList<>();

        IndexSearcher indexSearcher;

        IndexReader reader = null;
        try {
            Directory directory = FSDirectory.open(Paths.get(props.getIndexPath()));
            reader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(reader);
            Analyzer analyzer = new SmartChineseAnalyzer();

            log.info(keyword  + " " +  category + " "  + rank + " " + field);
            Query query = getQuery(analyzer,keyword, category, field);
            assert query != null;
            log.info(query.toString());

            TopDocs topDocs;
            Sort sort;

            switch(rank){
                case 1:
                    topDocs = indexSearcher.search(query, 500);
                    break;
                case 2:
                    sort = new Sort(new SortField(NEWS_HEAT, SortField.Type.INT, true));
                    topDocs = indexSearcher.search(query, 500, sort);
                    break;
                case 3:
                    sort = new Sort(new SortField(NEWS_DATE, SortField.Type.INT, true));
                    topDocs = indexSearcher.search(query, 500, sort);
                    break;
                default:
                    return null;
            }

//            TotalHits totalHits = topDocs.totalHits;
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            log.info(String.valueOf(scoreDocs.length));


            // 高亮显示
            QueryScorer scorer = new QueryScorer(query);
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
            SimpleHTMLFormatter shf = new SimpleHTMLFormatter("<span style=\"color:red;\">", "</span>");
            Highlighter highlighter = new Highlighter(shf, scorer);
            highlighter.setTextFragmenter(fragmenter);
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                Document document = indexSearcher.doc(scoreDocs[i].doc);
                TokenStream tokenStrem;

                String title = document.get(NEWS_TITLE);
                tokenStrem = analyzer.tokenStream(NEWS_TITLE, new StringReader(title));
                String htitle = highlighter.getBestFragment(tokenStrem, title);

                String content = document.get(NEWS_CONTENT);
                tokenStrem = analyzer.tokenStream(NEWS_CONTENT, new StringReader(content));
                String hcontent  = highlighter.getBestFragment(tokenStrem, content);

                if(htitle == null){
                    htitle = title;
                }

                if(hcontent == null){
                    hcontent = content;
                }

                News news = new News(document, htitle, hcontent);
                res.add(news);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(reader != null){
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    /**
     *  根据category和filed拿到query语句
     * @param analyzer 分词器
     * @param keyword 搜索词
     * @param category 新闻所属分类
     * @param field 检索字段
     * @return Query语句
     */
    private Query getQuery(Analyzer analyzer, String keyword, String category, int field) throws ParseException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        BooleanQuery bq;
        if (!"all".equals(category)) {
            QueryParser qp = new QueryParser(NEWS_CATEGORY, analyzer);
            Query query = qp.parse(category);
            BooleanClause bc = new BooleanClause(query, BooleanClause.Occur.MUST);
            builder.add(bc);
        }

        if (field == 0) {
            QueryParser qp1= new QueryParser(NEWS_TITLE, analyzer);
            Query query1 = qp1.parse(keyword);
//            QueryParser qp2 = new QueryParser(NEWS_ABSTRACT, analyzer);
//            Query query2 = qp2.parse(keyword);
            QueryParser qp3 = new QueryParser(NEWS_CONTENT, analyzer);
            Query query3 = qp3.parse(keyword);

            BooleanClause bc1 = new BooleanClause(query1, BooleanClause.Occur.SHOULD);
//            BooleanClause bc2 = new BooleanClause(query2, BooleanClause.Occur.SHOULD);
            BooleanClause bc3 = new BooleanClause(query3, BooleanClause.Occur.SHOULD);
//            bq = new BooleanQuery.Builder().add(bc1).add(bc2).add(bc3).build();
            bq = new BooleanQuery.Builder().add(bc1).add(bc3).build();
        } else {
            String newsField;
            switch (field) {
                case 1:
                    newsField = NEWS_TITLE;
                    break;
//                case 2:
//                    newsField = NEWS_ABSTRACT;
//                    break;
                case 2:
                    newsField = NEWS_CONTENT;
                    break;
                default:
                    return null;
            }
            QueryParser qp= new QueryParser(newsField, analyzer);
            Query query = qp.parse(keyword);
            BooleanClause bc = new BooleanClause(query, BooleanClause.Occur.MUST);
            bq = new BooleanQuery.Builder().add(bc).build();
        }
        return builder.add(bq, BooleanClause.Occur.MUST).build();
    }
}
