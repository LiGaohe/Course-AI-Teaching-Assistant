package org.example.ta.util;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface MyIcons {
    Icon Action = IconLoader.getIcon("/icons/pluginIcon.svg", MyIcons.class);
    Icon Structure = IconLoader.getIcon("/icons/pluginIcon.svg", MyIcons.class);
    Icon FileType = IconLoader.getIcon("/icons/pluginIcon.svg", MyIcons.class);
}