/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2010 The eXist Project
 *  http://exist-db.org
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.security.realm;

import java.util.Collection;

import org.exist.EXistException;
import org.exist.security.AuthenticationException;
import org.exist.security.Group;
import org.exist.security.PermissionDeniedException;
import org.exist.security.User;
import org.exist.storage.DBBroker;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public interface Realm {
	
	public String getId();
	
	//accounts manipulation methods
	public User getAccount(String name);
	public Collection<User> getAccounts();
	public boolean hasAccount(String accountName);
	
	public boolean updateAccount(User account) throws PermissionDeniedException, EXistException;
	
	//roles manipulation methods
	public Collection<Group> getRoles();

	public Group getRole(String name);
	public boolean hasRole(String name);

	User authenticate(String username, Object credentials) throws AuthenticationException;

	//possible, internal methods
	public User getAccount(int id);
	public Group getRole(int id);

	public void startUp(DBBroker broker) throws EXistException;
}