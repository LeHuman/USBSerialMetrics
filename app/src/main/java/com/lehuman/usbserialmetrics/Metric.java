package com.lehuman.usbserialmetrics;


import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Metric {
    public static class MovingAverage {

        private final double[] window;
        private float sum = 0f;
        private int fill = 0;
        private int position;

        public MovingAverage(int size) {
            this.window = new double[size];
        }

        public void add(double number) {
            if (fill == window.length) {
                sum -= window[position];
            } else {
                fill++;
            }
            sum += number;
            window[position++] = number;
            if (position == window.length) {
                position = 0;
            }
        }

        public double getAverage() {
            return sum / fill;
        }
    }

    boolean run = false;

    long elapsedTime = 0;
    long lastUpdate = 0;
    long lastValue = 0;

    MovingAverage average10 = new MovingAverage(10);
    MovingAverage average100 = new MovingAverage(100);
    MovingAverage average1000 = new MovingAverage(1000);
    MovingAverage averageT10 = new MovingAverage(10);
    MovingAverage averageT100 = new MovingAverage(100);
    MovingAverage averageT1000 = new MovingAverage(1000);

    public void start() {
        if (run)
            return;
        run = true;
        lastUpdate = SystemClock.elapsedRealtimeNanos();
        lastValue = 0;
    }

    public void stop() {
        run = false;
    }

    public void reset() {
        stop();
        average10 = new MovingAverage(10);
        average100 = new MovingAverage(100);
        average1000 = new MovingAverage(1000);
        averageT10 = new MovingAverage(10);
        averageT100 = new MovingAverage(100);
        averageT1000 = new MovingAverage(1000);
    }

    public void newValue(byte[] data) {
        if (!run)
            return;

        long currentTime = SystemClock.elapsedRealtimeNanos();
        elapsedTime = currentTime - lastUpdate;

        lastValue = (long) data.length * Byte.SIZE;
        average10.add(lastValue);
        average100.add(lastValue);
        average1000.add(lastValue);
        averageT10.add(elapsedTime / 1000000000.0);
        averageT100.add(elapsedTime / 1000000000.0);
        averageT1000.add(elapsedTime / 1000000000.0);
        lastUpdate = currentTime;
    }

    @NonNull
    @Override
    public String toString() {
        try {
            double avg10 = average10.getAverage() / averageT10.getAverage() / 1000;
            double avg100 = average100.getAverage() / averageT100.getAverage() / 1000;
            double avg1000 = average1000.getAverage() / averageT1000.getAverage() / 1000;
            return String.format(Locale.ENGLISH, "# bits : %d\nns : %d\navg last 10 : %.4f Kbps\navg last 100 : %.4f Kbps\navg last 1000 : %.4f Kbps", lastValue, elapsedTime, avg10, avg100, avg1000);
        } catch (Exception ignored) {
            return "Error";
        }

    }
}
