/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunction2DEditor;
import gui.TransferFunctionEditor;
import java.awt.image.BufferedImage;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import util.TFChangeListener;
import util.VectorMath;
import volume.GradientVolume;
import volume.Volume;
import volume.VoxelGradient;

/**
 *
 * @author michel
 */
public class RaycastRenderer extends Renderer implements TFChangeListener {

    private Volume volume = null;
    private GradientVolume gradients = null;
    RaycastRendererPanel panel;
    TransferFunction tFunc;
    TransferFunctionEditor tfEditor;
    TransferFunction2DEditor tfEditor2D;
    RenderingType renderingType = RenderingType.Slicer;
    private double max;
    private boolean interpolation;
    
    public RaycastRenderer() {
        panel = new RaycastRendererPanel(this);
        panel.setSpeedLabel("0");
    }

    public void setVolume(Volume vol) {
        System.out.println("Assigning volume");
        volume = vol;

        System.out.println("Computing gradients");
        gradients = new GradientVolume(vol);

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        // create a standard TF where lowest intensity maps to black, the highest to white, and opacity increases
        // linearly from 0.0 to 1.0 over the intensity range
        tFunc = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        
        // uncomment this to initialize the TF with good starting values for the orange dataset 
        //tFunc.setTestFunc();
        
        
        tFunc.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFunc, volume.getHistogram());
        
        tfEditor2D = new TransferFunction2DEditor(volume, gradients);
        tfEditor2D.addTFChangeListener(this);

        System.out.println("Finished initialization of RaycastRenderer");
    }

    public RaycastRendererPanel getPanel() {
        return panel;
    }

    public TransferFunction2DEditor getTF2DPanel() {
        return tfEditor2D;
    }
    
    public TransferFunctionEditor getTFPanel() {
        return tfEditor;
    }
     
    short getVoxel(double[] coord){
        if (coord[0] < 0 || coord[0] > volume.getDimX()-1 || coord[1] < 0 || coord[1] > volume.getDimY()-1
                || coord[2] < 0 || coord[2] > volume.getDimZ()-1) {
            return 0;
        }
        
        if (interpolation)
            return getVoxelWithInterpolation(coord);        
        else
            return getVoxelWithoutInterpolation(coord);        
    }

    private short getVoxelWithoutInterpolation(double[] coord) {
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);

        return volume.getVoxel(x, y, z);
    }
        
    /**
     * Implementation of tri-linear interpolation to approximate the value of the 
     * point at real coordinates coord using the values of its integer-coordinates neighbors.
     * @param coord
     * @return 
     */
    private short getVoxelWithInterpolation(double[] coord){        
        short X0, X1, X2, X3, X4, X5, X6, X7;
        final int x = (int) Math.floor(coord[0]);
        final int y = (int) Math.floor(coord[1]);
        final int z = (int) Math.floor(coord[2]);
        
        // https://dlwpswbsp.tue.nl/120-2015/2e6a8659155e485da8ef413b5a9cab63/Documents/2-spatial.pdf
        // consider X0 everything floored and alpha = x axis, beta = y axis, gamma = z axis

        X0 = volume.getVoxel(x, y , z);
        X1 = volume.getVoxel(x + 1, y , z);
        X2 = volume.getVoxel(x, y + 1, z);
        X3 = volume.getVoxel(x + 1, y + 1, z);
        
        X4 = volume.getVoxel(x, y , z + 1);
        X5 = volume.getVoxel(x + 1, y , z + 1);
        X6 = volume.getVoxel(x, y + 1, z + 1);
        X7 = volume.getVoxel(x + 1, y + 1, z + 1);

        double alpha    = coord[0] - x;
        double beta     = coord[1] - y;
        double gamma    = coord[2] - z;
        
        return (short)( (1-alpha) * (1-beta) * (1-gamma) * X0    + 
                alpha * (1-beta)  * (1-gamma) * X1      +
                (1-alpha) * beta  * (1-gamma) * X2      +
                alpha * beta  * (1-gamma) *     X3      +
                (1 - alpha) * (1-beta)  * gamma * X4    +
                alpha * (1-beta)  * gamma * X5          +
                (1 - alpha) * beta  * gamma * X6        +
                alpha * beta * gamma * X7 );
    }
    
    private int getSlicePixel(double[] pixelCoord) {
        return getVoxel(pixelCoord);
    }
    
    public void setRenderingType(RenderingType renderingType) {
        this.renderingType = renderingType;
    }
    
    /**
     * Shifts, moves, translates etc. the coordinates of a single pixel along vec by offset
     * @param pixelCoord Coordinates to be shifted
     * @param vec The axis
     * @param offset The amount
     */
    private void doOffset(double[] pixelCoord, double[] vec, int offset) {       
        VectorMath.setVector(pixelCoord, 
                pixelCoord[0] + vec[0] * offset, 
                pixelCoord[1] + vec[1] * offset, 
                pixelCoord[2] + vec[2] * offset);
    }

    private int getMIPPixel(double[] pixelCoord, double[] viewVec) {

        final int offset = 140;
        int maxVal = getVoxel(pixelCoord);
        int val = maxVal;
               
        doOffset(pixelCoord, viewVec, -offset);
        for (int step = -offset; step < offset; ++step){
            doOffset(pixelCoord, viewVec, 1);
                       
            val = getVoxel(pixelCoord);
            
            if (val > maxVal) {
                maxVal = val;
            }
        }
        
        return maxVal;
    }
        
    private TFColor getCompositePixel(double[] pixelCoord, double[] viewVec) {
        final int offset = 140;
        doOffset(pixelCoord, viewVec, -offset);

        TFColor c = null;
        TFColor C = new TFColor(0, 0, 0, 0);
        
        for (int step = -offset; step < offset; ++step){
            doOffset(pixelCoord, viewVec, 1);
            
            c = tFunc.getColor( getVoxel(pixelCoord) );
            
            C.r = c.a * c.r + (1-c.a) * C.r;
            C.g = c.a * c.g + (1-c.a) * C.g;
            C.b = c.a * c.b + (1-c.a) * C.b;
            C.a = c.a + (1-c.a) * C.a;
        }
        
        return C;        
    }

    private TFColor mapIntensityToGray(int val) {
        TFColor voxelColor = new TFColor();
        // Map the intensity to a grey value by linear scaling
        voxelColor.r = val / max;
        voxelColor.g = voxelColor.r;
        voxelColor.b = voxelColor.r;
        voxelColor.a = val > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
        // Alternatively, apply the transfer function to obtain a color
        // voxelColor = tFunc.getColor(val);
        
        return voxelColor;
    }
    
    private void setImageRGB(double[] viewMatrix){

        // clear image
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                image.setRGB(i, j, 0);
            }
        }

        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // sample on a plane through the origin of the volume data
        TFColor voxelColor = null;
        max = volume.getMaximum();
        
        
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                        + volumeCenter[0];
                pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                        + volumeCenter[1];
                pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                        + volumeCenter[2];                                              
                
                int val = 0;
                switch(renderingType){
                    case Slicer:                        
                        val = getSlicePixel(pixelCoord);
                        voxelColor = mapIntensityToGray(val);
                        break;
                    case MIP:
                        val = getMIPPixel(pixelCoord, viewVec);
                        voxelColor = mapIntensityToGray(val);
                        break;
                    case Composite:
                        voxelColor = getCompositePixel(pixelCoord, viewVec);
                        break;
                    default:
                        System.err.println("Error: unknown rendering type!");
                }
                
                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
                int c_red = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
                int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
                int c_blue = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, pixelColor);
            }
        }
    
    }


    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();

    }

    @Override
    public void visualize(GL2 gl) {


        if (volume == null) {
            return;
        }

        drawBoundingBox(gl);

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);

        long startTime = System.currentTimeMillis();
        setImageRGB(viewMatrix);    
        
        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panel.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();


        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }

    }
    private BufferedImage image;
    private double[] viewMatrix = new double[4 * 4];

    @Override
    public void changed() {
        for (int i=0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }

    public void setInterpolation(boolean selected) {
        this.interpolation = selected;
    }
}
