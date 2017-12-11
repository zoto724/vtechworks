/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

<<<<<<< HEAD
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
=======
import org.dspace.content.comparator.NameAscendingComparator;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.util.*;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

/**
 * Class representing an item in DSpace.
 * <P>
 * This class holds in memory the item Dublin Core metadata, the bundles in the
 * item, and the bitstreams in those bundles. When modifying the item, if you
 * modify the Dublin Core or the "in archive" flag, you must call
 * <code>update</code> for the changes to be written to the database.
 * Creating, adding or removing bundles or bitstreams has immediate effect in
 * the database.
 *
 * @author Robert Tansley
 * @author Martin Hald
 * @version $Revision$
 */
@Entity
@Table(name="item")
public class Item extends DSpaceObject implements DSpaceObjectLegacySupport
{
    /**
     * Wild card for Dublin Core metadata qualifiers/languages
     */
    public static final String ANY = "*";

    @Column(name="item_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name= "in_archive")
    private boolean inArchive = false;

    @Column(name= "discoverable")
    private boolean discoverable = false;

    @Column(name= "withdrawn")
    private boolean withdrawn = false;

    @Column(name= "last_modified", columnDefinition="timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified = new Date();

<<<<<<< HEAD
        String query = "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' " +
                "AND item.item_id = metadatavalue.resource_id AND metadatavalue.resource_type_id=2 AND metadata_field_id = ?";
        TableRowIterator rows = null;
        if (Item.ANY.equals(authority)) {
            rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID());
        } else {
            query += " AND metadatavalue.authority = ?";
            rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID(), authority);
        }
        return new ItemIterator(context, rows);
    }
=======
    @ManyToOne(fetch = FetchType.LAZY, cascade={CascadeType.PERSIST})
    @JoinColumn(name = "owning_collection")
    private Collection owningCollection;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "template")
    private Collection templateItemOf;

    /** The e-person who submitted this item */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id")
    private EPerson submitter = null;


    /** The bundles in this item - kept in sync with DB */
    @ManyToMany(fetch = FetchType.LAZY, cascade={CascadeType.PERSIST})
    @JoinTable(
            name = "collection2item",
            joinColumns = {@JoinColumn(name = "item_id") },
            inverseJoinColumns = {@JoinColumn(name = "collection_id") }
    )
    private final Set<Collection> collections = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "items")
    private final List<Bundle> bundles = new ArrayList<>();

    @Transient
    private transient ItemService itemService;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.ItemService#create(Context, WorkspaceItem)}
     *
     */
    protected Item()
    {

    }

    /**
     * Find out if the item is part of the main archive
     *
     * @return true if the item is in the main archive
     */
    public boolean isArchived()
    {
        return inArchive;
    }

    /**
     * Find out if the item has been withdrawn
     *
     * @return true if the item has been withdrawn
     */
    public boolean isWithdrawn()
    {
        return withdrawn;
    }


    /**
     * Set an item to be withdrawn, do NOT make this method public, use itemService().withdraw() to withdraw an item
     * @param withdrawn
     */
    void setWithdrawn(boolean withdrawn) {
        this.withdrawn = withdrawn;
    }

    /**
     * Find out if the item is discoverable
     *
     * @return true if the item is discoverable
     */
    public boolean isDiscoverable()
    {
        return discoverable;
    }

    /**
     * Get the date the item was last modified, or the current date if
     * last_modified is null
     *
     * @return the date the item was last modified, or the current date if the
     *         column is null.
     */
    public Date getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Set the "is_archived" flag. This is public and only
     * <code>WorkflowItem.archive()</code> should set this.
     *
     * @param isArchived
     *            new value for the flag
     */
    public void setArchived(boolean isArchived)
    {
        this.inArchive = isArchived;
        setModified();
    }

    /**
     * Set the "discoverable" flag. This is public and only
     *
     * @param discoverable
     *            new value for the flag
     */
    public void setDiscoverable(boolean discoverable)
    {
        this.discoverable = discoverable;
        setModified();
    }

    /**
     * Set the owning Collection for the item
     *
     * @param c
     *            Collection
     */
    public void setOwningCollection(Collection c)
    {
        this.owningCollection = c;
        setModified();
    }

    /**
     * Get the owning Collection for the item
     *
     * @return Collection that is the owner of the item
     */
    public Collection getOwningCollection()
    {
        return owningCollection;
    }

    /**
     * Get the e-person that originally submitted this item
     *
     * @return the submitter
     */
    public EPerson getSubmitter()
    {
        return submitter;
    }

    /**
     * Set the e-person that originally submitted this item. This is a public
     * method since it is handled by the WorkspaceItem class in the ingest
     * package. <code>update</code> must be called to write the change to the
     * database.
     *
     * @param sub
     *            the submitter
     */
    public void setSubmitter(EPerson sub)
    {
        this.submitter = sub;
        setModified();
    }

    /**
     * Get the collections this item is in. The order is sorted ascending by collection name.
     *
     * @return the collections this item is in, if any.
     */
    public List<Collection> getCollections()
    {
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Collection[] output = collections.toArray(new Collection[]{});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
    }

    void addCollection(Collection collection)
    {
        collections.add(collection);
    }

    void removeCollection(Collection collection)
    {
        collections.remove(collection);
    }

    public void clearCollections(){
        collections.clear();
    }

    public Collection getTemplateItemOf() {
        return templateItemOf;
    }


    void setTemplateItemOf(Collection templateItemOf) {
        this.templateItemOf = templateItemOf;
    }

    /**
     * Get the bundles in this item.
     *
     * @return the bundles in an unordered array
     */
    public List<Bundle> getBundles()
    {
        return bundles;
    }

    /**
     * Add a bundle to the item, should not be made public since we don't want to skip business logic
     * @param bundle the bundle to be added
     */
    void addBundle(Bundle bundle)
    {
        bundles.add(bundle);
    }

    /**
     * Remove a bundle from item, should not be made public since we don't want to skip business logic
     * @param bundle the bundle to be removed
     */
    void removeBundle(Bundle bundle)
    {
        bundles.remove(bundle);
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Item as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     * @return <code>true</code> if object passed in represents the same item
     *         as this object
     */
     @Override
     public boolean equals(Object obj)
     {
         if (obj == null)
         {
             return false;
         }
         Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
         if (this.getClass() != objClass)
         {
             return false;
         }
         final Item otherItem = (Item) obj;
         if (!this.getID().equals(otherItem.getID()))
         {
             return false;
         }

         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 5;
         hash += 71 * hash + getType();
         hash += 71 * hash + getID().hashCode();
         return hash;
     }

    /**
     * return type found in Constants
     *
     * @return int Constants.ITEM
     */
    @Override
    public int getType()
    {
        return Constants.ITEM;
    }

    @Override
    public String getName()
    {
        return getItemService().getMetadataFirstValue(this, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    public ItemService getItemService()
    {
        if(itemService == null)
        {
            itemService = ContentServiceFactory.getInstance().getItemService();
        }
<<<<<<< HEAD
    }

    /**
     * Remove all licenses from an item - it was rejected
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void removeLicenses() throws SQLException, AuthorizeException,
            IOException
    {
        // Find the License format
        BitstreamFormat bf = BitstreamFormat.findByShortDescription(ourContext,
                "License");
        int licensetype = bf.getID();

        // search through bundles, looking for bitstream type license
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            boolean removethisbundle = false;

            Bitstream[] bits = bunds[i].getBitstreams();

            for (int j = 0; j < bits.length; j++)
            {
                BitstreamFormat bft = bits[j].getFormat();

                if (bft.getID() == licensetype)
                {
                    removethisbundle = true;
                }
            }

            // probably serious troubles with Authorizations
            // fix by telling system not to check authorization?
            if (removethisbundle)
            {
                removeBundle(bunds[i]);
            }
        }
    }

    /**
     * Update the item "in archive" flag and Dublin Core metadata in the
     * database
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation
        // only do write authorization if user is not an editor
        if (!canEdit())
        {
            AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
        }

        log.info(LogManager.getHeader(ourContext, "update_item", "item_id="
                + getID()));

        // Set sequence IDs for bitstreams in item
        int sequence = 0;
        Bundle[] bunds = getBundles();

        // find the highest current sequence number
        for (int i = 0; i < bunds.length; i++)
        {
            Bitstream[] streams = bunds[i].getBitstreams();

            for (int k = 0; k < streams.length; k++)
            {
                if (streams[k].getSequenceID() > sequence)
                {
                    sequence = streams[k].getSequenceID();
                }
            }
        }

        // start sequencing bitstreams without sequence IDs
        sequence++;

        for (int i = 0; i < bunds.length; i++)
        {
            Bitstream[] streams = bunds[i].getBitstreams();

            for (int k = 0; k < streams.length; k++)
            {
                if (streams[k].getSequenceID() < 0)
                {
                    streams[k].setSequenceID(sequence);
                    sequence++;
                    streams[k].update();
                    modified = true;
                }
            }
        }

        if (modifiedMetadata || modified)
        {
            // Set the last modified date
            itemRow.setColumn("last_modified", new Date());

            // Make sure that withdrawn and in_archive are non-null
            if (itemRow.isColumnNull("in_archive"))
            {
                itemRow.setColumn("in_archive", false);
            }

            if (itemRow.isColumnNull("withdrawn"))
            {
                itemRow.setColumn("withdrawn", false);
            }

            if (itemRow.isColumnNull("discoverable"))
            {
                itemRow.setColumn("discoverable", false);
            }


            DatabaseManager.update(ourContext, itemRow);

            if (modifiedMetadata) {
                updateMetadata();
                clearDetails();
            }

            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                    null, getIdentifiers(ourContext)));
            modified = false;
        }
    }


    /**
     * Withdraw the item from the archive. It is kept in place, and the content
     * and metadata are not deleted, but it is not publicly accessible.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void withdraw() throws SQLException, AuthorizeException, IOException
    {
        // Check permission. User either has to have REMOVE on owning collection
        // or be COLLECTION_EDITOR of owning collection
        AuthorizeUtil.authorizeWithdrawItem(ourContext, this);

        String timestamp = DCDate.getCurrent().toString();

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();

        // Build some provenance data while we're at it.
        StringBuilder prov = new StringBuilder();

        prov.append("Item withdrawn by ").append(e.getFullName()).append(" (")
                .append(e.getEmail()).append(") on ").append(timestamp).append("\n")
                .append("Item was in collections:\n");

        Collection[] colls = getCollections();

        for (int i = 0; i < colls.length; i++)
        {
            prov.append(colls[i].getMetadata("name")).append(" (ID: ").append(colls[i].getID()).append(")\n");
        }

        // Set withdrawn flag. timestamp will be set; last_modified in update()
        itemRow.setColumn("withdrawn", true);

        // in_archive flag is now false
        itemRow.setColumn("in_archive", false);

        prov.append(InstallItem.getBitstreamProvenanceMessage(this));

        addDC("description", "provenance", "en", prov.toString());

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                "WITHDRAW", getIdentifiers(ourContext)));

        // switch all READ authorization policies to WITHDRAWN_READ
		AuthorizeManager.switchPoliciesAction(ourContext, this, Constants.READ, Constants.WITHDRAWN_READ);
		for (Bundle bnd : this.getBundles()) {
			AuthorizeManager.switchPoliciesAction(ourContext, bnd, Constants.READ, Constants.WITHDRAWN_READ);
			for (Bitstream bs : bnd.getBitstreams()) {
				AuthorizeManager.switchPoliciesAction(ourContext, bs, Constants.READ, Constants.WITHDRAWN_READ);
			}
		}

        // Write log
        log.info(LogManager.getHeader(ourContext, "withdraw_item", "user="
                + e.getEmail() + ",item_id=" + getID()));
    }


    /**
     * Reinstate a withdrawn item
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void reinstate() throws SQLException, AuthorizeException,
            IOException
    {
        // check authorization
        AuthorizeUtil.authorizeReinstateItem(ourContext, this);

        String timestamp = DCDate.getCurrent().toString();

        // Check permission. User must have ADD on all collections.
        // Build some provenance data while we're at it.
        Collection[] colls = getCollections();

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        EPerson e = ourContext.getCurrentUser();
        StringBuilder prov = new StringBuilder();
        prov.append("Item reinstated by ").append(e.getFullName()).append(" (")
                .append(e.getEmail()).append(") on ").append(timestamp).append("\n")
                .append("Item was in collections:\n");

        for (int i = 0; i < colls.length; i++)
        {
            prov.append(colls[i].getMetadata("name")).append(" (ID: ").append(colls[i].getID()).append(")\n");
        }
        
        // Clear withdrawn flag
        itemRow.setColumn("withdrawn", false);

        // in_archive flag is now true
        itemRow.setColumn("in_archive", true);

        // Add suitable provenance - includes user, date, collections +
        // bitstream checksums
        prov.append(InstallItem.getBitstreamProvenanceMessage(this));

        addDC("description", "provenance", "en", prov.toString());

        // Update item in DB
        update();

        ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                "REINSTATE", getIdentifiers(ourContext)));

        // restore all WITHDRAWN_READ authorization policies back to READ
        for (Bundle bnd : this.getBundles()) {
			AuthorizeManager.switchPoliciesAction(ourContext, bnd, Constants.WITHDRAWN_READ, Constants.READ);
			for (Bitstream bs : bnd.getBitstreams()) {
				AuthorizeManager.switchPoliciesAction(ourContext, bs, Constants.WITHDRAWN_READ, Constants.READ);
			}
		}
        
        // check if the item was withdrawn before the fix DS-3097
        if (AuthorizeManager.getPoliciesActionFilter(ourContext, this, Constants.WITHDRAWN_READ).size() != 0) {
        	AuthorizeManager.switchPoliciesAction(ourContext, this, Constants.WITHDRAWN_READ, Constants.READ);
        }
        else {
	        // authorization policies
	        if (colls.length > 0)
	        {
	            // remove the item's policies and replace them with
	            // the defaults from the collection
	        	adjustItemPolicies(getOwningCollection());
	        }
        }
        
        // Write log
        log.info(LogManager.getHeader(ourContext, "reinstate_item", "user="
                + e.getEmail() + ",item_id=" + getID()));
    }

    /**
     * Delete (expunge) the item. Bundles and bitstreams are also deleted if
     * they are not also included in another item. The Dublin Core metadata is
     * deleted.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    void delete() throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation here. If we don't, it may happen that we remove the
        // metadata but when getting to the point of removing the bundles we get an exception
        // leaving the database in an inconsistent state
        AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);

        ourContext.addEvent(new Event(Event.DELETE, Constants.ITEM, getID(), 
                getHandle(), getIdentifiers(ourContext)));

        log.info(LogManager.getHeader(ourContext, "delete_item", "item_id="
                + getID()));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove from browse indices, if appropriate
        /** XXX FIXME
         ** Although all other Browse index updates are managed through
         ** Event consumers, removing an Item *must* be done *here* (inline)
         ** because otherwise, tables are left in an inconsistent state
         ** and the DB transaction will fail.
         ** Any fix would involve too much work on Browse code that
         ** is likely to be replaced soon anyway.   --lcs, Aug 2006
         **
         ** NB Do not check to see if the item is archived - withdrawn /
         ** non-archived items may still be tracked in some browse tables
         ** for administrative purposes, and these need to be removed.
         **/
//               FIXME: there is an exception handling problem here
        try
        {
//               Remove from indices
            IndexBrowse ib = new IndexBrowse(ourContext);
            ib.itemRemoved(this);
        }
        catch (BrowseException e)
        {
            log.error("caught exception: ", e);
            throw new SQLException(e.getMessage(), e);
        }

        // Delete the Dublin Core
        removeMetadataFromDatabase();

        // Remove bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            removeBundle(bunds[i]);
        }

        // remove all of our authorization policies
        AuthorizeManager.removeAllPolicies(ourContext, this);
        
        // Remove any Handle
        HandleManager.unbindHandle(ourContext, this);
        
        // remove version attached to the item
        removeVersion();


        // Finally remove item row
        DatabaseManager.delete(ourContext, itemRow);
    }
    
    private void removeVersion() throws AuthorizeException, SQLException
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(versioningService.getVersion(ourContext, this)!=null)
        {
            versioningService.removeVersion(ourContext, this);
        }else{
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            try {
                identifierService.delete(ourContext, this);
            } catch (IdentifierException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Remove item and all its sub-structure from the context cache.
     * Useful in batch processes where a single context has a long,
     * multi-item lifespan
     */
    public void decache() throws SQLException
    {
        // Remove item and it's submitter from cache
        ourContext.removeCached(this, getID());
        if (submitter != null)
        {
                ourContext.removeCached(submitter, submitter.getID());
        }
        // Remove bundles & bitstreams from cache if they have been loaded
        if (bundles != null)
        {
                Bundle[] bunds = getBundles();
                for (int i = 0; i < bunds.length; i++)
                {
                        ourContext.removeCached(bunds[i], bunds[i].getID());
                        Bitstream[] bitstreams = bunds[i].getBitstreams();
                        for (int j = 0; j < bitstreams.length; j++)
                        {
                                ourContext.removeCached(bitstreams[j], bitstreams[j].getID());
                        }
                }
        }
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Item as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     * @return <code>true</code> if object passed in represents the same item
     *         as this object
     */
     @Override
     public boolean equals(Object obj)
     {
         if (obj == null)
         {
             return false;
         }
         if (getClass() != obj.getClass())
         {
             return false;
         }
         final Item other = (Item) obj;
         if (this.getType() != other.getType())
         {
             return false;
         }
         if (this.getID() != other.getID())
         {
             return false;
         }

         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 5;
         hash = 71 * hash + (this.itemRow != null ? this.itemRow.hashCode() : 0);
         return hash;
     }




    /**
     * Return true if this Collection 'owns' this item
     *
     * @param c
     *            Collection
     * @return true if this Collection owns this item
     */
    public boolean isOwningCollection(Collection c)
    {
        int owner_id = itemRow.getIntColumn("owning_collection");

        if (c.getID() == owner_id)
        {
            return true;
        }

        // not the owner
        return false;
    }

    /**
     * return type found in Constants
     *
     * @return int Constants.ITEM
     */
    public int getType()
    {
        return Constants.ITEM;
    }

    /**
     * remove all of the policies for item and replace them with a new list of
     * policies
     *
     * @param newpolicies -
     *            this will be all of the new policies for the item and its
     *            contents
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void replaceAllItemPolicies(List<ResourcePolicy> newpolicies) throws SQLException,
            AuthorizeException
    {
        // remove all our policies, add new ones
        AuthorizeManager.removeAllPolicies(ourContext, this);
        AuthorizeManager.addPolicies(ourContext, newpolicies, this);
    }

    /**
     * remove all of the policies for item's bitstreams and bundles and replace
     * them with a new list of policies
     *
     * @param newpolicies -
     *            this will be all of the new policies for the bundle and
     *            bitstream contents
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void replaceAllBitstreamPolicies(List<ResourcePolicy> newpolicies)
            throws SQLException, AuthorizeException
    {
        // remove all policies from bundles, add new ones
        // Remove bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];
            mybundle.replaceAllBitstreamPolicies(newpolicies);
        }
    }

    /**
     * remove all of the policies for item's bitstreams and bundles that belong
     * to a given Group
     *
     * @param g
     *            Group referenced by policies that needs to be removed
     * @throws SQLException
     */
    public void removeGroupPolicies(Group g) throws SQLException
    {
        // remove Group's policies from Item
        AuthorizeManager.removeGroupPolicies(ourContext, this, g);

        // remove all policies from bundles
        Bundle[] bunds = getBundles();

        for (int i = 0; i < bunds.length; i++)
        {
            Bundle mybundle = bunds[i];

            Bitstream[] bs = mybundle.getBitstreams();

            for (int j = 0; j < bs.length; j++)
            {
                // remove bitstream policies
                AuthorizeManager.removeGroupPolicies(ourContext, bs[j], g);
            }

            // change bundle policies
            AuthorizeManager.removeGroupPolicies(ourContext, mybundle, g);
        }
    }

    /**
     * remove all policies on an item and its contents, and replace them with
     * the DEFAULT_ITEM_READ and DEFAULT_BITSTREAM_READ policies belonging to
     * the collection.
     *
     * @param c
     *            Collection
     * @throws java.sql.SQLException
     *             if an SQL error or if no default policies found. It's a bit
     *             draconian, but default policies must be enforced.
     * @throws AuthorizeException
     */
    public void inheritCollectionDefaultPolicies(Collection c)
            throws java.sql.SQLException, AuthorizeException
    {
        adjustItemPolicies(c);
        adjustBundleBitstreamPolicies(c);

        log.debug(LogManager.getHeader(ourContext, "item_inheritCollectionDefaultPolicies",
                                                   "item_id=" + getID()));
    }

    public void adjustBundleBitstreamPolicies(Collection c) throws SQLException, AuthorizeException {

        List<ResourcePolicy> defaultCollectionPolicies = AuthorizeManager.getPoliciesActionFilter(ourContext, c, Constants.DEFAULT_BITSTREAM_READ);

        if (defaultCollectionPolicies.size() < 1){
            throw new SQLException("Collection " + c.getID()
                    + " (" + c.getHandle() + ")"
                    + " has no default bitstream READ policies");
        }

        // remove all policies from bundles, add new ones
        // Remove bundles
        Bundle[] bunds = getBundles();
        for (int i = 0; i < bunds.length; i++){
            Bundle mybundle = bunds[i];

            // if come from InstallItem: remove all submission/workflow policies
            AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, mybundle, ResourcePolicy.TYPE_SUBMISSION);
            AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, mybundle, ResourcePolicy.TYPE_WORKFLOW);

            List<ResourcePolicy> policiesBundleToAdd = filterPoliciesToAdd(defaultCollectionPolicies, mybundle);
            AuthorizeManager.addPolicies(ourContext, policiesBundleToAdd, mybundle);

            for(Bitstream bitstream : mybundle.getBitstreams()){
                // if come from InstallItem: remove all submission/workflow policies
                AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, bitstream, ResourcePolicy.TYPE_SUBMISSION);
                AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, bitstream, ResourcePolicy.TYPE_WORKFLOW);

                List<ResourcePolicy> policiesBitstreamToAdd = filterPoliciesToAdd(defaultCollectionPolicies, bitstream);
                AuthorizeManager.addPolicies(ourContext, policiesBitstreamToAdd, bitstream);
            }
        }
    }

    public void adjustItemPolicies(Collection c) throws SQLException, AuthorizeException {
        // read collection's default READ policies
        List<ResourcePolicy> defaultCollectionPolicies = AuthorizeManager.getPoliciesActionFilter(ourContext, c, Constants.DEFAULT_ITEM_READ);

        // MUST have default policies
        if (defaultCollectionPolicies.size() < 1)
        {
            throw new SQLException("Collection " + c.getID()
                    + " (" + c.getHandle() + ")"
                    + " has no default item READ policies");
        }

        // if come from InstallItem: remove all submission/workflow policies
        AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, this, ResourcePolicy.TYPE_SUBMISSION);
        AuthorizeManager.removeAllPoliciesByDSOAndType(ourContext, this, ResourcePolicy.TYPE_WORKFLOW);

        // add default policies only if not already in place
        List<ResourcePolicy> policiesToAdd = filterPoliciesToAdd(defaultCollectionPolicies, this);
        AuthorizeManager.addPolicies(ourContext, policiesToAdd, this);
    }

    private List<ResourcePolicy> filterPoliciesToAdd(List<ResourcePolicy> defaultCollectionPolicies, DSpaceObject dso) throws SQLException {
        List<ResourcePolicy> policiesToAdd = new ArrayList<ResourcePolicy>();
        for (ResourcePolicy rp : defaultCollectionPolicies){
            rp.setAction(Constants.READ);
            // if an identical policy is already in place don't add it
            if(!AuthorizeManager.isAnIdenticalPolicyAlreadyInPlace(ourContext, dso, rp)){
                rp.setRpType(ResourcePolicy.TYPE_INHERITED);
                policiesToAdd.add(rp);
            }
        }
        return policiesToAdd;
    }

    /**
     * Moves the item from one collection to another one
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void move (Collection from, Collection to) throws SQLException, AuthorizeException, IOException
    {
        // Use the normal move method, and default to not inherit permissions
        this.move(from, to, false);
    }

    /**
     * Moves the item from one collection to another one
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void move (Collection from, Collection to, boolean inheritDefaultPolicies) throws SQLException, AuthorizeException, IOException
    {
        // Check authorisation on the item before that the move occur
        // otherwise we will need edit permission on the "target collection" to archive our goal
        // only do write authorization if user is not an editor
        if (!canEdit())
        {
            AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
        }
        
        // Move the Item from one Collection to the other
        to.addItem(this);
        from.removeItem(this);

        // If we are moving from the owning collection, update that too
        if (isOwningCollection(from))
        {
            // Update the owning collection
            log.info(LogManager.getHeader(ourContext, "move_item",
                                          "item_id=" + getID() + ", from " +
                                          "collection_id=" + from.getID() + " to " +
                                          "collection_id=" + to.getID()));
            setOwningCollection(to);

            // If applicable, update the item policies
            if (inheritDefaultPolicies)
            {
                log.info(LogManager.getHeader(ourContext, "move_item",
                         "Updating item with inherited policies"));
                inheritCollectionDefaultPolicies(to);
            }

            // Update the item
            ourContext.turnOffAuthorisationSystem();
            update();
            ourContext.restoreAuthSystemState();
        }
        else
        {
            // Although we haven't actually updated anything within the item
            // we'll tell the event system that it has, so that any consumers that
            // care about the structure of the repository can take account of the move

            // Note that updating the owning collection above will have the same effect,
            // so we only do this here if the owning collection hasn't changed.
            
            ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), 
                    null, getIdentifiers(ourContext)));
        }
    }
    
    /**
     * Check the bundle ORIGINAL to see if there are any uploaded files
     *
     * @return true if there is a bundle named ORIGINAL with one or more
     *         bitstreams inside
     * @throws SQLException
     */
    public boolean hasUploadedFiles() throws SQLException
    {
        Bundle[] bundles = getBundles("ORIGINAL");
        if (bundles.length == 0)
        {
            // if no ORIGINAL bundle,
            // return false that there is no file!
            return false;
        }
        else
        {
            Bitstream[] bitstreams = bundles[0].getBitstreams();
            if (bitstreams.length == 0)
            {
                // no files in ORIGINAL bundle!
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the collections this item is not in.
     *
     * @return the collections this item is not in, if any.
     * @throws SQLException
     */
    public Collection[] getCollectionsNotLinked() throws SQLException
    {
        Collection[] allCollections = Collection.findAll(ourContext);
        Collection[] linkedCollections = getCollections();
        Collection[] notLinkedCollections = new Collection[allCollections.length - linkedCollections.length];

        if ((allCollections.length - linkedCollections.length) == 0)
        {
                return notLinkedCollections;
        }
        
        int i = 0;
                 
        for (Collection collection : allCollections)
        {
                 boolean alreadyLinked = false;
                         
                 for (Collection linkedCommunity : linkedCollections)
                 {
                         if (collection.getID() == linkedCommunity.getID())
                         {
                                 alreadyLinked = true;
                                 break;
                         }
                 }
                         
                 if (!alreadyLinked)
                 {
                         notLinkedCollections[i++] = collection;
                 }
        }
        
        return notLinkedCollections;
    }

    /**
     * return TRUE if context's user can edit item, false otherwise
     *
     * @return boolean true = current user can edit item
     * @throws SQLException
     */
    public boolean canEdit() throws java.sql.SQLException
    {
        // can this person write to the item?
        if (AuthorizeManager.authorizeActionBoolean(ourContext, this,
                Constants.WRITE))
        {
            return true;
        }

        // is this collection not yet created, and an item template is created
        if (getOwningCollection() == null)
        {
        	if (!isInProgressSubmission()) {
        		return true;
        	}
        	else {
        		return false;
        	}
        }

        // is this person an COLLECTION_EDITOR for the owning collection?
        if (getOwningCollection().canEditBoolean(false))
        {
            return true;
        }

        return false;
    }
    
    /**
     * Check if the item is an inprogress submission
     * @param context
     * @param item
     * @return <code>true</code> if the item is an inprogress submission, i.e. a WorkspaceItem or WorkflowItem
     * @throws SQLException
     */
    public boolean isInProgressSubmission() throws SQLException {
		return WorkspaceItem.findByItem(ourContext, this) != null ||
				((ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow")
						&& XmlWorkflowItem.findByItem(ourContext, this) != null)
						|| WorkflowItem.findByItem(ourContext, this) != null);
    }
    
    public String getName()
    {
        return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
    }

    /**
     * Returns an iterator of Items possessing the passed metadata field, or only
     * those matching the passed value, if value is not Item.ANY
     *
     * @param context DSpace context object
     * @param schema metadata field schema
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     * @param value field value or Item.ANY to match any value
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     *
     */
    public static ItemIterator findByMetadataField(Context context,
               String schema, String element, String qualifier, String value)
          throws SQLException, AuthorizeException, IOException
    {
        MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException(
                    "No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }
        
        String query = "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' "+
                       "AND item.item_id = metadatavalue.resource_id AND metadata_field_id = ? AND resource_type_id = ?";
        TableRowIterator rows = null;
        if (Item.ANY.equals(value))
        {
                rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID(), Constants.ITEM);
        }
        else
        {
                query += " AND metadatavalue.text_value = ?";
                rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID(),  Constants.ITEM, value);
        }
        return new ItemIterator(context, rows);
     }
    
    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        DSpaceObject adminObject = null;
        Collection collection = getOwningCollection();
        Community community = null;
        if (collection != null)
        {
            Community[] communities = collection.getCommunities();
            if (communities != null && communities.length > 0)
            {
                community = communities[0];
            }
        }
        else
        {
            // is a template item?
            TableRow qResult = DatabaseManager.querySingle(ourContext,
                       "SELECT collection_id FROM collection " +
                       "WHERE template_item_id = ?",getID());
            if (qResult != null)
            {
                collection = Collection.find(ourContext, qResult.getIntColumn("collection_id"));
                Community[] communities = collection.getCommunities();
                if (communities != null && communities.length > 0)
                {
                    community = communities[0];
                }
            }
        }
        
        switch (action)
        {
            case Constants.ADD:
                // ADD a cc license is less general than add a bitstream but we can't/won't
                // add complex logic here to know if the ADD action on the item is required by a cc or
                // a generic bitstream so simply we ignore it.. UI need to enforce the requirements.
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamCreation())
                {
                    adminObject = this;
                }
                else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamCreation())
                {
                    adminObject = collection;
                }
                else if (AuthorizeConfiguration.canCommunityAdminPerformBitstreamCreation())
                {
                    adminObject = community;
                }
                break;
            case Constants.REMOVE:
                // see comments on ADD action, same things...
                if (AuthorizeConfiguration.canItemAdminPerformBitstreamDeletion())
                {
                    adminObject = this;
                }
                else if (AuthorizeConfiguration.canCollectionAdminPerformBitstreamDeletion())
                {
                    adminObject = collection;
                }
                else if (AuthorizeConfiguration.canCommunityAdminPerformBitstreamDeletion())
                {
                    adminObject = community;
                }
                break;
            case Constants.DELETE:
                if (getOwningCollection() != null)
                {
                    if (AuthorizeConfiguration.canCollectionAdminPerformItemDeletion())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminPerformItemDeletion())
                    {
                        adminObject = community;
                    }
                }
                else
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                break;
            case Constants.WRITE:
                // if it is a template item we need to check the
                // collection/community admin configuration
                if (getOwningCollection() == null)
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                else
                {
                    adminObject = this;
                }
                break;
            default:
                adminObject = this;
                break;
            }
        return adminObject;
    }
    
    public DSpaceObject getParentObject() throws SQLException
    {
        Collection ownCollection = getOwningCollection();
        if (ownCollection != null)
        {
            return ownCollection;
        }
        else
        {
            // is a template item?
            TableRow qResult = DatabaseManager.querySingle(ourContext,
                       "SELECT collection_id FROM collection " +
                       "WHERE template_item_id = ?",getID());
            if (qResult != null)
            {
                return Collection.find(ourContext,qResult.getIntColumn("collection_id"));
            }
            return null;
        }
    }

    /**
     * Find all the items in the archive with a given authority key value
     * in the indicated metadata field.
     *
     * @param context DSpace context object
     * @param schema metadata field schema
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     * @param value the value of authority key to look for
     * @return an iterator over the items matching that authority value
     * @throws SQLException, AuthorizeException, IOException
     */
    public static ItemIterator findByAuthorityValue(Context context,
            String schema, String element, String qualifier, String value)
        throws SQLException, AuthorizeException, IOException
    {
        MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException("No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }

        TableRowIterator rows = DatabaseManager.queryTable(context, "item",
            "SELECT item.* FROM metadatavalue,item WHERE item.in_archive='1' "+
            "AND item.item_id = metadatavalue.resource_id AND metadata_field_id = ? AND authority = ? AND resource_type_id = ?",
            mdf.getFieldID(), value, Constants.ITEM);
        return new ItemIterator(context, rows);
    }

    @Override
    protected void getAuthoritiesAndConfidences(String fieldKey, String[] values, String[] authorities, int[] confidences, int i) {
        Choices c = ChoiceAuthorityManager.getManager().getBestMatch(fieldKey, values[i], getOwningCollectionID(), null);
        authorities[i] = c.values.length > 0 ? c.values[0].authority : null;
        confidences[i] = c.confidence;
=======
        return itemService;
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    }
}
