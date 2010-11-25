package org.opennms.systemreport.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.opennms.netmgt.dao.hibernate.AbstractDaoHibernate;
import org.opennms.netmgt.model.OnmsEvent;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

public class EventCountDaoHibernate extends AbstractDaoHibernate<OnmsEvent, Integer> implements EventCountDao {

    public EventCountDaoHibernate() {
        super(OnmsEvent.class);
    }

    public Set<CountedObject<String>> getUeiCounts(final Integer limit) {
        Set<CountedObject<String>> ueis = new TreeSet<CountedObject<String>>();
        HibernateCallback<List<CountedObject<String>>> hc = new HibernateCallback<List<CountedObject<String>>>() {
            public List<CountedObject<String>> doInHibernate(Session session) throws HibernateException {
                Query queryObject = session.createQuery("SELECT event.eventUei, COUNT(event.eventUei) FROM OnmsEvent event GROUP BY event.eventUei ORDER BY COUNT(event.eventUei) desc");
                queryObject.setMaxResults(limit);
                SessionFactoryUtils.applyTransactionTimeout(queryObject, getSessionFactory());
                List<CountedObject<String>> ueis = new ArrayList<CountedObject<String>>();
                @SuppressWarnings("unchecked")
                final List<Object[]> l = queryObject.list();
                for (final Object[] o : l) {
                    ueis.add(new CountedObject<String>((String)o[0], (Long)o[1]));
                }
                return ueis;
            }
        };
        ueis.addAll((List<CountedObject<String>>)getHibernateTemplate().executeWithNativeSession(hc));
        return ueis;
    }

}
