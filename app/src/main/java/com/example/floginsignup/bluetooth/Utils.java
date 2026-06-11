package com.example.floginsignup.bluetooth;

public class Utils {

    public static Utils instance;
    private MainActivity2 mainAct;

    public MainActivity2 getMainAct() {
        return mainAct;
    }

    public Utils(MainActivity2 mainAct)
    {
        this.mainAct = mainAct;
    }

    public Utils()
    {
    }

    public static Utils getInstance(MainActivity2 mainAct)
    {
        if (instance == null)
            instance = new Utils(mainAct);

        return instance;
    }

    public static Utils getInstance()
    {
        if (instance == null)
            instance = new Utils();

        return instance;
    }
}
