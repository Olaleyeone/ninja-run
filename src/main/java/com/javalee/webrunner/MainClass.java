package com.javalee.webrunner;

import ninja.standalone.NinjaJetty;
import ninja.utils.NinjaConstant;

/**
 * @author Olaleye Afolabi <olaleyeone@gmail.com>
 */
public class MainClass {

    public static void main(String[] a) {
        System.setProperty(NinjaConstant.MODE_KEY_NAME, NinjaConstant.MODE_DEV);
        new NinjaJetty().run();
    }
}
