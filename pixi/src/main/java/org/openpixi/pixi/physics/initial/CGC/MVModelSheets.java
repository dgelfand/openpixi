package org.openpixi.pixi.physics.initial.CGC;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.openpixi.pixi.math.AlgebraElement;
import org.openpixi.pixi.math.GroupElement;
import org.openpixi.pixi.physics.Simulation;
import org.openpixi.pixi.physics.util.GridFunctions;

import java.util.Random;

public class MVModelSheets implements IInitialChargeDensity {

	/**
	 * Charge density as an array of AlgebraElements.
	 */
	private AlgebraElement[] rho;

	/**
	 * Transverse Wilson lines as an array of GroupElements.
	 */
	private GroupElement[] V;

	/**
	 * CGC Poisson solver
	 */
	private LightConePoissonSolverImprovedFull poisson;

	/**
	 * Direction of movement of the charge density. Values range from 0 to numberOfDimensions-1.
	 */
	private int direction;

	/**
	 * Orientation of movement. Values are -1 or 1.
	 */
	private int orientation;

	/**
	 * Longitudinal location of the initial charge density in the simulation box.
	 */
	private double location;

	/**
	 * Longitudinal width of the charge density.
	 */
	private double longitudinalWidth;

	/**
	 * Number of sheets
	 */
	public int numSheets;

	/**
	 * \mu parameter of the MV model.
	 */
	private double mu;

	/**
	 * Seed for the random variables.
	 */
	private boolean useSeed = false;
	private int seed;

	/**
	 * Coefficient used for the transverse UV regulator, which is implemented as a hard cutoff. This parameter is given
	 * in units of inverse lattice spacings. A value of sqrt(2)*PI corresponds to no UV cutoff. A value of 0.0 cuts off
	 * all modes in momentum space.
	 */
	private double ultravioletCutoffTransverse;

	/**
	 * Coefficient used for the longitudinal UV regulator, which is implemented as a hard cutoff. This parameter is
	 * given in units of inverse lattice spacings. A value of sqrt(2)*PI corresponds to no UV cutoff. A value of 0.0
	 * cuts off all modes in momentum space.
	 */
	private double ultravioletCutoffLongitudinal;

	/**
	 * Coefficient used for the IR regulator, which is implemented as a mass-term in the Poisson solver. As with the
	 * UV regulator this coefficient is given in units of inverse lattice spacings. A value of 0.0 removes the IR
	 * regulator, any other value leads to a finite mass term in the Poisson equation.
	 */
	private double infraredCoefficient;

	/**
	 * This class implements the color charge density of the MV model with coherent longitudinal structure. The fields
	 * are regulated in Fourier space with a hard UV cutoff in the transverse and longitudinal directions, but the
	 * longitudinal cutoff should not have any effect. The IR modes are regulated in the transverse plane with a
	 * 'gluon mass' term.
	 *
	 * @param direction                     index of the longitudinal direction
	 * @param orientation                   orientation of movement in the longitudinal direction
	 * @param location                      longitudinal position
	 * @param longitudinalWidth             longitudinal width of the MV model
	 * @param mu                            MV model parameter
	 * @param useSeed                       use a fixed seed for random number generation
	 * @param seed                          seed of the random number generator
	 * @param ultravioletCutoffTransverse   UV cutoff in transverse plane (in inverse lattice spacings)
	 * @param ultravioletCutoffLongitudinal UV cutoff in the longitudinal direction (in inverse lattice spacings)
	 * @param infraredCoefficient           IR regulator coefficient in the transverse plane
	 */
	public MVModelSheets(int direction, int orientation, double location, double longitudinalWidth, double mu,
						 boolean useSeed, int seed,
						 double ultravioletCutoffTransverse, double ultravioletCutoffLongitudinal,
						 double infraredCoefficient, int numSheets) {

		this.direction = direction;
		this.orientation = orientation;
		this.location = location;
		this.longitudinalWidth = longitudinalWidth;
		this.numSheets = numSheets;
		this.mu = mu;
		this.useSeed = useSeed;
		this.seed = seed;
		this.ultravioletCutoffTransverse = ultravioletCutoffTransverse;
		this.ultravioletCutoffLongitudinal = ultravioletCutoffLongitudinal;
		this.infraredCoefficient = infraredCoefficient;
	}

	public void initialize(Simulation s) {
		int totalCells = s.grid.getTotalNumberOfCells();
		int numberOfColors = s.getNumberOfColors();
		int numberOfComponents = (numberOfColors > 1) ? numberOfColors * numberOfColors - 1 : 1;
		int[] transNumCells = GridFunctions.reduceGridPos(s.grid.getNumCells(), direction);
		int totalTransCells = GridFunctions.getTotalNumberOfCells(transNumCells);
		int longitudinalNumCells = s.grid.getNumCells(direction);

		this.rho = new AlgebraElement[totalCells];
		for (int i = 0; i < s.grid.getTotalNumberOfCells(); i++) {
			this.rho[i] = s.grid.getElementFactory().algebraZero();
		}
		double[] tempRho = new double[s.grid.getTotalNumberOfCells()];

		this.V = new GroupElement[totalTransCells];
		for (int i = 0; i < totalTransCells; i++) {
			this.V[i] = s.grid.getElementFactory().groupIdentity();
		}

		Random rand = new Random();
		if (useSeed) {
			rand.setSeed(seed);
		}

		poisson.initialize(s);

		// Longitudinal and transverse lattice spacing
		double aL = s.grid.getLatticeSpacing(direction);
		double aT = s.grid.getLatticeSpacing((direction + 1) % s.getNumberOfDimensions());

		for (int n = 0; n < numSheets; n++) {
			for (int j = 0; j < numberOfComponents; j++) {

				// Place random charges on the grid (with coherent longitudinal structure).
				for (int i = 0; i < totalTransCells; i++) {
					double charge = rand.nextGaussian() * mu * s.getCouplingConstant() / aT / numSheets;
					int[] transPos = GridFunctions.getCellPos(i, transNumCells);
					for (int k = 0; k < longitudinalNumCells; k++) {
						int[] gridPos = GridFunctions.insertGridPos(transPos, direction, k);
						int index = s.grid.getCellIndex(gridPos);
						tempRho[index] = charge;
					}
				}

				// Apply hard momentum regulation in Fourier space.
				tempRho = FourierFunctions.regulateChargeDensityHard(tempRho, s.grid.getNumCells(),
						ultravioletCutoffTransverse, ultravioletCutoffLongitudinal, infraredCoefficient, direction,
						aT, aL);

				for (int i = 0; i < s.grid.getTotalNumberOfCells(); i++) {
					this.rho[i].set(j, tempRho[i]);
				}
			}

			poisson.solve(this);

			GroupElement[] wilsonLines = poisson.getV();

			for (int i = 0; i < totalTransCells; i++) {
				V[i].multAssign(wilsonLines[i]);
			}
		}

		AlgebraElement[] phi = new AlgebraElement[totalTransCells];
		for (int i = 0; i < totalTransCells; i++) {
			phi[i] = s.grid.getElementFactory().algebraZero();
		}
		for (int i = 0; i < totalTransCells; i++) {
			phi[i].set(V[i].getAlgebraElement());
		}

		for (int j = 0; j < numberOfComponents; j++) {

			for (int i = 0; i < totalTransCells; i++) {
				int[] transPos = GridFunctions.getCellPos(i, transNumCells);
				for (int k = 0; k < longitudinalNumCells; k++) {
					int[] gridPos = GridFunctions.insertGridPos(transPos, direction, k);
					int index = s.grid.getCellIndex(gridPos);
					tempRho[index] = phi[i].get(j);
				}
			}

			// Compute charge density from a solution to an inverse Poisson equation.
			tempRho = FourierFunctions.solveInversePoisson(tempRho, s.grid.getNumCells(), direction, aT);

			// Apply longitudinal profile.
			Gaussian gauss = new Gaussian(location, longitudinalWidth);
			for (int i = 0; i < s.grid.getTotalNumberOfCells(); i++) {
				int[] pos = s.grid.getCellPos(i);
				double longPos = pos[direction] * s.grid.getLatticeSpacing(direction);
				double profile = gauss.value(longPos);
				tempRho[i] *= profile;
			}

			/*
			 Put everything into rho array, but exclude charges that lie outside of a simulation box centered around the
			 longitudinal location of the MV model.
			  */
			double simulationBoxWidth = s.grid.getNumCells(direction) * s.grid.getLatticeSpacing(direction);
			double zmin = Math.max(this.location - simulationBoxWidth / 2.0, 0.0);
			double zmax = Math.min(this.location + simulationBoxWidth / 2.0, simulationBoxWidth);
			int lmin = (int) Math.floor(zmin / s.grid.getLatticeSpacing(direction));
			int lmax = (int) Math.ceil(zmax / s.grid.getLatticeSpacing(direction));
			for (int i = 0; i < s.grid.getTotalNumberOfCells(); i++) {
				int longPos = s.grid.getCellPos(i)[direction];
				if (lmin < longPos && longPos < lmax && s.grid.isActive(i)) {
					this.rho[i].set(j, tempRho[i]);
				}
			}
		}

		// Done!
	}

	public AlgebraElement getChargeDensity(int index) {
		return rho[index];
	}

	public AlgebraElement[] getChargeDensity() {
		return rho;
	}

	public int getDirection() {
		return direction;
	}

	public int getOrientation() {
		return orientation;
	}

	public String getInfo() {
		/*
			mu   ... MV model parameter
			w    ... longitudinal width
			UVT  ... transverse UV cutoff
			m    ... IR regulator
		 */
		return String.format("MV coherent, mu: %f, w: %f, UVT: %f, m: %f",
				mu, longitudinalWidth, ultravioletCutoffTransverse, infraredCoefficient);
	}

	public void clear() {
		this.rho = null;
	}
}


