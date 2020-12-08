package com.ir.config;

import com.ir.entity.NewsProperties;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class PropertiesFactory {
    private static NewsProperties props = null;

    public NewsProperties getProps(){
        if(props == null){
            Yaml yaml = new Yaml(new Constructor(NewsProperties.class));

//            InputStream inputStream = this.getClass()
//                    .getClassLoader()
//                    .getResourceAsStream("ir.yaml");
//            props = yaml.load(inputStream);
            try {
                props = yaml.load(new FileInputStream("ir.yaml"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return props;
    }
}
