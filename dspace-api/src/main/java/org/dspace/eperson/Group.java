/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a group of e-people.
 *
 * @author David Stuve
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "epersongroup" )
public class Group extends DSpaceObject implements DSpaceObjectLegacySupport
{

    @Transient
    public static final String ANONYMOUS = "Anonymous";

    @Transient
    public static final String ADMIN = "Administrator";

    /**
     * Initial value is set to 2 since 0 and 1 are reserved for anonymous and administrative uses, respectively
     */
    @Column(name="eperson_group_id", insertable = false, updatable = false)
    private Integer legacyId;

    /** This Group may not be deleted or renamed. */
    @Column
    private Boolean permanent = false;

    @Column(length = 250, unique = true)
    private String name;

    /** lists of epeople and groups in the group */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "epersongroup2eperson",
            joinColumns = {@JoinColumn(name = "eperson_group_id") },
            inverseJoinColumns = {@JoinColumn(name = "eperson_id") }
    )
    private final List<EPerson> epeople = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group2group",
            joinColumns = {@JoinColumn(name = "parent_id") },
            inverseJoinColumns = {@JoinColumn(name = "child_id") }
    )
    private final List<Group> groups = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "groups")
    private final List<Group> parentGroups = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "supervisorGroups")
    private final List<WorkspaceItem> supervisedItems = new ArrayList<>();

    @Transient
    private boolean groupsChanged;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.GroupService#create(Context)}
     *
     */
    protected Group()
    {

    }

    void addMember(EPerson e)
    {
        getMembers().add(e);
    }

    /**
     * Return EPerson members of a Group
     *
     * @return list of EPersons
     */
    public List<EPerson> getMembers()
    {
        return epeople;
    }

    void addMember(Group g)
    {
        getMemberGroups().add(g);
        groupsChanged = true;
    }

<<<<<<< HEAD
    /**
     * Check to see if g is a direct group member.
     * If g is a subgroup via another group will be returned <code>false</code>
     * 
     * @param g
     *            group to check
     */
    public boolean isMember(Group g)
    {
        loadData(); // make sure Group has data loaded

        return groups.contains(g);
    }

    /**
     * fast check to see if the current EPerson is a member of a Group.  Does
     * database lookup without instantiating all of the EPerson objects and is
     * thus a static method.
     * 
     * @param c
     *            context
     * @param groupid
     *            group ID to check
     */
    public static boolean isMember(Context c, int groupid) throws SQLException
    {
        // special, everyone is member of group 0 (anonymous)
        if (groupid == 0)
        {
            return true;
        }

        EPerson currentuser = c.getCurrentUser();

        return epersonInGroup(c, groupid, currentuser);
    }

    /**
     * Fast check to see if a given EPerson is a member of a Group.
     * Does database lookup without instantiating all of the EPerson objects and
     * is thus a static method.
     *
     * @param c current DSpace context.
     * @param eperson candidate to test for membership.
     * @param groupid group whose membership is to be tested.
     * @return true if {@link eperson} is a member of Group {@link groupid}.
     * @throws SQLException passed through
     */
    public static boolean isMember(Context c, EPerson eperson, int groupid)
            throws SQLException
    {
        // Every EPerson is a member of Anonymous
        if (groupid == 0)
        {
            return true;
        }

        return epersonInGroup(c, groupid, eperson);
    }

    /**
     * Get all of the groups that an eperson is a member of.
     * 
     * @param c
     * @param e
     * @throws SQLException
     */
    public static Group[] allMemberGroups(Context c, EPerson e)
            throws SQLException
    {
        List<Group> groupList = new ArrayList<Group>();

        Set<Integer> myGroups = allMemberGroupIDs(c, e);
        // now convert those Integers to Groups
        Iterator<Integer> i = myGroups.iterator();

        while (i.hasNext())
        {
            groupList.add(Group.find(c, (i.next()).intValue()));
        }

        return groupList.toArray(new Group[groupList.size()]);
    }

    /**
     * get Set of Integers all of the group memberships for an eperson
     * 
     * @param c
     * @param e
     * @return Set of Integer groupIDs
     * @throws SQLException
     */
    public static Set<Integer> allMemberGroupIDs(Context c, EPerson e)
            throws SQLException
    {
        Set<Integer> groupIDs = new HashSet<Integer>();
        
        if (e != null)
        {
            // two queries - first to get groups eperson is a member of
            // second query gets parent groups for groups eperson is a member of

            TableRowIterator tri = DatabaseManager.queryTable(c,
                    "epersongroup2eperson",
                    "SELECT * FROM epersongroup2eperson WHERE eperson_id= ?", e
                            .getID());

            try
            {
                while (tri.hasNext())
                {
                    TableRow row = tri.next();

                    int childID = row.getIntColumn("eperson_group_id");

                    groupIDs.add(Integer.valueOf(childID));
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
        }
        // Also need to get all "Special Groups" user is a member of!
        // Otherwise, you're ignoring the user's membership to these groups!
        // However, we only do this is we are looking up the special groups
        // of the current user, as we cannot look up the special groups
        // of a user who is not logged in.
        if ((c.getCurrentUser() == null) || (((c.getCurrentUser() != null) && (c.getCurrentUser().getID() == e.getID()))))
        {
            Group[] specialGroups = c.getSpecialGroups();
            for(Group special : specialGroups)
            {
                groupIDs.add(Integer.valueOf(special.getID()));
            }
        }

        // all the users are members of the anonymous group 
        groupIDs.add(Integer.valueOf(0));
        
        // now we have all owning groups, also grab all parents of owning groups
        // yes, I know this could have been done as one big query and a union,
        // but doing the Oracle port taught me to keep to simple SQL!

        StringBuilder groupQuery = new StringBuilder();
        groupQuery.append("SELECT * FROM group2groupcache WHERE ");

        Iterator<Integer> i = groupIDs.iterator();

        // Build a list of query parameters
        Object[] parameters = new Object[groupIDs.size()];
        int idx = 0;
        while (i.hasNext())
        {
            int groupID = (i.next()).intValue();

            parameters[idx++] = Integer.valueOf(groupID);
            
            groupQuery.append("child_id= ? ");
            if (i.hasNext())
            {
                groupQuery.append(" OR ");
            }
        }

        // was member of at least one group
        // NOTE: even through the query is built dynamically, all data is
        // separated into the parameters array.
        TableRowIterator tri = DatabaseManager.queryTable(c, "group2groupcache",
                groupQuery.toString(),
                parameters);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                int parentID = row.getIntColumn("parent_id");

                groupIDs.add(Integer.valueOf(parentID));
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

        return groupIDs;
    }
    
    
    /**
     * Get all of the epeople who are a member of the
     * specified group, or a member of a sub-group of the
     * specified group, etc.
     * 
     * @param c   
     *          DSpace context
     * @param g   
     *          Group object
     * @return   Array of EPerson objects
     * @throws SQLException
     */
    public static EPerson[] allMembers(Context c, Group g)
            throws SQLException
=======
    void addParentGroup(Group group)
>>>>>>> aaafc1887bc2e36d28f8d9c37ba8cac67a059689
    {
        getParentGroups().add(group);
        groupsChanged = true;
    }

    void removeParentGroup(Group group)
    {
        getParentGroups().remove(group);
        groupsChanged = true;
    }

    boolean remove(EPerson e)
    {
        return getMembers().remove(e);
    }

    boolean remove(Group g)
    {
        groupsChanged = true;
        return getMemberGroups().remove(g);
    }

    boolean contains(Group g)
    {
        return getMemberGroups().contains(g);
    }

    boolean contains(EPerson e)
    {
        return getMembers().contains(e);
    }

    List<Group> getParentGroups() {
        return parentGroups;
    }

    /**
     * Return Group members of a Group.
     *
     * @return list of groups
     */
    public List<Group> getMemberGroups()
    {
        return groups;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Group as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same group
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
         if (getClass() != objClass)
         {
             return false;
         }
         final Group other = (Group) obj;
         return this.getID().equals(other.getID());
     }

     @Override
     public int hashCode()
     {
         int hash = 7;
         hash = 59 * hash + this.getID().hashCode();
         hash = 59 * hash + (this.getName() != null? this.getName().hashCode():0);
         return hash;
     }



    @Override
    public int getType()
    {
        return Constants.GROUP;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /** Change the name of this Group. */
    void setName(String name) throws SQLException
    {
        if(!StringUtils.equals(this.name, name) && !isPermanent()) {
            this.name = name;
            groupsChanged = true;
        }
    }

    public boolean isGroupsChanged() {
        return groupsChanged;
    }

    public void clearGroupsChanged() {
        this.groupsChanged = false;
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    public List<WorkspaceItem> getSupervisedItems() {
        return supervisedItems;
    }

    /**
     * May this Group be renamed or deleted?  (The content of any group may be
     * changed.)
     *
     * @return true if this Group may not be renamed or deleted.
     */
    public Boolean isPermanent()
    {
        return permanent;
    }

    /**
     * May this Group be renamed or deleted?  (The content of any group may be
     * changed.)
     *
     * @param permanence true if this group may not be renamed or deleted.
     */
    void setPermanent(boolean permanence)
    {
        permanent = permanence;
        setModified();
    }
}
