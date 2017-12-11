/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.dspace.content.comparator.NameAscendingComparator;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.*;
import org.dspace.eperson.Group;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.util.*;

/**
 * Class representing a community
 * <P>
 * The community's metadata (name, introductory text etc.) is loaded into'
 * memory. Changes to this metadata are only reflected in the database after
 * <code>update</code> is called.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
@Entity
@Table(name="community")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class Community extends DSpaceObject implements DSpaceObjectLegacySupport
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(Community.class);

    @Column(name="community_id", insertable = false, updatable = false)
    private Integer legacyId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "community2community",
            joinColumns = {@JoinColumn(name = "parent_comm_id") },
            inverseJoinColumns = {@JoinColumn(name = "child_comm_id") }
    )
    private Set<Community> subCommunities = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "subCommunities")
    private Set<Community> parentCommunities = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "communities", cascade = {CascadeType.PERSIST})
    private Set<Collection> collections = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "admin")
    /** The default group of administrators */
    private Group admins;

    /** The logo bitstream */
    @OneToOne
    @JoinColumn(name = "logo_bitstream_id")
    private Bitstream logo = null;

    // Keys for accessing Community metadata
    public static final String COPYRIGHT_TEXT = "copyright_text";
    public static final String INTRODUCTORY_TEXT = "introductory_text";
    public static final String SHORT_DESCRIPTION = "short_description";
    public static final String SIDEBAR_TEXT = "side_bar_text";

    @Transient
    protected transient CommunityService communityService;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.CommunityService#create(Community, Context)}
     * or
     * {@link org.dspace.content.service.CommunityService#create(Community, Context, String)}
     *
<<<<<<< HEAD
     * @param context
     *            DSpace context object
     * @param handle the pre-determined Handle to assign to the new community
     *
     * @return the newly created community
     */
    public static Community create(Community parent, Context context, String handle)
            throws SQLException, AuthorizeException
    {
        if (!(AuthorizeManager.isAdmin(context) ||
              (parent != null && AuthorizeManager.authorizeActionBoolean(context, parent, Constants.ADD))))
        {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        TableRow row = DatabaseManager.create(context, "community");
        Community c = new Community(context, row);
        
        try
        {
            c.handle = (handle == null) ?
                       HandleManager.createHandle(context, c) :
                       HandleManager.createHandle(context, c, handle);
        }
        catch(IllegalStateException ie)
        {
            //If an IllegalStateException is thrown, then an existing object is already using this handle
            //Remove the community we just created -- as it is incomplete
            try
            {
                if(c!=null)
                {
                    c.delete();
                }
            } catch(Exception e) { }

            //pass exception on up the chain
            throw ie;
        }

        if(parent != null)
        {
            parent.addSubcommunity(c);
        }

        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = Group.find(context, 0);

        ResourcePolicy myPolicy = ResourcePolicy.create(context);
        myPolicy.setResource(c);
        myPolicy.setAction(Constants.READ);
        myPolicy.setGroup(anonymousGroup);
        myPolicy.update();

        context.addEvent(new Event(Event.CREATE, Constants.COMMUNITY, c.getID(), 
                c.handle, c.getIdentifiers(context)));

        // if creating a top-level Community, simulate an ADD event at the Site.
        if (parent == null)
        {
            context.addEvent(new Event(Event.ADD, Constants.SITE, Site.SITE_ID, 
                    Constants.COMMUNITY, c.getID(), c.handle, 
                    c.getIdentifiers(context)));
        }

        log.info(LogManager.getHeader(context, "create_community",
                "community_id=" + row.getIntColumn("community_id"))
                + ",handle=" + c.handle);

        return c;
    }

    /**
     * Get a list of all communities in the system. These are alphabetically
     * sorted by community name.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the communities in the system
     */
    public static Community[] findAll(Context context) throws SQLException
    {
        TableRowIterator tri = null;
        try {
            String query = "SELECT c.* FROM community c " +
                    "LEFT JOIN metadatavalue m on (m.resource_id = c.community_id and m.resource_type_id = ? and m.metadata_field_id = ?) ";
            if(DatabaseManager.isOracle()){
                query += " ORDER BY cast(m.text_value as varchar2(128))";
            }else{
                query += " ORDER BY m.text_value";
            }

            tri = DatabaseManager.query(context,
                    query,
                    Constants.COMMUNITY,
                    MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID()
            );
        } catch (SQLException e) {
            log.error("Find all Communities - ",e);
            throw e;
        }

        List<Community> communities = new ArrayList<Community>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next(context);

                // First check the cache
                Community fromCache = (Community) context.fromCache(
                        Community.class, row.getIntColumn("community_id"));

                if (fromCache != null)
                {
                    communities.add(fromCache);
                }
                else
                {
                    communities.add(new Community(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Community[] communityArray = new Community[communities.size()];
        communityArray = (Community[]) communities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Get a list of all top-level communities in the system. These are
     * alphabetically sorted by community name. A top-level community is one
     * without a parent community.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the top-level communities in the system
     */
    public static Community[] findAllTop(Context context) throws SQLException
    {
        // get all communities that are not children
        TableRowIterator tri = null;
        try {
            String query = "SELECT c.* FROM community c  "
                    + "LEFT JOIN metadatavalue m on (m.resource_id = c.community_id and m.resource_type_id = ? and m.metadata_field_id = ?) "
                    + "WHERE NOT c.community_id IN (SELECT child_comm_id FROM community2community) ";
            if(DatabaseManager.isOracle()){
                query += " ORDER BY cast(m.text_value as varchar2(128))";
            }else{
                query += " ORDER BY m.text_value";
            }
            tri = DatabaseManager.query(context,
                    query,
                    Constants.COMMUNITY,
                    MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID()
            );
        } catch (SQLException e) {
            log.error("Find all Top Communities - ",e);
            throw e;
        }

        List<Community> topCommunities = new ArrayList<Community>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next(context);

                // First check the cache
                Community fromCache = (Community) context.fromCache(
                        Community.class, row.getIntColumn("community_id"));

                if (fromCache != null)
                {
                    topCommunities.add(fromCache);
                }
                else
                {
                    topCommunities.add(new Community(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Community[] communityArray = new Community[topCommunities.size()];
        communityArray = (Community[]) topCommunities.toArray(communityArray);

        return communityArray;
    }

    /**
     * Get the internal ID of this collection
     * 
     * @return the internal identifier
=======
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
     */
    protected Community()
    {

    }

    void addSubCommunity(Community subCommunity)
    {
        subCommunities.add(subCommunity);
        setModified();
    }

    void removeSubCommunity(Community subCommunity)
    {
        subCommunities.remove(subCommunity);
        setModified();
    }

    /**
     * Get the logo for the community. <code>null</code> is return if the
     * community does not have a logo.
     * 
     * @return the logo of the community, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    void setLogo(Bitstream logo) {
        this.logo = logo;
        setModified();
    }

    /**
     * Get the default group of administrators, if there is one. Note that the
     * authorization system may allow others to be administrators for the
     * community.
     * <P>
     * The default group of administrators for community 100 is the one called
     * <code>community_100_admin</code>.
     * 
     * @return group of administrators, or <code>null</code> if there is no
     *         default group.
     */
    public Group getAdministrators()
    {
        return admins;
    }

    void setAdmins(Group admins) {
        this.admins = admins;
        setModified();
    }

    /**
     * Get the collections in this community. Throws an SQLException because
     * creating a community object won't load in all collections.
     * 
     * @return array of Collection objects
     */
    public List<Collection> getCollections()
    {
<<<<<<< HEAD
        List<Collection> collections = new ArrayList<Collection>();

        // Get the table rows
        TableRowIterator tri = null;
        try {
            String query = "SELECT c.* FROM community2collection c2c, collection c "
                    + "LEFT JOIN metadatavalue m on (m.resource_id = c.collection_id and m.resource_type_id = ? and m.metadata_field_id = ?) "
                    + "WHERE c2c.collection_id=c.collection_id AND c2c.community_id=? ";
            if(DatabaseManager.isOracle()){
                query += " ORDER BY cast(m.text_value as varchar2(128))";
            }else{
                query += " ORDER BY m.text_value";
            }
            tri = DatabaseManager.query(
                    ourContext,
                    query,
                    Constants.COLLECTION,
                    MetadataField.findByElement(ourContext, MetadataSchema.find(ourContext, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID(),
                    getID()
            );
        } catch (SQLException e) {
            log.error("Find all Collections for this community - ",e);
            throw e;
        }

        // Make Collection objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next(ourContext);

                // First check the cache
                Collection fromCache = (Collection) ourContext.fromCache(
                        Collection.class, row.getIntColumn("collection_id"));

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(new Collection(ourContext, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
=======
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Collection[] output = collections.toArray(new Collection[]{});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
    }
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

    void addCollection(Collection collection)
    {
        collections.add(collection);
    }

    void removeCollection(Collection collection)
    {
        collections.remove(collection);
    }

    /**
     * Get the immediate sub-communities of this community. Throws an
     * SQLException because creating a community object won't load in all
     * collections.
     * 
     * @return array of Community objects
     */
    public List<Community> getSubcommunities()
    {
<<<<<<< HEAD
        List<Community> subcommunities = new ArrayList<Community>();

        // Get the table rows
        TableRowIterator tri = null;
        try {
            String query = "SELECT c.* FROM community2community c2c, community c " +
                    "LEFT JOIN metadatavalue m on (m.resource_id = c.community_id and m.resource_type_id = ? and m.metadata_field_id = ?) " +
                    "WHERE c2c.child_comm_id=c.community_id " +
                    "AND c2c.parent_comm_id= ? ";
            if(DatabaseManager.isOracle()){
                query += " ORDER BY cast(m.text_value as varchar2(128))";
            }else{
                query += " ORDER BY m.text_value";
            }

            tri = DatabaseManager.query(
                    ourContext,
                    query,
                    Constants.COMMUNITY,
                    MetadataField.findByElement(ourContext, MetadataSchema.find(ourContext, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID(),
                    getID()
            );
        } catch (SQLException e) {
            log.error("Find all Sub Communities - ",e);
            throw e;
        }


        // Make Community objects
        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next(ourContext);

                // First check the cache
                Community fromCache = (Community) ourContext.fromCache(
                        Community.class, row.getIntColumn("community_id"));

                if (fromCache != null)
                {
                    subcommunities.add(fromCache);
                }
                else
                {
                    subcommunities.add(new Community(ourContext, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // Put them in an array
        Community[] communityArray = new Community[subcommunities.size()];
        communityArray = (Community[]) subcommunities.toArray(communityArray);

        return communityArray;
=======
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Community[] output = subCommunities.toArray(new Community[]{});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    }

    /**
     * Return the parent community of this community, or null if the community
     * is top-level
     * 
     * @return the immediate parent community, or null if top-level
     */
    public List<Community> getParentCommunities()
    {
<<<<<<< HEAD
        Community parentCommunity = null;

        // Get the table rows
        TableRowIterator tri = DatabaseManager.queryTable(
                ourContext,"community",
                "SELECT community.* FROM community, community2community WHERE " +
                "community2community.parent_comm_id=community.community_id " +
                "AND community2community.child_comm_id= ? ",
                getID());
        
        // Make Community object
        try
        {
            if (tri.hasNext())
            {
                TableRow row = tri.next(ourContext);

                // First check the cache
                Community fromCache = (Community) ourContext.fromCache(
                        Community.class, row.getIntColumn("community_id"));

                if (fromCache != null)
                {
                    parentCommunity = fromCache;
                }
                else
                {
                    parentCommunity = new Community(ourContext, row);
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        return parentCommunity;
    }

    /**
     * Return an array of parent communities of this community, in ascending
     * order. If community is top-level, return an empty array.
     * 
     * @return an array of parent communities, empty if top-level
     */
    public Community[] getAllParents() throws SQLException
    {
        List<Community> parentList = new ArrayList<Community>();
        Community parent = getParentCommunity();

        while (parent != null)
        {
            parentList.add(parent);
            parent = parent.getParentCommunity();
        }

        // Put them in an array
        Community[] communityArray = new Community[parentList.size()];
        communityArray = (Community[]) parentList.toArray(communityArray);

        return communityArray;
    }

    /**
     * Return an array of collections of this community and its subcommunities
     * 
     * @return an array of collections
     */

    public Collection[] getAllCollections() throws SQLException
    {
        List<Collection> collectionList = new ArrayList<Collection>();
        for (Community subcommunity : getSubcommunities())
        {
            addCollectionList(subcommunity, collectionList);
        }

        for (Collection collection : getCollections())
        {
            collectionList.add(collection);
        }

        // Put them in an array
        Collection[] collectionArray = new Collection[collectionList.size()];
        collectionArray = (Collection[]) collectionList.toArray(collectionArray);

        return collectionArray;

    }
    /**
     * Internal method to process subcommunities recursively
     */
    private void addCollectionList(Community community, List<Collection> collectionList) throws SQLException
    {
        for (Community subcommunity : community.getSubcommunities())
        {
            addCollectionList(subcommunity, collectionList);
        }

        for (Collection collection : community.getCollections())
        {
            collectionList.add(collection);
        }
    }

    /**
     * Create a new collection within this community. The collection is created
     * without any workflow groups or default submitter group.
     * 
     * @return the new collection
     */
    public Collection createCollection() throws SQLException,
            AuthorizeException
    {
        return createCollection(null);
    }

    /**
     * Create a new collection within this community. The collection is created
     * without any workflow groups or default submitter group.
     *
     * @param handle the pre-determined Handle to assign to the new community
     * @return the new collection
     */
    public Collection createCollection(String handle) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        Collection c = Collection.create(ourContext, handle);
        addCollection(c);

        return c;
    }

    /**
     * Add an exisiting collection to the community
     * 
     * @param c
     *            collection to add
     */
    public void addCollection(Collection c) throws SQLException,
            AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_collection",
                "community_id=" + getID() + ",collection_id=" + c.getID()));

        // Find out if mapping exists
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,
                "community2collection",
                "SELECT * FROM community2collection WHERE " +
                "community_id= ? AND collection_id= ? ",getID(),c.getID());

        try
        {
            if (!tri.hasNext())
            {
                // No existing mapping, so add one
                TableRow mappingRow = DatabaseManager.row("community2collection");

                mappingRow.setColumn("community_id", getID());
                mappingRow.setColumn("collection_id", c.getID());

                ourContext.addEvent(new Event(Event.ADD, Constants.COMMUNITY, 
                        getID(), Constants.COLLECTION, c.getID(), c.getHandle(),
                        getIdentifiers(ourContext)));

                DatabaseManager.insert(ourContext, mappingRow);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
=======
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Community[] output = parentCommunities.toArray(new Community[]{});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    }

    void addParentCommunity(Community parentCommunity) {
        parentCommunities.add(parentCommunity);
    }

    void clearParentCommunities(){
        parentCommunities.clear();
    }

    public void removeParentCommunity(Community parentCommunity)
    {
        parentCommunities.remove(parentCommunity);
        setModified();
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Community
     * as this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same
     *         community as this object
     */
    @Override
    public boolean equals(Object other)
    {
        if (other == null)
        {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(other);
        if (this.getClass() != objClass)
        {
            return false;
        }
        final Community otherCommunity = (Community) other;
        if (!this.getID().equals(otherCommunity.getID() ))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(getID()).toHashCode();
    }

    /**
     * return type found in Constants
     * @return Community type
     */
    @Override
    public int getType()
    {
        return Constants.COMMUNITY;
    }

    @Override
    public String getName() {
        String value = getCommunityService().getMetadataFirstValue(this, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        return value == null ? "" : value;
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    private CommunityService getCommunityService() {
        if(communityService == null)
        {
            communityService = ContentServiceFactory.getInstance().getCommunityService();
        }
        return communityService;
    }
}
