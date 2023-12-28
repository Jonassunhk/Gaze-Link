package com.demo.opencv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link clinician_mode_display#newInstance} factory method to
 * create an instance of this fragment.
 */
public class clinician_mode_display extends Fragment {

    Activity mActivity;
    LineChart mChart;
    public clinician_mode_display() {
        // Required empty public constructor
    }

    private static float[] FloatListToArray(List<Float> floatList) {
        float[] floatArray = new float[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            floatArray[i] = floatList.get(i);
        }
        return floatArray;
    }

    private static String[] StringListToArray(List<String> stringList) {
        String[] stringArray = new String[stringList.size()];
        for (int i = 0; i < stringList.size(); i++) {
            stringArray[i] = stringList.get(i);
        }
        return stringArray;
    }

    private static ArrayList<Entry> setChartData(float[] floatArray, String[] gazeType) {
        ArrayList<Entry> dataset = new ArrayList<>();
        for (int i = 0; i < floatArray.length; i++) {
            if (floatArray[i] < 1 && floatArray[i] > 0 && !Objects.equals(gazeType[i], "Closed")) { // only get values in the range
                dataset.add(new Entry(i, floatArray[i]));
            }
        }
        return dataset;
    }

    public static clinician_mode_display newInstance(ClinicalData clinicalData) {
        clinician_mode_display fragment = new clinician_mode_display();
        Bundle args = new Bundle();

        if (clinicalData != null && clinicalData.leftNICX != null && clinicalData.gazeType != null) {
            args.putFloatArray("leftNICX", FloatListToArray(clinicalData.leftNICX));
            args.putFloatArray("leftNICY", FloatListToArray(clinicalData.leftNICY));
            args.putStringArray("gazeType", StringListToArray(clinicalData.gazeType));
        }

        fragment.setArguments(args);
        return fragment;
    }

    private static LineDataSet initLineDataSet(ArrayList<Entry> dataset, String label, int color) {
        LineDataSet lineDataSet = new LineDataSet(dataset, label);
        lineDataSet.setDrawIcons(false);
        lineDataSet.enableDashedLine(10f, 5f, 0f);
        lineDataSet.enableDashedHighlightLine(10f, 5f, 0f);
        lineDataSet.setColor(color);
        lineDataSet.setCircleColor(color);
        lineDataSet.setLineWidth(1f);
        lineDataSet.setCircleRadius(1f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setValueTextSize(9f);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFormLineWidth(1f);
        return lineDataSet;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = requireActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        mChart = mActivity.findViewById(R.id.chart); // create chart
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);

        Bundle args = getArguments();
        if (args != null) {
            Log.d("ClinicalMode", "Received Data");
            float[] leftNICX = args.getFloatArray("leftNICX"); // left NIC x-coordinate
            float[] leftNICY = args.getFloatArray("leftNICY"); // left NIC y-coordinate
            String[] gazeType = args.getStringArray("gazeType"); // gaze type

            if (leftNICX != null && leftNICY != null && gazeType != null) { // if data present
                Log.d("ClinicalMode", "leftNICX size = " + leftNICX.length + " gazeType size = " + gazeType.length);
                ArrayList<Entry> dataset1 = setChartData(leftNICX, gazeType); // convert to data type
                ArrayList<Entry> dataset2 = setChartData(leftNICY, gazeType);

                LineDataSet set1, set2; // define data sets
                set1 = initLineDataSet(dataset1, "NIC left x-coordinate", Color.BLUE);
                set2 = initLineDataSet(dataset2, "NIC left y-coordinate", Color.RED);

                ArrayList<ILineDataSet> dataSets = new ArrayList<>(); // combine the data sets
                dataSets.add(set1);
                dataSets.add(set2);
                LineData data = new LineData(dataSets);
                mChart.setData(data); // set data to the graph
                YAxis yAxis = mChart.getAxisLeft();
                yAxis.setAxisMinimum(0f);
                yAxis.setAxisMaximum(1f);
                yAxis.setGranularity(0.1f);
            } else {
                Log.d("ClinicalMode",  "Data incomplete or missing");
            }
        } else {
            Log.d("ClinicalMode", "No data received");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.clinician_mode_fragment, container, false);
    }
}