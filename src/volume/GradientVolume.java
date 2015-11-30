/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

/**
 *
 * @author michel
 */
public class GradientVolume {

    public GradientVolume(Volume vol) {
        volume = vol;
        dimX = vol.getDimX();
        dimY = vol.getDimY();
        dimZ = vol.getDimZ();
        data = new VoxelGradient[dimX * dimY * dimZ];
        compute();
        maxmag = -1.0;
    }

    public VoxelGradient getGradient(int x, int y, int z) {
        return data[x + dimX * (y + dimY * z)];
    }

    
    public void setGradient(int x, int y, int z, VoxelGradient value) {
        data[x + dimX * (y + dimY * z)] = value;
    }

    public void setVoxel(int i, VoxelGradient value) {
        data[i] = value;
    }

    public VoxelGradient getVoxel(int i) {
        return data[i];
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getDimZ() {
        return dimZ;
    }

    private void compute() {

        float gx, gy, gz;
        for(int x = 0; x < dimX; ++x)
            for(int y = 0; y < dimY; ++y)
                for(int z = 0; z < dimZ; ++z){
                    
                    if (x == 0 || y == 0 || z == 0 || x == dimX-1 || y == dimY-1 || z == dimZ-1){
                        // for simiplicity, boundaries have gradient zero
                        data[x + dimX * (y + dimY * z)] = new VoxelGradient(0, 0, 0);
                    }
                    else {
                        // https://dlwpswbsp.tue.nl/120-2015/2e6a8659155e485da8ef413b5a9cab63/Documents/2-spatial.pdf slide 24
                        gx = volume.getVoxel(x + 1, y, z) - volume.getVoxel(x - 1, y, z);
                        gy = volume.getVoxel(x, y + 1, z) - volume.getVoxel(x, y - 1, z);
                        gz = volume.getVoxel(x, y, z + 1) - volume.getVoxel(x, y, z - 1);

                        data[x + dimX * (y + dimY * z)] = new VoxelGradient(gx / 2, gy / 2, gz / 2);
                    }
                }
    }
    
    public double getMaxGradientMagnitude() {
        if (maxmag >= 0) {
            return maxmag;
        } else {
            double magnitude = data[0].mag;
            for (int i=0; i<data.length; i++) {
                magnitude = data[i].mag > magnitude ? data[i].mag : magnitude;
            }   
            maxmag = magnitude;
            return magnitude;
        }
    }
    
    private int dimX, dimY, dimZ;
    private VoxelGradient zero = new VoxelGradient();
    VoxelGradient[] data;
    Volume volume;
    double maxmag;
}
