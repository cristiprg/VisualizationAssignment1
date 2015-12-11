/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.geom.Ellipse2D;

/**
 *
 * @author cristiprg
 */
class TriangleWidgetView {

    public Ellipse2D.Double baseControlPoint, radiusControlPoint;
    public boolean selectedBaseControlPoint, selectedRadiusControlPoint;

    public Ellipse2D.Double lowerControlPoint, upperControlPoint;
    public boolean selectedLowerControlPoint, selectedUpperControlPoint;
    public int lowerValue, upperValue;

    public TriangleWidgetView() {

        selectedBaseControlPoint = false;
        selectedRadiusControlPoint = false;
        selectedLowerControlPoint = false;
        selectedUpperControlPoint = false;

        upperValue = 0;
        lowerValue = 300 - TransferFunction2DView.DOTSIZE;
    }
}
