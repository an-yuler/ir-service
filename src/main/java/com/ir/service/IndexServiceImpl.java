package com.ir.service;

import com.ir.bo.News;
import com.ir.mapper.NewsMapper;
import com.ir.pojo.NewsBean;
import com.ir.utils.MybatisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuler
 */
@Slf4j
public class IndexServiceImpl implements IndexService{
    public static String NEWS_ID = "news_id";
    public static String NEWS_SRC = "news_src";
    public static String NEWS_URL = "news_url";
    public static String NEWS_TITLE = "news_title";
    public static String NEWS_ABSTRACT = "news_abstract";
    public static String NEWS_DATE = "news_date";
    public static String NEWS_SOURCE = "news_source";
    public static String NEWS_SOURCE_URL = "news_source_url";
    public static String NEWS_CONTENT = "news_content";
    public static String NEWS_EDITOR = "news_editor";
    public static String NEWS_HEAT = "news_heat";

    final private static String DIR_PATH = "./index";
    private NewsMapper newsMapper;

    /**
     * 创建索引
     */
    @Override
    public void index(){

        IndexWriter indexWriter = null;
        // Analyzer:
        try {
            Directory dir = FSDirectory.open(Paths.get(DIR_PATH));
            log.info("index path: " + dir);
            Analyzer analyzer = new SmartChineseAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            indexWriter = new IndexWriter(dir, iwc);

            // get all news from mapper
            List<NewsBean> newss;
            try (SqlSession session = MybatisUtils.getSqlSession()) {
                newsMapper = session.getMapper(NewsMapper.class);
                newss = newsMapper.getAllNewsBean();
            }
            log.info("Received " + newss.size() + " news from mysql.");
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

            FieldType indexAndStoreType = new FieldType() {{
                setTokenized(true);
                setStored(true);
                setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            }};

            indexAndStoreType.freeze();

            log.info("Index building.");
            for (NewsBean news : newss) {
                Document doc = new Document();
                // set field for document
                doc.add(new StoredField(NEWS_ID, String.valueOf(news.getId())));
                doc.add(new IntPoint((NEWS_ID), news.getId()));
                doc.add(new Field(NEWS_SRC, news.getSrc(), indexAndStoreType));
                doc.add(new StoredField(NEWS_URL, news.getUrl()));
                doc.add(new Field(NEWS_TITLE, news.getTitle(), indexAndStoreType));
                doc.add(new StoredField(NEWS_ABSTRACT, news.getArticleAbstract()));
                doc.add(new StoredField(NEWS_DATE, news.getDate().toString()));
                doc.add(new NumericDocValuesField(NEWS_DATE, news.getDate().getTime()));
                doc.add(new StoredField(NEWS_SOURCE, news.getArticleSource()));
                doc.add(new StoredField(NEWS_SOURCE_URL, news.getArticleSourceUrl()));
                doc.add(new StoredField(NEWS_EDITOR, news.getEditor()));
                doc.add(new Field(NEWS_CONTENT, news.getContent(), indexAndStoreType));
                doc.add(new StoredField(NEWS_HEAT, news.getHeat()));
                doc.add(new NumericDocValuesField(NEWS_HEAT, news.getHeat()));
                indexWriter.updateDocument(new Term(NEWS_ID, String.valueOf(news.getId())), doc);
            }
            log.info("Index completed.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (indexWriter != null){
                try {
                    indexWriter.close();
                    log.info("Index writer closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 根据新闻id获取新闻
     *
     * @param id 新闻id
     * @return 指定的新闻
     */
    @Override
    public News getNewsById(int id) {
        NewsBean newsBean;
        try (SqlSession session = MybatisUtils.getSqlSession()) {
            newsMapper = session.getMapper(NewsMapper.class);
            newsBean = newsMapper.getNewsBeanById(id);
        }
        return new News(newsBean);
    }

    /**
     * 根据新闻id获取10条相似的新闻
     *
     * @param id 新闻id
     * @return 与指定新闻相似的10条新闻
     */
    @Override
    public List<News> getNewsLike(int id) {
        return getNewsLike(id, 20);
    }


    @Override
    public List<News> getNewsLike(int id, int num) {
        List<News> res = new ArrayList<>();

        try {
            log.info("新闻在数据库中的唯一id: " + id);

            Analyzer analyzer = new SmartChineseAnalyzer();
            FSDirectory directory = FSDirectory.open(Paths.get(DIR_PATH));
            IndexReader ir = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(ir);
            // 先检索文档在lucene中的id
            Query query1 = IntPoint.newExactQuery(NEWS_ID, id);
            ScoreDoc[] scoreDocs1 = searcher.search(query1, 1).scoreDocs;
            int docNum = scoreDocs1[0].doc;

            log.info("新闻在索引中的唯一id: " + docNum);
            // 再检索跟该文档类似的文档
            MoreLikeThis mlt = new MoreLikeThis(ir);
            mlt.setFieldNames(new String[]{NEWS_CONTENT});
            mlt.setAnalyzer(analyzer);
            Query query2 = mlt.like(docNum);
            ScoreDoc[] scoreDocs = searcher.search(query2, num + 1).scoreDocs;

            for (int i = 0; i < scoreDocs.length && res.size() < num; i++) {
                if (scoreDocs[i].doc != docNum) {
                    res.add(new News(searcher.doc(scoreDocs[i].doc)));
                }
            }
            log.info("查找到" + res.size() + "条相似新闻");

            return res;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<News> search(String keyword, String category, int rank, int field) {
        return search(keyword, category, rank, field, 500);
    }


    /**
     * @param keyword  要检索的关键字
     * @param category 分类 "all", "netease"
     * @param rank     排序方式 1相关度, 2热度, 3时间
     * @param field    检索字段 0标题和全文, 1标题, 2全文
     * @return 返回一个News对象的列表
     */
    @Override
    public List<News> search(String keyword, String category, int rank, int field, int num) {
        Map<Integer, String> rankMap = new HashMap<Integer, String>() {{
            put(1, "按照相关度排序");
            put(2, "按照热度排序");
            put(3, "按照时间排序");
        }};
        Map<Integer, String> fieldMap = new HashMap<Integer, String>() {{
            put(0, "标题和全文");
            put(1, "标题");
            put(2, "全文");
        }};

        List<News> res = new ArrayList<>();

        IndexSearcher indexSearcher;
        IndexReader reader = null;
        try {
            Directory directory = FSDirectory.open(Paths.get(DIR_PATH));
            reader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(reader);
            Analyzer analyzer = new SmartChineseAnalyzer();

            // log
            log.info("关键词：" + keyword + " 新闻网：" + category + " 排序方式："
                    + rankMap.get(rank) + " 检索字段：" + fieldMap.get(field));
            Query query = getQuery(analyzer, keyword, category, field);
            assert query != null;
            log.info("Query statement: " + query.toString());

            TopDocs topDocs;
            Sort sort;

            switch (rank) {
                case 1:
                    topDocs = indexSearcher.search(query, num);
                    break;
                case 2:
                    sort = new Sort(new SortField(NEWS_HEAT, SortField.Type.INT, true));
                    topDocs = indexSearcher.search(query, num, sort);
                    break;
                case 3:
                    sort = new Sort(new SortField(NEWS_DATE, SortField.Type.LONG, true));
                    topDocs = indexSearcher.search(query, num, sort);
                    break;
                default:
                    return null;
            }

//            TotalHits totalHits = topDocs.totalHits;
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            log.info("Number of search results: " + String.valueOf(scoreDocs.length));


            // 高亮显示
            QueryScorer scorer = new QueryScorer(query);
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
            SimpleHTMLFormatter shf = new SimpleHTMLFormatter("<span style=\"color:red;\">", "</span>");
            Highlighter highlighter = new Highlighter(shf, scorer);
            highlighter.setTextFragmenter(fragmenter);


            try (SqlSession session = MybatisUtils.getSqlSession()) {
                newsMapper = session.getMapper(NewsMapper.class);

                for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                    Document document = indexSearcher.doc(scoreDocs[i].doc);
                    TokenStream tokenStrem;

                    String title = document.get(NEWS_TITLE);
                    tokenStrem = analyzer.tokenStream(NEWS_TITLE, new StringReader(title));
                    String htitle = highlighter.getBestFragment(tokenStrem, title);

                    String content = document.get(NEWS_CONTENT);
                    tokenStrem = analyzer.tokenStream(NEWS_CONTENT, new StringReader(content));
                    String hcontent = highlighter.getBestFragment(tokenStrem, content);

                    if (htitle == null) {
                        htitle = title;
                    }

                    if (hcontent == null) {
                        hcontent = content;
                    }


                    News news = new News(document, htitle, hcontent);
                    res.add(news);
                }
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
            QueryParser qp = new QueryParser(NEWS_SRC, analyzer);
            Query query = qp.parse(category);
            BooleanClause bc = new BooleanClause(query, BooleanClause.Occur.MUST);
            builder.add(bc);
        }

        if (field == 0) {
            QueryParser qp1= new QueryParser(NEWS_TITLE, analyzer);
            Query query1 = qp1.parse(keyword);
            QueryParser qp3 = new QueryParser(NEWS_CONTENT, analyzer);
            Query query3 = qp3.parse(keyword);

            BooleanClause bc1 = new BooleanClause(query1, BooleanClause.Occur.SHOULD);
            BooleanClause bc3 = new BooleanClause(query3, BooleanClause.Occur.SHOULD);
            bq = new BooleanQuery.Builder().add(bc1).add(bc3).build();
        } else {
            String newsField;
            switch (field) {
                case 1:
                    newsField = NEWS_TITLE;
                    break;
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
