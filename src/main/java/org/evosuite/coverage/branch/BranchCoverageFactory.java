/**
 * Copyright (C) 2011,2012 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * 
 * This file is part of EvoSuite.
 * 
 * EvoSuite is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * EvoSuite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Public License for more details.
 * 
 * You should have received a copy of the GNU Public License along with
 * EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage.branch;

import java.util.ArrayList;
import java.util.List;

import org.evosuite.Properties;
import org.evosuite.coverage.lcsaj.LCSAJPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * BranchCoverageFactory class.
 * </p>
 * 
 * @author Gordon Fraser, Andre Mis
 */
public class BranchCoverageFactory extends AbstractFitnessFactory {

	private static Logger logger = LoggerFactory.getLogger(BranchCoverageFactory.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.evosuite.coverage.TestCoverageFactory#getCoverageGoals()
	 */
	/** {@inheritDoc} */
	@Override
	public List<TestFitnessFunction> getCoverageGoals() {
		return getCoverageGoals(Properties.TARGET_METHOD);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.evosuite.coverage.TestCoverageFactory#getCoverageGoals()
	 */
	/**
	 * <p>
	 * getCoverageGoals
	 * </p>
	 * 
	 * @param targetMethod
	 *            a {@link java.lang.String} object.
	 * @return a {@link java.util.List} object.
	 */
	public List<TestFitnessFunction> getCoverageGoals(String targetMethod) {
		long start = System.currentTimeMillis();
		List<TestFitnessFunction> goals = new ArrayList<TestFitnessFunction>();

		// logger.info("Getting branches");
		for (String className : BranchPool.knownClasses()) {
			/*
			boolean classNameMatches = className.equals(Properties.TARGET_CLASS);

			if (!classNameMatches && !Properties.TARGET_CLASS_PREFIX.isEmpty()) {
				classNameMatches |= className.startsWith(Properties.TARGET_CLASS_PREFIX);
			}
			if (!classNameMatches && className.startsWith(Properties.TARGET_CLASS + "$")) {
				classNameMatches = true;
			}

			if (!classNameMatches)
				continue;
				*/

			// Branchless methods
			for (String method : BranchPool.getBranchlessMethods(className)) {
				if (targetMethod.isEmpty() || method.endsWith(targetMethod)) {
					goals.add(createRootBranchTestFitness(className, method));
				}
			}

			// Branches
			for (String methodName : BranchPool.knownMethods(className)) {
				if (!targetMethod.equals("") && !methodName.equals(targetMethod)) {
					logger.info("Method " + methodName + " does not equal target method "
					        + targetMethod);
					continue;
				}

				for (Branch b : BranchPool.retrieveBranchesInMethod(className, methodName)) {
					if (!(b.getInstruction().isForcedBranch() || LCSAJPool.isLCSAJBranch(b))) {
						goals.add(createBranchCoverageTestFitness(b, true));
						//if (!b.isSwitchCaseBranch())
						goals.add(createBranchCoverageTestFitness(b, false));
					}
				}
			}
		}
		goalComputationTime = System.currentTimeMillis() - start;
		return goals;
	}

	/**
	 * Create a fitness function for branch coverage aimed at executing the
	 * given ControlDependency.
	 * 
	 * @param cd
	 *            a {@link org.evosuite.graphs.cfg.ControlDependency} object.
	 * @return a {@link org.evosuite.coverage.branch.BranchCoverageTestFitness}
	 *         object.
	 */
	public static BranchCoverageTestFitness createBranchCoverageTestFitness(
	        ControlDependency cd) {
		return createBranchCoverageTestFitness(cd.getBranch(),
		                                       cd.getBranchExpressionValue());
	}

	/**
	 * Create a fitness function for branch coverage aimed at executing the
	 * Branch identified by b as defined by branchExpressionValue.
	 * 
	 * @param b
	 *            a {@link org.evosuite.coverage.branch.Branch} object.
	 * @param branchExpressionValue
	 *            a boolean.
	 * @return a {@link org.evosuite.coverage.branch.BranchCoverageTestFitness}
	 *         object.
	 */
	public static BranchCoverageTestFitness createBranchCoverageTestFitness(Branch b,
	        boolean branchExpressionValue) {

		return new BranchCoverageTestFitness(new BranchCoverageGoal(b,
		        branchExpressionValue, b.getClassName(), b.getMethodName()));
	}

	/**
	 * Create a fitness function for branch coverage aimed at covering the root
	 * branch of the given method in the given class. Covering a root branch
	 * means entering the method.
	 * 
	 * @param className
	 *            a {@link java.lang.String} object.
	 * @param method
	 *            a {@link java.lang.String} object.
	 * @return a {@link org.evosuite.coverage.branch.BranchCoverageTestFitness}
	 *         object.
	 */
	public static BranchCoverageTestFitness createRootBranchTestFitness(String className,
	        String method) {

		return new BranchCoverageTestFitness(new BranchCoverageGoal(className,
		        method.substring(method.lastIndexOf(".") + 1)));
	}

	/**
	 * Convenience method calling createRootBranchTestFitness(class,method) with
	 * the respective class and method of the given BytecodeInstruction.
	 * 
	 * @param instruction
	 *            a {@link org.evosuite.graphs.cfg.BytecodeInstruction} object.
	 * @return a {@link org.evosuite.coverage.branch.BranchCoverageTestFitness}
	 *         object.
	 */
	public static BranchCoverageTestFitness createRootBranchTestFitness(
	        BytecodeInstruction instruction) {
		if (instruction == null)
			throw new IllegalArgumentException("null given");

		return createRootBranchTestFitness(instruction.getClassName(),
		                                   instruction.getMethodName());
	}
}
