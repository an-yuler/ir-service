package com.ir.utils;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyDateTypeHandler implements TypeHandler<Date> {

    // 2020年12月11日 08:08:21
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd hh:mm:ss");

    /**
     * 从数据库到javaBean
     */
    @Override
    public Date getResult(ResultSet rs, String columnName) throws SQLException {

        String value = rs.getString(columnName);
        if (value == null) {
            return null;
        } else {

            Date date = null;
            try {
                date = simpleDateFormat.parse(value);
            } catch (ParseException e) {

                e.printStackTrace();
            }
            return date;
        }

    }

    @Override
    public Date getResult(ResultSet rs, int columnIndex) throws SQLException {

        //首先获取数据库中的值
        String value = rs.getString(columnIndex);
        if (value == null) {

            return null;
        } else {

            Date date = null;
            try {
                date = simpleDateFormat.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return date;
        }
    }

    @Override
    public Date getResult(CallableStatement cs, int columnIndex) throws SQLException {

        //首先获取数据库中的值
        String value = cs.getString(columnIndex);
        if (value == null) {

            return null;
        } else {

            Date date = null;
            try {
                date = simpleDateFormat.parse(value);
            } catch (ParseException e) {

                e.printStackTrace();
            }
            return date;
        }
    }

    /**
     * javabean到数据库
     * i 当前参数的位置
     * date 当前参数的Java对象
     * jdbcType 当前参数的数据库类型
     * date--string
     */
    @Override
    public void setParameter(PreparedStatement ps, int i, Date date, JdbcType jdbcType) throws SQLException {
        if (date == null) {
            //如果是空的话 就将其在数据库中存成null
            ps.setNull(i, Types.VARCHAR);
        } else {
            String str = simpleDateFormat.format(date);
            ps.setString(i, str);
        }
    }

}

