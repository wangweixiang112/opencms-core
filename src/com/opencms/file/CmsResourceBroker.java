package com.opencms.file;

import javax.servlet.http.*;
import java.util.*;

import com.opencms.core.*;

/**
 * This is THE resource broker. It merges all resource broker
 * into one public class. The interface is local to package. <B>All</B> methods
 * get additional parameters (callingUser and currentproject) to check the security-
 * police.
 * 
 * @author Andreas Schouten
 * @version $Revision: 1.16 $ $Date: 2000/01/11 11:46:05 $
 */
class CmsResourceBroker implements I_CmsResourceBroker, I_CmsConstants {
	
	/**
	 * The resource broker for user
	 */
	private I_CmsRbUserGroup m_userRb;

	/**
	 * The resource broker for file
	 */
	private I_CmsRbFile m_fileRb;

	/**
	 * The resource broker for metadef
	 */
	private I_CmsRbMetadefinition m_metadefRb;

	/**
	 * The resource broker for property
	 */
	private I_CmsRbProperty m_propertyRb;

	/**
	 * The resource broker for project
	 */
	private I_CmsRbProject m_projectRb;

	/**
	 * The resource broker for task
	 */
	private I_CmsRbTask m_taskRb;
	
	/**
	 * A Hashtable with all resource-types.
	 */
	private Hashtable m_resourceTypes = null;
	
	/**
	 * The constructor for this ResourceBroker. It gets all underlaying 
	 * resource-brokers.
	 */
	public CmsResourceBroker(I_CmsRbUserGroup userRb, I_CmsRbFile fileRb , 
							 I_CmsRbMetadefinition metadefRb, I_CmsRbProperty propertyRb,
							 I_CmsRbProject projectRb /*,  I_CmsRbTask taskRb */) {
		m_userRb = userRb;
		m_fileRb = fileRb;
		m_metadefRb = metadefRb;
		m_propertyRb = propertyRb;
		m_projectRb = projectRb;
		// m_taskRb = taskRb; // TODO: add the taskRb here - if available
    
	}

	// Projects:
	
	/**
	 * Returns the onlineproject. This is the default project. All anonymous 
	 * (A_CmsUser callingUser, or guest) user will see the rersources of this project.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @return the onlineproject object.
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public A_CmsProject onlineProject(A_CmsUser currentUser, 
									  A_CmsProject currentProject)
		throws CmsException {
		return( readProject(currentUser, currentProject, C_PROJECT_ONLINE) );
	}

	/**
	 * Tests if the user can access the project.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param projectname the name of the project.
	 * 
	 * @return true, if the user has access, else returns false.
	 */
	public boolean accessProject(A_CmsUser currentUser, A_CmsProject currentProject,
								 String projectname) 
		throws CmsException {
		
		A_CmsProject testProject = readProject(currentUser, currentProject, projectname);
		
		// is the current-user admin, or the owner of the project?
		if( (currentProject.getOwnerId() == currentUser.getId()) || 
			isAdmin(currentUser, currentProject) ) {
			return(true);
		}
		
		// get all groups of the user
		Vector groups = getGroupsOfUser(currentUser, currentProject, 
										currentUser.getName());
		
		// test, if the user is in the same groups like the project.
		for(int i = 0; i < groups.size(); i++) {
			if( ((A_CmsGroup) groups.elementAt(i)).getId() == testProject.getGroupId() ) {
				return( true );
			}
		}
		
		return( false );
	}

	/**
	 * Reads a project from the Cms.
	 * 
	 * <B>Security</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the project to read.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	 public A_CmsProject readProject(A_CmsUser currentUser, A_CmsProject currentProject, 
									 String name)
		 throws CmsException {
		 return( m_projectRb.readProject(name) );
	 }
	
	/**
	 * Creates a project.
	 * 
	 * <B>Security</B>
	 * Only the users which are in the admin or projectleader-group are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the project to read.
	 * @param description The description for the new project.
	 * @param groupname the name of the group to be set.
	 * @param flags The flags to be set.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	 public A_CmsProject createProject(A_CmsUser currentUser, A_CmsProject currentProject, 
									   String name, String description, String groupname,
									   int flags)
		 throws CmsException {
		 if( isAdmin(currentUser, currentProject) || 
			 isProjectLeader(currentUser, currentProject)) {
			 
			 // create a new task for the project
			 // TODO: create the task with the taskRb!
			 A_CmsTask task = new CmsTask();
			 
			 // read the needed group from the cms
			 A_CmsGroup group = readGroup(currentUser, currentProject, groupname);
			 
			 return( m_projectRb.createProject(name, description, task, 
											   currentUser, group, C_FLAG_ENABLED ) );
		} else {
			 throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	 }
	
	/**
	 * Returns all projects, which are owned by the user or which are accessible
	 * for the group of the user.
	 * 
	 * <B>Security</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * 
	 * @return a Vector of projects.
	 */
	 public Vector getAllAccessibleProjects(A_CmsUser currentUser, 
											A_CmsProject currentProject)
		 throws CmsException {
		 
		// get all groups of the user
		Vector groups = getGroupsOfUser(currentUser, currentProject, 
										currentUser.getName());
		
		// get all projects which are owned by the user.
		Vector projects = m_projectRb.getAllAccessibleProjectsByUser(currentUser);
		
		// get all projects, that the user can access with his groups.
		for(int i = 0; i < groups.size(); i++) {
			// get all projects, which can be accessed by the current group
			Vector projectsByGroup = 
				m_projectRb.getAllAccessibleProjectsByGroup((A_CmsGroup)
															 groups.elementAt(i));
			// merge the projects to the vector
			for(int j = 0; j < projectsByGroup.size(); j++) {
				// add only projects, which are new
				if(!groups.contains(projectsByGroup.elementAt(j))) {
					groups.addElement(projectsByGroup.elementAt(j));
				}
			}
		}
		
		// return the vector of projects
		return(projects);
	 }	
	
	/**
	 * Publishes a project.
	 * 
	 * <B>Security</B>
	 * Only the admin or the owner of the project can do this.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the project to be published.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public A_CmsProject publishProject(A_CmsUser currentUser, 
									   A_CmsProject currentProject,
									   String name)
		throws CmsException {
		// TODO: implement this!
		return null;
	}

	// Metainfos, Metadefinitions
	/**
	 * Reads a metadefinition for the given resource type.
	 * 
	 * <B>Security</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the metadefinition to read.
	 * @param resourcetype The name of the resource type for which the 
	 * metadefinition is valid.
	 * 
	 * @return metadefinition The metadefinition that corresponds to the overgiven
	 * arguments - or null if there is no valid metadefinition.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public A_CmsMetadefinition readMetadefinition(A_CmsUser currentUser, 
												  A_CmsProject currentProject, 
												  String name, String resourcetype)
		throws CmsException {
		// no security check is needed here
		return( m_metadefRb.readMetadefinition(name, this.getResourceType(currentUser, 
																		  currentProject, 
																		  resourcetype)) );
	}
	
	/**
	 * Reads all metadefinitions for the given resource type.
	 * 
	 * <B>Security</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resourcetype The name of the resource type to read the 
	 * metadefinitions for.
	 * 
	 * @return metadefinitions A Vector with metadefefinitions for the resource type.
	 * The Vector is maybe empty.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */	
	public Vector readAllMetadefinitions(A_CmsUser currentUser, A_CmsProject currentProject, 
										 String resourcetype)
		throws CmsException {
		
		// No security to check
		return( m_metadefRb.readAllMetadefinitions(getResourceType(currentUser, 
																   currentProject, 
																   resourcetype) ) );
	}
	
	/**
	 * Reads all metadefinitions for the given resource type.
	 * 
	 * <B>Security</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resourcetype The name of the resource type to read the metadefinitions for.
	 * @param type The type of the metadefinition (normal|mandatory|optional).
	 * 
	 * @return metadefinitions A Vector with metadefefinitions for the resource type.
	 * The Vector is maybe empty.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */	
	public Vector readAllMetadefinitions(A_CmsUser currentUser, A_CmsProject currentProject, 
										 String resourcetype, int type)
		throws CmsException {
		
		// No security to check
		return( m_metadefRb.readAllMetadefinitions(getResourceType(currentUser, 
																   currentProject, 
																   resourcetype), type ) );
	}
	
	/**
	 * Creates the metadefinition for the resource type.<BR/>
	 * 
	 * <B>Security</B>
	 * Only the admin can do this.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the metadefinition to overwrite.
	 * @param resourcetype The name of the resource-type for the metadefinition.
	 * @param type The type of the metadefinition (normal|mandatory|optional)
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public A_CmsMetadefinition createMetadefinition(A_CmsUser currentUser, 
													A_CmsProject currentProject, 
													String name, 
													String resourcetype, 
													int type)
		throws CmsException {
		// check the security
		if( isAdmin(currentUser, currentProject) ) {
			return( m_metadefRb.createMetadefinition(name, 
													 getResourceType(currentUser, 
																	 currentProject, 
																	 resourcetype),
													 type) );
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
		
	/**
	 * Delete the metadefinition for the resource type.<BR/>
	 * 
	 * <B>Security</B>
	 * Only the admin can do this.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the metadefinition to read.
	 * @param resourcetype The name of the resource type for which the 
	 * metadefinition is valid.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public void deleteMetadefinition(A_CmsUser currentUser, A_CmsProject currentProject, 
									 String name, String resourcetype)
		throws CmsException {
		// check the security
		if( isAdmin(currentUser, currentProject) ) {
			// first read and then delete the metadefinition.
			m_metadefRb.deleteMetadefinition(
				m_metadefRb.readMetadefinition(name, 
											   getResourceType(currentUser, 
															   currentProject, 
															   resourcetype)));
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
	
	/**
	 * Updates the metadefinition for the resource type.<BR/>
	 * 
	 * <B>Security</B>
	 * Only the admin can do this.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param metadef The metadef to be written.
	 * 
	 * @return The metadefinition, that was written.
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public A_CmsMetadefinition writeMetadefinition(A_CmsUser currentUser, 
												   A_CmsProject currentProject, 
												   A_CmsMetadefinition metadef)
		throws CmsException {
		// check the security
		if( isAdmin(currentUser, currentProject) ) {
			return( m_metadefRb.writeMetadefinition(metadef) );
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
	
	/**
	 * Returns a Metainformation of a file or folder.
	 * 
	 * <B>Security</B>
	 * Only the user is granted, who has the right to view the resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The name of the resource of which the Metainformation has 
	 * to be read.
	 * @param meta The Metadefinition-name of which the Metainformation has to be read.
	 * 
	 * @return metainfo The metainfo as string.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public String readMetainformation(A_CmsUser currentUser, A_CmsProject currentProject, 
									  String resource, String meta)
		throws CmsException {
		// TODO: check the security
		// TODO: read the resource realy!
		// A fake resource is created to check metainfo-handling
		A_CmsResource res = new CmsResource(resource, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0);
		
		return( m_metadefRb.readMetainformation(res, meta) );
	}	

	/**
	 * Writes a Metainformation for a file or folder.
	 * 
	 * <B>Security</B>
	 * Only the user is granted, who has the right to write the resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The name of the resource of which the Metainformation has 
	 * to be read.
	 * @param meta The Metadefinition-name of which the Metainformation has to be set.
	 * @param value The value for the metainfo to be set.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public void writeMetainformation(A_CmsUser currentUser, A_CmsProject currentProject, 
									 String resource, String meta, String value)
		throws CmsException {
		// TODO: check the security
		// TODO: read the resource realy!
		// A fake resource is created to check metainfo-handling
		A_CmsResource res = new CmsResource(resource, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0);
		
		m_metadefRb.writeMetainformation(res, meta, value);
	}

	/**
	 * Writes a couple of Metainformation for a file or folder.
	 * 
	 * <B>Security</B>
	 * Only the user is granted, who has the right to write the resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The name of the resource of which the Metainformation 
	 * has to be read.
	 * @param metainfos A Hashtable with Metadefinition- metainfo-pairs as strings.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public void writeMetainformations(A_CmsUser currentUser, A_CmsProject currentProject, 
									  String resource, Hashtable metainfos)
		throws CmsException {
		// TODO: check the security
		// TODO: read the resource realy!
		// A fake resource is created to check metainfo-handling
		A_CmsResource res = new CmsResource(resource, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0);
		
		m_metadefRb.writeMetainformations(res, metainfos);
	}

	/**
	 * Returns a list of all Metainformations of a file or folder.
	 * 
	 * <B>Security</B>
	 * Only the user is granted, who has the right to view the resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The name of the resource of which the Metainformation has to be 
	 * read.
	 * 
	 * @return Hashtable of Metainformation as Strings.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public Hashtable readAllMetainformations(A_CmsUser currentUser, A_CmsProject currentProject, 
											 String resource)
		throws CmsException {
		// TODO: check the security
		// TODO: read the resource realy!
		// A fake resource is created to check metainfo-handling
		A_CmsResource res = new CmsResource(resource, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0);
		
		return( m_metadefRb.readAllMetainformations(res) );
	}
	
	/**
	 * Deletes all Metainformation for a file or folder.
	 * 
	 * <B>Security</B>
	 * Only the user is granted, who has the right to write the resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The name of the resource of which the Metainformations 
	 * have to be deleted.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public void deleteAllMetainformations(A_CmsUser currentUser, 
										  A_CmsProject currentProject, 
										  String resource)
		throws CmsException {
		// TODO: check the security
		// TODO: read the resource realy!
		// A fake resource is created to check metainfo-handling
		A_CmsResource res = new CmsResource(resource, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0);
		
		// are there some mandatory metadefs?
		if( m_metadefRb.readAllMetadefinitions(res.getType(), 
											   C_METADEF_TYPE_MANDATORY).size() == 0  ) {
			// no - delete them all
			m_metadefRb.deleteAllMetainformations(res);
		} else {
			// yes - throw exception
			 throw new CmsException(CmsException.C_EXTXT[CmsException.C_MANDATORY_METAINFO],
				CmsException.C_MANDATORY_METAINFO);
		}
	}

	/**
	 * Deletes a Metainformation for a file or folder.
	 * 
	 * <B>Security</B>
	 * Only the user is granted, who has the right to write the resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The name of the resource of which the Metainformation 
	 * has to be read.
	 * @param meta The Metadefinition-name of which the Metainformation has to be set.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public void deleteMetainformation(A_CmsUser currentUser, A_CmsProject currentProject, 
									  String resource, String meta)
		throws CmsException {
		// TODO: check the security
		// TODO: read the resource realy!
		// A fake resource is created to check metainfo-handling
		A_CmsResource res = new CmsResource(resource, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0);

		// read the metadefinition
		A_CmsMetadefinition metadef = m_metadefRb.readMetadefinition(meta, res.getType());
		
		// is this a mandatory metadefinition?
		if(  (metadef != null) && 
			 (metadef.getMetadefType() != C_METADEF_TYPE_MANDATORY )  ) {
			// no - delete the information
			m_metadefRb.deleteMetainformation(res, meta);
		} else {
			// yes - throw exception
			 throw new CmsException(CmsException.C_EXTXT[CmsException.C_MANDATORY_METAINFO],
				CmsException.C_MANDATORY_METAINFO);
		}
	}

	// user and group stuff
	
	/**
	 * Determines, if the users current group is the admin-group.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @return true, if the users current group is the admin-group, 
	 * else it returns false.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */	
	public boolean isAdmin(A_CmsUser currentUser, A_CmsProject currentProject)
		throws CmsException {
		return( m_userRb.userInGroup(currentUser.getName(), C_GROUP_ADMIN) );
	}
    
   	/**
	 * Determines, if the users current group is the projectleader-group.<BR/>
	 * All projectleaders can create new projects, or close their own projects.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @return true, if the users current group is the projectleader-group, 
	 * else it returns false.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */	
	public boolean isProjectLeader(A_CmsUser currentUser, A_CmsProject currentProject) 
		throws CmsException { 
		return( m_userRb.userInGroup(currentUser.getName(), C_GROUP_PROJECTLEADER) );
	}

	/**
	 * Returns the anonymous user object.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @return the anonymous user object.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public A_CmsUser anonymousUser(A_CmsUser currentUser, A_CmsProject currentProject) 
		throws CmsException {
		return( m_userRb.readUser(C_USER_GUEST) );
	}
	
	/**
	 * Returns a user object.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param username The name of the user that is to be read.
	 * @return User
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public A_CmsUser readUser(A_CmsUser currentUser, A_CmsProject currentProject, 
							  String username)
		throws CmsException{
		return( m_userRb.readUser(username) );
	}
	
	/**
	 * Returns a user object if the password for the user is correct.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param username The username of the user that is to be read.
	 * @param password The password of the user that is to be read.
	 * @return User
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful
	 */		
	public A_CmsUser readUser(A_CmsUser currentUser, A_CmsProject currentProject, 
							  String username, String password)
		throws CmsException{
 		return( m_userRb.readUser(username, password) );
	}

	/**
	 * Returns a list of groups of a user.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param username The name of the user.
	 * @return Vector of groups
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public Vector getGroupsOfUser(A_CmsUser currentUser, A_CmsProject currentProject, 
								  String username)
		throws CmsException {
		return(m_userRb.getGroupsOfUser(username));
	}

	/**
	 * Returns a group object.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param groupname The name of the group that is to be read.
	 * @return Group.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful
	 */
	public A_CmsGroup readGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
								String groupname)
		throws CmsException {
		
		return m_userRb.readGroup(groupname);
	}

	/**
	 * Returns a group object.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param groupId The id of the group that is to be read.
	 * @return Group.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful
	 */
	private A_CmsGroup readGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
								int groupId)
		throws CmsException {
		
		return m_userRb.readGroup(groupId);
	}

	/**
	 * Returns a list of users in a group.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param groupname The name of the group to list users from.
	 * @return Vector of users.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */
	public Vector getUsersOfGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
								  String groupname)
		throws CmsException {
		// check the security
		if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
			return m_userRb.getUsersOfGroup(groupname);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/**
	 * Checks if a user is member of a group.<P/>
	 *  
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param nameuser The name of the user to check.
	 * @param groupname The name of the group to check.
	 * @return True or False
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public boolean userInGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
							   String username, String groupname)
		throws CmsException {
		// check the security
		if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
			return m_userRb.userInGroup(username, groupname);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/** 
	 * Adds a user to the Cms.
	 * 
	 * Only a adminstrator can add users to the cms.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The new name for the user.
	 * @param password The new password for the user.
	 * @param group The default groupname for the user.
	 * @param description The description for the user.
	 * @param additionalInfos A Hashtable with additional infos for the user. These
	 * Infos may be stored into the Usertables (depending on the implementation).
	 * @param flags The flags for a user (e.g. C_FLAG_ENABLED)
	 * 
	 * @return user The added user will be returned.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */
	public A_CmsUser addUser(A_CmsUser currentUser, A_CmsProject currentProject, 
							 String name, String password, 
					  String group, String description, 
					  Hashtable additionalInfos, int flags)
		throws CmsException {
		
		// Check the security
		if( isAdmin(currentUser, currentProject) ) {
			return( m_userRb.addUser(name, password, group, description, 
 									 additionalInfos, flags) );
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/** 
	 * Deletes a user from the Cms.
	 * 
	 * Only a adminstrator can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the user to be deleted.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */
	public void deleteUser(A_CmsUser currentUser, A_CmsProject currentProject, 
						   String username)
		throws CmsException{ 
		// Check the security
		// Avoid to delete admin or guest-user
		if( isAdmin(currentUser, currentProject) && 
			!(username.equals(C_USER_ADMIN) || username.equals(C_USER_GUEST))) {
			m_userRb.deleteUser(username);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/**
	 * Updated the user information.<BR/>
	 * 
	 * Only the administrator can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param user The  user to be updated.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
    public void writeUser(A_CmsUser currentUser, A_CmsProject currentProject, 
						  A_CmsUser user)			
		throws CmsException {
		// Check the security
		if( isAdmin(currentUser, currentProject) ) {
			
			// prevent the admin to be set disabled!
			if( isAdmin(user, currentProject) ) {
				user.setEnabled();
			}
			
			m_userRb.writeUser(user);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/**
	 * Add a new group to the Cms.<BR/>
	 * 
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param name The name of the new group.
	 * @param description The description for the new group.
	 * @int flags The flags for the new group.
	 * @param name The name of the parent group (or null).
	 *
	 * @return Group
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */	
	public A_CmsGroup addGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
							   String name, String description, int flags, String parent)
		throws CmsException {
		// Check the security
		if( isAdmin(currentUser, currentProject) ) {
			return( m_userRb.addGroup(name, description, flags, parent) );
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

    /**
	 * Writes an already existing group in the Cms.<BR/>
	 * 
	 * Only the admin can do this.<P/>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param group The group that should be written to the Cms.
	 * @exception CmsException  Throws CmsException if operation was not succesfull.
	 */	
	public void writeGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
						   A_CmsGroup group)
		throws CmsException {
		
		// Check the security
		if( isAdmin(currentUser, currentProject) ) {
			m_userRb.writeGroup(group);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
    
	/**
	 * Delete a group from the Cms.<BR/>
	 * Only groups that contain no subgroups can be deleted.
	 * 
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param delgroup The name of the group that is to be deleted.
	 * @exception CmsException  Throws CmsException if operation was not succesfull.
	 */	
	public void deleteGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
							String delgroup)
		throws CmsException {
		// Check the security
		if( isAdmin(currentUser, currentProject) ) {
			m_userRb.deleteGroup(delgroup);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/**
	 * Adds a user to a group.<BR/>
     *
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param username The name of the user that is to be added to the group.
	 * @param groupname The name of the group.
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */	
	public void addUserToGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
							   String username, String groupname)
		throws CmsException {
		// Check the security
		if( isAdmin(currentUser, currentProject) ) {
			m_userRb.addUserToGroup(username, groupname);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/**
	 * Removes a user from a group.
	 * 
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param username The name of the user that is to be removed from the group.
	 * @param groupname The name of the group.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */	
	public void removeUserFromGroup(A_CmsUser currentUser, A_CmsProject currentProject, 
									String username, String groupname)
		throws CmsException {
		// Check the security
		if( isAdmin(currentUser, currentProject) ) {
			m_userRb.removeUserFromGroup(username, groupname);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/**
	 * Returns all users<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @return users A Vector of all existing users.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */
	public Vector getUsers(A_CmsUser currentUser, A_CmsProject currentProject)
        throws CmsException {
		
		// check security
		if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
			return m_userRb.getUsers();
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
	
	/**
	 * Returns all groups<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @return users A Vector of all existing groups.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */
	public Vector getGroups(A_CmsUser currentUser, A_CmsProject currentProject)
        throws CmsException {
		// check security
		if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
			return m_userRb.getGroups();
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
    
    /**
	 * Returns all child groups of a groups<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param groupname The name of the group.
	 * @return users A Vector of all child groups or null.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */
	public Vector getChild(A_CmsUser currentUser, A_CmsProject currentProject, 
						   String groupname)
        throws CmsException {
		// check security
		if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) ) {
			return m_userRb.getChild(groupname);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/** 
	 * Sets the password for a user.
	 * 
	 * Only a adminstrator or the curretuser can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * Current users can change their own password.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param username The name of the user.
	 * @param newPassword The new password.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */
	public void setPassword(A_CmsUser currentUser, A_CmsProject currentProject, 
							String username, String newPassword)
		throws CmsException {
		// read the user
		A_CmsUser user = readUser(currentUser, currentProject, username);
		if( ! anonymousUser(currentUser, currentProject).equals( currentUser ) && 
			( isAdmin(user, currentProject) || user.equals(currentUser)) ) {
			m_userRb.setPassword(username, newPassword);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

    /**
	 * Adds a new CmsMountPoint. 
	 * A new mountpoint for a mysql filesystem is added.
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param mountpoint The mount point in the Cms filesystem.
	 * @param driver The driver for the db-system. 
	 * @param connect The connectstring to access the db-system.
	 * @param name A name to describe the mountpoint.
	 */
	synchronized public void addMountPoint(A_CmsUser currentUser, 
										   A_CmsProject currentProject,
										   String mountpoint, String driver, 
										   String connect, String name)
		throws CmsException {
		
		if( isAdmin(currentUser, currentProject) ) {
			
			// TODO: check, if the mountpoint is valid (exists the folder?)
			
			// create the new mountpoint			
			A_CmsMountPoint newMountPoint = new CmsMountPoint(mountpoint, driver,
															  connect, name);
			
			// read all mountpoints from propertys
			Hashtable mountpoints = (Hashtable) 
									 m_propertyRb.readProperty(C_PROPERTY_MOUNTPOINT);
			
			// if mountpoints dosen't exists - create them.
			if(mountpoints == null) {
				mountpoints = new Hashtable();
				m_propertyRb.addProperty(C_PROPERTY_MOUNTPOINT, mountpoints);
			}
			
			// add the new mountpoint
			mountpoints.put(newMountPoint.getMountpoint(), newMountPoint);
			
			// write the mountpoints back to the properties
			m_propertyRb.writeProperty(C_PROPERTY_MOUNTPOINT, mountpoints);			
			
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}		
	}

    /**
	 * Adds a new CmsMountPoint. 
	 * A new mountpoint for a disc filesystem is added.
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param mountpoint The mount point in the Cms filesystem.
	 * @param mountpath The physical location this mount point directs to. 
	 * @param name The name of this mountpoint.
	 * @param user The default user for this mountpoint.
	 * @param group The default group for this mountpoint.
	 * @param type The default resourcetype for this mountpoint.
	 * @param accessFLags The access-flags for this mountpoint.
	 */
	synchronized public void addMountPoint(A_CmsUser currentUser, 
										   A_CmsProject currentProject,
										   String mountpoint, String mountpath, 
										   String name, String user, String group,
										   String type, int accessFlags)
		throws CmsException {
		
		if( isAdmin(currentUser, currentProject) ) {
			
			// TODO: check, if the mountpoint is valid (exists the folder?)
			
			// create the new mountpoint
			
			// TODO: implement this
			A_CmsMountPoint newMountPoint = null;
/*			A_CmsMountPoint newMountPoint = 
				new CmsMountPoint(mountpoint, mountpath,
								  name, 
								  readUser(currentUser, currentProject, user), 
								  readGroup(currentUser, currentProject, group), 
								  onlineProject(currentUser, currentProject), 
								  null,
								  accessFlags);
*/			
			// read all mountpoints from propertys
			Hashtable mountpoints = (Hashtable) 
									 m_propertyRb.readProperty(C_PROPERTY_MOUNTPOINT);
			
			// if mountpoints dosen't exists - create them.
			if(mountpoints == null) {
				mountpoints = new Hashtable();
				m_propertyRb.addProperty(C_PROPERTY_MOUNTPOINT, mountpoints);
			}
			
			// add the new mountpoint
			mountpoints.put(newMountPoint.getMountpoint(), newMountPoint);
			
			// write the mountpoints back to the properties
			m_propertyRb.writeProperty(C_PROPERTY_MOUNTPOINT, mountpoints);			
			
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}		
	}
	
	/**
	 * Gets a CmsMountPoint. 
	 * A mountpoint will be returned.
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param mountpoint The mount point in the Cms filesystem.
	 * 
	 * @return the mountpoint - or null if it doesen't exists.
	 */
	public A_CmsMountPoint readMountPoint(A_CmsUser currentUser, 
										  A_CmsProject currentProject, 
										  String mountpoint )
		throws CmsException {
		
		if( isAdmin(currentUser, currentProject) ) {
			
			// read all mountpoints from propertys
			Hashtable mountpoints = (Hashtable) 
									 m_propertyRb.readProperty(C_PROPERTY_MOUNTPOINT);
			
			// no mountpoints available?
			if(mountpoint == null) {
				return(null);
			}
			// return the mountpoint
			return( (A_CmsMountPoint) mountpoints.get(mountpoint));
			
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}		
	}

    /**
	 * Deletes a CmsMountPoint. 
	 * A mountpoint will be deleted.
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param mountpoint The mount point in the Cms filesystem.
	 */
	synchronized public void deleteMountPoint(A_CmsUser currentUser, 
											  A_CmsProject currentProject, 
											  String mountpoint )
		throws CmsException {
		
		if( isAdmin(currentUser, currentProject) ) {
			
			// read all mountpoints from propertys
			Hashtable mountpoints = (Hashtable) 
									 m_propertyRb.readProperty(C_PROPERTY_MOUNTPOINT);
			
			if(mountpoint != null) {
				// remove the mountpoint
				mountpoints.remove(mountpoint);
				// write the mountpoints back to the properties
				m_propertyRb.writeProperty(C_PROPERTY_MOUNTPOINT, mountpoints);			
			}
			
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}		
	}
	
	/**
	 * Gets all CmsMountPoints. 
	 * All mountpoints will be returned.
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * 
	 * @return the mountpoints - or null if they doesen't exists.
	 */
	public Hashtable getAllMountPoints(A_CmsUser currentUser, A_CmsProject currentProject)
		throws CmsException {
		
		if( isAdmin(currentUser, currentProject) ) {
			
			// read all mountpoints from propertys
			return( (Hashtable) m_propertyRb.readProperty(C_PROPERTY_MOUNTPOINT));
			
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}		
	}
	
	/**
	 * Returns a Vector with all I_CmsResourceTypes.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * 
	 * Returns a Hashtable with all I_CmsResourceTypes.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful.
	 */
	public Hashtable getAllResourceTypes(A_CmsUser currentUser, 
										 A_CmsProject currentProject) 
		throws CmsException {
		// check, if the resourceTypes were read bevore
		if(m_resourceTypes == null) {
			// read the resourceTypes from the propertys
			m_resourceTypes = (Hashtable) 
							   m_propertyRb.readProperty(C_PROPERTY_RESOURCE_TYPE);

			// remove the last index.
			m_resourceTypes.remove(C_TYPE_LAST_INDEX);
		}
		
		// return the resource-types.
		return(m_resourceTypes);
	}

	/**
	 * Returns a CmsResourceTypes.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resourceType the name of the resource to get.
	 * 
	 * Returns a CmsResourceTypes.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful.
	 */
	public A_CmsResourceType getResourceType(A_CmsUser currentUser, 
									 A_CmsProject currentProject,
									 String resourceType) 
		throws CmsException {
		return((A_CmsResourceType)getAllResourceTypes(currentUser, currentProject).
			get(resourceType));
	}
	
	/**
	 * Adds a CmsResourceTypes.
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resourceType the name of the resource to get.
	 * @param launcherType the launcherType-id
	 * @param launcherClass the name of the launcher-class normaly ""
	 * 
	 * Returns a CmsResourceTypes.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful.
	 */
	public A_CmsResourceType addResourceType(A_CmsUser currentUser, 
											 A_CmsProject currentProject,
											 String resourceType, int launcherType, 
											 String launcherClass) 
		throws CmsException {
		if( isAdmin(currentUser, currentProject) ) {

			// read the resourceTypes from the propertys
			m_resourceTypes = (Hashtable) 
							   m_propertyRb.readProperty(C_PROPERTY_RESOURCE_TYPE);

			synchronized(m_resourceTypes) {

				// get the last index and increment it.
				Integer lastIndex = 
					new Integer(((Integer)
								  m_resourceTypes.get(C_TYPE_LAST_INDEX)).intValue() + 1);
						
				// write the last index back
				m_resourceTypes.put(C_TYPE_LAST_INDEX, lastIndex); 
						
				// add the new resource-type
				m_resourceTypes.put(resourceType, new CmsResourceType(lastIndex.intValue(), 
																	  launcherType, 
																	  resourceType, 
																	  launcherClass));
						
				// store the resource types in the properties
				m_propertyRb.writeProperty(C_PROPERTY_RESOURCE_TYPE, m_resourceTypes);
						
			}

			// the cached resource types aren't valid any more.
			m_resourceTypes = null;				
			// return the new resource-type
			return(getResourceType(currentUser, currentProject, resourceType));
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
	
	/**
	 * Reads a file from the Cms.<BR/>
	 * 
	 * <B>Security:</B>
	 * Access is granted, if:
	 * <ul>
	 * <li>the user has access to the project</li>
	 * <li>the user can read the resource</li>
	 * </ul>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param filename The name of the file to be read.
	 * 
	 * @return The file read from the Cms.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful.
	 * */
	 public CmsFile readFile(A_CmsUser currentUser, A_CmsProject currentProject,
							 String filename)
		 throws CmsException {
		 // HACK: !!!
		 // TODO: THIS is NOT correct, because there is no security-check!
		 // HACK: !!!
		 //return( m_fileRb.readFile(currentProject, filename) );
         return null;
	 }

	/**
	 * Reads a folder from the Cms.<BR/>
	 * 
	 * <B>Security:</B>
	 * Access is granted, if:
	 * <ul>
	 * <li>the user has access to the project</li>
	 * <li>the user can read the resource</li>
	 * </ul>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param folder The complete path to the folder from which the folder will be 
	 * read.
	 * @param foldername The name of the folder to be read.
	 * 
	 * @return folder The read folder.
	 * 
	 * @exception CmsException will be thrown, if the folder couldn't be read. 
	 * The CmsException will also be thrown, if the user has not the rights 
	 * for this resource.
	 */
	public CmsFolder readFolder(A_CmsUser currentUser, A_CmsProject currentProject,
								String folder, String folderName)
		throws CmsException {
			
		CmsFolder cmsFolder = m_fileRb.readFolder(currentProject, 
												  folder + folderName);
		if( accessRead(currentUser, currentProject, (A_CmsResource)cmsFolder) ) {
				
			// acces to all subfolders was granted - return the folder.
			return(cmsFolder);
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}
	
	/**
	 * Creates a new folder.
	 * If some mandatory Metadefinitions for the resourcetype are missing, a 
	 * CmsException will be thrown, because the file cannot be created without
	 * the mandatory Metainformations.<BR/>
	 * 
	 * <B>Security:</B>
	 * Access is granted, if:
	 * <ul>
	 * <li>the user has access to the project</li>
	 * <li>the user can write the resource</li>
	 * <li>the resource is not locked by another user</li>
	 * </ul>
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param folder The complete path to the folder in which the new folder will 
	 * be created.
	 * @param newFolderName The name of the new folder (No pathinformation allowed).
	 * @param metainfos A Hashtable of metainfos, that should be set for this folder.
	 * The keys for this Hashtable are the names for Metadefinitions, the values are
	 * the values for the metainfos.
	 * 
	 * @return file The created file.
	 * 
	 * @exception CmsException will be thrown for missing metainfos, for worng metadefs
	 * or if the filename is not valid. The CmsException will also be thrown, if the 
	 * user has not the rights for this resource.
	 */
	public CmsFolder createFolder(A_CmsUser currentUser, A_CmsProject currentProject, 
								  String folder, String newFolderName, 
								  Hashtable metainfos)
		throws CmsException {
		
		// TODO: write the metainfos!
		
		// checks, if the filename is valid, if not it throws a exception
		validFilename(newFolderName);
		
		CmsFolder cmsFolder = m_fileRb.readFolder(currentProject, 
												  folder);
		if( accessCreate(currentUser, currentProject, (A_CmsResource)cmsFolder) ) {
				
			// write-acces  was granted - create and return the folder.
			return(m_fileRb.createFolder(currentUser, currentProject, 
										 folder + newFolderName + C_FOLDER_SEPERATOR,
										 0));
		} else {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_NO_ACCESS],
				CmsException.C_NO_ACCESS);
		}
	}

	/**
	 * Checks, if the user may read this resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The resource to check.
	 * 
	 * @return wether the user has access, or not.
	 */
	private boolean accessRead(A_CmsUser currentUser, A_CmsProject currentProject,
							   A_CmsResource resource) 
		throws CmsException	{
		
		// check the access to the project
		if( ! accessProject(currentUser, currentProject, currentProject.getName()) ) {
			// no access to the project!
			return(false);
		}
		
		// check the rights.
		do {
			if( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_READ) || 
				accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_READ) ||
				accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_READ) ) {
				
				// read next resource
				if(resource.getParent() != null) {
					resource = m_fileRb.readFolder(currentProject, resource.getParent());
				}
			} else {
				// last check was negative
				return(false);
			}
		} while(resource.getParent() != null);
		
		// all checks are done positive
		return(true);
	}

	/**
	 * Checks, if the user may create this resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The resource to check.
	 * 
	 * @return wether the user has access, or not.
	 */
	private boolean accessCreate(A_CmsUser currentUser, A_CmsProject currentProject,
								A_CmsResource resource) 
		throws CmsException	{
		
		// check, if this is the onlineproject
		// TODO: this should be uncommented - the online-project is not for creating!
/*		if(onlineProject(currentUser, currentProject).equals(currentProject)){
			// the online-project is not writeable!
			return(false);
		} */
		
		// check the access to the project
		if( ! accessProject(currentUser, currentProject, currentProject.getName()) ) {
			// no access to the project!
			return(false);
		}
		
		// check the rights and if the resource is not locked
		do {
			if( accessOther(currentUser, currentProject, resource, C_ACCESS_PUBLIC_WRITE) || 
				accessOwner(currentUser, currentProject, resource, C_ACCESS_OWNER_WRITE) ||
				accessGroup(currentUser, currentProject, resource, C_ACCESS_GROUP_WRITE) ) {
				
				// is the resource locked?
				if(resource.isLocked()) {
					// resource locked, no creation allowed
					return(false);					
				}
				
				// read next resource
				if(resource.getParent() != null) {
					resource = m_fileRb.readFolder(currentProject, resource.getParent());
				}
			} else {
				// last check was negative
				return(false);
			}
		} while(resource.getParent() != null);
		
		// all checks are done positive
		return(true);
	}
	
	/**
	 * Checks, if the owner may access this resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The resource to check.
	 * @param flags The flags to check.
	 * 
	 * @return wether the user has access, or not.
	 */
	private boolean accessOwner(A_CmsUser currentUser, A_CmsProject currentProject,
								A_CmsResource resource, int flags) 
		throws CmsException {
		// The Admin has always access
		if( isAdmin(currentUser, currentProject) ) {
			return(true);
		}
		// is the resource owned by this user?
		if(resource.getOwnerId() == currentUser.getId()) {
			if( (resource.getAccessFlags() & flags) == flags ) {
				return( true );
			}
		}
		// the resource isn't accesible by the user.
		return(false);
	}

	/**
	 * Checks, if the group may access this resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The resource to check.
	 * @param flags The flags to check.
	 * 
	 * @return wether the user has access, or not.
	 */
	private boolean accessGroup(A_CmsUser currentUser, A_CmsProject currentProject,
								A_CmsResource resource, int flags)
		throws CmsException {

		// TODO: implement this! read group by id is missing here!
		
		// is the user in the group for the resource?
		if(userInGroup(currentUser, currentProject, currentUser.getName(), 
					   readGroup(currentUser, currentProject, 
								 resource.getGroupId()).getName())) {
			if( (resource.getAccessFlags() & flags) == flags ) {
				return( true );
			}
		}
		// the resource isn't accesible by the user.

		return(false);

	}

	/**
	 * Checks, if others may access this resource.
	 * 
	 * @param currentUser The user who requested this method.
	 * @param currentProject The current project of the user.
	 * @param resource The resource to check.
	 * @param flags The flags to check.
	 * 
	 * @return wether the user has access, or not.
	 */
	private boolean accessOther(A_CmsUser currentUser, A_CmsProject currentProject, 
								A_CmsResource resource, int flags)
		throws CmsException {
		
		if( (resource.getAccessFlags() & flags) == flags ) {
			return( true );
		} else {
			return( false );
		}
		
	}
	
	/**
	 * Checks ii characters in a String are allowed for filenames
	 * 
	 * @param filename String to check
	 * 
	 * @exception throws a exception, if the check fails.
	 */	
	private void validFilename( String filename ) 
		throws CmsException {
		
		if (filename == null) {
			throw new CmsException(CmsException.C_EXTXT[CmsException.C_BAD_NAME],
				CmsException.C_BAD_NAME);
		}

		int l = filename.length();

		for (int i=0; i<l; i++) {
			char c = filename.charAt(i);
			if ( 
				((c < 'a') || (c > 'z')) &&
				((c < '0') || (c > '9')) &&
				((c < 'A') || (c > 'Z')) &&
				(c != '-') && (c != '/') && (c != '.') &&
				(c != '|') && (c != '_') //removed because of MYSQL regexp syntax
				) {
				throw new CmsException(CmsException.C_EXTXT[CmsException.C_BAD_NAME],
					CmsException.C_BAD_NAME);
			}
		}
	}
}