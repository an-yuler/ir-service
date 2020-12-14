package com.ir.mapper;

import com.ir.pojo.NewsBean;

import java.util.List;

public interface NewsMapper {
    NewsBean getNewsBeanById(int id);

    List<NewsBean> getAllNewsBean();
}
