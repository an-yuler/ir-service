package com.ir.dao;

import com.ir.config.PropertiesFactory;
import com.ir.entity.News;
import com.ir.entity.NewsList;
import com.ir.entity.NewsProperties;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.DateConverter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yuler
 */
public class XmlNewsMapper implements NewsMapper {

    private static NewsProperties pros = new PropertiesFactory().getProps();

    /**
     * 根据ir.yaml配置，读取所有的新闻数据
     * @return News对象的列表
     */
    @Override
    public List<News> getAllNews() {
        List<News> res = new ArrayList<>();
        for(Map.Entry<String, String> entry : pros.getCategory().entrySet()){
           res.addAll(getNewsByName(entry.getKey(), entry.getValue()));
        }
//        for (String value : pros.getCategory().values()) {
//            res.addAll(getNewsByName(value));
//        }
        return res;
    }

    /**
     *
     * @param name 新闻
     * @param filePath 新闻数据存放的位置
     * @return News对象的列表
     */
    @Override
    public List<News> getNewsByName(String name, String filePath) {
        Class<?>[] classes = new Class[] { News.class, NewsList.class };
        XStream xStream = new XStream();
        xStream.registerConverter(new DateConverter("yy-MM-dd HH-mm-ss", new String[]{"yy-MM-dd HH:mm:ss"}));
        XStream.setupDefaultSecurity(xStream);
        xStream.allowTypes(classes);

        xStream.alias("items", NewsList.class);
        xStream.alias("item", News.class);
        xStream.addImplicitArray(NewsList.class, "newsList", News.class);
        xStream.aliasField("abstract", News.class, "articleAbstract");
        xStream.aliasField("article_source", News.class, "articleSource");
        xStream.aliasField("article_source_url", News.class, "articleSourceUrl");


        NewsList newsList = null;

//        InputStream ins = XMLNewsMapper.class.getClassLoader().getResourceAsStream(filePath);
//        newsList = ((NewsList) xStream.fromXML(ins));
        try {
            newsList = ((NewsList) xStream.fromXML(new FileInputStream(filePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        assert newsList != null;
        List<News> res = newsList.getNewsList();
        res.forEach(x -> x.setCategory(name));
        return res;
    }
}
