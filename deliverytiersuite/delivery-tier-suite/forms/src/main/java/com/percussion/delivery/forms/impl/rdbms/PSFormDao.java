/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.delivery.forms.impl.rdbms;

import com.percussion.delivery.forms.IPSFormDao;
import com.percussion.delivery.forms.data.IPSFormData;
import com.percussion.delivery.forms.data.PSFormData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public class PSFormDao extends HibernateDaoSupport implements IPSFormDao
{


    /*
     * (non-Javadoc)
     * @see com.percussion.delivery.forms.IPSFormDao#createFormData(java.lang.String, java.util.Map)
     */
    public IPSFormData createFormData(String formname, Map<String, String[]> formdata)
    {
        if(StringUtils.isBlank(formname))
            throw new IllegalArgumentException("formname cannnot be blank.");
        if(formdata == null)
            throw new IllegalArgumentException("formdata cannot be null");
        return new PSFormData(formname, formdata);
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#save(com.percussion.delivery.forms.data.IPSFormData)
     */
    public void save(IPSFormData form)
    {
        if (form == null)
        {
            throw new IllegalArgumentException("clist may not be null");
        }

        getHibernateTemplate().saveOrUpdate(form);
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#delete(com.percussion.delivery.forms.data.IPSFormData)
     */
    public void delete(IPSFormData form)
    {
        getHibernateTemplate().delete(form);
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#getExportedFormCount(java.lang.String)
     */
    public long getExportedFormCount(String name)
    {
        String query = "select count(*) from PSFormData formData where formData.isExported = 'y'";
        if (name != null && name.trim().length() > 0)
            query += " and lower(formData.name) = lower('" + name + "')";
        return ((Long) getHibernateTemplate().find(query).iterator().next())
                .intValue();
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#getTotalFormCount(java.lang.String)
     */
    public long getTotalFormCount(String name)
    {
        String query = "select count(*) from PSFormData formData";
        if (name != null && name.trim().length() > 0)
            query += " where lower(formData.name) = lower('" + name + "')";
        return ((Long) getHibernateTemplate().find(query).iterator().next())
                .intValue();
    }

    // rather than saving all the forms, we just change the exported property
    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#markAsExported(java.util.Collection)
     */
    public void markAsExported(Collection<IPSFormData> forms)
    {
        Session session = getSession();
        try
        {
            // because of limitations in JDBC/hibernate, we have to keep IN
            // clauses less than 1k elements
            String query = "update PSFormData set isExported = 'y' where id in (:ids)";
            Collection<Long> values = new ArrayList<>();
            for (IPSFormData form : forms)
            {
                values.add(Long.valueOf(form.getId()));
                if (values.size() > 950 || values.size() == forms.size())
                {
                    session.createQuery(query).setParameterList("ids", values)
                            .executeUpdate();
                    session.flush();
                    values.clear();
                }
            }
        }
        finally
        {
           // releaseSession(session);
        }
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#deleteExportedForms(java.lang.String)
     */
    public void deleteExportedForms(String formName)
    {
        List<IPSFormData> forms = findExportedForms(formName);
        if (!forms.isEmpty())
            getHibernateTemplate().deleteAll(forms);

    }

    @SuppressWarnings("unchecked")
    private List<IPSFormData> findExportedForms(String formName)
    {
        String sqlString = "";
        if (StringUtils.isEmpty(formName))
        {
            sqlString = "from PSFormData where isExported = 'y' order by name asc, created asc";
        }
        else
        {
            sqlString = "from PSFormData formData where formData.isExported = 'y' and lower(formData.name) = "
                    + "lower('" + formName + "')";
        }
        return (List<IPSFormData>) getHibernateTemplate().find(sqlString);
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#findFormsByName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<IPSFormData> findFormsByName(String name)
    {
        Validate.notNull(name);

        return (List<IPSFormData>) getHibernateTemplate().findByNamedParam(
                "from PSFormData formData " +
                        "where lower(formData.name) = lower(:name) " +
                        "order by created asc ",
                "name", name);
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#findAllForms()
     */
    @SuppressWarnings("unchecked")
    public List<IPSFormData> findAllForms()
    {
        return (List<IPSFormData>) getHibernateTemplate().find(
                "from PSFormData order by name asc, created asc");
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.forms.impl.rdbms.IPSFormDao#findDistinctFormNames()
     */
    @SuppressWarnings("unchecked")
    public List<String> findDistinctFormNames()
    {
        List<String> lowerNames = new ArrayList<>();
        List<String> distinctNames = (List<String>) getHibernateTemplate().find(
                "select distinct name from PSFormData order by name asc");
        Iterator<String> iter = distinctNames.iterator();
        while (iter.hasNext())
        {
            String name = iter.next().toLowerCase();
            if (!lowerNames.contains(name))
            {
                lowerNames.add(name);
            }
            else
            {
                iter.remove();
            }
        }

        return distinctNames;
    }

    private Session getSession(){

        return getSessionFactory().getCurrentSession();

    }


}
