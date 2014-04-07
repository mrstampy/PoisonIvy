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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class ProcessContainer encapsulates a running process, allowing
 * listeners to be added to the output streams.
 */
class ProcessContainer {

	private Process process;
	private List<ProcessListener> listeners = new ArrayList<ProcessListener>();
	private volatile boolean running = true;
	
	/**
	 * Instantiates a new process container with a process and list of listeners.
	 *
	 * @param p the p
	 * @param listeners the listeners
	 */
	public ProcessContainer(Process p, List<ProcessListener> listeners) {
		setProcess(p);
		if(listeners != null) this.listeners.addAll(listeners);
		init();
	}

	private void init() {
		logOutput(process.getInputStream());
		logError(process.getErrorStream());
	}

	private void logError(final InputStream in) {
		Thread thread = new Thread("Resolver error stream thread") {
			public void run() {
				while (isRunning()) {
					try {
						Thread.sleep(200);
						String err = getOutput(in);
						if (err != null) error(err);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};

		thread.start();
	}

	private void logOutput(final InputStream in) {
		Thread thread = new Thread("Resolver out stream thread") {
			public void run() {
				while (isRunning()) {
					try {
						Thread.sleep(200);
						String out = getOutput(in);
						if (out != null) output(out);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};

		thread.start();
	}

	private void error(String err) {
		System.err.print(err);

		for (ProcessListener l : listeners) {
			l.error(err);
		}
	}

	private void output(String out) {
		System.out.println(out);

		for (ProcessListener l : listeners) {
			l.output(out);
		}
	}

	private String getOutput(InputStream in) throws IOException {
		int available = in.available();
		if (available <= 0) return null;

		byte[] b = new byte[available];
		in.read(b);

		return new String(b);
	}

	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Sets the running.  Call when the process has finished running.
	 *
	 * @param running the new running
	 * @see Process#waitFor()
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * Gets the process.
	 *
	 * @return the process
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * Sets the process.
	 *
	 * @param process the new process
	 */
	public void setProcess(Process process) {
		this.process = process;
	}
}
