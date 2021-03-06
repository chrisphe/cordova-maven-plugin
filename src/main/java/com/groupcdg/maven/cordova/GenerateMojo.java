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

import com.groupcdg.maven.cordova.platform.Platform;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.groupcdg.maven.cordova.platform.Platform.OS;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.filefilter.FileFilterUtils.*;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateMojo extends AbstractCordovaMojo {


	private static final String GENERATE = "generate";

	private static final String CREATE = "create";

	private static final String PLUGIN = "plugin";

	private static final String ADD = "add";

	private static final String GENERATE_RESOURCES_ERROR_MESSAGE = "Failed to generate resources";



	public void execute() throws MojoExecutionException {
		try {
			final File outputDirectory = getOutputDirectory();

			if(outputDirectory.exists())
				FileUtils.deleteDirectory(outputDirectory);
			if (!outputDirectory.mkdirs())
				throw new MojoExecutionException( CREATE_DIRECTORY_ERROR_MESSAGE + outputDirectory.getAbsolutePath());

			create(outputDirectory, prepare());
			addPlatforms(outputDirectory);
			addPlugins(outputDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException(GENERATE_RESOURCES_ERROR_MESSAGE, e);
		}
	}



	private File prepare() throws IOException {
		File resourcesDirectory = getResourcesDirectory();
		resourcesDirectory.mkdir();

		for(FileSet fileSet : getFileSets())
			copyDirectory(new File(fileSet.getDirectory()), resourcesDirectory, createFilter(fileSet));

		return resourcesDirectory;
	}

	private void create(final File outputDirectory, final File resourcesDirectory) throws MojoExecutionException {
		run(new ProcessBuilder(OS.system().cordova(CREATE, outputDirectory.getAbsolutePath(),
				getProject().getGroupId() + '.' + getProject().getArtifactId(),
				getEscapedName(), "--src=" + resourcesDirectory.getAbsolutePath())), GENERATE);
	}

	private void addPlatforms(final File outputDirectory) throws MojoExecutionException {
		final OS system = OS.system();
		for(Platform platform : system.platforms(getPlatforms())) {
			run(new ProcessBuilder(platform.addCommand(system)).directory(outputDirectory), GENERATE, false);
		}
	}

	private void addPlugins(final File outputDirectory) throws MojoExecutionException {
		final OS system = OS.system();
		for(String plugin : getPlugins()) {
			run(new ProcessBuilder(system.cordova(PLUGIN, ADD, plugin)).directory(outputDirectory), GENERATE);
		}
	}

	private IOFileFilter createFilter(final FileSet fileSet) {
		List<IOFileFilter> includes = new ArrayList<IOFileFilter>();
		for(String inc : fileSet.getIncludes()) { includes.add(new WildcardFileFilter(inc)); }
		
		List<IOFileFilter> excludes = new ArrayList<IOFileFilter>();
		for(String ex : fileSet.getExcludes()) { excludes.add(new WildcardFileFilter(ex)); }

		IOFileFilter include = includes.isEmpty() ? trueFileFilter()
				: or(directoryFileFilter(), or(includes.toArray(new IOFileFilter[includes.size()])));
		return excludes.isEmpty() ? include
				: and(include, notFileFilter(or(excludes.toArray(new IOFileFilter[excludes.size()]))));
	}
}
