package com.haulmont.simpleplugins;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceLoader;

public class Main {

    public static void main(String[] args) throws IOException {
        File loc = new File("plugins");

        File[] flist = loc.listFiles(new FileFilter() {
            public boolean accept(File file) {return file.getPath().toLowerCase().endsWith(".jar");}
        });
        URL[] urls = new URL[flist.length];
        for (int i = 0; i < flist.length; i++)
            urls[i] = flist[i].toURI().toURL();
        URLClassLoader ucl = new URLClassLoader(urls);

        ServiceLoader<SimplePlugin> sl = ServiceLoader.load(SimplePlugin.class, ucl);
        Iterator<SimplePlugin> apit = sl.iterator();
        while (apit.hasNext()) {
            System.out.println(apit.next().getClass().getName());
        }
    }
}
