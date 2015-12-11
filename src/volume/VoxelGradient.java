/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

import util.VectorMath;

/**
 *
 * @author michel
 */
public class VoxelGradient {

    public float x, y, z;
    public float mag;
    //public double[] N;
    
    public VoxelGradient() {
        x = y = z = mag = 0.0f;
    }
    
    public VoxelGradient(float gx, float gy, float gz) {
        x = gx;
        y = gy;
        z = gz;
        mag = (float) Math.sqrt(x*x + y*y + z*z);
      /*  N = new double[3];
        
        if (mag < 0.0001)
            VectorMath.setVector(N, 0, 0, 0);
        else{
            //VectorMath.setVector(N, gx / mag, gy / mag, gz / mag);
            N[0] = gx/mag;
            N[1] = gy/mag;
            N[2] = gz/mag;
        }*/
    }
    
    public double[] getNormalizedGradient(){
        if (mag > 1)
            return new double[] {x/mag, y/mag, z/mag};
        return new double[] {0, 0, 0};
    }
    
}
