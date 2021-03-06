
package org.openpixi.pixi.opencl;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;
import org.openpixi.pixi.physics.Settings;
import org.openpixi.pixi.physics.Simulation;
import org.openpixi.pixi.physics.grid.ChargeConservingCIC;
import org.openpixi.pixi.physics.solver.BorisDamped;
import org.openpixi.pixi.physics.util.ClassCopier;
import org.openpixi.pixi.physics.util.ParticleComparator;

/**
 * Tests the OpenCL version of openpixi.
 * OpenCL and also sequential simulations and then compares the results.
 */
public class BorisDampedTest extends TestCase {

	public void testParallelSimulation() throws FileNotFoundException,IOException {
		Settings defaultSettings = new Settings();
		//defaultSettings.setTimeStep(0.1);
		defaultSettings.setGridCellsX(10);
		defaultSettings.setGridCellsY(10);
		defaultSettings.setNumOfParticles(100);
		defaultSettings.setIterations(100);
		defaultSettings.setParticleSolver(new BorisDamped());
                defaultSettings.setInterpolator(new ChargeConservingCIC());
                defaultSettings.setOCLParticleSolver("boris damped");
                defaultSettings.setOCLGridInterpolator("charge conserving CIC");
                
		Simulation sequentialSimulation = new Simulation(defaultSettings);

		Settings openCLSettings = ClassCopier.copy(defaultSettings);
		Simulation openCLSimulation = new Simulation(openCLSettings);

		sequentialSimulation.run();
		openCLSimulation.run();

		ParticleComparator comparator = new ParticleComparator();
		comparator.compare(sequentialSimulation.particles, openCLSimulation.particles);
	}
}
