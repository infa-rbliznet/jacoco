package org.jacoco.cli.internal.commands;

import java.io.BufferedReader;

/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

/**
 * Formatter for coverage reports in multiple HTML pages.
 */
public class IncrementalVisitor implements IReportVisitor {

	private final ISourceFileLocator oldSourceLocator;

	/**
	 * @param oldSourceLocator
	 */
	public IncrementalVisitor(final ISourceFileLocator oldSourceLocator) {
		super();
		this.oldSourceLocator = oldSourceLocator;
	}

	public void visitInfo(final List<SessionInfo> sessionInfos,
			final Collection<ExecutionData> executionData) throws IOException {
	}

	public void visitBundle(final IBundleCoverage bundle,
			final ISourceFileLocator locator) throws IOException {
		float linesCovered = 0;
		int totalNumberOfLinesChanged = 0;
		for (final IPackageCoverage pkg : bundle.getPackages()) {
			for (final ISourceFileCoverage source : pkg.getSourceFiles()) {
				if (!source.containsCode()) {
					continue;
				}
				final String sourcename = source.getName();
				final String packagename = pkg.getName();
				final Reader reader = locator.getSourceFile(packagename,
						sourcename);

				final Reader oldFileReader = oldSourceLocator
						.getSourceFile(packagename, sourcename);

				if (reader != null) {
					final List<String> newSourceFile = readAllLines(reader);
					final List<String> oldSourceFile;

					if (oldFileReader != null) {
						oldSourceFile = readAllLines(oldFileReader);
						final Patch<String> changed = DiffUtils
								.diff(oldSourceFile, newSourceFile);
						for (final AbstractDelta<String> d : changed
								.getDeltas()) {
							final int startLine = d.getTarget().getPosition();
							final int lineCount = d.getTarget().getLines()
									.size();
							for (int i = startLine; i < startLine
									+ lineCount; i++) {
								final ILine iLine = source.getLine(i);
								final float percent = getCoveragePercent(iLine);
								linesCovered += percent;
								totalNumberOfLinesChanged++;
							}

						}
					} else {
						for (int i = 0; i < newSourceFile.size(); i++) {
							final ILine iLine = source.getLine(i);
							final float percent = getCoveragePercent(iLine);
							linesCovered += percent;
							totalNumberOfLinesChanged++;
						}
					}

				}

			}

//			for (final IClassCoverage clazz : pkg.getClasses()) {
//				final String fileName = clazz.getSourceFileName();
//				for (final IMethodCoverage mthd : clazz.getMethods()) {
//
//				}
//			}
		}
		final float percent = totalNumberOfLinesChanged == 0 ? 100
				: (linesCovered / totalNumberOfLinesChanged);
		System.out.println(
				"Total Number of lines changed: " + totalNumberOfLinesChanged
						+ ", coverage percent: " + percent * 100);
	}

	private float getCoveragePercent(final ILine iLine) {
		float percent = 0;
		switch (iLine.getStatus()) {
		case ICounter.NOT_COVERED:
			percent = 0f;
			break;
		case ICounter.FULLY_COVERED:
			percent = 1f;
			break;
		case ICounter.PARTLY_COVERED:
			percent = 0.5f;
			break;
		}
		return percent;
	}

	List<String> readAllLines(final Reader reader) throws IOException {
		final BufferedReader lineBuffer = new BufferedReader(reader);
		String line;
		final List<String> result = new ArrayList<String>();
		int nr = 0;
		while ((line = lineBuffer.readLine()) != null) {
			nr++;
			result.add(line);
		}
		return result;
	}

	public IReportGroupVisitor visitGroup(final String name)
			throws IOException {
		return null;

	}

	public void visitEnd() throws IOException {
	}

}
