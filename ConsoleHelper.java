package com.javarush.task.task30.task3008;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    private static BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message) {
        System.out.println(message);
    }

    public static String readString() {
        String str = "";
        boolean flag = true;

        while (flag) {
            try {
                str = bufferedReader.readLine();
                flag = false;
            } catch (IOException e) {
                System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
            }
        }
        return str;
    }

    public static int readInt() {  //считывание порта для соединения
        int num = 0;
        boolean flag = true;

        while (flag) {
            try {
                num =  Integer.parseInt(readString());
                flag = false;
            } catch (NumberFormatException e) {
                System.out.println("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");;
            }
        }
        return num;
    }
}
