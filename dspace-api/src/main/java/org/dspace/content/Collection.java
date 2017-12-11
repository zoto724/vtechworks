/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.comparator.NameAscendingComparator;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.*;
import org.dspace.eperson.Group;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

<<<<<<< HEAD
import java.io.Serializable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
=======
import javax.persistence.*;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
import java.sql.SQLException;
import java.util.*;
import org.dspace.authorize.AuthorizeException;

/**
 * Class representing a collection.
 * <P>
 * The collection's metadata (name, introductory text etc), workflow groups, and
 * default group of submitters are loaded into memory. Changes to metadata are
 * not written to the database until <code>update</code> is called. If you
 * create or remove a workflow group, the change is only reflected in the
 * database after calling <code>update</code>. The default group of
 * submitters is slightly different - creating or removing this has instant
 * effect.
 *
 * @author Robert Tansley
 */
@Entity
@Table(name="collection")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class Collection extends DSpaceObject implements DSpaceObjectLegacySupport
{

    @Column(name="collection_id", insertable = false, updatable = false)
    private Integer legacyId;

    /** The logo bitstream */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_bitstream_id")
    private Bitstream logo;

    /** The item template */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_item_id")
    private Item template;

    /**
     * Groups corresponding to workflow steps - NOTE these start from one, so
     * workflowGroups[0] corresponds to workflow_step_1.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_1")
    private Group workflowStep1;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_2")
    private Group workflowStep2;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_3")
    private Group workflowStep3;


    @OneToOne
    @JoinColumn(name = "submitter")
    /** The default group of administrators */
    private Group submitters;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin")
    /** The default group of administrators */
    private Group admins;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinTable(
            name = "community2collection",
            joinColumns = {@JoinColumn(name = "collection_id") },
            inverseJoinColumns = {@JoinColumn(name = "community_id") }
    )
    private Set<Community> communities = new HashSet<>();

    @Transient
    private transient CollectionService collectionService;

    // Keys for accessing Collection metadata
    @Transient
    public static final String COPYRIGHT_TEXT = "copyright_text";
    @Transient
    public static final String INTRODUCTORY_TEXT = "introductory_text";
    @Transient
    public static final String SHORT_DESCRIPTION = "short_description";
    @Transient
    public static final String SIDEBAR_TEXT = "side_bar_text";
    @Transient
    public static final String PROVENANCE_TEXT = "provenance_description";

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.CollectionService#create(Context, Community)}
     * or
     * {@link org.dspace.content.service.CollectionService#create(Context, Community, String)}
     *
     */
    protected Collection()
    {

    }

    @Override
    public String getName()
    {
        String value = getCollectionService().getMetadataFirstValue(this, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        return value == null ? "" : value;
    }

    /**
     * Get the logo for the collection. <code>null</code> is returned if the
     * collection does not have a logo.
     *
     * @return the logo of the collection, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    protected void setLogo(Bitstream logo) {
        this.logo = logo;
        setModified();
    }


    /**
     * Get the default group of submitters, if there is one. Note that the
     * authorization system may allow others to submit to the collection, so
     * this is not necessarily a definitive list of potential submitters.
     * <P>
     * The default group of submitters for collection 100 is the one called
     * <code>collection_100_submit</code>.
     *
     * @return the default group of submitters, or <code>null</code> if there
     *         is no default group.
     */
    public Group getSubmitters()
    {
        return submitters;
    }

    /**
     * Set the default group of submitters
     *
     * Package protected in order to preven unauthorized calls to this method
     *
     * @param submitters the group of submitters
     */
<<<<<<< HEAD
    public static Collection[] findAll(Context context) throws SQLException
    {
        TableRowIterator tri = null;
        List<Collection> collections = null;
        List<Serializable> params = new ArrayList<Serializable>();
        StringBuffer query = new StringBuffer(
            "SELECT c.*" +
            "FROM collection c " +
            "LEFT JOIN metadatavalue m ON (" +
              "m.resource_id = c.collection_id AND " +
              "m.resource_type_id = ? AND " +
              "m.metadata_field_id = ?" +
            ")"
        );

        if (DatabaseManager.isOracle())
        {
            query.append(" ORDER BY cast(m.text_value as varchar2(128))");
        }
        else
        {
            query.append(" ORDER BY m.text_value");
        }

        params.add(Constants.COLLECTION);
        params.add(
          MetadataField.findByElement(
            context,
            MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(),
            "title",
            null
          ).getFieldID()
        );

        try
        {
            tri = DatabaseManager.query(
              context, query.toString(), params.toArray()
            );

            collections = new ArrayList<Collection>();

            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Collection fromCache = (Collection) context.fromCache(
                        Collection.class, row.getIntColumn("collection_id"));

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(new Collection(context, row));
                }
            }
        }
        catch (SQLException e)
        {
            log.error("Find all Collections - ", e);
            throw e;
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }

    /**
     * Get all collections in the system. Adds support for limit and offset.
     * @param context
     * @param limit
     * @param offset
     * @return
     * @throws SQLException
     */
    public static Collection[] findAll(Context context, Integer limit, Integer offset) throws SQLException
    {
        TableRowIterator tri = null;
        List<Collection> collections = null;
        List<Serializable> params = new ArrayList<Serializable>();
        StringBuffer query = new StringBuffer(
            "SELECT c.*" +
            "FROM collection c " +
            "LEFT JOIN metadatavalue m ON (" +
              "m.resource_id = c.collection_id AND " +
              "m.resource_type_id = ? AND " +
              "m.metadata_field_id = ?" +
            ")"
        );

        if (DatabaseManager.isOracle())
        {
            query.append(" ORDER BY cast(m.text_value as varchar2(128))");
        }
        else
        {
            query.append(" ORDER BY m.text_value");
        }

        params.add(Constants.COLLECTION);
        params.add(
          MetadataField.findByElement(
            context,
            MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(),
            "title",
            null
          ).getFieldID()
        );

        DatabaseManager.applyOffsetAndLimit(query, params, offset, limit);

        try
        {
            tri = DatabaseManager.query(
              context, query.toString(), params.toArray()
            );

            collections = new ArrayList<Collection>();

            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Collection fromCache = (Collection) context.fromCache(
                        Collection.class, row.getIntColumn("collection_id"));

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(new Collection(context, row));
                }
            }
        }
        catch (SQLException e)
        {
            log.error("Find all Collections offset/limit - ", e);
            throw e;
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }
=======
    void setSubmitters(Group submitters) {
        this.submitters = submitters;
        setModified();
    }

>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

    /**
     * Get the default group of administrators, if there is one. Note that the
     * authorization system may allow others to be administrators for the
     * collection.
     * <P>
     * The default group of administrators for collection 100 is the one called
     * <code>collection_100_admin</code>.
     *
     * @return group of administrators, or <code>null</code> if there is no
     *         default group.
     */
    public Group getAdministrators()
    {
        return admins;
    }

<<<<<<< HEAD
    /**
     * Get the in_archive items in this collection. The order is indeterminate.
     * Provides the ability to use limit and offset, for efficient paging.
     * @param limit Max number of results in set
     * @param offset Number of results to jump ahead by. 100 = 100th result is first, not 100th page.
     * @return an iterator over the items in the collection.
     * @throws SQLException
     */
    public ItemIterator getItems(Integer limit, Integer offset) throws SQLException
    {
        List<Serializable> params = new ArrayList<Serializable>();
        StringBuffer myQuery = new StringBuffer(
            "SELECT item.* " + 
            "FROM item, collection2item " + 
            "WHERE item.item_id = collection2item.item_id " +
              "AND collection2item.collection_id = ? " +
              "AND item.in_archive = '1'"
        );

        params.add(getID());
        DatabaseManager.applyOffsetAndLimit(myQuery, params, offset, limit);

        TableRowIterator rows = DatabaseManager.query(ourContext,
                myQuery.toString(), params.toArray());
=======
    void setAdmins(Group admins) {
        this.admins = admins;
        setModified();
    }
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

    public Group getWorkflowStep1() {
        return workflowStep1;
    }

    public Group getWorkflowStep2() {
        return workflowStep2;
    }

    public Group getWorkflowStep3() {
        return workflowStep3;
    }

    void setWorkflowStep1(Group workflowStep1) {
        this.workflowStep1 = workflowStep1;
        setModified();
    }

    void setWorkflowStep2(Group workflowStep2) {
        this.workflowStep2 = workflowStep2;
        setModified();
    }

    void setWorkflowStep3(Group workflowStep3) {
        this.workflowStep3 = workflowStep3;
        setModified();
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection.
     *
     * @return the license for this collection
     */
    public String getLicenseCollection()
    {
        return getCollectionService().getMetadata(this, "license");
    }

    /**
     * Set the license for this collection. Passing in <code>null</code> means
     * that the site-wide default will be used.
     *
     * @param context context
     * @param license the license, or <code>null</code>
     * @throws SQLException if database error
     */
    public void setLicense(Context context, String license) throws SQLException {
        getCollectionService().setMetadata(context, this, "license", license);
    }

    /**
     * Get the template item for this collection. <code>null</code> is
     * returned if the collection does not have a template. Submission
     * mechanisms may copy this template to provide a convenient starting point
     * for a submission.
     *
     * @return the item template, or <code>null</code>
     * @throws SQLException if database error
     */
    public Item getTemplateItem() throws SQLException
    {
        return template;
    }

    void setTemplateItem(Item template) {
        this.template = template;
        setModified();
    }

    /**
     * Get the communities this collection appears in
     *
     * @return array of <code>Community</code> objects
     * @throws SQLException if database error
     */
    public List<Community> getCommunities() throws SQLException
    {
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Community[] output = communities.toArray(new Community[]{});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
    }

    void addCommunity(Community community) {
        this.communities.add(community);
        setModified();
    }

    void removeCommunity(Community community) {
        this.communities.remove(community);
        setModified();
    }


    /**
     * Return <code>true</code> if <code>other</code> is the same Collection
     * as this object, <code>false</code> otherwise
     *
     * @param other
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         collection as this object
     */
<<<<<<< HEAD
    public Group createWorkflowGroup(int step) throws SQLException,
            AuthorizeException
    {
        // Check authorisation - Must be an Admin to create Workflow Group
        AuthorizeUtil.authorizeManageWorkflowsGroup(ourContext, this);

        if (workflowGroup[step - 1] == null)
        {
            //turn off authorization so that Collection Admins can create Collection Workflow Groups
            ourContext.turnOffAuthorisationSystem();
            Group g = Group.create(ourContext);
            ourContext.restoreAuthSystemState();

            g.setName("COLLECTION_" + getID() + "_WORKFLOW_STEP_" + step);
            g.update();
            setWorkflowGroup(step, g);
        }
=======
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
         final Collection otherCollection = (Collection) other;
         if (!this.getID().equals(otherCollection.getID() ))
         {
             return false;
         }

         return true;
     }
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

     @Override
     public int hashCode()
     {
         int hash = 5;
         hash += 71 * hash + getType();
         hash += 71 * hash + getID().hashCode();
         return hash;
     }

    /**
<<<<<<< HEAD
     * Set the workflow group corresponding to a particular workflow step.
     * <code>null</code> can be passed in if there should be no associated
     * group for that workflow step.  Any existing group is NOT deleted.
     *
     * @param step
     *            the workflow step (1-3)
     * @param newGroup
     *            the new workflow group, or <code>null</code>
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public void setWorkflowGroup(int step, Group newGroup)
            throws SQLException, AuthorizeException
    {
        Group oldGroup = getWorkflowGroup(step);
        String stepColumn;
        int action;
        switch(step)
        {
        case 1:
            action = Constants.WORKFLOW_STEP_1;
            stepColumn = "workflow_step_1";
            break;
        case 2:
            action = Constants.WORKFLOW_STEP_2;
            stepColumn = "workflow_step_2";
            break;
        case 3:
            action = Constants.WORKFLOW_STEP_3;
            stepColumn = "workflow_step_3";
            break;
        default:
            throw new IllegalArgumentException("Illegal step count:  " + step);
        }
        workflowGroup[step-1] = newGroup;
        if (newGroup != null)
            collectionRow.setColumn(stepColumn, newGroup.getID());
        else
            collectionRow.setColumnNull(stepColumn);
        modified = true;

        // Deal with permissions.
        try {
            ourContext.turnOffAuthorisationSystem();
            // remove the policies for the old group
            if (oldGroup != null)
            {
                List<ResourcePolicy> oldPolicies = AuthorizeManager
                        .getPoliciesActionFilter(ourContext, this, action);
                int oldGroupID = oldGroup.getID();
                for (ResourcePolicy rp : oldPolicies)
                {
                    if (rp.getGroupID() == oldGroupID)
                        rp.delete();
                }

                oldPolicies = AuthorizeManager
                        .getPoliciesActionFilter(ourContext, this, Constants.ADD);
                for (ResourcePolicy rp : oldPolicies)
                {
                    if ((rp.getGroupID() == oldGroupID)
                            && ResourcePolicy.TYPE_WORKFLOW.equals(rp.getRpType()))
                        rp.delete();
                }
           }

            // New group can be null to delete workflow step.
            // We need to grant permissions if new group is not null.
            if (newGroup != null)
            {
                AuthorizeManager.addPolicy(ourContext, this, action, newGroup,
                        ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(ourContext, this, Constants.ADD, newGroup,
                        ResourcePolicy.TYPE_WORKFLOW);
            }
        } finally {
            ourContext.restoreAuthSystemState();
        }
=======
     * return type found in Constants
     *
     * @return int Constants.COLLECTION
     */
    @Override
    public int getType()
    {
        return Constants.COLLECTION;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    }

    public void setWorkflowGroup(Context context, int step, Group g)
            throws SQLException, AuthorizeException 
    {
        getCollectionService().setWorkflowGroup(context, this, step, g);
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    private CollectionService getCollectionService() {
        if(collectionService == null)
        {
<<<<<<< HEAD
            // Fallback to site-wide default
            license = LicenseManager.getDefaultSubmissionLicense();
        }

        return license;
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection.
     *
     * @return the license for this collection
     */
    public String getLicenseCollection()
    {
        return getMetadata("license");
    }

    /**
     * Find out if the collection has a custom license
     *
     * @return <code>true</code> if the collection has a custom license
     */
    public boolean hasCustomLicense()
    {
        String license = getMetadata("license");

        return !( license == null || license.trim().equals("") );
    }

    /**
     * Set the license for this collection. Passing in <code>null</code> means
     * that the site-wide default will be used.
     *
     * @param license
     *            the license, or <code>null</code>
     */
    public void setLicense(String license) {
        setMetadata("license",license);
    }

    /**
     * Get the template item for this collection. <code>null</code> is
     * returned if the collection does not have a template. Submission
     * mechanisms may copy this template to provide a convenient starting point
     * for a submission.
     *
     * @return the item template, or <code>null</code>
     */
    public Item getTemplateItem() throws SQLException
    {
        return template;
    }

    /**
     * Create an empty template item for this collection. If one already exists,
     * no action is taken. Caution: Make sure you call <code>update</code> on
     * the collection after doing this, or the item will have been created but
     * the collection record will not refer to it.
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void createTemplateItem() throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(ourContext, this);

        if (template == null)
        {
            template = Item.create(ourContext);
            collectionRow.setColumn("template_item_id", template.getID());

            log.info(LogManager.getHeader(ourContext, "create_template_item",
                    "collection_id=" + getID() + ",template_item_id="
                            + template.getID()));
        }
        modified = true;
    }

    /**
     * Remove the template item for this collection, if there is one. Note that
     * since this has to remove the old template item ID from the collection
     * record in the database, the collection record will be changed, including
     * any other changes made; in other words, this method does an
     * <code>update</code>.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeTemplateItem() throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation
        AuthorizeUtil.authorizeManageTemplateItem(ourContext, this);

        collectionRow.setColumnNull("template_item_id");
        DatabaseManager.update(ourContext, collectionRow);

        if (template != null)
        {
            log.info(LogManager.getHeader(ourContext, "remove_template_item",
                    "collection_id=" + getID() + ",template_item_id="
                            + template.getID()));
            // temporarily turn off auth system, we have already checked the permission on the top of the method
            // check it again will fail because we have already broken the relation between the collection and the item
            ourContext.turnOffAuthorisationSystem();
            template.delete();
            ourContext.restoreAuthSystemState();
            template = null;
        }

        ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION, 
                getID(), "remove_template_item", getIdentifiers(ourContext)));
    }

    /**
     * Add an item to the collection. This simply adds a relationship between
     * the item and the collection - it does nothing like set an issue date,
     * remove a personal workspace item etc. This has instant effect;
     * <code>update</code> need not be called.
     *
     * @param item
     *            item to add
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void addItem(Item item) throws SQLException, AuthorizeException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);

        log.info(LogManager.getHeader(ourContext, "add_item", "collection_id="
                + getID() + ",item_id=" + item.getID()));

        // Create mapping
        TableRow row = DatabaseManager.row("collection2item");

        row.setColumn("collection_id", getID());
        row.setColumn("item_id", item.getID());

        DatabaseManager.insert(ourContext, row);

        ourContext.addEvent(new Event(Event.ADD, Constants.COLLECTION, getID(), 
                Constants.ITEM, item.getID(), item.getHandle(), 
                getIdentifiers(ourContext)));
    }

    /**
     * Remove an item. If the item is then orphaned, it is deleted.
     *
     * @param item
     *            item to remove
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeItem(Item item) throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        // will the item be an orphan? is it in other collections?
        TableRow row = DatabaseManager.querySingle(ourContext,
                "SELECT COUNT(DISTINCT collection_id) AS num FROM collection2item WHERE item_id= ? ",
                item.getID());
        boolean orphan = (row.getLongColumn("num") == 1);

        log.info(LogManager.getHeader(ourContext, "remove_item",
                "collection_id=" + getID() + ",item_id=" + item.getID()));

        // First, remove its association with this collection
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM collection2item WHERE collection_id= ? "+
                "AND item_id= ? ",
                getID(), item.getID());

        // Then, if it is an orphaned Item, delete it
        if (orphan)
        {
            item.delete();
        }

        ourContext.addEvent(new Event(Event.REMOVE, Constants.COLLECTION, 
                getID(), Constants.ITEM, item.getID(), item.getHandle(),
                getIdentifiers(ourContext)));
    }

    /**
     * Update the collection metadata (including logo and workflow groups) to
     * the database. Inserts if this is a new collection.
     *
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation
        canEdit(true);

        log.info(LogManager.getHeader(ourContext, "update_collection",
                "collection_id=" + getID()));

        DatabaseManager.update(ourContext, collectionRow);

        if (modified)
        {
            ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION, 
                    getID(), null, getIdentifiers(ourContext)));
            modified = false;
        }
        if (modifiedMetadata)
        {
            updateMetadata();
            clearDetails();
        }
    }

    public boolean canEditBoolean() throws java.sql.SQLException
    {
        return canEditBoolean(true);
    }

    public boolean canEditBoolean(boolean useInheritance) throws java.sql.SQLException
    {
        try
        {
            canEdit(useInheritance);

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    public void canEdit()  throws AuthorizeException, SQLException
    {
        canEdit(true);
    }

    public void canEdit(boolean useInheritance) throws AuthorizeException, SQLException
    {
        Community[] parents = getCommunities();

        for (int i = 0; i < parents.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.WRITE, useInheritance))
            {
                return;
            }

            if (AuthorizeManager.authorizeActionBoolean(ourContext, parents[i],
                    Constants.ADD, useInheritance))
            {
                return;
            }
        }

        AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE, useInheritance);
    }

    /**
     * Delete the collection, including the metadata and logo. Items that are
     * then orphans are deleted. Groups associated with this collection
     * (workflow participants and submitters) are NOT deleted.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    void delete() throws SQLException, AuthorizeException, IOException
    {
        log.info(LogManager.getHeader(ourContext, "delete_collection",
                "collection_id=" + getID()));

        ourContext.addEvent(new Event(Event.DELETE, Constants.COLLECTION, 
                getID(), getHandle(), getIdentifiers(ourContext)));

        // remove subscriptions - hmm, should this be in Subscription.java?
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM subscription WHERE collection_id= ? ",
                getID());

        // Remove Template Item
        removeTemplateItem();

        // Remove items
        ItemIterator items = getAllItems();

        try
        {
        	while (items.hasNext())
        	{
        		Item item = items.next();
        		IndexBrowse ib = new IndexBrowse(ourContext);

        		if (item.isOwningCollection(this))
        		{
        			// the collection to be deleted is the owning collection, thus remove
        			// the item from all collections it belongs to
        			Collection[] collections = item.getCollections();
        			for (int i=0; i< collections.length; i++)
        			{
        				//notify Browse of removing item.
        				ib.itemRemoved(item);
        				// Browse.itemRemoved(ourContext, itemId);
        				collections[i].removeItem(item);
        			}

        		}
        		// the item was only mapped to this collection, so just remove it
        		else
        		{
        			//notify Browse of removing item mapping.
        			ib.indexItem(item);
        			// Browse.itemChanged(ourContext, item);
        			removeItem(item);
        		}
        	}
        }
        catch (BrowseException e)
        {
        	log.error("caught exception: ", e);
        	throw new IOException(e.getMessage(), e);
        }
        finally
        {
            if (items != null)
            {
                items.close();
            }
        }

        // Delete bitstream logo
        setLogo(null);

        // Remove all authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);

        if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){
            // Remove any xml_WorkflowItems
            XmlWorkflowItem[] xmlWfarray = XmlWorkflowItem
                    .findByCollection(ourContext, this);

            for (XmlWorkflowItem aXmlWfarray : xmlWfarray) {
                // remove the workflowitem first, then the item
                Item myItem = aXmlWfarray.getItem();
                aXmlWfarray.deleteWrapper();
                myItem.delete();
            }
        }else{
            // Remove any WorkflowItems
            WorkflowItem[] wfarray = WorkflowItem
                    .findByCollection(ourContext, this);

            for (WorkflowItem aWfarray : wfarray) {
                // remove the workflowitem first, then the item
                Item myItem = aWfarray.getItem();
                aWfarray.deleteWrapper();
                myItem.delete();
            }
        }



        // Remove any WorkspaceItems
        WorkspaceItem[] wsarray = WorkspaceItem.findByCollection(ourContext,
                this);

        for (WorkspaceItem aWsarray : wsarray) {
            aWsarray.deleteAll();
        }

        //  get rid of the content count cache if it exists
        try
        {
        	ItemCounter ic = new ItemCounter(ourContext);
        	ic.remove(this);
        }
        catch (ItemCountException e)
        {
        	// FIXME: upside down exception handling due to lack of good
        	// exception framework
        	throw new IllegalStateException(e.getMessage(), e);
        }

        // Remove any Handle
        HandleManager.unbindHandle(ourContext, this);

        if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){
            // delete all CollectionRoles for this Collection
            for (CollectionRole collectionRole : CollectionRole.findByCollection(ourContext, this.getID())) {
                collectionRole.delete();
            }
        }

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Delete collection row
        DatabaseManager.delete(ourContext, collectionRow);

        // Remove any workflow groups - must happen after deleting collection
        Group g = null;

        g = getWorkflowGroup(1);

        if (g != null)
        {
            g.delete();
        }

        g = getWorkflowGroup(2);

        if (g != null)
        {
            g.delete();
        }

        g = getWorkflowGroup(3);

        if (g != null)
        {
            g.delete();
        }

        // Remove default administrators group
        g = getAdministrators();

        if (g != null)
        {
            g.delete();
        }

        // Remove default submitters group
        g = getSubmitters();

        if (g != null)
        {
            g.delete();
        }

        removeMetadataFromDatabase();
    }

    /**
     * Get the communities this collection appears in
     *
     * @return array of <code>Community</code> objects
     * @throws SQLException
     */
    public Community[] getCommunities() throws SQLException
    {
        // Get the bundle table rows
        TableRowIterator tri = DatabaseManager.queryTable(ourContext,"community",
                        "SELECT community.* FROM community, community2collection WHERE " +
                        "community.community_id=community2collection.community_id " +
                        "AND community2collection.collection_id= ? ",
                        getID());

        // Build a list of Community objects
        List<Community> communities = new ArrayList<Community>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Community owner = (Community) ourContext.fromCache(Community.class,
                        row.getIntColumn("community_id"));

                if (owner == null)
                {
                    owner = new Community(ourContext, row);
                }

                communities.add(owner);

                // now add any parent communities
                Community[] parents = owner.getAllParents();
                communities.addAll(Arrays.asList(parents));
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
     * Return <code>true</code> if <code>other</code> is the same Collection
     * as this object, <code>false</code> otherwise
     *
     * @param other
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         collection as this object
     */
     @Override
     public boolean equals(Object other)
     {
         if (other == null)
         {
             return false;
         }
         if (getClass() != other.getClass())
         {
             return false;
         }
         final Collection otherCollection = (Collection) other;
         if (this.getID() != otherCollection.getID())
         {
             return false;
         }

         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 7;
         hash = 89 * hash + (this.collectionRow != null ? this.collectionRow.hashCode() : 0);
         return hash;
     }


    /**
     * Utility method for reading in a group from a group ID in a column. If the
     * column is null, null is returned.
     *
     * @param col
     *            the column name to read
     * @return the group referred to by that column, or null
     * @throws SQLException
     */
    private Group groupFromColumn(String col) throws SQLException
    {
        if (collectionRow.isColumnNull(col))
        {
            return null;
        }

        return Group.find(ourContext, collectionRow.getIntColumn(col));
    }

    /**
     * return type found in Constants
     *
     * @return int Constants.COLLECTION
     */
    public int getType()
    {
        return Constants.COLLECTION;
    }

    /**
     * return an array of collections that user has a given permission on
     * (useful for trimming 'select to collection' list) or figuring out which
     * collections a person is an editor for.
     *
     * @param context
     * @param comm
     *            (optional) restrict search to a community, else null
     * @param actionID
     *            of the action
     *
     * @return Collection [] of collections with matching permissions
     * @throws SQLException
     */
    public static Collection[] findAuthorized(Context context, Community comm,
            int actionID) throws java.sql.SQLException
    {
        List<Collection> myResults = new ArrayList<Collection>();

        Collection[] myCollections = null;

        if (comm != null)
        {
            myCollections = comm.getCollections();
        }
        else
        {
            myCollections = Collection.findAll(context);
        }

        // now build a list of collections you have authorization for
        for (int i = 0; i < myCollections.length; i++)
        {
            if (AuthorizeManager.authorizeActionBoolean(context,
                    myCollections[i], actionID))
            {
                myResults.add(myCollections[i]);
            }
        }

        myCollections = new Collection[myResults.size()];
        myCollections = (Collection[]) myResults.toArray(myCollections);

        return myCollections;
    }

    public static Collection[] findAuthorizedOptimized(Context context, int actionID) throws java.sql.SQLException
    {
        if(! ConfigurationManager.getBooleanProperty("org.dspace.content.Collection.findAuthorizedPerformanceOptimize", false)) {
            // Fallback to legacy query if config says so. The rationale could be that a site found a bug.
            return findAuthorized(context, null, actionID);
        }

        List<Collection> myResults = new ArrayList<Collection>();

        if(AuthorizeManager.isAdmin(context))
        {
            return findAll(context);
        }

        //Check eperson->policy
        Collection[] directToCollection = findDirectMapped(context, actionID);
        for (int i = 0; i< directToCollection.length; i++)
        {
            if(!myResults.contains(directToCollection[i]))
            {
                myResults.add(directToCollection[i]);
            }
        }

        //Check eperson->groups->policy
        Collection[] groupToCollection = findGroupMapped(context, actionID);

        for (int i = 0; i< groupToCollection.length; i++)
        {
            if(!myResults.contains(groupToCollection[i]))
            {
                myResults.add(groupToCollection[i]);
            }
        }

        //Check eperson->groups->groups->policy->collection
        //i.e. Malcolm Litchfield is a member of OSU_Press_Embargo,
        // which is a member of: COLLECTION_24_ADMIN, COLLECTION_24_SUBMIT
        Collection[] group2GroupToCollection = findGroup2GroupMapped(context, actionID);

        for (int i = 0; i< group2GroupToCollection.length; i++)
        {
            if(!myResults.contains(group2GroupToCollection[i]))
            {
                myResults.add(group2GroupToCollection[i]);
            }
        }

        //TODO Check eperson->groups->groups->policy->community


        //TODO Check eperson->groups->policy->community
        // i.e. Typical Community Admin -- name.# > COMMUNITY_10_ADMIN > Ohio State University Press

        //Check eperson->comm-admin
        Collection[] group2commCollections = findGroup2CommunityMapped(context);
        for (int i = 0; i< group2commCollections.length; i++)
        {
            if(!myResults.contains(group2commCollections[i]))
            {
                myResults.add(group2commCollections[i]);
            }
        }


        // Return the collections, sorted alphabetically
        Collections.sort(myResults, new CollectionComparator());

        Collection[] myCollections = new Collection[myResults.size()];
        myCollections = (Collection[]) myResults.toArray(myCollections);

        return myCollections;


    }

	/**
     * counts items in this collection
     *
     * @return  total items
     */
     public int countItems()
        throws SQLException
     {
         int itemcount = 0;
         PreparedStatement statement = null;
         ResultSet rs = null;

         try
         {
             String query = "SELECT count(*) FROM collection2item, item WHERE "
                    + "collection2item.collection_id =  ? "
                    + "AND collection2item.item_id = item.item_id "
                    + "AND in_archive ='1' AND item.withdrawn='0' ";

            statement = ourContext.getDBConnection().prepareStatement(query);
            statement.setInt(1,getID());

            rs = statement.executeQuery();
            if (rs != null)
            {
                rs.next();
                itemcount = rs.getInt(1);
            }
         }
         finally
         {
             if (rs != null)
             {
                 try { rs.close(); } catch (SQLException sqle) { }
             }

             if (statement != null)
             {
                 try { statement.close(); } catch (SQLException sqle) { }
             }
         }

        return itemcount;
     }

    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        DSpaceObject adminObject = null;
        Community community = null;
        Community[] communities = getCommunities();
        if (communities != null && communities.length > 0)
        {
            community = communities[0];
        }

        switch (action)
        {
        case Constants.REMOVE:
            if (AuthorizeConfiguration.canCollectionAdminPerformItemDeletion())
            {
                adminObject = this;
            }
            else if (AuthorizeConfiguration.canCommunityAdminPerformItemDeletion())
            {
                adminObject = community;
            }
            break;

        case Constants.DELETE:
            if (AuthorizeConfiguration.canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = community;
            }
            break;
        default:
            adminObject = this;
            break;
        }
        return adminObject;
    }

    @Override
    public DSpaceObject getParentObject() throws SQLException
    {
        Community[] communities = this.getCommunities();
        if (communities != null && (communities.length > 0 && communities[0] != null))
        {
            return communities[0];
        }
        else
        {
            return null;
        }
    }

    @Override
    public void updateLastModified()
    {
        //Also fire a modified event since the collection HAS been modified
        ourContext.addEvent(new Event(Event.MODIFY, Constants.COLLECTION, 
                getID(), null, getIdentifiers(ourContext)));
    }

    //TODO replace hard-coded action_id's with constants...
    public static Collection[] findDirectMapped(Context context, int actionID) throws java.sql.SQLException
    {
        //eperson_id -> resourcepolicy.eperson_id
        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT * FROM collection, resourcepolicy, eperson " +
                        "WHERE resourcepolicy.resource_id = collection.collection_id AND " +
                        "eperson.eperson_id = resourcepolicy.eperson_id AND "+
                        "resourcepolicy.resource_type_id = 3 AND "+
                        "( resourcepolicy.action_id = 3 OR resourcepolicy.action_id = 11 ) AND "+
                        "eperson.eperson_id = ?", context.getCurrentUser().getID());
        return produceCollectionsFromQuery(context, tri);
    }

    public static Collection[] findGroupMapped(Context context, int actionID) throws java.sql.SQLException
    {
        //eperson_id -> resourcepolicy.eperson_id
        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT * FROM collection, resourcepolicy, eperson, epersongroup2eperson " +
                        "WHERE resourcepolicy.resource_id = collection.collection_id AND "+
                        "eperson.eperson_id = epersongroup2eperson.eperson_id AND "+
                        "epersongroup2eperson.eperson_group_id = resourcepolicy.epersongroup_id AND "+
                        "resourcepolicy.resource_type_id = 3 AND "+
                        "( resourcepolicy.action_id = 3 OR resourcepolicy.action_id = 11 ) AND "+
                        "eperson.eperson_id = ?", context.getCurrentUser().getID());
        return produceCollectionsFromQuery(context, tri);
    }

    public static Collection[] findGroup2GroupMapped(Context context, int actionID) throws SQLException {
        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT \n" +
                        "  * \n" +
                        "FROM \n" +
                        "  public.eperson, \n" +
                        "  public.epersongroup2eperson, \n" +
                        "  public.epersongroup, \n" +
                        "  public.group2group, \n" +
                        "  public.resourcepolicy rp_parent, \n" +
                        "  public.collection\n" +
                        "WHERE \n" +
                        "  epersongroup2eperson.eperson_id = eperson.eperson_id AND\n" +
                        "  epersongroup.eperson_group_id = epersongroup2eperson.eperson_group_id AND\n" +
                        "  group2group.child_id = epersongroup.eperson_group_id AND\n" +
                        "  rp_parent.epersongroup_id = group2group.parent_id AND\n" +
                        "  collection.collection_id = rp_parent.resource_id AND\n" +
                        "  eperson.eperson_id = ? AND \n" +
                        "  (rp_parent.action_id = 3 OR \n" +
                        "  rp_parent.action_id = 11  \n" +
                        "  )  AND rp_parent.resource_type_id = 3;", context.getCurrentUser().getID());
        return produceCollectionsFromQuery(context, tri);
    }

    public static Collection[] findGroup2CommunityMapped(Context context) throws SQLException {
        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT \n" +
                        "  * \n" +
                        "FROM \n" +
                        "  public.eperson, \n" +
                        "  public.epersongroup2eperson, \n" +
                        "  public.epersongroup, \n" +
                        "  public.community, \n" +
                        "  public.resourcepolicy\n" +
                        "WHERE \n" +
                        "  epersongroup2eperson.eperson_id = eperson.eperson_id AND\n" +
                        "  epersongroup.eperson_group_id = epersongroup2eperson.eperson_group_id AND\n" +
                        "  resourcepolicy.epersongroup_id = epersongroup.eperson_group_id AND\n" +
                        "  resourcepolicy.resource_id = community.community_id AND\n" +
                        " ( resourcepolicy.action_id = 3 OR \n" +
                        "  resourcepolicy.action_id = 11) AND \n" +
                        "  resourcepolicy.resource_type_id = 4 AND eperson.eperson_id = ?", context.getCurrentUser().getID());

        return produceCollectionsFromCommunityQuery(context, tri);
    }

    public static class CollectionComparator implements Comparator<Collection> {
        @Override
        public int compare(Collection collection1, Collection collection2) {
            return collection1.getName().compareTo(collection2.getName());
        }
    }

    public static Collection[] produceCollectionsFromQuery(Context context, TableRowIterator tri) throws SQLException {
        List<Collection> collections = new ArrayList<Collection>();

        while(tri.hasNext()) {
            TableRow row = tri.next();
            Collection collection = Collection.find(context, row.getIntColumn("collection_id"));
            collections.add(collection);
        }

        return collections.toArray(new Collection[0]);
    }

    public static Collection[] produceCollectionsFromCommunityQuery(Context context, TableRowIterator tri) throws SQLException {
        List<Collection> collections = new ArrayList<Collection>();

        while(tri.hasNext()) {
            TableRow commRow = tri.next();
            Community community = Community.find(context, commRow.getIntColumn("community_id"));

            Collection[] comCollections = community.getCollections();
            for(Collection collection : comCollections) {
                collections.add(collection);
            }

            //ugh, handle that communities has subcommunities...
            //TODO  community.getAllCollections();

=======
            collectionService = ContentServiceFactory.getInstance().getCollectionService();
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
        }
        return collectionService;
    }
}
