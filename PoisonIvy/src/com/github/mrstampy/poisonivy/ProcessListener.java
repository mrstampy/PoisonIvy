/*
 * Poison Ivy - Java Library Dependency Resolver and Application Launcher 
 *
 * Copyright (C) 2014 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.poisonivy;

/**
 * The listener interface for receiving process events. The class that is
 * interested in processing a process event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addProcessListener<code> method. When
 * the process event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see ProcessContainer
 * @see PoisonIvy#addResolveListener(ProcessListener)
 * @see PoisonIvy#addExeListener(ProcessListener)
 */
public interface ProcessListener {

	/**
	 * Called when a process's output stream has data.
	 * 
	 * @param out
	 *          the out
	 */
	void output(String out);

	/**
	 * Called when a process's error stream has data.
	 * 
	 * @param err
	 *          the err
	 */
	void error(String err);
}
