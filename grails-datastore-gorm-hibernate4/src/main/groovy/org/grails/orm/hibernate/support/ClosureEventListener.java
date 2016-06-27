/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.support;

import groovy.lang.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.gorm.GormValidateable;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassUtils;
import org.grails.datastore.mapping.validation.ValidationException;
import org.grails.orm.hibernate.AbstractHibernateGormValidationApi;
import org.grails.orm.hibernate.cfg.GrailsDomainBinder;
import org.grails.orm.hibernate.cfg.Mapping;
import org.grails.datastore.gorm.support.BeforeValidateHelper.BeforeValidateEventTriggerCaller;
import org.grails.datastore.gorm.support.EventTriggerCaller;
import org.grails.datastore.gorm.timestamp.DefaultTimestampProvider;
import org.grails.datastore.gorm.timestamp.TimestampProvider;
import org.grails.datastore.mapping.engine.event.ValidationEvent;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.ExecutableList;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * <p>Also deals with auto time stamping of domain classes that have properties named 'lastUpdated' and/or 'dateCreated'.
 *
 * @author Lari Hotari
 * @since 1.3.5
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class ClosureEventListener implements SaveOrUpdateEventListener,
                                             PreLoadEventListener,
                                             PostLoadEventListener,
                                             PostInsertEventListener,
                                             PostUpdateEventListener,
                                             PostDeleteEventListener,
                                             PreDeleteEventListener,
                                             PreUpdateEventListener {

    private static final long serialVersionUID = 1;
    protected static final Log LOG = LogFactory.getLog(ClosureEventListener.class);

    final EventTriggerCaller saveOrUpdateCaller;
    final EventTriggerCaller beforeInsertCaller;
    final EventTriggerCaller preLoadEventCaller;
    final EventTriggerCaller postLoadEventListener;
    final EventTriggerCaller postInsertEventListener;
    final EventTriggerCaller postUpdateEventListener;
    final EventTriggerCaller postDeleteEventListener;
    final EventTriggerCaller preDeleteEventListener;
    final EventTriggerCaller preUpdateEventListener;
    final BeforeValidateEventTriggerCaller beforeValidateEventListener;
    boolean shouldTimestamp = false;
    MetaProperty dateCreatedProperty;
    MetaProperty lastUpdatedProperty;
    MetaClass domainMetaClass;
    boolean failOnErrorEnabled = false;
    Map validateParams;
    final TimestampProvider timestampProvider;

    public ClosureEventListener(Class<?> domainClazz, boolean failOnError, List failOnErrorPackages) {
        this(domainClazz, failOnError, failOnErrorPackages, new DefaultTimestampProvider());
    }

    public ClosureEventListener(Class<?> domainClazz, boolean failOnError, List failOnErrorPackages, TimestampProvider timestampProvider) {
        this.timestampProvider = timestampProvider;
        domainMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(domainClazz);
        applyAutotimestampSettings(domainClazz, timestampProvider);

        saveOrUpdateCaller = buildCaller(ClosureEventTriggeringInterceptor.ONLOAD_SAVE, domainClazz);
        beforeInsertCaller = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_INSERT_EVENT, domainClazz);
        EventTriggerCaller preLoadEventCaller = buildCaller(ClosureEventTriggeringInterceptor.ONLOAD_EVENT, domainClazz);
        if (preLoadEventCaller  == null) {
            this.preLoadEventCaller = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_LOAD_EVENT, domainClazz);
        }
        else {
            this.preLoadEventCaller = preLoadEventCaller;
        }
        postLoadEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_LOAD_EVENT, domainClazz);
        postInsertEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_INSERT_EVENT, domainClazz);
        postUpdateEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_UPDATE_EVENT, domainClazz);
        postDeleteEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_DELETE_EVENT, domainClazz);
        preDeleteEventListener = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_DELETE_EVENT, domainClazz);
        preUpdateEventListener = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_UPDATE_EVENT, domainClazz);
        
        beforeValidateEventListener = new BeforeValidateEventTriggerCaller(domainClazz, domainMetaClass);

        if (failOnErrorPackages.size() > 0) {
            failOnErrorEnabled = ClassUtils.isClassBelowPackage(domainClazz, failOnErrorPackages);
        } else {
            failOnErrorEnabled = failOnError;
        }

        validateParams = new HashMap();
        validateParams.put(AbstractHibernateGormValidationApi.ARGUMENT_DEEP_VALIDATE, Boolean.FALSE);

        try {
            actionQueueUpdatesField=ReflectionUtils.findField(ActionQueue.class, "updates");
            actionQueueUpdatesField.setAccessible(true);
            entityUpdateActionStateField=ReflectionUtils.findField(EntityUpdateAction.class, "state");
            entityUpdateActionStateField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    private void applyAutotimestampSettings(Class<?> domainClazz, TimestampProvider timestampProvider) {
        dateCreatedProperty = domainMetaClass.getMetaProperty(GormProperties.DATE_CREATED);
        if(dateCreatedProperty != null && !verifyTimestampFieldTypeSupport(domainClazz, timestampProvider, dateCreatedProperty)) {
            dateCreatedProperty = null;
        }
        lastUpdatedProperty = domainMetaClass.getMetaProperty(GormProperties.LAST_UPDATED);
        if(lastUpdatedProperty != null && !verifyTimestampFieldTypeSupport(domainClazz, timestampProvider, lastUpdatedProperty)) {
            lastUpdatedProperty = null;
        }
        if (dateCreatedProperty != null || lastUpdatedProperty != null) {
            Mapping m = GrailsDomainBinder.getMapping(domainClazz);
            shouldTimestamp = m == null || m.isAutoTimestamp();
        } else {
            shouldTimestamp = false;
        }
    }
    
    private boolean verifyTimestampFieldTypeSupport(Class<?> domainClazz, TimestampProvider timestampProvider, MetaProperty timestampField) {
        if(timestampProvider.supportsCreating(timestampField.getType())) {
            return true;
        } else {
            LOG.warn("TimestampProvider doesn't support creating timestamps for " + domainClazz.getName() + "." + timestampField.getName() + " field of type " + timestampField.getType());
            return false;
        }
    }
    
    private EventTriggerCaller buildCaller(String eventName, Class<?> domainClazz) {
        return EventTriggerCaller.buildCaller(eventName, domainClazz, domainMetaClass, null);
    }

    public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
        // no-op, merely a hook for plugins to override
    }

    private Field actionQueueUpdatesField;
    private Field entityUpdateActionStateField;
    
    private void synchronizePersisterState(AbstractPreDatabaseOperationEvent event, Object[] state) {
        Object entity = event.getEntity();
        EntityPersister persister = event.getPersister();
        
        String[] propertyNames = persister.getPropertyNames();
        HashMap<Integer, Object> changedState=new HashMap<Integer, Object>();
        for (int i = 0; i < propertyNames.length; i++) {
            String p = propertyNames[i];
            MetaProperty metaProperty = domainMetaClass.getMetaProperty(p);
            if (ClosureEventTriggeringInterceptor.IGNORED.contains(p) || metaProperty == null) {
                continue;
            }
            Object value = metaProperty.getProperty(entity);
            if(state[i] != value) {
                changedState.put(i, value);
            }
            state[i] = value;
            persister.setPropertyValue(entity, i, value);
        }
        
        synchronizeEntityUpdateActionState(event, entity, changedState);
    }

    protected void synchronizeEntityUpdateActionState(AbstractPreDatabaseOperationEvent event, Object entity,
            HashMap<Integer, Object> changedState) {
        if(actionQueueUpdatesField != null && event instanceof PreInsertEvent && changedState.size() > 0) {
            try {
                ExecutableList<EntityUpdateAction> updates = (ExecutableList<EntityUpdateAction>)actionQueueUpdatesField.get(event.getSource().getActionQueue());
                if(updates != null) {
                    for (EntityUpdateAction updateAction : updates) {
                        if(updateAction.getInstance() == entity) {
                            Object[] updateState = (Object[])entityUpdateActionStateField.get(updateAction);
                            if (updateState != null) {
                                for(Map.Entry<Integer, Object> entry : changedState.entrySet()) {
                                    updateState[entry.getKey()] = entry.getValue();
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Exception in synchronizeEntityUpdateActionState", e);
            }
        }
     }

    public void onPreLoad(final PreLoadEvent event) {
        if (preLoadEventCaller == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                preLoadEventCaller.call(event.getEntity());
                return null;
            }
        });
    }

    public void onPostLoad(final PostLoadEvent event) {
        if (postLoadEventListener == null) {
            return;
        }

       doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postLoadEventListener.call(event.getEntity());
                return null;
            }
        });
    }

    public void onPostInsert(PostInsertEvent event) {
        final Object entity = event.getEntity();
        if (postInsertEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postInsertEventListener.call(entity);
                return null;
            }
        });
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }

    public void onPostUpdate(PostUpdateEvent event) {
        final Object entity = event.getEntity();
        if (postUpdateEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postUpdateEventListener.call(entity);
                return null;
            }
        });
    }

    public void onPostDelete(PostDeleteEvent event) {
        final Object entity = event.getEntity();
        if (postDeleteEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postDeleteEventListener.call(entity);
                return null;
            }
        });
    }

    public boolean onPreDelete(final PreDeleteEvent event) {
        if (preDeleteEventListener == null) {
            return false;
        }

        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                return preDeleteEventListener.call(event.getEntity());
            }
        });
    }

    public boolean onPreUpdate(final PreUpdateEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean evict = false;
                if (preUpdateEventListener != null) {
                    evict = preUpdateEventListener.call(entity);
                    synchronizePersisterState(event, event.getState());
                }
                handleTimestampingBeforeUpdate(event, entity);
                if(!evict) {
                    return doValidate(entity);
                }
                else {
                    return evict;
                }
            }
        });
    }

    private <T> T doWithManualSession(AbstractEvent event, Closure<T> callable) {
        Session session = event.getSession();
        FlushMode current = session.getFlushMode();
        try {
           session.setFlushMode(FlushMode.MANUAL);
           return callable.call();
        } finally {
            session.setFlushMode(current);
        }
    }

    public boolean onPreInsert(final PreInsertEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean synchronizeState = false;
                if (beforeInsertCaller != null) {
                    if (beforeInsertCaller.call(entity)) {
                        return true;
                    }
                    synchronizeState = true;
                }
                synchronizeState = handleTimestampingBeforeInsert(entity, synchronizeState);

                if (synchronizeState) {
                    synchronizePersisterState(event, event.getState());
                }

                return doValidate(entity);
            }

        });
    }

    protected boolean doValidate(Object entity) {
        boolean evict = false;
        GormValidateable validateable = (GormValidateable) entity;
        if ( !validateable.shouldSkipValidation()
                && !validateable.validate(validateParams)) {
            evict = true;
            if (failOnErrorEnabled) {
                Errors errors = validateable.getErrors();
                throw ValidationException.newInstance("Validation error whilst flushing entity [" + entity.getClass().getName()
                        + "]", errors);
            }
        }
        return evict;
    }

    public void onValidate(ValidationEvent event) {
        beforeValidateEventListener.call(event.getEntityObject(), event.getValidatedFields());
    }

    protected void handleTimestampingBeforeUpdate(final PreUpdateEvent event, Object entity) {
        if (shouldTimestamp) {
            Class<?> dateCreatedType = null;
            Object timestamp = null;
            String[] propertyNames = event.getPersister().getPropertyNames();
            if (dateCreatedProperty != null && dateCreatedProperty.getProperty(entity)==null) {
                dateCreatedType = dateCreatedProperty.getType();
                timestamp = timestampProvider.createTimestamp(dateCreatedType);
                dateCreatedProperty.setProperty(entity, timestamp);
                event.getState()[ Arrays.asList(propertyNames).indexOf(GormProperties.DATE_CREATED) ] = timestamp;
            }
            if (lastUpdatedProperty != null) {
                Class<?> lastUpdateType = lastUpdatedProperty.getType();
                if(dateCreatedType == null || !lastUpdateType.isAssignableFrom(dateCreatedType)) {
                    timestamp = timestampProvider.createTimestamp(lastUpdateType);
                }
                lastUpdatedProperty.setProperty(entity, timestamp);
                event.getState()[ Arrays.asList(propertyNames).indexOf( GormProperties.LAST_UPDATED) ] = timestamp;
            }
        }
    }

    protected boolean handleTimestampingBeforeInsert(Object entity, boolean synchronizeState) {
        if (shouldTimestamp) {
            Class<?> dateCreatedType = null;
            Object timestamp = null;
            if (dateCreatedProperty != null) {
                dateCreatedType = dateCreatedProperty.getType();
                timestamp = timestampProvider.createTimestamp(dateCreatedType);
                dateCreatedProperty.setProperty(entity, timestamp);
                synchronizeState = true;
            }
            if (lastUpdatedProperty != null) {
                Class<?> lastUpdateType = lastUpdatedProperty.getType();
                if(dateCreatedType == null || !lastUpdateType.isAssignableFrom(dateCreatedType)) {
                    timestamp = timestampProvider.createTimestamp(lastUpdateType);
                }
                lastUpdatedProperty.setProperty(entity, timestamp);
                synchronizeState = true;
            }
        }
        return synchronizeState;
    }
}
