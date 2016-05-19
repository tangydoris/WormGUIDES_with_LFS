package wormguides.model;

import java.util.ArrayList;
import java.util.Arrays;

public class Quaternion {
	private double w, x, y, z;

	/**
	 * Initial quaternion will be <1,0,0,0> i.e. no rotation
	 */
	public Quaternion() {
		this.w = 1.0;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
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
	}

	/**
	 * * Based on the angle of rotation, this method computes the local_rotation
	 * quaternion and then updates 'this' quaternion
	 * 
	 * Local rotation quaternion values: w = cos(angOfRotation/2) x = axis.x *
	 * sin(angOfRotation/2) y = axis.y * sin(angOfRotation/2) z = axis.z *
	 * sin(angOfRotation/2)
	 * 
	 * @param angOfRotation
	 *            - the angle of rotation
	 * @param axisX
	 *            - the x axis of the rotation direction
	 * @param axisY
	 *            - the y axis of the rotation direction
	 * @param axisZ
	 *            - the z axis of the rotation direction
	 */
	public void updateOnRotate(double angOfRotation, double axisX, double axisY, double axisZ) {
		// compute the local rotation quaternion
		double w, x, y, z;

		w = Math.cos(angOfRotation / 2.);
		x = (double) (axisX * Math.sin(angOfRotation / 2.));
		y = (double) (axisY * Math.sin(angOfRotation / 2.));
		z = (double) (axisZ * Math.sin(angOfRotation / 2.));

		Quaternion local_rotation = new Quaternion(w, x, y, z);

		multiplyQuaternions(local_rotation);
	}

	/**
	 * Performs the quaternion update: total = local_rotation * total
	 * 
	 * Quaternion multiplication rules, given Q1 and Q2:
	 * 
	 * (Q1 * Q2).w = (w1*w2 - x1*x2 - y1*y2 - z1*z2) (Q1 * Q2).x = (w1*x2 +
	 * x1*w2 + y1*z2 - z1*y2) (Q1 * Q2).y = (w1*y2 - x1*z2 + y1*w2 + z1*x2) (Q1
	 * * Q2).z = (w1*z2 + x1*y2 - y1*x2 + z1*w2)
	 * 
	 * @param local_rotation
	 */
	public void multiplyQuaternions(Quaternion local_rotation) {

		this.w = ((getW() * local_rotation.getW()) - (getX() * local_rotation.getX()) - (getY() * local_rotation.getY())
				- (getZ() * local_rotation.getZ()));

		this.x = ((getW() * local_rotation.getX()) + (getX() * local_rotation.getW()) + (getY() * local_rotation.getZ())
				- (getZ() * local_rotation.getY()));

		this.y = ((getW() * local_rotation.getY()) - (getX() * local_rotation.getZ()) + (getY() * local_rotation.getW())
				+ (getZ() * local_rotation.getX()));

		this.z = ((getW() * local_rotation.getZ()) + (getX() * local_rotation.getY()) - (getY() * local_rotation.getW())
				+ (getZ() * local_rotation.getW()));

		// normalize quaternion
		double magnitude = Math
				.sqrt(Math.pow(getW(), 2) + Math.pow(getX(), 2) + Math.pow(getY(), 2) + Math.pow(getZ(), 2));

		this.w = getW() / magnitude;
		this.x = getX() / magnitude;
		this.y = getY() / magnitude;
		this.z = getZ() / magnitude;
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
		// return y;
		return z;
	}

	/**
	 * Conversion from Quaternion to Euler
	 * 
	 * heading = y-axis attitude = z-axis bank = x-axis
	 * 
	 * heading = atan2(2*qy*(qw-2)*qx*qz , 1 - (2*(qy^2)) - 2*(qz^2)) attitude =
	 * asin(2*qx*qy + 2*qz*qw) bank = atan2(2*qx*(qw-2)*qy*qz , 1 - (2*(qx^2)) -
	 * 2*(qz^2))
	 * 
	 * Source:
	 * http://www.euclideanspace.com/maths/geometry/rotations/conversions/
	 * quaternionToEuler/
	 * 
	 * http://www.cprogramming.com/tutorial/3d/quaternions.html
	 * 
	 * @return the converted quaternion
	 */
	public ArrayList<Double> toEulerRotation() {
		ArrayList<Double> eulerRotation = new ArrayList<Double>();

		double heading, attitude, bank;
		heading = 0.;
		attitude = 0.;
		bank = 0.;

		/*
		 * check for cases north and south poles
		 * 
		 * North pole: x*y + z*w = 0.5 --> which gives heading = 2 * atan2(x, w)
		 * bank = 0
		 * 
		 * South pole: x*y + z*w = -0.5 --> which gives heading = -2 * atan2(x,
		 * w) bank = 0
		 */
		// TODO check if it should be greater or less than NORTH_POLE or
		// SOUTH_POLE
		double f = (x * y) + (z * w);
		if (f > NORTH_POLE) {
			heading = (double) (2 * Math.atan2(x, w));
			attitude = Math.PI / 2.;
			bank = 0.f;
		} else if (f < SOUTH_POLE) {
			heading = (double) (-2 * Math.atan2(x, w));
			attitude = -Math.PI / 2.;
			bank = 0;
		} else {
			double sqx = this.getX() * this.getX();
			double sqy = this.getY() * this.getY();
			double sqz = this.getZ() * this.getZ();

			heading = (double) Math.atan2((2 * y * (w - 2.) * x * z), (1 - (2 * sqy) - (2 * sqz)));
			attitude = (double) Math.asin(2 * f);
			bank = (double) Math.atan2((2 * x * (w - 2) * y * z), (1 - 2 * sqx - 2 * sqz));
		}

		eulerRotation.add(Math.toDegrees(heading));
		eulerRotation.add(Math.toDegrees(attitude));
		eulerRotation.add(Math.toDegrees(bank));

		return eulerRotation;
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

	private void vcopy(double v1[], double v2[]) {
		for (int i = 0; i < 3; i++)
			v2[i] = v1[i];
	}

	public double[] vcross(double v1[], double v2[]) {
		double cross[] = new double[3];
		double v10 = v1[0];
		double v11 = v1[1];
		double v12 = v1[2];
		double v20 = v2[0];
		double v21 = v2[1];
		double v22 = v2[2];
		cross[0] = (v11 * v22) - (v12 * v21);
		cross[1] = (v12 * v20) - (v10 * v22);
		cross[2] = (v10 * v21) - (v11 * v20);
		return cross;
	}

	private double[] vsub(double p1[], double p2[]) {
		double d[] = new double[3];
		d[0] = p1[0] - p2[0];
		d[1] = p1[1] - p2[1];
		d[2] = p1[2] - p2[2];
		return d;
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

	private double[] vcopy(double v[]) {
		return Arrays.copyOf(v, v.length);
	}

	/**
	 * Given an axis and angle, compute quaternion.
	 */
	private double[] gfs_gl_axis_to_quat(double a[], double phi) {
		double temp[] = new double[4];
		vnormal(a);
		vcopy(a, temp);
		vscale(temp, Math.sin(phi / 2.0));
		temp[3] = Math.cos(phi / 2.0);
		return temp;
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
		double a[];
		double phi;
		double p1[];
		double p2[];
		double d[];
		double t;

		if (p1x == p2x && p1y == p2y) {
			// zero rotation
			return q;
		}

		/*
		 * First, figure out z-coordinates for projection of P1 and P2 to
		 * deformed sphere
		 */
		p1 = new double[] { p1x, p1y, tb_project_to_sphere(TRACKBALL_SIZE, p1x, p1y) };
		p2 = new double[] { p2x, p2y, tb_project_to_sphere(TRACKBALL_SIZE, p2x, p2y) };

		/*
		 * Now, we want the cross product of P1 and P2
		 */
		a = vcross(p2, p1);

		/*
		 * Figure out how much to rotate around that axis
		 */
		d = vsub(p1, p2);
		t = vlength(d) / (2.0 * TRACKBALL_SIZE);

		/*
		 * Avoid problems with out-of-control values...
		 */
		if (t > 1.0)
			t = 1.0;
		if (t < -1.0)
			t = -1.0;
		phi = 2.0 * Math.asin(t);

		q = gfs_gl_axis_to_quat(a, phi);
		return q;
	}

	private void normalize_quat(double q[]) {
		int i;
		double mag;

		mag = (q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
		for (i = 0; i < 4; i++)
			q[i] /= mag;
	}

	// public ArrayList<Double> toEulerRotation() {
	// ArrayList<Double> eulerRotation = new ArrayList<Double>();
	//
	// double heading, attitude, bank;
	// heading = 0.;
	// attitude = 0.;
	// bank = 0.;
	//
	// double sqw = this.getW()*this.getW();
	// double sqx = this.getX()*this.getX();
	// double sqy = this.getY()*this.getY();
	// double sqz = this.getZ()*this.getZ();
	//
	// double unit = sqx + sqy + sqz + sqw;
	// double test = this.getX()*this.getY() + this.getZ()*this.getW();
	//
	// if (test > NORTH_POLE*unit) {
	// heading = 2*Math.atan2(this.getX(), this.getW());
	// attitude = Math.PI/2;
	// bank = 0;
	// } else if (test < SOUTH_POLE*unit) {
	// heading = -2*Math.atan2(this.getX(), this.getW());
	// attitude = -Math.PI/2;
	// bank = 0;
	// } else {
	// heading = Math.atan2(2*this.getY()*this.getW() -
	// 2*this.getX()*this.getZ(), sqx-sqy-sqz+sqw);
	// attitude = Math.asin(2*(test/unit));
	// bank = Math.atan2(2*this.getX()*this.getW() - 2*this.getY()*this.getZ(),
	// -sqx+sqy-sqz+sqw);
	// }
	//
	// eulerRotation.add(heading);
	// eulerRotation.add(attitude);
	// eulerRotation.add(bank);
	//
	// return eulerRotation;
	// }

	private final static double NORTH_POLE = 0.4999;
	private final static double SOUTH_POLE = -0.499;

	private final double TRACKBALL_SIZE = 0.1;
}