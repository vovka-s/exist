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
package org.exist.security.openid;

import java.util.Set;

import org.exist.config.ConfigurationException;
import org.exist.config.annotation.ConfigurationClass;
import org.exist.security.AXSchemaType;
import org.exist.security.Group;
import org.exist.security.PermissionDeniedException;
import org.exist.security.AbstractAccount;
import org.exist.security.AbstractRealm;
import org.exist.security.Account;
import org.exist.security.internal.GroupImpl;
import org.exist.security.SecurityManager;
import org.exist.xmldb.XmldbURI;
import org.openid4java.discovery.Identifier;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
@ConfigurationClass("account")
public class AccountImpl extends AbstractAccount {

	Identifier  _identifier = null;
	
	public AccountImpl(AbstractRealm realm, Identifier identifier) throws ConfigurationException {
		super(realm, -1, identifier.getIdentifier());
		_identifier = identifier;
	}

	@Override
	public void setPassword(String passwd) {
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public XmldbURI getHome() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDigestPassword() {
		return null;
	}

	//TODO: find a place to construct 'full' name
	public String getName_() {
            String name = "";

            Set<AXSchemaType> metadataKeys = getMetadataKeys();

            if(metadataKeys.contains(AXSchemaType.FIRSTNAME)) {
                name += getMetadataValue(AXSchemaType.FIRSTNAME);
            }

            if(metadataKeys.contains(AXSchemaType.LASTNAME)) {
                if(name.length() > 0 ) {
                    name += " ";
                }
                name += getMetadataValue(AXSchemaType.LASTNAME);
            }

            if(name.length() == 0) {
                name += getMetadataValue(AXSchemaType.FULLNAME);
            }

            if(name.length() == 0) {
                name = _identifier.getIdentifier();
            }

            return name;
	}

    @Override
    public Group addGroup(Group group) throws PermissionDeniedException {

        if(group == null){
            return null;
        }

        Account user = getDatabase().getSubject();


        if(!((user != null && user.hasDbaRole()) || ((GroupImpl)group).isMembersManager(user))){
                throw new PermissionDeniedException("not allowed to change group memberships");
        }

        if(!groups.contains(group)) {
            groups.add(group);

            if(SecurityManager.DBA_GROUP.equals(name)) {
                hasDbaRole = true;
            }
        }

        return group;
    }
}