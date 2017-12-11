/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowItem;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an item in the process of being submitted by a user
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
@Entity
@Table(name = "workspaceitem")
public class WorkspaceItem implements InProgressSubmission, Serializable, ReloadableEntity<Integer>
{

    @Id
    @Column(name = "workspace_item_id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="workspaceitem_seq")
    @SequenceGenerator(name="workspaceitem_seq", sequenceName="workspaceitem_seq", allocationSize = 1)
    private Integer workspaceItemId;

    /** The item this workspace object pertains to */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;


    /** The collection the item is being submitted to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

<<<<<<< HEAD
    /**
     * Construct a workspace item corresponding to the given database row
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the database row
     */
    WorkspaceItem(Context context, TableRow row) throws SQLException
    {
        ourContext = context;
        wiRow = row;

        item = Item.find(context, wiRow.getIntColumn("item_id"));
        collection = Collection.find(context, wiRow
                .getIntColumn("collection_id"));

        // Cache ourselves
        context.cache(this, row.getIntColumn("workspace_item_id"));
    }

    /**
     * Get a workspace item from the database. The item, collection and
     * submitter are loaded into memory.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the workspace item
     * 
     * @return the workspace item, or null if the ID is invalid.
     * @throws java.sql.SQLException passed through.
     */
    public static WorkspaceItem find(Context context, int id)
            throws SQLException
    {
        // First check the cache
        WorkspaceItem fromCache = (WorkspaceItem) context.fromCache(
                WorkspaceItem.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "workspaceitem", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workspace_item",
                        "not_found,workspace_item_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workspace_item",
                        "workspace_item_id=" + id));
            }

            return new WorkspaceItem(context, row);
        }
    }

    /**
     * Create a new workspace item, with a new ID. An Item is also created. The
     * submitter is the current user in the context.
     * 
     * @param c
     *            DSpace context object
     * @param coll
     *            Collection being submitted to
     * @param template
     *            if <code>true</code>, the workspace item starts as a copy
     *            of the collection's template item
     * 
     * @return the newly created workspace item
     * @throws org.dspace.authorize.AuthorizeException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     */
    public static WorkspaceItem create(Context c, Collection coll,
            boolean template) throws AuthorizeException, SQLException,
            IOException
    {
        // Check the user has permission to ADD to the collection
        AuthorizeManager.authorizeAction(c, coll, Constants.ADD);

        // Create an item
        Item item = Item.create(c);
        item.setSubmitter(c.getCurrentUser());

        // Now create the policies for the submitter to modify item and contents.
        // contents = bitstreams, bundles
        // FIXME: icky hardcoded workflow steps
        Group step1group = coll.getWorkflowGroup(1);
        Group step2group = coll.getWorkflowGroup(2);
        Group step3group = coll.getWorkflowGroup(3);

        EPerson submitter = c.getCurrentUser();

        // Add policies for the submitter
        AuthorizeManager.addPolicy(c, item, Constants.READ, submitter, ResourcePolicy.TYPE_SUBMISSION);
        AuthorizeManager.addPolicy(c, item, Constants.WRITE, submitter, ResourcePolicy.TYPE_SUBMISSION);
        AuthorizeManager.addPolicy(c, item, Constants.ADD, submitter, ResourcePolicy.TYPE_SUBMISSION);
        AuthorizeManager.addPolicy(c, item, Constants.REMOVE, submitter, ResourcePolicy.TYPE_SUBMISSION);

        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("originalworkflow"))
        {
            // Add policies for the workflow step administrative groups
            if (step1group != null)
            {
                AuthorizeManager.addPolicy(c, item, Constants.READ, step1group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.WRITE, step1group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.ADD, step1group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.REMOVE, step1group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step2group != null)
            {
                AuthorizeManager.addPolicy(c, item, Constants.READ, step2group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.WRITE, step2group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.ADD, step2group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.REMOVE, step2group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step3group != null)
            {
                AuthorizeManager.addPolicy(c, item, Constants.READ, step3group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.WRITE, step3group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.ADD, step3group, ResourcePolicy.TYPE_WORKFLOW);
                AuthorizeManager.addPolicy(c, item, Constants.REMOVE, step3group, ResourcePolicy.TYPE_WORKFLOW);
            }
        }

        // Copy template if appropriate
        Item templateItem = coll.getTemplateItem();

        if (template && (templateItem != null))
        {
            Metadatum[] md = templateItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (int n = 0; n < md.length; n++)
            {
                item.addMetadata(md[n].schema, md[n].element, md[n].qualifier, md[n].language,
                        md[n].value);
            }
        }

        item.update();

        // Create the workspace item row
        TableRow row = DatabaseManager.row("workspaceitem");

        row.setColumn("item_id", item.getID());
        row.setColumn("collection_id", coll.getID());

        log.info(LogManager.getHeader(c, "create_workspace_item",
                "workspace_item_id=" + row.getIntColumn("workspace_item_id")
                        + "item_id=" + item.getID() + "collection_id="
                        + coll.getID()));

        DatabaseManager.insert(c, row);

        WorkspaceItem wi = new WorkspaceItem(c, row);

        return wi;
    }

    /**
     * Get all workspace items for a particular e-person. These are ordered by
     * workspace item ID, since this should likely keep them in the order in
     * which they were created.
     * 
     * @param context
     *            the context object
     * @param ep
     *            the eperson
     * 
     * @return the corresponding workspace items
     * @throws java.sql.SQLException passed through.
     */
    public static WorkspaceItem[] findByEPerson(Context context, EPerson ep)
            throws SQLException
    {
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

        TableRowIterator tri = DatabaseManager.queryTable(context, "workspaceitem",
                "SELECT workspaceitem.* FROM workspaceitem, item WHERE " +
                "workspaceitem.item_id=item.item_id AND " +
                "item.submitter_id= ? " +
                "ORDER BY workspaceitem.workspace_item_id", 
                ep.getID());
=======
    @Column(name = "multiple_titles")
    private boolean multipleTitles = false;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

    @Column(name = "published_before")
    private boolean publishedBefore = false;

<<<<<<< HEAD
        return wsItems.toArray(new WorkspaceItem[wsItems.size()]);
    }

    /**
     * Get all workspace items for a particular collection.
     * 
     * @param context
     *            the context object
     * @param c
     *            the collection
     * 
     * @return the corresponding workspace items
     * @throws java.sql.SQLException passed through.
     */
    public static WorkspaceItem[] findByCollection(Context context, Collection c)
            throws SQLException
    {
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();
=======
    @Column(name = "multiple_files")
    private boolean multipleFiles = false;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

    @Column(name = "stage_reached")
    private Integer stageReached = -1;

    @Column(name = "page_reached")
    private Integer pageReached = -1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "epersongroup2workspaceitem",
            joinColumns = {@JoinColumn(name = "workspace_item_id") },
            inverseJoinColumns = {@JoinColumn(name = "eperson_group_id") }
    )
    private final List<Group> supervisorGroups = new ArrayList<>();

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.WorkspaceItemService#create(Context, Collection, boolean)}
     * or
     * {@link org.dspace.content.service.WorkspaceItemService#create(Context, WorkflowItem)}
     *
<<<<<<< HEAD
     * @return workflow item corresponding to the item, or null
     * @throws java.sql.SQLException passed through.
=======
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
     */
    protected WorkspaceItem()
    {

    }

<<<<<<< HEAD

    /**
     * Get all workspace items in the whole system
     *
     * @param   context     the context object
     *
     * @return      all workspace items
     * @throws java.sql.SQLException passed through.
     */
    public static WorkspaceItem[] findAll(Context context)
        throws SQLException
    {
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();
        String query = "SELECT * FROM workspaceitem ORDER BY item_id";
        TableRowIterator tri = DatabaseManager.queryTable(context,
                                    "workspaceitem",
                                    query);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Check the cache
                WorkspaceItem wi = (WorkspaceItem) context.fromCache(
                        WorkspaceItem.class, row.getIntColumn("workspace_item_id"));

                // not in cache? turn row into workspaceitem
                if (wi == null)
                {
                    wi = new WorkspaceItem(context, row);
                }

                wsItems.add(wi);
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
        
        return wsItems.toArray(new WorkspaceItem[wsItems.size()]);
    }
    
=======
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    /**
     * Get the internal ID of this workspace item
     * 
     * @return the internal identifier
     */
    @Override
    public Integer getID()
    {
        return workspaceItemId;
    }

    /**
     * Get the value of the stage reached column
     * 
     * @return the value of the stage reached column
     */
    public int getStageReached()
    {
        return stageReached;
    }

    /**
     * Set the value of the stage reached column
     * 
     * @param v
     *            the value of the stage reached column
     */
    public void setStageReached(int v)
    {
        stageReached = v;
    }

    /**
     * Get the value of the page reached column (which represents the page
     * reached within a stage/step)
     * 
     * @return the value of the page reached column
     */
    public int getPageReached()
    {
        return pageReached;
    }

    /**
     * Set the value of the page reached column (which represents the page
     * reached within a stage/step)
     * 
     * @param v
     *            the value of the page reached column
     */
    public void setPageReached(int v)
    {
<<<<<<< HEAD
        wiRow.setColumn("page_reached", v);
    }

    /**
     * Update the workspace item, including the unarchived item.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Authorisation is checked by the item.update() method below

        log.info(LogManager.getHeader(ourContext, "update_workspace_item",
                "workspace_item_id=" + getID()));

        // Update the item
        item.update();

        // Update ourselves
        DatabaseManager.update(ourContext, wiRow);
=======
        pageReached = v;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    }

    /**
     * Decide if this WorkspaceItem is equal to another
     *
     * @param o The other workspace item to compare to
     * @return If they are equal or not
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
        {
            return true;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(o);
        if (getClass() != objClass)
        {
            return false;
        }
        final WorkspaceItem that = (WorkspaceItem)o;
        if (this.getID() != that.getID())
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

<<<<<<< HEAD
    /**
     * Delete the workspace item. The entry in workspaceitem, the unarchived
     * item and its contents are all removed (multiple inclusion
     * notwithstanding.)
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException
     *          if not original submitter or an administrator.
     * @throws java.io.IOException passed through.
     */
    public void deleteAll() throws SQLException, AuthorizeException,
            IOException
    {
        /*
         * Authorisation is a special case. The submitter won't have REMOVE
         * permission on the collection, so our policy is this: Only the
         * original submitter or an administrator can delete a workspace item.

         */
        if (!AuthorizeManager.isAdmin(ourContext)
                && ((ourContext.getCurrentUser() == null) || (ourContext
                        .getCurrentUser().getID() != item.getSubmitter()
                        .getID())))
        {
            // Not an admit, not the submitter
            throw new AuthorizeException("Must be an administrator or the "
                    + "original submitter to delete a workspace item");
        }

        log.info(LogManager.getHeader(ourContext, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //deleteSubmitPermissions();
        // Remove from cache
        ourContext.removeCached(this, getID());

        // Need to delete the epersongroup2workspaceitem row first since it refers
        // to workspaceitem ID
        deleteEpersonGroup2WorkspaceItem();

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(ourContext, wiRow);

        // Delete item
        item.delete();
    }

    private void deleteEpersonGroup2WorkspaceItem() throws SQLException
    {
        
        String removeSQL="DELETE FROM epersongroup2workspaceitem WHERE workspace_item_id = ?";
        DatabaseManager.updateQuery(ourContext, removeSQL,getID());
        
    }

    public void deleteWrapper() throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation. We check permissions on the enclosed item.
        AuthorizeManager.authorizeAction(ourContext, item, Constants.WRITE);

        log.info(LogManager.getHeader(ourContext, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //        deleteSubmitPermissions();
        // Remove from cache
        ourContext.removeCached(this, getID());

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(ourContext, wiRow);
    }

=======
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    // InProgressSubmission methods
    @Override
    public Item getItem()
    {
        return item;
    }

    void setItem(Item item) {
        this.item = item;
    }

    @Override
    public Collection getCollection()
    {
        return collection;
    }

    void setCollection(Collection collection) {
        this.collection = collection;
    }

    @Override
    public EPerson getSubmitter() throws SQLException
    {
        return item.getSubmitter();
    }

    @Override
    public boolean hasMultipleFiles()
    {
        return multipleFiles;
    }

    @Override
    public void setMultipleFiles(boolean b)
    {
        multipleFiles = b;
    }

    @Override
    public boolean hasMultipleTitles()
    {
        return multipleTitles;
    }

    @Override
    public void setMultipleTitles(boolean b)
    {
        multipleTitles = b;
    }

    @Override
    public boolean isPublishedBefore()
    {
        return publishedBefore;
    }

    @Override
    public void setPublishedBefore(boolean b)
    {
        publishedBefore = b;
    }

    public List<Group> getSupervisorGroups() {
        return supervisorGroups;
    }

    void removeSupervisorGroup(Group group)
    {
        supervisorGroups.remove(group);
    }

    void addSupervisorGroup(Group group)
    {
        supervisorGroups.add(group);
    }
}
