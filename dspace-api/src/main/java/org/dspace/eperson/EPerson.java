/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class representing an e-person.
 * 
 * @author David Stuve
 * @version $Revision$
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "eperson")
public class EPerson extends DSpaceObject implements DSpaceObjectLegacySupport
{
    @Column(name="eperson_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name="netid", length = 64)
    private String netid;

    @Column(name="last_active")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActive;

    @Column(name="can_log_in", nullable = true)
    private Boolean canLogIn;

    @Column(name="email", unique=true, length = 64)
    private String email;

    @Column(name="require_certificate")
    private boolean requireCertificate = false;

    @Column(name="self_registered")
    private boolean selfRegistered = false;

    @Column(name="password", length = 128)
    private String password;

    @Column(name="salt", length = 32)
    private String salt;

    @Column(name="session_salt", length = 32)
    private String sessionSalt;

    @Column(name="digest_algorithm", length = 16)
    private String digestAlgorithm;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "epeople")
    private final List<Group> groups = new ArrayList<>();

    /** The e-mail field (for sorting) */
    public static final int EMAIL = 1;

    /** The last name (for sorting) */
    public static final int LASTNAME = 2;

    /** The e-mail field (for sorting) */
    public static final int ID = 3;

    /** The netid field (for sorting) */
    public static final int NETID = 4;

    /** The e-mail field (for sorting) */
    public static final int LANGUAGE = 5;

    @Transient
    protected transient EPersonService ePersonService;

    @Transient
    private Date previousActive;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.EPersonService#create(Context)}
     *
     */
    protected EPerson()
    {

    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    /**
     * Return true if this object equals obj, false otherwise.
     * 
     * @param obj another EPerson.
     * @return true if EPerson objects are equal in ID, email, and full name
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (getClass() != objClass)
        {
            return false;
        }
        final EPerson other = (EPerson) obj;
        if (this.getID() != other.getID())
        {
            return false;
        }
        if (!StringUtils.equals(this.getEmail(), other.getEmail()))
        {
            return false;
        }
        if (!StringUtils.equals(this.getFullName(), other.getFullName()))
        {
            return false;
        }
        return true;
    }

    /**
     * Return a hash code for this object.
     *
     * @return int hash of object
     */
    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 89 * hash + this.getID().hashCode();
        hash = 89 * hash + (this.getEmail() != null? this.getEmail().hashCode():0);
        hash = 89 * hash + (this.getFullName() != null? this.getFullName().hashCode():0);
        return hash;
    }

<<<<<<< HEAD


    /**
     * Get an EPerson from the database.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the EPerson
     * 
     * @return the EPerson format, or null if the ID is invalid.
     */
    public static EPerson find(Context context, int id) throws SQLException
    {
        // First check the cache
        EPerson fromCache = (EPerson) context.fromCache(EPerson.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "eperson", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new EPerson(context, row);
        }
    }

    /**
     * Find the eperson by their email address.
     * 
     * @return EPerson, or {@code null} if none such exists.
     */
    public static EPerson findByEmail(Context context, String email)
            throws SQLException, AuthorizeException
    {
        if (email == null)
        {
            return null;
        }
        
        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup 
        TableRow row = DatabaseManager.findByUnique(context, "eperson",
                "email", email.toLowerCase());

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                    .getIntColumn("eperson_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new EPerson(context, row);
            }
        }
    }

    /**
     * Find the eperson by their netid.
     * 
     * @param context
     *            DSpace context
     * @param netid
     *            Network ID
     * 
     * @return corresponding EPerson, or <code>null</code>
     */
    public static EPerson findByNetid(Context context, String netid)
            throws SQLException
    {
        if (netid == null)
        {
            return null;
        }

        TableRow row = DatabaseManager.findByUnique(context, "eperson", "netid", netid);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                    .getIntColumn("eperson_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new EPerson(context, row);
            }
        }
    }

    /**
     * Find the epeople that match the search query across firstname, lastname or email.
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return array of EPerson objects
     */
    public static EPerson[] search(Context context, String query)
            throws SQLException
    {
        return search(context, query, -1, -1);
    }
    
    /**
     * Find the epeople that match the search query across firstname, lastname or email. 
     * This method also allows offsets and limits for pagination purposes. 
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset 
     * @param limit
     *            Maximum number of matches returned
     * 
     * @return array of EPerson objects
     */
    public static EPerson[] search(Context context, String query, int offset, int limit) 
    		throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("select e.* from eperson e " +
                " LEFT JOIN metadatavalue fn on (resource_id=e.eperson_id AND fn.resource_type_id=? and fn.metadata_field_id=?) " +
                " LEFT JOIN metadatavalue ln on (ln.resource_id=e.eperson_id AND ln.resource_type_id=? and ln.metadata_field_id=?) " +
                " WHERE e.eperson_id = ? OR " +
                "LOWER(fn.text_value) LIKE LOWER(?) OR LOWER(ln.text_value) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) ORDER BY  ");

        if(DatabaseManager.isOracle()) {
            queryBuf.append(" dbms_lob.substr(ln.text_value), dbms_lob.substr(fn.text_value) ASC");
        }else{
            queryBuf.append(" ln.text_value, fn.text_value ASC");
        }

        // Add offset and limit restrictions - Oracle requires special code
        if (DatabaseManager.isOracle())
        {
            // First prepare the query to generate row numbers
            if (limit > 0 || offset > 0)
            {
                queryBuf.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
                queryBuf.append(") ");
            }

            // Restrict the number of rows returned based on the limit
            if (limit > 0)
            {
                queryBuf.append("rec WHERE rownum<=? ");
                // If we also have an offset, then convert the limit into the maximum row number
                if (offset > 0)
                {
                    limit += offset;
                }
            }

            // Return only the records after the specified offset (row number)
            if (offset > 0)
            {
                queryBuf.insert(0, "SELECT * FROM (");
                queryBuf.append(") WHERE rnum>?");
            }
        }
        else
        {
            if (limit > 0)
            {
                queryBuf.append(" LIMIT ? ");
            }

            if (offset > 0)
            {
                queryBuf.append(" OFFSET ? ");
            }
        }

        String dbquery = queryBuf.toString();

        // When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}

		catch (NumberFormatException e) {
			int_param = Integer.valueOf(-1);
		}


        Integer f = MetadataField.findByElement(context, MetadataSchema.find(context, "eperson").getSchemaID(), "firstname", null).getFieldID();
        Integer l = MetadataField.findByElement(context, MetadataSchema.find(context, "eperson").getSchemaID(), "lastname", null).getFieldID();

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[] {Constants.EPERSON,f, Constants.EPERSON,l, int_param,params,params,params};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{Constants.EPERSON,f, Constants.EPERSON,l, int_param,params,params,params, limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{Constants.EPERSON,f, Constants.EPERSON,l, int_param,params,params,params, limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{Constants.EPERSON,f, Constants.EPERSON,l, int_param,params,params,params, offset};
        }

        // Get all the epeople that match the query
		TableRowIterator rows = DatabaseManager.query(context, 
		        dbquery, paramArr);
		try
        {
            List<TableRow> epeopleRows = rows.toList();
            EPerson[] epeople = new EPerson[epeopleRows.size()];

            for (int i = 0; i < epeopleRows.size(); i++)
            {
                TableRow row = (TableRow) epeopleRows.get(i);

                // First check the cache
                EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                        .getIntColumn("eperson_id"));

                if (fromCache != null)
                {
                    epeople[i] = fromCache;
                }
                else
                {
                    epeople[i] = new EPerson(context, row);
                }
            }

            return epeople;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    /**
     * Returns the total number of epeople returned by a specific query, without the overhead 
     * of creating the EPerson objects to store the results.
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return the number of epeople matching the query
     */
    public static int searchResultCount(Context context, String query)
    	throws SQLException
	{
		String dbquery = "%"+query.toLowerCase()+"%";
		Long count;
		
		// When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = Integer.valueOf(-1);
		}

		// Get all the epeople that match the query
		TableRow row = DatabaseManager.querySingle(context,
		        "SELECT count(*) as epcount FROM eperson " +
                "WHERE eperson_id = ? OR " +
		        "LOWER((select text_value from metadatavalue where resource_id=? and resource_type_id=? and metadata_field_id=?)) LIKE LOWER(?) " +
                "OR LOWER((select text_value from metadatavalue where resource_id=? and resource_type_id=? and metadata_field_id=?)) LIKE LOWER(?) " +
                "OR LOWER(eperson.email) LIKE LOWER(?)",
		        new Object[] {
                        int_param,

                        int_param,
                        Constants.EPERSON,
                        MetadataField.findByElement(context, MetadataSchema.find(context, "eperson").getSchemaID(), "firstname", null).getFieldID(),
                        dbquery,

                        int_param,
                        Constants.EPERSON,
                        MetadataField.findByElement(context, MetadataSchema.find(context, "eperson").getSchemaID(), "lastname", null).getFieldID(),
                        dbquery,

                        dbquery
                });
				
		// use getIntColumn for Oracle count data
        if (DatabaseManager.isOracle())
        {
            count = Long.valueOf(row.getIntColumn("epcount"));
        }
        else  //getLongColumn works for postgres
        {
            count = Long.valueOf(row.getLongColumn("epcount"));
        }
        
		return count.intValue();
	}
    
    
    
    /**
     * Find all the epeople that match a particular query
     * <ul>
     * <li><code>ID</code></li>
     * <li><code>LASTNAME</code></li>
     * <li><code>EMAIL</code></li>
     * <li><code>NETID</code></li>
     * </ul>
     * 
     * @return array of EPerson objects
     */
    public static EPerson[] findAll(Context context, int sortField)
            throws SQLException
    {
        String s, t = "", theQuery = "";

        switch (sortField)
        {
        case ID:
            s = "e.eperson_id";
            break;

        case EMAIL:
            s = "e.email";
            break;

        case LANGUAGE:
            s = "m.text_value";
            t = "language";
            break;
        case NETID:
            s = "e.netid";
            break;

        default:
            s = "m.text_value";
            t = "lastname";
        }

        // NOTE: The use of 's' in the order by clause can not cause an SQL
        // injection because the string is derived from constant values above.
        TableRowIterator rows;
        if(!t.equals("")) {
            rows = DatabaseManager.query(context,
                    "SELECT * FROM eperson e " +
                    "LEFT JOIN metadatavalue m on (m.resource_id = e.eperson_id and m.resource_type_id = ? and m.metadata_field_id = ?) " +
                    "ORDER BY " + s,
                    Constants.EPERSON,
                    MetadataField.findByElement(context, MetadataSchema.find(context, "eperson").getSchemaID(), t, null).getFieldID()
            );
        }
        else {
        	rows =  DatabaseManager.query(context, "SELECT * FROM eperson e ORDER BY " + s);
        }
        



        try
        {
            List<TableRow> epeopleRows = rows.toList();

            EPerson[] epeople = new EPerson[epeopleRows.size()];

            for (int i = 0; i < epeopleRows.size(); i++)
            {
                TableRow row = (TableRow) epeopleRows.get(i);

                // First check the cache
                EPerson fromCache = (EPerson) context.fromCache(EPerson.class, row
                        .getIntColumn("eperson_id"));

                if (fromCache != null)
                {
                    epeople[i] = fromCache;
                }
                else
                {
                    epeople[i] = new EPerson(context, row);
                }
            }

            return epeople;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    /**
     * Create a new eperson
     * 
     * @param context
     *            DSpace context object
     */
    public static EPerson create(Context context) throws SQLException,
            AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "eperson");

        EPerson e = new EPerson(context, row);

        log.info(LogManager.getHeader(context, "create_eperson", "eperson_id="
                + e.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.EPERSON, e.getID(), 
                null, e.getIdentifiers(context)));

        return e;
    }

    /**
     * Delete an eperson
     * 
     */
    public void delete() throws SQLException, AuthorizeException,
            EPersonDeletionException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(ourContext))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an EPerson");
        }

        // check for presence of eperson in tables that
        // have constraints on eperson_id
        List<String> constraintList = getDeleteConstraints();

        // if eperson exists in tables that have constraints
        // on eperson, throw an exception
        if (constraintList.size() > 0)
        {
            throw new EPersonDeletionException(constraintList);
        }

        // Delete the Dublin Core
        removeMetadataFromDatabase();

        ourContext.addEvent(new Event(Event.DELETE, Constants.EPERSON, getID(), getEmail(), getIdentifiers(ourContext)));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // XXX FIXME: This sidesteps the object model code so it won't
        // generate  REMOVE events on the affected Groups.

        // Remove any group memberships first
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM EPersonGroup2EPerson WHERE eperson_id= ? ",
                getID());

        // Remove any subscriptions
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM subscription WHERE eperson_id= ? ",
                getID());

        // Remove ourself
        DatabaseManager.delete(ourContext, myRow);

        log.info(LogManager.getHeader(ourContext, "delete_eperson",
                "eperson_id=" + getID()));
    }

    /**
     * Get the e-person's internal identifier
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("eperson_id");
    }
    
=======
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    /**
     * Get the e-person's language
     * 
     * @return language code (or null if the column is an SQL NULL)
     */
     public String getLanguage()
     {
         return getePersonService().getMetadataFirstValue(this, "eperson", "language", null, Item.ANY);
     }
     
     /**
     * Set the EPerson's language.  Value is expected to be a Unix/POSIX
     * Locale specification of the form {language} or {language}_{territory},
     * e.g. "en", "en_US", "pt_BR" (the latter is Brazilian Portugese).
     * 
     * @param context
     *     The relevant DSpace Context.
     * @param language
     *     language code
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
     public void setLanguage(Context context, String language) throws SQLException {
         getePersonService().setMetadataSingleValue(context, this, "eperson", "language", null, null, language);
     }

    /**
     * Get the e-person's email address
     * 
     * @return their email address (or null if the column is an SQL NULL)
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * Set the EPerson's email
     * 
     * @param s
     *     the new email
     */
    public void setEmail(String s)
    {
        this.email = StringUtils.lowerCase(s);
        setModified();
    }

    /**
     * Get the e-person's netid
     * 
     * @return their netid (DB constraints ensure it's never NULL)
     */
    public String getNetid()
    {
        return netid;
    }

    /**
     * Set the EPerson's netid
     * 
     * @param netid
     *     the new netid
     */
    public void setNetid(String netid) {
        this.netid = netid;
        setModified();
    }

    /**
     * Get the e-person's full name, combining first and last name in a
     * displayable string.
     * 
     * @return their full name (first + last name; if both are NULL, returns email)
     */
    public String getFullName()
    {
        String f = getFirstName();
        String l= getLastName();

        if ((l == null) && (f == null))
        {
            return getEmail();
        }
        else if (f == null)
        {
            return l;
        }
        else
        {
            return (f + " " + l);
        }
    }

    /**
     * Get the eperson's first name.
     * 
     * @return their first name (or null if the column is an SQL NULL)
     */
    public String getFirstName()
    {
        return getePersonService().getMetadataFirstValue(this, "eperson", "firstname", null, Item.ANY);
    }

    /**
     * Set the eperson's first name
     * 
     * @param context
     *     The relevant DSpace Context.
     * @param firstname
     *     the person's first name
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public void setFirstName(Context context, String firstname) throws SQLException {
        getePersonService().setMetadataSingleValue(context, this, "eperson", "firstname", null, null, firstname);
        setModified();
    }

    /**
     * Get the eperson's last name.
     * 
     * @return their last name (or null if the column is an SQL NULL)
     */
    public String getLastName()
    {
        return getePersonService().getMetadataFirstValue(this, "eperson", "lastname", null, Item.ANY);
    }

    /**
     * Set the eperson's last name
     * 
     * @param context
     *     The relevant DSpace Context.
     * @param lastname
     *     the person's last name
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public void setLastName(Context context, String lastname) throws SQLException {
        getePersonService().setMetadataSingleValue(context, this, "eperson", "lastname", null, null, lastname);
        setModified();
    }

    /**
     * Indicate whether the user can log in
     * 
     * @param login
     *     boolean yes/no
     */
    public void setCanLogIn(boolean login)
    {
        this.canLogIn = login;
        setModified();
    }

    /**
     * Can the user log in?
     * 
     * @return boolean, yes/no
     */
    public boolean canLogIn()
    {
        return BooleanUtils.isTrue(canLogIn);
    }

    /**
     * Set require cert yes/no
     * 
     * @param isrequired
     *     boolean yes/no
     */
    public void setRequireCertificate(boolean isrequired)
    {
        this.requireCertificate = isrequired;
        setModified();
    }

    /**
     * Get require certificate or not
     * 
     * @return boolean, yes/no (or false if the column is an SQL NULL)
     */
    public boolean getRequireCertificate()
    {
        return requireCertificate;
    }

    /**
     * Indicate whether the user self-registered
     * 
     * @param sr
     *     boolean yes/no
     */
    public void setSelfRegistered(boolean sr)
    {
        this.selfRegistered = sr;
        setModified();
    }

    /**
     * Is the user self-registered?
     * 
     * @return boolean, yes/no (or false if the column is an SQL NULL)
     */
    public boolean getSelfRegistered()
    {
        return selfRegistered;
    }

    /**
     * Stamp the EPerson's last-active date.
     * 
     * @param when latest activity timestamp, or null to clear.
     */
    public void setLastActive(Date when)
    {
        this.previousActive = lastActive;
        this.lastActive = when;
    }

    /**
     * Get the EPerson's last-active stamp.
     * 
     * @return date when last logged on, or null.
     */
    public Date getLastActive()
    {
        return lastActive;
    }

    /**
     * @return type found in Constants, see {@link org.dspace.core.Constants#Constants Constants}
     */
    @Override
    public int getType()
    {
        return Constants.EPERSON;
    }

    @Override
    public String getName()
    {
        return getEmail();
    }

    String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    String getSalt() {
        return salt;
    }

    void setSalt(String salt) {
        this.salt = salt;
    }

    String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    public List<Group> getGroups() {
        return groups;
    }

    private EPersonService getePersonService() {
        if(ePersonService == null)
        {
            ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        }
        return ePersonService;
    }

    public String getSessionSalt() {
        return sessionSalt;
    }

    public void setSessionSalt(String sessionSalt) {
        this.sessionSalt = sessionSalt;
    }

    public Date getPreviousActive() {
        if (previousActive == null) {
            return new Date(0);
        }
        return previousActive;
    }

}
