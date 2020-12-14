package com.ir.service;

import com.ir.bo.News;

import java.util.List;

public interface IndexService {

    void index();

    News getNewsById(int id);

    List<News> getNewsLike(int id);

    List<News> getNewsLike(int id, int num);

    List<News> search(String keyword, String category, int rank, int field);

    List<News> search(String keyword, String category, int rank, int field, int num);
}
