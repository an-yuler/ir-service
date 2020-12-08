package com.ir.dao;

import com.ir.entity.News;

import java.util.List;

public interface NewsMapper {

    List<News> getNewsByName(String name, String filePath);
    List<News> getAllNews();
}
