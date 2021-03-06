/*
 * Copyright 2014 Computing Distribution Group Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.groupcdg.maven.cordova;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;



public abstract class AbstractCordovaMojo extends AbstractMojo {

	protected static final String CREATE_DIRECTORY_ERROR_MESSAGE = "Could not create directory ";



	private static final String RESOURCES_DIRECTORY = "resources";

	private static final String LOGS_DIRECTORY = "logs";

	private static final String OUT_LOG_SUFFIX = ".out";

	private static final String ERR_LOG_SUFFIX = ".err";

	private static final String COMMAND_MESSAGE_PREFIX = "Running: ";



	@Parameter(defaultValue = "${project.build.directory}/cordova", required = true, readonly = true)
	private File cordovaDirectory;

	@Parameter(defaultValue = "${project.basedir}/src/main/webapp", required = true, readonly = true)
	private String defaultFileSet;

	@Parameter(property = "project", defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "name", defaultValue = "${project.name}", required = true)
	private String name;

	@Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/cordova", required = true)
	private File outputDirectory;

	@Parameter(property = "fileSets")
	private List<FileSet> fileSets;

	@Parameter(property = "icon")
	private String icon;

	@Parameter(property = "splash")
	private String splash;

	@Parameter(property = "failOnError")
	private boolean failOnError = false;

	@Parameter(property = "platforms")
	private List<String> platforms;

	@Parameter(property = "plugins")
	private List<String> plugins;

	private final Log log = getLog();



	public void setProject(MavenProject project) {
		this.project = project;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public void setFileSets(List<FileSet> fileSets) {
		this.fileSets = fileSets;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public void setSplash(String splash) {
		this.splash = splash;
	}

	public void setPlatforms(List<String> platforms) {
		this.platforms = platforms;
	}

	public void setPlugins(List<String> plugins) {
		this.plugins = plugins;
	}

	public boolean isFailOnError() {
		return this.failOnError;
	}

	public void setIsFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	protected MavenProject getProject() {
		return project;
	}

	protected String getName() {
		return name;
	}

	public String getEscapedName() {
		return getName().replaceAll("\\s", "_");
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	List<FileSet> getFileSets() {
		if(fileSets == null || fileSets.isEmpty()) {
			FileSet r = new FileSet();
			r.setDirectory(defaultFileSet);
			fileSets = Collections.singletonList(r);
		}
		return fileSets;
	}

	public String getIcon() {
		return icon;
	}

	public String getSplash() {
		return splash;
	}

	List<String> getPlatforms() {
		return platforms;
	}

	List<String> getPlugins() {
		return plugins;
	}

	File getCordovaDirectory() {
		cordovaDirectory.mkdirs();
		return cordovaDirectory;
	}

	public File getResourcesDirectory() {
		return new File(getCordovaDirectory(), RESOURCES_DIRECTORY);
	}

	void run(ProcessBuilder processBuilder, String goal) throws MojoExecutionException {
		run(processBuilder, goal, failOnError);
	}

	public void run(ProcessBuilder processBuilder, String goal, boolean failOnError) throws MojoExecutionException {
		final File out = new File(getLogsDirectory(), goal + OUT_LOG_SUFFIX);
		final File err = new File(getLogsDirectory(), goal + ERR_LOG_SUFFIX);

		try {
			if (System.getenv() != null)
				processBuilder.environment().putAll(System.getenv());

			notifyError(logCommand(processBuilder)
					.redirectOutput(out.exists() ? ProcessBuilder.Redirect.appendTo(out) : ProcessBuilder.Redirect.to(out))
					.redirectError(err.exists() ? ProcessBuilder.Redirect.appendTo(err) : ProcessBuilder.Redirect.to(err))
					.start().waitFor(), goal, failOnError);
		} catch (IOException | InterruptedException e) {
			if(failOnError)
				throw new MojoExecutionException(new StringBuilder("Failed to execute ")
						.append(goal).append(" goal.").toString(), e);
			else log.warn("Could not run process: " + processBuilder);
		}
	}



	private File getLogsDirectory() {
		File logsDirectory = new File(getCordovaDirectory(), LOGS_DIRECTORY);
		logsDirectory.mkdirs();
		return logsDirectory;
	}

	private ProcessBuilder logCommand(ProcessBuilder processBuilder) {
		if(log.isInfoEnabled()) {
			StringBuilder sb = new StringBuilder(COMMAND_MESSAGE_PREFIX);
			for(String s : processBuilder.command()) sb.append(' ').append(s);
			log.info(sb);
		}
		return processBuilder;
	}

	private void notifyError(int errorCode, String goal, boolean failOnError) throws MojoExecutionException {
		if(errorCode != 0) {
			String message = new StringBuilder(failOnError ? "Failed to execute " : "An error occurred in the ")
					.append(goal).append(" goal. Details of the error can be found at ")
					.append(new File(getLogsDirectory(), goal + ERR_LOG_SUFFIX).getAbsolutePath()).toString();
			if(failOnError) throw new MojoExecutionException(message);
			else log.error(message);
		}
	}
}
