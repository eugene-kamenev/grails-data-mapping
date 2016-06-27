/* Copyright (C) 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.persistence.FlushModeType;

import org.grails.datastore.mapping.proxy.ProxyHandler;
import org.grails.orm.hibernate.proxy.HibernateProxyHandler;
import org.grails.orm.hibernate.query.HibernateQuery;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.query.jpa.JpaQueryBuilder;
import org.grails.datastore.mapping.query.jpa.JpaQueryInfo;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Session implementation that wraps a Hibernate {@link Session}.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class HibernateSession extends AbstractHibernateSession {

    ProxyHandler proxyHandler = new HibernateProxyHandler();

    public HibernateSession(HibernateDatastore hibernateDatastore) {
        this(hibernateDatastore, hibernateDatastore.getSessionFactory());
    }

    public HibernateSession(HibernateDatastore hibernateDatastore, SessionFactory sessionFactory, int defaultFlushMode) {
        super(hibernateDatastore, sessionFactory);

        hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, (HibernateDatastore) getDatastore());
    }
    
    public HibernateSession(HibernateDatastore hibernateDatastore, SessionFactory sessionFactory) {
        this(hibernateDatastore, sessionFactory, hibernateDatastore.getDefaultFlushMode());
    }    

    @Override
    public Serializable getObjectIdentifier(Object instance) {
        if(instance == null) return null;
        if(proxyHandler.isProxy(instance)) {
            return ((HibernateProxy)instance).getHibernateLazyInitializer().getIdentifier();
        }
        Class<?> type = instance.getClass();
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(type);
        final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(type.getName());
        if(persistentEntity != null) {
            return (Serializable) cpf.getPropertyValue(instance, persistentEntity.getIdentity().getName());
        }
        return null;
    }

    /**
     * Deletes all objects matching the given criteria.
     *
     * @param criteria The criteria
     * @return The total number of records deleted
     */
    public long deleteAll(final QueryableCriteria criteria) {
        return getHibernateTemplate().execute(new HibernateCallback<Integer>() {
            public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                JpaQueryBuilder builder = new JpaQueryBuilder(criteria);
                builder.setHibernateCompatible(true);
                JpaQueryInfo jpaQueryInfo = builder.buildDelete();

                String hibernateQuery = jpaQueryInfo.getQuery();
                org.hibernate.Query query = session.createQuery(hibernateQuery);
                getHibernateTemplate().applySettings(query);

                List parameters = jpaQueryInfo.getParameters();
                if (parameters != null) {
                    for (int i = 0, count = parameters.size(); i < count; i++) {
                        query.setParameter(JpaQueryBuilder.PARAMETER_NAME_PREFIX + (i+1), parameters.get(i));
                    }
                }
                return query.executeUpdate();
            }
        });
    }

    /**
     * Updates all objects matching the given criteria and property values.
     *
     * @param criteria The criteria
     * @param properties The properties
     * @return The total number of records updated
     */
    public long updateAll(final QueryableCriteria criteria, final Map<String, Object> properties) {
        return getHibernateTemplate().execute(new HibernateCallback<Integer>() {
            public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                JpaQueryBuilder builder = new JpaQueryBuilder(criteria);
                builder.setHibernateCompatible(true);
                JpaQueryInfo jpaQueryInfo = builder.buildUpdate(properties);

                org.hibernate.Query query = session.createQuery(jpaQueryInfo.getQuery());
                getHibernateTemplate().applySettings(query);
                List parameters = jpaQueryInfo.getParameters();
                if (parameters != null) {
                    for (int i = 0, count = parameters.size(); i < count; i++) {
                        query.setParameter(JpaQueryBuilder.PARAMETER_NAME_PREFIX + (i+1), parameters.get(i));
                    }
                }
                return query.executeUpdate();
            }
        });
    }

    public List retrieveAll(final Class type, final Iterable keys) {
        final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(type.getName());
        return getHibernateTemplate().execute(new HibernateCallback<List>() {
            public List doInHibernate(org.hibernate.Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(type);
                getHibernateTemplate().applySettings(criteria);
                return criteria.add(
                        Restrictions.in(persistentEntity.getIdentity().getName(), getIterableAsCollection(keys)))
                        .list();
            }
        });
    }

    public Query createQuery(Class type) {
        return createQuery(type, null);
    }

    @Override
    public Query createQuery(Class type, String alias) {
        final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(type.getName());
        org.hibernate.classic.Session currentSession = getHibernateTemplate().getSessionFactory().getCurrentSession();
        final Criteria criteria = alias != null ? currentSession.createCriteria(type, alias) : currentSession.createCriteria(type);
        getHibernateTemplate().applySettings(criteria);
        return new HibernateQuery(criteria, this, persistentEntity);
    }

    protected GrailsHibernateTemplate getHibernateTemplate() {
        return (GrailsHibernateTemplate)getNativeInterface();
    }

    public void setFlushMode(FlushModeType flushMode) {
        if (flushMode == FlushModeType.AUTO) {
            hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_AUTO);
        }
        else if (flushMode == FlushModeType.COMMIT) {
            hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_COMMIT);
        }
    }

    public FlushModeType getFlushMode() {
        switch (hibernateTemplate.getFlushMode()) {
            case HibernateTemplate.FLUSH_AUTO:   return FlushModeType.AUTO;
            case HibernateTemplate.FLUSH_COMMIT: return FlushModeType.COMMIT;
            case HibernateTemplate.FLUSH_ALWAYS: return FlushModeType.AUTO;
            default:                             return FlushModeType.AUTO;
        }
    }
}
