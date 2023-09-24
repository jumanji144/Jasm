package me.darknet.assembler.util;

public class LabelUtil {

    public static String getLabelName(int index) {
        StringBuilder label = new StringBuilder();

        while (index >= 0) {
            label.insert(0, (char) ('A' + index % 26));
            index = (index / 26) - 1;
        }

        return label.toString();
    }

}
