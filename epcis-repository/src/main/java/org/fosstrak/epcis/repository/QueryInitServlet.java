/*
 * Copyright (C) 2007 ETH Zurich
 *
 * This file is part of Accada (www.accada.org).
 *
 * Accada is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * Accada is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Accada; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package org.accada.epcis.repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.sql.DataSource;
import javax.xml.ws.Endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.Log4jLogger;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

/**
 * TODO: javadoc
 * 
 * @author Marco Steybe
 */
public class QueryInitServlet extends CXFNonSpringServlet {

    private static final String APP_CONFIG_LOCATION = "appConfigLocation";
    private static final String PROP_MAX_QUERY_ROWS = "maxQueryResultRows";
    private static final String PROP_MAX_QUERY_TIME = "maxQueryExecutionTime";
    private static final String PROP_INSERT_MISSING_VOC = "insertMissingVoc";
    private static final String PROP_TRIGGER_CHECK_SEC = "trigger.condition.check.sec";
    private static final String PROP_TRIGGER_CHECK_MIN = "trigger.condition.check.min";
    private static final String JNDI_DATASOURCE_NAME = "java:comp/env/jdbc/EPCISDB";
    private static final String WS_SERVICE_ADDRESS = "/query";

    private static final Log LOG = LogFactory.getLog(QueryInitServlet.class);

    static {
        LogUtils.setLoggerClass(Log4jLogger.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.cxf.transport.servlet.CXFNonSpringServlet#loadBus(javax.servlet.ServletConfig)
     */
    public void loadBus(ServletConfig servletConfig) throws ServletException {
        super.loadBus(servletConfig);
        BusFactory.setDefaultBus(getBus());
        getBus().getInInterceptors().add(new LoggingInInterceptor());
        getBus().getOutInterceptors().add(new LoggingOutInterceptor());
        //getBus().getOutFaultInterceptors().add(new FaultOutInterceptor());
        //getBus().getOutFaultInterceptors().add(new LoggingOutInterceptor());
        //getBus().getInFaultInterceptors().add(new LoggingInInterceptor());
        setupQueryOperationsModule(servletConfig);
    }

    private void setupQueryOperationsModule(ServletConfig servletConfig) {
        Properties props = loadApplicationProperties(servletConfig);
        DataSource dataSource = loadDataSource();

        LOG.debug("Initializing query operations module");
        QueryOperationsModule service = new QueryOperationsModule();
        service.setMaxQueryRows(Integer.parseInt(props.getProperty(PROP_MAX_QUERY_ROWS)));
        service.setMaxQueryTime(Integer.parseInt(props.getProperty(PROP_MAX_QUERY_TIME)));
        service.setTriggerConditionMinutes(props.getProperty(PROP_TRIGGER_CHECK_MIN));
        service.setTriggerConditionSeconds(props.getProperty(PROP_TRIGGER_CHECK_SEC));
        service.setDataSource(dataSource);

        LOG.debug("Publishing query operations module service at " + WS_SERVICE_ADDRESS);
        Endpoint.publish(WS_SERVICE_ADDRESS, service);
    }

    private Properties loadApplicationProperties(ServletConfig servletConfig) {
        // read application properties from servlet context
        ServletContext ctx = servletConfig.getServletContext();
        String path = ctx.getRealPath("/");
        String appConfigFile = ctx.getInitParameter(APP_CONFIG_LOCATION);
        Properties properties = new Properties();
        try {
            InputStream is = new FileInputStream(path + appConfigFile);
            properties.load(is);
            is.close();
            LOG.info("Loaded application properties from " + path + appConfigFile);
        } catch (IOException e) {
            LOG.error("Unable to load application properties from " + path + appConfigFile, e);
        }
        return properties;
    }

    private DataSource loadDataSource() {
        DataSource dataSource = null;
        try {
            // read data source from application context via JNDI
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(JNDI_DATASOURCE_NAME);
            LOG.info("Loaded data source via JNDI from " + JNDI_DATASOURCE_NAME);
        } catch (NamingException e) {
            LOG.error("Unable to load data source via JNDI from " + JNDI_DATASOURCE_NAME, e);
        }
        return dataSource;
    }

    // try {
    // String delimiter = dbconnection.getMetaData().getIdentifierQuoteString();
    // LOG.debug("Resolved string delimiter used to quote SQL identifiers as '"
    // + delimiter + "'.");
    //
    // Map<String, QuerySubscriptionScheduled> subscribedMap = (HashMap<String,
    // QuerySubscriptionScheduled>) ctx.getAttribute("subscribedMap");
    // if (subscribedMap != null && log.isDebugEnabled()) {
    // LOG.debug("Restored " + subscribedMap.size() + " subscriptions from
    // servlet context");
    // }
    // }catch (SQLException e) {
    // String msg = "An error connecting to the database occurred";
    // LOG.error(msg, e);
    // }
}