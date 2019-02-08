package com.haulmont.simpleplugins;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;


public class Main {

    public static final String PLUGINS_DIR = "plugins";

    public static void main(String[] args) throws IOException {
        Iterator<SimplePlugin> pluginIterator = loadPlugins(PLUGINS_DIR);
        Iterator<SimplePlugin> apitJigsaw = loadWithJigsaw(PLUGINS_DIR);
        while (pluginIterator.hasNext()) {
            System.out.println(pluginIterator.next().getClass().getName());
        }
    }

    public static Iterator<SimplePlugin> loadPlugins(String pluginsDir) throws IOException {
        // получаем URL-ы плагинов
        File loc = new File(pluginsDir);
        File[] filesList = loc.listFiles(file -> file.getPath().toLowerCase().endsWith(".jar"));
        URL[] urls = getUrls(filesList);

        // грузим классы плагинов
        ClassLoader baseClassLoader = ClassLoader.getSystemClassLoader();
        URLClassLoader ucl = new URLClassLoader(urls, baseClassLoader);
        return ServiceLoader.load(SimplePlugin.class, ucl).iterator();
    }

    private static URL[] getUrls(File[] filesList) throws MalformedURLException {
        URL[] urls = new URL[filesList.length];

        for (int i = 0; i < filesList.length; i++)
            urls[i] = filesList[i].toURI().toURL();
        return urls;
    }


    public static Iterator<SimplePlugin> loadWithJigsaw(String pluginsDir) {
        ModuleLayer pluginLayer = createPluginLayer(pluginsDir);
        return ServiceLoader.load(pluginLayer, SimplePlugin.class).iterator();
    }

    private static ModuleLayer createPluginLayer(String pluginsDir) {
        // получаем список модулей в директории
        Path pluginsPath = Paths.get(pluginsDir);
        ModuleFinder pluginsModuleFinder = ModuleFinder.of(pluginsPath);
        List<String> pluginModules = pluginsModuleFinder.findAll().stream()
                .map(module -> module.descriptor().name())
                .collect(Collectors.toList());

        // добавляем свои модули (из core) в список зависимостей загружаемого модуля
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration configuration = bootLayer.configuration().resolve(pluginsModuleFinder, ModuleFinder.of(), pluginModules);
        List<ModuleLayer> parentModules = new LinkedList<>();
        parentModules.add(bootLayer);

        // грузим модули плагинов в новый слой одним класс лоадером и возвращаем этот слой
        return ModuleLayer.defineModulesWithOneLoader(
                configuration,
                parentModules,
                ClassLoader.getPlatformClassLoader()
        ).layer();
    }


}
