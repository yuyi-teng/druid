package com.alibaba.druid.pool.vendor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.alibaba.druid.logging.Log;
import com.alibaba.druid.logging.LogFactory;
import com.alibaba.druid.pool.ValidConnectionChecker;
import com.alibaba.druid.util.JdbcUtils;

public class MySQLValidConnectionChecker implements ValidConnectionChecker, Serializable {

    private static final long serialVersionUID    = 1L;
    private static final Log  LOG                 = LogFactory.getLog(MySQLValidConnectionChecker.class);

    private Method            ping;
    private boolean           driverHasPingMethod = false;

    // The timeout (apparently the timeout is ignored?)
    private static Object[]   params              = new Object[] {};

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MySQLValidConnectionChecker(){
        try {
            Class mysqlConnection = Thread.currentThread().getContextClassLoader().loadClass("com.mysql.jdbc.Connection");
            ping = mysqlConnection.getMethod("ping", new Class[] {});
            if (ping != null) {
                driverHasPingMethod = true;
            }
        } catch (Exception e) {
            LOG.warn("Cannot resolve com.mysq.jdbc.Connection.ping method.  Will use 'SELECT 1' instead.", e);
        }
    }

    public SQLException isValidConnection(Connection c) {
        // if there is a ping method then use it, otherwise just use a 'SELECT 1' statement
        if (driverHasPingMethod) {
            try {
                ping.invoke(c, params);
            } catch (Exception e) {
                if (e instanceof SQLException) {
                    return (SQLException) e;
                } else {
                    LOG.warn("Unexpected error in ping", e);
                    return new SQLException("ping failed: " + e.toString());
                }
            }

        } else {

            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = c.createStatement();
                rs = stmt.executeQuery("SELECT 1");
            } catch (Exception e) {
                if (e instanceof SQLException) {
                    return (SQLException) e;
                } else {
                    LOG.warn("Unexpected error in ping (SELECT 1)", e);
                    return new SQLException("ping (SELECT 1) failed: " + e.toString());
                }
            } finally {
                JdbcUtils.close(rs);
                JdbcUtils.close(stmt);
            }

        }
        return null;
    }

}
