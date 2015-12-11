/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import gui.TransferFunction2DEditor.TriangleWidget;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 *
 * @author michel
 */
public class TransferFunction2DView extends javax.swing.JPanel {

    TransferFunction2DEditor ed;
    public static final int DOTSIZE = 8;    
    

    /**
     * Creates new form TransferFunction2DView
     * @param ed
     */
    public TransferFunction2DView(TransferFunction2DEditor ed) {
        initComponents();
        
        this.ed = ed;
        
        addMouseMotionListener(new TriangleWidgetHandler());
        addMouseListener(new SelectionHandler());        
    }
    
    @Override
    public void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        int w = this.getWidth();
        int h = this.getHeight();
        g2.setColor(Color.white);
        g2.fillRect(0, 0, w, h);
        
        double maxHistoMagnitude = ed.histogram[0];
        for (int i = 0; i < ed.histogram.length; i++) {
            maxHistoMagnitude = ed.histogram[i] > maxHistoMagnitude ? ed.histogram[i] : maxHistoMagnitude;
        }
        
        double binWidth = (double) w / (double) ed.xbins;
        double binHeight = (double) h / (double) ed.ybins;
        maxHistoMagnitude = Math.log(maxHistoMagnitude);
        
        for (int y = 0; y < ed.ybins; y++) {
            for (int x = 0; x < ed.xbins; x++) {
                if (ed.histogram[y * ed.xbins + x] > 0) {
                    int intensity = (int) Math.floor(255 * (1.0 - Math.log(ed.histogram[y * ed.xbins + x]) / maxHistoMagnitude));
                    g2.setColor(new Color(intensity, intensity, intensity));
                    g2.fill(new Rectangle2D.Double(x * binWidth, h - (y * binHeight), binWidth, binHeight));
                }
            }
        }
        
        int ypos = h;
        for (TriangleWidget widget : ed.triangleWidgets) {
            
            TriangleWidgetView v = widget.v;
            int xpos = (int) (widget.baseIntensity * binWidth);
            g2.setColor(Color.black);    
            v.baseControlPoint = new Ellipse2D.Double(xpos - DOTSIZE / 2, ypos - DOTSIZE, DOTSIZE, DOTSIZE);
            g2.fill(v.baseControlPoint);
            g2.drawLine(xpos, ypos, xpos - (int) (widget.radius * binWidth * ed.maxGradientMagnitude), 0);
            g2.drawLine(xpos, ypos, xpos + (int) (widget.radius * binWidth * ed.maxGradientMagnitude), 0);
            
            
            g2.setColor(new Color((float)widget.color.r, (float)widget.color.g, (float)widget.color.b, (float)widget.color.a));
            
            Polygon p = new Polygon();
//            p.addPoint(xpos - (int) (widget.radius * binWidth * v.upperValue), v.upperValue);
            

            int base = (int) (widget.radius * binWidth * ed.maxGradientMagnitude);
            int long_base  = base * v.upperValue / ypos;
            int short_base = base * v.lowerValue / ypos;

            p.addPoint(  xpos - (base - long_base), v.upperValue);
            p.addPoint(  xpos - (base - short_base), v.lowerValue);
            p.addPoint(  xpos + (base - short_base), v.lowerValue);
            p.addPoint(  xpos + (base - long_base), v.upperValue);
            
            
            //p.addPoint(xpos + (int) (widget.radius * binWidth * v.upperValue), v.upperValue);

            g2.draw(p);
            g2.fillPolygon(p);
            
            v.radiusControlPoint = new Ellipse2D.Double(xpos + (widget.radius * binWidth * ed.maxGradientMagnitude) - DOTSIZE / 2, 0, DOTSIZE, DOTSIZE);
            g2.fill(v.radiusControlPoint);

            v.lowerControlPoint = new Ellipse2D.Double(0, v.lowerValue, DOTSIZE, DOTSIZE);
            g2.fill(v.lowerControlPoint);

            v.upperControlPoint = new Ellipse2D.Double(0, v.upperValue, DOTSIZE, DOTSIZE);
            g2.fill(v.upperControlPoint);

            g2.drawLine(0, v.lowerValue, w, v.lowerValue);
            g2.drawLine(0, v.upperValue, w, v.upperValue);
        }
    }
    
    
    private class TriangleWidgetHandler extends MouseMotionAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            
            for (TriangleWidgetView v : ed.getTriangleWidgetViews()){            
                if (v.baseControlPoint.contains(e.getPoint()) || v.radiusControlPoint.contains(e.getPoint())
                        || v.upperControlPoint.contains(e.getPoint()) || v.lowerControlPoint.contains(e.getPoint())) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));                    
                    return;
                }
            }
            setCursor(Cursor.getDefaultCursor());
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            
            for (TriangleWidget widget : ed.triangleWidgets) {
                
                TriangleWidgetView v = widget.v;
                
                if (v.selectedBaseControlPoint || v.selectedRadiusControlPoint) {
                    Point dragEnd = e.getPoint();

                    if (v.selectedBaseControlPoint) {
                        // restrain to horizontal movement
                        dragEnd.setLocation(dragEnd.x, v.baseControlPoint.getCenterY());
                    } else if (v.selectedRadiusControlPoint) {
                        // restrain to horizontal movement and avoid radius getting 0
                        dragEnd.setLocation(dragEnd.x, v.radiusControlPoint.getCenterY());
                        if (dragEnd.x - v.baseControlPoint.getCenterX() <= 0) {
                            dragEnd.x = (int) (v.baseControlPoint.getCenterX() + 1);
                        }
                    }
                    if (dragEnd.x < 0) {
                        dragEnd.x = 0;
                    }
                    if (dragEnd.x >= getWidth()) {
                        dragEnd.x = getWidth() - 1;
                    }
                    double w = getWidth();
                    double h = getHeight();
                    double binWidth = (double) w / (double) ed.xbins;
                    if (v.selectedBaseControlPoint) {
                        
                        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        
                        widget.baseIntensity = (short) (dragEnd.x / binWidth);
                    } else if (v.selectedRadiusControlPoint) {
                        widget.radius = (dragEnd.x - (widget.baseIntensity * binWidth)) / (binWidth * ed.maxGradientMagnitude);
                    }
                    ed.setSelectedInfo();
                    repaint();
                } else if (v.selectedUpperControlPoint) {
                    v.upperValue = e.getY();
                    widget.upperValue = (300 - v.upperValue) * ((int) (ed.gradvol.getMaxGradientMagnitude())) / 300;
                    //ed.triangleWidget.upperValue = (int)  (( Math.log(300) - Math.log(upperValue)) / Math.log(300) * (ed.gradvol.getMaxGradientMagnitude()));

                    //ed.triangleWidget.upperValue = (int) ( Math.log(300 - upperValue) / Math.log(300) *ed.gradvol.getMaxGradientMagnitude() );
                    //ed.triangleWidget.upperValue = (300 / upperValue;
                    //ed.triangleWidget.upperValue = (300 / upperValue) * ((int) ed.gradvol.getMaxGradientMagnitude());
                    //System.out.println("upper = " + upperValue + " ed.triangleWidget.upperValue = " +ed.triangleWidget.upperValue);
                    repaint();
                } else if (v.selectedLowerControlPoint) {
                    v.lowerValue = e.getY();
                    widget.lowerValue = (300 - v.lowerValue) * ((int) ed.gradvol.getMaxGradientMagnitude()) / 300;

                    //ed.triangleWidget.lowerValue = 300 / lowerValue * ((int) ed.gradvol.getMaxGradientMagnitude());
                    //System.out.println("lower = " + ed.triangleWidget.lowerValue);
                    repaint();
                }
            }

        }

    }
        
    private class SelectionHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            
            int index = 0; // array list ... hiuh, we know that the for each loop iterates from index 0 to length-1
            for(TriangleWidgetView v : ed.getTriangleWidgetViews()){
                
                if (v.baseControlPoint.contains(e.getPoint())) {
                    v.selectedBaseControlPoint = true;
                    ed.selectedIndex = index;
                    return;
                } else if (v.radiusControlPoint.contains(e.getPoint())) {
                    v.selectedRadiusControlPoint = true;
                    ed.selectedIndex = index;
                    return;
                } else if (v.upperControlPoint.contains(e.getPoint())) {
                    v.selectedUpperControlPoint = true;
                    ed.selectedIndex = index;
                    return;
                } else if (v.lowerControlPoint.contains(e.getPoint())) {
                    v.selectedLowerControlPoint = true;
                    ed.selectedIndex = index;
                    return;
                } else {
                    v.selectedRadiusControlPoint = false;
                    v.selectedBaseControlPoint = false;

                    v.selectedUpperControlPoint = false;
                    v.selectedLowerControlPoint = false;
                }
                
                index++;
            }
            //System.out.println(e.getPoint());
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            
            for (TriangleWidgetView v : ed.getTriangleWidgetViews()) {

                v.selectedRadiusControlPoint = false;
                v.selectedBaseControlPoint = false;
                v.selectedUpperControlPoint = false;
                v.selectedLowerControlPoint = false;
            }
            ed.changed();
            repaint();
            ed.setSelectedInfo();
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
