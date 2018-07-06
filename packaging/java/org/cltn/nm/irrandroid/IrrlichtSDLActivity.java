package org.cltn.nm.irrandroid;

import org.libsdl.app.SDLActivity;

public class IrrlichtSDLActivity extends SDLActivity {
    /*
    static {
        try
        {
            System.loadLibrary("MGD");
        }
        catch (UnsatisfiedLinkError e)
        {

        }
    }
    */
    protected String getMainSharedObject() {
        return "libhello.so";
    }
    protected String[] getLibraries() {
        return new String[] {
            "hello"
        };
    }
};
