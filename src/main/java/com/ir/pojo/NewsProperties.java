package com.ir.pojo;

import lombok.Data;

import java.util.Map;

/**
 * @author yuler
 */
@Data
public class NewsProperties {
    private String indexPath;
    private Map<String, String> category;
}
