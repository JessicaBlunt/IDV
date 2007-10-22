/*
 * $Id: FlowPlanViewControl.java,v 1.65 2007/08/21 14:30:37 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;

import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;



/**
 * A plan view control for flow data (vector or wind barbs)
 *
 * @author IDV Development Team
 * @version $Revision: 1.65 $
 */
public class FlowPlanViewControl extends PlanViewControl {


    /** property for sharing flow range */
    public static final String SHARE_FLOWRANGE =
        "FlowPlanViewControl.SHARE_FLOWRANGE";

    /** property for sharing flow scale */
    public static final String SHARE_FLOWSCALE =
        "FlowPlanViewControl.SHARE_FLOWRANGE";

    /** a component to change the barb size */
    ValueSliderWidget barbSizeWidget;

    /** a component to change the skip */
    ValueSliderWidget skipFactorWidget;

    /** a component to change the streamline density */
    JComponent densityComponent;

    /** a label listing the range of the data */
    JLabel flowRangeLabel;

    /** the density label */
    private JLabel densityLabel;

    /** flag for streamlines */
    boolean isStreamlines = false;

    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** flag for wind barbs */
    boolean isThreeComponents = false;

    /** a scale factor */
    protected final float scaleFactor = 0.02f;

    /** a scale value */
    float flowScaleValue = 4.0f;

    /** streamline density value */
    float streamlineDensity = 1.0f;

    /** slider components */
    private JComponent[] widthSliderComps;

    /** Range for flow scale */
    private Range flowRange;

    /**
     * Create a new FlowPlanViewControl; set attribute flags
     */
    public FlowPlanViewControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH);
    }

    /**
     * Get the color table widget label text.
     * @return  the label text.
     */
    public String getColorWidgetLabel() {
        return "Color";
    }

    /**
     * Get the displayable for depicting the data
     * @return  the FlowDisplayable
     */
    FlowDisplayable getGridDisplay() {
        return (FlowDisplayable) getPlanDisplay();
    }

    /**
     * Create the {@link ucar.visad.display.Displayable} for the data depiction.
     * @return the Displayable.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        FlowDisplayable planDisplay;
        if (isWindBarbs) {
            planDisplay =
                new WindBarbDisplayable("FlowPlanViewControl_windbarbs_"
                                        + ((datachoice != null)
                                           ? datachoice.toString()
                                           : ""), null);
        } else {
            planDisplay = new FlowDisplayable("FlowPlanViewControl_vectors_"
                    + ((datachoice != null)
                       ? datachoice.toString()
                       : ""), null);
        }
        planDisplay.setStreamlinesEnabled(isStreamlines);
        planDisplay.setStreamlineDensity(streamlineDensity);
        //addAttributedDisplayable(planDisplay, FLAG_SKIPFACTOR);
        addAttributedDisplayable(planDisplay);
        return planDisplay;
    }

    /**
     * Called to initialize this control from the given dataChoice;
     * override super class instance to set skip factor before displaying data.
     *
     * @param dataChoice  choice that describes the data to be loaded.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        Trace.call1("FlowPlanView.setData");
        boolean result = super.setData(dataChoice);
        if ( !result) {
            Trace.call2("FlowPlanView.setData");
            return false;
        }
        if ( !getWindbarbs()) {
            setFlowRange();
        }
        setFlowScale(flowScaleValue);
        //        setSkipValue(skipValue);
        Trace.call2("FlowPlanView.setData");
        return true;
    }

    /**
     * Get control widgets special to this control.
     *
     * @param controlWidgets   list of control widget from the superclass
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {

        barbSizeWidget = new ValueSliderWidget(this, 1, 21, "flowScale",
                getSizeLabel());
        JPanel extra = GuiUtils.hbox(GuiUtils.rLabel("Scale:  "),
                                     barbSizeWidget.getContents(false));
        if ( !getWindbarbs()) {
            extra = GuiUtils.hbox(extra,
                                  GuiUtils.hbox(GuiUtils.filler(),
                                      doMakeFlowRangeComponent()));
        }
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Vector Size: "),
                GuiUtils.left(extra)));

        skipFactorWidget = new ValueSliderWidget(this, 0, 10, "skipValue",
                getSkipWidgetLabel());
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Skip: "),
                GuiUtils.left(
                    GuiUtils.hbox(
                        GuiUtils.rLabel("XY:  "),
                        skipFactorWidget.getContents(false)))));
        if ( !getIsThreeComponents()) {
            JCheckBox toggle = new JCheckBox("Show", isStreamlines);
            densityLabel     = GuiUtils.rLabel("Density: ");
            densityComponent = doMakeDensityComponent();
            enableDensityComponents();
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isStreamlines = ((JCheckBox) e.getSource()).isSelected();
                    getGridDisplay().setStreamlinesEnabled(isStreamlines);
                    enableBarbSizeBox();
                    enableDensityComponents();
                }
            });
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Streamlines:"),
                    GuiUtils.hbox(toggle, densityComponent)));

            controlWidgets.add(new WrapperWidget(this, densityLabel,
                    densityComponent));
        }
        /*
        if (!getWindbarbs()) {
            controlWidgets.add(new WrapperWidget(this, GuiUtils.rLabel("Scaling Range: "),
                    doMakeFlowRangeComponent()));
        }
        */
        enableBarbSizeBox();
        super.getControlWidgets(controlWidgets);
    }

    /**
     * Create the streamline density slider
     *
     * @return The panel that shows the streamline density slider
     */
    protected JComponent doMakeDensityComponent() {
        int sliderPos = (int) (getStreamlineDensity() * 100);
        int min       = 10;
        int max       = 500;
        sliderPos = Math.min(Math.max(sliderPos, min), max);
        JSlider densitySlider = GuiUtils.makeSlider(min, max, sliderPos,
                                    this, "densitySliderChanged");
        densitySlider.setToolTipText(
            "Control the density of the streamlines");

        return GuiUtils.doLayout(new Component[] { GuiUtils.rLabel("Low "),
                densitySlider, GuiUtils.lLabel(" High"),
                GuiUtils.filler() }, 4, GuiUtils.WT_NYNY, GuiUtils.WT_N);
    }

    /**
     * Create the streamline density slider
     *
     * @return The panel that shows the streamline density slider
     */
    protected JComponent doMakeFlowRangeComponent() {

        setFlowRangeLabel();
        JButton editButton =
            GuiUtils.getImageButton("/ucar/unidata/idv/images/edit.gif",
                                    getClass());
        editButton.setToolTipText("Range used for scaling the vector size");
        final RangeDialog rd =
            new RangeDialog(this, flowRange,
                            "Set the range of data for sizing vectors",
                            "setFlowRange");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                rd.showDialog();
                setFlowRangeLabel();
            }
        });
        return GuiUtils.hbox(flowRangeLabel, editButton);
    }

    /**
     * Method called by other classes that share the the state.
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        try {
            if (dataId.equals(SHARE_FLOWRANGE)) {
                setFlowRange((Range) data[0]);
            } else if (dataId.equals(SHARE_FLOWSCALE)) {
                setFlowScale(((Float) data[0]).floatValue());
            } else {
                super.receiveShareData(from, dataId, data);
            }
        } catch (Exception exc) {
            logException("Error processing shared state: " + dataId, exc);
        }
    }

    /**
     * Set the range label
     */
    private void setFlowRangeLabel() {
        if (flowRangeLabel == null) {
            flowRangeLabel = new JLabel("Range: ", SwingConstants.RIGHT);

        }
        Range r = getFlowRange();
        if (r != null) {
            flowRangeLabel.setText("Range: " + r.formatMin() + " to "
                                   + r.formatMax());
        } else {
            flowRangeLabel.setText("Range: Undefined");
        }
    }

    /**
     * The streamline density slider changed
     *
     * @param value slider value
     */
    public void densitySliderChanged(int value) {
        try {
            setStreamlineDensity((float) (value / 100.));
        } catch (Exception exc) {
            logException("Setting streamline density ", exc);
        }
    }

    /**
     * enable the density slider components
     */
    private void enableDensityComponents() {
        if (densityComponent != null) {
            GuiUtils.enableTree(densityComponent, isStreamlines);
        }
        if (densityLabel != null) {
            GuiUtils.enableTree(densityLabel, isStreamlines);
        }
    }

    /**
     * enable the barb size box
     */
    private void enableBarbSizeBox() {
        barbSizeWidget.setEnabled( !isStreamlines);
    }

    /**
     * Get the appropriate size label for this instance.
     * @return the label
     */
    private String getSizeLabel() {
        return (isWindBarbs)
               ? "Barb Size"
               : "Vector Size";
    }

    /**
     * Wrapper around {@link #addTopographyMap(int)} to allow subclasses
     * to set their own index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void addTopographyMap() throws VisADException, RemoteException {
        addTopographyMap(2);
    }

    /**
     * Set the streamline property.
     * Used by XML persistence
     *
     * @param v  true to use streamlines
     */
    public void setStreamlines(boolean v) {
        isStreamlines = v;
    }

    /**
     * Get the streamline property.
     * Used by XML persistence
     *
     * @return  the current streamline property (true if streamlines)
     */
    public boolean getStreamlines() {
        return isStreamlines;
    }

    /**
     * Set the wind barb property.
     * Used by XML persistence
     *
     * @param v   true if you want to use wind barbs
     */
    public void setWindbarbs(boolean v) {
        isWindBarbs = v;
    }

    /**
     * Get the wind barb property.
     * Used by XML persistence
     *
     * @return  true if wind barbs are being depicted
     */
    public boolean getWindbarbs() {
        return isWindBarbs;
    }

    /**
     * Set the use 3 components property.
     * Used by XML persistence
     *
     * @param v   true if using u, v and w
     */
    public void setIsThreeComponents(boolean v) {
        isThreeComponents = v;
    }

    /**
     * Get the use 3 components property.
     * Used by XML persistence
     *
     * @return  true if using 3 components for wind
     */
    public boolean getIsThreeComponents() {
        return isThreeComponents;
    }

    /**
     * Set the scale factor.
     * Used by XML persistence
     *
     * @param s   the new factor
     */
    public void setScaleFactor(float s) {
        //No op
    }

    /**
     * Get the flow scale.
     * Used by XML persistence
     *
     * @return  the flow scale for this control
     */
    public float getFlowScale() {
        return flowScaleValue;
    }

    /**
     * Set the flow scale.
     * Used by XML persistence
     *
     * @param f   new flow scale
     */
    public void setFlowScale(float f) {
        flowScaleValue = f;
        if (getGridDisplay() != null) {
            getGridDisplay().setFlowScale(flowScaleValue * scaleFactor);
        }
        if (getHaveInitialized()) {
            doShare(SHARE_FLOWRANGE, flowRange);
        }
    }

    /**
     * Get the flow range.
     * Used by XML persistence
     *
     * @return  the flow range for this control
     */
    public Range getFlowRange() {
        return flowRange;
    }

    /**
     * Set the flow range.
     * Used by XML persistence
     *
     * @param f   new flow range
     */
    public void setFlowRange(Range f) {
        flowRange = f;
        if (getHaveInitialized()) {
            if (getGridDisplay() != null && flowRange != null && !getWindbarbs()) {
                try {
                    getGridDisplay().setFlowRange(flowRange.getMin(),
                            flowRange.getMax());
                } catch (Exception excp) {
                    logException("setFlowRange: ", excp);
                }
            }
            setFlowRangeLabel();
            doShare(SHARE_FLOWRANGE, flowRange);
        }
    }

    /**
     * Get the streamline density.
     * Used by XML persistence
     *
     * @return  the streamline density for this control
     */
    public float getStreamlineDensity() {
        return streamlineDensity;
    }

    /**
     * Set the streamline density.
     * Used by XML persistence
     *
     * @param f   new flow scale
     */
    public void setStreamlineDensity(float f) {
        streamlineDensity = f;
        if (getGridDisplay() != null) {
            getGridDisplay().setStreamlineDensity(f);
        }
    }

    /**
     *  Return the range attribute of the colorTable  (if non-null)
     *  else return null;
     * @return The range from the color table attribute
     */
    public Range getColorRangeFromData() {
        Range r = super.getColorRangeFromData();
        return makeFlowRange(r);
    }

    /**
     * Get the range for the current slice.
     * @return range or null
     */
    protected Range getLevelColorRange() {
        return makeFlowRange(super.getLevelColorRange());
    }

    /**
     * Make a flow range from the given range (max of abs of min and max)
     *
     * @param r   range to normalize
     *
     * @return  flow type range
     */
    private Range makeFlowRange(Range r) {
        if (haveMultipleFields()) {
            return r;
        }
        if (r == null) {
            return r;
        }
        double max = Math.max(Math.abs(r.getMax()), Math.abs(r.getMin()));
        return new Range(-max, max);
    }

    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        if (getGridDataInstance() == null) {
            return false;
        }
        return (getGridDataInstance().getNumRealTypes()
                > ((getIsThreeComponents())
                   ? 3
                   : 2)) && !getMultipleIsTopography();
    }

    /**
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges.
     *
     * @return  The index to be used for the color range.
     */
    protected int getColorRangeIndex() {
        if (getMultipleIsTopography()) {
            return 0;
        }
        return getIsThreeComponents()
               ? 3
               : 2;
    }

    /**
     * Set the range for the flow components
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setFlowRange() throws RemoteException, VisADException {
        if ((getGridDisplay() != null) && !getWindbarbs()) {
            if (getFlowRange() == null) {
                Range[] ranges = null;
                Data    data   = getGridDisplay().getData();
                if (data != null) {
                    ranges = GridUtil.getMinMax((FieldImpl) data);
                    double max      = Double.NEGATIVE_INFINITY;
                    double min      = Double.POSITIVE_INFINITY;
                    int    numComps = getIsThreeComponents()
                                      ? 3
                                      : 2;
                    for (int i = 0; i < numComps; i++) {
                        Range compRange = ranges[i];
                        max = Math.max(compRange.getMax(), max);
                        min = Math.min(compRange.getMin(), min);
                    }
                    if ( !Double.isInfinite(max) && !Double.isInfinite(min)) {
                        max = Math.max(max, -min);
                        min = -max;
                    }
                    // System.out.println("setFlowRange: " + min + " to " + max);
                    //getGridDisplay().setFlowRange(min,max);
                    setFlowRange(new Range(min, max));
                } else {  // gotta set it to something
                    setFlowRange(new Range(-40, 40));
                }
            } else {
                getGridDisplay().setFlowRange(flowRange.getMin(),
                        flowRange.getMax());
            }
        }
    }

}

