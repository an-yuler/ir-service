package com.ir.utils;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MybatisUtils {


    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            final String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            InputStream propsStream = new FileInputStream("db.properties");

            Properties props = new Properties();
            props.load(propsStream);

            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession();
    }
}
