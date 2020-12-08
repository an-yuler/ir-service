package com.ir.service;


import com.ir.entity.News;

import java.util.List;

public interface IndexService {

    void index();
    List<News> search(String keyword, String category, int rank, int field);
}
