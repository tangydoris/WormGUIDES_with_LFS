package wormguides.model;

public class Quaternion {

	private double w, x, y, z;
	private int renormalize_count;

	/**
	 * Initial quaternion will be <1,0,0,0> i.e. no rotation
	 */
	public Quaternion() {
		this.w = 1.0;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;

		renormalize_count = 0;
	}

	/**
	 * Used only locally to compute the 'local_rotation'
	 * 
	 * @param w
	 * @param x
	 * @param y
	 * @param z
	 */
	public Quaternion(double w, double x, double y, double z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;

		renormalize_count = 0;
	}

	public double getW() {
		return w;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	/**
	 * Converts current local quaternion paramters to intrinsic Euler angles
	 * with rotations about z, y', x''.<br>
	 * heading = z-axis<br>
	 * attitude = y'-axis<br>
	 * bank = x''-axis<br>
	 * Conversion:
	 * https://en.wikipedia.org/wiki/Rotation_formalisms_in_three_dimensions#
	 * Quaternion_.E2.86.92_Euler_angles_.28z-x-z_extrinsic.29<br>
	 * Algorithm:
	 * http://www.euclideanspace.com/maths/geometry/rotations/conversions/
	 * quaternionToEuler/<br>
	 * Tait-Bryan Euler Angles:
	 * https://en.wikipedia.org/wiki/Euler_angles#Tait.E2.80.93Bryan_angles
	 */
	public double[] convertToIntrinsicEuler() {
		double heading, attitude, bank;
		double sqw = w * w;
		double sqx = x * x;
		double sqy = y * y;
		double sqz = z * z;
		double unit = sqx + sqy + sqz + sqw; // is one if normalised, otherwise
												// is correction factor

		double test = (x * y) + (z * w);
		if (test > NORTH_POLE * unit) { // singularity at north pole
			heading = 2 * Math.atan2(x, w);
			attitude = Math.PI / 2;
			bank = 0;
			return new double[] { heading, attitude, bank };
		}
		if (test < SOUTH_POLE * unit) { // singularity at south pole
			heading = -2 * Math.atan2(x, w);
			attitude = -Math.PI / 2;
			bank = 0;
			return new double[] { heading, attitude, bank };
		}

		heading = Math.atan2((2 * y * w) - (2 * x * z), sqx - sqy - sqz + sqw);
		attitude = Math.asin(2 * test / unit);
		bank = Math.atan2((2 * x * w) - (2 * y * z), -sqx + sqy - sqz + sqw);
		return new double[] { heading, attitude, bank };
	}

	public double tb_project_to_sphere(double r, double x, double y) {
		double d, t, z;

		double sqrt2 = Math.sqrt(2);
		d = Math.sqrt(x * x + y * y);
		if (d < r * (sqrt2 / 2)) { /* Inside sphere */
			z = Math.sqrt(r * r - d * d);
		} else { /* On hyperbola */
			t = r / sqrt2;
			z = t * t / d;
		}
		return z;
	}

	private void vcross(double v1[], double v2[], double dest[]) {
		double v10 = v1[0];
		double v11 = v1[1];
		double v12 = v1[2];
		double v20 = v2[0];
		double v21 = v2[1];
		double v22 = v2[2];

		dest[0] = (v11 * v22) - (v12 * v21);
		dest[1] = (v12 * v20) - (v10 * v22);
		dest[2] = (v10 * v21) - (v11 * v20);
	}

	private double vdot(double v1[], double v2[]) {
		return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
	}

	private void vsub(double p1[], double p2[], double dest[]) {
		dest[0] = p1[0] - p2[0];
		dest[1] = p1[1] - p2[1];
		dest[2] = p1[2] - p2[2];
	}

	private void vadd(double p1[], double p2[], double dest[]) {
		dest[0] = p1[0] + p2[0];
		dest[1] = p1[1] + p2[1];
		dest[2] = p1[2] + p2[2];
	}

	private void vnormal(double v[]) {
		vscale(v, 1.0 / vlength(v));
	}

	private double vlength(double v[]) {
		double v0 = v[0];
		double v1 = v[1];
		double v2 = v[2];
		return Math.sqrt(v0 * v0 + v1 * v1 + v2 * v2);
	}

	private void vscale(double v[], double div) {
		v[0] = v[0] * div;
		v[1] = v[1] * div;
		v[2] = v[2] * div;
	}

	private void vcopy(double v[], double d[]) {
		d[0] = v[0];
		d[1] = v[1];
		d[2] = v[2];
	}

	private void vset(double dest[], double x, double y, double z) {
		dest[0] = x;
		dest[1] = y;
		dest[2] = z;
	}

	/**
	 * Given an axis and angle, compute quaternion.
	 */
	private void gfs_gl_axis_to_quat(double a[], double phi, double destQ[]) {
		vnormal(a);
		vcopy(a, destQ);
		vscale(destQ, Math.sin(phi / 2.0));
		destQ[3] = Math.cos(phi / 2.0);
	}

	/**
	 * Computes the result of of this local quaternion and the input quaternion,
	 * then stuffs it into this local one.
	 * 
	 * @param q2
	 *            Quaternion array [x, y, z, w] to append to the local
	 *            quaternion.
	 */
	public void gfs_gl_add_quat(double q2[]) {
		double q1[] = new double[] { x, y, z, w };

		double t1[], t2[], t3[];
		double tf[];
		t1 = new double[4];
		t2 = new double[4];
		t3 = new double[4];
		tf = new double[4];

		vcopy(q1, t1);
		vscale(t1, q2[3]);

		vcopy(q2, t2);
		vscale(t2, q1[3]);

		vcross(q2, q1, t3);
		vadd(t1, t2, tf);
		vadd(t3, tf, tf);
		tf[3] = q1[3] * q2[3] - vdot(q1, q2);

		x = tf[0];
		y = tf[1];
		z = tf[2];
		w = tf[3];

		if (++renormalize_count > RENORMALIZE_COUNT) {
			renormalize_count = 0;
			normalize_quat(q1);
			x = q1[0];
			y = q1[1];
			z = q1[2];
			w = q1[3];
		}
	}

	/**
	 * Simulate a track-ball. Project the points onto the virtual trackball,
	 * then figure out the axis of rotation, which is the cross product of P1 P2
	 * and O P1 (O is the center of the ball, 0,0,0) Note: This is a deformed
	 * trackball-- is a trackball in the center, but is deformed into a
	 * hyperbolic sheet of rotation away from the center. This particular
	 * function was chosen after trying out several variations.
	 *
	 * It is assumed that the arguments to this routine are in the range (-1.0
	 * ... 1.0)
	 */
	public double[] gfs_gl_trackball(double p1x, double p1y, double p2x, double p2y) {
		double q[] = new double[] { 0.0, 0.0, 0.0, 1.0 };
		double a[] = new double[3];
		double phi;
		double p1[] = new double[3];
		double p2[] = new double[3];
		double d[] = new double[3];
		double t;

		if (p1x == p2x && p1y == p2y) {
			// zero rotation
			return q;
		}

		/*
		 * First, figure out z-coordinates for projection of P1 and P2 to
		 * deformed sphere
		 */
		vset(p1, p1x, p1y, tb_project_to_sphere(TRACKBALL_SIZE, p1x, p1y));
		vset(p2, p2x, p2y, tb_project_to_sphere(TRACKBALL_SIZE, p2x, p2y));

		/*
		 * Now, we want the cross product of P1 and P2
		 */
		vcross(p2, p1, a);

		/*
		 * Figure out how much to rotate around that axis
		 */
		vsub(p1, p2, d);
		t = vlength(d) / (2.0 * TRACKBALL_SIZE);

		/*
		 * Avoid problems with out-of-control values...
		 */
		if (t > 1.0)
			t = 1.0;
		if (t < -1.0)
			t = -1.0;
		phi = 2.0 * Math.asin(t);

		gfs_gl_axis_to_quat(a, phi, q);
		return q;
	}

	private void normalize_quat(double q[]) {
		int i;
		double mag;

		mag = (q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
		for (i = 0; i < 4; i++)
			q[i] /= mag;
	}

	private final static double NORTH_POLE = 0.4999;
	private final static double SOUTH_POLE = -0.4999;

	private final double TRACKBALL_SIZE = 0.8;
	private final int RENORMALIZE_COUNT = 97;
}